# Protein

A simple, offline Android app for tracking daily **protein** (the main number) and **calories** (secondary).
Logging a meal takes a few seconds: tap **Log food**, type two or three letters, tap the food, type the amount, **Save**.

- No account, no internet permission, no ads. Your data stays on your phone.
- Neutral colours throughout: no red, no warnings, no streaks.

## Features

- **Today**: a large animated ring for protein vs. your target, a calorie bar, and today's entries grouped by meal.
- **Fast logging**: forgiving search ("chkn" or "panir" still work). Favourites and recent foods come first.
  The search box sits at the bottom, near your thumb.
- **Grams or servings**: enter `150` g, or `1.75` slices, `2` eggs or `1` scoop. A live preview shows protein and kcal as you type.
- **Meal slots**: Morning, Afternoon, Evening and Night are picked from the clock, and you can change them.
- **Quick entry**: type protein and kcal directly, without choosing a food.
- **Entries**: swipe right to edit, swipe left to delete (with Undo), long-press to log the same thing again today.
- **Day boundary**: a new day starts at midnight or at a time you choose (default 4:00 AM), so late-night snacks count toward the day you ate them.
- **History**: a weekly bar chart with the 7-day average drawn as a line, a calendar to open any past day, and monthly averages.
- **Foods**: 22 starter foods (values per 100 g). Edit any value and add your own foods.
- **Settings**: protein and calorie targets, optional body weight with a suggested 1.6–2.0 g/kg range, day-start time,
  theme (system/light/dark), wallpaper colours (Android 12+), JSON backup export/import and CSV export.

## Tech

Kotlin · Jetpack Compose · Material 3 (dynamic colour on Android 12+) · Room · DataStore · MVVM with ViewModel + StateFlow ·
minSdk 26 · targetSdk 35 · Gradle Kotlin DSL with a version catalog · GitHub Actions.

## Project layout

```
.github/workflows/build.yml         Builds the APK on GitHub
gradle/libs.versions.toml           All library versions in one place
gradle/wrapper/                     Gradle wrapper (downloads the right Gradle)
app/build.gradle.kts                App settings: SDK levels, dependencies, signing
app/debug.keystore                  Fixed signing key so new APKs install over old ones
app/src/main/AndroidManifest.xml
app/src/main/res/                   Icon, colours, startup theme
app/src/main/java/app/protein/tracker/
  MainActivity.kt, ProteinApp.kt, AppContainer.kt
  domain/      Pure logic: day boundary, meal slots, search, maths, formatting, starter foods
  data/        Room database, settings (DataStore), repository, backup and CSV
  ui/          Compose screens: today, log, history, foods, settings, shared components, theme
app/src/test/                       Unit tests for the logic, plus a UI test that runs the whole app
                                    on the computer (Robolectric): logging, undo, history, foods,
                                    settings, backup and dark mode
```

## Build the APK and install it (no Android Studio needed)

1. Put the code on the `main` branch of your GitHub repository (see the next section if it isn't there yet).
2. Open the repository on GitHub and tap the **Actions** tab.
3. Pick **Build APK** on the left. A build starts on every push to `main`.
   To start one yourself, tap **Run workflow** → **Run workflow**.
4. Wait about 5–8 minutes for a green tick. The first build is the slowest.
5. Open the finished run. Under **Artifacts**, tap **Protein-debug-apk**. This downloads a `.zip` file.
6. On your phone, open the zip in the **Files** app and extract it. You get `app-debug.apk`.
7. Tap `app-debug.apk`. If Android asks, allow installing apps from this source (your browser or Files app), then tap **Install**.
   If Play Protect warns about an unknown developer, choose **More details → Install anyway**. You built this app yourself.

Later builds install straight over the old version and keep your data, because every build is signed with the same
key (`app/debug.keystore`).

## Getting the files onto GitHub yourself (only needed for a fresh repository)

1. On github.com tap **+** → **New repository**, give it a name, and tap **Create repository**.
2. Tap **uploading an existing file**, then drag in **everything inside** the project folder (not the folder itself),
   including `gradle/wrapper/gradle-wrapper.jar` and `app/debug.keystore`.
   Tap **Commit changes**.
3. Folders that start with a dot are often hidden by your computer and get skipped. If `.github` is missing after the upload,
   tap **Add file → Create new file**, type `.github/workflows/build.yml` as the name, paste in the workflow, and commit.
4. Check the branch is called `main` (top-left of the file list). Committing to `main` starts the build.

## First-build problems and how to fix them

| What you see | Why | Fix |
|---|---|---|
| The **Actions** tab shows no runs | The workflow file isn't at exactly `.github/workflows/build.yml`, or you pushed to a branch other than `main` | Check the path and the branch name, or use **Run workflow** |
| `./gradlew: Permission denied` | Web uploads lose the "executable" flag | The workflow already runs `chmod +x ./gradlew`. Make sure that step is still in `build.yml` |
| `Could not find or load main class org.gradle.wrapper.GradleWrapperMain` | `gradle/wrapper/gradle-wrapper.jar` was not uploaded | Upload that file into `gradle/wrapper/` |
| `Plugin [id: 'com.android.application', version: '…'] was not found` or `Could not find androidx…:…` | A version number in `gradle/libs.versions.toml` is mistyped or doesn't exist | Fix the version in `libs.versions.toml` |
| `Dependency '…' requires … compile against version 36` | A newer library needs a newer `compileSdk` | Raise `compileSdk` in `app/build.gradle.kts`, or use an older version of that library |
| `ksp-… is too old for kotlin-…` | The KSP version must match Kotlin | `ksp` must start with the `kotlin` version, e.g. Kotlin `2.2.21` → KSP `2.2.21-2.0.4` |
| `Android Gradle plugin requires Java 17` | The wrong Java version | Keep the **Set up Java 17** step in the workflow |
| `Unresolved reference: …` in a `.kt` file | A file landed in the wrong folder | Every file under `app/src/main/java/app/protein/tracker/` must keep its exact folder |
| `Keystore file '…/debug.keystore' not found` | `app/debug.keystore` was not uploaded | Upload it into `app/` |
| `Java heap space` or `Gradle daemon disappeared` | The build ran out of memory | Lower `-Xmx3g` in `gradle.properties` to `-Xmx2g` |
| Phone: **App not installed** / **package conflicts** | The old APK was signed with a different key | Export a backup (Settings), uninstall the old app, install the new one, then import the backup |
| Phone: **There was a problem parsing the package** | You tapped the `.zip`, or the download was cut off | Extract the zip first, or download it again |
| No **Artifacts** section | The build failed, or the APK is more than 30 days old | Open the red step to read the error, or run the workflow again |

## Backups

Settings → **Export backup** saves a JSON file with all your foods, entries and settings. **Import backup** replaces
everything with a backup file. **Export entries as CSV** creates a spreadsheet of every entry.

Note: the signing key in `app/debug.keystore` is for personal builds. If your repository is public, anyone could sign an
APK with it, so only install APKs from your own Actions page.
