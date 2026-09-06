# AGENTS.md — Recurrence

Minimal Android notification-reminder app (Java, API 15–24). Single-module Gradle project targeting the `app/` subproject.

## Build & Run

```bash
# Debug build
./gradlew assembleDebug

# Release build (ProGuard enabled)
./gradlew assembleRelease

# Install on connected device/emulator
./gradlew installDebug
```

- Uses the **Gradle wrapper** (`gradlew` / `gradlew.bat`) — always prefer it over a system `gradle`.
- Gradle version: **2.14.1** (see `gradle/wrapper/gradle-wrapper.properties`).
- Android Gradle plugin: **2.1.3** (see root `build.gradle`).
- `compileSdkVersion 24`, `minSdkVersion 15`, `targetSdkVersion 24`.

## Tests

```bash
# Unit/instrumentation tests (requires connected device or emulator)
./gradlew connectedAndroidTest
```

The only test file is `app/src/androidTest/java/com/bleyl/recurrence/ApplicationTest.java` — a boilerplate `ApplicationTestCase`. There are no unit tests.

## Key Dependencies

- **ButterKnife 8.1.0** — view binding via `@BindView` / `@OnClick` annotations; `ButterKnife.bind(this)` must be called after `setContentView`.
- **Annotation processor** applied via `android-apt` plugin; use `apt` configuration in `build.gradle` (not `annotationProcessor`).
- **Material Dialogs 0.8.6.1** (`afollestad`) — used for colour picker and dialogs.
- **PagerSlidingTabStrip** (`jpardogo`) — tab navigation in `MainActivity`.
- **SQLite via `DatabaseHelper`** — singleton accessed with `DatabaseHelper.getInstance(context)`; always call `.close()` after use.

## Architecture

```
activities/       — UI screens (AppCompatActivity subclasses)
  shortcut/       — Home-screen shortcut variants of CreateEditActivity
adapters/         — RecyclerView adapters (ButterKnife ViewHolder pattern)
database/         — DatabaseHelper (SQLiteOpenHelper singleton)
dialogs/          — DialogFragment subclasses (repeat selector, icon picker, etc.)
fragments/        — TabFragment (reminder list), PreferenceFragment
models/           — Plain Java models: Reminder, Icon, Colour
receivers/        — BroadcastReceivers: Alarm, Boot, Dismiss, Nag, Snooze*
utils/            — Stateless helpers: AlarmUtil, AnimationUtil, DateAndTimeUtil,
                    NotificationUtil, TextFormatUtil
res/
  drawable-*/     — ~230 PNG icons per density bucket (hdpi/mdpi/xhdpi/xxhdpi/xxxhdpi)
  values-*/       — Translations: de, en-rUS, es, fr, hr, hu, it, pl, pt-rBR, ru, sk,
                    zh-rCN, zh-rTW
```

**Data flow:** `Reminder` objects are stored in SQLite via `DatabaseHelper`. `AlarmUtil` schedules/cancels `AlarmManager` alarms that fire `AlarmReceiver`, which calls `NotificationUtil` to post the notification. `BootReceiver` reschedules alarms after device reboot.

## ProGuard (Release)

Rules in `app/proguard-rules.pro`:
- ButterKnife classes and `**$$ViewBinder` are kept.
- Material Dialogs uses `-dontwarn -ignorewarnings`.
- `MissingTranslation` lint warning is suppressed in release builds.

## Code Conventions

- **Java only** — no Kotlin; keep additions in Java for consistency.
- **ButterKnife** for all view references — avoid `findViewById` in Activity/Fragment code.
- **DatabaseHelper singleton** — use `DatabaseHelper.getInstance(context)`, never `new DatabaseHelper(...)`.
- Icon resources are referenced by **string name** at runtime via `getResources().getIdentifier(iconName, "drawable", packageName)` — icon drawable names must match DB-stored strings exactly.
- Dates/times are serialised as strings by `DateAndTimeUtil`; use its `toStringDateAndTime` / `parseDateAndTime` methods consistently — do not use raw date formatting elsewhere.
- `Reminder.DOES_NOT_REPEAT`, `SPECIFIC_DAYS`, etc. are int constants defined on `Reminder` — use them instead of magic numbers.

## Gotchas

- **`android-apt` plugin** (not the standard `annotationProcessor` DSL) is required for ButterKnife's code generation to work with this AGP version.
- **Shared element transitions** (enter/exit in `ViewActivity`) are guarded by `Build.VERSION.SDK_INT >= LOLLIPOP` — keep that guard when touching transition code.
- **Alarm rescheduling on boot** is handled by `BootReceiver`; any new alarm type must also be rescheduled there.
- **`strings-private.xml`** contains values (e.g. email address, URLs) intentionally separated from the translatable `strings.xml` — do not merge them.
- The release build suppresses `MissingTranslation` lint; adding new strings does not require updating all translation files immediately, but should be tracked.

---

## Modernisation Plan

> **Goal:** Bring the app from its 2016 API-24 baseline to a state where it builds and runs correctly on current Android (API 35 / Android 15), targeting `minSdkVersion 29` (Android 10). Preserve all existing functionality and UI. Do not redesign or rewrite anything unnecessarily.
>
> Execute the steps below in order. Each step is self-contained and safe to commit independently. After all steps are complete, run `./gradlew assembleDebug` and verify there are zero errors before installing.

---

### Step 1 — Upgrade the Gradle wrapper

**File:** `gradle/wrapper/gradle-wrapper.properties`

Replace the `distributionUrl` line:

```
distributionUrl=https\://services.gradle.org/distributions/gradle-8.11.1-all.zip
```

No other lines in this file need to change.

**Verify:** No standalone check yet — the wrapper is validated as part of the Step 4 gate below.

---

### Step 2 — Upgrade the root build file

**File:** `build.gradle` (project root)

Replace the entire file with:

```groovy
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath 'com.android.tools.build:gradle:8.7.3'
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}
```

Key changes:
- `jcenter()` → `google()` + `mavenCentral()` (JCenter is shut down).
- AGP bumped from `2.1.3` to `8.7.3`.
- `android-apt` classpath entry deleted entirely (annotation processing is built into AGP 3+).

**Verify:** No standalone check yet — validated as part of the Step 4 gate below.

---

### Step 3 — Upgrade the app build file

**File:** `app/build.gradle`

Replace the entire file with:

```groovy
apply plugin: 'com.android.application'

android {
    compileSdk 35

    defaultConfig {
        applicationId "com.bleyl.recurrence"
        minSdk 29
        targetSdk 35
        versionCode 24
        versionName "1.5"
    }

    buildTypes {
        release {
            minifyEnabled true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }

    lint {
        disable 'MissingTranslation'
    }

    compileOptions {
        sourceCompatibility JavaVersion.VERSION_11
        targetCompatibility JavaVersion.VERSION_11
    }
}

dependencies {
    implementation fileTree(dir: 'libs', include: ['*.jar'])

    // AndroidX core
    implementation 'androidx.appcompat:appcompat:1.7.0'
    implementation 'androidx.recyclerview:recyclerview:1.3.2'
    implementation 'androidx.cardview:cardview:1.0.0'
    implementation 'androidx.localbroadcastmanager:localbroadcastmanager:1.1.0'
    implementation 'androidx.preference:preference:1.2.1'

    // Material Components (replaces com.android.support:design AND
    // com.afollestad.material-dialogs AND com.jpardogo.materialtabstrip)
    implementation 'com.google.android.material:material:1.12.0'

    // ButterKnife (upgraded; same @BindView/@OnClick API)
    implementation 'com.jakewharton:butterknife:10.2.3'
    annotationProcessor 'com.jakewharton:butterknife-compiler:10.2.3'
}
```

Key changes:
- `apply plugin: 'com.neenbedankt.android-apt'` removed.
- `compile` → `implementation` everywhere.
- `apt` → `annotationProcessor` for ButterKnife compiler.
- `buildToolsVersion` removed (auto-managed by AGP 8).
- `lintOptions` block moved to top-level `lint` block (AGP 8 DSL).
- `compileOptions` added to pin Java 11 source/target.
- All `com.android.support:*` replaced with `androidx.*` equivalents.
- `com.afollestad.material-dialogs:commons:0.8.6.1` removed; replaced by Material Components.
- `com.jpardogo.materialtabstrip` removed; `TabLayout` is in Material Components.
- `androidx.preference:preference:1.2.1` added for the settings screen migration.

**Verify:** No standalone check yet — validated as part of the Step 4 gate below.

---

### Step 4 — Enable AndroidX and Jetifier

**File:** `gradle.properties`

Append these two lines at the end of the file:

```properties
android.useAndroidX=true
android.enableJetifier=true
```

`Jetifier` automatically rewrites any `android.support.*` references remaining in transitive dependency bytecode. Combined with the source import updates in Step 6, this completes the support-library migration without touching every file manually.

**Verify (gate for Steps 1–4):** Run:
```bash
./gradlew help
```
Expected: Gradle downloads the 8.11.1 wrapper, resolves AGP 8.7.3, and prints a task list without errors. This confirms the toolchain, repositories, and plugin configuration are all correct. The source will not compile yet (imports not migrated) — that is expected.

---

### Step 5 — Update the manifest

**File:** `app/src/main/AndroidManifest.xml`

Make the following changes (do not alter anything else):

**5a. Add new permissions** inside the `<manifest>` block alongside the existing permissions:

```xml
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
```

- `POST_NOTIFICATIONS` is required as a declared + runtime-requested permission on API 33+.
- `SCHEDULE_EXACT_ALARM` is required for `AlarmManager.setExactAndAllowWhileIdle()` on API 31+. For a sideloaded personal app this is the correct permission (not `USE_EXACT_ALARM`, which is Play-Store-restricted to alarm-clock category apps).

**5b. Add `android:dataExtractionRules`** to the `<application>` tag:

```xml
android:dataExtractionRules="@xml/data_extraction_rules"
```

Keep the existing `android:fullBackupContent="@xml/backup_scheme"` — it is still used on API 30 and below.

**5c. Add `android:exported` to every receiver and activity with an intent filter.** AGP targeting API 31+ requires this to be explicit.

Receivers without intent filters (invoked only via explicit `AlarmManager`/`PendingIntent` — should NOT be reachable from outside the app):
```xml
<receiver android:name=".receivers.AlarmReceiver"        android:exported="false" />
<receiver android:name=".receivers.SnoozeActionReceiver" android:exported="false" />
<receiver android:name=".receivers.SnoozeReceiver"       android:exported="false" />
<receiver android:name=".receivers.DismissReceiver"      android:exported="false" />
<receiver android:name=".receivers.NagReceiver"          android:exported="false" />
```

Receivers with intent filters (must be reachable by the system):
```xml
<receiver
    android:name=".receivers.BootReceiver"
    android:exported="true"
    android:enabled="true">
    ...
</receiver>
```

Activities that must be reachable by the system launcher or shortcut framework:
```xml
<activity android:name=".activities.MainActivity"              android:exported="true" ...>
<activity android:name=".activities.shortcut.ShortcutActivity" android:exported="true" ...>
```

All other activities should have `android:exported="false"` added.

**Verify:** Visual inspection only — confirm every `<receiver>` and `<activity>` element has an explicit `android:exported` attribute. Errors here surface at the next compile gate (Step 9).

---

### Step 6 — Create the data extraction rules XML

**File:** `app/src/main/res/xml/data_extraction_rules.xml` *(new file)*

```xml
<?xml version="1.0" encoding="utf-8"?>
<data-extraction-rules>
    <cloud-backup>
        <exclude domain="database" path="RECURRENCE_DB"/>
    </cloud-backup>
    <device-transfer>
        <exclude domain="database" path="RECURRENCE_DB"/>
    </device-transfer>
</data-extraction-rules>
```

This mirrors the exclusion in the existing `backup_scheme.xml` using the API 31+ format.

**Verify:** Confirm the file exists at `app/src/main/res/xml/data_extraction_rules.xml` and contains both `<cloud-backup>` and `<device-transfer>` blocks.

---

### Step 7 — Migrate all `android.support.*` imports to AndroidX

This is a mechanical find-and-replace across all Java source files under `app/src/main/java/`. Apply every substitution in the table below. Do all files in one pass — do not skip any file that contains a matching import.

| Remove (old import) | Replace with (new import) |
|---|---|
| `android.support.v7.app.AppCompatActivity` | `androidx.appcompat.app.AppCompatActivity` |
| `android.support.v7.app.AlertDialog` | `androidx.appcompat.app.AlertDialog` |
| `android.support.v7.widget.Toolbar` | `androidx.appcompat.widget.Toolbar` |
| `android.support.v7.widget.RecyclerView` | `androidx.recyclerview.widget.RecyclerView` |
| `android.support.v7.widget.CardView` | `androidx.cardview.widget.CardView` |
| `android.support.v7.widget.SwitchCompat` | `androidx.appcompat.widget.SwitchCompat` |
| `android.support.v7.widget.LinearLayoutManager` | `androidx.recyclerview.widget.LinearLayoutManager` |
| `android.support.v7.widget.GridLayoutManager` | `androidx.recyclerview.widget.GridLayoutManager` |
| `android.support.design.widget.CoordinatorLayout` | `androidx.coordinatorlayout.widget.CoordinatorLayout` |
| `android.support.design.widget.Snackbar` | `com.google.android.material.snackbar.Snackbar` |
| `android.support.design.widget.FloatingActionButton` | `com.google.android.material.floatingactionbutton.FloatingActionButton` |
| `android.support.design.widget.TextInputLayout` | `com.google.android.material.textfield.TextInputLayout` |
| `android.support.v4.app.DialogFragment` | `androidx.fragment.app.DialogFragment` |
| `android.support.v4.app.Fragment` | `androidx.fragment.app.Fragment` |
| `android.support.v4.app.FragmentPagerAdapter` | `androidx.fragment.app.FragmentPagerAdapter` |
| `android.support.v4.app.NotificationCompat` | `androidx.core.app.NotificationCompat` |
| `android.support.v4.app.ActivityCompat` | `androidx.core.app.ActivityCompat` |
| `android.support.v4.content.ContextCompat` | `androidx.core.content.ContextCompat` |
| `android.support.v4.content.LocalBroadcastManager` | `androidx.localbroadcastmanager.content.LocalBroadcastManager` |
| `android.support.v4.view.ViewPager` | `androidx.viewpager.widget.ViewPager` |
| `android.support.v4.view.ViewCompat` | `androidx.core.view.ViewCompat` |
| `android.support.annotation.NonNull` | `androidx.annotation.NonNull` |
| `android.support.annotation.Nullable` | `androidx.annotation.Nullable` |
| `android.preference.PreferenceManager` | `androidx.preference.PreferenceManager` |

After substitution, verify there are no remaining `android.support.` strings in any `.java` file:
```bash
grep -r "android\.support\." app/src/main/java/
```
The result must be empty.

**Verify (compile gate for Steps 5–7):** Run:
```bash
./gradlew compileDebugJavaWithJavac
```
Expected: zero errors. At this point all support-library imports are gone and the manifest is correct. Errors about `MaterialDialog` or `PagerSlidingTabStrip` are still expected and will be fixed in Steps 8–9. Any other `cannot find symbol` errors must be fixed before continuing.

---

### Step 8 — Replace `PagerSlidingTabStrip` with `TabLayout`

The `com.jpardogo.materialtabstrip` library is unmaintained and unavailable from `mavenCentral()`. Replace it with `TabLayout` from Material Components.

**File:** `app/src/main/java/com/bleyl/recurrence/activities/MainActivity.java`

1. Remove the import:
   ```java
   import com.astuetz.PagerSlidingTabStrip;
   ```
   Add imports:
   ```java
   import com.google.android.material.tabs.TabLayout;
   import com.google.android.material.tabs.TabLayoutMediator;
   ```

2. Change the `@BindView` field:
   ```java
   // Before:
   @BindView(R.id.tabs) PagerSlidingTabStrip pagerSlidingTabStrip;
   // After:
   @BindView(R.id.tabs) TabLayout tabLayout;
   ```

3. In `onCreate`, replace the tab strip wiring:
   ```java
   // Before:
   pagerSlidingTabStrip.setViewPager(viewPager);
   int pageMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, getResources().getDisplayMetrics());
   viewPager.setPageMargin(pageMargin);

   // After:
   new TabLayoutMediator(tabLayout, viewPager,
       (tab, position) -> tab.setText(adapter.getPageTitle(position))
   ).attach();
   ```

**File:** `app/src/main/res/layout/activity_main.xml`

Find the `<com.astuetz.PagerSlidingTabStrip .../>` element and replace the element name with `com.google.android.material.tabs.TabLayout`. Keep all existing attributes (`android:id`, `android:layout_width`, `android:layout_height`, style attributes, etc.) exactly as they are — only the element tag name changes.

**File:** `app/src/main/java/com/bleyl/recurrence/adapters/ViewPageAdapter.java`

`TabLayoutMediator` calls `getPageTitle(position)`. Verify that `ViewPageAdapter` already overrides `getPageTitle(int position)` and returns a `CharSequence`. If it does, no change is needed. If it does not, add the override returning the appropriate tab label string.

**Verify:** Run:
```bash
./gradlew compileDebugJavaWithJavac
```
Expected: no errors related to `PagerSlidingTabStrip` or `TabLayout`. Material Dialogs errors may still be present until Step 9 is complete.

---

### Step 9 — Migrate `material-dialogs` usages to `MaterialAlertDialogBuilder`

The `com.afollestad.material-dialogs:commons:0.8.6.1` library is removed. Its usages must be replaced with `com.google.android.material.dialog.MaterialAlertDialogBuilder`.

Read each dialog file and replace the `MaterialDialog.Builder` / `MaterialDialog` pattern with `MaterialAlertDialogBuilder`. The API shape is similar — `setTitle()`, `setItems()`, `setPositiveButton()`, `setNegativeButton()`, `.show()` all exist on `AlertDialog.Builder`.

Files to update (read each one first, then make the targeted replacement):

- `app/src/main/java/com/bleyl/recurrence/dialogs/RepeatSelector.java`
- `app/src/main/java/com/bleyl/recurrence/dialogs/AdvancedRepeatSelector.java`
- `app/src/main/java/com/bleyl/recurrence/dialogs/DaysOfWeekSelector.java`
- `app/src/main/java/com/bleyl/recurrence/dialogs/IconPicker.java`
- `app/src/main/java/com/bleyl/recurrence/dialogs/PreferenceNagTimePicker.java`
- Any activity that directly builds a `MaterialDialog` (check `CreateEditActivity.java` and `ViewActivity.java`)

For each file:
1. Remove `import com.afollestad.materialdialogs.MaterialDialog;` and any other `com.afollestad` imports.
2. Add `import com.google.android.material.dialog.MaterialAlertDialogBuilder;`.
3. Replace `new MaterialDialog.Builder(context)` with `new MaterialAlertDialogBuilder(context)`.
4. Replace `.show()` at the end of the builder chain — the method is the same name.
5. For colour-picker usage specifically: `material-dialogs:commons` bundled a colour picker. Replace it with a simple `AlertDialog` containing a grid of colour swatches, or accept the colour as a plain hex string input. Check `CreateEditActivity.java` to see how the colour picker was invoked.

**Verify (compile gate for Steps 7–9):** Run:
```bash
./gradlew compileDebugJavaWithJavac
```
Expected: zero errors. All removed-library symbols should now be resolved. If any `com.afollestad` or `com.astuetz` symbols remain, a file was missed — search and fix before continuing:
```bash
grep -r "com\.afollestad\|com\.astuetz" app/src/main/java/
```
Result must be empty.

---

### Step 10 — Migrate the preferences screen

The framework `android.preference.*` classes are fully removed in the AndroidX path. Migrate to `androidx.preference`.

**File:** `app/src/main/java/com/bleyl/recurrence/fragments/PreferenceFragment.java`

Replace the class declaration:
```java
// Before:
public class PreferenceFragment extends android.preference.PreferenceFragment
        implements SharedPreferences.OnSharedPreferenceChangeListener {

// After:
public class PreferenceFragment extends androidx.preference.PreferenceFragmentCompat
        implements SharedPreferences.OnSharedPreferenceChangeListener {
```

Change the import for `Preference`:
```java
// Before:
import android.preference.Preference;
// After:
import androidx.preference.Preference;
```

Change `getPreferenceScreen().getSharedPreferences()` to:
```java
androidx.preference.PreferenceManager.getDefaultSharedPreferences(requireContext())
```

The `addPreferencesFromResource(R.xml.prefs)` call is identical in the AndroidX API — no change needed there.

`onStart`/`onStop` listener registration — identical API, no change needed.

**File:** `app/src/main/java/com/bleyl/recurrence/activities/PreferenceActivity.java`

Replace `getFragmentManager()` with `getSupportFragmentManager()`:
```java
// Before:
getFragmentManager().beginTransaction().replace(R.id.content_frame, new PreferenceFragment()).commit();
// After:
getSupportFragmentManager().beginTransaction().replace(R.id.content_frame, new PreferenceFragment()).commit();
```

Remove the dead `getActionBar()` call (it always returns null when using AppCompat and does nothing):
```java
// Delete this line:
if (getActionBar() != null) getActionBar().setDisplayHomeAsUpEnabled(true);
```

**File:** `app/src/main/res/xml/prefs.xml`

`RingtonePreference` was removed from the framework and does not exist in `androidx.preference`. Replace it with a standard `Preference` that launches the system ringtone picker:

```xml
<!-- Before: -->
<RingtonePreference
    android:showDefault="true"
    android:key="NotificationSound"
    android:title="@string/checkbox_sound"
    android:defaultValue="content://settings/system/notification_sound"
    android:ringtoneType="notification" />

<!-- After: -->
<Preference
    android:key="NotificationSound"
    android:title="@string/checkbox_sound" />
```

Then in `PreferenceFragment.java`, wire up the click to launch `RingtoneManager.ACTION_RINGTONE_PICKER` and save the result via `ActivityResult`. Add the following to `onCreate` in `PreferenceFragment`:

```java
ActivityResultLauncher<Intent> ringtonePicker = registerForActivityResult(
    new ActivityResultContracts.StartActivityForResult(),
    result -> {
        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
            Uri uri = result.getData().getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
            String uriString = (uri != null) ? uri.toString() : "";
            androidx.preference.PreferenceManager
                .getDefaultSharedPreferences(requireContext())
                .edit().putString("NotificationSound", uriString).apply();
        }
    });

findPreference("NotificationSound").setOnPreferenceClickListener(pref -> {
    Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
    String current = androidx.preference.PreferenceManager
        .getDefaultSharedPreferences(requireContext())
        .getString("NotificationSound", "content://settings/system/notification_sound");
    if (!current.isEmpty()) {
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(current));
    }
    ringtonePicker.launch(intent);
    return true;
});
```

Add imports: `android.app.Activity`, `android.media.RingtoneManager`, `android.net.Uri`, `androidx.activity.result.ActivityResultLauncher`, `androidx.activity.result.contract.ActivityResultContracts`.

**File:** `app/src/main/java/com/bleyl/recurrence/dialogs/PreferenceNagTimePicker.java`

Read this file. It extends a framework `Preference` or `DialogPreference`. Update the base class import to the `androidx.preference` equivalent:
- `android.preference.DialogPreference` → `androidx.preference.DialogPreference`
- `android.preference.Preference` → `androidx.preference.Preference`

**Verify:** Run:
```bash
./gradlew compileDebugJavaWithJavac
```
Expected: zero errors in the preferences package. Also confirm no `android.preference.` imports remain:
```bash
grep -r "android\.preference\." app/src/main/java/
```
Result must be empty.

---

### Step 11 — Fix `NotificationUtil` (channel, permissions, PendingIntent flags)

**File:** `app/src/main/java/com/bleyl/recurrence/utils/NotificationUtil.java`

This file has multiple issues that cause silent failure or crashes on modern Android. Apply all fixes below.

**11a. Add imports:**
```java
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.media.AudioAttributes;
import android.net.Uri;
import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;
```
Remove `import android.support.v4.app.NotificationCompat;` and `import android.preference.PreferenceManager;`.

**11b. Add a channel ID constant and `createNotificationChannel()` helper at the top of the class:**

```java
private static final String CHANNEL_ID = "recurrence_reminders";

private static void createNotificationChannel(Context context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        String soundUri = prefs.getString("NotificationSound", "content://settings/system/notification_sound");
        Uri sound = soundUri.isEmpty() ? null : Uri.parse(soundUri);

        long[] vibrationPattern = prefs.getBoolean("checkBoxVibrate", true) ? new long[]{0, 300, 0} : null;

        NotificationChannel channel = new NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.app_name),
            NotificationManager.IMPORTANCE_HIGH
        );
        channel.enableLights(prefs.getBoolean("checkBoxLED", true));
        channel.setLightColor(Color.BLUE);
        if (sound != null) {
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build();
            channel.setSound(sound, audioAttributes);
        }
        if (vibrationPattern != null) {
            channel.enableVibration(true);
            channel.setVibrationPattern(vibrationPattern);
        }

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            nm.createNotificationChannel(channel);
        }
    }
}
```

**11c. At the very start of `createNotification()`, call the channel helper:**
```java
createNotificationChannel(context);
```

**11d. Fix the `NotificationCompat.Builder` constructor** — pass the channel ID:
```java
// Before:
NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
// After:
NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
```

**11e. Remove the `setPriority` block entirely** — priority is controlled by channel importance on API 26+:
```java
// Delete these lines:
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
    builder.setPriority(Notification.PRIORITY_HIGH);
}
```
Also remove `import android.app.Notification;` if it is only used for `Notification.PRIORITY_HIGH`.

**11f. Remove the per-notification sound, LED, and vibration builder calls** — these are ignored on API 26+ and now live on the channel:
```java
// Delete these blocks:
if (soundUri.length() != 0) {
    builder.setSound(Uri.parse(soundUri));
}
if (sharedPreferences.getBoolean("checkBoxLED", true)) {
    builder.setLights(Color.BLUE, 700, 1500);
}
if (sharedPreferences.getBoolean("checkBoxVibrate", true)) {
    long[] pattern = {0, 300, 0};
    builder.setVibrate(pattern);
}
```
Also remove the `soundUri` variable declaration. The `sharedPreferences` variable is still needed for `checkBoxNagging`, `checkBoxOngoing`, `checkBoxMarkAsDone`, `checkBoxSnooze` — keep those reads.

**11g. Add `FLAG_IMMUTABLE` to all four `PendingIntent` calls in this file:**
```java
// content intent:
PendingIntent pending = PendingIntent.getActivity(context, reminder.getId(), viewIntent,
    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

// snooze action:
PendingIntent pendingSnooze = PendingIntent.getBroadcast(context, reminder.getId(), snoozeIntent,
    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

// dismiss/delete intent:
PendingIntent pendingDismiss = PendingIntent.getBroadcast(context, reminder.getId(), swipeIntent,
    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

// mark-as-done action:
PendingIntent pendingIntent = PendingIntent.getBroadcast(context, reminder.getId(), intent,
    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
```

**Verify:** Run:
```bash
./gradlew compileDebugJavaWithJavac
```
Expected: zero errors in `NotificationUtil`. Also confirm `FLAG_IMMUTABLE` is present and `PRIORITY_HIGH` is absent:
```bash
grep "PRIORITY_HIGH\|FLAG_IMMUTABLE\|CHANNEL_ID" app/src/main/java/com/bleyl/recurrence/utils/NotificationUtil.java
```
Expected: `FLAG_IMMUTABLE` and `CHANNEL_ID` present; `PRIORITY_HIGH` absent.

---

### Step 12 — Fix `AlarmUtil` (PendingIntent flags + exact-alarm guard)

**File:** `app/src/main/java/com/bleyl/recurrence/utils/AlarmUtil.java`

**12a. Add `FLAG_IMMUTABLE` to both `PendingIntent.getBroadcast()` calls** (`setAlarm` and `cancelAlarm`):
```java
PendingIntent pendingIntent = PendingIntent.getBroadcast(context, notificationId, intent,
    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
```

**12b. Replace the `setAlarm` method body** with an exact-alarm guard for API 31+:

```java
public static void setAlarm(Context context, Intent intent, int notificationId, Calendar calendar) {
    intent.putExtra("NOTIFICATION_ID", notificationId);
    AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    PendingIntent pendingIntent = PendingIntent.getBroadcast(context, notificationId, intent,
        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
        // Exact-alarm permission revoked by user — fall back to inexact
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
    } else {
        // minSdk is 29 (>= M), so setExactAndAllowWhileIdle is always available
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
    }
}
```

Note: `minSdk` is 29, so the pre-M `alarmManager.set()` and pre-KITKAT branches are unreachable and can be removed.

**Verify:** Run:
```bash
./gradlew compileDebugJavaWithJavac
```
Also confirm `FLAG_IMMUTABLE` is present and old SDK branches are gone:
```bash
grep "FLAG_IMMUTABLE\|KITKAT\|canScheduleExactAlarms" app/src/main/java/com/bleyl/recurrence/utils/AlarmUtil.java
```
Expected: `FLAG_IMMUTABLE` and `canScheduleExactAlarms` present; `KITKAT` absent.

---

### Step 13 — Fix `SnoozeActionReceiver` (removed broadcast)

**File:** `app/src/main/java/com/bleyl/recurrence/receivers/SnoozeActionReceiver.java`

Delete the two lines that send `ACTION_CLOSE_SYSTEM_DIALOGS` — this broadcast was removed in API 31 and throws a `SecurityException`:

```java
// Delete these two lines:
Intent closeIntent = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
context.sendBroadcast(closeIntent);
```

No replacement is needed. The snooze dialog activity opens via `startActivity` on the next line, which is sufficient.

**Verify:** Confirm the lines are gone:
```bash
grep "ACTION_CLOSE_SYSTEM_DIALOGS" app/src/main/java/com/bleyl/recurrence/receivers/SnoozeActionReceiver.java
```
Result must be empty. Then run:
```bash
./gradlew compileDebugJavaWithJavac
```

---

### Step 14 — Fix `ShortcutActivity` (deprecated shortcut API)

**File:** `app/src/main/java/com/bleyl/recurrence/activities/shortcut/ShortcutActivity.java`

The `Intent.EXTRA_SHORTCUT_INTENT` / `EXTRA_SHORTCUT_NAME` / `EXTRA_SHORTCUT_ICON_RESOURCE` API was removed in API 34. Replace the entire `onCreate` body with `ShortcutManagerCompat.requestPinShortcut()`:

```java
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.drawable.IconCompat;

@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    if (ShortcutManagerCompat.isRequestPinShortcutSupported(this)) {
        Intent launchIntent = new Intent(this, CreateEditShortcut.class);
        launchIntent.setAction(Intent.ACTION_VIEW);

        ShortcutInfoCompat shortcutInfo = new ShortcutInfoCompat.Builder(this, "add_reminder")
            .setShortLabel(getString(R.string.add_reminder))
            .setIcon(IconCompat.createWithResource(this, R.mipmap.ic_launcher))
            .setIntent(launchIntent)
            .build();

        ShortcutManagerCompat.requestPinShortcut(this, shortcutInfo, null);
    }

    finish();
}
```

Change `extends Activity` to `extends AppCompatActivity` and add `import androidx.appcompat.app.AppCompatActivity;`.

**Verify:** Confirm old shortcut extras are gone:
```bash
grep "EXTRA_SHORTCUT" app/src/main/java/com/bleyl/recurrence/activities/shortcut/ShortcutActivity.java
```
Result must be empty. Then run:
```bash
./gradlew compileDebugJavaWithJavac
```

---

### Step 15 — Add runtime permission requests in `MainActivity`

**File:** `app/src/main/java/com/bleyl/recurrence/activities/MainActivity.java`

Add the following imports:
```java
import android.Manifest;
import android.app.AlarmManager;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
```

At the end of `onCreate()`, after all existing setup, add:

```java
requestRequiredPermissions();
```

Add the helper method to the class:

```java
private void requestRequiredPermissions() {
    // POST_NOTIFICATIONS — required on API 33+
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
        }
    }

    // SCHEDULE_EXACT_ALARM — direct user to system settings if not granted
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (!alarmManager.canScheduleExactAlarms()) {
            Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        }
    }
}
```

**Verify:** Run:
```bash
./gradlew compileDebugJavaWithJavac
```
Expected: zero errors. This is the final compile gate — all source changes are now complete.

---

### Step 16 — Update `proguard-rules.pro`

**File:** `app/proguard-rules.pro`

Replace the entire file with:

```pro
# ButterKnife
-keep class butterknife.** { *; }
-dontwarn butterknife.internal.**
-keep class **_ViewBinding { *; }

-keepclasseswithmembernames class * {
    @butterknife.* <fields>;
}
-keepclasseswithmembernames class * {
    @butterknife.* <methods>;
}

# Material Components
-keep class com.google.android.material.** { *; }
-dontwarn com.google.android.material.**

# AndroidX
-keep class androidx.** { *; }
-dontwarn androidx.**
```

Key changes:
- `**$$ViewBinder` → `**_ViewBinding` (ButterKnife 10 uses a different generated class name suffix).
- `android.support.design.**` keep rule replaced with `com.google.android.material.**` and `androidx.**`.
- Removed the blanket `-dontwarn -ignorewarnings` (too broad; scoped replacements above are sufficient).

**Verify:** ProGuard only runs on release builds. Check it now:
```bash
./gradlew assembleRelease
```
Expected: BUILD SUCCESSFUL. If R8 reports missing classes, add a scoped `-dontwarn com.example.MissingClass` for the specific class reported.

---

### Step 17 — Verify the build

After all steps are complete:

```bash
./gradlew assembleDebug
```

Expected outcome: BUILD SUCCESSFUL with zero errors. Warnings about deprecated APIs or unused resources are acceptable.

If there are compilation errors:
1. `cannot find symbol` for `android.support.*` — a file was missed in Step 7; fix the import.
2. `cannot find symbol` for Material Dialogs types — a file was missed in Step 9; replace the usage.
3. `error: package androidx.preference does not exist` — confirm `androidx.preference:preference:1.2.1` is in `app/build.gradle`.
4. R8/ProGuard errors on release build — add a targeted `-dontwarn` for the specific class reported.

---

### Step 18 — Install and smoke-test on device

```bash
./gradlew installDebug
```

Manual test checklist:
- [ ] App launches without crash.
- [ ] On API 33+ device: system dialog appears requesting notification permission; grant it.
- [ ] On API 31+ device: system settings page for exact alarms appears on first launch; grant it.
- [ ] Create a reminder set 1–2 minutes in the future.
- [ ] Lock the screen and wait — notification should appear at the scheduled time.
- [ ] Notification actions (snooze, mark as done) work correctly.
- [ ] Settings screen opens and all preferences are visible.
- [ ] Sound preference can be changed via the ringtone picker.
- [ ] Reboot device — active reminders should still fire after reboot (`BootReceiver`).
