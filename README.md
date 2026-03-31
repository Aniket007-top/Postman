# Postman

Postman is an Android private chat app built with Kotlin, Jetpack Compose, Firebase, and Cloudinary.

It currently includes:

- Email/password sign up and sign in
- Forgot-password reset by email
- Friend requests before private chat starts
- Realtime 1-to-1 text chat with Firestore
- Cloudinary media upload for images, videos, documents, and profile photos
- Push notifications with Firebase Cloud Messaging
- Online/offline presence indicators

## Tech Stack

- Kotlin
- Jetpack Compose
- Material 3
- Firebase Authentication
- Cloud Firestore
- Firebase Cloud Messaging
- Firebase Functions
- Cloudinary

## Project Details

- Package name: `com.beinganie.postman`
- Min SDK: 24
- Target SDK: 35
- Compile SDK: 35

## Firebase Notes

Required Firestore rules for the current app:

```txt
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId} {
      allow read, write: if request.auth != null;
    }

    match /conversations/{conversationId} {
      allow read, write: if request.auth != null;
    }

    match /conversations/{conversationId}/messages/{messageId} {
      allow read, write: if request.auth != null;
    }

    match /friend_requests/{requestId} {
      allow read, write: if request.auth != null;
    }
  }
}
```

Push notifications require the Firebase Function in `functions/` to be deployed.

Cloudinary handles media uploads because Firebase Storage on the current project was not used for this app setup.

## Repository

GitHub: `https://github.com/Aniket007-top/Postman`
