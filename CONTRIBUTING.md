# Contributing to Pocket AI

Thank you for contributing to Pocket AI! We welcome contributions that uphold the project's core principles: **100% offline privacy**, **clean architecture**, and a **Samsung One UI design language**.

---

## Code Guidelines

1. **Offline Integrity**: Never add network libraries, cloud dependencies, or telemetry SDKs. The app must remain operable without internet connectivity.
2. **Single-Activity Architecture**: Follow MVVM with Android Jetpack Compose and unidirectional data flow.
3. **Storage Access Framework**: Always use content URIs and scoped storage. Do not assume arbitrary direct filesystem paths.
4. **Honest Runtime State**: Do not mock or fake model execution. If native libraries or model files are unavailable, report errors clearly to the user.
5. **Memory Safety**: Guard against high-memory allocations on constrained Android devices.

---

## Submitting Pull Requests

1. Fork the repository and create a feature branch (`git checkout -b feature/my-feature`).
2. Verify all tests pass locally:
   ```bash
   ./gradlew testDebugUnitTest
   ./gradlew assembleDebug
   ```
3. Commit logical, small changes with descriptive commit messages.
4. Open a pull request against the `main` branch.
