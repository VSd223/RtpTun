# Google Play Services & ML Kit Code Scanner (GmsBarcodeScanning)
-keep class com.google.android.gms.common.api.internal.** { *; }
-keep class com.google.android.gms.internal.** { *; }
-keep class com.google.android.gms.dynamite.** { *; }
-keep class com.google.android.gms.tasks.** { *; }
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.vision.barcode.** { *; }
-keep class com.google.android.gms.vision.** { *; }
-keep class com.google.android.gms.common.** { *; }

-dontwarn com.google.android.gms.**
-dontwarn com.google.mlkit.**

# WorkManager and Room (fixes java.lang.NoSuchMethodException: androidx.work.impl.WorkDatabase_Impl.<init>)
-keep class androidx.work.impl.WorkDatabase_Impl {
    <init>();
}
-keep class androidx.work.impl.WorkDatabase_Impl$* { *; }
-keep class androidx.room.** { *; }

