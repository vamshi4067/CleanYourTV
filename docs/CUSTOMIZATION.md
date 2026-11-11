# Customization

Java files below are in `launcher/app/src/main/java/com/gathin/tvhome/`; resources are under `launcher/app/src/main/`.

| Change | Location |
| --- | --- |
| Dock order, package names, captions | `CricketHome.java`, `APPS` |
| Service marks and colours | `CricketBadge.java`, `res/drawable/cricket_mark_*.xml` |
| Shortcut artwork | `res/drawable-nodpi/cricket_action_*.png` |
| Favourite titles, row positions, links | `CricketLineup.java` |
| Main picker catalogue | `CricketCatalog.java` |
| Additional browser catalogue | `CricketDiscover.java` |
| Categories and stable store keys | `CricketShowcase.java`, `MOODS`, `PICKS` |
| Browser controls | `CricketPickBrowser.java` |
| Fixtures and followed teams | `CricketFixtures.java`, `CricketShowcase.java` |
| Image loading and backgrounds | `CricketArt.java` |
| General settings | `res/values/config.xml`, theme `config.json` |

## Artwork and cues

`assets/cricket/` contains photos, `lineup/` cutouts/scenes, and `posters/` provider-ID/channel-handle artwork. Preserve transparency and intended aspect ratios. Upsizing a small image cannot restore original photographic detail. Focus audio is in `res/raw/cricket_*_cue.*`; keep resource names synchronized with `CricketLineup.java`.

Use media you can redistribute. The code licence does not relicense photographs, show stills, characters, logos, or music. See [media notice](../MEDIA-NOTICE.md).

Add movies/channels with the correct installed provider package, HTTPS title link, and corresponding artwork. Category changes must preserve stable store keys and migration logic. The one-time migration merges old cricket categories into `cricket_custom_picks_cricket`; Watch Later stays in `cricket_watch_later`. Do not map storage solely by display order.

## Optional weather

`res/values/weather.xml` has blank `weather_latitude` / `weather_longitude` strings. Blank or invalid coordinates disable weather. Set decimal degrees locally and rebuild. Weather requests Open-Meteo in Celsius about every 30 minutes while Home is active; no API key is needed. Do not commit your location. The export checker rejects configured weather coordinates. Upstream `latitude` / `longitude` theme options are separate sunrise/sunset settings and should also remain private.

Source/resource edits require a rebuild. Theme JSON supports upstream configuration keys; it cannot rewrite hard-coded characters or catalogue membership. Class names remain `Cricket*` and the on-TV name remains Cricket Legends.
