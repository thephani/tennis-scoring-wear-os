# Play release preparation

App name: **Tennis Score for Wear OS**
Android application ID: **`com.thephani.tennisscore`**
Support email: **thephani.old@gmail.com**

## Privacy policy on GitHub Pages

The prepared page is [`docs/privacy/index.md`](../docs/privacy/index.md). This checkout has no Git remote, so it has no public policy URL yet.

1. Put this project in a GitHub repository you control. Review what you will make public before pushing; do not publish keys, passwords, or private files.
2. In that repository, open **Settings → Pages**. Choose **Deploy from a branch**, then the branch containing this project and the **`/docs`** folder.
3. After deployment, open `https://YOUR-GITHUB-NAME.github.io/YOUR-REPOSITORY/privacy/` in a private browser window and verify the complete policy and contact email are visible.
4. Paste that exact public URL into **Play Console → Policy and programs → App content → Privacy policy**. The app itself also has a Privacy screen.

Do not guess the URL: the GitHub username, repository name, and Pages settings determine it.

## Create an upload key and signed bundle

Use [Android Studio's signed-bundle flow](https://developer.android.com/studio/publish/app-signing): **Build → Generate Signed Bundle / APK → Android App Bundle → Create new**. Save the `.jks` keystore outside this project, choose a long unique password, and keep its password and alias in your password manager. Back up the keystore securely. Never commit the keystore or passwords.

Choose the **release** build variant. Android Studio creates a signed `.aab` file. Verify it with `jarsigner -verify -verbose -certs PATH_TO_SIGNED_AAB` and confirm that it says the jar is verified and shows the upload certificate. The ordinary `./gradlew bundleRelease` output is currently unsigned and must not be uploaded.

In Play Console, let **Play App Signing** manage the app signing key; use your local key as the separate upload key. Keep the same upload key for future releases, unless you request a reset through Play Console. Do not upload a bundle until the listing, privacy policy, and test track are ready.

## Store assets

- Copy text from [`store-listing.md`](store-listing.md) into the Play listing and review it there.
- Use screenshots from the running Wear OS app, without watch frames, added words, or extra backgrounds. The Wear OS screenshots must be square and at least 384 × 384 pixels.
- Review the final app icon and other image requirements shown by Play Console before upload.
- Opt in to the **Wear OS** form factor in Play Console and add the Wear OS screenshots.

The first release still needs physical-watch testing and a closed-test release before public rollout.
