# Play release preparation

App name: **Tennis Score for Wear OS**
Android application ID: **`com.thephani.tennisscore`**
Support email: **thephani.old@gmail.com**

## Privacy policy on GitHub Pages

The prepared page is [`docs/privacy/index.md`](../docs/privacy/index.md), and its intended public URL is:

`https://thephani.github.io/tennis-scoring-wear-os/privacy/`

The URL returned HTTP 200 on September 30, 2026, but the live page still had the September 26 Singles-only wording. Deploy the local update before entering the URL in Play Console.

1. Push the updated privacy page to the public repository without any keys, passwords, or private files.
2. Confirm GitHub Pages deploys the branch's **`/docs`** folder.
3. Open the URL above in a private browser window and verify the updated Singles and Doubles wording and contact email are visible.
4. Paste that exact public URL into **Play Console → Policy and programs → App content → Privacy policy**. The app itself also has a Privacy screen.

Do not guess the URL: the GitHub username, repository name, and Pages settings determine it.

## Create an upload key and signed bundle

Use [Android Studio's signed-bundle flow](https://developer.android.com/studio/publish/app-signing): **Build → Generate Signed Bundle / APK → Android App Bundle → Create new**. Save the `.jks` keystore outside this project, set **Validity (years)** to at least **25**, choose a long unique password, and keep its password and alias in your password manager. Back up the keystore securely. Never commit the keystore or passwords. Google Play requires the certificate to expire after October 22, 2033.

Choose the **release** build variant. Android Studio creates a signed `.aab` file. Verify it with `jarsigner -verify -verbose -certs PATH_TO_SIGNED_AAB` and confirm that it says the jar is verified and shows the upload certificate. The ordinary `./gradlew bundleRelease` output is currently unsigned and must not be uploaded.

The first signing attempt made on September 30, 2026 expired too soon and must not be uploaded. The replacement bundle at `app/release/app-release.aab` was regenerated with a certificate expiring September 24, 2051 and passed `jarsigner` verification on September 30, 2026.

In Play Console, let **Play App Signing** manage the app signing key; use your local key as the separate upload key. Keep the same upload key for future releases, unless you request a reset through Play Console. Do not upload a bundle until the listing, privacy policy, and test track are ready.

## Store assets

- Copy text from [`store-listing.md`](store-listing.md) into the Play listing and review it there.
- Use the prepared files under `store-assets/`: a 512 × 512 app icon, 1024 × 500 feature graphic, and opaque 454 × 454 Wear OS screenshots.
- Use screenshots from the running Wear OS app, without watch frames, added words, or extra backgrounds. The Wear OS screenshots must be square and at least 384 × 384 pixels.
- Review the final app icon and other image requirements shown by Play Console before upload.
- Opt in to the **Wear OS** form factor in Play Console and add the Wear OS screenshots.

Use [`play-console-declarations.md`](play-console-declarations.md) for the current Console answers and [`release-checklist.md`](release-checklist.md) for the remaining sequence. The first release still needs a signed AAB, physical-watch testing, and a closed-test release before public rollout.
