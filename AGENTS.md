# Android TV repository rules

- This repository contains only the Android/Google TV client, its tests, build automation and documentation. Keep the Node server, website source, `.env`, caches, signing keys and passwords out of it.
- The app's default update channel is Release. Experimental is explicitly opt-in.
- Never create or publish a GitHub Release, create a release tag, or upload/change release assets unless the human explicitly requests that release. Ordinary commits and pushes are not release authorization.
- Push builds may update only the generated Experimental feed on `updates`. They must not create prereleases as a workaround.
- `release` is the stable-source branch. Promote it for a release only when the human asks. The initial branch setup does not publish a release.
- Keep a single persistent signing identity across both channels. Never commit a keystore, password, access token or private key. Respect Android installation confirmation and do not bypass certificate or signing checks.
