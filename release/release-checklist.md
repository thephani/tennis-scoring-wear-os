# First Play release checklist

## Prepared locally

- [x] Permanent package name verified: `com.thephani.tennisscore`
- [x] Release version code advanced to `3` after Play Console reserved version codes `1` and `2` during rejected track attempts
- [x] Wear OS-only targeting and standalone metadata verified
- [x] Target SDK 35 verified for the September 2026 Wear OS requirement
- [x] Listing copy updated for Singles and Doubles
- [x] Privacy policy source updated for Singles and Doubles
- [x] Console declaration answers prepared from current source
- [x] Four authentic square Wear OS screenshots available, including current Home and Singles/Doubles Settings screens
- [x] Store icon and feature graphic prepared

## Signing and bundle

- [x] Fresh unsigned release AAB built at `app/build/outputs/bundle/release/app-release.aab` on September 30, 2026
- [x] Unsigned AAB SHA-256 recorded: `62a24cc7a39eaae551fba6ec29556292e2cff077ac9bc205f19ec651e5e04da7`
- [x] `jarsigner` confirmed the current AAB is **unsigned** and therefore not uploadable
- [x] First signing attempt verified at `app/release/app-release.aab`; SHA-256: `a71e46eeb99aca84281cc43c1f9bf6f04e175b0986efcc1664e29e31c4d2ae30`
- [x] `jarsigner` returned `jar verified` for that attempt, but its certificate expires September 29, 2028. **Do not upload it:** Google Play requires the signing certificate to expire after October 22, 2033 and recommends at least 25 years.
- [x] Create the private upload keystore outside the repository at `/Users/thephani/tennis.jks` with 25-year validity.
- [x] Save the keystore, alias, and passwords in a password manager and secure backup. Never put them in source control or chat.
- [x] Generate a signed **Android App Bundle** for the release variant at `app/release/app-release.aab`.
- [x] Verify the bundle with `jarsigner`: `jar verified`.
- [x] Confirm the upload certificate expires September 24, 2051, after October 22, 2033.
- [x] Record signed AAB SHA-256: `c212a33221bdf94384504fe452d9ae625f954243d47314988d820479f3790545`
- [x] Record upload certificate SHA-256: `E6:1F:0D:F3:D1:A7:7E:B2:93:FB:32:7A:EE:91:7B:0B:92:D3:D1:C0:03:9F:AC:44:FD:9F:E7:80:93:C9:73:B0`
- [ ] Enroll in Play App Signing when Play Console requests it; the local keystore remains the upload key.

## Testing

- [x] Fresh `build bundleRelease --offline` verification passed on September 30, 2026.
- [x] Full large-round instrumentation suite passed: 47 tests, 0 skipped, 0 failed on `Wear_OS_Large_Round(AVD) - 17`.
- [ ] Install a 192 dp small-round AVD and run the full instrumentation suite there. Only the large-round AVD is currently installed.
- [ ] Install the release candidate on a physical Galaxy Watch4-or-newer device.
- [ ] Manually verify launch, Singles, Doubles, serving team, best-of-3/5, full deciding set, match tiebreak, undo, resume, history, settings, privacy, swipe-to-dismiss/back, larger font size, and 48 dp touch targets.
- [ ] Upload only to an internal or closed testing track, then review Play's pre-launch report for stability, accessibility, security, and compatibility findings.

## Console setup

- [ ] Publish the updated privacy page and verify the public URL. The URL is reachable, but its live content was still the older Singles-only policy on September 30, 2026.
- [ ] Complete all answers in [`play-console-declarations.md`](play-console-declarations.md).
- [ ] Select Sports as the category and confirm support contact details.
- [ ] Enter the listing copy and upload all prepared store assets.
- [ ] Add the Wear OS form factor under **Test and release → Advanced settings → Form factors** after a closed-test release exists, and accept the Wear OS review policy only when ready.

## Closed-test gate

- [ ] Build a list of at least 12 reliable testers with compatible Wear OS watches.
- [ ] Create the closed-testing tester list and opt-in link.
- [ ] Start the closed test only after the release candidate and listing are ready.
- [ ] Keep at least 12 testers continuously opted in for 14 days.
- [ ] Collect and retain concise feedback: watch models, Wear OS versions, scenarios tested, issues found, and fixes made.
- [ ] After the 14-day gate, answer the production-access questions with actual testing evidence.

## Stop point

Preparing drafts, builds, and assets is reversible. Do not click **Start rollout**, **Send for review**, or **Apply for production** without a separate final approval.
