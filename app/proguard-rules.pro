# Mantener tu MainActivity y sus constructores
-keep class es.ace.photosketchapp2121.MainActivity { <init>(...); }

# Mantener clases de las que hereda tu Activity
-keep public class * extends androidx.activity.ComponentActivity {
    public <init>(...);
}

# Reglas generales para Compose (deberían estar, pero por si acaso)
# ... (las que te pasé anteriormente para @Composable, @Preview, Composer, Composition)

# ViewModels
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
  public <init>(...);
}
-keepclassmembers class * implements androidx.lifecycle.ViewModelProvider$Factory {
  public <init>(...);
}

# Tus Data Classes (PhotoInfo, Expediente, PathData, etc.) y Enums
-keepattributes Signature, InnerClasses
-keep public class es.ace.photosketchapp2121.Expediente { *; }
-keep public class es.ace.photosketchapp2121.PhotoInfo { *; }
-keep public class es.ace.photosketchapp2121.viewmodel.PathData { *; }
-keep public class es.ace.photosketchapp2121.viewmodel.PathProperties { *; }
-keep public enum es.ace.photosketchapp2121.SyncStatus { *; }
-keep public enum es.ace.photosketchapp2121.viewmodel.DrawingTool { *; }

# Reglas para Google API Client y Gson (imprescindibles)
# ... (las que te pasé anteriormente, son genéricas y no dependen del nombre de tu paquete)
# Ejemplo:
# -keepclassmembers class com.google.api.client.json.GenericJson { <fields>; }
# -keepclassmembers class * extends com.google.api.client.util.GenericData { <fields>; <methods>; }

# Reglas para Credential Manager
-keep class com.google.android.libraries.identity.googleid.** { *; }
-keep class androidx.credentials.** { *; }

# Desactivar warnings comunes
-dontwarn org.apache.http.**
-dontwarn com.google.common.**
-dontwarn sun.misc.Unsafe