# Cricket theme: what is in the pictures

Every card in `wallpapers/` is a photograph of the player it names, from [Wikimedia
Commons](https://commons.wikimedia.org/). They are built the way the "Your TV, Your Way" guide builds a
wallpaper: cropped to 16:9 and filling the screen, black and white because it looks calm, darkened with
a dark lower third so the dock's logos sit on black, and saved as PNG because JPEG bands in dark
gradients. The name and the shirt number are set over that. The drawn-silhouette cards this theme
started with are still reproducible — run `python3 make_wallpapers.py` in this folder without
`--photos`.

## The photographs

Each picture keeps its own licence. This repository's MIT licence covers the theme's code and its
`config.json`, not these images.

| Card | Player | Photographer | Licence | Source |
|---|---|---|---|---|
| `01-virat-kohli.png` | Virat Kohli | Dun.can from Leicestershire, UK | [CC BY 2.0](https://creativecommons.org/licenses/by/2.0/) | [Virat Kohli after his century at Trent Bridge](https://commons.wikimedia.org/wiki/File:Virat_Kohli_after_his_century_at_Trent_Bridge.jpg) |
| `02-ms-dhoni.png` | MS Dhoni | Agashe Foundation | [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/) | [Mandir Agashe felicitating MS Dhoni](https://commons.wikimedia.org/wiki/File:Mandar_Agashe_felicitating_MS_Dhoni.jpg) |
| `03-ab-de-villiers.png` | AB de Villiers | paddynapper | [CC BY-SA 2.0](https://creativecommons.org/licenses/by-sa/2.0/) | [AB de Villiers 2](https://commons.wikimedia.org/wiki/File:AB_de_Villiers_2.jpg) |
| `04-sachin-tendulkar.png` | Sachin Tendulkar | British High Commission, New Delhi | [CC BY-SA 2.0](https://creativecommons.org/licenses/by-sa/2.0/) | [Sachin Tendulkar at the Oval Maidan](https://commons.wikimedia.org/wiki/File:The_cricket_legend_Sachin_Tendulkar_at_the_Oval_Maidan_in_Mumbai_During_the_Duke_and_Duchess_of_Cambridge_Visit(26271019082).jpg) |
| `05-rohit-sharma.png` | Rohit Sharma | Bahnfrend | [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/) | [2015 CWC I v UAE, Sharma](https://commons.wikimedia.org/wiki/File:2015_CWC_I_v_UAE_02-28_Sharma_(06).JPG) |
| `06-chris-gayle.png` | Chris Gayle | Dun.can from Leicestershire, UK | [CC BY 2.0](https://creativecommons.org/licenses/by/2.0/) | [Two views of Chris Gayle](https://commons.wikimedia.org/wiki/File:Two_views_of_Chris_Gayle_(48020785077).jpg) |
| `07-yuvraj-singh.png` | Yuvraj Singh | lensbug.chandru | [CC BY 2.0](https://creativecommons.org/licenses/by/2.0/) | [Yuvraj against New Zealand](https://commons.wikimedia.org/wiki/File:Yuvraj_against_New_zealand.jpg) |
| `08-jasprit-bumrah.png` | Jasprit Bumrah | Dun.can from Leicestershire, UK | [CC BY 2.0](https://creativecommons.org/licenses/by/2.0/) | [England v India, Trent Bridge](https://commons.wikimedia.org/wiki/File:England_v_India,_Trent_Bridge_(44180102251).jpg) |
| `09-sourav-ganguly.png` | Sourav Ganguly | Press Information Department, Bangladesh | Public domain | [Sourav Ganguly meets Sheikh Hasina](https://commons.wikimedia.org/wiki/File:Sourav_Ganguly_Meets_Sheikh_Hasina_at_Ganabhaban,_2023-02-24_(PID-0017748).jpg) |
| `10-ricky-ponting.png` | Ricky Ponting | Tourism Victoria from Australia | [CC BY 2.0](https://creativecommons.org/licenses/by/2.0/) | [Cricketer Ricky Ponting and Rianna Ponting](https://commons.wikimedia.org/wiki/File:Cricketer_Ricky_Ponting_and_Rianna_Ponting_(24079181379).jpg) |

The four cards marked **CC BY-SA** (Dhoni, de Villiers, Tendulkar, Sharma) are adaptations of those
photographs, so each one carries the same share-alike licence: it may be redistributed, but only under
CC BY-SA, with the photographer credited. The six CC BY and public-domain cards need the credit above
and nothing more. If you swap a picture, keep the credit with it — `themes/README.md` says the same
thing about every theme's pictures.

The names, shirt numbers and descriptions are facts about real players, used here as a tribute. They
do not imply that any player, team, board or league has endorsed, licensed or is connected with this
theme. A photograph of a player is almost always licensed by someone, and "not for profit" is not
automatically a permission: anything you put on a card is your responsibility.

`make_wallpapers.py --photos <folder>` builds the same cards on your own pictures; it changes nothing
about the licence question.

## Fonts

The pictures are set in two fonts, both under the SIL Open Font Licence 1.1, and neither is committed
here: `make_wallpapers.py` downloads them from Google Fonts on its first run.

| Font | Used for | Licence |
|---|---|---|
| [Bebas Neue](https://fonts.google.com/specimen/Bebas+Neue) | the names and the shirt numbers | SIL OFL 1.1 |
| [Outfit](https://fonts.google.com/specimen/Outfit) | the small line under a name | SIL OFL 1.1 |

The launcher's own interface type is Outfit, so the small line matches it.
