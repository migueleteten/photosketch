package com.example.photosketch3

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column // Import necesario
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding // Import necesario
import androidx.compose.material3.MaterialTheme // Import necesario
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface // Import necesario
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue // Import necesario
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle // ¡Import CLAVE!
import androidx.lifecycle.viewmodel.compose.viewModel // ¡Import CLAVE!
import com.example.photosketch3.ui.theme.PhotoSketch3Theme // Asegúrate que el theme sea el tuyo
import com.example.photosketch3.viewmodel.ExpedientesViewModel
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult // Este puede que ya estuviera
import androidx.activity.result.IntentSenderRequest // Para lanzar intents de forma segura
import androidx.activity.result.contract.ActivityResultContracts // Este puede que ya estuviera
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text // Asegúrate que esté
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf // Para guardar el estado (logueado/error)
import androidx.compose.runtime.remember // Para recordar objetos
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue // Para cambiar el estado
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager // El nuevo gestor
import androidx.credentials.CustomCredential // Tipo base de credencial
import androidx.credentials.GetCredentialRequest // La solicitud
import androidx.credentials.GetCredentialResponse // La respuesta
import androidx.credentials.exceptions.GetCredentialException // Excepción general
import com.google.android.libraries.identity.googleid.GetGoogleIdOption // Opción específica para Google ID
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential // La credencial específica de Google
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException // Excepción específica
import kotlinx.coroutines.launch
import android.app.Activity
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
// imports añadidos para poder buscar expedientes
import androidx.compose.material3.OutlinedTextField // O TextField normal
import androidx.compose.foundation.layout.Spacer // Para espaciar (opcional)
import androidx.compose.foundation.layout.height // Para Spacer (opcional)
// imports añadidos para poder navegar entre pantallas
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.foundation.clickable // Import para hacerlo clicable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons // Para icono flecha atrás (opcional)
import androidx.compose.material.icons.filled.ArrowBack // Para icono flecha atrás (opcional)
import androidx.compose.material3.Icon // Para icono flecha atrás (opcional)
import androidx.compose.material3.IconButton // Para icono flecha atrás (opcional)
import androidx.compose.material3.TopAppBar // Para poner título y botón atrás (opcional)
import androidx.compose.material3.ExperimentalMaterial3Api // Necesario para TopAppBar
import androidx.compose.foundation.layout.Box
// imports añadidos para la cámara
import android.Manifest // Para el permiso
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture // Lo usaremos pronto
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView // La vista de cámara
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.DisposableEffect // Para limpiar recursos
import androidx.compose.runtime.LaunchedEffect // Para operaciones asíncronas
import androidx.compose.runtime.mutableStateOf // Para estado de permiso
import androidx.compose.runtime.remember // Para recordar estado y objetos
import androidx.compose.ui.platform.LocalLifecycleOwner // Para vincular cámara al ciclo de vida
import androidx.compose.ui.viewinterop.AndroidView // Para usar PreviewView en Compose
import androidx.core.content.ContextCompat // Para comprobar permisos
// imports añadidos para el botón de la cámara
import android.net.Uri // Para la URI de la imagen guardada
import android.os.Environment // Para el directorio de imágenes (aunque usaremos uno específico de la app)
import android.widget.Toast // Para mostrar mensajes rápidos
import androidx.camera.core.ImageCaptureException
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.filled.PhotoCamera // Icono para el botón
import java.io.File // Para manejar archivos
import java.text.SimpleDateFormat // Para nombres de archivo únicos
import java.util.Locale // Para el formato de fecha
import java.util.concurrent.Executor // Usaremos el principal
// imports añadidos para la vibración
import android.os.VibratorManager // El nuevo gestor
import android.os.Vibrator       // Sigue siendo necesario
import android.os.VibrationEffect // Para el efecto moderno
import android.os.Build          // Para comprobar la versión de SDK
import androidx.compose.foundation.BorderStroke
// imports añadidos para mostrar la miniatura
import androidx.compose.ui.graphics.Color // Para el borde opcional
import androidx.compose.foundation.border // Para el borde opcional
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage // ¡La estrella para cargar imágenes!
import java.net.URLEncoder // Para codificar la URI
import java.net.URLDecoder // Para decodificarla
import java.nio.charset.StandardCharsets // Para la codificación
import androidx.compose.material.icons.outlined.PhotoLibrary // Icono de galería
// imports añadidos para galería
import androidx.compose.foundation.lazy.grid.GridCells // Para la cuadrícula
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid // La cuadrícula
import androidx.compose.foundation.lazy.grid.items // Para items de la cuadrícula
import androidx.compose.foundation.layout.aspectRatio // Para mantener proporción de imagen
// imports para el editor
import androidx.compose.material.icons.filled.Edit // Lápiz
import androidx.compose.material.icons.filled.Timeline // Línea recta (o usa otro)
import androidx.compose.material.icons.filled.Palette // Color
import androidx.compose.material.icons.filled.LineWeight // Grosor
import androidx.compose.material.icons.filled.Save // Guardar
import androidx.compose.foundation.layout.Row // Para la barra de herramientas
import androidx.compose.foundation.layout.fillMaxWidth // Para la barra
import androidx.compose.foundation.layout.wrapContentHeight // Para la barra
import androidx.compose.foundation.background // Para el fondo de la barra (opcional)
import androidx.compose.material3.BottomAppBar // Alternativa para barra inferior
import androidx.compose.material3.Divider // Separador visual
// imports para dibujar a mano alzada (lápiz)
import androidx.compose.ui.graphics.Path // Para guardar las líneas dibujadas
import androidx.compose.ui.graphics.StrokeCap // Para la terminación de la línea
import androidx.compose.ui.graphics.StrokeJoin // Para las uniones de la línea
import androidx.compose.ui.graphics.drawscope.Stroke // Para el estilo del trazo
import androidx.compose.foundation.Canvas // El lienzo donde dibujaremos
import androidx.compose.ui.input.pointer.pointerInput // Para detectar gestos táctiles
import androidx.compose.ui.geometry.Offset // Para las coordenadas del dedo
import androidx.compose.foundation.gestures.detectDragGestures // El detector específico
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.mutableStateListOf // Lista mutable que recompone UI
import androidx.compose.runtime.MutableState // Para el path actual
import androidx.compose.ui.graphics.asImageBitmap // Para el bitmap si lo usáramos
// imports para iconos hacer / deshacer
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Undo
import com.example.photosketch3.viewmodel.PathData
import com.example.photosketch3.viewmodel.PathProperties
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalContentColor
// paginación bonita
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.pager.PagerState // Para el estado del pager
import androidx.compose.runtime.snapshotFlow
import androidx.core.net.toUri
import kotlinx.coroutines.flow.distinctUntilChanged
// bloqueo de orientación
import android.content.pm.ActivityInfo
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.platform.LocalDensity
// cámaras
import androidx.compose.material.icons.filled.SwitchCamera
import androidx.compose.material.icons.filled.CameraFront
import androidx.compose.material.icons.filled.CameraRear
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.camera.core.CameraSelector.Builder
import androidx.camera.core.CameraSelector.LensFacing
import androidx.compose.material.icons.filled.CameraOutdoor
import androidx.camera.camera2.interop.Camera2CameraInfo // Para acceder a características avanzadas
import android.hardware.camera2.CameraCharacteristics // Para leer características como focal length
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.CameraInfo // Para obtener info de cada cámara
import androidx.camera.view.LifecycleCameraController
import androidx.lifecycle.compose.LocalLifecycleOwner

class MainActivity : ComponentActivity() {

    // @OptIn se necesita si usas APIs experimentales como TopAppBar
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PhotoSketch3Theme {
                // --- 1. Controlador de Navegación ---
                val navController = rememberNavController()

                // --- 2. Contenedor de Navegación (NavHost) ---
                NavHost(
                    navController = navController, // Le pasamos el controlador
                    startDestination = "lista_expedientes" // Pantalla inicial
                ) {
                    // --- 3. Definición de Pantalla: Lista de Expedientes ---
                    composable("lista_expedientes") {
                        // Llamamos al Composable que contiene toda la lógica/UI de la lista
                        ListaExpedientesScreen(navController = navController)
                    }

                    // --- 4. Definición de Pantalla: Cámara (Placeholder) ---
                    composable(
                        route = "pantalla_camara/{idCarpetaDrive}/{expedienteNombre}", // Ruta con argumento
                        arguments = listOf(
                            navArgument("idCarpetaDrive") { type = NavType.StringType },
                            navArgument("expedienteNombre") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val idCarpeta = backStackEntry.arguments?.getString("idCarpetaDrive")
                        val nombreExp = backStackEntry.arguments?.getString("expedienteNombre")
                        CameraScreen(
                            navController = navController,
                            idCarpetaDrive = idCarpeta,
                            expedienteNombre = nombreExp
                        )
                    }

                    // --- 5. Definición de Pantalla: Editor de fotos ---
                    composable(
                        // Pasamos la URI de la foto como argumento en la ruta
                        // La codificamos porque las URIs pueden tener caracteres especiales
                        route = "pantalla_editor/{photoUri}/{idCarpetaDrive}",
                        arguments = listOf(
                            navArgument("photoUri") { type = NavType.StringType },
                            navArgument("idCarpetaDrive") {
                                type = NavType.StringType
                                nullable = true // Hacemos que pueda ser nulo por si acaso
                            }
                        )
                    ) { backStackEntry ->
                        // Obtenemos la URI codificada y la decodificamos
                        val encodedUri = backStackEntry.arguments?.getString("photoUri")
                        val photoUriString = encodedUri?.let { URLDecoder.decode(it, StandardCharsets.UTF_8.name()) }
                        val idCarpeta = backStackEntry.arguments?.getString("idCarpetaDrive")
                        val nombreExp = backStackEntry.arguments?.getString("expedienteNombre")
                        EditorScreen(
                            navController = navController,
                            photoUriString = photoUriString, // Pasamos la URI como String
                            idCarpetaDrive = idCarpeta,
                            expedienteNombre = nombreExp
                        )
                    }

                    // --- 6. Definición de Pantalla: Galería ---
                    composable(
                        route = "pantalla_galeria/{idCarpetaDrive}/{expedienteNombre}", // Mismos args que cámara
                        arguments = listOf(
                            navArgument("idCarpetaDrive") { type = NavType.StringType; nullable = true }, // Permitir nulo por si acaso
                            navArgument("expedienteNombre") { type = NavType.StringType; nullable = true }
                        )
                    ) { backStackEntry ->
                        val idCarpeta = backStackEntry.arguments?.getString("idCarpetaDrive")
                        val nombreExp = backStackEntry.arguments?.getString("expedienteNombre")
                        GalleryScreen( // Llamamos a la pantalla de galería
                            navController = navController,
                            idCarpetaDrive = idCarpeta,
                            expedienteNombre = nombreExp
                        )
                    }
                    // TODO: Añadir más pantallas aquí (ej: Galería, Editor...)
                }
            }
        }
    } // Fin onCreate
} // Fin MainActivity

// ========================================================================
// Composable para la pantalla PRINCIPAL (Lista de Expedientes)
// Contiene toda la lógica y UI que antes estaba en setContent
// ========================================================================
@Composable
fun ListaExpedientesScreen(navController: NavHostController) {

    // --- Setup (ViewModel, Estados, Credential Manager, etc. - TODO: Mover todo aquí) ---
    val viewModel: ExpedientesViewModel = viewModel() // Obtiene el ViewModel
    // Observa estados del ViewModel
    val userState by viewModel.googleUser.collectAsStateWithLifecycle()
    val errorState by viewModel.errorMessage.collectAsStateWithLifecycle()
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()
    val listaFiltrada by viewModel.expedientesFiltrados.collectAsStateWithLifecycle()

    // Setup Credential Manager y Google Sign-In (necesario para el botón de login)
    val WEB_CLIENT_ID = "364405725042-0mav9dqnhie25upukiegp35g7vcpki3p.apps.googleusercontent.com" // Tu Web Client ID
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val googleIdOption: GetGoogleIdOption = remember {
        GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(WEB_CLIENT_ID)
            .build()
    }
    val request: GetCredentialRequest = remember {
        GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()
    }

    // Launcher para manejar el resultado del CONSENTIMIENTO de Sheets API
    val consentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            Log.d("API_CONSENT", "Permiso concedido por el usuario a través del Intent.")
            // El usuario concedió el permiso (para Sheets o Drive).
            // Ahora tenemos que REINTENTAR la operación que lo necesitó.
            // Si el consentimiento era para Sheets, reintentamos cargar expedientes.
            // Si era para Drive (del testDriveConnection), reintentamos el test de Drive.
            // Por ahora, vamos a asumir que si saltó el consentimiento, fue porque
            // initializeGoogleServices no pudo completar la creación de los servicios
            // o la llamada de prueba a Drive falló pidiendo permiso.
            // La acción más segura aquí es REINTENTAR la inicialización de servicios,
            // que a su vez llamará a cargarExpedientes y testDriveConnection.

            // --- CAMBIO IMPORTANTE AQUÍ ---
            val currentUser = viewModel.googleUser.value // Obtenemos el usuario actual del ViewModel
            currentUser?.id?.let { accountEmail -> // id suele ser el email
                Log.d("API_CONSENT", "Reintentando inicialización de servicios para: $accountEmail")
                viewModel.initializeGoogleServices(context, accountEmail)
            } ?: run {
                Log.e("API_CONSENT", "No se pudo obtener el usuario para reintentar inicialización de servicios.")
                viewModel.setErrorMessage("Error al reintentar después del permiso: Usuario no disponible.")
            }
            // --- FIN CAMBIO ---

        } else {
            Log.w("API_CONSENT", "Permiso denegado por el usuario a través del Intent.")
            viewModel.setErrorMessage("Se necesitan permisos para acceder a los servicios de Google.")
        }
    }

    // Observador para lanzar el Intent de Consentimiento si es necesario
    val intentParaConsentimiento by viewModel.consentIntent.collectAsStateWithLifecycle()
    LaunchedEffect(intentParaConsentimiento) {
        intentParaConsentimiento?.let { intent ->
            Log.d("SHEETS_API_CONSENT", "Lanzando intent de consentimiento...")
            consentLauncher.launch(intent)
            viewModel.consentIntentHandled()
        }
    }

    // Lambda para la lógica de INICIO de sesión
    val startSignInFlow: () -> Unit = {
        viewModel.clearErrorMessage()
        scope.launch {
            try {
                Log.d("CREDMAN_SIGN_IN", "Lanzando solicitud...")
                val result = credentialManager.getCredential(context, request)
                val credential = result.credential
                Log.d("CREDMAN_SIGN_IN", "Recibida credencial tipo: ${credential.type}")

                if (credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    try {
                        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                        viewModel.setLoggedInUser(googleIdTokenCredential)
                        Log.d("CREDMAN_SIGN_IN", "Éxito: displayName='${googleIdTokenCredential.displayName}', id='${googleIdTokenCredential.id}'")
                        // Usamos el ID directamente de la credencial obtenida
                        googleIdTokenCredential.id.let { accountEmail -> // id suele ser el email aquí
                            viewModel.initializeGoogleServices(context, accountEmail)
                        }
                    } catch (e: GoogleIdTokenParsingException) {
                        Log.e("CREDMAN_SIGN_IN", "Error parseo", e)
                        viewModel.setErrorMessage("Error procesando respuesta.")
                    }
                } else {
                    Log.e("CREDMAN_SIGN_IN", "Tipo de credencial inesperado: ${credential.type}")
                    viewModel.setErrorMessage("Tipo de credencial no esperado.")
                }
            } catch (e: GetCredentialException) {
                Log.e("CREDMAN_SIGN_IN", "Error al obtener credencial", e)
                viewModel.setErrorMessage("Error: ${e.javaClass.simpleName}")
            }
        } // Fin launch
    }
    // --- Fin Setup ---


    // --- UI de la Lista de Expedientes ---
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(bottom = 8.dp), // Padding inferior opcional
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Info de usuario / Botón Login / Buscador
                if (userState != null) {
                    // --- Bloque si está logueado ---
                    Text("¡Hola, ${userState?.displayName ?: "Usuario"}!")
                    Text("ID: ${userState?.id ?: "N/A"}")
                    Button(onClick = { viewModel.signOut() }) { Text("Cerrar Sesión") }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = query,
                        onValueChange = { viewModel.onSearchQueryChanged(it) },
                        label = { Text("Buscar por Código o Dirección") },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp) // Ocupa ancho
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider() // Separador antes de la lista
                    // --- Fin bloque logueado ---
                } else {
                    // --- Bloque si NO está logueado ---
                    Spacer(modifier = Modifier.height(16.dp)) // Espacio arriba
                    Text("Estado: No iniciada la sesión")
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { startSignInFlow() }) { Text("Iniciar Sesión") }
                    Spacer(modifier = Modifier.height(16.dp)) // Espacio abajo
                    // --- Fin bloque no logueado ---
                }

                // Lista de Expedientes (Clicable) - Solo si está logueado
                if (userState != null) {
                    if (listaFiltrada.isNotEmpty()) {
                        LazyColumn(modifier = Modifier.weight(1f)) { // Ocupa el espacio restante
                            items(listaFiltrada) { expediente ->
                                // ENVOLVEMOS LA FILA CON CLICKABLE
                                Box(modifier = Modifier
                                    .clickable {
                                        Log.d("NAV", "Clic en expediente: ${expediente.nombre}, ID Carpeta: ${expediente.idCarpetaDrive}")
                                        if (expediente.idCarpetaDrive.isNotBlank()) {
                                            // ¡NAVEGAMOS!
                                            navController.navigate("pantalla_camara/${expediente.idCarpetaDrive}/${expediente.nombre}")
                                        } else {
                                            Log.w("NAV", "ID Carpeta vacío, no se navega.")
                                            // TODO: Mostrar mensaje? (Toast/Snackbar)
                                        }
                                    }
                                    .fillMaxWidth() // Ocupa todo el ancho para clic fácil
                                ) {
                                    ExpedienteRow(expediente = expediente)
                                }
                                HorizontalDivider() // Separador entre filas
                            }
                        }
                    } else if (query.isBlank()){ // Logueado, lista vacía, sin búsqueda -> Cargando/Sin datos
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Cargando expedientes o no hay datos...")
                        // CircularProgressIndicator() // Podrías mostrar un spinner
                    } else { // Logueado, lista vacía, CON búsqueda -> No hay resultados
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No se encontraron expedientes para '$query'")
                    }
                }

                // Mensaje de error (si existe)
                if (errorState != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Error: $errorState", color = MaterialTheme.colorScheme.error)
                }

                // Contador de expedientes (muestra el total filtrado)
                if (userState != null) { // Solo mostrar si está logueado
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "(${listaFiltrada.size} expedientes mostrados)")
                }

            } // Fin Column principal
        } // Fin Surface
    } // Fin Scaffold
} // Fin ListaExpedientesScreen

// ========================================================================
// Composable para la Cámara (Necesita @OptIn)
// ========================================================================
@androidx.annotation.OptIn(ExperimentalCamera2Interop::class)
@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(navController: NavHostController, idCarpetaDrive: String?, expedienteNombre: String?) {
    val context = LocalContext.current
    val viewModel: ExpedientesViewModel = viewModel()
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    val cameraController = remember { LifecycleCameraController(context) }

    // --- Estado y Launcher para el Permiso de Cámara ---
    var hasCamPermission by remember { mutableStateOf(
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    )}
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        hasCamPermission = isGranted
        if (!isGranted) {
            Log.w("CAMARA", "Permiso de cámara DENEGADO")
            // TODO: Mostrar mensaje permanente o guiar a ajustes?
        }
    }

    // Estados para guardar los Selectors específicos que encontremos
    var backCameraSelector by remember { mutableStateOf<CameraSelector?>(null) }
    var frontCameraSelector by remember { mutableStateOf<CameraSelector?>(null) }
    var wideAngleCameraSelector by remember { mutableStateOf<CameraSelector?>(null) }

    // Estado para guardar la referencia al caso de uso ImageCapture
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }

    // Para mostrar la miniatura de la última foto
    var lastPhotoUri by remember { mutableStateOf<Uri?>(null) }

    // Estado para saber cuál selector está activo actualmente
    var activeCameraSelector by remember { mutableStateOf<CameraSelector?>(null) }

    // Efecto para pedir permiso si no lo tenemos al entrar
    LaunchedEffect(key1 = true) { // Se ejecuta solo una vez al entrar al composable
        if (!hasCamPermission) {
            Log.d("CAMARA", "Permiso de cámara no concedido, solicitando...")
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(Unit) { // Ejecutar solo una vez
        try {
            val cameraProvider = ProcessCameraProvider.getInstance(context).get() // Obtener provider síncrono aquí (cuidado si tarda mucho)
            val availableCameras = cameraProvider.availableCameraInfos

            Log.d("CAMARA_DETECT", "Cámaras encontradas: ${availableCameras.size}")

            // Variables temporales para guardar los mejores candidatos
            var foundBack: Pair<CameraInfo, Float>? = null // Guardamos CameraInfo y su focal length "normal"
            var foundFront: CameraInfo? = null
            var foundWide: Pair<CameraInfo, Float>? = null // Guardamos CameraInfo y su focal length mínima

            availableCameras.forEach { cameraInfo ->
                try { // Es buena idea envolver en try-catch por si alguna característica falla
                    // --- CORRECCIONES AQUÍ ---
                    // Obtenemos Camera2Info para acceder a características específicas
                    val cam2Info = Camera2CameraInfo.from(cameraInfo)

                    // Obtenemos LENS_FACING directamente usando su Key
                    val lensFacing: Int? = cam2Info.getCameraCharacteristic(CameraCharacteristics.LENS_FACING)

                    // Obtenemos LENS_INFO_AVAILABLE_FOCAL_LENGTHS directamente usando su Key
                    // El tipo que devuelve esta Key es FloatArray?
                    val focalLengths: FloatArray? = cam2Info.getCameraCharacteristic(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
                    // --- FIN CORRECCIONES ---

                    // Ahora que focalLengths es FloatArray?, 'it' funcionará en las lambdas
                    val minFocalLength = focalLengths?.minOrNull() ?: Float.MAX_VALUE
                    val typicalFocalLength = focalLengths?.firstOrNull { it > 20 && it < 70 } ?: focalLengths?.firstOrNull() ?: -1f

                    Log.d("CAMARA_DETECT", "ID: ${cam2Info.cameraId}, Facing: $lensFacing, Focales: ${focalLengths?.joinToString()}, MinFocal: $minFocalLength, Typical: $typicalFocalLength")

                    // La lógica para decidir qué cámara es cuál (back, front, wide) se mantiene igual,
                    // pero ahora usa la variable 'lensFacing' obtenida correctamente.
                    if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                        if (foundWide == null || minFocalLength < foundWide.second) {
                            foundWide = Pair(cameraInfo, minFocalLength)
                        }
                        if (foundBack == null) { // Asignar el primero trasero como 'back' por defecto
                            foundBack = Pair(cameraInfo, typicalFocalLength)
                        } else {
                            // Lógica opcional para elegir un 'back' más "normal" si hay varios
                        }
                    } else if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                        foundFront = cameraInfo
                    }
                } catch (e: Exception) {
                    // Usamos cam2Info aquí si ya se obtuvo, si no, cameraInfo.toString()
                    val camIdForError = try { Camera2CameraInfo.from(cameraInfo).cameraId } catch (_: Exception) { cameraInfo.toString() }
                    Log.e("CAMARA_DETECT", "Error procesando características para $camIdForError", e)
                }
            } // Fin forEach

            // Creamos los Selectors basados en los CameraInfo encontrados (seleccionando por ID)
            backCameraSelector = foundBack?.first?.let { CameraSelector.Builder().addCameraFilter { infos -> infos.filter { Camera2CameraInfo.from(it).cameraId == Camera2CameraInfo.from(foundBack.first).cameraId } }.build() } ?: CameraSelector.DEFAULT_BACK_CAMERA
            frontCameraSelector = foundFront?.let { CameraSelector.Builder().addCameraFilter { infos -> infos.filter { Camera2CameraInfo.from(it).cameraId == Camera2CameraInfo.from(foundFront).cameraId } }.build() } ?: CameraSelector.DEFAULT_FRONT_CAMERA
            wideAngleCameraSelector = foundWide?.first?.let {
                // Asegurarnos que no es la misma que la 'back' normal si solo hay una
                if (foundBack?.first != foundWide.first) {
                    CameraSelector.Builder().addCameraFilter { infos -> infos.filter { Camera2CameraInfo.from(it).cameraId == Camera2CameraInfo.from(foundWide.first).cameraId } }.build()
                } else { null } // Si solo hay una trasera, no hay gran angular separada
            }


            Log.d("CAMARA_DETECT", "Back selector: ${backCameraSelector != null}, Front selector: ${frontCameraSelector != null}, Wide selector: ${wideAngleCameraSelector != null}")

            // Establecemos la cámara activa inicial (la trasera por defecto)
            activeCameraSelector = backCameraSelector ?: CameraSelector.DEFAULT_BACK_CAMERA

        } catch (e: Exception) {
            Log.e("CAMARA_DETECT", "Error detectando cámaras", e)
            // Usar defaults si falla la detección
            backCameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            frontCameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
            activeCameraSelector = backCameraSelector
        }
    }
    // --- Fin Permiso ---

    // --- Lógica y UI de CameraX (solo si hay permiso) ---
    // Usaremos estos estados/objetos más adelante para la captura
    // val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    // val cameraProvider: ProcessCameraProvider? = cameraProviderFuture.get() // Podría bloquear, mejor en LaunchedEffect


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Exp: ${expedienteNombre ?: "Desconocido"}") }, // Mostramos ID carpeta
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    // Botón para alternar Trasera/Frontal
                    val canSwitchFrontBack = frontCameraSelector != null && backCameraSelector != null
                    IconButton(
                        onClick = {
                            activeCameraSelector = if (activeCameraSelector == backCameraSelector) {
                                frontCameraSelector ?: CameraSelector.DEFAULT_FRONT_CAMERA // Fallback
                            } else {
                                backCameraSelector ?: CameraSelector.DEFAULT_BACK_CAMERA // Fallback
                            }
                        },
                        enabled = canSwitchFrontBack // Solo si tenemos ambas
                    ) {
                        Icon(Icons.Filled.SwitchCamera, "Alternar Cámara")
                    }

                    // Botón para Gran Angular (si existe y NO está activa)
                    if (wideAngleCameraSelector != null && activeCameraSelector != wideAngleCameraSelector) {
                        IconButton(onClick = { activeCameraSelector = wideAngleCameraSelector!! }) {
                            Icon(Icons.Filled.CameraOutdoor, "Gran Angular") // O el icono que elegiste
                        }
                    }

                    // Botón para volver a Trasera Normal (si estamos en Gran Angular y la normal existe)
                    if (activeCameraSelector == wideAngleCameraSelector && backCameraSelector != null) {
                        IconButton(onClick = { activeCameraSelector = backCameraSelector!! }) {
                            Icon(Icons.Filled.CameraRear, "Cámara Normal") // Icono para volver a normal
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        // --- ENVOLTORIO BoxWithConstraints ---
        BoxWithConstraints(
            modifier = Modifier
                .padding(innerPadding) // Aplicamos padding del Scaffold aquí
                .fillMaxSize()
        ) {
            // Obtenemos la densidad de píxeles actual de la pantalla
            val density = LocalDensity.current
            // Convertimos 600.dp a Píxeles (Int) usando la densidad y comparamos
            val isWideScreen = constraints.maxWidth > with(density) { 600.dp.roundToPx() }

            // Box principal para superponer Preview y Controles
            Box(modifier = Modifier.fillMaxSize()) {
                if (hasCamPermission) {
                    // --- Vista Previa (Tu código AndroidView) ---
                    AndroidView(
                        factory = { ctx ->
                            Log.d("CAMARA_PREVIEW", "AndroidView Factory: Creando PreviewView")
                            PreviewView(ctx).apply {
                                this.scaleType = PreviewView.ScaleType.FILL_CENTER
                                // ASIGNAMOS el controlador aquí, como antes
                                this.controller = cameraController
                            }
                        },
                        // --- BLOQUE UPDATE ---
                        // Este bloque se llama cuando el factory termina y CADA VEZ que
                        // uno de los estados leídos aquí dentro ('activeCameraSelector') cambia.
                        update = { previewView -> // previewView aquí es la instancia creada en factory
                            val selectorToSet = activeCameraSelector ?: CameraSelector.DEFAULT_BACK_CAMERA // Usamos un default seguro
                            Log.d("CAMARA_PREVIEW", "AndroidView Update - Intentando aplicar Selector: $selectorToSet")
                            try {
                                // 1. (Re)Asegurar la vinculación al Lifecycle ANTES de usar el selector
                                cameraController.bindToLifecycle(lifecycleOwner)
                                Log.d("CAMARA_PREVIEW", "Update: bindToLifecycle llamado/reafirmado.")

                                // 2. Asignar el selector al controlador
                                //    Ahora debería saber que está vinculado al lifecycle
                                cameraController.cameraSelector = selectorToSet
                                Log.d("CAMARA_PREVIEW", "Update: cameraSelector asignado.")

                            } catch (e: Exception) {
                                Log.e("CAMARA_PREVIEW", "Error en bloque update de AndroidView al vincular/asignar selector", e)
                                // Podríamos mostrar un Toast o actualizar estado de error aquí si falla
                                Toast.makeText(context, "Error interno cámara: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        // --- FIN BLOQUE UPDATE ---
                        modifier = Modifier.fillMaxSize()
                    )
                    // --- Fin Vista Previa ---

                    // --- CONTROLES (Posición Condicional) ---
                    if (isWideScreen) {
                        // --- Layout ANCHO: Columna a la Derecha ---
                        Column(
                            modifier = Modifier
                                .align(Alignment.CenterEnd) // Alinea columna a la derecha
                                .padding(16.dp)             // Padding exterior
                                .fillMaxHeight(),           // Ocupa altura
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceEvenly // Espacia botones
                        ) {
                            // --- Icono Galería (Tu código con tu lógica onClick) ---
                            IconButton(
                                modifier = Modifier.size(56.dp),
                                onClick = {
                                    Log.d("NAV", "Navegando a galería para Exp: $expedienteNombre Carp: $idCarpetaDrive")
                                    if (!idCarpetaDrive.isNullOrBlank() && !expedienteNombre.isNullOrBlank()) {
                                        navController.navigate("pantalla_galeria/$idCarpetaDrive/$expedienteNombre")
                                    } else {
                                        Log.w("NAV", "Faltan datos para navegar a galería.")
                                        Toast.makeText(context, "Error: Falta info del expediente", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.PhotoLibrary,
                                    contentDescription = "Ver Galería del Expediente",
                                    tint = MaterialTheme.colorScheme.primary, // Tu color elegido
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                            // --- Fin Icono Galería ---

                            // --- Botón Captura (Tu código con tu lógica onClick llamando a takePhoto) ---
                            IconButton(
                                modifier = Modifier.size(72.dp),
                                onClick = {
                                    takePhoto( // Tu función helper
                                        context = context,
                                        cameraController = cameraController,
                                        idCarpetaDrive = idCarpetaDrive,
                                        onImageSaved = { uri, photoFile -> // <--- LA LAMBDA AHORA RECIBE uri y photoFile
                                            Log.d("CAMARA", "Foto guardada localmente: $uri, Nombre archivo: ${photoFile.name}")
                                            // El Toast.makeText(context, "Foto guardada en: $uri", Toast.LENGTH_SHORT).show() lo podemos quitar si prefieres,
                                            // ya que la vibración y la actualización de la miniatura dan feedback. O mantenerlo.

                                            // Lógica de Vibración (igual que la tenías, funciona bien)
                                            try {
                                                val vibrator: android.os.Vibrator?
                                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                                                    val vibratorManager =
                                                        context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager
                                                    vibrator = vibratorManager.defaultVibrator
                                                } else {
                                                    @Suppress("DEPRECATION")
                                                    vibrator =
                                                        context.getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator
                                                }
                                                if (vibrator?.hasVibrator() == true) {
                                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                                        vibrator.vibrate(
                                                            android.os.VibrationEffect.createOneShot(
                                                                80, // Duración de la vibración
                                                                android.os.VibrationEffect.DEFAULT_AMPLITUDE
                                                            )
                                                        )
                                                    } else {
                                                        @Suppress("DEPRECATION")
                                                        vibrator.vibrate(80)
                                                    }
                                                } else {
                                                    Log.w("VIBRACION", "No se obtuvo vibrador o el dispositivo no puede vibrar.")
                                                }
                                            } catch (e: Exception) {
                                                Log.e("VIBRACION", "Error al intentar vibrar", e)
                                            }

                                            // Actualizar la URI de la miniatura (esto sigue igual)
                                            lastPhotoUri = uri

                                            // --- ¡NUEVO! Llamar al ViewModel para que guarde en Room ---
                                            // Necesitamos idCarpetaDrive (que CameraScreen ya recibe como parámetro)
                                            if (!idCarpetaDrive.isNullOrBlank()) {
                                                // El nombre de la carpeta de fecha lo podemos obtener del parentFile de photoFile
                                                val dateFolderName = photoFile.parentFile?.name ?: ""
                                                // Comprobamos que dateFolderName no esté vacío (por si acaso photoFile.parentFile fuera null)
                                                if (dateFolderName.isNotBlank()){
                                                    viewModel.registrarNuevaFotoLocal(
                                                        localUri = uri.toString(),
                                                        fileName = photoFile.name, // Nombre del archivo (ej. 20250509_070000.jpg)
                                                        idExpedienteDrive = idCarpetaDrive,
                                                        dateFolderName = dateFolderName, // Nombre de la carpeta de fecha (ej. 20250509)
                                                        isEdited = false // Una foto nueva de cámara no es una edición de otra
                                                    )
                                                } else {
                                                    Log.e("ROOM_SAVE", "No se pudo obtener dateFolderName para registrar foto en Room.")
                                                    Toast.makeText(context, "Error al registrar foto (sin carpeta fecha)", Toast.LENGTH_SHORT).show()
                                                }
                                            } else {
                                                Log.e("ROOM_SAVE", "No se puede registrar foto en Room, falta idCarpetaDrive.")
                                                Toast.makeText(context, "Error al registrar foto (sin ID Exp)", Toast.LENGTH_SHORT).show()
                                            }
                                            // --- FIN NUEVO ---

                                        }, // <-- Fin de la lambda onImageSaved
                                        onError = { exception ->
                                            Log.e("CAMARA", "Error al guardar foto:", exception)
                                            Toast.makeText(context, "Error al guardar: ${exception.message}", Toast.LENGTH_LONG).show()
                                        }
                                    )
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PhotoCamera,
                                    contentDescription = "Capturar Foto",
                                    tint = MaterialTheme.colorScheme.primary, // Tu color
                                    modifier = Modifier.size(64.dp)
                                )
                            }
                            // --- Fin Botón Captura ---

                            // --- Miniatura (Tu código if/else con AsyncImage y clickable) ---
                            if (lastPhotoUri != null) {
                                AsyncImage(
                                    model = lastPhotoUri,
                                    contentDescription = "Última foto tomada",
                                    modifier = Modifier
                                        .size(80.dp) // Tamaño miniatura
                                        .border(BorderStroke(2.dp, Color.White), RoundedCornerShape(8.dp))
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { // Tu lógica clickable
                                            lastPhotoUri?.let { uri ->
                                                val encodedUri = URLEncoder.encode(uri.toString(), StandardCharsets.UTF_8.name())
                                                Log.d("NAV", "Navegando a editor con URI: $encodedUri")
                                                if (!idCarpetaDrive.isNullOrBlank()) {
                                                    navController.navigate("pantalla_editor/$encodedUri/$idCarpetaDrive")
                                                } else { /* ... error ... */ }
                                            }
                                        },
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Spacer(Modifier.size(80.dp)) // Espacio si no hay miniatura
                            }
                            // --- Fin Miniatura ---
                        } // Fin Column Derecha
                        // --- Fin Layout Ancho ---
                        } else {

                        IconButton(
                            modifier = Modifier
                                .align(Alignment.BottomCenter) // Lo posiciona abajo centrado
                                .padding(16.dp) // Le da un margen
                                .size(72.dp), // Tamaño del botón (ajusta si quieres)
                            onClick = {
                                // Llamaremos a una función para hacer la foto
                                takePhoto(
                                    context = context,
                                    cameraController = cameraController,
                                    idCarpetaDrive = idCarpetaDrive,
                                    onImageSaved = { uri, photoFile -> // <--- LA LAMBDA AHORA RECIBE uri y photoFile
                                        Log.d("CAMARA", "Foto guardada localmente: $uri, Nombre archivo: ${photoFile.name}")
                                        // El Toast.makeText(context, "Foto guardada en: $uri", Toast.LENGTH_SHORT).show() lo podemos quitar si prefieres,
                                        // ya que la vibración y la actualización de la miniatura dan feedback. O mantenerlo.

                                        // Lógica de Vibración (igual que la tenías, funciona bien)
                                        try {
                                            val vibrator: android.os.Vibrator?
                                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                                                val vibratorManager =
                                                    context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager
                                                vibrator = vibratorManager.defaultVibrator
                                            } else {
                                                @Suppress("DEPRECATION")
                                                vibrator =
                                                    context.getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator
                                            }
                                            if (vibrator?.hasVibrator() == true) {
                                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                                    vibrator.vibrate(
                                                        android.os.VibrationEffect.createOneShot(
                                                            80, // Duración de la vibración
                                                            android.os.VibrationEffect.DEFAULT_AMPLITUDE
                                                        )
                                                    )
                                                } else {
                                                    @Suppress("DEPRECATION")
                                                    vibrator.vibrate(80)
                                                }
                                            } else {
                                                Log.w("VIBRACION", "No se obtuvo vibrador o el dispositivo no puede vibrar.")
                                            }
                                        } catch (e: Exception) {
                                            Log.e("VIBRACION", "Error al intentar vibrar", e)
                                        }

                                                // Actualizar la URI de la miniatura (esto sigue igual)
                                                lastPhotoUri = uri

                                                // --- ¡NUEVO! Llamar al ViewModel para que guarde en Room ---
                                                // Necesitamos idCarpetaDrive (que CameraScreen ya recibe como parámetro)
                                                if (!idCarpetaDrive.isNullOrBlank()) {
                                                    // El nombre de la carpeta de fecha lo podemos obtener del parentFile de photoFile
                                                    val dateFolderName = photoFile.parentFile?.name ?: ""
                                                    // Comprobamos que dateFolderName no esté vacío (por si acaso photoFile.parentFile fuera null)
                                                    if (dateFolderName.isNotBlank()){
                                                        viewModel.registrarNuevaFotoLocal(
                                                            localUri = uri.toString(),
                                                            fileName = photoFile.name, // Nombre del archivo (ej. 20250509_070000.jpg)
                                                            idExpedienteDrive = idCarpetaDrive,
                                                            dateFolderName = dateFolderName, // Nombre de la carpeta de fecha (ej. 20250509)
                                                            isEdited = false // Una foto nueva de cámara no es una edición de otra
                                                        )
                                                    } else {
                                                        Log.e("ROOM_SAVE", "No se pudo obtener dateFolderName para registrar foto en Room.")
                                                        Toast.makeText(context, "Error al registrar foto (sin carpeta fecha)", Toast.LENGTH_SHORT).show()
                                                    }
                                                } else {
                                                    Log.e("ROOM_SAVE", "No se puede registrar foto en Room, falta idCarpetaDrive.")
                                                    Toast.makeText(context, "Error al registrar foto (sin ID Exp)", Toast.LENGTH_SHORT).show()
                                                }
                                                // --- FIN NUEVO ---

                                    }, // <-- Fin de la lambda onImageSaved
                                    onError = { exception ->
                                        Log.e("CAMARA", "Error al guardar foto:", exception)
                                        Toast.makeText(
                                            context,
                                            "Error al guardar: ${exception.message}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        // TODO: Actualizar estado de error en UI?
                                    }
                                )
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PhotoCamera,
                                contentDescription = "Capturar Foto",
                                tint = MaterialTheme.colorScheme.primary, // Color del icono
                                modifier = Modifier.size(64.dp) // Tamaño del icono dentro del botón
                            )
                        }
                        if (lastPhotoUri != null) { // Solo se muestra si hemos hecho una foto
                            AsyncImage(
                                model = lastPhotoUri, // La URI de la foto guardada
                                contentDescription = "Última foto tomada",
                                modifier = Modifier
                                    .align(Alignment.BottomEnd) // Alinea abajo a la derecha
                                    .padding(16.dp) // Margen
                                    .size(80.dp) // Tamaño de la miniatura
                                    .border(
                                        BorderStroke(2.dp, Color.White),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clip(RoundedCornerShape(8.dp)) // Borde blanco opcional
                                    .clickable {
                                        lastPhotoUri?.let { uri -> // Solo navega si la URI no es null
                                            // Codificamos la URI para pasarla segura en la ruta
                                            val encodedUri = URLEncoder.encode(
                                                uri.toString(),
                                                StandardCharsets.UTF_8.name()
                                            )
                                            Log.d("NAV", "Navegando a editor con URI: $encodedUri")
                                            if (!idCarpetaDrive.isNullOrBlank()) {
                                                navController.navigate("pantalla_editor/$encodedUri/$idCarpetaDrive")
                                            } else {
                                                Log.e(
                                                    "NAV_CAM_TO_EDITOR",
                                                    "idCarpetaDrive es nulo, no se puede navegar."
                                                )
                                                Toast.makeText(
                                                    context,
                                                    "Error: Falta ID de expediente para editar",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    },
                                contentScale = ContentScale.Crop // Escala la imagen para llenar el espacio
                            )
                        }

                        IconButton(
                            modifier = Modifier
                                .align(Alignment.BottomStart) // Alinea abajo a la izquierda
                                .padding(16.dp)
                                .size(56.dp), // Un poco más pequeño que el de captura quizás
                            onClick = {
                                // Navegamos a la nueva pantalla de galería, pasando los mismos datos
                                Log.d(
                                    "NAV",
                                    "Navegando a galería para Exp: $expedienteNombre Carp: $idCarpetaDrive"
                                )
                                if (!idCarpetaDrive.isNullOrBlank() && !expedienteNombre.isNullOrBlank()) {
                                    // ¡IMPORTANTE! Usamos los parámetros que ya recibe CameraScreen
                                    navController.navigate("pantalla_galeria/$idCarpetaDrive/$expedienteNombre")
                                } else {
                                    Log.w("NAV", "Faltan datos para navegar a galería.")
                                    // Quizás mostrar un Toast
                                    Toast.makeText(
                                        context,
                                        "Error: Falta info del expediente",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PhotoLibrary, // Icono de galería
                                contentDescription = "Ver Galería del Expediente",
                                tint = MaterialTheme.colorScheme.primary, // O el color que prefieras
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }

                } else {
                    // --- Mensaje si no hay permiso ---
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ){
                        Text("Se necesita permiso de la cámara para continuar.")
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                            Text("Otorgar Permiso")
                        }
                    }
                    // --- Fin Mensaje Permiso ---
                }
            }
        } // Fin Box
    } // Fin Scaffold
} // Fin CameraScreen

@SuppressLint("ContextCastToActivity", "UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    navController: NavHostController,
    photoUriString: String?,
    idCarpetaDrive: String?,
    expedienteNombre: String?
) {
    val viewModel: ExpedientesViewModel = viewModel()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Estados del ViewModel
    val galleryPhotos by viewModel.galleryPhotos.collectAsStateWithLifecycle()
    val currentPhotoUriForVM by viewModel.currentPhotoUriForEditor.collectAsStateWithLifecycle()
    val targetIndexFromVM by viewModel.currentPhotoInGalleryIndex.collectAsStateWithLifecycle()
    val isEditing by viewModel.isEditingMode.collectAsStateWithLifecycle()
    val drawnPaths by viewModel.drawnPaths.collectAsStateWithLifecycle()
    val canUndo by viewModel.canUndo.collectAsStateWithLifecycle()
    val canRedo by viewModel.canRedo.collectAsStateWithLifecycle()
    val currentPoints by viewModel.currentPoints.collectAsStateWithLifecycle()
    val currentProps by viewModel.currentPathProperties.collectAsStateWithLifecycle()
    val errorState by viewModel.errorMessage.collectAsStateWithLifecycle()
    val hasChanges by viewModel.hasUnsavedChanges.collectAsStateWithLifecycle()
    val currentTool by viewModel.currentTool.collectAsStateWithLifecycle()
    val currentPhotoOriginalSize by viewModel.currentPhotoOriginalDimensions.collectAsStateWithLifecycle()


    var canvasDrawSize by remember { mutableStateOf<IntSize?>(null) }
    var showDiscardDialog by remember { mutableStateOf(false) }

    // --- PAGER STATE ---
    val pagerState = rememberPagerState(initialPage = 0) {
        galleryPhotos.size.coerceAtLeast(1)
    }

    val lineStartPoint by viewModel.lineStartPoint.collectAsStateWithLifecycle()
    val currentLineEndPoint by viewModel.currentLineEndPoint.collectAsStateWithLifecycle()

    val requestedOrientation by viewModel.requestedOrientationLock.collectAsStateWithLifecycle()

    // --- NUEVO: Efecto para aplicar bloqueo/desbloqueo de orientación ---
    val activity = (LocalContext.current as? Activity) // Obtenemos la Activity actual
    LaunchedEffect(requestedOrientation, activity) {
        if (activity != null) {
            Log.d("ORIENTATION_LOCK", "Aplicando orientación solicitada por VM: $requestedOrientation")
            // Aplicamos la orientación solicitada por el ViewModel a la Activity
            activity.requestedOrientation = requestedOrientation
        } else {
            Log.e("ORIENTATION_LOCK", "No se pudo obtener la Activity para cambiar orientación.")
        }
    }
// --- FIN Efecto Orientación ---

    // --- EFECTOS DE SINCRONIZACIÓN ---

    // 1. EFECTO INICIAL: Cuando entramos o cambian los argumentos de navegación
    LaunchedEffect(photoUriString, idCarpetaDrive, context) {
        Log.d("EDITOR_SCREEN_EFFECT", "EFECTO INICIAL: photoUriString=$photoUriString, idCarpetaDrive=$idCarpetaDrive")
        viewModel.initializeEditorFor(context, photoUriString, idCarpetaDrive)
    }

    // 2. EFECTO PAGER SIGUE A VM: Cuando el ViewModel tiene un nuevo índice actual, el Pager va allí
    // Necesitamos galleryPhotos aquí para el pageCount y para evitar errores si está vacía
    val galleryIsNotEmpty = galleryPhotos.isNotEmpty()

    LaunchedEffect(targetIndexFromVM, galleryIsNotEmpty, pagerState.isScrollInProgress) {
        if (galleryIsNotEmpty) {
            val gallerySize = galleryPhotos.size // Usamos el tamaño actual
            val safeTargetIndex = targetIndexFromVM.coerceIn(0, gallerySize - 1)

            if (pagerState.currentPage != safeTargetIndex && !pagerState.isScrollInProgress) {
                Log.d("EDITOR_SCREEN_EFFECT", "PAGER_SCROLL_FROM_VM: VM targetIndex=$safeTargetIndex, Pager current=${pagerState.currentPage}. Scrolling Pager.")
                scope.launch {
                    pagerState.scrollToPage(safeTargetIndex)
                }
            }
        } else if (targetIndexFromVM == 0 && !pagerState.isScrollInProgress && pagerState.currentPage != 0) {
            // Si la galería está vacía (o va a estarlo) y el target es 0, resetea el pager a 0
            Log.d("EDITOR_SCREEN_EFFECT", "PAGER_SCROLL_FROM_VM: Galería vacía/reseteando, VM targetIndex=0. Scrolling Pager a 0.")
            scope.launch {
                pagerState.scrollToPage(0)
            }
        }
    }

    // 3. EFECTO VM SIGUE A PAGER: Cuando el USUARIO hace swipe y el Pager se asienta
    LaunchedEffect(pagerState, galleryPhotos) { // Key en pagerState para acceder a snapshotFlow
        if (galleryPhotos.isEmpty()) {
            Log.d("EDITOR_SCREEN_EFFECT", "VM_FOLLOWS_PAGER: Galería vacía, no se hace nada.")
            return@LaunchedEffect
        }

        snapshotFlow { pagerState.settledPage } // Observa cuando el Pager se asienta
            .distinctUntilChanged() // Solo si el valor es realmente nuevo
            .collect { settledPageIndex ->
                // Solo actualiza el VM si el Pager se asentó en una página DIFERENTE
                // a la que el VM ya tiene como actual Y el índice es válido.
                if (settledPageIndex < galleryPhotos.size && settledPageIndex != viewModel.currentPhotoInGalleryIndex.value) {
                    Log.d("EDITOR_SCREEN_EFFECT", "PAGER_SYNC_TO_VM: Pager (user swipe) settled on $settledPageIndex. VM index was ${viewModel.currentPhotoInGalleryIndex.value}. Updating VM.")
                    viewModel.userSwipedToPhotoAtIndex(settledPageIndex, context)
                }
            }
    }

    // --- Diálogo de Descartar Cambios ---
    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Cambios sin Guardar") },
            text = { Text("¿Seguro que quieres salir? Los cambios no guardados se perderán.") },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardDialog = false
                    viewModel.clearDrawingStateInternal()
                    viewModel.setEditingMode(false)
                    navController.popBackStack()
                }) { Text("Descartar y Salir") }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) { Text("Seguir Editando") }
            }
        )
    }

    // --- Manejador del Botón Atrás del Sistema ---
    BackHandler(enabled = isEditing && hasChanges) {
        showDiscardDialog = true
    }

    // --- UI Principal ---
// --- INICIO UI CON BoxWithConstraints PARA ADAPTAR LAYOUT ---
    BoxWithConstraints {
        // Calculamos si es pantalla ancha
        val density = LocalDensity.current
        val isWideScreen = constraints.maxWidth > with(density) { 600.dp.roundToPx() }
        Log.d("LAYOUT_CHECK", "EditorScreen - maxWidth(px): ${constraints.maxWidth}, isWideScreen: $isWideScreen")

        Scaffold(
            // --- TopAppBar (Completa - TU CÓDIGO) ---
            topBar = {
                TopAppBar(
                    title = { Text("Editor: ${expedienteNombre ?: "Expediente"}") },
                    navigationIcon = {
                        IconButton(onClick = {
                            if (isEditing && hasChanges) {
                                showDiscardDialog = true
                            } else {
                                viewModel.setEditingMode(false)
                                navController.popBackStack()
                            }
                        }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.undo() }, enabled = canUndo) { Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Deshacer") }
                        IconButton(onClick = { viewModel.redo() }, enabled = canRedo) { Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Rehacer") }
                        IconButton(
                            onClick = {
                                Log.d("EDITOR", "Botón Guardar pulsado.")
                                val uriToSave = viewModel.currentPhotoUriForEditor.value
                                val originalDims = viewModel.currentPhotoOriginalDimensions.value
                                val currentIndex = viewModel.currentPhotoInGalleryIndex.value
                                val esUriNoNula = uriToSave != null
                                val sonDimensionesNoNulas = originalDims != null
                                val esCanvasNoNulo = canvasDrawSize != null

                                Log.d("EDITOR_SAVE_CLICK", "Valores ANTES del IF: URI=${uriToSave}, DimsOrig=${originalDims}, CanvasSize=$canvasDrawSize, Index=$currentIndex")
                                Log.d("EDITOR_SAVE_CLICK_DEBUG", "Resultados checks -> UriOK: $esUriNoNula, DimsOK: $sonDimensionesNoNulas, CanvasOK: $esCanvasNoNulo")

                                if (esUriNoNula && sonDimensionesNoNulas && esCanvasNoNulo) {
                                    Log.d("EDITOR_SAVE_CLICK_DEBUG", "Entrando al bloque IF para guardar")
                                    viewModel.saveEditedImage(
                                        context = context,
                                        originalPhotoUriString = uriToSave!!.toString(),
                                        idCarpetaDrive = idCarpetaDrive,
                                        originalPhotoIndex = currentIndex,
                                        drawnPathsToSave = drawnPaths,
                                        currentProperties = currentProps,
                                        currentPointsToSave = currentPoints,
                                        originalImageSize = originalDims!!,
                                        canvasDrawSize = canvasDrawSize!!
                                    )
                                    Toast.makeText(context, "Guardando imagen...", Toast.LENGTH_SHORT).show()
                                } else {
                                    Log.e("EDITOR_SAVE", "Faltan datos para guardar. URI: $uriToSave, DimsOrig: $originalDims, Canvas: $canvasDrawSize")
                                    Toast.makeText(context, "Error: Datos incompletos para guardar.", Toast.LENGTH_LONG).show()
                                }
                            },
                            enabled = hasChanges && isEditing
                        ) { Icon(Icons.Filled.Save, contentDescription = "Guardar") }
                    }
                )
            }, // --- Fin TopAppBar ---

            // --- BottomAppBar Condicional ---
            bottomBar = {
                if (!isWideScreen) { // SOLO si NO es pantalla ancha
                    BottomAppBar(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            // --- Botón Lápiz (Tu código) ---
                            val isPencilSelected = currentTool == ExpedientesViewModel.DrawingTool.PENCIL
                            val pencilBgColor = if (isPencilSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                            val pencilIconColor = if (isPencilSelected) MaterialTheme.colorScheme.onPrimary else LocalContentColor.current
                            IconButton(
                                onClick = { viewModel.selectTool(if (isPencilSelected) null else ExpedientesViewModel.DrawingTool.PENCIL) },
                                modifier = Modifier.size(48.dp).background(pencilBgColor, CircleShape)
                            ) { Icon(Icons.Filled.Edit, "Lápiz", tint = pencilIconColor) }

                            // --- Botón Línea (Tu código) ---
                            val isLineSelected = currentTool == ExpedientesViewModel.DrawingTool.LINE
                            val lineBgColor = if (isLineSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                            val lineIconColor = if (isLineSelected) MaterialTheme.colorScheme.onPrimary else LocalContentColor.current
                            IconButton(
                                onClick = { viewModel.selectTool(if (isLineSelected) null else ExpedientesViewModel.DrawingTool.LINE) },
                                modifier = Modifier.size(48.dp).background(lineBgColor, CircleShape)
                            ) { Icon(Icons.Filled.Timeline, "Línea Recta", tint = lineIconColor) }

                            // --- Botones Color y Grosor (Placeholders - Tu código) ---
                            IconButton(onClick = { Log.d("EDITOR", "TODO: Mostrar Selector Color") }) { Icon(Icons.Filled.Palette, "Color") }
                            IconButton(onClick = { Log.d("EDITOR", "TODO: Mostrar Selector Grosor") }) { Icon(Icons.Filled.LineWeight, "Grosor") }
                        }
                    }
                } // Fin if(!isWideScreen)
            } // --- Fin BottomAppBar ---

        ) { innerPadding -> // Contenido del Scaffold

            // --- Box Principal del Contenido ---
            Box(
                modifier = Modifier
                    .padding(innerPadding) // Aplica padding interno del Scaffold
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                // --- Pager y Canvas (Tu código) ---
                if (galleryPhotos.isNotEmpty() || currentPhotoUriForVM != null) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        userScrollEnabled = !isEditing
                    ) { pageIndex ->
                        val photoUriForThisPage = galleryPhotos.getOrNull(pageIndex) ?: currentPhotoUriForVM
                        if (photoUriForThisPage != null) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                AsyncImage( model = photoUriForThisPage, contentDescription = "Foto página $pageIndex", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit, onError = { error -> Log.e("ASYNC_IMG_ERROR", "Error cargando $photoUriForThisPage: ${error.result.throwable}") }, onSuccess = { success -> Log.d("ASYNC_IMG_SUCCESS", "Éxito cargando $photoUriForThisPage: ${success.result.dataSource}") })
                                Canvas( modifier = Modifier.fillMaxSize().onSizeChanged { newSize -> canvasDrawSize = newSize }.then( if (isEditing && photoUriForThisPage == currentPhotoUriForVM) Modifier.pointerInput(Unit){ detectDragGestures( onDragStart = { offset -> viewModel.startDrawing(offset) }, onDragCancel = { viewModel.finishCurrentPath() }, onDragEnd = { viewModel.finishCurrentPath() }, onDrag = { change, dragAmount -> viewModel.updateDrawingInProgress(change.position); change.consume() } ) } else Modifier ) ) {
                                    if (isEditing && photoUriForThisPage == currentPhotoUriForVM) {
                                        drawnPaths.forEach { pathData ->
                                            if (pathData.points.size > 1) { val path = Path().apply { moveTo(pathData.points.first().x, pathData.points.first().y); pathData.points.drop(1).forEach { lineTo(it.x, it.y) } }; drawPath( path = path, color = pathData.properties.color, style = Stroke( width = pathData.properties.strokeWidth, cap = pathData.properties.strokeCap, join = pathData.properties.strokeJoin ) ) } }
                                        if (currentTool == ExpedientesViewModel.DrawingTool.PENCIL) {
                                            // Usamos la lista de puntos actual observada del ViewModel
                                            if (currentPoints.isNotEmpty()) { // Comprobamos que la lista no esté vacía
                                                if (currentPoints.size == 1) {
                                                    // --- NUEVO: Si solo hay un punto, dibujar un círculo ---
                                                    val firstPoint = currentPoints.first()
                                                    drawCircle(
                                                        color = currentProps.color, // Color actual
                                                        radius = currentProps.strokeWidth / 2f, // Radio = mitad del grosor del trazo
                                                        center = firstPoint // Posición del primer toque
                                                        // No necesitamos style = Fill aquí, drawCircle lo rellena por defecto
                                                    )
                                                    // --- FIN NUEVO ---
                                                } else {
                                                    // --- Si hay más de un punto, dibujar la línea (Path) como antes ---
                                                    val currentDrawingPath = Path().apply {
                                                        moveTo(currentPoints.first().x, currentPoints.first().y)
                                                        // Asegurarse de usar currentPoints (la lista observada)
                                                        currentPoints.drop(1).forEach { point -> lineTo(point.x, point.y) }
                                                    }
                                                    drawPath(
                                                        path = currentDrawingPath,
                                                        color = currentProps.color,
                                                        style = Stroke(
                                                            width = currentProps.strokeWidth,
                                                            cap = currentProps.strokeCap,
                                                            join = currentProps.strokeJoin
                                                        )
                                                    )
                                                    // --- FIN LÍNEA ---
                                                }
                                            }
                                        }
                                        // --- FIN DIBUJO LÁPIZ ---
                                        if (currentTool == ExpedientesViewModel.DrawingTool.LINE && lineStartPoint != null && currentLineEndPoint != null) { drawLine( color = currentProps.color, start = lineStartPoint!!, end = currentLineEndPoint!!, strokeWidth = currentProps.strokeWidth, cap = currentProps.strokeCap ) }
                                    }
                                } // Fin Canvas
                            } // Fin Box interno página
                        } else { Text("URI no disponible página $pageIndex") }
                    } // Fin Pager
                } else { Text("No hay fotos...") }
                // --- Fin Pager y Canvas ---

                // --- COLUMNA LATERAL CONDICIONAL (AÑADIDA/CORREGIDA) ---
                if (isWideScreen) { // SOLO si SÍ es pantalla ancha
                    Column(
                        modifier = Modifier
                            .align(Alignment.CenterEnd) // A la derecha, centrada verticalmente
                            .padding(horizontal = 8.dp, vertical = 16.dp) // Padding exterior
                            .fillMaxHeight() // Ocupa toda la altura
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f), RoundedCornerShape(8.dp)) // Fondo semi-transparente opcional
                            .padding(vertical = 8.dp, horizontal = 4.dp), // Padding interior
                        horizontalAlignment = Alignment.CenterHorizontally,
                        // Espaciado entre botones y centrado vertical dentro de la columna
                        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
                    ) {
                        // --- Botón Lápiz (Copiado de tu BottomAppBar) ---
                        val isPencilSelected = currentTool == ExpedientesViewModel.DrawingTool.PENCIL
                        val pencilBgColor = if (isPencilSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                        val pencilIconColor = if (isPencilSelected) MaterialTheme.colorScheme.onPrimary else LocalContentColor.current
                        IconButton(
                            onClick = { viewModel.selectTool(if (isPencilSelected) null else ExpedientesViewModel.DrawingTool.PENCIL) },
                            modifier = Modifier.size(48.dp).background(pencilBgColor, CircleShape)
                        ) { Icon(Icons.Filled.Edit, "Lápiz", tint = pencilIconColor) }

                        // --- Botón Línea (Copiado de tu BottomAppBar) ---
                        val isLineSelected = currentTool == ExpedientesViewModel.DrawingTool.LINE
                        val lineBgColor = if (isLineSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                        val lineIconColor = if (isLineSelected) MaterialTheme.colorScheme.onPrimary else LocalContentColor.current
                        IconButton(
                            onClick = { viewModel.selectTool(if (isLineSelected) null else ExpedientesViewModel.DrawingTool.LINE) },
                            modifier = Modifier.size(48.dp).background(lineBgColor, CircleShape)
                        ) { Icon(Icons.Filled.Timeline, "Línea Recta", tint = lineIconColor) }

                        // --- Botones Color y Grosor (Placeholders - Copiados) ---
                        IconButton(onClick = { Log.d("EDITOR", "TODO: Mostrar Selector Color") }) { Icon(Icons.Filled.Palette, "Color") }
                        IconButton(onClick = { Log.d("EDITOR", "TODO: Mostrar Selector Grosor") }) { Icon(Icons.Filled.LineWeight, "Grosor") }
                    } // Fin Column lateral
                } // Fin if(isWideScreen)
                // --- FIN COLUMNA LATERAL ---

            } // Fin Box principal contenido
        } // Fin Scaffold
    } // Fin BoxWithConstraints
} // Fin EditorScreen

@OptIn(ExperimentalMaterial3Api::class) // Para TopAppBar
@Composable
fun GalleryScreen( // Nombre corregido/final
    navController: NavHostController,
    idCarpetaDrive: String?,
    expedienteNombre: String?
) {
    // --- 1. Obtener ViewModel ---
    // Se obtiene aquí dentro para que este Composable tenga acceso a él
    val viewModel: ExpedientesViewModel = viewModel()

    // --- 2. Observar Estados del ViewModel ---
    // Observamos la lista de fotos que expondrá el ViewModel
    val photos by viewModel.galleryPhotos.collectAsStateWithLifecycle()
    // Observamos también el estado de error por si falla la carga
    val errorState by viewModel.errorMessage.collectAsStateWithLifecycle()

    // --- 3. Obtener Contexto (necesario para llamar a loadGalleryPhotos) ---
    val context = LocalContext.current

    // --- 4. Efecto para Cargar Fotos al Entrar/Cambiar Expediente ---
    // Este LaunchedEffect se ejecuta cuando el Composable entra en pantalla
    // o si el valor de 'idCarpetaDrive' cambia.
    LaunchedEffect(idCarpetaDrive, context) { // Añadimos context como key también por si acaso
        if (idCarpetaDrive != null) { // Solo llamamos si tenemos un id válido
            Log.d("GALLERY_SCREEN", "LaunchedEffect: Llamando a triggerGalleryLoad para $idCarpetaDrive")
            // --- CAMBIO AQUÍ ---
            viewModel.triggerGalleryLoad(context, idCarpetaDrive)
            // --- FIN CAMBIO ---
            viewModel.clearErrorMessage() // Esto está bien aquí
        } else {
            Log.w("GALLERY_SCREEN", "LaunchedEffect: idCarpetaDrive es null, no se carga la galería.")
            // Si idCarpetaDrive es null, triggerGalleryLoad no hará nada útil
            // o podrías directamente limpiar la lista en el ViewModel si prefieres
            // viewModel.clearGalleryPhotos() // Si tuvieras una función así
        }
    }

    // --- 5. UI Principal (Scaffold) ---
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Galería Exp: ${expedienteNombre ?: "Desconocido"}") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) { // Botón Atrás
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver a Cámara")
                    }
                }
            )
        }
    ) { innerPadding -> // Lambda de contenido del Scaffold

        // --- 6. Contenido DENTRO del Scaffold ---
        Column( // Usamos Column para poder poner texto/errores además de la cuadrícula
            modifier = Modifier
                .padding(innerPadding) // Aplica padding de TopAppBar
                .fillMaxSize()
        ){
            // Mensaje de error si existe
            if (errorState != null) {
                Text(
                    "Error: $errorState",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // Cuadrícula de fotos o mensaje de "No hay fotos"
            if (photos.isNotEmpty()) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 110.dp), // Ajusta tamaño mínimo de celda
                    modifier = Modifier.fillMaxSize(), // Ocupa todo el espacio disponible
                    contentPadding = PaddingValues(4.dp) // Espacio alrededor de la cuadrícula
                ) {
                    items(photos) { photoUri ->
                        // Celda individual con la imagen clicable
                        AsyncImage(
                            model = photoUri, // URI de la foto a cargar
                            contentDescription = "Foto del expediente",
                            modifier = Modifier
                                .padding(4.dp) // Espacio entre fotos
                                .aspectRatio(1f) // Mantiene la proporción cuadrada
                                .clickable {
                                    // Navega al editor pasando la URI (codificada)
                                    val encodedUri = URLEncoder.encode(photoUri.toString(), StandardCharsets.UTF_8.name())
                                    Log.d("NAV", "Navegando a editor desde galería con URI: $encodedUri")
                                    if (!idCarpetaDrive.isNullOrBlank()) {
                                        navController.navigate("pantalla_editor/$encodedUri/$idCarpetaDrive")
                                    } else {
                                        Log.e("NAV_CAM_TO_EDITOR", "idCarpetaDrive es nulo, no se puede navegar.")
                                        Toast.makeText(context, "Error: Falta ID de expediente para editar", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .border(BorderStroke(1.dp, Color.LightGray)), // Borde fino opcional
                            contentScale = ContentScale.Crop // Recorta para llenar el cuadrado
                        )
                    }
                }
            } else {
                // Mensaje si la lista está vacía (puede ser porque no hay o aún no ha cargado)
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No hay fotos locales para este expediente.")
                    // Aquí podríamos añadir un indicador de carga si quisiéramos diferenciar
                }
            }
        } // Fin Column contenido
    } // Fin Scaffold
} // Fin GalleryScreen

// Función takePhoto SIMPLIFICADA (fuera del Composable CameraScreen)
private fun takePhoto(
    context: Context,
    cameraController: LifecycleCameraController, // Recibe el Controlador
    idCarpetaDrive: String?,
    onImageSaved: (uri: Uri, photoFile: File) -> Unit,
    onError: (ImageCaptureException) -> Unit
) {
    // YA NO NECESITAMOS COMPROBAR imageCapture
    // if (imageCapture == null) { ... return } // <-- BORRAR ESTE BLOQUE IF

    // La comprobación del idCarpetaDrive sigue siendo importante
    if (idCarpetaDrive.isNullOrBlank()){
        Log.e("CAMARA", "ID de Carpeta Drive es nulo o vacío. No se puede guardar la foto.")
        Toast.makeText(context, "Error: Falta ID de expediente.", Toast.LENGTH_SHORT).show()
        return
    }

    // 2. Crear directorio y archivo de salida (igual que antes)
    val baseDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    val expedienteDirName = idCarpetaDrive.replace(Regex("[^a-zA-Z0-9.-]"), "_")
    val dateFolderName = SimpleDateFormat("yyyyMMdd", Locale.US).format(System.currentTimeMillis())
    val expedienteDir = File(baseDir, expedienteDirName)
    val dateDir = File(expedienteDir, dateFolderName)
    if (!dateDir.exists()) {
        val success = dateDir.mkdirs()
        if (!success) {
            Log.e("CAMARA", "ERROR al crear directorio de fecha: ${dateDir.absolutePath}")
            onError(ImageCaptureException(ImageCapture.ERROR_FILE_IO, "No se pudo crear el directorio de fecha", null))
            return
        } else {
            Log.d("CAMARA", "Creado directorio de fecha: ${dateDir.absolutePath}")
        }
    }
    val photoFileName = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis()) + ".jpg"
    val photoFile = File(dateDir, photoFileName)

    // 3. Crear opciones de salida (igual que antes)
    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

    // 4. Configurar el callback (igual que antes)
    val imageSavedCallback = object : ImageCapture.OnImageSavedCallback {
        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
            val savedUri = outputFileResults.savedUri ?: Uri.fromFile(photoFile)
            onImageSaved(savedUri, photoFile) // Llama al callback de éxito con la URI
        }
        override fun onError(exception: ImageCaptureException) {
            onError(exception) // Llama al callback de error con la excepción
        }
    }

    // 5. ¡Hacer la foto usando el cameraController! (igual que antes)
    Log.d("CAMARA", "Intentando capturar foto con Controller en: ${photoFile.absolutePath}")
    cameraController.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context), // Executor
        imageSavedCallback // Callback
    )
}

// ========================================================================
// Composable para la fila de Expediente (Añadido fillMaxWidth)
// ========================================================================
@Composable
fun ExpedienteRow(expediente: Expediente) {
    // Añadimos fillMaxWidth para que ocupe todo el ancho y el clickable funcione bien
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth()) {
        Text(
            text = expediente.nombre,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = expediente.direccion,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}