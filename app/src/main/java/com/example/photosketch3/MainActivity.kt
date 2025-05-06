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
                        route = "pantalla_editor/{photoUri}",
                        arguments = listOf(navArgument("photoUri") { type = NavType.StringType })
                    ) { backStackEntry ->
                        // Obtenemos la URI codificada y la decodificamos
                        val encodedUri = backStackEntry.arguments?.getString("photoUri")
                        val photoUriString = encodedUri?.let { URLDecoder.decode(it, StandardCharsets.UTF_8.name()) }
                        EditorScreen(
                            navController = navController,
                            photoUriString = photoUriString // Pasamos la URI como String
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
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) { // Usamos Box para superponer botón más tarde
            if (hasCamPermission) {
                // --- Vista Previa de la Cámara ---
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = androidx.camera.core.Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }
                            // TODO: Configurar ImageCapture aquí también
                            // Creamos el caso de uso ImageCapture
                            val builtImageCapture = ImageCapture.Builder()
                                // Aquí podrías configurar opciones como flash, etc. más adelante
                                .build()
                            // Guardamos la instancia en nuestro estado
                            imageCapture = builtImageCapture

                            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                            try {
                                // Desvincular antes de volver a vincular
                                cameraProvider.unbindAll()
                                // Vincular casos de uso al ciclo de vida
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner, cameraSelector, preview , builtImageCapture // <-- Añadir imageCapture aquí luego
                                )
                                Log.d("CAMARA", "CameraX vinculado (Preview + ImageCapture).")
                            } catch(exc: Exception) {
                                Log.e("CAMARA", "Fallo al vincular CameraX", exc)
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                        previewView // Devolvemos la vista
                    },
                    modifier = Modifier.fillMaxSize()
                )
                // --- Fin Vista Previa ---

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
                                        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager
                                        vibrator = vibratorManager.defaultVibrator
                                    } else {
                                        // Forma antigua para versiones anteriores a Android 12
                                        @Suppress("DEPRECATION") // Suprimimos el aviso aquí porque es intencionado
                                        vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator
                                    }

                                    // Comprobamos si obtuvimos un vibrador y si el dispositivo puede vibrar
                                    if (vibrator?.hasVibrator() == true) {
                                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                            // Para Android 8.0 (API 26) o superior - Usar VibrationEffect
                                            vibrator.vibrate(android.os.VibrationEffect.createOneShot(80, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
                                        } else {
                                            // Para versiones anteriores a Android 8.0 (obsoleto pero necesario)
                                            @Suppress("DEPRECATION")
                                            vibrator.vibrate(80) // Vibra por 80 milisegundos
                                        }
                                    } else {
                                        Log.w("VIBRACION", "No se obtuvo vibrador o el dispositivo no puede vibrar.")
                                    }
                                } catch (e: Exception) {
                                    Log.e("VIBRACION", "Error al intentar vibrar", e)
                                }
                                lastPhotoUri = uri
                                // TODO: Guardar uri para mostrar thumbnail o subir a Drive
                            },
                            onError = { exception ->
                                Log.e("CAMARA", "Error al guardar foto:", exception)
                                Toast.makeText(context, "Error al guardar: ${exception.message}", Toast.LENGTH_LONG).show()
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
                                    val encodedUri = URLEncoder.encode(uri.toString(), StandardCharsets.UTF_8.name())
                                    Log.d("NAV", "Navegando a editor con URI: $encodedUri")
                                    navController.navigate("pantalla_editor/$encodedUri")
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
                        Log.d("NAV", "Navegando a galería para Exp: $expedienteNombre Carp: $idCarpetaDrive")
                        if (!idCarpetaDrive.isNullOrBlank() && !expedienteNombre.isNullOrBlank()) {
                            // ¡IMPORTANTE! Usamos los parámetros que ya recibe CameraScreen
                            navController.navigate("pantalla_galeria/$idCarpetaDrive/$expedienteNombre")
                        } else {
                            Log.w("NAV", "Faltan datos para navegar a galería.")
                            // Quizás mostrar un Toast
                            Toast.makeText(context, "Error: Falta info del expediente", Toast.LENGTH_SHORT).show()
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
        } // Fin Box
    } // Fin Scaffold
} // Fin CameraScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen( // Nombre actualizado
    navController: NavHostController,
    photoUriString: String?
) {
    val viewModel: ExpedientesViewModel = viewModel()
    val context = LocalContext.current // Podríamos necesitarlo luego
    val photoUri = remember(photoUriString) { // Convertimos String a Uri de forma segura
        photoUriString?.let { Uri.parse(it) }
    }

    // --- OBSERVAR ESTADOS DE DIBUJO DEL VIEWMODEL ---
    val drawnPaths by viewModel.drawnPaths.collectAsStateWithLifecycle()
    val canUndo by viewModel.canUndo.collectAsStateWithLifecycle()
    val canRedo by viewModel.canRedo.collectAsStateWithLifecycle()

    val currentPoints by viewModel.currentPoints.collectAsStateWithLifecycle()
    val currentProps by viewModel.currentPathProperties.collectAsStateWithLifecycle()

    // Lista mutable para guardar todos los trazos completados
    // Usamos mutableStateListOf para que Compose reaccione a los cambios
    val paths = remember { mutableStateListOf<PathData>() }
    // Lista para trazos deshechos (para poder Rehacer)
    val undonePaths = remember { mutableStateListOf<PathData>() }

    // --- Limpiar estado al entrar (IMPORTANTE) ---
    LaunchedEffect(key1 = photoUriString) {
        viewModel.prepareEditor(photoUriString)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Editor") }, // Título simple por ahora
                navigationIcon = {
                    // TODO: Añadir lógica para detectar cambios antes de volver
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = { // Iconos de acción en la barra superior
                    IconButton(
                        onClick = { viewModel.undo() },
                        enabled = canUndo // Habilitado solo si se puede deshacer
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Deshacer")
                    }
                    IconButton(
                        onClick = { viewModel.redo() },
                        enabled = canRedo // Habilitado solo si se puede rehacer
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Rehacer")
                    }
                    // --- Icono de Guardar ---
                    IconButton(onClick = {
                        Log.d("EDITOR", "TODO: Implementar Guardar")
                        Toast.makeText(context, "TODO: Guardar", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Filled.Save, contentDescription = "Guardar Cambios")
                    }
                }
            )
        },
        // --- Barra de Herramientas Inferior (Ejemplo) ---
        bottomBar = {
            BottomAppBar(
                containerColor = MaterialTheme.colorScheme.surfaceVariant // Un color de fondo
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround // Espacia los iconos
                ) {
                    // Icono Lápiz
                    IconButton(onClick = { Log.d("EDITOR", "TODO: Seleccionar Lápiz") }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Lápiz")
                    }
                    // Icono Línea
                    IconButton(onClick = { Log.d("EDITOR", "TODO: Seleccionar Línea") }) {
                        Icon(Icons.Filled.Timeline, contentDescription = "Línea Recta")
                    }
                    // Icono Color
                    IconButton(onClick = { Log.d("EDITOR", "TODO: Mostrar Selector Color") }) {
                        Icon(Icons.Filled.Palette, contentDescription = "Color")
                    }
                    // Icono Grosor
                    IconButton(onClick = { Log.d("EDITOR", "TODO: Mostrar Selector Grosor") }) {
                        Icon(Icons.Filled.LineWeight, contentDescription = "Grosor")
                    }
                }
            }
        }
        // --- Fin Barra Inferior ---
    ) { innerPadding -> // Contenido principal del Scaffold

        // --- Lienzo Principal (Imagen + Dibujo) ---
        Box(
            modifier = Modifier
                .padding(innerPadding) // IMPORTANTE: Aplicar padding del Scaffold
                .fillMaxSize(),
            contentAlignment = Alignment.Center // Centra la imagen si es más pequeña
        ) {
            if (photoUri != null) {
                // Mostramos la imagen de fondo sobre la que dibujaremos
                AsyncImage(
                    model = photoUri,
                    contentDescription = "Foto para editar",
                    modifier = Modifier.fillMaxSize(), // Ocupa todo el espacio disponible
                    contentScale = ContentScale.Fit // Ajusta para verla entera sin recortar
                )

                // TODO: AQUÍ AÑADIREMOS EL CANVAS PARA DIBUJAR ENCIMA
                Canvas(
                    modifier = Modifier
                        .fillMaxSize() // Ocupa todo el espacio disponible
                        .pointerInput(Unit) { // Detecta la entrada del puntero (dedo/stylus)
                            detectDragGestures(
                                onDragStart = { offset ->
                                    viewModel.startDrawing(offset) // <-- LLAMAR A VIEWMODEL
                                },
                                onDragCancel = {
                                    viewModel.finishCurrentPath() // Limpia puntos si se cancela
                                },
                                onDragEnd = {
                                    viewModel.finishCurrentPath() // <-- LLAMAR A VIEWMODEL
                                    // TODO: Marcar cambios sin guardar
                                },
                                onDrag = { change, dragAmount ->
                                    viewModel.addPointToCurrentPath(change.position) // <-- LLAMAR A VIEWMODEL
                                    change.consume()
                                }
                            )
                        } // Fin pointerInput
                ) { // Lambda onDraw del Canvas: Aquí es donde realmente se dibuja
                    // 1. Dibujar los trazos COMPLETADOS que vienen del ViewModel
                    // Usamos la variable 'drawnPaths' que observa el StateFlow del ViewModel
                    drawnPaths.forEach { pathData ->
                        drawPath(
                            path = pathData.path,
                            color = pathData.properties.color,
                            style = Stroke(
                                width = pathData.properties.strokeWidth,
                                cap = pathData.properties.strokeCap,
                                join = pathData.properties.strokeJoin
                            )
                        )
                    }

                    // 2. Dibujar el trazo ACTUAL que se está haciendo (basado en currentPoints)
                    // Solo dibujamos si hay al menos 2 puntos para formar una línea
                    if (currentPoints.size > 1) {
                        // Construimos un Path temporal al vuelo para el dibujo en vivo
                        val currentDrawingPath = Path().apply {
                            moveTo(currentPoints.first().x, currentPoints.first().y)
                            currentPoints.drop(1).forEach { lineTo(it.x, it.y) }
                        }
                        // Dibujamos este Path temporal con las propiedades actuales
                        drawPath(
                            path = currentDrawingPath,
                            color = currentProps.color, // Usa currentProps del VM
                            style = Stroke(
                                width = currentProps.strokeWidth, // Usa currentProps del VM
                                cap = currentProps.strokeCap,
                                join = currentProps.strokeJoin
                            )
                        )
                        // Log opcional para ver cuándo se redibuja el trazo actual
                        // Log.d("EDITOR_DRAW", "Dibujando trazo actual con ${currentPoints.size} puntos")
                    }
                } // Fin Canvas onDraw

            } else {
                // Mensaje si la URI es nula (no debería pasar si la navegación funciona)
                Text("Error: No se pudo cargar la imagen.")
            }
        }
        // --- Fin Lienzo ---
    } // Fin contenido Scaffold
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
    LaunchedEffect(idCarpetaDrive) {
        viewModel.loadGalleryPhotos(context, idCarpetaDrive)
        // Limpiamos cualquier error previo al intentar cargar
        viewModel.clearErrorMessage()
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
                                    navController.navigate("pantalla_editor/$encodedUri")
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