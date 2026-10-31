# PDF Pocket launch setup

Working store title: **PDF Pocket: Scan & PDF Tools** (28 characters). This name has not been cleared for trademark or store availability. Android application ID is `com.waseem.pdfpocket`; debug adds `.debug`. Choose the final ID before the first Play upload; changing it later creates a different app.

## Current implementation

- Existing PDF/scanner tools remain free. Premium removes ads; no new document limits are imposed.
- Native AdMob banner on the Tools home screen, labelled and separated from document controls. No full-screen or launch ads. Debug builds use Google's test app and banner IDs.
- UMP consent flow after in-app policy acceptance, with a Settings privacy-options entry when required.
- RevenueCat Android SDK for Google Play receipt verification, product loading, purchases and restore. Monthly/annual offers use store-localised total price and period, with automatic-renewal disclosure. No hardcoded price, fake purchase or locally stored premium toggle.
- Premium customer status refreshes on app resume. An unresolved configured entitlement suppresses ads until resolved; document tools stay available when offline.
- Static website source in `docs/` and matching generated in-app policy text. The support email is deliberately unset. Temporary support uses this project's public issues; never post documents there.

## Build and test

Requires JDK 17, Android SDK 36 and extension 19, and network access to Google's/Maven/Gradle dependency hosts. The GitHub Actions workflow runs for the implementation pull request. A fork may require its owner to enable Actions.

```bash
./gradlew :app:testReleaseUnitTest :app:assembleDebug :app:bundleRelease
```

CI APKs and AABs are **verification artifacts**: release falls back to a debug signing key without an upload key. Do not submit these to production. Use `:app:storeBundle` with production configuration and your own upload key for the store bundle. `storeBundle` validates configuration; regular `bundleRelease` intentionally remains available for checks.

Do the upstream device checks on a real Android phone as well:

```bash
./gradlew :app:connectedDebugAndroidTest -PsideBySide
```

Verify Android 7/11 external-viewer fallback and modern Android built-in viewer; PDFs with passwords, large scans and multiple pages; offline operations; background/resume; and rotation/process recreation. The bundled OCR supports Latin text, not a promise of Hindi/Urdu recognition.

## Google Play and RevenueCat

1. Create the app in your existing Play Console using the final application ID.
2. Set up the payments profile and complete store declarations. Create an auto-renewing subscription such as `pdf_pocket_premium`, with monthly and annual base plans. Select and validate prices; no live prices are committed.
3. Create a RevenueCat Google Play app with the **same** application ID. Connect Google Play using RevenueCat's current instructions. Private service-account credentials belong in the provider dashboard, never this repository or the Android app.
4. Import the products; attach both plans to entitlement **`premium`**. Set a current/default offering with only standard, non-promotional `$rc_monthly` and `$rc_annual` packages. Do not enable trials/intro discounts until the purchase UI is extended to disclose their stages accurately.
5. Set `REVENUECAT_PUBLIC_KEY` to the public app SDK key (`goog_...` for Play). Debug can use a separately configured test-store key. Only the public SDK key goes into BuildConfig.
6. Upload an internally tested release and add license testers. Test monthly/yearly purchases, cancellation without payment, pending-payment completion, purchase restoration, expiry, refund/revocation, network loss, and returning from the store. Confirm ads disappear only when `premium` is active and can reappear after expiry. Confirm the store's renewal terms and prices match the screen.
7. Check current RevenueCat fees/limits and retention settings before launch. RevenueCat is an additional hosted dependency, not free unlimited infrastructure promised by this repository.

## AdMob

1. Create your own Android app and banner unit in AdMob.
2. Configure geographic privacy messages in Privacy & messaging; test consent required, not required, declined, privacy-choice changes and offline/error paths. A privacy acceptance checkbox is not ad-personalisation consent.
3. Supply `ADMOB_APP_ID`, `ADMOB_BANNER_ID`, and `LIVE_ADS_ENABLED=true` only after consent, billing and policy review. Never use live ads while testing; never click your ads.
4. Copy the exact authorised-seller line from AdMob into `docs/app-ads.txt`. The committed file has no fabricated publisher ID. Publish it at the root of the developer-website domain listed on Google Play.
5. Complete AdMob verification/review steps and payment profile. Integration alone does not guarantee ad delivery or revenue.

## Website and support contact

Create the support Gmail, then regenerate the static website and embedded policy with the public email (not a password):

```bash
SUPPORT_EMAIL='your-created-address@gmail.com' python3 tools/build_support.py
```

Commit generated changes. Set that same email in Android build configuration. Deploy **only `docs/`**, not the Android project, to static hosting (Vercel: framework Other, root `docs`, no build command). No paid domain is required initially. Confirm `/privacy.html`, `/terms.html`, `/support.html`, `/app-ads.txt` are publicly accessible. Set `WEBSITE_URL=https://your-live-host` for the Android build, so purchase-screen policy links point to the hosted pages. Current pages are draft development pages; remove preview/noindex notices only after final review.

Review actual SDK processing, child-audience declaration, retention, consent configuration, contact identity and regional privacy requirements before submission. Fill Play Data safety from the **final** SDKs/configuration; do not copy the original app's no-data/no-ad answers. Preserve MIT and third-party notices. Refresh store screenshots from your build; upstream screenshots are not your app's listing assets.

## Build configuration

Public IDs can be set as Gradle properties in local user configuration or environment variables. Signing material stays outside Git.

| Setting | Default | Production requirement |
|---|---|---|
| `REVENUECAT_PUBLIC_KEY` | Empty, billing disabled | Your public Google Play SDK key |
| `SUPPORT_EMAIL` | Empty, public project link fallback | Your working support email |
| `WEBSITE_URL` | Empty, source policy fallback | Public HTTPS website |
| `LIVE_ADS_ENABLED` | `false` in release | Set true only when ready |
| `ADMOB_APP_ID` | Google test app | Your app ID if live ads enabled |
| `ADMOB_BANNER_ID` | Empty in release | Your banner unit if live ads enabled |
| `PDF_TOOLS_UPLOAD_STORE_FILE` | Unset | Own keystore path (Gradle property) |
| `PDF_TOOLS_UPLOAD_STORE_PASSWORD` | Unset | Private Gradle property |
| `PDF_TOOLS_UPLOAD_KEY_ALIAS` | Unset | Private Gradle property |
| `PDF_TOOLS_UPLOAD_KEY_PASSWORD` | Unset | Private Gradle property |

Do not paste passwords, RevenueCat private keys, service-account JSON or keystores into GitHub issues or chat.
