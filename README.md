# Bloom — Native Android (Kotlin · Jetpack Compose)

Native Android Studio conversion of the Bloom private diary & personal-growth app.
Same Firebase backend, same Firestore schema, same insight rules as the React Native
version — the two clients are drop-in interchangeable against one project.

## Stack

| Layer | Choice |
|---|---|
| Language | Kotlin 2.0.21 |
| UI | Jetpack Compose + Material 3, custom "quiet paper" theme (5 accents, light/dark) |
| Architecture | MVVM — `ViewModel` + `StateFlow` per screen, Hilt DI (KSP) |
| Data | Firebase Auth, Firestore (offline persistence on by default), FCM, Cloud Functions (`asia-south1`) |
| Local | DataStore (check-in draft autosave, reminder settings mirror, onboarding flag) |
| Sign-in | Email/password + Google via **Credential Manager** (`androidx.credentials` + `googleid`) |
| Reminders | `AlarmManager` daily inexact alarms + `BootReceiver` (no exact-alarm permission needed) |
| Screen time | `UsageStatsManager` first-class (was a custom Expo module in the RN version) |
| Privacy | BiometricPrompt app lock, private notification mode, export + delete-account callables |
| Tests | Pure-JVM domain layer with JUnit tests (`./gradlew test`) |

## Getting it running

1. **Open in Android Studio** (Ladybug or newer). Let it sync; if Gradle asks to
   generate the wrapper, accept (the wrapper jar is intentionally not shipped in the zip).
2. **Firebase config** — in the Firebase console:
   - Add an Android app with package `mobile.dairy.app`.
   - Download `google-services.json` into `app/`.
   - For Google Sign-In: add your debug SHA-1 (`./gradlew signingReport`) to the Android app,
     then copy the **Web client ID** (Authentication → Sign-in method → Google → Web SDK
     configuration) into `app/src/main/res/values/strings.xml` →
     `default_web_client_id_bloom`.
3. **Backend** — reuse the Firebase project from the React Native Bloom delivery:
   `firestore.rules`, `firestore.indexes.json`, and `functions/` deploy unchanged.
   The Android client calls the same `exportUserData` / `deleteAccount` callables and
   receives the same milestone/weekly-summary FCM pushes.
4. **Run tests**: `./gradlew test` — `app/src/test/java/com/pukal/bloom/DomainTest.kt`
   covers streaks, finance math, INR formatting, and every insight rule
   (the same 24 assertions were compiled and executed with `kotlinc` during generation: 24/24 pass).

> If Gradle sync complains about the KSP version, bump `ksp` in
> `gradle/libs.versions.toml` to the latest `2.0.21-1.0.x`.

## Where things live

```
app/src/main/java/com/pukal/bloom/
├── core/          Constants (moods, categories, suggestions), Dates (YYYY-MM-DD keys), Format (INR lakh grouping)
├── domain/        Pure Kotlin, no Android imports — Models, Streaks, Finance, InsightEngine
├── data/          Firestore repositories (snapshot-listener Flows), AuthRepository
├── di/            Hilt module (Auth / Firestore / Functions instances)
├── services/      Notifier, LocalPrefs (DataStore), Reminders (AlarmManager + receivers),
│                  BloomMessagingService (FCM), ScreenTimeService (UsageStatsManager)
└── ui/            theme, shared components (MoodPicker, RatingScale, Bars, chips),
                   auth, dashboard (+TabScaffold), checkin (pager + draft), journal (+editor),
                   goals (list/new/detail), money, insights, screentime, settings, lock, onboarding
```

## Design notes (RN → native mapping)

- **expo-router tabs** → `NavHost` + a shared `TabScaffold` with Material 3 `NavigationBar`.
- **Zustand + Firestore listeners** → repository `callbackFlow`s combined in ViewModels
  into one immutable screen state (`combine(...).stateIn(...)`).
- **AsyncStorage check-in draft** → DataStore + `kotlinx.serialization`; every keystroke
  autosaves, and the draft is only restored if it belongs to today.
- **Expo Notifications** → notification channels + `AlarmManager` for local reminders;
  quiet hours and the private "Your daily reflection is ready." mode are enforced in
  `Notifier`, including for FCM pushes.
- **Screen-time Expo module (Kotlin)** → folded directly into `ScreenTimeService`;
  the Usage Access permission still requires the system settings toggle
  (`ACTION_USAGE_ACCESS_SETTINGS`), which the screen deep-links to.
- **Insight engine** — ported line-for-line: max 4 insights/day, deduped by
  `date-type` document IDs, exact supportive wording preserved (e.g.
  "You avoided ₹300 in unnecessary spending today.").

## Known limits

- Voice notes / photo attachments and a habits screen are not built (same gaps as the RN delivery).
- Screen time is Android-only by nature; `unlocks` counting needs API 28+.
- Gradle sync and a device build must happen in Android Studio — this sandbox has no Android SDK,
  so only the pure-JVM domain layer could be compile-verified (it passes 24/24).
