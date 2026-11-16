# SubCycler

SubCycler is a local-first Android subscription tracker. Track recurring bills, get renewal reminders, and plan breaks between paid months. The app is built with Kotlin and Jetpack Compose and runs without a SubCycler account.

## What you can do

- Track streaming services, software, and other subscriptions, with onboarding presets and support for several currencies.
- Plan a 45-day rotation with 30 days of paid access and a 15-day break, or keep a subscription active year-round.
- Alternate two services and see projected savings based on your prices and schedules.
- Get reminders to turn off auto-renewal or resubscribe. Open official billing or help pages from the app to make those changes yourself.
- Opt in to suggestions based on selected device calendars.

## Build and run

Install JDK 17 and Android SDK Platform 35 using [Android Studio](https://developer.android.com/studio) or the SDK command-line tools. Run SubCycler on an emulator or device with Android 8.0 (API 26) or later.

For a terminal build, set `ANDROID_HOME` to your SDK path if Android Studio hasn't created `local.properties`. Add the SDK's `platform-tools` directory to your `PATH` to use `adb`.

```bash
git clone https://github.com/SafalFrom2050/SubCycler.git
cd SubCycler
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

On Windows PowerShell, use `./gradlew.bat :app:assembleDebug`. You can also open the project in Android Studio, sync Gradle, and run the `app` configuration.

## Contribute

Pull requests are welcome. Pick up a [good first issue](https://github.com/SafalFrom2050/SubCycler/contribute) or open a [bug report or feature request](https://github.com/SafalFrom2050/SubCycler/issues/new/choose).

See the [contributor guide](CONTRIBUTING.md) for the fork-to-PR workflow and local test commands. The [architecture guide](ARCHITECTURE.md), [design guide](DESIGN.md), and [feature plans](PLANS.md) describe the codebase.

## Data and privacy

SubCycler stores subscription records on the device using Room. Android backup and device transfer settings can include the subscription database. Calendar suggestions are off by default. If enabled, the app requests read access and uses the calendars you select. Billing links open outside SubCycler.

## License

SubCycler is available under the [MIT License](LICENSE). The bundled Gradle Wrapper files are available under the [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0).
