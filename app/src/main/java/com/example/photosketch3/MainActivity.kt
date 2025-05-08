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
            Log.d("SHEETS_API_CONSENT", "Permiso concedido, reintentando carga.")
            // Reintentamos usando el userState que AHORA debería estar actualizado
            viewModel.cargarExpedientes(context, userState?.id)
        } else {
            Log.w("SHEETS_API_CONSENT", "Permiso denegado por el usuario.")
            viewModel.setErrorMessage("Se necesita permiso para leer hojas de cálculo.")
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
                        viewModel.cargarExpedientes(context, googleIdTokenCredential.id)
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
@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(navController: NavHostController, idCarpetaDrive: String?, expedienteNombre: String?) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

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

    // Estado para guardar la referencia al caso de uso ImageCapture
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }

    // Para mostrar la miniatura de la última foto
    var lastPhotoUri by remember { mutableStateOf<Uri?>(null) }

    // Efecto para pedir permiso si no lo tenemos al entrar
    LaunchedEffect(key1 = true) { // Se ejecuta solo una vez al entrar al composable
        if (!hasCamPermission) {
            Log.d("CAMARA", "Permiso de cámara no concedido, solicitando...")
            permissionLauncher.launch(Manifest.permission.CAMERA)
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
                            val previewView = PreviewView(ctx)
                            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                            cameraProviderFuture.addListener({
                                val cameraProvider = cameraProviderFuture.get()
                                val preview = androidx.camera.core.Preview.Builder().build().also {
                                    it.setSurfaceProvider(previewView.surfaceProvider)
                                }
                                val builtImageCapture = ImageCapture.Builder().build()
                                imageCapture = builtImageCapture // Guardamos instancia
                                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                                try {
                                    cameraProvider.unbindAll()
                                    cameraProvider.bindToLifecycle(
                                        lifecycleOwner, cameraSelector, preview , builtImageCapture
                                    )
                                    Log.d("CAMARA", "CameraX vinculado (Preview + ImageCapture).")
                                } catch(exc: Exception) { Log.e("CAMARA", "Fallo al vincular CameraX", exc) }
                            }, ContextCompat.getMainExecutor(ctx))
                            previewView
                        },
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
                                        imageCapture = imageCapture,
                                        idCarpetaDrive = idCarpetaDrive,
                                        onImageSaved = { uri ->
                                            Log.d("CAMARA", "Foto guardada correctamente en: $uri")
                                            // Vibración (tu código)
                                            try {
                                                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator // etc...
                                                if (vibrator?.hasVibrator() == true) {
                                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                        vibrator.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
                                                    } else {
                                                        @Suppress("DEPRECATION")
                                                        vibrator.vibrate(80)
                                                    }
                                                }
                                            } catch (e: Exception) { Log.e("VIBRACION", "Error", e) }
                                            lastPhotoUri = uri // Actualiza estado miniatura
                                        },
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
                                    imageCapture = imageCapture, // Pasamos la instancia que guardamos en el estado
                                    idCarpetaDrive = idCarpetaDrive,
                                    onImageSaved = { uri ->
                                        Log.d("CAMARA", "Foto guardada correctamente en: $uri")
                                        // Toast.makeText(context, "Foto guardada en: $uri", Toast.LENGTH_SHORT).show()
                                        try {
                                            val vibrator: android.os.Vibrator? // Declaramos la variable fuera

                                            // Comprobamos la versión de Android del dispositivo
                                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                                                // Forma moderna para Android 12 (API 31) o superior
                                                val vibratorManager =
                                                    context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager
                                                vibrator = vibratorManager.defaultVibrator
                                            } else {
                                                // Forma antigua para versiones anteriores a Android 12
                                                @Suppress("DEPRECATION") // Suprimimos el aviso aquí porque es intencionado
                                                vibrator =
                                                    context.getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator
                                            }

                                            // Comprobamos si obtuvimos un vibrador y si el dispositivo puede vibrar
                                            if (vibrator?.hasVibrator() == true) {
                                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                                    // Para Android 8.0 (API 26) o superior - Usar VibrationEffect
                                                    vibrator.vibrate(
                                                        android.os.VibrationEffect.createOneShot(
                                                            80,
                                                            android.os.VibrationEffect.DEFAULT_AMPLITUDE
                                                        )
                                                    )
                                                } else {
                                                    // Para versiones anteriores a Android 8.0 (obsoleto pero necesario)
                                                    @Suppress("DEPRECATION")
                                                    vibrator.vibrate(80) // Vibra por 80 milisegundos
                                                }
                                            } else {
                                                Log.w(
                                                    "VIBRACION",
                                                    "No se obtuvo vibrador o el dispositivo no puede vibrar."
                                                )
                                            }
                                        } catch (e: Exception) {
                                            Log.e("VIBRACION", "Error al intentar vibrar", e)
                                        }
                                        lastPhotoUri = uri
                                        // TODO: Guardar uri para mostrar thumbnail o subir a Drive
                                    },
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

@SuppressLint("ContextCastToActivity")
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
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Editor: ${expedienteNombre ?: "Expediente"}") }, // Usa el nombre del expediente
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
                    IconButton(onClick = { viewModel.undo() }, enabled = canUndo) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Deshacer")
                    }
                    IconButton(onClick = { viewModel.redo() }, enabled = canRedo) {
                        Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Rehacer")
                    }
                    IconButton(
                        onClick = {
                            Log.d("EDITOR", "Botón Guardar pulsado.")
                            // Obtenemos los valores ACTUALES de los estados del ViewModel
                            val uriToSave = viewModel.currentPhotoUriForEditor.value
                            val originalDims = viewModel.currentPhotoOriginalDimensions.value
                            // canvasDrawSize es el estado local de EditorScreen
                            // --- ¡AÑADIR OBTENCIÓN DEL ÍNDICE AQUÍ! ---
                            val currentIndex = viewModel.currentPhotoInGalleryIndex.value
                            // --- FIN OBTENCIÓN ÍNDICE ---

                            Log.d("EDITOR_SAVE_CLICK", "Valores ANTES del IF: URI=${uriToSave}, DimsOrig=${originalDims}, CanvasSize=$canvasDrawSize, Index=$currentIndex")

                            // Comprobaciones de nulidad (igual que antes)
                            val esUriNoNula = uriToSave != null
                            val sonDimensionesNoNulas = originalDims != null
                            val esCanvasNoNulo = canvasDrawSize != null
                            Log.d("EDITOR_SAVE_CLICK_DEBUG", "Resultados checks -> UriOK: $esUriNoNula, DimsOK: $sonDimensionesNoNulas, CanvasOK: $esCanvasNoNulo")

                            if (esUriNoNula && sonDimensionesNoNulas && esCanvasNoNulo) {
                                Log.d("EDITOR_SAVE_CLICK_DEBUG", "Entrando al bloque IF para guardar")
                                viewModel.saveEditedImage(
                                    context = context,
                                    originalPhotoUriString = uriToSave!!.toString(),
                                    idCarpetaDrive = idCarpetaDrive,
                                    originalPhotoIndex = currentIndex, // <-- PASAR EL ÍNDICE AQUÍ
                                    drawnPathsToSave = drawnPaths,
                                    currentProperties = currentProps,
                                    currentPointsToSave = currentPoints,
                                    originalImageSize = originalDims!!,
                                    canvasDrawSize = canvasDrawSize!!
                                )
                                Toast.makeText(context, "Guardando imagen...", Toast.LENGTH_SHORT).show()
                            } else {
                                Log.e("EDITOR_SAVE", "Faltan datos para guardar. URI: $uriToSave, DimsOrig: $originalDims, Canvas: $canvasDrawSize")
                                Toast.makeText(context, "Error: No se pueden obtener datos completos para guardar.", Toast.LENGTH_LONG).show()
                            }
                        },
                        enabled = hasChanges && isEditing
                    ) { Icon(Icons.Filled.Save, contentDescription = "Guardar") }
                }
            )
        },
        bottomBar = {
            BottomAppBar(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    // --- Botón Lápiz ---
                    val isPencilSelected = currentTool == ExpedientesViewModel.DrawingTool.PENCIL
                    val pencilBgColor = if (isPencilSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                    val pencilIconColor = if (isPencilSelected) MaterialTheme.colorScheme.onPrimary else LocalContentColor.current
                    val isLineSelected = currentTool == ExpedientesViewModel.DrawingTool.LINE
                    val lineButtonBackgroundColor = if (isLineSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                    val lineIconTintColor = if (isLineSelected) MaterialTheme.colorScheme.onPrimary else LocalContentColor.current
                    IconButton(
                        onClick = { viewModel.selectTool(if (isPencilSelected) null else ExpedientesViewModel.DrawingTool.PENCIL) },
                        modifier = Modifier.size(48.dp).background(pencilBgColor, CircleShape)
                    ) { Icon(Icons.Filled.Edit, "Lápiz", tint = pencilIconColor) }

                    // --- Otros botones de herramientas (placeholders) ---
                    IconButton(
                        onClick = {
                            if (isLineSelected) {
                                viewModel.selectTool(null) // Intenta deseleccionar
                            } else {
                                viewModel.selectTool(ExpedientesViewModel.DrawingTool.LINE) // Selecciona Línea
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .background(color = lineButtonBackgroundColor, shape = CircleShape)
                    ) {
                        Icon(
                            // Elige el icono que más te guste para línea recta
                            imageVector = Icons.Filled.Timeline, // O Icons.Outlined.HorizontalRule, Icons.Filled.Timeline, etc.
                            contentDescription = "Línea Recta",
                            tint = lineIconTintColor
                        )
                    }
                    IconButton(onClick = { Log.d("EDITOR", "TODO: Mostrar Selector Color") }) {
                        Icon(Icons.Filled.Palette, "Color")
                    }
                    IconButton(onClick = { Log.d("EDITOR", "TODO: Mostrar Selector Grosor") }) {
                        Icon(Icons.Filled.LineWeight, "Grosor")
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding) // Aplicar el padding del Scaffold es MUY IMPORTANTE
                .fillMaxSize(),
            contentAlignment = Alignment.Center // Centra el contenido si es más pequeño que el Box
        ) {
            // Solo intentamos mostrar el Pager si tenemos una galería con fotos
            // o al menos una URI de foto actual en el ViewModel (ej. la que vino de la cámara)
            if (galleryPhotos.isNotEmpty() || currentPhotoUriForVM != null) {
                HorizontalPager(
                    state = pagerState, // El PagerState que creamos antes
                    modifier = Modifier.fillMaxSize(),
                    userScrollEnabled = !isEditing // El swipe con el dedo solo funciona si NO estamos editando
                ) { pageIndex -> // Esta lambda se ejecuta para CADA página del Pager

                    // Obtenemos la URI para esta página específica.
                    // Si galleryPhotos está vacía pero currentPhotoUriForVM tiene algo (ej. foto de cámara no guardada aún en lista de galería), usa esa.
                    val photoUriForThisPage = galleryPhotos.getOrNull(pageIndex) ?: currentPhotoUriForVM

                    if (photoUriForThisPage != null) {
                        // Usamos un Box interno para superponer AsyncImage y Canvas para esta página
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center // Centra AsyncImage si usa ContentScale.Fit y la imagen es más pequeña
                        ) {
                            // 1. Muestra la imagen de la página actual
                            Log.d("EDITOR_PAGER_ITEM", "Mostrando AsyncImage para página $pageIndex, URI: $photoUriForThisPage")
                            AsyncImage(
                                model = photoUriForThisPage,
                                contentDescription = "Foto para editar (Página $pageIndex)",
                                modifier = Modifier.fillMaxSize(), // La imagen intenta llenar el Box
                                contentScale = ContentScale.Fit,  // Se ajusta manteniendo proporción, puede dejar bandas
                                onError = { error -> Log.e("ASYNC_IMG_ERROR", "Error cargando $photoUriForThisPage: ${error.result.throwable}") },
                                onSuccess = { success -> Log.d("ASYNC_IMG_SUCCESS", "Éxito cargando $photoUriForThisPage: ${success.result.dataSource}") }
                            )

                            // 2. Canvas para dibujar encima
                            Canvas(
                                modifier = Modifier
                                    .fillMaxSize() // El Canvas ocupa el mismo espacio que AsyncImage
                                    .onSizeChanged { newSize -> canvasDrawSize = newSize } // Para obtener el tamaño del Canvas de dibujo
                                    .then( // Aplicamos el pointerInput condicionalmente
                                        if (isEditing && photoUriForThisPage == currentPhotoUriForVM) {
                                            // Solo permitimos dibujar si estamos en modo edición Y esta es la foto activa en el ViewModel
                                            Modifier.pointerInput(Unit) { // key1 = Unit para que el detector no se reinicie innecesariamente
                                                detectDragGestures(
                                                    onDragStart = { offset -> viewModel.startDrawing(offset) },
                                                    onDragCancel = { viewModel.finishCurrentPath() },
                                                    onDragEnd = { viewModel.finishCurrentPath() },
                                                    onDrag = { change, dragAmount ->
                                                        viewModel.updateDrawingInProgress(change.position)
                                                        change.consume()
                                                    }
                                                )
                                            }
                                        } else {
                                            Modifier // No se aplica pointerInput para dibujar si no se cumplen las condiciones
                                        }
                                    )
                            ) { // Lambda onDraw del Canvas
                                // Dibuja solo si estamos en modo edición Y esta es la foto activa en el ViewModel
                                if (isEditing && photoUriForThisPage == currentPhotoUriForVM) {
                                    // Dibujar los trazos completados
                                    drawnPaths.forEach { pathData ->
                                        if (pathData.points.size > 1) {
                                            val path = Path().apply { // Creamos el objeto Path de Compose
                                                moveTo(pathData.points.first().x, pathData.points.first().y)
                                                pathData.points.drop(1).forEach { lineTo(it.x, it.y) }
                                            }
                                            drawPath(
                                                path = path,
                                                color = pathData.properties.color,
                                                style = Stroke(
                                                    width = pathData.properties.strokeWidth,
                                                    cap = pathData.properties.strokeCap,
                                                    join = pathData.properties.strokeJoin
                                                )
                                            )
                                        }
                                    }
                                    // Dibujar el trazo actual (el que se está haciendo ahora mismo)
                                    if (currentTool == ExpedientesViewModel.DrawingTool.PENCIL && currentPoints.size > 1) {
                                        val currentDrawingPath = Path().apply {
                                            moveTo(currentPoints.first().x, currentPoints.first().y)
                                            currentPoints.drop(1).forEach { lineTo(it.x, it.y) }
                                        }
                                        drawPath(
                                            path = currentDrawingPath,
                                            color = currentProps.color, // Usa las propiedades actuales del ViewModel
                                            style = Stroke(
                                                width = currentProps.strokeWidth,
                                                cap = currentProps.strokeCap,
                                                join = currentProps.strokeJoin
                                            )
                                        )
                                    }
                                    if (currentTool == ExpedientesViewModel.DrawingTool.LINE && lineStartPoint != null && currentLineEndPoint != null) {
                                        drawLine(
                                            color = currentProps.color,
                                            start = lineStartPoint!!, // Sabemos que no es null por la condición
                                            end = currentLineEndPoint!!, // Sabemos que no es null
                                            strokeWidth = currentProps.strokeWidth,
                                            cap = currentProps.strokeCap
                                        )
                                    }
                                }
                            } // Fin Canvas
                        } // Fin Box interno de la página
                    } else {
                        // Esto se mostraría si photoUriForThisPage es null, lo cual no debería pasar
                        // si galleryPhotos no está vacía o currentPhotoUriForVM tiene valor.
                        Text("URI de foto no disponible para la página $pageIndex")
                    }
                } // Fin HorizontalPager
            } else {
                // Esto se mostraría si tanto galleryPhotos está vacía COMO currentPhotoUriForVM es null
                // (ej. al entrar por primera vez y antes de que el ViewModel cargue nada)
                Text("No hay fotos para mostrar o seleccionar.")
                // Aquí podrías poner un CircularProgressIndicator() si el ViewModel tuviera un estado de "cargando galería"
            }
        } // Fin Box principal (el que va dentro del Scaffold)
    } // Fin Scaffold
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

private fun takePhoto(
    context: Context,
    imageCapture: ImageCapture?, // Recibe la instancia de ImageCapture (puede ser null si aún no se ha inicializado)
    idCarpetaDrive: String?,
    onImageSaved: (Uri) -> Unit, // Callback para éxito (devuelve la URI del archivo guardado)
    onError: (ImageCaptureException) -> Unit // Callback para error
) {
    // 1. Comprobar si imageCapture está listo
    if (imageCapture == null) {
        Log.e("CAMARA", "ImageCapture no está listo todavía.")
        Toast.makeText(context, "La cámara no está lista.", Toast.LENGTH_SHORT).show()
        return
    }
    if (idCarpetaDrive.isNullOrBlank()){
        Log.e("CAMARA", "ID de Carpeta Drive es nulo o vacío. No se puede guardar la foto.")
        Toast.makeText(context, "Error: Falta ID de expediente.", Toast.LENGTH_SHORT).show()
        return
    }

    // 2.1. Obtener directorio base de imágenes de la app
    val baseDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)

    // 2.2. Nombre de la carpeta del expediente (asegurarse que sea válido para nombre de carpeta)
    val expedienteDirName = idCarpetaDrive.replace(Regex("[^a-zA-Z0-9.-]"), "_") // Reemplaza caracteres raros por _ (opcional pero seguro)

    // 2.3. Nombre de la carpeta de fecha (formato YYYYMMDD)
    val dateFolderName = SimpleDateFormat("yyyyMMdd", Locale.US).format(System.currentTimeMillis())

    // 2.4. Crear el objeto File para el directorio del expediente
    val expedienteDir = File(baseDir, expedienteDirName)

    // 2.5. Crear el objeto File para el directorio de fecha DENTRO del expediente
    val dateDir = File(expedienteDir, dateFolderName)

    // 2.6. ¡Importante! Crear AMBOS directorios (expediente y fecha) si no existen
    //    mkdirs() crea los directorios padres necesarios.
    if (!dateDir.exists()) {
        val success = dateDir.mkdirs()
        if (success) {
            Log.d("CAMARA", "Creado directorio de fecha: ${dateDir.absolutePath}")
        } else {
            Log.e("CAMARA", "ERROR al crear directorio de fecha: ${dateDir.absolutePath}")
            // Si falla la creación del directorio, no podemos guardar la foto ahí
            onError(ImageCaptureException(ImageCapture.ERROR_FILE_IO, "No se pudo crear el directorio de fecha", null))
            return // Salimos de la función takePhoto
        }
    }

    // 2.7. Crear el nombre de archivo único (igual que antes)
    val photoFileName = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis()) + ".jpg"

    // 2.8. Crear el objeto File para la foto DENTRO del directorio de fecha
    val photoFile = File(dateDir, photoFileName) // <-- Guardamos dentro de dateDir

    // 3. Crear opciones de salida indicando el archivo
    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

    // 4. Configurar el listener para el resultado de la captura
    val imageSavedCallback = object : ImageCapture.OnImageSavedCallback {
        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
            val savedUri = outputFileResults.savedUri ?: Uri.fromFile(photoFile)
            onImageSaved(savedUri) // Llama al callback de éxito con la URI
        }

        override fun onError(exception: ImageCaptureException) {
            onError(exception) // Llama al callback de error con la excepción
        }
    }

    // 5. ¡Hacer la foto!
    Log.d("CAMARA", "Intentando capturar foto...")
    imageCapture.takePicture(
        outputOptions, // Dónde guardar
        ContextCompat.getMainExecutor(context), // En qué hilo ejecutar el callback (el principal para Toasts)
        imageSavedCallback // El callback que definimos
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