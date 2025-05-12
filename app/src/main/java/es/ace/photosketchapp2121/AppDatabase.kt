package es.ace.photosketchapp2121
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// Anotación @Database para definir las entidades y la versión de la BD
// exportSchema = false es para evitar un warning de compilación por ahora,
// en apps de producción se suele poner a true y gestionar los esquemas.
@Database(entities = [PhotoInfo::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    // Room generará la implementación de este método abstracto
    abstract fun photoInfoDao(): PhotoInfoDao

    // Companion object para implementar el patrón Singleton y obtener la instancia de la BD
    companion object {
        // @Volatile asegura que el valor de INSTANCE siempre sea el más actualizado
        // y visible para todos los hilos de ejecución.
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            // synchronized evita que múltiples hilos creen múltiples instancias
            // si INSTANCE es null al mismo tiempo.
            synchronized(this) {
                var instance = INSTANCE
                if (instance == null) {
                    instance = Room.databaseBuilder(
                        context.applicationContext, // Usamos applicationContext para evitar memory leaks
                        AppDatabase::class.java,    // Nuestra clase de Database
                        "photo_sketch_database"     // Nombre del archivo de la base de datos en el dispositivo
                    )
                        // Aquí podríamos añadir .fallbackToDestructiveMigration() si no queremos hacer migraciones
                        // o .addMigrations(...) si tuviéramos migraciones definidas.
                        // Por ahora, con version = 1, no necesitamos migraciones.
                        .build()
                    INSTANCE = instance
                }
                return instance
            }
        }
    }
}