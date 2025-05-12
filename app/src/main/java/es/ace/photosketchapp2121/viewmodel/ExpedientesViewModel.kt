package es.ace.photosketchapp2121.viewmodel

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import es.ace.photosketchapp2121.Expediente
import android.content.Context
import androidx.lifecycle.viewModelScope
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.SheetsScopes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.util.Log
import android.accounts.Account
// Importa GoogleIdTokenCredential si planeas pasarla (aunque usaremos el ID/email)
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import android.content.Intent // Import para el Intent
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException // La excepción específica
// Importar cosas para poder buscar
import kotlinx.coroutines.flow.SharingStarted // Import necesario
import kotlinx.coroutines.flow.combine // Import necesario
import kotlinx.coroutines.flow.stateIn // Import necesario
// Imports para buscar fotos para cada carpeta de expediente (local)
import java.io.File // Para buscar archivos
import android.net.Uri // Para las URIs de las fotos
import android.os.Environment
import androidx.compose.ui.geometry.Offset
import androidx.core.net.toUri // Extensión cómoda para convertir File a Uri
// Imports para que no se pierdan los trazos al girar el dispositivo
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import android.graphics.Bitmap // Para android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.toArgb
import android.graphics.Canvas as AndroidCanvas // Alias para evitar conflicto con Compose Canvas
import android.graphics.Paint as AndroidPaint
import android.graphics.Path as AndroidPath
import java.io.FileOutputStream
import androidx.compose.ui.unit.IntSize // Si no está ya
import java.text.SimpleDateFormat
import java.util.Locale
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import androidx.core.graphics.createBitmap
import kotlinx.coroutines.withContext
import android.content.pm.ActivityInfo // Para las constantes de orientación
// imports para DRIVE!
import com.google.api.services.drive.Drive // Para el servicio de Drive
import com.google.api.services.drive.DriveScopes // Para los permisos de Drive
import android.app.Application // Para el parámetro del constructor
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel // Nueva clase base
import es.ace.photosketchapp2121.AppDatabase
import es.ace.photosketchapp2121.PhotoInfo
import es.ace.photosketchapp2121.PhotoInfoDao
import es.ace.photosketchapp2121.SyncStatus
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import com.google.api.client.http.FileContent
import com.google.api.client.http.HttpRequestInitializer
import com.google.api.services.drive.model.File as DriveFile // Alias para evitar colisión con java.io.File
import com.google.api.services.drive.model.FileList
import java.io.IOException // Para manejar errores de IO
import kotlin.collections.iterator

// Guarda las propiedades de un trazo (color, grosor)
data class PathProperties(
    val color: Color = Color.Red, // Color por defecto (¡Rojo para que se vea bien!)
    val strokeWidth: Float = 6f, // Grosor por defecto (un poco grueso para empezar)
    val strokeCap: StrokeCap = StrokeCap.Round, // Terminación redondeada
    val strokeJoin: StrokeJoin = StrokeJoin.Round // Unión redondeada
)

// Guarda un Path (la línea) y sus propiedades
data class PathData(
    val points: List<Offset> = emptyList(), // <-- CAMBIADO CUANDO DESARROLLAMOS LO DE GUARDAR
    // val path: Path = Path(), // El objeto Path que contiene los puntos
    val properties: PathProperties = PathProperties() // Las propiedades de este trazo
)

// Hereda de ViewModel para obtener sus beneficios
class ExpedientesViewModel(application: Application) : AndroidViewModel(application) {

    // Estado interno mutable (solo modificable desde el ViewModel)
    // Inicialmente contiene una lista vacía de expedientes
    private val _listaCompletaExpedientes = MutableStateFlow<List<Expediente>>(emptyList())

    // Estado para el usuario logueado
    private val _googleUser = MutableStateFlow<GoogleIdTokenCredential?>(null)
    val googleUser: StateFlow<GoogleIdTokenCredential?> = _googleUser.asStateFlow()

    // Estado para mensajes de error de UI
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Estado público inmutable (solo para observar desde la UI)
    // val expedientes: StateFlow<List<Expediente>> = _listaCompletaExpedientes.asStateFlow()

    // Guarda la URI de la foto que se está editando actualmente
    private var currentEditingUri: String? = null

    // Nuevo StateFlow para guardar el texto de búsqueda actual
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow() // Público para que la UI lo observe

    private val photoInfoDao: PhotoInfoDao // Propiedad para guardar nuestro DAO

    init { // Bloque de inicialización, se ejecuta cuando se crea el ViewModel
        // Obtenemos la instancia de nuestra base de datos AppDatabase
        val database = AppDatabase.getInstance(application) // 'application' viene del constructor de AndroidViewModel
        // Obtenemos el DAO desde la instancia de la base de datos
        photoInfoDao = database.photoInfoDao()
        Log.d("VIEWMODEL_INIT", "PhotoInfoDao inicializado en ExpedientesViewModel.")
        // Aquí podríamos llamar a cargar datos iniciales si fuera necesario
    }

    // StateFlow para la lista de URIs de las fotos en la galería actual
    private val _galleryPhotos = MutableStateFlow<List<Uri>>(emptyList())
    val galleryPhotos: StateFlow<List<Uri>> = _galleryPhotos.asStateFlow()

    private val _galleryPhotosInfo = MutableStateFlow<List<PhotoInfo>>(emptyList()) // Guardará objetos PhotoInfo
    val galleryPhotosInfo: StateFlow<List<PhotoInfo>> = _galleryPhotosInfo.asStateFlow()

    // Nuevo StateFlow público que expone la lista FILTRADA a la UI
    val expedientesFiltrados: StateFlow<List<Expediente>> = combine(
        _listaCompletaExpedientes, // Observa la lista completa
        searchQuery                // Observa el texto de búsqueda
    ) { listaCompleta, query ->   // Función que se ejecuta si cualquiera de los dos cambia
        if (query.isBlank()) {
            listaCompleta // Si no hay búsqueda, devuelve la lista completa
        } else {
            // Si hay búsqueda, filtra la lista completa
            listaCompleta.filter { expediente ->
                // Comprueba si el nombre O la dirección contienen el texto (ignorando mayús/minús)
                expediente.nombre.contains(query, ignoreCase = true) ||
                        expediente.direccion.contains(query, ignoreCase = true)
            }
        }
    }.stateIn( // Convierte el Flow combinado en un StateFlow
        scope = viewModelScope, // Necesita el scope del ViewModel
        started = SharingStarted.WhileSubscribed(5000L), // Mantiene el estado 5s después de que la UI deje de observar
        initialValue = emptyList() // Valor inicial mientras se cargan los datos
    )

    private val SPREADSHEET_ID = "1mT6odh9pbkdJoC_valykj5y1YIX3xjV86ucvIvovLGI"
    private val SHEET_NAME_NO_ACEPTADOS = "No aceptados"
    private val SHEET_NAME_ACEPTADOS = "Aceptados"
    // Asumiendo que los datos empiezan en fila 2 y son columnas A, B, C en ambas hojas
    private val COLUMNS_TO_READ = "A2:C"

    private val _consentIntent = MutableStateFlow<Intent?>(null)
    val consentIntent: StateFlow<Intent?> = _consentIntent.asStateFlow()

    // Lista de trazos dibujados y guardados (historial principal)
    private val _drawnPaths = MutableStateFlow<List<PathData>>(emptyList())
    val drawnPaths: StateFlow<List<PathData>> = _drawnPaths.asStateFlow()

    // Estado para los puntos del trazo actual
    private val _currentPoints = MutableStateFlow<List<Offset>>(emptyList())
    val currentPoints: StateFlow<List<Offset>> = _currentPoints.asStateFlow()

    // Estado para las propiedades del trazo actual
    private val _currentPathProperties = MutableStateFlow(PathProperties()) // Usa el constructor por defecto
    val currentPathProperties: StateFlow<PathProperties> = _currentPathProperties.asStateFlow()

    // Pilas para Deshacer y Rehacer
    private val undoStack = mutableListOf<PathData>() // Guarda los trazos deshechos
    private val redoStack = mutableListOf<PathData>() // Guarda los trazos rehechos temporalmente

    // Estados para habilitar/deshabilitar botones Undo/Redo
    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    // Índice de la foto actual DENTRO de la lista galleryPhotos
    private val _currentPhotoInGalleryIndex = MutableStateFlow(0)
    val currentPhotoInGalleryIndex: StateFlow<Int> = _currentPhotoInGalleryIndex.asStateFlow() // No necesitamos exponer el índice

    // La URI de la foto que se está mostrando actualmente en el editor/visor
    private val _currentPhotoUriForEditor = MutableStateFlow<Uri?>(null)
    val currentPhotoUriForEditor: StateFlow<Uri?> = _currentPhotoUriForEditor.asStateFlow()

    // Estado para saber si estamos en modo edición o modo vista
    private val _isEditingMode = MutableStateFlow(false)
    val isEditingMode: StateFlow<Boolean> = _isEditingMode.asStateFlow()

    // Estado para saber si hay cambios
    private val _lastSavedEditedPhotoUri = MutableStateFlow<Uri?>(null)
    val lastSavedEditedPhotoUri: StateFlow<Uri?> = _lastSavedEditedPhotoUri.asStateFlow()

    private val _currentPhotoOriginalDimensions = MutableStateFlow<IntSize?>(null)
    val currentPhotoOriginalDimensions: StateFlow<IntSize?> = _currentPhotoOriginalDimensions.asStateFlow()

    enum class DrawingTool {
        PENCIL,
        LINE
        // Añadiremos más si es necesario
    }

    private val _hasUnsavedChanges = MutableStateFlow(false)
    val hasUnsavedChanges: StateFlow<Boolean> = _hasUnsavedChanges.asStateFlow()

    private val _currentTool = MutableStateFlow<DrawingTool?>(null) // Null si no hay herramienta seleccionada
    val currentTool: StateFlow<DrawingTool?> = _currentTool.asStateFlow()

    private val _lineStartPoint = MutableStateFlow<Offset?>(null)
    val lineStartPoint: StateFlow<Offset?> = _lineStartPoint.asStateFlow() // Para la preview en Canvas

    // Usaremos _currentLineEndPoint para la preview en vivo de la línea
    private val _currentLineEndPoint = MutableStateFlow<Offset?>(null)
    val currentLineEndPoint: StateFlow<Offset?> = _currentLineEndPoint.asStateFlow() // Para la preview

    private val _requestedOrientationLock = MutableStateFlow(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) // Inicialmente no especificado (desbloqueado)
    val requestedOrientationLock: StateFlow<Int> = _requestedOrientationLock.asStateFlow()

    private var isEditorInitializedForCurrentData = false

    // --- Servicios de Google API ---
    private var googleAccountCredential: GoogleAccountCredential? = null
    private var sheetsService: Sheets? = null
    private var driveService: Drive? = null // Para Google Drive

    private val idCarpetaFotografiasCache = mutableMapOf<String, String>() // Clave: idExpedienteDrive, Valor: idCarpeta0Fotografias
    private val idCarpetasFechaCache = mutableMapOf<String, String>()

    // --- Inicialización y Autenticación ---
    fun initializeGoogleServices(context: Context, accountId: String) {
        Log.d("GOOGLE_SERVICES", "Inicializando GoogleAccountCredential y servicios para cuenta: $accountId")
        if (googleAccountCredential != null && googleAccountCredential?.selectedAccount?.name == accountId && driveService != null && sheetsService != null) {
            Log.d("GOOGLE_SERVICES", "Servicios ya inicializados para esta cuenta.")
            // Si los servicios ya están listos, procedemos a cargar expedientes como acción post-login
            viewModelScope.launch { cargarExpedientes() }
            return
        }

        try {
            this.googleAccountCredential = GoogleAccountCredential.usingOAuth2(
                context,
                listOf(SheetsScopes.SPREADSHEETS_READONLY, DriveScopes.DRIVE)
            ).apply {
                // 'this' aquí dentro se refiere a la instancia de GoogleAccountCredential que se está creando
                // y 'selectedAccount' es una propiedad de esa instancia.
                this.selectedAccount = Account(accountId, "com.google")
            }

            val httpRequestInitializer = googleAccountCredential?.let { credential ->
                HttpRequestInitializer { request ->
                    credential.initialize(request) // Aplica la credencial
                    request.connectTimeout = 60 * 1000 // 60 segundos para conectar (en milisegundos)
                    request.readTimeout = 120 * 1000    // 120 segundos para leer/subir (en milisegundos)
                    Log.d("HTTP_CONFIG", "Timeouts configurados: Connect=${request.connectTimeout}, Read=${request.readTimeout}")
                }
            }

            if (httpRequestInitializer == null) {
                Log.e("GOOGLE_SERVICES", "Falló la creación de httpRequestInitializer, credencial nula.")
                setErrorMessage("Error de autenticación al configurar servicios.")
                // Limpiar servicios y salir
                sheetsService = null
                driveService = null
                googleAccountCredential = null // Podríamos limpiar la credencial si falla su uso
                return@initializeGoogleServices // O return desde la corrutina si está dentro de una
            }

            sheetsService = Sheets.Builder(
                NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                httpRequestInitializer
            ).setApplicationName("Photo Sketch 3").build()
            Log.d("GOOGLE_SERVICES", "Servicio de Sheets inicializado.")

            driveService = Drive.Builder(
                NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                httpRequestInitializer
            ).setApplicationName("Photo Sketch 3").build()
            Log.d("GOOGLE_SERVICES", "Servicio de Drive inicializado.")

            viewModelScope.launch { cargarExpedientes() } // Carga expedientes después de inicializar
            Log.d("GOOGLE_SERVICES", "Servicio de Drive inicializado.")
            testDriveConnection() // Test de conexión con Drive

        } catch (e: Exception) {
            Log.e("GOOGLE_SERVICES", "Error inicializando Google services", e)
            setErrorMessage("Error al conectar con Google: ${e.message}")
            googleAccountCredential = null
            sheetsService = null
            driveService = null
        }
    }

    suspend fun cargarExpedientes() {
        if (sheetsService == null || googleAccountCredential?.selectedAccount == null) {
            Log.w("SHEETS_API", "Servicio de Sheets o cuenta no inicializados para cargar expedientes.")
            _listaCompletaExpedientes.value = emptyList()
            return
        }
        val accountIdForLog = googleAccountCredential!!.selectedAccount!!.name
        Log.d("SHEETS_API", "Iniciando carga de expedientes (ambas hojas) para cuenta: $accountIdForLog")
        clearErrorMessage()
        _listaCompletaExpedientes.value = emptyList()
        _consentIntent.value = null

        // Ejecutamos en IO porque es una llamada de red
        withContext(Dispatchers.IO) {
            val listaCombinada = mutableListOf<Expediente>()
            val listaErrores = mutableListOf<String>()
            var necesitaConsentimiento = false

            // Función interna para leer una hoja
            suspend fun leerHoja(sheetName: String, range: String) {
                try {
                    Log.d("SHEETS_API", "Realizando llamada para: $sheetName!$range")
                    val response = sheetsService!!.spreadsheets().values().get(SPREADSHEET_ID, "$sheetName!$range").execute()
                    val values = response.getValues()
                    if (values != null && values.isNotEmpty()) {
                        Log.d("SHEETS_API", "Datos recibidos de '$sheetName': ${values.size} filas.")
                        values.mapNotNull { mapRowToExpediente(it) }.also { listaCombinada.addAll(it) }
                    } else {
                        Log.d("SHEETS_API", "No se encontraron datos en '$sheetName'.")
                    }
                } catch (e: UserRecoverableAuthIOException) {
                    Log.w("SHEETS_API", "Se necesita consentimiento (leyendo $sheetName)", e)
                    _consentIntent.value = e.intent
                    necesitaConsentimiento = true
                } catch (e: Exception) {
                    Log.e("SHEETS_API", "Error al leer '$sheetName'", e)
                    listaErrores.add("Error en '$sheetName': ${e.message}")
                }
            }

            // Leer ambas hojas
            leerHoja(SHEET_NAME_NO_ACEPTADOS, COLUMNS_TO_READ)
            if (necesitaConsentimiento) { // Si la primera hoja pidió consentimiento, no seguir
                withContext(Dispatchers.Main) { clearErrorMessage() }
                return@withContext
            }
            leerHoja(SHEET_NAME_ACEPTADOS, COLUMNS_TO_READ)
            if (necesitaConsentimiento) { // Si la segunda hoja pidió consentimiento
                withContext(Dispatchers.Main) { clearErrorMessage() }
                return@withContext
            }

            // Actualizar Estados Finales en Hilo Principal
            withContext(Dispatchers.Main) {
                _listaCompletaExpedientes.value = listaCombinada
                if (listaErrores.isNotEmpty()) {
                    setErrorMessage(listaErrores.joinToString("\n"))
                } else {
                    clearErrorMessage()
                }
                Log.d("SHEETS_API", "Carga finalizada. Total expedientes: ${listaCombinada.size}. Errores: ${listaErrores.size}")
            }
        }
    }

    // Esta función ahora es privada y suspend, devuelve la lista
    private suspend fun loadGalleryPhotosInternal(context: Context, idCarpetaDrive: String?): List<Uri> = withContext(Dispatchers.IO) {
        if (idCarpetaDrive.isNullOrBlank()) {
            Log.w("GALLERY_VM", "ID Carpeta Drive nulo/vacío, devolviendo lista vacía.")
            return@withContext emptyList<Uri>() // Devuelve lista vacía desde withContext
        }
        Log.d("GALLERY_VM", "Buscando fotos locales para: $idCarpetaDrive")

        // Declara photoUris aquí, directamente en el scope de withContext
        val photoUris = mutableListOf<Uri>()

        try {
            val baseDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            // Sanitizamos el nombre para que sea un nombre de carpeta válido
            val expedienteDirName = idCarpetaDrive.replace(Regex("[^a-zA-Z0-9.-]"), "_")
            val expedienteDir = File(baseDir, expedienteDirName)

            if (expedienteDir.exists() && expedienteDir.isDirectory) {
                val dateFolders = expedienteDir.listFiles { file -> file.isDirectory } ?: emptyArray()

                if (dateFolders.isNotEmpty()) {
                    dateFolders.sortBy { it.name } // Ordena por fecha (nombre de carpeta)
                    for (dateDir in dateFolders) {
                        val photosInDate = dateDir.listFiles { file ->
                            file.isFile && file.name.endsWith(".jpg", ignoreCase = true) // Mejor usar endsWith
                        } ?: emptyArray()
                        photosInDate.sortBy { it.name } // Ordena por nombre (timestamp)
                        photosInDate.forEach { photoFile ->
                            photoUris.add(photoFile.toUri())
                        }
                    }
                } else {
                    // Si no hay carpetas de fecha, buscar fotos directamente en la carpeta del expediente
                    val photosInExp = expedienteDir.listFiles { file ->
                        file.isFile && file.name.endsWith(".jpg", ignoreCase = true)
                    } ?: emptyArray()
                    photosInExp.sortBy{ it.name }
                    photosInExp.forEach { photoFile ->
                        photoUris.add(photoFile.toUri())
                    }
                    if (photosInExp.isEmpty()) Log.d("GALLERY_VM", "No se encontraron fotos directamente en ${expedienteDir.absolutePath}")
                }
            } else {
                Log.d("GALLERY_VM", "El directorio del expediente no existe: ${expedienteDir.absolutePath}")
            }

            // Ya no actualizamos _galleryPhotos.value aquí. Solo logueamos.
            Log.d("GALLERY_VM", "Búsqueda finalizada para $idCarpetaDrive. Fotos encontradas: ${photoUris.size}")

        } catch (e: Exception) {
            Log.e("GALLERY_VM", "Error al listar fotos locales para $idCarpetaDrive", e)
            // La función que llama a esta puede decidir si mostrar un error al usuario.
            // Aquí simplemente devolvemos una lista vacía para indicar fallo.
            return@withContext emptyList<Uri>() // Devuelve lista vacía en caso de error
        }

        return@withContext photoUris // Devuelve la lista de URIs (puede estar vacía)
    }

    // La función pública que llamábamos antes ahora solo actualiza el estado
    // (la llamaremos desde setupEditorWithPhoto y saveEditedImage)
    fun triggerGalleryLoad(idCarpetaDrive: String?) { // Ya no necesita context si solo lee de Room
        if (idCarpetaDrive.isNullOrBlank()) {
            Log.w("GALLERY_VM", "triggerGalleryLoad: ID Carpeta Drive nulo/vacío.")
            _galleryPhotosInfo.value = emptyList()
            return
        }
        Log.d("GALLERY_VM", "triggerGalleryLoad: Cargando galería desde Room para expediente: $idCarpetaDrive")
        viewModelScope.launch {
            // photoInfoDao.getPhotosForExpedienteFlow devuelve un Flow, lo colectamos
            photoInfoDao.getPhotosForExpedienteFlow(idCarpetaDrive)
                .collect { photosFromDb ->
                    Log.d("GALLERY_VM", "Fotos obtenidas de Room para $idCarpetaDrive: ${photosFromDb.size}")
                    _galleryPhotosInfo.value = photosFromDb
                }
        }
    }

    // --- Funciones para manejar estado de UI ---
    fun setLoggedInUser(user: GoogleIdTokenCredential?) {
        _googleUser.value = user
        _errorMessage.value = null
        if (user == null) {
            Log.d("AUTH_STATE", "setLoggedInUser: user es NULL. Limpiando servicios y datos.") // <-- LOG AQUÍ
            _listaCompletaExpedientes.value = emptyList()
            googleAccountCredential = null
            sheetsService = null
            driveService = null // <-- SE PONE A NULL AQUÍ
            isEditorInitializedForCurrentData = false
            clearDrawingStateInternal() // También limpia estados de dibujo
        } else {
            Log.d("AUTH_STATE", "setLoggedInUser: user NO es NULL.") // <-- LOG AQUÍ
            // La inicialización de servicios se hace explícitamente con initializeGoogleServices
        }
    }

    fun setErrorMessage(message: String?) {
        _errorMessage.value = message
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    // Se llamará desde la UI cuando se complete un trazo
    fun addPath(pathData: PathData) {
        // Creamos una nueva lista añadiendo el nuevo trazo
        val newPaths = _drawnPaths.value + pathData
        _drawnPaths.value = newPaths
        // Al añadir un trazo nuevo, se borra la posibilidad de Rehacer
        redoStack.clear()
        // Actualizamos estado de botones
        _canUndo.value = true
        _canRedo.value = false
        _hasUnsavedChanges.value = true
        Log.d("UNDO_REDO", "Path añadido. Undo: ${_canUndo.value}, Redo: ${_canRedo.value}")
    }

    fun undo() {
        if (_drawnPaths.value.isNotEmpty()) {
            // Cogemos el último trazo de la lista principal
            val lastPath = _drawnPaths.value.last()
            // Lo añadimos a la pila de Rehacer (redoStack)
            // Es importante que redoStack sea una MutableList para que .add() funcione bien
            // Si la definiste como: private val redoStack = mutableListOf<PathData>() está perfecto.
            redoStack.add(lastPath)

            // Creamos una nueva lista sin ese último trazo
            _drawnPaths.value = _drawnPaths.value.dropLast(1)

            // Actualizamos estado de botones de ViewModel
            _canUndo.value = _drawnPaths.value.isNotEmpty()
            _canRedo.value = true // Siempre que deshacemos, podemos rehacer

            // --- ESTA ES LA LÍNEA CLAVE MODIFICADA ---
            // hasUnsavedChanges es true SI AÚN QUEDAN TRAZOS DIBUJADOS en el lienzo principal.
            // Si _drawnPaths está vacío, no hay "cambios sin guardar" visibles.
            _hasUnsavedChanges.value = _drawnPaths.value.isNotEmpty()
            // --- FIN DE LA MODIFICACIÓN ---

            // Actualizamos el Log para incluir el estado de hasUnsavedChanges
            Log.d("UNDO_REDO", "Undo. Paths: ${_drawnPaths.value.size}, Redo Stack: ${redoStack.size}, CanUndo: ${_canUndo.value}, CanRedo: ${_canRedo.value}, hasUnsavedChanges: ${_hasUnsavedChanges.value}")
        }
    }

    fun redo() {
        // Usamos toMutableList() para asegurar que tenemos una lista mutable con los métodos correctos
        val currentRedoStack = redoStack.toMutableList()
        if (redoStack.isNotEmpty()) {
            // Cogemos el último trazo de la pila de Rehacer
            // Quitamos el último elemento usando removeAt y el último índice
            val pathToRestore = currentRedoStack.removeAt(currentRedoStack.lastIndex)
            // Actualizamos la pila original (esto podría ser más eficiente, pero es claro)
            redoStack.clear()
            redoStack.addAll(currentRedoStack)
            // Lo añadimos de nuevo a la lista principal
            _drawnPaths.value = _drawnPaths.value + pathToRestore
            // Actualizamos estado de botones
            _canUndo.value = true
            _canRedo.value = redoStack.isNotEmpty()
            _hasUnsavedChanges.value = true
            Log.d("UNDO_REDO", "Redo. Paths: ${_drawnPaths.value.size}, Redo Stack: ${redoStack.size}, CanUndo: ${_canUndo.value}, CanRedo: ${_canRedo.value}")
        }
    }

    fun startDrawing(offset: Offset) {
        when (_currentTool.value) {
            DrawingTool.PENCIL -> {
                _currentPoints.value = listOf(offset)
            }
            DrawingTool.LINE -> {
                _lineStartPoint.value = offset
                _currentLineEndPoint.value = offset // El fin es igual al inicio al empezar
            }
            null -> Log.d("DRAWING_VM", "startDrawing: Ninguna herramienta seleccionada")
        }
        // Podríamos resetear redoStack aquí si una nueva acción de dibujo invalida el redo
        // redoStack.clear()
        // _canRedo.value = false
    }

    fun updateDrawingInProgress(offset: Offset) {
        when (_currentTool.value) {
            DrawingTool.PENCIL -> {
                _currentPoints.value = _currentPoints.value + offset
            }
            DrawingTool.LINE -> {
                _currentLineEndPoint.value = offset // Solo actualizamos el punto final temporal
            }
            null -> Log.d("DRAWING_VM", "updateDrawingInProgress: Ninguna herramienta seleccionada")
        }
    }

    fun finishCurrentPath() {
        when (_currentTool.value) {
            DrawingTool.PENCIL -> {
                if (_currentPoints.value.size > 1) {
                    addPath(PathData(points = _currentPoints.value.toList(), properties = _currentPathProperties.value))
                }
                _currentPoints.value = emptyList()
            }
            DrawingTool.LINE -> {
                val start = _lineStartPoint.value
                val end = _currentLineEndPoint.value // El punto final definitivo
                if (start != null && end != null && start != end) { // Solo si es una línea real
                    // Para una línea recta, la lista de puntos solo necesita el inicio y el fin
                    addPath(PathData(points = listOf(start, end), properties = _currentPathProperties.value))
                }
                _lineStartPoint.value = null // Limpiamos para la siguiente línea
                _currentLineEndPoint.value = null
            }
            null -> Log.d("DRAWING_VM", "finishCurrentPath: Ninguna herramienta seleccionada")
        }
        // TODO: Marcar que hay cambios sin guardar ya se hace en addPath
    }

    // Modifica clearDrawingState para limpiar también estos nuevos estados
    fun clearDrawingStateInternal() {
        _drawnPaths.value = emptyList()
        undoStack.clear()
        redoStack.clear()
        _currentPoints.value = emptyList() // <-- Añadir limpieza
        _canUndo.value = false
        _canRedo.value = false
        _hasUnsavedChanges.value = false
        _requestedOrientationLock.value = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        Log.d("UNDO_REDO", "Estado de dibujo interno limpiado. Orientación desbloqueada.")
    }

    fun selectTool(tool: DrawingTool?) {
        if (tool == null) { // Intentando deseleccionar una herramienta (volver a modo vista)
            if (!_hasUnsavedChanges.value) { // Solo se permite si no hay cambios sin guardar
                _currentTool.value = null
                setEditingMode(false) // Esto también limpiará la herramienta si es necesario
                Log.d("EDITOR_TOOL", "Herramienta deseleccionada. Modo Vista.")
            } else {
                Log.d("EDITOR_TOOL", "No se puede deseleccionar herramienta, hay cambios sin guardar.")
                // Opcional: Podrías usar _errorMessage para notificar a la UI
                // _errorMessage.value = "Guarda o descarta cambios para salir del modo edición."
            }
        } else { // Seleccionando una herramienta
            _currentTool.value = tool
            setEditingMode(true) // Entrar (o permanecer) en modo edición
            Log.d("EDITOR_TOOL", "Herramienta seleccionada: $tool. Modo Edición.")
            _lineStartPoint.value = null
            _currentLineEndPoint.value = null
            _currentPoints.value = emptyList()
            // --- NUEVO: Bloquear Orientación ---
            val dims = currentPhotoOriginalDimensions.value // Obtenemos las dimensiones actuales
            Log.d("ORIENTATION_CHECK", "Dimensiones originales leídas: Ancho=${dims?.width}, Alto=${dims?.height}")
            if (dims != null && dims.width > 0 && dims.height > 0) {
                // Si la altura es mayor o igual al ancho -> Bloquear en Vertical (Portrait)
                // Si el ancho es mayor -> Bloquear en Horizontal (Landscape)
                val orientation = if (dims.height >= dims.width) {
                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT // O SCREEN_ORIENTATION_PORTRAIT si quieres fijo
                } else {
                    ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE // O SCREEN_ORIENTATION_LANDSCAPE
                }
                _requestedOrientationLock.value = orientation
                Log.d("ORIENTATION_LOCK", "Bloqueo solicitado: ${if(orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) "PORTRAIT" else "LANDSCAPE"}")
            } else {
                // Si no tenemos dimensiones, no bloqueamos (o bloqueamos a un default?)
                _requestedOrientationLock.value = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                Log.w("ORIENTATION_LOCK", "No hay dimensiones para determinar orientación, desbloqueando.")
            }
            // --- FIN Bloqueo ---
        }
    }

    // Se llama desde la UI al entrar/cambiar de foto en el editor
    fun prepareEditor(newPhotoUriString: String?) {
        // Comparamos la nueva URI con la que teníamos guardada
        if (currentEditingUri != newPhotoUriString) {
            Log.d("EDITOR_LIFECYCLE", "URI cambiada o es la primera vez ($currentEditingUri -> $newPhotoUriString). Limpiando estado.")
            // Si son diferentes (o era null), limpiamos el lienzo
            clearDrawingStateInternal()
            // Actualizamos la URI que estamos editando ahora
            currentEditingUri = newPhotoUriString
        } else {
            // Si es la misma URI (ej. por rotación), NO limpiamos nada
            Log.d("EDITOR_LIFECYCLE", "Misma URI ($newPhotoUriString), NO se limpia estado.")
        }
    }

    // Esta función se llama AL ENTRAR al EditorScreen
    fun initializeEditorFor(context: Context, initialPhotoUriString: String?, targetIdCarpetaDrive: String?) {
        Log.d("EDITOR_VM", "initializeEditorFor - URI Inicial: $initialPhotoUriString, Carpeta: $targetIdCarpetaDrive")

        if (isEditorInitializedForCurrentData && _currentPhotoUriForEditor.value?.toString() == initialPhotoUriString) {
            Log.d("EDITOR_VM_SETUP", "Editor ya inicializado para esta URI ($initialPhotoUriString) y datos. No se resetea por rotación.")
            // ... (código para cargar dimensiones si faltan, etc.)
            return
        }
        // ... (comprobación de targetIdCarpetaDrive nulo) ...

        viewModelScope.launch {
            // 1. OBTENER LA LISTA DE PhotoInfo DESDE ROOM PARA ESTE EXPEDIENTE
            // Esto lo hacemos llamando al DAO. triggerGalleryLoad ya hace esto y actualiza _galleryPhotosInfo
            // Así que, si GalleryScreen lo llamó, _galleryPhotosInfo ya está bien.
            // PERO, si venimos de CameraScreen, puede que no.
            // La forma más segura es que initializeEditorFor SIEMPRE refresque _galleryPhotosInfo
            // o que triggerGalleryLoad sea la única forma de poblarlo y se llame antes.

            // ---- PROPUESTA MÁS SEGURA Y CONSISTENTE ----
            // Hacemos que initializeEditorFor también refresque la galería desde Room
            photoInfoDao.getPhotosForExpedienteFlow(targetIdCarpetaDrive!!) // Usamos !! porque ya comprobamos isNullOrBlank
                .catch { e ->
                    Log.e("EDITOR_VM", "Error colectando fotos de Room en initializeEditorFor", e)
                    _galleryPhotosInfo.value = emptyList()
                    setErrorMessage("Error al cargar galería para editor: ${e.message}")
                }
                .collect { photosFromDb ->
                    Log.d("EDITOR_VM", "initializeEditorFor: Galería de Room cargada para $targetIdCarpetaDrive: ${photosFromDb.size}")
                    _galleryPhotosInfo.value = photosFromDb // Actualiza el StateFlow principal

                    // Ahora que _galleryPhotosInfo está actualizada, continuamos con la lógica que ya tenías:
                    val initialUriToFind = initialPhotoUriString?.toUri()
                    var targetIndex = 0
                    var targetUriForEditor: Uri? = null // Renombrada para claridad

                    if (photosFromDb.isNotEmpty()) {
                        targetIndex = photosFromDb.indexOfFirst { it.localUri.toUri() == initialUriToFind }.takeIf { it != -1 } ?: 0
                        targetUriForEditor = photosFromDb.getOrNull(targetIndex)?.localUri?.toUri()
                    } else if (initialUriToFind != null) {
                        // Caso: Navegamos con una URI (ej. de cámara) pero la BD está vacía para este expediente
                        // Esto podría pasar si registrarNuevaFotoLocal aún no ha terminado o falló.
                        // Por seguridad, creamos una lista temporal solo con esta foto para el Pager.
                        Log.w("EDITOR_VM", "Galería de Room vacía, usando URI inicial $initialUriToFind provisionalmente.")
                        val tempPhotoInfo = PhotoInfo(localUri = initialUriToFind.toString(), idExpedienteDrive = targetIdCarpetaDrive, fileName = "temp", dateFolderName = "temp")
                        _galleryPhotosInfo.value = listOf(tempPhotoInfo) // Sobrescribe la galería vacía (solo para el Pager)
                        targetUriForEditor = initialUriToFind
                        targetIndex = 0
                    }

                    Log.d("EDITOR_VM", "initializeEditorFor - Índice objetivo: $targetIndex, URI objetivo para editor: $targetUriForEditor")

                    _currentPhotoInGalleryIndex.value = targetIndex
                    _currentPhotoUriForEditor.value = targetUriForEditor
                    loadPhotoDimensions(context, targetUriForEditor)
                    clearDrawingStateInternal()
                    setEditingMode(false)
                    isEditorInitializedForCurrentData = true
                    Log.d("EDITOR_VM", "initializeEditorFor - Estado final: Índice $targetIndex, URI $targetUriForEditor, Init: $isEditorInitializedForCurrentData")
                }
            // ---- FIN PROPUESTA ----
        }
    }

    // Esta función se llama cuando el PAGER (UI) cambia de página por swipe del USUARIO
    suspend fun userSwipedToPhotoAtIndex(newIndex: Int, context: Context) {
        val galleryInfoList = _galleryPhotosInfo.value // Esta es List<PhotoInfo>
        if (newIndex >= 0 && newIndex < galleryInfoList.size) {
            val photoInfo = galleryInfoList[newIndex]
            val newUri = photoInfo.localUri.toUri() // Obtenemos la Uri del PhotoInfo

            if (newUri != _currentPhotoUriForEditor.value) {
                Log.d("EDITOR_VM", "Usuario hizo swipe a índice $newIndex, URI: $newUri")
                _currentPhotoInGalleryIndex.value = newIndex
                _currentPhotoUriForEditor.value = newUri // Actualizamos con la nueva Uri
                loadPhotoDimensions(context, newUri)
                clearDrawingStateInternal()
                setEditingMode(false)
            }
        } else {
            Log.w("EDITOR_VM", "userSwipedToPhotoAtIndex - Índice $newIndex fuera de rango para galería de tamaño ${galleryInfoList.size}")
        }
    }

    fun saveEditedImage(
        context: Context,
        originalPhotoUriString: String?, // La URI de la foto original que se está editando
        originalFileName: String?,
        idCarpetaDrive: String?,
        originalPhotoIndex: Int,
        drawnPathsToSave: List<PathData>,
        currentProperties: PathProperties, // Propiedades del trazo actual (por si se estaba dibujando)
        currentPointsToSave: List<Offset>, // Puntos del trazo actual (por si se estaba dibujando)
        originalImageSize: IntSize?,
        canvasDrawSize: IntSize?
    ) { // Cambiamos el tipo de retorno a Unit, la URI se expondrá por StateFlow
        if (originalPhotoUriString == null || idCarpetaDrive.isNullOrBlank() || originalImageSize == null || canvasDrawSize == null) {
            Log.e("EDITOR_SAVE", "Faltan datos para guardar: URI, ID Carpeta, o dimensiones.")
            setErrorMessage("Error: Faltan datos para guardar.")
            return // Salimos de la función
        }
        // Solo guardamos si hay dibujos hechos o un trazo en curso con más de un punto
        if (drawnPathsToSave.isEmpty() && currentPointsToSave.size <= 1) {
            Log.i("EDITOR_SAVE", "No hay nada que guardar (sin trazos significativos).")
            setErrorMessage("No hay dibujos para guardar.") // Informamos al usuario
            return // Salimos
        }

        Log.d("EDITOR_SAVE", "Iniciando proceso de guardado...")
        viewModelScope.launch(Dispatchers.IO) {
            var newFileUri: Uri? = null // Variable local para la URI
            try {
                // 1. Cargar Bitmap Original y Corregir Orientación EXIF (Tu código - SIN CAMBIOS)
                val originalBitmapNotRotated: Bitmap? = context.contentResolver.openInputStream(
                    originalPhotoUriString!!.toUri() // Asumimos originalPhotoUriString no es null aquí por el if previo
                )?.use {
                    BitmapFactory.decodeStream(it)
                }
                if (originalBitmapNotRotated == null) {
                    Log.e("EDITOR_SAVE", "No se pudo cargar el bitmap original (not rotated).")
                    setErrorMessage("Error cargando imagen original.")
                    return@launch
                }
                var finalOriginalBitmap: Bitmap? = null
                context.contentResolver.openInputStream(originalPhotoUriString.toUri())?.use { inputStream ->
                    val exif = ExifInterface(inputStream)
                    val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                    val matrix = Matrix()
                    when (orientation) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                    }
                    finalOriginalBitmap = Bitmap.createBitmap(
                        originalBitmapNotRotated, 0, 0,
                        originalBitmapNotRotated.width, originalBitmapNotRotated.height,
                        matrix, true
                    )
                }
                if (finalOriginalBitmap == null) {
                    Log.e("EDITOR_SAVE", "Fallo al crear 'finalOriginalBitmap' con corrección EXIF.")
                    setErrorMessage("Error al procesar orientación de imagen.")
                    return@launch
                }

                // 2. Crear Bitmap Mutable para la salida (Tu código - SIN CAMBIOS)
                val outputBitmap =
                    createBitmap(finalOriginalBitmap!!.width, finalOriginalBitmap!!.height) // KTX
                val androidBitmapCanvas = AndroidCanvas(outputBitmap)

                // 3. Dibujar la imagen original (Tu código - SIN CAMBIOS)
                androidBitmapCanvas.drawBitmap(finalOriginalBitmap!!, 0f, 0f, null)

                // 4. Calcular factores de escala y offsets (Tu código - SIN CAMBIOS)
                val canvasWidth = canvasDrawSize!!.width.toFloat() // Asumimos no nulos por el if previo
                val canvasHeight = canvasDrawSize.height.toFloat()
                val imgOrigWidth = finalOriginalBitmap!!.width.toFloat()
                val imgOrigHeight = finalOriginalBitmap!!.height.toFloat()
                val scaleRatio = minOf(canvasWidth / imgOrigWidth, canvasHeight / imgOrigHeight)
                val scaledImageWidth = imgOrigWidth * scaleRatio
                val scaledImageHeight = imgOrigHeight * scaleRatio
                val imageOffsetX = (canvasWidth - scaledImageWidth) / 2f
                val imageOffsetY = (canvasHeight - scaledImageHeight) / 2f
                // ... (resto de tu lógica transformPoint, etc. SIN CAMBIOS)
                fun transformPoint(composeCanvasPoint: Offset): Offset {
                    val pointRelativeToScaledImageX = composeCanvasPoint.x - imageOffsetX
                    val pointRelativeToScaledImageY = composeCanvasPoint.y - imageOffsetY
                    return Offset(
                        x = pointRelativeToScaledImageX / scaleRatio,
                        y = pointRelativeToScaledImageY / scaleRatio
                    )
                }


                // 5. Dibujar los trazos (Tu código - SIN CAMBIOS)
                val allPathsToDraw = drawnPathsToSave.toMutableList()
                if (currentPointsToSave.size > 1) {
                    allPathsToDraw.add(PathData(points = currentPointsToSave.toList(), properties = currentProperties))
                }
                allPathsToDraw.forEach { pathData ->
                    if (pathData.points.size > 1) {
                        val androidPath = AndroidPath()
                        val firstPoint = transformPoint(pathData.points.first())
                        androidPath.moveTo(firstPoint.x, firstPoint.y)
                        pathData.points.drop(1).forEach {
                            val transformedPoint = transformPoint(it)
                            androidPath.lineTo(transformedPoint.x, transformedPoint.y)
                        }
                        val paint = AndroidPaint().apply {
                            color = pathData.properties.color.toArgb()
                            strokeWidth = pathData.properties.strokeWidth / scaleRatio
                            style = AndroidPaint.Style.STROKE
                            strokeCap = when(pathData.properties.strokeCap) {
                                StrokeCap.Round -> AndroidPaint.Cap.ROUND
                                StrokeCap.Square -> AndroidPaint.Cap.SQUARE
                                else -> AndroidPaint.Cap.BUTT
                            }
                            strokeJoin = when(pathData.properties.strokeJoin) {
                                StrokeJoin.Round -> AndroidPaint.Join.ROUND
                                StrokeJoin.Miter -> AndroidPaint.Join.MITER
                                else -> AndroidPaint.Join.BEVEL
                            }
                            isAntiAlias = true
                        }
                        androidBitmapCanvas.drawPath(androidPath, paint)
                    }
                }

                // 6. Guardar el outputBitmap en un archivo nuevo (Tu código de nomenclatura - SIN CAMBIOS)
                val baseDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                val expedienteDirNameSanitized = idCarpetaDrive!!.replace(Regex("[^a-zA-Z0-9.-]"), "_")
                val dateFolderName = SimpleDateFormat("yyyyMMdd", Locale.US).format(System.currentTimeMillis())
                val expedienteDir = File(baseDir, expedienteDirNameSanitized)
                val dateDir = File(expedienteDir, dateFolderName)
                if (!dateDir.exists()) dateDir.mkdirs()

                val baseName = originalFileName!!.removeSuffix(".jpg") // originalFileName lo recibe saveEditedImage
                var editCount = 1
                var newPhotoFileName: String
                var potentialFile: File
                do {
                    newPhotoFileName = "${baseName}_edited_$editCount.jpg"
                    potentialFile = File(dateDir, newPhotoFileName)
                    editCount++
                } while (potentialFile.exists())
                val newPhotoFile = File(dateDir, newPhotoFileName)

                FileOutputStream(newPhotoFile).use { out ->
                    outputBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }
                newFileUri = newPhotoFile.toUri()
                Log.d("EDITOR_SAVE", "Imagen editada guardada localmente en: $newFileUri")

                // --- INICIO LÓGICA POST-GUARDADO (ADAPTADA) ---

                // 7. Registrar la nueva foto editada en Room (Tu llamada - SIN CAMBIOS)
                registrarNuevaFotoLocal(
                    localUri = newFileUri.toString(),
                    fileName = newPhotoFile.name,
                    idExpedienteDrive = idCarpetaDrive,
                    dateFolderName = newPhotoFile.parentFile?.name ?: "",
                    isEdited = true
                )
                _lastSavedEditedPhotoUri.value = newFileUri // Para otros observers

                // 8. Obtener la lista MÁS RECIENTE de Room después de registrar la nueva foto.
                //    Usamos .first() para obtener la emisión actual del Flow después del upsert.
                val refreshedGalleryFromRoom = photoInfoDao.getPhotosForExpedienteFlow(idCarpetaDrive).first()
                val mutableGallery = refreshedGalleryFromRoom.toMutableList()

                // 9. Encontrar el PhotoInfo de la foto recién guardada
                val newSavedPhotoInfo = mutableGallery.find { it.localUri == newFileUri.toString() }

                if (newSavedPhotoInfo == null) {
                    Log.e("EDITOR_SAVE", "¡ERROR CRÍTICO! La foto recién guardada y registrada en Room no se encontró en el Flow de Room.")
                    // Fallback: Intentar inicializar con la URI que tenemos, aunque el índice será incierto.
                    initializeEditorFor(context, newFileUri.toString(), idCarpetaDrive)
                    return@launch
                }

                // 10. Reordenar: Quitar la foto editada de donde esté y reinsertarla después de la original.
                //     'originalPhotoIndex' es el índice de la foto original en la lista ANTES de añadir la editada.
                mutableGallery.remove(newSavedPhotoInfo) // La quitamos para reinsertarla
                // El índice de inserción es después del original, cuidado con los límites.
                val insertionIndex = (originalPhotoIndex + 1).coerceIn(0, mutableGallery.size)
                mutableGallery.add(insertionIndex, newSavedPhotoInfo)
                Log.d("EDITOR_SAVE", "Foto editada reinsertada en la galería en el índice: $insertionIndex")

                // 11. Actualizar el StateFlow de la galería con la lista REORDENADA
                _galleryPhotosInfo.value = mutableGallery.toList()

                // 12. Establecer la foto ACTUALMENTE VISIBLE a la foto editada en su nueva posición
                _currentPhotoUriForEditor.value = newFileUri // La URI de la foto editada
                _currentPhotoInGalleryIndex.value = insertionIndex // El índice donde la hemos puesto

                // 13. Cargar dimensiones para la nueva foto activa y resetear estados de edición
                loadPhotoDimensions(context, newFileUri)
                setEditingMode(false)
                clearDrawingStateInternal() // Limpia trazos, undo/redo, hasUnsavedChanges, etc.

                Log.d("EDITOR_SAVE", "Guardado completo. VM actualizado. Foto activa: $newFileUri en índice: $insertionIndex.")
                // --- FIN LÓGICA POST-GUARDADO (ADAPTADA) ---

            } catch (e: Exception) {
                Log.e("EDITOR_SAVE", "Error al guardar imagen editada", e)
                setErrorMessage("Error al guardar: ${e.message}")
                _lastSavedEditedPhotoUri.value = null
            }
        } // Fin viewModelScope.launch
    }

    // Se llama cuando entramos al EditorScreen con una URI específica
    fun setupEditorWithPhoto(context: Context, initialPhotoUriString: String?, targetIdCarpetaDrive: String?) {
        Log.d("EDITOR_VM_SETUP", "Setup con URI: $initialPhotoUriString, Carpeta: $targetIdCarpetaDrive")
        if (targetIdCarpetaDrive.isNullOrBlank()) {
            setErrorMessage("ID de carpeta no válido para el editor.")
            _galleryPhotos.value = emptyList()
            _currentPhotoUriForEditor.value = null
            _currentPhotoInGalleryIndex.value = 0 // Resetea índice
            return
        }

        viewModelScope.launch {
            val uris = loadGalleryPhotosInternal(context, targetIdCarpetaDrive)
            _galleryPhotos.value = uris // Actualiza la galería primero

            var targetUri = initialPhotoUriString?.toUri()
            var targetIndex = uris.indexOf(targetUri).takeIf { it != -1 }

            if (targetIndex == null && uris.isNotEmpty()) { // Si la URI inicial no está (ej. recién guardada y no en lista previa) o es la primera vez
                targetIndex = 0 // Por defecto la primera de la galería del expediente
                targetUri = uris.getOrNull(targetIndex)
            } else if (uris.isEmpty() && targetUri != null) { // Galería vacía pero tenemos una URI (desde cámara)
                _galleryPhotos.value = listOf(targetUri) // La añadimos temporalmente
                targetIndex = 0
            }


            _currentPhotoInGalleryIndex.value = targetIndex ?: 0
            _currentPhotoUriForEditor.value = targetUri
            loadPhotoDimensions(context, targetUri)
            clearDrawingStateInternal()
            setEditingMode(false)
            Log.d("EDITOR_VM_SETUP", "Setup completo. Foto: $targetUri, Índice: ${targetIndex ?: 0}, Total en galería: ${uris.size}")
        }
    }

    fun setEditingMode(isEditing: Boolean) {
        _isEditingMode.value = isEditing
        Log.d("EDITOR_VM", "Modo Edición: $isEditing")
        if (!isEditing) {
            _currentTool.value = null
            _requestedOrientationLock.value = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            Log.d("ORIENTATION_LOCK", "Modo Edición OFF. Desbloqueando orientación.")
            // Podríamos resetear la herramienta seleccionada aquí
        }
        Log.d("EDITOR_VM", "Modo Edición: $isEditing")
    }

    private suspend fun loadPhotoDimensions(context: Context, photoUri: Uri?): IntSize? = withContext(Dispatchers.IO) {
        if (photoUri == null) {
            Log.w("EDITOR_VM_DIMS", "loadPhotoDimensions - URI nula.")
            _currentPhotoOriginalDimensions.value = null // Asegura limpiar estado si URI es null
            return@withContext null
        }
        Log.d("EDITOR_VM_DIMS", "loadPhotoDimensions - INTENTANDO cargar para URI: $photoUri")

        var width = -1
        var height = -1
        var orientation = ExifInterface.ORIENTATION_NORMAL

        try {
            // Intentamos leer EXIF primero. Necesita su propio InputStream porque decodeStream consume el otro.
            context.contentResolver.openInputStream(photoUri)?.use { exifInputStream ->
                val exifInterface = ExifInterface(exifInputStream)
                orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                Log.d("EDITOR_VM_DIMS", "EXIF Orientation tag leído: $orientation")
            } ?: Log.w("EDITOR_VM_DIMS", "InputStream nulo para leer EXIF.")

            // Leemos Dimensiones con otro InputStream
            context.contentResolver.openInputStream(photoUri)?.use { dimInputStream ->
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeStream(dimInputStream, null, options)
                width = options.outWidth
                height = options.outHeight
                Log.d("EDITOR_VM_DIMS", "Dimensiones leídas del archivo: ${width}x${height}")
            } ?: run { // Si el segundo InputStream falla
                Log.e("EDITOR_VM_DIMS", "InputStream nulo para leer dimensiones.")
                _currentPhotoOriginalDimensions.value = null
                return@withContext null
            }

            if (width <= 0 || height <= 0) {
                Log.w("EDITOR_VM_DIMS", "Dimensiones leídas inválidas: ${width}x${height}")
                _currentPhotoOriginalDimensions.value = null
                return@withContext null
            }

            // --- CORRECCIÓN SEGÚN EXIF ---
            val finalWidth: Int
            val finalHeight: Int
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90,
                ExifInterface.ORIENTATION_ROTATE_270 -> {
                    // Si EXIF dice rotar 90/270, las dimensiones visuales están intercambiadas
                    finalWidth = height // El alto leído es el ancho visual
                    finalHeight = width  // El ancho leído es el alto visual
                    Log.d("EDITOR_VM_DIMS", "EXIF indica rotación 90/270. Dimensiones CORREGIDAS: ${finalWidth}x${finalHeight}")
                }
                else -> {
                    // Para 0, 180 o indefinido, las dimensiones coinciden
                    finalWidth = width
                    finalHeight = height
                    Log.d("EDITOR_VM_DIMS", "EXIF sin rotación 90/270. Dimensiones FINALES: ${finalWidth}x${finalHeight}")
                }
            }
            // --- FIN CORRECCIÓN ---

            val resultSize = IntSize(finalWidth, finalHeight)
            _currentPhotoOriginalDimensions.value = resultSize // Actualizamos StateFlow
            Log.d("EDITOR_VM_DIMS", "loadPhotoDimensions - ÉXITO FINAL para $photoUri: $resultSize")
            return@withContext resultSize

        } catch (e: Exception) {
            Log.e("EDITOR_VM_DIMS", "Error GENERAL cargando dimensiones para $photoUri", e)
            _currentPhotoOriginalDimensions.value = null
            setErrorMessage("Error al leer dimensiones de imagen.") // Ya lo teníamos
            return@withContext null
        }
    }

    // TODO: Añadir funciones para cambiar _currentPathProperties.value (color, grosor)
    fun changeColor(newColor: Color) { _currentPathProperties.value = _currentPathProperties.value.copy(color = newColor) }
    fun changeStrokeWidth(newWidth: Float) { _currentPathProperties.value = _currentPathProperties.value.copy(strokeWidth = newWidth) }

    fun signOut() {
        // Limpia el usuario y los expedientes
        setLoggedInUser(null)
        clearDrawingStateInternal() // Llama a la interna
        // TODO: Podríamos añadir aquí la llamada a CredentialManager para limpiar estado si fuera necesario
        // credentialManager.clearCredentialState(...) - Necesitaríamos el manager aquí o pasarlo
        Log.d("VIEWMODEL_SIGN_OUT", "Usuario deslogueado y dibujo limpiado")
    }
    // --- Fin Funciones UI State ---

    // Extraemos la lógica para convertir una fila de la hoja a un objeto Expediente
    private fun mapRowToExpediente(row: List<Any>): Expediente? {
        // Asegurarnos que la fila tiene al menos 3 elementos (A, B, C)
        return if (row.size >= 3) {
            Expediente(
                nombre = row.getOrNull(0)?.toString()?.trim() ?: "", // Columna A (Expediente)
                direccion = row.getOrNull(1)?.toString()?.trim() ?: "", // Columna B (Dirección)
                idCarpetaDrive = row.getOrNull(2)?.toString()?.trim() ?: "" // Columna C (id_carpeta_drive)
            )
        } else {
            Log.w("SHEETS_API_MAP", "Fila ignorada por tener menos de 3 columnas: $row")
            null // Ignora filas que no tengan las 3 columnas esperadas
        }
    }

    // Función para que la UI actualice el texto de búsqueda
    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun consentIntentHandled() {
        _consentIntent.value = null
    }

    fun testDriveConnection() {
        Log.d("DRIVE_TEST", "Función testDriveConnection LLAMADA. driveService es null?: ${driveService == null}") // Log inicial
        if (driveService == null) {
            Log.e("DRIVE_TEST", "Servicio de Drive NO inicializado al inicio de testDriveConnection.")
            setErrorMessage("Drive no conectado (test).")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            Log.d("DRIVE_TEST", "Dentro de corrutina de testDriveConnection.") // Log dentro de launch
            try {
                Log.d("DRIVE_TEST", "Intentando obtener info 'About' de Drive...")
                val about = driveService!!.about().get().setFields("user, storageQuota").execute()
                // ... resto de los logs y Toast (asegúrate que el Toast usa el applicationContext bien)
                withContext(Dispatchers.Main) {
                    Log.i("DRIVE_TEST", "Usuario de Drive: ${about.user.displayName}, Email: ${about.user.emailAddress}")
                    // Si tu ViewModel es AndroidViewModel:
                    // Toast.makeText(getApplication<Application>().applicationContext, "Conexión Drive OK: ${about.user.displayName}", Toast.LENGTH_LONG).show()
                    // Si no, necesitarías pasar el Context a esta función para el Toast
                }
            } catch (e: UserRecoverableAuthIOException) {
                Log.w("DRIVE_TEST", "Se necesita consentimiento para Drive (testDriveConnection).", e)
                _consentIntent.value = e.intent
            } catch (e: Exception) {
                Log.e("DRIVE_TEST", "Error en test de conexión con Drive", e)
                withContext(Dispatchers.Main) {
                    setErrorMessage("Error Test Drive: ${e.message}")
                }
            }
        }
    }

    // Dentro de ExpedientesViewModel
    fun registrarNuevaFotoLocal(
        localUri: String,
        fileName: String,
        idExpedienteDrive: String,
        dateFolderName: String,
        isEdited: Boolean = false // Parámetro opcional, por defecto no es editada
    ) {
        viewModelScope.launch(Dispatchers.IO) { // Operación de base de datos en hilo de fondo
            try {
                val photoInfo = PhotoInfo(
                    localUri = localUri,
                    idExpedienteDrive = idExpedienteDrive,
                    fileName = fileName,
                    dateFolderName = dateFolderName,
                    syncStatus = SyncStatus.LOCAL_ONLY, // Nueva foto, pendiente de subir
                    isEdited = isEdited,
                    timestamp = System.currentTimeMillis(), // Timestamp actual
                    driveFileId = null // Aún no tiene ID de Drive
                )
                photoInfoDao.upsertPhoto(photoInfo) // Usamos upsert (inserta o reemplaza)
                Log.d("ROOM_SAVE", "Foto registrada en Room: $localUri")
                // Opcional: Refrescar la galería si está visible y debe mostrar esta nueva foto
                // Esto dependerá de si la galería ya observa el Flow del DAO
                // Por ahora, la galería se refresca con loadGalleryPhotosInternal,
                // que lee del sistema de archivos. Para que Room sea la fuente,
                // loadGalleryPhotosInternal debería leer de Room. ¡Lo haremos después!
            } catch (e: Exception) {
                Log.e("ROOM_SAVE", "Error al registrar foto en Room: $localUri", e)
                // setErrorMessage("Error al guardar datos de foto.") // Opcional
            }
        }
    }

    // Función auxiliar para buscar o crear una carpeta y devolver su ID
    private suspend fun getOrCreateFolderId(
        name: String,
        parentId: String?,
        cache: MutableMap<String, String>,
        isDateFolder: Boolean = false // Nuevo parámetro para saber si buscamos carpeta de fecha
    ): String? = withContext(Dispatchers.IO) {
        if (driveService == null) return@withContext null

        val cacheKey = "${parentId ?: "root"}/$name" // Clave única para la carpeta y su padre
        val cacheKeyContains = "${parentId ?: "root"}/contains_$name"
        if (cache.containsKey(cacheKey)) {
            Log.d("DRIVE_API_CACHE", "Carpeta '$name' con padre '$parentId' encontrada en caché: ${cache[cacheKey]}")
            return@withContext cache[cacheKey]
        }

        if (isDateFolder && cache.containsKey(cacheKeyContains)) {
            Log.d("DRIVE_API_CACHE", "Carpeta '$name' con padre '$parentId' encontrada en caché: ${cache[cacheKeyContains]}")
            return@withContext cache[cacheKeyContains]
        }

        try {
            // 1. Intento de Búsqueda Exacta (para "0 fotografias" y el nombre de fecha estándar)
            var queryExact = "mimeType='application/vnd.google-apps.folder' and name='$name' and trashed=false"
            parentId?.let { queryExact += " and '$it' in parents" }

            Log.d("DRIVE_API", "Buscando (exacto) carpeta: '$name' con parent: $parentId. Query: $queryExact")
            var result: FileList = driveService!!.files().list()
                .setQ(queryExact)
                .setSpaces("drive").setFields("files(id, name)").setSupportsAllDrives(true)
                .execute()

            if (result.files.isNotEmpty()) {
                val folderId = result.files[0].id
                Log.d("DRIVE_API", "Carpeta '$name' (exacta) encontrada con ID: $folderId")
                cache[cacheKey] = folderId // Cacheamos por nombre exacto
                if (isDateFolder) cache[cacheKeyContains] = folderId // También para 'contains' si es de fecha
                return@withContext folderId
            }

            // 2. Si es una carpeta de fecha y no se encontró por nombre exacto, intentamos con "contains"
            if (isDateFolder) {
                var queryContains = "mimeType='application/vnd.google-apps.folder' and name contains '$name' and trashed=false" // '$name' aquí es YYYYMMDD
                parentId?.let { queryContains += " and '$it' in parents" }

                Log.d("DRIVE_API", "Buscando (contains) carpeta de fecha: '$name' con parent: $parentId. Query: $queryContains")
                result = driveService!!.files().list()
                    .setQ(queryContains)
                    .setSpaces("drive").setFields("files(id, name)").setSupportsAllDrives(true)
                    .execute()

                if (result.files.isNotEmpty()) {
                    // Si encuentra UNA SOLA carpeta que contiene la fecha, la usamos.
                    // Si encuentra varias, podríamos tener un problema de ambigüedad.
                    // Por ahora, usamos la primera si solo hay una con ese prefijo de fecha.
                    // Una lógica más robusta podría ser buscar la que tenga el nombre más corto o la más antigua.
                    // O si hay varias, crear la estándar 'YYYYMMDD'.
                    if (result.files.size == 1) { // Solo si encuentra exactamente UNA
                        val folderId = result.files[0].id
                        Log.d("DRIVE_API", "Carpeta de fecha '$name' (contains, única) encontrada con ID: $folderId (${result.files[0].name})")
                        cache[cacheKeyContains] = folderId // Cacheamos por 'contains'
                        // No cacheamos por nombre exacto porque no es el nombre exacto
                        return@withContext folderId
                    } else {
                        Log.d("DRIVE_API", "Se encontraron ${result.files.size} carpetas con 'contains $name'. Se creará la estándar.")
                        // Si hay 0 o más de 1, procedemos a crear la carpeta con el nombre exacto YYYYMMDD
                    }
                }
            }

            // 3. Si no se encontró de ninguna forma (o es "0 fotografias", o es de fecha y no hubo match único con contains), se crea
            Log.d("DRIVE_API", "Carpeta '$name' (parent: $parentId) no encontrada por búsqueda. Creando con nombre exacto '$name'...")
            val fileMetadata = DriveFile()
            fileMetadata.name = name // Nombre exacto ("0 fotografias" o "YYYYMMDD")
            fileMetadata.mimeType = "application/vnd.google-apps.folder"
            parentId?.let { fileMetadata.parents = listOf(it) }

            val createdFolder = driveService!!.files().create(fileMetadata)
                .setFields("id, name").setSupportsAllDrives(true)
                .execute()
            Log.d("DRIVE_API", "Carpeta '${createdFolder.name}' creada con ID: ${createdFolder.id} (parent: $parentId)")
            cache[cacheKey] = createdFolder.id // Cachear por nombre exacto
            if (isDateFolder) cache[cacheKeyContains] = createdFolder.id // También para 'contains'
            return@withContext createdFolder.id

        } catch (e: UserRecoverableAuthIOException) {
            Log.w("DRIVE_API", "Se necesita consentimiento para buscar/crear carpeta '$name'", e)
            _consentIntent.value = e.intent // Usamos postValue si estamos en hilo IO y _consentIntent es LiveData/MutableStateFlow
            return@withContext null
        } catch (e: GoogleJsonResponseException) { // Captura específica para errores de Google API
            Log.e("DRIVE_API", "Error GoogleJsonResponseException buscando/creando carpeta '$name' (parent: $parentId): ${e.statusCode} - ${e.details?.message}", e)
            setErrorMessage("Error API Drive (${e.statusCode}): ${e.details?.message}")
            return@withContext null
        } catch (e: IOException) {
            Log.e("DRIVE_API", "Error de IO buscando/creando carpeta '$name' (parent: $parentId)", e)
            setErrorMessage("Error de red o Drive: ${e.message}")
            return@withContext null
        } catch (e: Exception) {
            Log.e("DRIVE_API", "Error inesperado buscando/creando carpeta '$name' (parent: $parentId)", e)
            setErrorMessage("Error inesperado en Drive: ${e.message}")
            return@withContext null
        }
    }


    suspend fun uploadPhotoToDrive(
        context: Context,
        photoInfo: PhotoInfo,
        targetDriveFolderId: String // ID de la carpeta de FECHA donde subir
    ): String? = withContext(Dispatchers.IO) {
        if (driveService == null) {
            Log.e("DRIVE_UPLOAD", "DriveService no inicializado.")
            setErrorMessage("Servicio de Drive no disponible.")
            return@withContext null
        }
        if (photoInfo.localUri.isBlank()) {
            Log.e("DRIVE_UPLOAD", "URI local vacía para ${photoInfo.fileName}")
            return@withContext null
        }

        Log.d("DRIVE_UPLOAD", "Iniciando subida a Drive para: ${photoInfo.fileName}")

        try {

            // Paso 4: Preparar metadatos del archivo y contenido
            val fileMetadata = DriveFile()
            fileMetadata.name = photoInfo.fileName
            fileMetadata.parents = listOf(targetDriveFolderId) // Especifica la carpeta padre

            val localFile = File(photoInfo.localUri.toUri().path!!) // Necesitamos el path del archivo
            if (!localFile.exists()) {
                Log.e("DRIVE_UPLOAD", "Archivo local no encontrado: ${localFile.path}")
                setErrorMessage("Error: archivo local no encontrado para subir.")
                return@withContext null
            }
            val mediaContent = FileContent("image/jpeg", localFile)

            // Paso 5: Subir el archivo
            Log.d("DRIVE_UPLOAD", "Subiendo ${photoInfo.fileName} a carpeta Drive ID: $targetDriveFolderId")
            photoInfoDao.updatePhoto(photoInfo.copy(syncStatus = SyncStatus.SYNCING_UP))
            triggerGalleryLoad(photoInfo.idExpedienteDrive) // Para refrescar UI con icono "subiendo"
            val uploadedFile = driveService!!.files().create(fileMetadata, mediaContent)
                .setFields("id, name, webViewLink") // Qué campos queremos de vuelta
                .setSupportsAllDrives(true)
                .execute()

            Log.i("DRIVE_UPLOAD", "Archivo subido con éxito! Nombre: ${uploadedFile.name}, ID: ${uploadedFile.id}, Link: ${uploadedFile.webViewLink}")

            // Actualizar el estado en Room
            photoInfoDao.markAsSynced(photoInfo.localUri, uploadedFile.id)
            // Disparar recarga de galería para que se actualice el icono de estado
            triggerGalleryLoad(photoInfo.idExpedienteDrive)


            return@withContext uploadedFile.id // Devolvemos el ID del archivo en Drive

        } catch (e: UserRecoverableAuthIOException) {
            Log.w("DRIVE_UPLOAD", "Se necesita consentimiento para subir archivo ${photoInfo.fileName}", e)
            photoInfoDao.updatePhoto(photoInfo.copy(syncStatus = SyncStatus.ERROR_UPLOADING))
            triggerGalleryLoad(photoInfo.idExpedienteDrive)
            _consentIntent.value = e.intent // Para re-autenticación
            return@withContext null
        } catch (e: IOException) {
            Log.e("DRIVE_UPLOAD", "Error de IO al subir archivo ${photoInfo.fileName}", e)
            photoInfoDao.updatePhoto(photoInfo.copy(syncStatus = SyncStatus.ERROR_UPLOADING))
            triggerGalleryLoad(photoInfo.idExpedienteDrive)
            setErrorMessage("Error de red o Drive al subir: ${e.message}")
            return@withContext null
        } catch (e: Exception) {
            Log.e("DRIVE_UPLOAD", "Error inesperado al subir archivo ${photoInfo.fileName}", e)
            photoInfoDao.updatePhoto(photoInfo.copy(syncStatus = SyncStatus.ERROR_UPLOADING))
            triggerGalleryLoad(photoInfo.idExpedienteDrive)
            setErrorMessage("Error inesperado en Drive al subir: ${e.message}")
            return@withContext null
        }
    }

    fun subirFotosPendientesDelExpediente(context: Context, idExpedienteDrive: String) {
        // --- AÑADIR ESTE LOG ---
        Log.d("DRIVE_UPLOAD_BATCH", "Función llamada. ¿Está driveService null?: ${driveService == null}")
        // --- FIN LOG ---

        if (driveService == null) { // Añadimos esta comprobación explícita aquí también
            Log.e("DRIVE_UPLOAD_BATCH", "¡DriveService es null ANTES de la corrutina! No se puede subir.")
            setErrorMessage("Error crítico: Servicio de Drive no está listo.")
            return
        }

        viewModelScope.launch {
            Log.d("DRIVE_UPLOAD_BATCH", "Dentro de corrutina. ¿Está driveService null?: ${driveService == null}") // Otro log aquí
            Log.d("DRIVE_UPLOAD_BATCH", "Iniciando subida de pendientes para expediente: $idExpedienteDrive")
            val fotosPendientes = photoInfoDao.getLocalOnlyPhotosForExpediente(idExpedienteDrive)

            if (fotosPendientes.isEmpty()) {
                Log.i("DRIVE_UPLOAD_BATCH", "No hay fotos pendientes de subir para este expediente.")
                withContext(Dispatchers.Main) { // Para mostrar Toast en hilo UI
                    Toast.makeText(context, "No hay fotos nuevas/editadas para subir.", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            var subidasExitosas = 0
            var subidasFallidas = 0
            Toast.makeText(context, "Iniciando subida de ${fotosPendientes.size} foto(s)...", Toast.LENGTH_SHORT).show()

            // 1. Obtener/Crear la carpeta "0 fotografias" UNA SOLA VEZ
            var fotografiasFolderId = idCarpetaFotografiasCache[idExpedienteDrive] // Intenta obtener de la caché del VM
            if (fotografiasFolderId == null) {
                Log.d("DRIVE_UPLOAD_BATCH", "Cache miss para '0 fotografias' del exp: $idExpedienteDrive. Consultando Drive.")
                fotografiasFolderId = getOrCreateFolderId("0 fotografias", idExpedienteDrive, idCarpetaFotografiasCache, isDateFolder = false) // Pasamos la caché de sesión
                if (fotografiasFolderId != null) {
                    idCarpetaFotografiasCache[idExpedienteDrive] = fotografiasFolderId // Guardamos en la caché del VM para futuras subidas a ESTE expediente
                    Log.d("DRIVE_UPLOAD_BATCH", "'0 fotografias' para $idExpedienteDrive obtenida/creada y cacheada en VM con ID: $fotografiasFolderId")
                } else {
                    Log.e("DRIVE_UPLOAD_BATCH", "Fallo crítico al obtener/crear '0 fotografias'. Abortando subida.")
                    setErrorMessage("Error creando carpeta base '0 fotografias' en Drive.")
                    // Actualizar UI con conteo de fallos si es necesario
                    withContext(Dispatchers.Main) { Toast.makeText(context, "Error base Drive. Fallos: ${fotosPendientes.size}", Toast.LENGTH_LONG).show() }
                    return@launch
                }
            } else {
                Log.d("DRIVE_UPLOAD_BATCH", "Cache hit en VM para '0 fotografias' del exp: $idExpedienteDrive. ID: $fotografiasFolderId")
            }

            // Agrupamos las fotos por su dateFolderName para optimizar la creación de carpetas de fecha
            val fotosPorCarpetaDeFecha = fotosPendientes.groupBy { it.dateFolderName }

            for ((dateFolderNombre, fotosEnEsaFecha) in fotosPorCarpetaDeFecha) {
                if (dateFolderNombre.isBlank()) {
                    Log.w("DRIVE_UPLOAD_BATCH", "Carpeta de fecha vacía para algunas fotos, saltando.")
                    subidasFallidas += fotosEnEsaFecha.size
                    continue
                }
                // 2. Obtener/Crear la carpeta de FECHA dentro de "0 fotografias" UNA SOLA VEZ POR FECHA
                val keyDateFolder = "$fotografiasFolderId/$dateFolderNombre"
                var dateFolderId = idCarpetasFechaCache[keyDateFolder] // Intenta obtener de la caché del VM

                if (dateFolderId == null) {
                    Log.d("DRIVE_UPLOAD_BATCH", "Cache miss en VM para carpeta de fecha: '$dateFolderNombre'. Consultando Drive.")
                    // getOrCreateFolderId usará y actualizará la caché que se le pasa
                    dateFolderId = getOrCreateFolderId(dateFolderNombre, fotografiasFolderId, idCarpetasFechaCache, isDateFolder = true) // ¡PASA LA CACHÉ DEL VM!
                    if (dateFolderId != null) {
                        // getOrCreateFolderId ya debería haberla cacheado si la creó/encontró
                        // idCarpetasFechaCache[keyDateFolder] = dateFolderId // Esta línea es redundante si getOrCreate lo hace
                        Log.d("DRIVE_UPLOAD_BATCH", "Carpeta de fecha '$dateFolderNombre' obtenida/creada y cacheada en VM con ID: $dateFolderId")
                    }
                } else {
                    Log.d("DRIVE_UPLOAD_BATCH", "Cache hit en VM para carpeta de fecha: '$dateFolderNombre'. ID: $dateFolderId")
                }

                // Ahora, después de intentar obtener/crear, comprueba si tenemos un dateFolderId válido
                if (dateFolderId == null) {
                    // Si DESPUÉS de todo, dateFolderId sigue siendo null, es un fallo crítico para esta fecha
                    Log.e("DRIVE_UPLOAD_BATCH", "Fallo crítico al obtener/crear carpeta de fecha '$dateFolderNombre'. Saltando ${fotosEnEsaFecha.size} fotos de esta fecha.")
                    setErrorMessage("Error creando carpeta de fecha '$dateFolderNombre' en Drive.")
                    subidasFallidas += fotosEnEsaFecha.size // Sumamos todas las fotos de esta fecha como fallidas
                    continue // Pasamos al siguiente dateFolderNombre en el bucle principal
                }

                // 3. Subir todas las fotos de esa fecha a esa carpeta de fecha
                for (photoInfo in fotosEnEsaFecha) {
                    // Ahora uploadPhotoToDrive recibe el ID de la carpeta de fecha final
                    val driveId = uploadPhotoToDrive(context, photoInfo, dateFolderId)
                    if (driveId != null) {
                        subidasExitosas++
                    } else {
                        subidasFallidas++
                    }
                }
            }

            val mensajeFinal: String
            if (subidasFallidas > 0 && subidasExitosas > 0) {
                mensajeFinal = "Subida parcial. Éxitos: $subidasExitosas, Fallos: $subidasFallidas"
                setErrorMessage("Algunas fotos no pudieron subirse.") // Error si hubo fallos
            } else if (subidasFallidas > 0) {
                mensajeFinal = "Fallaron todas las subidas: $subidasFallidas"
                setErrorMessage("Error: Ninguna foto pudo subirse.") // Error
            } else {
                mensajeFinal = "¡Subida completada! Éxitos: $subidasExitosas"
                clearErrorMessage() // Limpiar si todo fue bien
            }
            Log.i("DRIVE_UPLOAD_BATCH", mensajeFinal)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, mensajeFinal, Toast.LENGTH_LONG).show()
            }
        }
    }

}