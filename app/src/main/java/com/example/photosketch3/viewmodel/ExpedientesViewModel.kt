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

    // Nuevo StateFlow para guardar el texto de búsqueda actual
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow() // Público para que la UI lo observe

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

    fun signOut() {
        // Limpia el usuario y los expedientes
        setLoggedInUser(null)
        // TODO: Podríamos añadir aquí la llamada a CredentialManager para limpiar estado si fuera necesario
        // credentialManager.clearCredentialState(...) - Necesitaríamos el manager aquí o pasarlo
        Log.d("VIEWMODEL_SIGN_OUT", "Usuario deslogueado")
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