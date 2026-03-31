# Postman

Postman is an Android chat app prototype built with Kotlin, Jetpack Compose, and Firebase.

It currently includes:

- A welcome screen
- A chat list screen
- A conversation screen
- A local fallback mode for UI testing
- Firebase-ready wiring for realtime chat

## Tech Stack

- Kotlin
- Jetpack Compose
- Material 3
- Android Navigation Compose
- Firebase Authentication
- Cloud Firestore
- Firebase Storage

## Project Details

- Package name: `com.beinganie.postman`
- Min SDK: 24
- Target SDK: 35
- Compile SDK: 35

## Run Locally

1. Open the project in Android Studio.
2. Sync Gradle.
3. If you want Firebase enabled, place your own `google-services.json` file inside `app/`.
4. Run the app on an emulator or Android device.

## Firebase Notes

The app can fall back to local sample data when Firebase is not configured. For realtime chat across devices, Firebase setup is required.

Do not commit your personal `google-services.json` file if you want to keep the repository clean for public sharing.

## Repository

GitHub: `https://github.com/Aniket007-top/Postman`
