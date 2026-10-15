# Play Console declarations

These answers are based on the current source and dependencies. Recheck them if the app later adds networking, accounts, analytics, ads, payments, health sensors, or another SDK.

## App content

| Console task | Prepared answer |
| --- | --- |
| Privacy policy | `https://thephani.github.io/tennis-scoring-wear-os/privacy/` after the updated page is publicly verified |
| Sign-in details / app access | All functionality is available without special access. No account, login, membership, location restriction, or authentication is required. |
| Ads | No, the app does not contain ads. |
| Content rating | Complete as an app, not a game. Based on current functionality, answer No to violence, sexual content, language, controlled substances, gambling, user interaction, user-generated content, and location sharing. Submit the questionnaire to receive the regional ratings. |
| Target audience | Recommended: ages 13–15, 16–17, and 18+. The app is a general tennis utility and is not specifically directed at children. Do not enroll it in Designed for Families. |
| Data safety | No user data is collected or shared. Match data and settings are processed and stored only on the watch and are not transmitted off-device. There is no account. Users can delete local data by clearing app storage or uninstalling the app. |
| Government apps | No. The app is not developed by or on behalf of a government entity. |
| Financial features | None. The app does not provide banking, payments, lending, investment, cryptocurrency, insurance, or other financial functionality. |
| Health apps | Select **My app doesn't provide any health features**. It scores tennis matches but does not monitor activity, workouts, body signals, medical information, or Health Connect data. |

## Store organization

| Field | Prepared value |
| --- | --- |
| App or game | App |
| Category | Sports |
| Tags | Tennis and scorekeeping, where offered and accurate |
| Support email | `thephani.old@gmail.com` |
| Website | Optional. Use the public project or support page only after verifying it is suitable for customers. |
| App name | Tennis Score for Wear OS |
| Default language | English (United States) – en-US |

The short and full descriptions are in [`store-listing.md`](store-listing.md). Upload the assets from `store-assets/`, and add the screenshots specifically to the Wear OS screenshot section.

## Technical declarations confirmed from source

- Package name: `com.thephani.tennisscore`
- Version: `1.0` (`versionCode` 1)
- Minimum SDK: 28
- Target SDK: 35
- Required device feature: `android.hardware.type.watch`
- Standalone metadata: `com.google.android.wearable.standalone=true`
- 64-bit Wear OS support present: `arm64-v8a` and `x86_64` native libraries are included
- No internet, dangerous, or sensitive Android permissions in the merged release manifest; only the app-scoped signature permission generated for non-exported dynamic receivers is present
- No account, advertising, analytics, backend, phone companion, payments, or health-data integration
- Local match/settings storage is excluded from cloud backup and device transfer

## Current Console gates observed September 30, 2026

- 0 of 11 app-setup tasks are complete.
- Internal testing is 1 of 3 tasks complete; a release has been created, but testers and preview/confirmation remain.
- Closed testing cannot start until app setup is complete.
- Production access requires at least 12 testers continuously opted in for at least 14 days.
- The Console currently shows 0 opted-in testers.

Do not send the app for review, begin a rollout, or apply for production access until the signed bundle, listing, declarations, testing, and privacy URL are verified.
