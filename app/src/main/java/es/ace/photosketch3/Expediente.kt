package es.ace.photosketch3

// Define la estructura de datos para un expediente
data class Expediente(
    val nombre: String,       // Columna "Expediente" de tu Sheet
    val direccion: String,    // Columna "Dirección"
    val idCarpetaDrive: String // Columna "id_carpeta_drive"
)