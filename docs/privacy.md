# Privacy and publication

This is a fresh snapshot without the working machine's local Git history. Original source, tools, themes, examples, public docs, and copyright notices are retained.

## Excluded

- TV backups, settings/package/UI dumps, logs, traces, and debugging sessions.
- ADB keys, signing keys, credentials, environment files, local SDK paths, caches, and APKs.
- Account/profile data, saved Wi-Fi data, TV serials, MAC addresses, Android IDs, and private TV network addresses.
- Runtime Watch Later/custom additions, local Git metadata, and upstream maintainer automation.

Personal weather coordinates are removed; published fields are blank. The Match Day subtitle uses a timezone rather than a city. The portable TV test accepts a device serial through an environment variable.

## Gallery

Only reviewed launcher screenshots are included. Account/network settings, streaming profiles, raw diagnostics, and the removed photo screensaver are excluded. Gallery JPEGs are re-encoded without EXIF or PNG text metadata. Video uses only those reviewed images, with no microphone or desktop recording. Source artwork/audio are bundled project assets with their own rights; see [media notice](../MEDIA-NOTICE.md).

## Runtime

Preferences and lists stay in private app storage; Android backup is disabled in the manifest. Streaming apps handle their own credentials. Configuring optional weather sends coordinates to Open-Meteo. Match Day requests Willow's schedule. Opening provider links causes normal requests in those apps. This differs from the original offline launcher.

## Future uploads

Run `python3 scripts/check_public_export.py`. It checks common credential formats, private keys, authenticated URLs, local home paths, private IPv4 addresses, hardware/SSID patterns, weather coordinates, forbidden files, and gallery metadata. Reports show paths/rules, not matched secrets. Review screenshots visually too: a text scan cannot recognize every personal detail in images, videos, or audio. If a credential is published, revoke/rotate it and remove it from history, not just the latest version.

The review covers this export, not unrelated files or external accounts. It is not a guarantee that arbitrary future content is safe.

Original upstream dummy test fixtures and protocol/documentation examples are recorded in `scripts/privacy-baseline.json`. Each exception is tied to the entire original file's SHA-256 and specific rule names. Changing such a file invalidates its exception. Credential-token rules are not excluded by this baseline. Metadata was also removed losslessly from the customized PNG/JPEG assets, preserving the compressed image pixels and colour profiles.
