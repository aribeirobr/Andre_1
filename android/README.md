# Shake Flashlight (Samsung Galaxy S23)

Turns the phone's flashlight on and off with a Motorola-style **double chop**
(two quick downward flicks). Runs as a foreground service so it works even
when the app is in the background or the screen is off.

- `minSdk` 26, `targetSdk` 34, Kotlin, Jetpack Compose.
- Detection is done in `ChopDetector.kt` from raw linear-accelerometer
  samples — no third-party gesture libraries.

## Why no APK in this repo?

The sandbox this code was scaffolded in has Google's artifact hosts
(`dl.google.com`, `maven.google.com`) firewalled off, so neither the Android
SDK nor the Android Gradle Plugin can be downloaded from it. The APK must be
built on a machine that can reach Google's Maven repo — two easy paths:

## Build the APK with Android Studio (easiest)

1. Install [Android Studio](https://developer.android.com/studio).
2. File → Open → select the `android/` folder of this repo.
3. Let Gradle sync (it will download AGP + AndroidX automatically).
4. Build → **Build Bundle(s) / APK(s) → Build APK(s)**.
5. The APK is written to `android/app/build/outputs/apk/debug/app-debug.apk`.

## Build the APK from the command line

Requires Android command-line tools + platform 34 + build-tools 34.0.0.

```bash
# one-time: install the SDK and accept licenses
export ANDROID_HOME="$HOME/android-sdk"
mkdir -p "$ANDROID_HOME/cmdline-tools"
curl -fsSL -o /tmp/clt.zip \
  https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
unzip -q /tmp/clt.zip -d "$ANDROID_HOME/cmdline-tools"
mv "$ANDROID_HOME/cmdline-tools/cmdline-tools" "$ANDROID_HOME/cmdline-tools/latest"
yes | "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" --licenses
"$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" \
  "platform-tools" "platforms;android-34" "build-tools;34.0.0"

# build
cd android
./gradlew :app:assembleDebug
# -> app/build/outputs/apk/debug/app-debug.apk
```

## Install on the Galaxy S23

1. Copy `app-debug.apk` to the phone (USB, Google Drive, email — anything).
2. Open the file in Files / My Files.
3. Android will ask you to allow "Install unknown apps" for the file
   manager — approve once.
4. Tap **Install**, then **Open**.
5. Grant **Camera** permission (required to toggle the torch).
6. On Android 13+ grant **Notifications** permission (for the "running"
   notification).
7. Flip the **Start detection** switch. The notification **"Shake detection
   running"** appears.
8. Pick a **Gesture** and **Sensitivity** in the UI, then perform the
   gesture. The torch toggles on; repeat to toggle off. Defaults are
   *Double chop* at *Normal* sensitivity.

Use the **Test flashlight** button first to confirm your camera permission
and hardware are working before relying on the gesture.

## Choosing a gesture

The app offers five gestures — pick whichever feels most natural:

| Gesture         | Motion                                                                                 |
| --------------- | -------------------------------------------------------------------------------------- |
| Double chop     | Two quick downward wrist-flicks (~1 s apart). Motorola-style.                          |
| Single shake    | One firm shake in any direction. Easiest; may false-trigger.                           |
| Triple shake    | Three shakes in a row within ~1.5 s. Very hard to trigger by accident.                 |
| Wrist twist     | Flip the phone quickly on its screen axis (wrist rotation ~90°). Uses the gyroscope.   |
| Custom motion   | Record your own motion (e.g. an "L", a figure-8, a specific shake pattern). See below. |

### Custom motion

Select **Custom motion** and tap **Record pattern**. A 3-second countdown
runs, then the app records your linear-acceleration trace for 2 seconds —
do the motion you want to use during that window. Incoming motion is then
compared to the recorded trace with Dynamic Time Warping (DTW). A
**Match strictness** slider (Very loose → Very strict) sets how close a
live motion must be to the recorded one to fire. Start loose, tighten only
if the torch toggles by accident.

## Tuning sensitivity

The **Sensitivity** slider on the main screen has 5 steps from *Very sensitive*
to *Very stiff*. Lower it if gestures are too hard to trigger; raise it if the
torch toggles accidentally. Exact thresholds live in
`app/src/main/kotlin/com/andre/shakeflashlight/SensitivityProfile.kt` and
can be tweaked there.

Gesture detection logic is in
`app/src/main/kotlin/com/andre/shakeflashlight/ChopDetector.kt`
(chop/shake detectors) and `WristTwistDetector.kt` (gyro-based twist).
