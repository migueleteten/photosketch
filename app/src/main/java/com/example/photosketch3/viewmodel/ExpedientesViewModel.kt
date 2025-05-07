package com.example.photosketch3.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.photosketch3.Expediente
import android.content.Context
import androidx.lifecycle.viewModelScope
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.SheetsScopes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Collections
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
import androidx.compose.ui.graphics.Path
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
class ExpedientesViewModel : ViewModel() {

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

    // StateFlow para la lista de URIs de las fotos en la galería actual
    private val _galleryPhotos = MutableStateFlow<List<Uri>>(emptyList())
    val galleryPhotos: StateFlow<List<Uri>> = _galleryPhotos.asStateFlow()

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

    // --- Funciones futuras ---
    // TODO: Añadir aquí la función para cargar los datos desde Google Sheets
    fun cargarExpedientes(context: Context, accountId: String?) {
        if (accountId == null) {
            Log.w("SHEETS_API", "No se puede cargar expedientes, accountId es null")
            _listaCompletaExpedientes.value = emptyList()
            setErrorMessage("No se pudo obtener la cuenta para cargar datos.") // Establece mensaje de error
            return
        }

        Log.d("SHEETS_API", "Iniciando carga de expedientes (ambas hojas) para cuenta: $accountId")
        clearErrorMessage() // Limpia errores previos ANTES de empezar la operación
        _listaCompletaExpedientes.value = emptyList() // Limpiamos la lista mientras carga (opcional, podrías quitarlo si prefieres mostrar datos viejos)
        _consentIntent.value = null // Aseguramos que no hay un intent de consentimiento pendiente

        viewModelScope.launch(Dispatchers.IO) {
            val listaCombinada = mutableListOf<Expediente>()
            val listaErrores = mutableListOf<String>() // Para acumular mensajes de error
            var necesitaConsentimiento = false // Flag para saber si debemos pedir permiso

            try {
                // 1. Setup Credencial & Service (igual)
                val credential = GoogleAccountCredential.usingOAuth2(
                    context, Collections.singleton(SheetsScopes.SPREADSHEETS_READONLY)
                )
                val androidAccount = Account(accountId, "com.google")
                credential.selectedAccount = androidAccount
                // credential.selectedAccountName = accountId
                val service = Sheets.Builder(
                    NetHttpTransport(), GsonFactory.getDefaultInstance(), credential
                ).setApplicationName("Photo Sketch 3").build()

                // --- Leer Hoja 1: "No aceptados" ---
                val range1 = "$SHEET_NAME_NO_ACEPTADOS!$COLUMNS_TO_READ"
                try {
                    Log.d("SHEETS_API", "Realizando llamada para: $range1")
                    val response1 = service.spreadsheets().values().get(SPREADSHEET_ID, range1).execute()
                    val values1 = response1.getValues()
                    if (values1 != null && values1.isNotEmpty()) {
                        Log.d("SHEETS_API", "Datos recibidos de '$SHEET_NAME_NO_ACEPTADOS': ${values1.size} filas.")
                        values1.mapNotNull { mapRowToExpediente(it) }.also { listaCombinada.addAll(it) }
                    } else {
                        Log.d("SHEETS_API", "No se encontraron datos en '$SHEET_NAME_NO_ACEPTADOS'.")
                    }
                } catch (e: UserRecoverableAuthIOException) {
                    Log.w("SHEETS_API", "Se necesita consentimiento (leyendo $SHEET_NAME_NO_ACEPTADOS)", e)
                    _consentIntent.value = e.intent // Guardamos para pedir permiso
                    necesitaConsentimiento = true // Marcamos que necesitamos consentimiento
                    // NO ponemos mensaje de error aquí, la UI reaccionará al intent
                } catch (e: Exception) {
                    Log.e("SHEETS_API", "Error al leer '$SHEET_NAME_NO_ACEPTADOS'", e)
                    listaErrores.add("Error en '$SHEET_NAME_NO_ACEPTADOS': ${e.message}") // Acumulamos error
                }

                // --- Leer Hoja 2: "Aceptados" (solo si no necesitamos consentimiento) ---
                if (!necesitaConsentimiento) {
                    val range2 = "$SHEET_NAME_ACEPTADOS!$COLUMNS_TO_READ"
                    try {
                        Log.d("SHEETS_API", "Realizando llamada para: $range2")
                        val response2 = service.spreadsheets().values().get(SPREADSHEET_ID, range2).execute()
                        val values2 = response2.getValues()
                        if (values2 != null && values2.isNotEmpty()) {
                            Log.d("SHEETS_API", "Datos recibidos de '$SHEET_NAME_ACEPTADOS': ${values2.size} filas.")
                            values2.mapNotNull { mapRowToExpediente(it) }.also { listaCombinada.addAll(it) }
                        } else {
                            Log.d("SHEETS_API", "No se encontraron datos en '$SHEET_NAME_ACEPTADOS'.")
                        }
                    } catch (e: UserRecoverableAuthIOException) {
                        Log.w("SHEETS_API", "Se necesita consentimiento (leyendo $SHEET_NAME_ACEPTADOS)", e)
                        _consentIntent.value = e.intent // Guardamos para pedir permiso
                        necesitaConsentimiento = true // Marcamos que necesitamos consentimiento
                        // NO ponemos mensaje de error aquí
                    } catch (e: Exception) {
                        Log.e("SHEETS_API", "Error al leer '$SHEET_NAME_ACEPTADOS'", e)
                        listaErrores.add("Error en '$SHEET_NAME_ACEPTADOS': ${e.message}") // Acumulamos error
                    }
                }

                // --- Actualizar Estados Finales ---
                if (!necesitaConsentimiento) {
                    // Si no necesitamos pedir permiso, actualizamos la lista y los errores
                    _listaCompletaExpedientes.value = listaCombinada
                    if (listaErrores.isNotEmpty()) {
                        // Si hubo errores al leer alguna hoja, los mostramos
                        setErrorMessage(listaErrores.joinToString("\n"))
                    } else {
                        // Si todo fue bien (o no había datos), nos aseguramos de limpiar errores
                        clearErrorMessage()
                    }
                    Log.d("SHEETS_API", "Carga finalizada. Total expedientes: ${listaCombinada.size}. Errores acumulados: ${listaErrores.size}")
                } else {
                    // Si necesitamos consentimiento, limpiamos cualquier error genérico anterior
                    // La UI reaccionará al _consentIntent.value no siendo null
                    clearErrorMessage()
                    Log.d("SHEETS_API", "Carga abortada, esperando consentimiento del usuario.")
                }

            } catch (e: Exception) { // Catch para errores gordos en setup (credencial/servicio)
                Log.e("SHEETS_API", "Error general en setup", e)
                _listaCompletaExpedientes.value = emptyList() // Aseguramos lista vacía
                if (e is UserRecoverableAuthIOException) {
                    // Si el error de permiso ocurre muy pronto
                    _consentIntent.value = e.intent
                    clearErrorMessage() // Prioridad al consentimiento
                } else {
                    // Error inesperado durante el setup
                    setErrorMessage("Error inesperado al preparar carga: ${e.message}")
                }
                Log.d("SHEETS_API", "Carga fallida por error de setup.")
            }
        } // Fin viewModelScope.launch
    } // Fin cargarExpedientes

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
    fun triggerGalleryLoad(context: Context, idCarpetaDrive: String?) {
        viewModelScope.launch {
            _galleryPhotos.value = emptyList() // Limpia para mostrar carga si es necesario
            _galleryPhotos.value = loadGalleryPhotosInternal(context, idCarpetaDrive)
        }
    }

    // --- Funciones para manejar estado de UI ---
    fun setLoggedInUser(user: GoogleIdTokenCredential?) {
        _googleUser.value = user
        _errorMessage.value = null // Limpia errores al loguear/desloguear
        if (user != null) {
            // Podríamos iniciar la carga aquí si el usuario es válido
            // O asegurarnos de que se llame desde la UI como antes
        } else {
            _listaCompletaExpedientes.value = emptyList() // Limpiar expedientes al cerrar sesión
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
        _currentPoints.value = listOf(offset) // Inicia la lista con el primer punto
        // Podríamos resetear redoStack aquí también si queremos que empezar a dibujar cancele el Redo
        // redoStack.clear()
        // _canRedo.value = false
    }

    fun addPointToCurrentPath(offset: Offset) {
        // Añade el nuevo punto a la lista existente
        _currentPoints.value = _currentPoints.value + offset
    }

    fun finishCurrentPath() {
        // Solo añade el path si tiene sentido (más de 1 punto)
        if (_currentPoints.value.size > 1) {
            val pathDataInstance = PathData(
                points = _currentPoints.value.toList(), // Copiamos la lista de puntos
                properties = _currentPathProperties.value
            )
            addPath(pathDataInstance) // addPath ya recibe PathData
        }
        // Limpia los puntos actuales independientemente de si se añadió o no
        _currentPoints.value = emptyList()
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
        Log.d("UNDO_REDO", "Estado de dibujo interno limpiado.")
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
            // TODO: Aquí irá la lógica para bloquear la orientación de la pantalla según la foto
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
        Log.d("EDITOR_VM", "initializeEditorFor - URI: $initialPhotoUriString, Carpeta: $targetIdCarpetaDrive")
        if (targetIdCarpetaDrive.isNullOrBlank()) {
            setErrorMessage("ID de carpeta no válido.")
            _galleryPhotos.value = emptyList()
            _currentPhotoUriForEditor.value = null
            _currentPhotoInGalleryIndex.value = 0
            _currentPhotoOriginalDimensions.value = null
            clearDrawingStateInternal()
            setEditingMode(false)
            return
        }

        viewModelScope.launch {
            val uris = loadGalleryPhotosInternal(context, targetIdCarpetaDrive)
            _galleryPhotos.value = uris // Actualiza la galería

            val initialUri = initialPhotoUriString?.let { Uri.parse(it) }
            var targetIndex = 0
            var targetUri: Uri? = null

            if (uris.isNotEmpty()) {
                targetIndex = uris.indexOf(initialUri).takeIf { it != -1 } ?: 0 // Si no la encuentra, la primera
                targetUri = uris.getOrNull(targetIndex)
            } else if (initialUri != null) { // Galería vacía pero nos pasaron una URI (ej. desde cámara antes de refrescar)
                _galleryPhotos.value = listOf(initialUri) // La ponemos como la única foto por ahora
                targetUri = initialUri
                // targetIndex ya es 0
            }

            Log.d("EDITOR_VM", "initializeEditorFor - Galería cargada con ${uris.size} fotos. Índice objetivo: $targetIndex, URI objetivo: $targetUri")

            _currentPhotoInGalleryIndex.value = targetIndex
            // Solo actualiza currentPhotoUriForEditor si es realmente diferente,
            // para no disparar efectos innecesarios si ya era esa.
            if (_currentPhotoUriForEditor.value != targetUri) {
                _currentPhotoUriForEditor.value = targetUri
                loadPhotoDimensions(context, targetUri)
                clearDrawingStateInternal()
                setEditingMode(false)
            } else if (targetUri != null && _currentPhotoOriginalDimensions.value == null){
                // Misma URI, pero quizás no teníamos dimensiones (ej. al volver a la pantalla)
                loadPhotoDimensions(context, targetUri)
                // No limpiamos dibujo ni modo edición aquí si es la misma foto
            }
            Log.d("EDITOR_VM", "initializeEditorFor - Estado final: Índice ${_currentPhotoInGalleryIndex.value}, URI ${_currentPhotoUriForEditor.value}")
        }
    }

    // Esta función se llama cuando el PAGER (UI) cambia de página por swipe del USUARIO
    fun userSwipedToPhotoAtIndex(newIndex: Int, context: Context) {
        val gallery = _galleryPhotos.value
        if (newIndex >= 0 && newIndex < gallery.size) {
            val newUri = gallery[newIndex]
            if (newUri != _currentPhotoUriForEditor.value) { // Solo si la URI realmente cambió
                Log.d("EDITOR_VM", "Usuario hizo swipe a índice $newIndex, URI: $newUri")
                _currentPhotoInGalleryIndex.value = newIndex
                _currentPhotoUriForEditor.value = newUri
                loadPhotoDimensions(context, newUri)
                clearDrawingStateInternal() // Limpia dibujo para la nueva foto
                setEditingMode(false)       // Modo vista
            }
        } else {
            Log.w("EDITOR_VM", "userSwipedToPhotoAtIndex - Índice $newIndex fuera de rango para galería de tamaño ${gallery.size}")
        }
    }

    // Dentro de ExpedientesViewModel
    fun saveEditedImage(
        context: Context,
        originalPhotoUriString: String?, // La URI de la foto original que se está editando
        idCarpetaDrive: String?,
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
                // 1. Cargar Bitmap Original y Corregir Orientación EXIF (código igual que antes)
                val originalBitmapNotRotated: Bitmap? = context.contentResolver.openInputStream(
                    originalPhotoUriString.toUri())?.use {
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

                // 2. Crear Bitmap Mutable para la salida
                val outputBitmap =
                    createBitmap(finalOriginalBitmap!!.width, finalOriginalBitmap!!.height)
                val androidBitmapCanvas = AndroidCanvas(outputBitmap)

                // 3. Dibujar la imagen original (ya orientada)
                androidBitmapCanvas.drawBitmap(finalOriginalBitmap!!, 0f, 0f, null)

                // 4. Calcular factores de escala y offsets (igual que antes, usando finalOriginalBitmap)
                val canvasWidth = canvasDrawSize.width.toFloat()
                val canvasHeight = canvasDrawSize.height.toFloat()
                val imgOrigWidth = finalOriginalBitmap!!.width.toFloat()
                val imgOrigHeight = finalOriginalBitmap!!.height.toFloat()
                val scaleRatio = minOf(canvasWidth / imgOrigWidth, canvasHeight / imgOrigHeight)
                val scaledImageWidth = imgOrigWidth * scaleRatio
                val scaledImageHeight = imgOrigHeight * scaleRatio
                val imageOffsetX = (canvasWidth - scaledImageWidth) / 2f
                val imageOffsetY = (canvasHeight - scaledImageHeight) / 2f

                fun transformPoint(composeCanvasPoint: Offset): Offset {
                    val pointRelativeToScaledImageX = composeCanvasPoint.x - imageOffsetX
                    val pointRelativeToScaledImageY = composeCanvasPoint.y - imageOffsetY
                    return Offset(
                        x = pointRelativeToScaledImageX / scaleRatio,
                        y = pointRelativeToScaledImageY / scaleRatio
                    )
                }

                // 5. Dibujar los trazos guardados y el actual (igual que antes)
                val allPathsToDraw = drawnPathsToSave.toMutableList()
                if (currentPointsToSave.size > 1) { // Añadir el trazo actual si es válido
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
                            strokeWidth = pathData.properties.strokeWidth / scaleRatio // Escalar grosor
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

                // 6. Guardar el outputBitmap en un archivo nuevo (igual que antes)
                // ... (lógica de crear directorio de expediente, de fecha, nombre de archivo con "_edited.jpg")
                val baseDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                val expedienteDirNameSanitized = idCarpetaDrive.replace(Regex("[^a-zA-Z0-9.-]"), "_")
                val dateFolderName = SimpleDateFormat("yyyyMMdd", Locale.US).format(System.currentTimeMillis())
                val expedienteDir = File(baseDir, expedienteDirNameSanitized)
                val dateDir = File(expedienteDir, dateFolderName)
                if (!dateDir.exists()) dateDir.mkdirs()
                val photoFileName = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis()) + "_edited.jpg"
                val newPhotoFile = File(dateDir, photoFileName)

                FileOutputStream(newPhotoFile).use { out ->
                    outputBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }
                newFileUri = newPhotoFile.toUri()
                Log.d("EDITOR_SAVE", "Imagen editada guardada en: $newFileUri")

                // Volvemos a cargar TODA la galería del expediente para que incluya la nueva _edited.jpg
                val refreshedGallery = loadGalleryPhotosInternal(context, idCarpetaDrive)
                _galleryPhotos.value = refreshedGallery

                // Buscamos la nueva foto editada en la galería refrescada para ponerla como actual
                val newIndexInGallery = refreshedGallery.indexOf(newFileUri).takeIf { it != -1 }
                    ?: (refreshedGallery.size - 1) // Si no la encuentra (raro), la última

                // --- ¡NUEVO! Acciones Post-Guardado ---
                /*_lastSavedEditedPhotoUri.value = newFileUri // Actualiza el StateFlow
                _currentPhotoInGalleryIndex.value = newIndexInGallery.coerceAtLeast(0)
                _currentPhotoUriForEditor.value = newFileUri // Hace que el editor muestre la nueva imagen
                loadPhotoDimensions(context, newFileUri)
                setEditingMode(false) // Volvemos a modo vista
                _hasUnsavedChanges.value = false
                loadGalleryPhotosInternal(context, idCarpetaDrive) // Refrescamos la galería para que incluya esta nueva foto
                clearDrawingStateInternal() // Limpiamos el lienzo y las pilas undo/redo
                // --- FIN Acciones Post-Guardado ---*/
                _lastSavedEditedPhotoUri.value = newFileUri
                Log.d("EDITOR_SAVE", "Imagen editada guardada en: $newFileUri")
                // Llamamos a initializeEditorFor para que reconfigure todo para la nueva foto
                // El idCarpetaDrive original debería estar disponible
                initializeEditorFor(context, newFileUri.toString(), idCarpetaDrive)

            } catch (e: Exception) {
                Log.e("EDITOR_SAVE", "Error al guardar imagen editada", e)
                setErrorMessage("Error al guardar: ${e.message}")
                _lastSavedEditedPhotoUri.value = null // Limpiamos por si acaso
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

    /*fun nextPhotoInEditor(context: Context) {
        if (_galleryPhotos.value.isEmpty()) return
        var newIndex = _currentPhotoInGalleryIndex.value + 1
        if (newIndex >= _galleryPhotos.value.size) {
            newIndex = 0 // Vuelve al principio (loop)
        }
        _currentPhotoInGalleryIndex.value = newIndex
        _currentPhotoUriForEditor.value = _galleryPhotos.value[newIndex]
        loadPhotoDimensions(context, _currentPhotoUriForEditor.value)
        clearDrawingStateInternal() // Limpia lienzo para la nueva foto
        setEditingMode(false)
        Log.d("EDITOR_VM", "Siguiente foto: ${_currentPhotoUriForEditor.value}, Índice: $newIndex")
    }

    fun previousPhotoInEditor(context: Context) {
        if (_galleryPhotos.value.isEmpty()) return
        var newIndex = _currentPhotoInGalleryIndex.value - 1
        if (newIndex < 0) {
            newIndex = _galleryPhotos.value.size - 1 // Va al final (loop)
        }
        _currentPhotoInGalleryIndex.value = newIndex
        _currentPhotoUriForEditor.value = _galleryPhotos.value[newIndex]
        loadPhotoDimensions(context, _currentPhotoUriForEditor.value)
        clearDrawingStateInternal() // Limpia lienzo para la nueva foto
        setEditingMode(false)
        Log.d("EDITOR_VM", "Foto anterior: ${_currentPhotoUriForEditor.value}, Índice: $newIndex")
    }*/

    // Esta función ahora se encarga de preparar el editor para una URI específica
    fun changeDisplayedPhotoInEditor(newPhotoUri: Uri?, context: Context) {
        if (newPhotoUri == _currentPhotoUriForEditor.value && _currentPhotoUriForEditor.value != null) {
            // Ya estamos mostrando esta foto, no es necesario recargar todo,
            // a menos que queramos forzar un reseteo del modo edición.
            // Si entramos en modo edición y luego swipamos y volvemos, queremos que esté en modo vista.
            if (_isEditingMode.value) { // Si estábamos editando, salimos del modo edición
                setEditingMode(false)
            }
            // No limpiamos el dibujo aquí si es la misma foto, a menos que sea una regla de negocio
            // Si el dibujo debe persistir por foto HASTA QUE SE GUARDE, no limpiar aquí.
            // Si cada swipe a una foto nueva (o de vuelta a una ya vista) debe limpiar, sí.
            // Nuestra lógica actual en next/previous era limpiar, así que la mantenemos.
            clearDrawingStateInternal() // Limpia lienzo y estados de undo/redo para la nueva/misma foto
            Log.d("EDITOR_VM", "Mostrando misma foto o refrescando estado para: $newPhotoUri")
            // Dimensiones ya deberían haberse cargado si es la misma URI, pero por si acaso:
            if (_currentPhotoOriginalDimensions.value == null && newPhotoUri != null) {
                loadPhotoDimensions(context, newPhotoUri)
            }
            return
        }

        Log.d("EDITOR_VM", "Cambiando foto en editor a: $newPhotoUri")
        _currentPhotoUriForEditor.value = newPhotoUri
        // Actualizar el índice si la URI es parte de la galería actual
        _galleryPhotos.value.indexOf(newPhotoUri).takeIf { it != -1 }?.let {
            _currentPhotoInGalleryIndex.value = it
        }

        loadPhotoDimensions(context, newPhotoUri) // Carga dimensiones para la nueva foto
        clearDrawingStateInternal()             // Limpia el lienzo y estados de undo/redo
        setEditingMode(false)                   // Aseguramos que empieza en modo vista
    }

    fun setEditingMode(isEditing: Boolean) {
        _isEditingMode.value = isEditing
        Log.d("EDITOR_VM", "Modo Edición: $isEditing")
        if (!isEditing) {
            _currentTool.value = null
            // Podríamos resetear la herramienta seleccionada aquí
        }
        Log.d("EDITOR_VM", "Modo Edición: $isEditing")
    }

    // Dentro de ExpedientesViewModel
    private fun loadPhotoDimensions(context: Context, photoUri: Uri?) {
        Log.d("EDITOR_VM_DIMS", "loadPhotoDimensions - INTENTANDO cargar para URI: $photoUri")
        if (photoUri == null) {
            _currentPhotoOriginalDimensions.value = null
            Log.d("EDITOR_VM", "URI nula, no se cargan dimensiones.")
            return
        }
        // Ya estamos en viewModelScope.launch en las funciones que llaman a esto,
        // pero si esta función pudiera llamarse desde otro sitio, considera un scope.
        // Para BitmapFactory, el contexto directo es suficiente si el stream es corto.
        try {
            context.contentResolver.openInputStream(photoUri)?.use { inputStream ->
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true // Solo queremos las dimensiones
                }
                BitmapFactory.decodeStream(inputStream, null, options)
                if (options.outWidth > 0 && options.outHeight > 0) {
                    _currentPhotoOriginalDimensions.value = IntSize(options.outWidth, options.outHeight)
                    Log.d("EDITOR_VM_DIMS", "loadPhotoDimensions - ÉXITO para $photoUri: ${_currentPhotoOriginalDimensions.value}")
                } else {
                    Log.w("EDITOR_VM", "No se pudieron obtener dimensiones válidas para $photoUri")
                    _currentPhotoOriginalDimensions.value = null
                }
            } ?: run {
                Log.w("EDITOR_VM", "InputStream nulo para $photoUri al cargar dimensiones.")
                _currentPhotoOriginalDimensions.value = null
            }
        } catch (e: Exception) {
            Log.e("EDITOR_VM", "Error cargando dimensiones para $photoUri", e)
            _currentPhotoOriginalDimensions.value = null
            setErrorMessage("No se pudo leer la imagen para obtener dimensiones.")
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

    // TODO: Añadir lógica para manejar la selección de un expediente, etc.

}