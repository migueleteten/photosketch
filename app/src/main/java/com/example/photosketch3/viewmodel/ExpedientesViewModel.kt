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

// Guarda las propiedades de un trazo (color, grosor)
data class PathProperties(
    val color: Color = Color.Red, // Color por defecto (¡Rojo para que se vea bien!)
    val strokeWidth: Float = 6f, // Grosor por defecto (un poco grueso para empezar)
    val strokeCap: StrokeCap = StrokeCap.Round, // Terminación redondeada
    val strokeJoin: StrokeJoin = StrokeJoin.Round // Unión redondeada
)

// Guarda un Path (la línea) y sus propiedades
data class PathData(
    val path: Path = Path(), // El objeto Path que contiene los puntos
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

    // Función para buscar fotos locales para un expediente específico
    fun loadGalleryPhotos(context: Context, idCarpetaDrive: String?) {
        if (idCarpetaDrive.isNullOrBlank()) {
            Log.w("GALLERY", "ID Carpeta Drive nulo/vacío, no se pueden cargar fotos locales.")
            _galleryPhotos.value = emptyList()
            return
        }
        Log.d("GALLERY", "Buscando fotos locales para: $idCarpetaDrive")

        viewModelScope.launch(Dispatchers.IO) { // Trabajo de archivos en hilo IO
            try {
                val baseDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                val expedienteDirName = idCarpetaDrive.replace(Regex("[^a-zA-Z0-9.-]"), "_")
                val expedienteDir = File(baseDir, expedienteDirName)

                val photoUris = mutableListOf<Uri>()

                if (expedienteDir.exists() && expedienteDir.isDirectory) {
                    // Listamos las subcarpetas de fecha (o directamente los archivos si no usamos fechas)
                    val dateFolders = expedienteDir.listFiles { file -> file.isDirectory } ?: emptyArray()

                    if (dateFolders.isNotEmpty()) {
                        // Ordenamos por nombre para que las fechas salgan ordenadas (opcional)
                        dateFolders.sortBy { it.name }
                        // Recorremos cada carpeta de fecha
                        for (dateDir in dateFolders) {
                            val photosInDate = dateDir.listFiles { file ->
                                file.isFile && file.extension.equals("jpg", ignoreCase = true)
                            } ?: emptyArray()
                            // Ordenamos por nombre (timestamp) para orden cronológico (opcional)
                            photosInDate.sortBy { it.name }
                            // Añadimos las URIs a nuestra lista
                            photosInDate.forEach { photoFile ->
                                photoUris.add(photoFile.toUri()) // Usamos .toUri()
                            }
                        }
                    } else {
                        // Si no hay carpetas de fecha, buscar fotos directamente en la carpeta del expediente
                        // (Adaptar si la lógica de guardado fue diferente)
                        val photosInExp = expedienteDir.listFiles { file ->
                            file.isFile && file.extension.equals("jpg", ignoreCase = true)
                        } ?: emptyArray()
                        photosInExp.sortBy{ it.name }
                        photosInExp.forEach { photoFile ->
                            photoUris.add(photoFile.toUri())
                        }
                        if (photosInExp.isEmpty()) Log.d("GALLERY", "No se encontraron fotos en ${expedienteDir.absolutePath}")

                    }

                } else {
                    Log.d("GALLERY", "El directorio del expediente no existe: ${expedienteDir.absolutePath}")
                }

                Log.d("GALLERY", "Fotos locales encontradas: ${photoUris.size}")
                _galleryPhotos.value = photoUris // Actualizamos el StateFlow

            } catch (e: Exception) {
                Log.e("GALLERY", "Error al listar fotos locales", e)
                _galleryPhotos.value = emptyList() // Limpiamos en caso de error
                setErrorMessage("Error al cargar galería local.") // Informamos del error
            }
        }
    } // Fin cargar fotos expediente

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
        Log.d("UNDO_REDO", "Path añadido. Undo: ${_canUndo.value}, Redo: ${_canRedo.value}")
    }

    fun undo() {
        if (_drawnPaths.value.isNotEmpty()) {
            // Cogemos el último trazo de la lista principal
            val lastPath = _drawnPaths.value.last()
            // Lo añadimos a la pila de Rehacer (redoStack)
            redoStack.add(lastPath)
            // Creamos una nueva lista sin ese último trazo
            _drawnPaths.value = _drawnPaths.value.dropLast(1)
            // Actualizamos estado de botones
            _canUndo.value = _drawnPaths.value.isNotEmpty()
            _canRedo.value = true
            Log.d("UNDO_REDO", "Undo. Paths: ${_drawnPaths.value.size}, Redo Stack: ${redoStack.size}, CanUndo: ${_canUndo.value}, CanRedo: ${_canRedo.value}")
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
            val finalPath = Path().apply {
                moveTo(_currentPoints.value.first().x, _currentPoints.value.first().y)
                _currentPoints.value.drop(1).forEach { lineTo(it.x, it.y) }
            }
            addPath(PathData(path = finalPath, properties = _currentPathProperties.value))
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
        Log.d("UNDO_REDO", "Estado de dibujo interno limpiado.")
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