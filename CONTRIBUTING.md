# Contributing to SubCycler

Bug fixes, tests, documentation, and accessibility improvements are welcome. Browse the [good first issues](https://github.com/SafalFrom2050/SubCycler/contribute) for a small starting point. For a larger feature, open an [issue](https://github.com/SafalFrom2050/SubCycler/issues/new/choose) to discuss the intended behavior early.

## Set up your fork

Fork [SubCycler](https://github.com/SafalFrom2050/SubCycler) on GitHub. Clone your fork and replace `YOUR_USERNAME` in the URL with your GitHub username.

```bash
git clone https://github.com/YOUR_USERNAME/SubCycler.git
cd SubCycler
git switch -c describe-your-change
```

Follow Quick Start in the [README](README.md) to set up JDK 17, Android SDK Platform 35, and an emulator or device.

Debug builds open onboarding on each cold launch so the first-run flow is easy to test. Complete or skip onboarding to reach the dashboard.

## Test your change

Run the same build, JVM unit tests, and lint checks as GitHub Actions.

```bash
./gradlew :app:assembleDebug testDebugUnitTest lintDebug
```

On Windows PowerShell, use `./gradlew.bat :app:assembleDebug testDebugUnitTest lintDebug`.

For UI or Android integration changes, try the affected flow on an emulator or device. Run `./gradlew :app:connectedDebugAndroidTest` with a test device connected to run the instrumentation suite. Use sample subscriptions and calendar events in screenshots and test data.

The [architecture guide](ARCHITECTURE.md) describes the `core`, `feature`, and `app` modules. Keep business logic in the domain layer and UI changes consistent with the [design guide](DESIGN.md).

## Open your pull request

1. Commit your changes and push the branch to your fork.
2. Open a pull request against `SafalFrom2050/SubCycler:main`.
3. Describe what changed, link any related issue, and record the checks you ran. Add a screenshot or short recording for visible changes.

See the [code of conduct](CODE_OF_CONDUCT.md) for community expectations and the [security policy](SECURITY.md) for private vulnerability reports.
