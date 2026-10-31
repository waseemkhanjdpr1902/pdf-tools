# PDF Pocket — Scanner & Tools

An Android PDF utility built from [sammyZi/pdf-tools](https://github.com/sammyZi/pdf-tools), retaining its MIT licence and third-party notices.

PDF Pocket keeps the existing local scanner, OCR and PDF tools and adds an optional ad-free subscription through Google Play/RevenueCat, a consent-aware AdMob home banner, purchase restoration and static support pages.

**Development preview:** support contact is pdftoolsupport@gmail.com. No live ad IDs, payment products or signing secrets are committed. Debug uses Google test ads; subscriptions remain unavailable until a public RevenueCat SDK key and store products are configured. Release ads default off. A working build and device/store tests must be confirmed before release.

## Start here

Read **[LAUNCH_SETUP.md](LAUNCH_SETUP.md)** for billing, consent, hosting, signing, test and release instructions. The source namespace remains `com.sammyi57.pdftools`; the new Android app ID is `com.waseem.pdfpocket`.

```bash
./gradlew :app:testReleaseUnitTest :app:assembleDebug :app:bundleRelease
```

JDK 17, Android SDK 36 + extension 19 and online dependency resolution are required. GitHub Actions builds verification APK/AAB artifacts on pull requests. These are test artifacts, not production-signed uploads. Use `:app:storeBundle` with the required configuration for the store release.

## Policy/support pages

`docs/` contains a static homepage, privacy policy, terms, support and an unconfigured `app-ads.txt`. `tools/build_support.py` generates the same policy text for the app and website. The original developer's email and feedback form have been removed. No marketing analytics are added.

## What remains free

Scanning, merge/split, compression, page organisation, fill and drawn signature, local Latin-text OCR, password tools and optional Google Drive sync. Premium currently removes ads; it does not promise unimplemented cloud storage or additional OCR languages. The built-in PDF viewer requires a newer supported Android system; older devices use an external viewer.

## Licences and attribution

Application changes and upstream application code: [MIT](LICENSE). Keep the copyright and permission notices. Dependencies, fonts and the scanner model have separate terms listed in [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md). Google/RevenueCat service SDKs also have their applicable terms. Working product name is provisional pending store/trademark checks.
