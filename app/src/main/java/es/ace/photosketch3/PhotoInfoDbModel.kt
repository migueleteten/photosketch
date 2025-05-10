package es.ace.photosketch3

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow // Para consultas reactivas

// Enum para el estado de sincronización
enum class SyncStatus {
    LOCAL_ONLY,      // Solo en el dispositivo, pendiente de subir
    SYNCING_UP,      // Subiendo a Drive
    SYNCED,          // Confirmado en Drive y localmente igual (o solo en Drive si se limpió local)
    SYNCING_DOWN,    // (Futuro) Bajando de Drive
    DRIVE_ONLY,      // (Futuro) Detectado en Drive, no local
    ERROR_UPLOADING, // Falló la subida
    PENDING_DELETE_LOCAL, // Marcado para borrar localmente (después de borrar en Drive)
    PENDING_DELETE_DRIVE // Marcado para borrar en Drive (y luego local)
}

@Entity(tableName = "photos") // Nombre de la tabla en la base de datos
data class PhotoInfo(
    @PrimaryKey
    @ColumnInfo(name = "local_uri") // Nombre de la columna
    val localUri: String, // URI del archivo local como String (ej. de File.toUri().toString())

    @ColumnInfo(name = "id_expediente_drive")
    val idExpedienteDrive: String, // Para saber a qué expediente pertenece

    @ColumnInfo(name = "file_name")
    val fileName: String, // Ej: "20250509_064000.jpg" o "..._edited.jpg"

    @ColumnInfo(name = "date_folder_name")
    val dateFolderName: String, // Ej: "20250509"

    @ColumnInfo(name = "drive_file_id")
    val driveFileId: String? = null, // ID del archivo en Google Drive (nullable si no está subido)

    @ColumnInfo(name = "sync_status")
    var syncStatus: SyncStatus = SyncStatus.LOCAL_ONLY, // Estado actual

    @ColumnInfo(name = "is_edited")
    val isEdited: Boolean = false, // True si es una foto "_edited.jpg"

    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis() // Para ordenar o saber cuándo se creó/modificó
)

@Dao // Le dice a Room que esto es un DAO
interface PhotoInfoDao {

    // --- Operaciones de Escritura ---

    // Inserta una nueva PhotoInfo. Si ya existe una con la misma localUri (PrimaryKey), la reemplaza.
    // 'suspend' indica que debe llamarse desde una corrutina (para no bloquear el hilo principal).
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPhoto(photoInfo: PhotoInfo)

    // Actualiza una PhotoInfo existente.
    @Update
    suspend fun updatePhoto(photoInfo: PhotoInfo)

    // Borra una PhotoInfo.
    @Delete
    suspend fun deletePhoto(photoInfo: PhotoInfo)

    // Borra una PhotoInfo por su URI local.
    @Query("DELETE FROM photos WHERE local_uri = :localUri")
    suspend fun deletePhotoByLocalUri(localUri: String)

    // --- Operaciones de Lectura (Consultas) ---

    // Obtiene todas las fotos de un expediente específico, ordenadas por timestamp descendente (más nuevas primero).
    // Devuelve un Flow, lo que significa que la UI puede observar esta lista y se actualizará automáticamente si los datos cambian.
    @Query("SELECT * FROM photos WHERE id_expediente_drive = :expedienteId ORDER BY file_name DESC")
    fun getPhotosForExpedienteFlow(expedienteId: String): Flow<List<PhotoInfo>>

    // Obtiene una foto específica por su URI local (para ver detalles o editar).
    @Query("SELECT * FROM photos WHERE local_uri = :localUri LIMIT 1")
    suspend fun getPhotoByLocalUri(localUri: String): PhotoInfo?

    // Obtiene todas las fotos de un expediente que están marcadas como 'LOCAL_ONLY' (pendientes de subir).
    // La usaremos para el botón "Subir Cambios a Drive".
    @Query("SELECT * FROM photos WHERE id_expediente_drive = :expedienteId AND sync_status = 'LOCAL_ONLY'")
    suspend fun getLocalOnlyPhotosForExpediente(expedienteId: String): List<PhotoInfo>

    // Actualiza el estado de una foto a 'SYNCED' y guarda su ID de Drive (después de una subida exitosa).
    @Query("UPDATE photos SET drive_file_id = :driveId, sync_status = 'SYNCED' WHERE local_uri = :localUri")
    suspend fun markAsSynced(localUri: String, driveId: String)

    // (Opcional) Obtener todas las fotos (para debug o funciones globales)
    @Query("SELECT * FROM photos ORDER BY timestamp DESC")
    fun getAllPhotosFlow(): Flow<List<PhotoInfo>>
}