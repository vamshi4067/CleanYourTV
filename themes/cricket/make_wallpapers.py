#!/usr/bin/env python3
"""Builds themes/cricket/wallpapers/: ten cricket "legend cards", 1920x1080.

The committed cards are photographs, laid out the way the "Your TV, Your Way" guide lays out a
wallpaper: key art that fills the screen, black and white because it looks calm, a dark lower third so
the dock's row of logos sits on black, and PNG because JPEG bands in dark gradients. CREDITS.md names
the photographer and the licence behind each one.

Without --photos the same cards are drawn from nothing but code instead: a night match, a floodlit sky
in the player's colour, a stand of crowd bokeh, the pitch with a batsman at the crease, his shirt
number ghosted behind him. That version uses no photograph or likeness and can be redrawn by anyone.
In both, only the names, numbers and descriptions are facts about real players.

    python3 make_wallpapers.py                  # needs Pillow and NumPy
    python3 make_wallpapers.py --fonts <dir>    # where the two OFL fonts are, or may be downloaded to
    python3 make_wallpapers.py --only 18        # just that one card, while tweaking it
    python3 make_wallpapers.py --photos <dir>   # build the cards on photographs instead (see below)

--photos pairs the pictures in <dir>, sorted by name, with the legends in order and lays each one out
as a card: the photograph cropped to 16:9 and filling the card, in monochrome, with the number ghosted
behind the name block in the dark left column. PHOTO_CROP holds where each picture is cut, so a
portrait does not lose a forehead. Pictures you did not take yourself stay out of this repository
unless their licence allows it: `CREDITS.md` has to say where each picture came from and under which
licence (themes/README.md). Photos under about 1280px across look soft on a TV.

The type is Bebas Neue (numbers, names) and Outfit (the small line under a name), both SIL Open Font
Licence, fetched from Google Fonts on the first run into --fonts (a temp folder by default); nothing
binary is committed for them.

Kept dark on purpose (themes/README.md, Wallpapers): the clock, the hero name and the dock band are
designed for black, so the top band, the left side and the bottom third are shaded down at the end.
"""
from __future__ import annotations

import argparse
import math
import os
import tempfile
import urllib.request
import zlib
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageEnhance, ImageFilter, ImageFont, ImageOps

W, H = 1920, 1080
S = 1.5                                  # draw at 1.5x and downsample, so the edges stay clean
w, h = int(W * S), int(H * S)
HORIZON = 0.595                          # the ground line, as a fraction of the height

BEBAS = "BebasNeue-Regular.ttf"
OUTFIT = "Outfit.ttf"
FONT_URLS = {
    BEBAS: "https://raw.githubusercontent.com/google/fonts/main/ofl/bebasneue/BebasNeue-Regular.ttf",
    OUTFIT: "https://raw.githubusercontent.com/google/fonts/main/ofl/outfit/Outfit%5Bwght%5D.ttf",
}

# name, number, the line under it, accent colour, pose, seed
LEGENDS = [
    ("VIRAT KOHLI", "18", "INDIA \u00b7 RIGHT HAND \u00b7 CHASE MASTER", "#3D6BFF", "stance", 1801),
    ("MS DHONI", "7", "INDIA \u00b7 WICKETKEEPER \u00b7 FINISHER", "#F2C230", "pull", 704),
    ("AB DE VILLIERS", "17", "SOUTH AFRICA \u00b7 360 DEGREES", "#35B27A", "celebrate", 1702),
    ("SACHIN TENDULKAR", "10", "INDIA \u00b7 THE LITTLE MASTER", "#E8A33D", "stance", 1003),
    ("ROHIT SHARMA", "45", "INDIA \u00b7 OPENER \u00b7 SIX HITTER", "#4EA8FF", "pull", 4504),
    ("CHRIS GAYLE", "333", "WEST INDIES \u00b7 UNIVERSE BOSS", "#B23A6B", "celebrate", 3335),
    ("YUVRAJ SINGH", "12", "INDIA \u00b7 SIX SIXES \u00b7 2011", "#A06BFF", "pull", 1206),
    ("JASPRIT BUMRAH", "93", "INDIA \u00b7 YORKERS \u00b7 DEATH OVERS", "#35C2C2", "celebrate", 9307),
    ("SOURAV GANGULY", "99", "INDIA \u00b7 LEFT HAND \u00b7 CAPTAIN", "#E2703A", "stance", 9908),
    ("RICKY PONTING", "14", "AUSTRALIA \u00b7 PULL SHOT \u00b7 CAPTAIN", "#D9C04A", "pull", 1409),
]

# Where a photograph is cut, by shirt number: 0 keeps the top of the picture, 1 the bottom. The
# default suits a wide action shot; a portrait needs the top, or the 16:9 crop takes off a forehead.
# Only used by --photos, and only a starting point: swap in another picture and it may need tuning.
PHOTO_CROP = {"7": 0.05, "17": 0.18, "10": 0.36, "333": 0.08, "12": 0.30, "99": 0.00, "14": 0.10}


# ---- small helpers -----------------------------------------------------------------------------

def rgb(value: str) -> tuple[int, int, int]:
    value = value.lstrip("#")
    return int(value[0:2], 16), int(value[2:4], 16), int(value[4:6], 16)


def lerp(a: float, b: float, t: float) -> float:
    return a + (b - a) * t


def scale_colour(c, k):
    return tuple(int(max(0, min(255, v * k))) for v in c)


def fonts(folder: Path) -> tuple[Path, Path]:
    """The two font files, downloaded into `folder` if they are not there yet."""
    folder.mkdir(parents=True, exist_ok=True)
    for name, url in FONT_URLS.items():
        path = folder / name
        if path.is_file() and path.stat().st_size > 20000:
            continue
        print(f"fetching {name} ...")
        with urllib.request.urlopen(url, timeout=60) as r:
            path.write_bytes(r.read())
    return folder / BEBAS, folder / OUTFIT


def cover(im: Image.Image, tw: int, th: int, focus_x: float = 0.5, focus_y: float = 0.42) -> Image.Image:
    """`im` scaled to fill `tw` x `th` and cropped to it, the crop taken around `focus`."""
    src_ar = im.width / im.height
    if src_ar > tw / th:
        nh, nw = th, round(th * src_ar)
    else:
        nw, nh = tw, round(tw / src_ar)
    im = im.resize((nw, nh), Image.LANCZOS)
    x, y = round((nw - tw) * focus_x), round((nh - th) * focus_y)
    return im.crop((x, y, x + tw, y + th))


def limb(d: ImageDraw.ImageDraw, p0, p1, w0, w1, col):
    """A tapered limb between two points, with round ends so the joints read as joints."""
    (x0, y0), (x1, y1) = p0, p1
    dx, dy = x1 - x0, y1 - y0
    L = math.hypot(dx, dy) or 1.0
    nx, ny = -dy / L, dx / L
    d.polygon([(x0 + nx * w0 / 2, y0 + ny * w0 / 2), (x1 + nx * w1 / 2, y1 + ny * w1 / 2),
               (x1 - nx * w1 / 2, y1 - ny * w1 / 2), (x0 - nx * w0 / 2, y0 - ny * w0 / 2)], fill=col)
    for (x, y), r in ((p0, w0 / 2), (p1, w1 / 2)):
        d.ellipse([x - r, y - r, x + r, y + r], fill=col)


def bar(d: ImageDraw.ImageDraw, centre, angle, length, width, col):
    """A rounded bar: a bat's blade, or a stump."""
    cx, cy = centre
    ca, sa = math.cos(angle), math.sin(angle)
    hx, hy = ca * length / 2, sa * length / 2
    nx, ny = -sa * width / 2, ca * width / 2
    d.polygon([(cx - hx + nx, cy - hy + ny), (cx + hx + nx, cy + hy + ny),
               (cx + hx - nx, cy + hy - ny), (cx - hx - nx, cy - hy - ny)], fill=col)
    for s in (-1, 1):
        ex, ey = cx + hx * s, cy + hy * s
        r = width / 2
        d.ellipse([ex - r, ey - r, ex + r, ey + r], fill=col)


POSES = {
    #            hands                     bat angle, length   elbows
    "stance": ((0.215, -0.505), math.radians(87), 0.34, ((0.150, -0.655), (0.055, -0.600))),
    "pull":   ((0.055, -0.470), math.radians(-24), 0.34, ((0.150, -0.630), (-0.010, -0.620))),
    "celebrate": ((0.100, -1.000), math.radians(-90), 0.36, ((0.165, -0.945), (-0.020, -0.950))),
}


def batter(d: ImageDraw.ImageDraw, cx: float, foot: float, Hh: float, pose: str, col):
    """A batsman at the crease, side on. `Hh` is head to toe; `foot` is the ground line."""
    top = foot - Hh
    hands, bat_angle, bat_len, elbows = POSES[pose]
    hands = (cx + hands[0] * Hh, foot + hands[1] * Hh)          # y is measured up from the feet
    el_r, el_l = (cx + elbows[0][0] * Hh, foot + elbows[0][1] * Hh), (cx + elbows[1][0] * Hh, foot + elbows[1][1] * Hh)
    sh_r, sh_l = (cx + 0.095 * Hh, foot - 0.840 * Hh), (cx - 0.095 * Hh, foot - 0.840 * Hh)

    # legs: thigh, shin and the pad over both, with a knee in the middle
    for sgn, hip in ((-1, -0.050), (1, 0.050)):
        knee = (cx + (hip + sgn * 0.020) * Hh, foot - 0.255 * Hh)
        ankle = (cx + (hip + sgn * 0.030) * Hh, foot)
        limb(d, (cx + hip * Hh, foot - 0.480 * Hh), knee, 0.080 * Hh, 0.072 * Hh, col)
        limb(d, knee, ankle, 0.072 * Hh, 0.086 * Hh, col)
    # torso, with shoulders wide enough to read as shoulders
    d.polygon([(cx - 0.098 * Hh, foot - 0.855 * Hh), (cx + 0.098 * Hh, foot - 0.855 * Hh),
               (cx + 0.082 * Hh, foot - 0.470 * Hh), (cx - 0.082 * Hh, foot - 0.470 * Hh)], fill=col)
    limb(d, sh_l, sh_r, 0.070 * Hh, 0.070 * Hh, col)
    # arms, elbow included, then the gloves closed on the handle
    limb(d, sh_r, el_r, 0.060 * Hh, 0.050 * Hh, col)
    limb(d, el_r, hands, 0.050 * Hh, 0.044 * Hh, col)
    limb(d, sh_l, el_l, 0.056 * Hh, 0.048 * Hh, col)
    limb(d, el_l, hands, 0.048 * Hh, 0.044 * Hh, col)
    # the bat, tapered: a slim handle and a wide blade
    hx, hy = hands
    bar(d, (hx + math.cos(bat_angle) * bat_len * 0.5, hy + math.sin(bat_angle) * bat_len * 0.5),
        bat_angle, bat_len, 0.048 * Hh, col)
    gx, gy = hx + math.cos(bat_angle) * -0.02 * Hh, hy + math.sin(bat_angle) * -0.02 * Hh
    bar(d, (gx + math.cos(bat_angle) * 0.05 * Hh, gy + math.sin(bat_angle) * 0.05 * Hh),
        bat_angle, 0.10 * Hh, 0.022 * Hh, col)
    for s in (-1, 1):                                       # gloved hands
        r = 0.028 * Hh
        d.ellipse([hx + s * 0.017 * Hh - r, hy - r, hx + s * 0.017 * Hh + r, hy + r], fill=col)
    # neck, helmet, peak and grille
    limb(d, (cx + 0.010 * Hh, foot - 0.905 * Hh), (cx + 0.014 * Hh, foot - 0.845 * Hh),
         0.038 * Hh, 0.046 * Hh, col)
    hr = 0.049 * Hh
    hcx, hcy = cx + 0.014 * Hh, foot - 0.940 * Hh
    d.ellipse([hcx - hr * 0.95, hcy - hr, hcx + hr * 0.95, hcy + hr], fill=col)
    d.polygon([(hcx + hr * 0.60, hcy - hr * 0.20), (hcx + hr * 1.85, hcy - hr * 0.02),
               (hcx + hr * 1.65, hcy + hr * 0.34), (hcx + hr * 0.55, hcy + hr * 0.30)], fill=col)


def stumps(d: ImageDraw.ImageDraw, cx: float, foot: float, Hh: float, col):
    """Three stumps and the bails, just off the batsman's shoulder."""
    for i in (-1, 0, 1):
        x = cx + i * 0.030 * Hh
        d.rectangle([x - 0.007 * Hh, foot - 0.150 * Hh, x + 0.007 * Hh, foot], fill=col)
    d.rectangle([cx - 0.045 * Hh, foot - 0.158 * Hh, cx + 0.045 * Hh, foot - 0.142 * Hh], fill=col)


def streaks(dc: ImageDraw.ImageDraw, rng, from_xy, colour, n=9, reach=0.21, spread=0.028):
    """The arc a swung bat leaves, as short bright streaks."""
    x0, y0 = from_xy
    for _ in range(n):
        y = y0 + rng.uniform(-spread, spread) * h
        xa = x0 + rng.uniform(0.00, 0.10) * w
        xb = xa + rng.uniform(0.07, reach) * w
        dc.line([(xa, y), (xb, y + rng.uniform(-4, 4) * S)], fill=colour,
                width=max(1, int(rng.uniform(1.0, 2.2) * S)))


# ---- the parts every card shares -----------------------------------------------------------------

def ghost_number(scene, number, acc, bebas, alpha=0.32, at_x=0.795):
    """The shirt number, huge, ghosted behind the figure."""
    nums = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    nd = ImageDraw.Draw(nums)
    size = int(h * (0.56 if len(number) < 3 else 0.40))
    fnum = ImageFont.truetype(str(bebas), size)
    track = int(size * 0.02)
    total = sum(fnum.getlength(ch) + track for ch in number) - track
    x = w * at_x - total / 2
    for ch in number:
        nd.text((x, h * 0.315), ch, font=fnum, fill=scale_colour(acc, 0.72) + (255,))
        x += fnum.getlength(ch) + track
    nums = nums.filter(ImageFilter.GaussianBlur(1.4 * S))
    nums.putalpha(Image.fromarray(
        np.clip(np.asarray(nums.split()[3]).astype(np.float32) * alpha, 0, 255).astype(np.uint8)))
    scene.alpha_composite(nums)


def name_block(scene, name, subtitle, acc, bebas, outfit):
    """Name and line, left, in the dark column the clock and the hero name live in."""
    txt = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    td = ImageDraw.Draw(txt)
    fname = ImageFont.truetype(str(bebas), int(h * 0.086))
    x = w * 0.050
    for ch in name:
        td.text((x, h * 0.600), ch, font=fname, fill=(240, 242, 247, 236))
        x += fname.getlength(ch) + int(h * 0.0055)
    td.rectangle([w * 0.050, h * 0.707, w * 0.050 + w * 0.052, h * 0.707 + h * 0.0035],
                 fill=scale_colour(acc, 1.35) + (255,))
    fsub = ImageFont.truetype(str(outfit), int(h * 0.0205))
    try:
        fsub.set_variation_by_axes([600])
    except Exception:
        pass
    sx = w * 0.050 + w * 0.068
    for ch in subtitle:
        td.text((sx, h * 0.7015), ch, font=fsub, fill=(180, 186, 200, 214))
        sx += fsub.getlength(ch) + int(h * 0.0030)
    scene.alpha_composite(txt)


def vignette(scene, rng):
    """Keep the text areas dark: top band, left side, bottom third (themes/README.md)."""
    out = np.asarray(scene.convert("RGB")).astype(np.float32)
    Y, X = np.mgrid[0:h, 0:w].astype(np.float32)
    v = np.ones((h, w), dtype=np.float32)
    v *= np.clip(1 - np.exp(-((Y / h) / 0.15) ** 2) * 0.44, 0, 1)              # top band, the clock
    v *= np.clip(1 - np.exp(-((X / w) / 0.25) ** 2) * 0.42, 0, 1)              # left column
    v *= np.clip(1 - ((Y / h - 0.62) / 0.38).clip(0, 1) ** 1.3 * 0.66, 0, 1)   # bottom third, the dock
    out *= v[:, :, None]
    out += rng.normal(0, 1.5, out.shape)                                       # grain, against banding
    return Image.fromarray(np.clip(out, 0, 255).astype(np.uint8), "RGB").resize((W, H), Image.LANCZOS)


# ---- one card ----------------------------------------------------------------------------------

def photo_card(name, number, subtitle, accent, src, bebas, outfit):
    """A card built on a photograph, the way the "Your TV, Your Way" guide builds a wallpaper.

    Only for pictures you have the right to use. See the note in this file's docstring.

    The guide asks for one thing of a wallpaper: key art that fills the screen, black and white because
    it looks calm, with a dark lower third so the app row sits on black, exported as PNG because JPEG
    bands in dark gradients. So the picture is cropped to 16:9 and fills the card, rendered in
    monochrome, darkened, and the dock's third is taken down to black under it.
    """
    rng = np.random.default_rng(zlib.crc32(str(src).encode()) & 0xFFFFFFFF)
    acc = rgb(accent)
    src_im = Image.open(src).convert("RGB")

    fy = PHOTO_CROP.get(str(number), 0.42)
    photo = ImageEnhance.Contrast(ImageOps.grayscale(cover(src_im, w, h, focus_y=fy))).enhance(1.08)
    arr = np.repeat(np.asarray(photo, dtype=np.float32)[:, :, None], 3, axis=2) * 0.82
    scene = Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8), "RGB").convert("RGBA")

    # the number sits in the dark left column, under the name, where it will not cross the player
    ghost_number(scene, number, (255, 255, 255), bebas, alpha=0.12, at_x=0.20)

    # a photograph is busier than a drawn scene, so the left column gets a scrim under the name
    ramp = np.clip(1 - np.arange(w, dtype=np.float32) / (w * 0.58), 0, 1) ** 1.25 * 205
    scrim = Image.new("RGBA", (w, h), (4, 5, 8, 255))
    scrim.putalpha(Image.fromarray(np.tile(ramp.astype(np.uint8), (h, 1)), "L"))
    scene.alpha_composite(scrim.filter(ImageFilter.GaussianBlur(10 * S)))

    # and the dark lower third the guide asks for: the dock's row of logos sits on black
    vramp = np.clip((np.arange(h, dtype=np.float32) / h - 0.50) / 0.50, 0, 1) ** 1.5 * 215
    foot = Image.new("RGBA", (w, h), (4, 5, 8, 255))
    foot.putalpha(Image.fromarray(np.tile(vramp.astype(np.uint8)[:, None], (1, w)), "L"))
    scene.alpha_composite(foot.filter(ImageFilter.GaussianBlur(12 * S)))

    name_block(scene, name, subtitle, acc, bebas, outfit)
    # one colour would break the calm, so the name, the rule and the number are left in grey too
    return ImageOps.grayscale(vignette(scene, rng)).convert("RGB")


def card(name, number, subtitle, accent, pose, seed, bebas, outfit):
    rng = np.random.default_rng(seed)
    acc = rgb(accent)
    hy = int(h * HORIZON)

    # --- night sky, tinted in the player's colour, brightening towards the ground
    img = np.zeros((h, w, 3), dtype=np.float32)
    stops = [(0.00, (4, 6, 12)), (0.45, scale_colour(acc, 0.075)),
             (0.80, scale_colour(acc, 0.17)), (1.00, scale_colour(acc, 0.26))]
    ys = np.linspace(0, 1, hy)
    for c in range(3):
        img[:hy, :, c] = np.interp(ys, [p for p, _ in stops], [col[c] for _, col in stops])[:, None]
    yy, xx = np.mgrid[0:hy, 0:w].astype(np.float32)
    for fx, power in ((0.05, 0.45), (0.29, 0.85), (0.71, 0.90), (0.96, 0.45)):
        cxp, cyp = w * fx, h * 0.045
        glow = np.exp(-(((xx - cxp) ** 2) / (w * 0.105) ** 2) - (((yy - cyp) ** 2) / (h * 0.115) ** 2))
        halo = np.exp(-(((xx - cxp) ** 2) / (w * 0.30) ** 2) - (((yy - cyp) ** 2) / (h * 0.32) ** 2))
        img[:hy] += (glow * power)[:, :, None] * np.array([182, 202, 230], dtype=np.float32)
        img[:hy] += (halo * power * 0.26)[:, :, None] * np.array([86, 116, 164], dtype=np.float32)
    for _ in range(110):
        x, y = int(rng.integers(0, w)), int(rng.integers(0, int(h * 0.26)))
        img[y:y + 2, x:x + 2] += rng.uniform(16, 58)
    scene = Image.fromarray(np.clip(img, 0, 255).astype(np.uint8), "RGB").convert("RGBA")

    shafts = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    sd = ImageDraw.Draw(shafts)
    for fx in (0.29, 0.71):
        cxp = w * fx
        sd.polygon([(cxp, h * 0.04), (cxp - w * 0.21, hy * 1.05), (cxp + w * 0.21, hy * 1.05)],
                   fill=(200, 218, 248, 15))
    scene.alpha_composite(shafts.filter(ImageFilter.GaussianBlur(30 * S)))

    # --- the stands: a dark band with the crowd as dim bokeh, under a rail
    stand = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    ImageDraw.Draw(stand).rectangle([0, h * 0.315, w, hy + 2], fill=(7, 9, 14, 255))
    crowd = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    cd = ImageDraw.Draw(crowd)
    for _ in range(2600):
        x, y = rng.uniform(0, w), rng.uniform(h * 0.325, hy - h * 0.045)
        r = rng.uniform(1.4, 3.6) * S
        k = rng.uniform(0.10, 0.38)
        base = rng.choice(np.array([[226, 206, 184], [190, 180, 200], [152, 178, 210], [228, 190, 146]]))
        cd.ellipse([x - r, y - r, x + r, y + r],
                   fill=(int(base[0] * k), int(base[1] * k), int(base[2] * k), 210))
    stand.alpha_composite(crowd.filter(ImageFilter.GaussianBlur(3.2 * S)))
    sd = ImageDraw.Draw(stand)
    sd.rectangle([0, h * 0.315, w, h * 0.330], fill=(3, 4, 8, 255))
    sd.rectangle([0, hy - h * 0.010, w, hy], fill=scale_colour(acc, 0.16) + (255,))
    scene.alpha_composite(stand)

    # --- the outfield: one smooth surface, lit in a pool in front of the batsman
    ground = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    gd = ImageDraw.Draw(ground)
    gd.rectangle([0, hy, w, h], fill=(11, 24, 17, 255))
    gd.polygon([(w * 0.32, hy), (w * 0.68, hy), (w * 0.99, h), (w * 0.01, h)], fill=(27, 36, 25, 255))
    for t in (0.055, 0.30):                                  # the popping crease and the return crease
        y0 = lerp(hy, h, t)
        half = lerp(w * 0.13, w * 0.20, t)
        gd.rectangle([w * 0.545 - half, y0, w * 0.545 + half, y0 + 0.0035 * h], fill=(200, 208, 190, 46))
    gd.arc([w * 0.00, hy - h * 0.06, w * 1.00, h * 1.34], 0, 180, fill=(224, 230, 214, 26), width=int(2 * S))
    scene.alpha_composite(ground)

    pool = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    ImageDraw.Draw(pool).ellipse([w * 0.555 - w * 0.40, h * 0.86 - h * 0.16,
                                  w * 0.555 + w * 0.40, h * 0.86 + h * 0.16],
                                 fill=(158, 186, 162, 40))
    scene.alpha_composite(pool.filter(ImageFilter.GaussianBlur(52 * S)))

    ghost_number(scene, number, acc, bebas)

    # --- the batsman and his stumps
    foot = h * 0.880
    Hh = h * 0.470
    cx = w * 0.555
    fig = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    fd = ImageDraw.Draw(fig)
    if pose != "stance":
        streaks(fd, rng, (cx + 0.10 * Hh, foot - 0.60 * Hh), scale_colour(acc, 1.0) + (74,))
    batter(fd, cx, foot, Hh, pose, (0, 0, 0, 255))
    stumps(fd, cx + 0.235 * Hh, foot, Hh, (0, 0, 0, 255))

    sh = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    ImageDraw.Draw(sh).ellipse([cx - w * 0.13, foot - h * 0.008, cx + w * 0.20, foot + h * 0.020],
                               fill=(0, 0, 0, 170))
    scene.alpha_composite(sh.filter(ImageFilter.GaussianBlur(7 * S)))
    rim = Image.new("RGBA", (w, h), scale_colour(acc, 1.35) + (255,))
    rim.putalpha(fig.split()[3])
    scene.alpha_composite(rim.filter(ImageFilter.GaussianBlur(1.0 * S)), (int(-2 * S), int(-1 * S)))
    scene.alpha_composite(fig)

    # --- the ball, with the trail it came in on
    ball = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    bd = ImageDraw.Draw(ball)
    bx, by = w * (0.315 if pose == "stance" else 0.745), h * (0.470 if pose == "stance" else 0.335)
    r = 0.019 * h
    for i in range(7):
        t = i / 7
        bd.ellipse([bx + t * 0.13 * w - r * (1 - t * 0.5), by + t * 0.05 * h - r * (1 - t * 0.5),
                    bx + t * 0.13 * w + r * (1 - t * 0.5), by + t * 0.05 * h + r * (1 - t * 0.5)],
                   fill=(232, 230, 222, int(16 + t * 20)))
    bd.ellipse([bx - r, by - r, bx + r, by + r], fill=(224, 220, 208, 235))
    bd.line([(bx - r * 0.55, by - r * 0.75), (bx + r * 0.55, by + r * 0.75)],
            fill=(120, 112, 100, 210), width=max(1, int(1.6 * S)))
    scene.alpha_composite(ball.filter(ImageFilter.GaussianBlur(0.5 * S)))

    name_block(scene, name, subtitle, acc, bebas, outfit)
    return vignette(scene, rng)


def main():
    ap = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    ap.add_argument("--fonts", default=os.environ.get("CLEANYOURTV_FONTS")
                    or str(Path(tempfile.gettempdir()) / "cleanyourtv-fonts"),
                    help="folder holding BebasNeue-Regular.ttf and Outfit.ttf, or to download them into")
    ap.add_argument("--only", default=None, help="draw just the card with this shirt number")
    ap.add_argument("--photos", default=None,
                    help="folder of pictures to build the cards on instead of drawing them, sorted by "
                         "name and paired with the legends in order; only use pictures you may use")
    args = ap.parse_args()

    bebas, outfit = fonts(Path(args.fonts))
    out_dir = Path(__file__).with_name("wallpapers")
    out_dir.mkdir(exist_ok=True)
    photos = []
    if args.photos:
        exts = {".jpg", ".jpeg", ".png", ".webp"}
        photos = sorted(p for p in Path(args.photos).iterdir() if p.suffix.lower() in exts)
        if not photos:
            raise SystemExit(f"no .jpg, .png or .webp pictures in {args.photos}")
        if len(photos) != len(LEGENDS) and not args.only:
            raise SystemExit(f"{len(photos)} pictures in {args.photos} for {len(LEGENDS)} legends: one "
                             f"picture per legend, in order, or --only <number> for a single card")
        for p in photos:
            with Image.open(p) as im:
                if im.width < 1280:
                    print(f"warning: {p.name} is {im.width}px across; it will look soft at 1920")
    for i, (name, number, subtitle, accent, pose, seed) in enumerate(LEGENDS, start=1):
        if args.only and args.only != number:
            continue
        slug = name.lower().replace(" ", "-").replace(".", "")
        stem = out_dir / f"{i:02d}-{slug}"
        if photos:
            src = photos[(i - 1) % len(photos)]
            print(f"{stem.name:28} from {src.name}")
            final = photo_card(name, number, subtitle, accent, src, bebas, outfit)
            path = stem.with_suffix(".png")       # PNG, not JPEG: JPEG bands in dark gradients
            final.save(path, "PNG", optimize=True)
        else:
            final = card(name, number, subtitle, accent, pose, seed, bebas, outfit)
            path = stem.with_suffix(".jpg")
            final.save(path, "JPEG", quality=86, optimize=True, progressive=True)
        for other in (stem.with_suffix(".jpg"), stem.with_suffix(".png")):
            if other != path and other.exists():
                other.unlink()                    # a rebuilt card must not sit beside its old self
        print(f"{path.name:28} {path.stat().st_size // 1024:4} KB")


if __name__ == "__main__":
    main()
