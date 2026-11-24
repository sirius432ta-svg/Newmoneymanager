# Money Manager — Android source project

This is an initial Kotlin + Jetpack Compose Android project for a local-first personal finance app.

## Included in this starter build
- Dark interface
- English / Russian / Hebrew language switching (Hebrew uses RTL layout direction)
- Local SQLite storage
- ILS amounts stored as integer agorot
- Add income and expense transactions
- Balance and transaction history
- Basic savings-goal creation and progress display

## Important status
This is an initial working foundation, NOT the complete specification yet. Monthly/category charts, full budget management, recurring transactions, transaction editing/deletion, backup/restore, biometric lock, and full date filtering still need implementation and testing. Do not use it as the sole record of important finances until those features and backup are completed.

## Build APK
1. Install Android Studio on a computer.
2. Open this folder as an existing project.
3. Allow Gradle sync and install Android SDK 35 if prompted.
4. Select Build > Build Bundle(s) / APK(s) > Build APK(s).
5. Android Studio will show the generated APK location, usually `app/build/outputs/apk/debug/app-debug.apk`.

A release APK should be generated only after configuring signing and testing the app.
