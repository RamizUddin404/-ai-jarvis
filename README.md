# 🤖 AI Jarvis - Android Application

A Kotlin-based Android application built with Jetpack Compose and Firebase integration.

## 📋 Requirements

- **Android Studio**: Latest version
- **JDK**: Version 11 or higher
- **Gradle**: 8.0+
- **Android SDK**: API level 24+ (Min), API level 36 (Target)

## 🚀 Getting Started

### 1. Clone the Repository
```bash
git clone https://github.com/RamizUddin404/-ai-jarvis.git
cd -ai-jarvis
```

### 2. Setup Environment Variables
Create a `.env` file in the root directory with your Firebase credentials:
```env
FIREBASE_API_KEY=your_api_key
FIREBASE_PROJECT_ID=your_project_id
FIREBASE_APP_ID=your_app_id
KEYSTORE_PATH=path/to/your/keystore.jks
STORE_PASSWORD=your_store_password
KEY_PASSWORD=your_key_password
```

### 3. Build the Application

#### Debug Build
```bash
./gradlew assembleDebug
```

#### Release Build
```bash
./gradlew assembleRelease
```

#### Run Tests
```bash
./gradlew test
```

#### Install on Device
```bash
./gradlew installDebug
```

## 🔧 Project Structure

```
-ai-jarvis/
├── app/
│   ├── src/
│   │   ├── main/
│   │   ├── test/
│   │   └── androidTest/
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── gradle/
├── build.gradle.kts
└── settings.gradle.kts
```

## 📦 Key Dependencies

- **Jetpack Compose**: Modern UI toolkit
- **Firebase**: Auth, Firestore, AI, AppCheck
- **Room**: Local database
- **Retrofit**: HTTP client
- **OkHttp**: HTTP interceptor
- **Coil**: Image loading
- **Moshi**: JSON serialization
- **Kotlin Coroutines**: Asynchronous programming

## 🔨 GitHub Actions CI/CD

This project includes automated build workflows:

- **Trigger**: On every push to `main` branch
- **Build**: Debug and Release APK builds
- **Tests**: Unit tests execution
- **Artifacts**: APK files uploaded as artifacts

View workflow status: [Actions Tab](https://github.com/RamizUddin404/-ai-jarvis/actions)

## 🎯 Build Features

- ✅ Compose UI support
- ✅ Firebase integration
- ✅ Camera support
- ✅ DataStore preferences
- ✅ Navigation component
- ✅ Unit & instrumented tests
- ✅ ProGuard optimization for release builds

## 📱 Android Configuration

- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 36 (Android 15)
- **Compile SDK**: 36

## 🔐 Security

- Release APK signing configured
- Debug keystore included for development
- Environment-based secret management via `.env`

## 🐛 Troubleshooting

### Build Fails with Keystore Error
Ensure your `.env` file contains valid keystore paths and passwords.

### Gradle Cache Issues
```bash
./gradlew clean build
```

### Firebase Configuration Missing
Download `google-services.json` from Firebase Console and place in `app/` directory.

## 📄 License

This project uses the Google Gemini AI Studio Repository Template.

## 👨‍💻 Developer

**RamizUddin404** - [GitHub Profile](https://github.com/RamizUddin404)

---

**Need help?** Check the Issues tab or create a new issue!
