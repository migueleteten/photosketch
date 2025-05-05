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
import androidx.compose.ui.unit.dp // Para Spacer (opcional)
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
    // var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
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

                // TODO: Añadir aquí el botón de captura superpuesto
                // Button(onClick = { /* takePhoto() */ }, modifier = Modifier.align(Alignment.BottomCenter)...)

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