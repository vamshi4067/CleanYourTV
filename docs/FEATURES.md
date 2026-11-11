# Features and remote controls

## Home

Rotating full-screen cricket artwork, clock, optional weather, branded app dock, and high-contrast focus outlines. Dock order: Netflix, YouTube, Prime Video, Willow, Disney+, Apple TV, HTE, Hulu, aha. Missing apps are skipped. The Shortcuts button reveals More apps, My Favourites, Pick something, Match Day, and Watch Later. Back collapses the row.

## My Favourites

Back row: AB de Villiers, Iron Man, Spider-Man, Netflix, Dhoni. Front row: Virat, Friends, The Big Bang Theory, MasterChef Canada.

Idle artwork is monochrome. Focus adds colour, slight scale, a themed light, and a prepared dark scene. Front-row selection comes forward; back-row items stay behind the front row. A lower fade blends into the page. Dhoni is independent; the Virat shudder was removed.

Focus audio is bundled for Virat, AB de Villiers, Dhoni, Friends, The Big Bang Theory, and Netflix, and stops when focus leaves. Most long cues are around 12 seconds; Dhoni and Netflix use their shorter supplied files. Left/Right moves along a row, Up/Down changes row, OK opens, and hold OK saves an eligible title.

## Pick something

Categories: **Cricket**, **Telugu movies**, **Hindi movies**, **YouTube channels**, **Anything at all**. Cricket combines match picks, highlights, cricket films, and behind-the-game content. The standalone Home Match Day fixture shortcut remains.

Left/Right changes category. OK picks a title; OK on a result opens it. Results include provider artwork. The picker avoids an immediate repeat when alternatives exist. Hold OK saves the result to Watch Later.

## Add more

Up from the picker reaches Add more. Browse the bundled artwork catalogue, filter All / Netflix / Prime Video / YouTube / My additions, choose a destination Category, and press OK to add. The current catalogue has approximately 44 choices. An added card becomes Added · Remove; built-in category picks show Already included. Custom additions join their category and Anything at all, deduplicated by app plus link. Done or Back returns to the picker. This is a curated catalogue, not a search of every streaming title.

## Watch Later

Hold OK in Favourites or on a picker result to save. OK on a saved poster opens it; hold OK removes it. Lists persist locally and prevent duplicates. An app-only tile cannot save a meaningful title. This does not read browsing inside other apps or synchronize provider watchlists.

## Match Day and weather

Match Day fetches Willow's public upcoming schedule, applies an in-memory cache, and prefers India, India Women, and RCB fixtures. Times use US Eastern Time. Open Willow launches the app, not a guaranteed specific stream. Weather uses Open-Meteo only after you supply coordinates locally. The custom photo screensaver is removed; upstream clock components remain optional.

## Navigation

| Screen | Left / Right | Up / Down | OK | Hold OK | Back |
| --- | --- | --- | --- | --- | --- |
| Dock | Adjacent app; bounded edges | Top controls / Shortcuts | Open app | Upstream controls | Return toward dock |
| Shortcuts | Adjacent shortcut | Return to dock | Open panel | — | Collapse row |
| Favourites | Along row | Switch row | Open | Save eligible title | Close |
| Picker | Category | Add more / result | Pick / open | Save | Close |
| Add more | Cards / controls | Controls / list | Add, remove, filter | — | Return to picker |
| Watch Later | Saved titles | Within panel | Open | Remove | Close |

Panels contain arrow navigation. A closing overlay gives up focus before its fade completes, avoiding focus jumps during rapid presses.
