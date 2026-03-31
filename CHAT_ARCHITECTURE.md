# Postman Chat App Architecture

This project is now a Firebase-ready UI-first MVP for a realtime messaging app.

## What can stay on the device

- Message cache
- Media file references
- Downloaded image, video, and document files
- Drafts and offline send queue

## What still needs a network backend

- User authentication
- Contact discovery
- Message delivery between different devices
- Push notifications
- Media upload and download coordination
- Read receipts and online presence

## Recommended production stack

- Android app: Kotlin + Jetpack Compose
- Local storage: Room database for messages and conversations
- Realtime transport: Firebase Auth + Cloud Firestore
- Media storage: cloud object storage, with local file cache on device
- Notifications: Firebase Cloud Messaging

## Firebase modules chosen for this project

- Firebase Authentication for sign-in
- Cloud Firestore for conversation and message metadata
- Firebase Storage for media uploads
- Firebase Cloud Messaging for push notifications

## Current app state

- Welcome screen
- Chat list screen
- Conversation screen
- Automatic fallback to a local sample repository when Firebase config is missing
- Firebase anonymous-auth path for realtime text chat when config is present
- Shared Firestore `global-room` bootstrap for first realtime testing
- Firebase SDK dependencies added to Gradle

## Final setup still required

- Create a Firebase project
- Add Android app package `com.example.postman`
- Put `google-services.json` inside `app/`
- Replace attachment placeholder messages with real Firebase Storage uploads
- Add proper user-to-user conversations instead of only the shared realtime room

## Important constraint

If two different users must see the same message in realtime, device-only storage is not enough. The app needs a server or direct peer-to-peer transport layer.
