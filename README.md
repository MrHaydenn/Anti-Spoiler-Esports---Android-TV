# Anti-Spoiler Esports — Android / Google TV

Google TV Streamer / Android TV client for [Anti-Spoiler Esports](https://antispoileresports.mrhaydenn.us). This repository contains **only the TV client**, its tests, build tools and documentation. The Node server, PandaScore/YouTube credentials, cache and website source are separate and are intentionally not included.

[Download the latest signed Experimental APK](https://raw.githubusercontent.com/MrHaydenn/Anti-Spoiler-Esports---Android-TV/updates/experimental/anti-spoiler-esports-tv.apk). The installed app still defaults to Release updates.

## Use the app

The app opens the server's remote-friendly `/tv` interface with the existing spoiler-safe brackets and player. Use the D-pad and Select; Back closes dialogs/player, then returns to tournaments. Use the **Settings** gear at the top right, then **App updates** to change channels or check immediately. Hold Back (or press Menu) also opens native settings. App updates are also available on the native connection-error screen.

The default server is `https://antispoileresports.mrhaydenn.us`. Its SSL certificate must be active. The server must support `/tv`, `/api/health`, and the site's assets/API routes. Debug builds can use a manually entered LAN HTTP address; official update builds use HTTPS. A functioning Android System WebView is required. Tournament cards are single remote-focus targets; favorites are toggled on Overview and appear first on the home screen. Playback fills the TV, with a fading overlay containing Back, rewind, play/pause and forward. Hold OK on the 10-second seek buttons to accelerate; press a direction or OK to bring the toolbar back. Real Google TV Streamer playback/installer behavior still needs hardware testing.

Favorites, reveal state and progress are saved on this TV per server origin. They are not synchronized with desktop browsers yet. All devices use the existing shared backend API cache. No API credentials are embedded in the APK.

## Update channels

- **Release (default):** checks only the latest published, non-prerelease GitHub Release. Its `update.json` must identify a build from the `release` branch. With no published release, the app reports that no release exists; it does not fall back to Experimental.
- **Experimental (opt-in):** checks the generated `updates/experimental/update.json` feed. Each successful, signed push build on a source branch can replace this feed. Feed publishing creates **no GitHub Releases or prereleases**.
- Automatic checks run while the app is open/on resume: Release checks at most every six hours; Experimental at most every fifteen minutes. **Check now** is available any time. Updates require the Android installation confirmation; unattended installation is not attempted.
- Downloads require HTTPS, the expected repository, matching package/version, SHA-256 and the same APK signing certificate as the installed app. Versions never downgrade. After switching from a newer Experimental build to Release, a newer Release must be published before it updates.

Source branches: `main` is normal development; `release` holds approved stable source; `updates` is an automatically generated binary/manifest feed. Do not manually edit the generated feed.

## GitHub build automation

Every source push builds/tests the app. Pull requests run checks without signing/publishing. GitHub Actions uses JDK 17, Android SDK 36, Build Tools 35.0.0, AGP 8.13.2 and the checksum-pinned Gradle 8.13 wrapper. The build tests include update URL/channel/version rules.

Signed output requires these repository Actions secrets:

- `ANDROID_SIGNING_KEY_B64`: base64-encoded persistent PKCS12 signing keystore.
- `ANDROID_SIGNING_PASSWORD`: keystore and key password.
- `ANDROID_SIGNING_ALIAS`: key alias.

Keep the signing key/password private and backed up. Both channels must use the same key. No key or password belongs in this repository. If signing is not configured, source checks and a development debug artifact run, but the workflow **does not publish an update feed**. Development-debug artifacts use a different development signature and are not an installation/update starting point for the official app.

For initial installation, use the signed `android-tv-experimental-*` artifact or the Experimental feed APK once signing is configured. The app still defaults to Release until you opt into Experimental in settings.

## Releases are manual — only when explicitly requested

No workflow creates a tag, draft, prerelease or GitHub Release. A push never constitutes release authorization.

When the human explicitly asks for a release:

1. Promote the approved source to `release`.
2. Manually run **Android TV build** on that branch with channel `release` and a chosen version such as `0.1.0`. This creates a signed candidate artifact only.
3. After review and the explicit release instruction, publish a GitHub Release tagged `vVERSION` from that approved source and attach **both** `anti-spoiler-esports-tv.apk` and its matching `update.json` from the same candidate. Do not mark it as a prerelease for the Release channel.

The candidate manifest targets that tag's APK URL. Do not mix APKs/manifests from different builds. No GitHub Release has been created as part of the initial repository setup.

## Local build / sideload

Install Android Studio, open this folder, and install SDK Platform 36 / Build Tools 35.0.0 through SDK Manager, reviewing its license. Java 17+ is required. `build.bat` creates a development debug APK, or run:

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug
```

Official signed builds additionally need `TV_KEYSTORE_PATH`, `TV_KEYSTORE_PASSWORD` and `TV_KEY_ALIAS` environment variables, then `assembleRelease`. Never commit local signing configuration. SDK paths go in gitignored `local.properties`.

After connecting your TV to authorized ADB, install the signed APK:

```powershell
adb install -r anti-spoiler-esports-tv.apk
```

Android may require enabling installation from this app for in-app updates. The app opens Android's own permission/installer UI and never enables that permission itself. When finished with development, disable TV debugging if it is no longer needed.

## Validation status

Server UI navigation and routes have been tested separately. GitHub CI builds/lints/tests the native client. Actual TV installation, remote input, update permission/installer, video playback and background/resume need a Google TV Streamer test. YouTube's own overlays/broadcast may contain spoilers. Twitch embeds do not currently expose every playback action to the custom remote controls.
