# R8 rules for the release build.
#
# Components declared in AndroidManifest.xml (the Application, activities,
# services, the receiver) are kept automatically by AGP, so they aren't
# listed here.

# --- PebbleKitAndroid2 ------------------------------------------------------
#
# Keep the whole library. This is deliberate, not laziness:
#
#  * Neither the client nor the common AAR ships a consumer proguard.txt, so
#    R8 is told nothing about what the library needs preserved.
#  * It exists to do IPC with a *different app* (the official Pebble app).
#    That boundary carries AIDL stubs (SendDataCallback, UniversalRequestResponse
#    with their $Stub/$Proxy/$_Parcel inner classes) and Parcelable payloads,
#    which are resolved by class name across processes. Our side renaming them
#    while the Pebble app still uses the original names fails at runtime, with
#    no build-time warning - and the symptom is the alarm silently never
#    firing, which is exactly the bug this project already spent a debugging
#    round on.
#  * The library is small, so obfuscating it saves almost nothing.
-keep class io.rebble.pebblekit2.** { *; }
-keepclassmembers class io.rebble.pebblekit2.** { *; }

# --- Parcelables ------------------------------------------------------------
# Belt and braces: a Parcelable's CREATOR is looked up reflectively.
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# --- Crash reports ----------------------------------------------------------
# Keep line numbers so Play Console stack traces stay readable once the
# mapping file (app/build/outputs/mapping/release/mapping.txt) is uploaded.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
