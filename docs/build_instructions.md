# CodeForge Studio — Build & Compilation Instructions

## Prerequisites
- **JDK:** OpenJDK 17 or higher (Java 21 / 25 supported via Gradle 9.3+)
- **Android SDK:** API 35 Platform (`platforms;android-35`) and Build-Tools (`35.0.0` or `36.0.0`)
- **Gradle:** 9.3.1 (configured in project wrapper `gradlew`)
- **Target Architecture:** `arm64-v8a`, `armeabi-v7a`, `x86_64`

---

## Command Line Build

1. **Set Environment Variables:**
   ```powershell
   $env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
   $env:ANDROID_HOME = "C:\Users\OLI\AppData\Local\Android\Sdk"
   ```

2. **Execute Unit Tests:**
   ```powershell
   .\gradlew.bat testDebugUnitTest
   ```

3. **Assemble Debug APK:**
   ```powershell
   .\gradlew.bat assembleDebug
   ```
   The resulting APK will be generated at:
   `app/build/outputs/apk/debug/app-debug.apk`

4. **Assemble Release APK:**
   ```powershell
   .\gradlew.bat assembleRelease
   ```
