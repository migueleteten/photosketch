package es.ace.photosketchapp2121

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
import androidx.lifecycle.compose.collectAsStateWithLifecycle // ¡Import CLAVE!
import androidx.lifecycle.viewmodel.compose.viewModel // ¡Import CLAVE!
import es.ace.photosketchapp2121.ui.theme.PhotoSketch3Theme // Asegúrate que el theme sea el tuyo
import es.ace.photosketchapp2121.viewmodel.ExpedientesViewModel
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult // Este puede que ya estuviera
import androidx.activity.result.contract.ActivityResultContracts // Este puede que ya estuviera
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Button
import androidx.compose.runtime.mutableStateOf // Para guardar el estado (logueado/error)
import androidx.compose.runtime.remember // Para recordar objetos
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue // Para cambiar el estado
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager // El nuevo gestor
import androidx.credentials.CustomCredential // Tipo base de credencial
import androidx.credentials.GetCredentialRequest // La solicitud
import androidx.credentials.exceptions.GetCredentialException // Excepción general
import com.google.android.libraries.identity.googleid.GetGoogleIdOption // Opción específica para Google ID
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential // La credencial específica de Google
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException // Excepción específica
import kotlinx.coroutines.launch
import android.app.Activity
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.foundation.background // Para el fondo de la barra (opcional)
import androidx.compose.material3.BottomAppBar // Alternativa para barra inferior
// imports para dibujar a mano alzada (lápiz)
import androidx.compose.ui.graphics.Path // Para guardar las líneas dibujadas
import androidx.compose.ui.graphics.drawscope.Stroke // Para el estilo del trazo
import androidx.compose.foundation.Canvas // El lienzo donde dibujaremos
import androidx.compose.ui.input.pointer.pointerInput // Para detectar gestos táctiles
import androidx.compose.foundation.gestures.detectDragGestures // El detector específico
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
// imports para iconos hacer / deshacer
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalContentColor
// paginación bonita
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.snapshotFlow
import androidx.core.net.toUri
import kotlinx.coroutines.flow.distinctUntilChanged
// bloqueo de orientación
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.platform.LocalDensity
// cámaras
import androidx.compose.material.icons.filled.SwitchCamera
import androidx.compose.material.icons.filled.CameraRear
import androidx.camera.core.CameraSelector.Builder
import androidx.compose.material.icons.filled.CameraOutdoor
import androidx.camera.camera2.interop.Camera2CameraInfo // Para acceder a características avanzadas
import android.hardware.camera2.CameraCharacteristics // Para leer características como focal length
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.CameraInfo // Para obtener info de cada cámara
import androidx.camera.view.LifecycleCameraController
import androidx.compose.foundation.Image
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.ui.res.painterResource
import androidx.compose.material.icons.filled.CloudDone // Para SYNCED
import androidx.compose.material.icons.filled.PhoneAndroid // Para LOCAL_ONLY
import androidx.compose.material.icons.filled.ErrorOutline // Para ERROR_UPLOADING
import es.ace.photosketchapp2121.viewmodel.DrawingTool
// guardar en carpeta externa a la aplicación
import android.content.ContentValues
import android.provider.MediaStore
import java.io.FileOutputStream
import java.io.IOException

class MainActivity : ComponentActivity() {

    // @OptIn se necesita si usas APIs experimentales como TopAppBar
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PhotoSketch3Theme {
                val viewModel: ExpedientesViewModel = viewModel()
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
                        ListaExpedientesScreen(navController = navController, viewModel = viewModel)
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
                            expedienteNombre = nombreExp,
                            viewModel = viewModel

                        )
                    }

                    // --- 5. Definición de Pantalla: Editor de fotos ---
                    composable(
                        // Pasamos la URI de la foto como argumento en la ruta
                        // La codificamos porque las URIs pueden tener caracteres especiales
                        route = "pantalla_editor/{photoUri}/{idCarpetaDrive}/{expedienteNombre}",
                        arguments = listOf(
                            navArgument("photoUri") { type = NavType.StringType },
                            navArgument("idCarpetaDrive") {
                                type = NavType.StringType
                                nullable = true // Hacemos que pueda ser nulo por si acaso
                            },
                            navArgument("expedienteNombre") { type = NavType.StringType; nullable = true }
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
                            expedienteNombre = nombreExp,
                            viewModel = viewModel
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
                            expedienteNombre = nombreExp,
                            viewModel = viewModel
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
fun ListaExpedientesScreen(navController: NavHostController, viewModel: ExpedientesViewModel) {

    // --- Setup (ViewModel, Estados, Credential Manager, etc. - TODO: Mover todo aquí) ---
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
                    // --- INICIO CABECERA PERSONALIZADA ---
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp), // Padding horizontal mínimo para el logo
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Logo a la Izquierda
                        Image(
                            painter = painterResource(id = R.drawable.logo_acephoto_hor), // Tu logo
                            contentDescription = "Logo ACEPhoto",
                            modifier = Modifier
                                .height(45.dp) // Ajusta la altura del logo como necesites para que se vea bien
                                .padding(end = 8.dp), // Un pequeño padding a la derecha del logo antes del espacio
                            contentScale = ContentScale.Fit // Para que el logo se ajuste bien sin recortarse
                        )

                        // Spacer para empujar el saludo y el botón a la derecha
                        Spacer(modifier = Modifier.weight(1f))

                        // 2. Saludo y Botón a la Derecha (en la misma fila)
                        // Usaremos otra Row aquí para alinear el saludo y el botón verticalmente si fuera necesario,
                        // o simplemente ponerlos uno al lado del otro.
                        // Para que estén uno al lado del otro y alineados con el centro vertical de la Row principal:
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(20.dp) // Espacio entre el saludo y el botón
                        ) {
                            // Saludo (solo primer nombre)
                            val displayName = userState!!.displayName ?: "Usuario" // Usamos el !! que tenías, pero con elvis es más seguro
                            val firstName = displayName.split(" ").firstOrNull() ?: displayName
                            Text(
                                text = "¡Hola ${firstName}!", // Tu saludo
                                style = MaterialTheme.typography.titleMedium, // Un estilo adecuado
                                color = Color(0xFF353544) // Tu color personalizado
                            )

                            // Botón Cerrar Sesión
                            Button(
                                onClick = { viewModel.signOut() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF4CCBBC), // Tu color de fondo
                                    contentColor = MaterialTheme.colorScheme.onSecondary // Tu color de contenido
                                )
                                // No contentPadding para tamaño "normal", o ajusta según veas
                            ) {
                                Text("Cerrar Sesión") // Texto normal, sin estilo específico para que tome el del botón
                            }
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(top = 8.dp)) // Separador después de la cabecera
                    // --- FIN CABECERA PERSONALIZADA ---

                    // --- BUSCADOR (Tu código, lo mantenemos igual) ---
                    OutlinedTextField(
                        value = query, // query viene del viewModel.searchQuery.collectAsStateWithLifecycle()
                        onValueChange = { viewModel.onSearchQueryChanged(it) },
                        label = { Text("Buscar por Código o Dirección") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    // --- FIN BUSCADOR ---

                    // Aquí seguiría tu LazyColumn para mostrar los expedientesFiltrados
                    // ...

                } else {
                    // --- Bloque si NO está logueado ---
                    Spacer(modifier = Modifier.height(200.dp)) // Espacio entre la imagen y el botón
                    Image(
                        painter = painterResource(id = R.drawable.logo_acephoto), // Reemplaza con el nombre real de tu archivo
                        contentDescription = "Logo de la aplicación", // Descripción para accesibilidad
                        modifier = Modifier.size(300.dp) // Ajusta el tamaño como quieras
                        // contentScale = ContentScale.Fit // Opcional, para ajustar cómo se escala la imagen
                    )
                    Spacer(modifier = Modifier.height(32.dp)) // Espacio entre la imagen y el botón
                    // --- FIN IMAGEN ---

                    Button(
                        onClick = startSignInFlow,
                        // --- AÑADIR COLORES AQUÍ ---
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CCBBC), // Ejemplo: usa el color secundario de tu tema
                            contentColor = MaterialTheme.colorScheme.onSecondary   // Ejemplo: color de texto para contraste con el secundario
                            // O puedes usar colores específicos:
                            // containerColor = Color(0xFF0066CC), // Un azul específico
                            // contentColor = Color.White
                        )
                        // --- FIN COLORES ---
                    ) {
                        Text("Iniciar Sesión")
                    }
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
                        Text("Cargando expedientes...")
                        CircularProgressIndicator() // Podrías mostrar un spinner
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
                /*if (userState != null) { // Solo mostrar si está logueado
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "(${listaFiltrada.size} expedientes mostrados)")
                }*/

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
fun CameraScreen(
    navController: NavHostController,
    idCarpetaDrive: String?,
    expedienteNombre: String?,
    viewModel: ExpedientesViewModel
) {
    val context = LocalContext.current
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
            backCameraSelector = foundBack?.first?.let { Builder().addCameraFilter { infos -> infos.filter { Camera2CameraInfo.from(it).cameraId == Camera2CameraInfo.from(foundBack.first).cameraId } }.build() } ?: CameraSelector.DEFAULT_BACK_CAMERA
            frontCameraSelector = foundFront?.let { Builder().addCameraFilter { infos -> infos.filter { Camera2CameraInfo.from(it).cameraId == Camera2CameraInfo.from(foundFront).cameraId } }.build() } ?: CameraSelector.DEFAULT_FRONT_CAMERA
            wideAngleCameraSelector = foundWide?.first?.let {
                // Asegurarnos que no es la misma que la 'back' normal si solo hay una
                if (foundBack?.first != foundWide.first) {
                    Builder().addCameraFilter { infos -> infos.filter { Camera2CameraInfo.from(it).cameraId == Camera2CameraInfo.from(foundWide.first).cameraId } }.build()
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
                title = { Text("${expedienteNombre ?: "Desconocido"}") }, // Mostramos ID carpeta
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
                                    tint = Color(0xFF4CCBBC), // Tu color elegido
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
                                        expedienteNombreParaCarpeta = expedienteNombre,
                                        onImageSaved = { uri, photoFile -> // <--- LA LAMBDA AHORA RECIBE uri y photoFile
                                            Log.d("CAMARA", "Foto guardada localmente: $uri, Nombre archivo: ${photoFile.name}")
                                            // El Toast.makeText(context, "Foto guardada en: $uri", Toast.LENGTH_SHORT).show() lo podemos quitar si prefieres,
                                            // ya que la vibración y la actualización de la miniatura dan feedback. O mantenerlo.

                                            // Lógica de Vibración (igual que la tenías, funciona bien)
                                            try {
                                                val vibrator: Vibrator?
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                                    val vibratorManager =
                                                        context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                                                    vibrator = vibratorManager.defaultVibrator
                                                } else {
                                                    @Suppress("DEPRECATION")
                                                    vibrator =
                                                        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                                                }
                                                if (vibrator?.hasVibrator() == true) {
                                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                        vibrator.vibrate(
                                                            VibrationEffect.createOneShot(
                                                                80, // Duración de la vibración
                                                                VibrationEffect.DEFAULT_AMPLITUDE
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
                                    tint = Color(0xFF4CCBBC), // Tu color
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
                                        .border(BorderStroke(2.dp, Color(0xFF4CCBBC)), RoundedCornerShape(8.dp))
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { // Tu lógica clickable
                                            lastPhotoUri?.let { uri ->
                                                val encodedUri = URLEncoder.encode(uri.toString(), StandardCharsets.UTF_8.name())
                                                Log.d("NAV", "Navegando a editor con URI: $encodedUri")
                                                if (!idCarpetaDrive.isNullOrBlank()) {
                                                    navController.navigate("pantalla_editor/$encodedUri/$idCarpetaDrive/$expedienteNombre")
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
                                    expedienteNombreParaCarpeta = expedienteNombre,
                                    onImageSaved = { uri, photoFile -> // <--- LA LAMBDA AHORA RECIBE uri y photoFile
                                        Log.d("CAMARA", "Foto guardada localmente: $uri, Nombre archivo: ${photoFile.name}")
                                        // El Toast.makeText(context, "Foto guardada en: $uri", Toast.LENGTH_SHORT).show() lo podemos quitar si prefieres,
                                        // ya que la vibración y la actualización de la miniatura dan feedback. O mantenerlo.

                                        // Lógica de Vibración (igual que la tenías, funciona bien)
                                        try {
                                            val vibrator: Vibrator?
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                                val vibratorManager =
                                                    context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                                                vibrator = vibratorManager.defaultVibrator
                                            } else {
                                                @Suppress("DEPRECATION")
                                                vibrator =
                                                    context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                                            }
                                            if (vibrator?.hasVibrator() == true) {
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                    vibrator.vibrate(
                                                        VibrationEffect.createOneShot(
                                                            80, // Duración de la vibración
                                                            VibrationEffect.DEFAULT_AMPLITUDE
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
                                tint = Color(0xFF4CCBBC), // Color del icono
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
                                        BorderStroke(2.dp, Color(0xFF4CCBBC)),
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
                                            Log.d("NAV", "Navegando a editor con URI: $encodedUri, ID Carpeta: $idCarpetaDrive, Exp Nombre: $expedienteNombre")
                                            if (!idCarpetaDrive.isNullOrBlank()) {
                                                navController.navigate("pantalla_editor/$encodedUri/$idCarpetaDrive/$expedienteNombre")
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
                                tint = Color(0xFF4CCBBC), // O el color que prefieras
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
    expedienteNombre: String?,
    viewModel: ExpedientesViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Estados del ViewModel
    // val galleryPhotos by viewModel.galleryPhotos.collectAsStateWithLifecycle()
    val galleryPhotosInfo by viewModel.galleryPhotosInfo.collectAsStateWithLifecycle()
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
        galleryPhotosInfo.size.coerceAtLeast(1)
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

    // 1. EFECTO INICIAL (SIN CAMBIOS)
    LaunchedEffect(photoUriString, idCarpetaDrive, context) {
        Log.d("EDITOR_SCREEN_EFFECT", "EFECTO INICIAL: photoUriString=$photoUriString, idCarpetaDrive=$idCarpetaDrive")
        if (photoUriString != null && idCarpetaDrive != null) { // Importante comprobar nulls
            viewModel.initializeEditorFor(context, photoUriString, idCarpetaDrive)
        }
    }

    // 2. EFECTO PAGER SIGUE A VM:
    // val targetIndexFromVM by viewModel.currentPhotoInGalleryIndex.collectAsStateWithLifecycle()
    // galleryPhotosInfo ya la tienes observada más arriba en tu Composable con:
    // val galleryPhotosInfo by viewModel.galleryPhotosInfo.collectAsStateWithLifecycle()

    LaunchedEffect(targetIndexFromVM, galleryPhotosInfo, pagerState.isScrollInProgress) { // Key en galleryPhotosInfo (la lista)
        val currentGalleryList = galleryPhotosInfo // Captura el valor actual
        if (currentGalleryList.isNotEmpty()) {
            val gallerySize = currentGalleryList.size
            // Aseguramos que el índice del VM es válido para la galería actual
            if (targetIndexFromVM >= 0 && targetIndexFromVM < gallerySize) {
                // Solo si el Pager no está ya en esa página Y NO está en medio de un scroll
                if (pagerState.currentPage != targetIndexFromVM && !pagerState.isScrollInProgress) {
                    Log.d("EDITOR_SCREEN_EFFECT", "VM->PAGER: VM quiere $targetIndexFromVM, Pager en ${pagerState.currentPage}. SCROLLING.")
                    scope.launch {
                        try {
                            pagerState.scrollToPage(targetIndexFromVM)
                        } catch (e: Exception) {
                            Log.e("EDITOR_SCREEN_EFFECT", "Error en scrollToPage (VM->Pager)", e)
                        }
                    }
                }
            } else {
                Log.w("EDITOR_SCREEN_EFFECT", "VM->PAGER: targetIndexFromVM ($targetIndexFromVM) inválido para gallerySize ($gallerySize).")
                // Opcional: si el índice del VM es inválido, podríamos forzar al Pager a la página 0
                // if (pagerState.currentPage != 0 && !pagerState.isScrollInProgress && gallerySize > 0) {
                //     scope.launch { pagerState.scrollToPage(0) }
                // }
            }
        } else if (targetIndexFromVM == 0 && pagerState.currentPage != 0 && !pagerState.isScrollInProgress) {
            // Si la galería está vacía y el VM apunta a 0, y el Pager no está en 0
            Log.d("EDITOR_SCREEN_EFFECT", "VM->PAGER: Galería vacía, VM quiere 0. Reseteando Pager a 0.")
            scope.launch {
                try {
                    pagerState.scrollToPage(0)
                } catch (e: Exception) {
                    Log.e("EDITOR_SCREEN_EFFECT", "Error en scrollToPage a 0 (galería vacía)", e)
                }
            }
        }
    }

    // 3. EFECTO VM SIGUE A PAGER (SIN CAMBIOS RESPECTO A TU VERSIÓN "QUE FUNCIONABA")
    //    Asegúrate de que usa galleryPhotosInfo
    LaunchedEffect(pagerState, galleryPhotosInfo) {
        val currentGalleryList = galleryPhotosInfo // Captura valor actual
        if (currentGalleryList.isEmpty()) {
            Log.d("EDITOR_SCREEN_EFFECT", "PAGER->VM: Galería vacía, no hay nada que sincronizar con VM.")
            return@LaunchedEffect
        }

        snapshotFlow { pagerState.settledPage }
            .distinctUntilChanged()
            .collect { settledPageIndex ->
                if (settledPageIndex < currentGalleryList.size && // Comprueba contra el tamaño actual
                    settledPageIndex != viewModel.currentPhotoInGalleryIndex.value) { // Usa .value
                    Log.d("EDITOR_SCREEN_EFFECT", "PAGER->VM: Pager (user swipe) settled on $settledPageIndex. VM index era ${viewModel.currentPhotoInGalleryIndex.value}. Updating VM.")
                    viewModel.userSwipedToPhotoAtIndex(settledPageIndex, context)
                } else {
                    Log.d("EDITOR_SCREEN_EFFECT", "PAGER->VM: Pager settled on $settledPageIndex. No se requiere actualización de VM (mismo índice o inválido).")
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
                    title = { Text("${expedienteNombre ?: "Expediente"}") },
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
                        IconButton(onClick = {
                            // Lógica de Navegación a Galería
                            Log.d("NAV_EDITOR", "Navegando a galería desde editor para Exp: $expedienteNombre Carp: $idCarpetaDrive")
                            if (!idCarpetaDrive.isNullOrBlank() && !expedienteNombre.isNullOrBlank()) {
                                if (isEditing && hasChanges) {
                                    showDiscardDialog = true
                                } else {
                                    viewModel.setEditingMode(false) // Salimos de modo edición
                                    navController.navigate("pantalla_galeria/$idCarpetaDrive/$expedienteNombre") {
                                        popUpTo("lista_expedientes") { saveState = false } // O el route de tu ListaExpedientesScreen
                                        launchSingleTop = true // Evita múltiples copias de galería si ya está en el top
                                    }
                                }
                            } else {
                                Log.w("NAV_EDITOR", "Faltan datos para navegar a galería desde editor.")
                                Toast.makeText(context, "Error: Falta info del expediente para galería", Toast.LENGTH_SHORT).show()
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Outlined.PhotoLibrary, // Mismo icono que en CameraScreen
                                contentDescription = "Ver Galería del Expediente",
                                // Puedes usar el mismo tint o el por defecto
                                tint = MaterialTheme.colorScheme.onSurfaceVariant // Tinte estándar para iconos de action
                            )
                        }
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
                                    val originalFileName = uriToSave?.lastPathSegment
                                    viewModel.saveEditedImage(
                                        context = context,
                                        originalPhotoUriString = uriToSave!!.toString(),
                                        originalFileName = originalFileName,
                                        idCarpetaDrive = idCarpetaDrive,
                                        expedienteNombreParaCarpeta = expedienteNombre,
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
                            val isPencilSelected = currentTool == DrawingTool.PENCIL
                            val pencilBgColor = if (isPencilSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                            val pencilIconColor = if (isPencilSelected) MaterialTheme.colorScheme.onPrimary else LocalContentColor.current
                            IconButton(
                                onClick = { viewModel.selectTool(if (isPencilSelected) null else DrawingTool.PENCIL) },
                                modifier = Modifier.size(48.dp).background(pencilBgColor, CircleShape)
                            ) { Icon(Icons.Filled.Edit, "Lápiz", tint = pencilIconColor) }

                            // --- Botón Línea (Tu código) ---
                            val isLineSelected = currentTool == DrawingTool.LINE
                            val lineBgColor = if (isLineSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                            val lineIconColor = if (isLineSelected) MaterialTheme.colorScheme.onPrimary else LocalContentColor.current
                            IconButton(
                                onClick = { viewModel.selectTool(if (isLineSelected) null else DrawingTool.LINE) },
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
                if (galleryPhotosInfo.isNotEmpty() || currentPhotoUriForVM != null) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        userScrollEnabled = !isEditing
                    ) { pageIndex ->
                        // Obtenemos el PhotoInfo para esta página
                        val photoInfoForThisPage = galleryPhotosInfo.getOrNull(pageIndex)
                        // Y luego su URI local
                        val photoUriForThisPage = photoInfoForThisPage?.localUri?.toUri() ?: currentPhotoUriForVM // Fallback
                        if (photoUriForThisPage != null) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                AsyncImage( model = photoUriForThisPage, contentDescription = "Foto página $pageIndex", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit, onError = { error -> Log.e("ASYNC_IMG_ERROR", "Error cargando $photoUriForThisPage: ${error.result.throwable}") }, onSuccess = { success -> Log.d("ASYNC_IMG_SUCCESS", "Éxito cargando $photoUriForThisPage: ${success.result.dataSource}") })
                                Canvas( modifier = Modifier.fillMaxSize().onSizeChanged { newSize -> canvasDrawSize = newSize }.then( if (isEditing && photoUriForThisPage == currentPhotoUriForVM) Modifier.pointerInput(Unit){ detectDragGestures( onDragStart = { offset -> viewModel.startDrawing(offset) }, onDragCancel = { viewModel.finishCurrentPath() }, onDragEnd = { viewModel.finishCurrentPath() }, onDrag = { change, dragAmount -> viewModel.updateDrawingInProgress(change.position); change.consume() } ) } else Modifier ) ) {
                                    if (isEditing && photoUriForThisPage == currentPhotoUriForVM) {
                                        drawnPaths.forEach { pathData ->
                                            if (pathData.points.size > 1) { val path = Path().apply { moveTo(pathData.points.first().x, pathData.points.first().y); pathData.points.drop(1).forEach { lineTo(it.x, it.y) } }; drawPath( path = path, color = pathData.properties.color, style = Stroke( width = pathData.properties.strokeWidth, cap = pathData.properties.strokeCap, join = pathData.properties.strokeJoin ) ) } }
                                        if (currentTool == DrawingTool.PENCIL) {
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
                                        if (currentTool == DrawingTool.LINE && lineStartPoint != null && currentLineEndPoint != null) { drawLine( color = currentProps.color, start = lineStartPoint!!, end = currentLineEndPoint!!, strokeWidth = currentProps.strokeWidth, cap = currentProps.strokeCap ) }
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
                        val isPencilSelected = currentTool == DrawingTool.PENCIL
                        val pencilBgColor = if (isPencilSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                        val pencilIconColor = if (isPencilSelected) MaterialTheme.colorScheme.onPrimary else LocalContentColor.current
                        IconButton(
                            onClick = { viewModel.selectTool(if (isPencilSelected) null else DrawingTool.PENCIL) },
                            modifier = Modifier.size(48.dp).background(pencilBgColor, CircleShape)
                        ) { Icon(Icons.Filled.Edit, "Lápiz", tint = pencilIconColor) }

                        // --- Botón Línea (Copiado de tu BottomAppBar) ---
                        val isLineSelected = currentTool == DrawingTool.LINE
                        val lineBgColor = if (isLineSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                        val lineIconColor = if (isLineSelected) MaterialTheme.colorScheme.onPrimary else LocalContentColor.current
                        IconButton(
                            onClick = { viewModel.selectTool(if (isLineSelected) null else DrawingTool.LINE) },
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
    expedienteNombre: String?,
    viewModel: ExpedientesViewModel
) {

    // --- 2. Observar Estados del ViewModel ---
    // Observamos la lista de fotos que expondrá el ViewModel
    // val photos by viewModel.galleryPhotos.collectAsStateWithLifecycle()
    val photoInfoList by viewModel.galleryPhotosInfo.collectAsStateWithLifecycle() // <-- LÍNEA NUEVA
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
            viewModel.triggerGalleryLoad(idCarpetaDrive)
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
                title = { Text("Galería ${expedienteNombre ?: "Desconocido"}") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) { // Botón Atrás
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver a Cámara")
                    }
                }
            )
        },

                floatingActionButton = {
            val scope = rememberCoroutineScope() // Necesitamos un scope para lanzar la corutina
            FloatingActionButton(onClick = {
                Log.d("DRIVE_UPLOAD_UI", "Botón Subir a Drive pulsado para expediente: $idCarpetaDrive")
                if (!idCarpetaDrive.isNullOrBlank()) {
                    // Llamamos a una nueva función en el ViewModel
                    scope.launch { // Las operaciones de subida pueden ser varias
                        viewModel.subirFotosPendientesDelExpediente(context, idCarpetaDrive)
                    }
                } else {
                    Toast.makeText(context, "ID de expediente no disponible.", Toast.LENGTH_SHORT).show()
                }
            }) {
                Icon(Icons.Filled.CloudUpload, "Subir fotos a Drive")
            }
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
            if (photoInfoList.isNotEmpty()) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 110.dp), // Ajusta tamaño mínimo de celda
                    modifier = Modifier.fillMaxSize(), // Ocupa todo el espacio disponible
                    contentPadding = PaddingValues(4.dp) // Espacio alrededor de la cuadrícula
                ) {
                    items(photoInfoList) { photoInfo ->
                        // Celda individual con la imagen clicable
                        Box( // Nuevo Box contenedor para la imagen y el icono
                            modifier = Modifier
                                .padding(4.dp) // Mantenemos el padding que teníamos
                                .aspectRatio(1f) // Mantenemos la proporción cuadrada
                                .clickable {
                                    val encodedUri = URLEncoder.encode(photoInfo.localUri, StandardCharsets.UTF_8.name())
                                    navController.navigate("pantalla_editor/$encodedUri/$idCarpetaDrive/$expedienteNombre") // Pasamos también expedienteNombre
                                }
                        ) {
                            AsyncImage(
                                model = photoInfo.localUri.toUri(),
                                contentDescription = "Foto del expediente ${photoInfo.fileName}", // Descripción más útil
                                modifier = Modifier
                                    .fillMaxSize() // La imagen llena el Box
                                    .border(BorderStroke(1.dp, Color.LightGray)),
                                contentScale = ContentScale.Crop
                            )

                            // --- AÑADIR ICONO DE ESTADO SUPERPUESTO ---
                            val iconModifier = Modifier
                                .align(Alignment.BottomEnd) // Ejemplo: Abajo a la derecha
                                .padding(4.dp) // Pequeño padding para el icono
                                .size(20.dp)   // Tamaño del icono de estado

                            when (photoInfo.syncStatus) {
                                SyncStatus.LOCAL_ONLY -> {
                                    Icon(
                                        imageVector = Icons.Filled.PhoneAndroid,
                                        contentDescription = "Solo local",
                                        tint = Color.White, // O un color que contraste bien
                                        modifier = iconModifier.background(Color.Black.copy(alpha = 0.5f), CircleShape) // Fondo semitransparente
                                    )
                                }
                                SyncStatus.SYNCED -> {
                                    Icon(
                                        imageVector = Icons.Filled.CloudDone,
                                        contentDescription = "Sincronizado",
                                        tint = Color(0xFF4CCBBC), // Un verde para indicar éxito
                                        modifier = iconModifier
                                    )
                                }
                                SyncStatus.ERROR_UPLOADING -> {
                                    Icon(
                                        imageVector = Icons.Filled.ErrorOutline,
                                        contentDescription = "Error al subir",
                                        tint = Color.Red,
                                        modifier = iconModifier
                                    )
                                }
                                SyncStatus.SYNCING_UP -> { // Si implementamos este estado
                                    CircularProgressIndicator(
                                        modifier = iconModifier.padding(2.dp), // Un poco más pequeño el círculo de progreso
                                        strokeWidth = 2.dp
                                    )
                                }
                                // Añadir casos para DRIVE_ONLY, PENDING_DELETE si los implementas
                                else -> {
                                    // No mostrar icono si el estado no es uno de los anteriores o es desconocido
                                }
                            }
                            // --- FIN ICONO DE ESTADO ---
                        } // Fin Box contenedor
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
    idCarpetaDrive: String?, // Sigue siendo el ID del expediente para la ruta
    expedienteNombreParaCarpeta: String?,
    onImageSaved: (uri: Uri, photoFileForDb: File) -> Unit, // Callback para éxito
    onError: (ImageCaptureException) -> Unit // Callback para error
) {
    if (idCarpetaDrive.isNullOrBlank()) {
        Log.e("CAMARA", "ID de Carpeta Drive es nulo o vacío. No se puede guardar la foto.")
        Toast.makeText(context, "Error: Falta ID de expediente.", Toast.LENGTH_SHORT).show()
        // Llamamos a onError con un error genérico para que la UI sepa que algo falló
        onError(ImageCaptureException(ImageCapture.ERROR_UNKNOWN, "ID de expediente no proporcionado", null))
        return
    }

    val displayName = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis()) + ".jpg"
    val contentResolver = context.contentResolver

    val expedienteFolderNameParaRuta = expedienteNombreParaCarpeta?.replace(Regex("[^a-zA-Z0-9.-]"), "_") ?: "PhotoSketch_Fotos" // Usar el nombre del expediente
    val fechaParaRuta = SimpleDateFormat("yyyyMMdd", Locale.US).format(System.currentTimeMillis())

    val desiredRelativePath = Environment.DIRECTORY_PICTURES + File.separator +
            "ACEPhotoSketch" + File.separator +
            expedienteFolderNameParaRuta + File.separator +
            fechaParaRuta
    Log.d("CAMARA_PATH", "Ruta Relativa para MediaStore: $desiredRelativePath")

    // 2. Preparamos los ContentValues
    val contentValues = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
        put(MediaStore.Images.Media.DATE_MODIFIED, System.currentTimeMillis() / 1000)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Asignamos la ruta relativa UNA SOLA VEZ, con la versión que usa el NOMBRE del expediente
            put(MediaStore.Images.Media.RELATIVE_PATH, desiredRelativePath)
            // IS_PENDING lo gestionará CameraX si usamos el OutputFileOptions.Builder(contentResolver,...)
            // o lo ponemos a 0 en el callback onImageSaved si lo creamos con IS_PENDING = 1.
            // Si CameraX lo gestiona, no necesitamos IS_PENDING aquí.
        }
        // Para versiones < API 29, MediaStore intentará guardar en una ubicación por defecto
        // dentro de Pictures. La organización en subcarpetas puede no ser precisa,
        // pero la foto debería ir a la galería pública.
    }

// 3. Creamos las opciones de salida usando el ContentResolver (esto estaba bien)
    val outputFileOptions = ImageCapture.OutputFileOptions.Builder(
        contentResolver,
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, // La colección estándar de Imágenes
        contentValues // Los metadatos que preparamos
    ).build()

// 4. Preparamos el File "conceptual" para el callback (esto estaba bien, pero adaptamos dateFolderNameForCallback)
    val dateFolderNameForCallback = fechaParaRuta // Usamos la fecha que calculamos para la ruta
    val dummyParentDirForCallback = File(expedienteNombreParaCarpeta ?: "PhotoSketch_Fotos", dateFolderNameForCallback) // Usa el nombre del expediente para el padre conceptual
    val photoFileForDb = File(dummyParentDirForCallback, displayName)


    val imageSavedCallback = object : ImageCapture.OnImageSavedCallback {
        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
            val savedUri = outputFileResults.savedUri // Esta es la content:// URI de MediaStore
            if (savedUri == null) {
                Log.e("CAMARA_MEDIAPALABRA", "Error: MediaStore no devolvió URI después de guardar.")
                onError(ImageCaptureException(ImageCapture.ERROR_FILE_IO, "MediaStore no devolvió URI tras guardar", null))
                return
            }

            Log.d("CAMARA_MEDIAPALABRA", "Foto guardada con éxito en MediaStore. URI final: $savedUri")
            // Pasamos la URI de MediaStore y el File "conceptual" al callback
            onImageSaved(savedUri, photoFileForDb)

            // Lógica de Vibración (tu código)
            try {
                val vibrator: android.os.Vibrator?
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager
                    vibrator = vibratorManager.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator
                }
                if (vibrator?.hasVibrator() == true) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(80)
                    }
                }
            } catch (e: Exception) { Log.e("VIBRACION", "Error al intentar vibrar", e) }
        }

        override fun onError(exception: ImageCaptureException) {
            Log.e("CAMARA_MEDIAPALABRA", "Error de CameraX al guardar en MediaStore", exception)
            onError(exception) // Llama al callback de error original
        }
    }

    Log.d("CAMARA", "Intentando capturar foto (directo a MediaStore con OutputFileOptions)...")
    cameraController.takePicture(
        outputFileOptions, // Las opciones que usan MediaStore
        ContextCompat.getMainExecutor(context), // Executor para los callbacks
        imageSavedCallback // El callback
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