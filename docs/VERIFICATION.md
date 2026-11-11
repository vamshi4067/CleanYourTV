# Export verification

This source snapshot was prepared for a private GitHub repository in November 2026.

## Checked during export

- Source export privacy checker: common credentials, private identifiers/files, local paths, weather coordinates, and gallery metadata.
- Independent Gitleaks 8.30.1 scan, using the official binary verified against its release checksum. Three false positives in upstream comments about remote buttons were resolved by clarifying their wording.
- A separate byte scan for known personal location/address/account strings, with no matches in exported files.
- The customized README and guide links resolve to included files.
- Gallery screenshots were visually reviewed. JPEGs were re-encoded without source metadata. The silent MP4/GIF uses only these images and is labelled a slideshow.
- Private Git metadata, ADB keys, signing keys, build outputs, backup data, and runtime saved lists are not exported. Commit identity uses a GitHub no-reply address.

Original upstream dummy fixtures are allowed only through content-hash-bound entries in `scripts/privacy-baseline.json`; they are not credentials or private device defaults.

## Android build status

The customized launcher before this export had been built, installed, and exercised on TCL Google TV / Android 12. The navigation smoke test covered dock/top buttons, Shortcuts, Favourites, category wraparound, Add more, and Watch Later.

A fresh Android build of this sanitized copy could not be completed in the export session because the local Java runtime would not start. No fresh unit-test/lint/build pass is claimed for this copy. The share-only weather change disables requests until local coordinates are configured; it does not alter the installed TV build. Run the documented Gradle checks using a working JDK 17 and Android SDK before installing your own build.

Netflix profile handoff and automatic selection capture inside other apps remain limitations, not verified features.
