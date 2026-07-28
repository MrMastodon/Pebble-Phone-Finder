#!/usr/bin/env python3
"""Generates every icon and banner both apps need, from one shared motif.

Writes in-app assets straight into android/app/src/main/res/, and store
listing assets into store-assets/. The 25x25 Pebble menu icon has its own
script (watch/resources/images/generate_icon.py) because at that size it has
to be hand-placed pixel art rather than a downsampled drawing.

Everything is drawn at 4x and downsampled with LANCZOS, so edges are smooth
at every output size.

Requires Pillow:  pip install Pillow
Run from the repo root:  python3 tools/generate_assets.py
"""
import os
from PIL import Image, ImageDraw, ImageFont

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(REPO, "android", "app", "src", "main", "res")
STORE = os.path.join(REPO, "store-assets")

SS = 4  # supersampling factor

NAVY = (28, 32, 44, 255)
LIGHT = (235, 235, 240, 255)
ORANGE = (255, 149, 0, 255)
WHITE = (255, 255, 255, 255)


def draw_motif(size, phone_colour, wave_colour, screen_colour=None, scale=1.0,
               wave_weight=0.028):
    """A phone with sound waves radiating from its top-right corner.

    Returns an RGBA image of `size`x`size` with a transparent background,
    with the artwork auto-centred: it's drawn on an oversized canvas, then
    cropped to its own alpha bounds and re-centred, so the result is
    balanced no matter how the shapes below are laid out. `scale` is the
    fraction of the canvas the artwork fills - use it for adaptive-icon
    safe zones.
    """
    n = size * SS
    pad = n  # generous margin so nothing clips before cropping
    canvas = n + 2 * pad
    img = Image.new("RGBA", (canvas, canvas), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)

    def px(v):
        return v * n + pad

    body = [px(0.26), px(0.22), px(0.58), px(0.78)]
    radius = 0.055 * n
    d.rounded_rectangle(body, radius=radius, fill=phone_colour)

    if screen_colour is not None:
        inset = [px(0.30), px(0.27), px(0.54), px(0.68)]
        d.rounded_rectangle(inset, radius=radius * 0.5, fill=screen_colour)
        cx, cy, r = px(0.42), px(0.725), 0.022 * n
        d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=phone_colour)

    # Three arcs radiating from the phone's top-right corner, growing
    # outwards - the usual "ringing" shorthand.
    origin = (0.58, 0.26)
    width = max(SS, int(wave_weight * n))
    for r in (0.13, 0.21, 0.29):
        box = [px(origin[0] - r), px(origin[1] - r),
               px(origin[0] + r), px(origin[1] + r)]
        d.arc(box, start=-72, end=18, fill=wave_colour, width=width)

    art = img.crop(img.getbbox())
    target = max(1, int(size * scale * SS))
    ratio = target / max(art.size)
    art = art.resize(
        (max(1, int(art.size[0] * ratio)), max(1, int(art.size[1] * ratio))),
        Image.LANCZOS,
    )

    out = Image.new("RGBA", (size * SS, size * SS), (0, 0, 0, 0))
    out.alpha_composite(art, ((out.size[0] - art.size[0]) // 2,
                              (out.size[1] - art.size[1]) // 2))
    return out.resize((size, size), Image.LANCZOS)


def solid(size, colour):
    return Image.new("RGBA", (size, size), colour)


def save(img, path):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path)
    print(f"  {os.path.relpath(path, REPO)}  {img.size[0]}x{img.size[1]}")


# --- Android in-app assets -------------------------------------------------

# Adaptive icon layers are 108dp; only the middle 72dp is guaranteed visible,
# so the motif is scaled to sit inside that safe zone.
ADAPTIVE = {"mdpi": 108, "hdpi": 162, "xhdpi": 216, "xxhdpi": 324, "xxxhdpi": 432}
# Legacy square/round launcher icons for API < 26.
LEGACY = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
# Notification small icons: 24dp, white-on-transparent (Android tints them).
NOTIFICATION = {"mdpi": 24, "hdpi": 36, "xhdpi": 48, "xxhdpi": 72, "xxxhdpi": 96}


def build_android():
    print("Android in-app assets:")
    for density, size in ADAPTIVE.items():
        save(solid(size, NAVY), f"{RES}/mipmap-{density}/ic_launcher_background.png")
        # Only the central 72 of the 108dp layer survives masking, and a
        # circular mask inscribed in that leaves a square of 72/sqrt(2) =
        # 51dp, i.e. 0.47 of the layer. This motif has content in opposite
        # corners (phone bottom-left, waves top-right), so it has to respect
        # the circle rather than the square.
        save(draw_motif(size, LIGHT, ORANGE, NAVY, scale=0.47),
             f"{RES}/mipmap-{density}/ic_launcher_foreground.png")

    for density, size in LEGACY.items():
        icon = solid(size, NAVY)
        icon.alpha_composite(draw_motif(size, LIGHT, ORANGE, NAVY, scale=0.78))
        save(icon, f"{RES}/mipmap-{density}/ic_launcher.png")

    for density, size in NOTIFICATION.items():
        # Silhouette only: a notification icon is a mask, so any colour
        # detail would be flattened to a white blob anyway.
        save(draw_motif(size, WHITE, WHITE, scale=0.92),
             f"{RES}/drawable-{density}/ic_notification.png")


# --- Store listing assets --------------------------------------------------

def build_store():
    print("Store listing assets:")

    # Google Play listing icon: 512x512, 32-bit PNG.
    icon = solid(512, NAVY)
    icon.alpha_composite(draw_motif(512, LIGHT, ORANGE, NAVY, scale=0.74))
    save(icon, f"{STORE}/play/icon-512.png")

    # Rebble appstore icons, at the sizes its API serves.
    for size in (144, 80, 48, 28):
        listing = solid(size, NAVY)
        listing.alpha_composite(draw_motif(size, LIGHT, ORANGE, NAVY, scale=0.76))
        save(listing, f"{STORE}/rebble/icon-{size}.png")

    save(banner(1024, 500, subtitle=True), f"{STORE}/play/feature-graphic-1024x500.png")
    save(banner(720, 320, subtitle=False), f"{STORE}/rebble/banner-720x320.png")


def load_font(size):
    for path in (
        "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf",
        "/usr/share/fonts/truetype/liberation/LiberationSans-Bold.ttf",
    ):
        if os.path.exists(path):
            return ImageFont.truetype(path, size)
    return ImageFont.load_default()


def fitted_font(draw, text, max_width, start_size):
    """Largest bold font at or below `start_size` that keeps `text` inside
    `max_width`. Measured rather than guessed - eyeballing it is how banner
    titles end up clipped."""
    size = start_size
    while size > 8:
        font = load_font(size)
        if draw.textlength(text, font=font) <= max_width:
            return font
        size -= max(1, size // 20)
    return load_font(8)


def banner(width, height, subtitle):
    """Wide promotional banner. Flattened to RGB: Play rejects alpha here."""
    n_w, n_h = width * SS, height * SS
    img = Image.new("RGBA", (n_w, n_h), NAVY)
    d = ImageDraw.Draw(img)

    # Subtle vertical wash so it isn't a flat rectangle.
    for i in range(n_h):
        t = i / n_h
        d.line([(0, i), (n_w, i)],
               fill=(int(28 + 14 * t), int(32 + 16 * t), int(44 + 22 * t), 255))

    margin = int(n_w * 0.06)
    art = int(n_h * 0.68)
    motif = draw_motif(art // SS, LIGHT, ORANGE, NAVY, scale=1.0).resize(
        (art, art), Image.LANCZOS)
    img.alpha_composite(motif, (margin, (n_h - art) // 2))

    text_x = margin + art + int(n_w * 0.04)
    # Play crops the feature graphic in some placements, so keep a real
    # margin on the right rather than running to the edge.
    text_width = n_w - text_x - margin

    title_font = fitted_font(d, "Find My Phone", text_width, int(n_h * 0.20))
    lines = ["Ring your phone from your Pebble.", "Loud, even on silent."]

    if subtitle:
        sub_size = int(n_h * 0.085)
        sub_font = load_font(sub_size)
        while sub_size > 8 and max(
                d.textlength(t, font=sub_font) for t in lines) > text_width:
            sub_size -= 1
            sub_font = load_font(sub_size)

        title_h = title_font.getbbox("Find My Phone")[3]
        block_h = title_h + int(n_h * 0.06) + 2 * int(sub_size * 1.25)
        y = (n_h - block_h) // 2
        d.text((text_x, y), "Find My Phone", font=title_font, fill=WHITE)
        y += title_h + int(n_h * 0.06)
        for line in lines:
            d.text((text_x, y), line, font=sub_font, fill=ORANGE)
            y += int(sub_size * 1.25)
    else:
        title_h = title_font.getbbox("Find My Phone")[3]
        d.text((text_x, (n_h - title_h) // 2), "Find My Phone",
               font=title_font, fill=WHITE)

    return img.resize((width, height), Image.LANCZOS).convert("RGB")


def build_watch():
    """The Pebble menu icon: 25x25 is the SDK's hard maximum.

    Same motif as everything else, but with a much heavier wave stroke -
    at this size the default weight downsamples to well under a pixel and
    the arcs disappear.
    """
    print("Pebble watchapp:")
    icon = solid(25, NAVY)
    icon.alpha_composite(
        draw_motif(25, LIGHT, ORANGE, NAVY, scale=0.82, wave_weight=0.060))
    save(icon, os.path.join(REPO, "watch", "resources", "images", "icon.png"))


if __name__ == "__main__":
    build_android()
    build_watch()
    build_store()
    print("\nScreenshots are deliberately not generated: both stores expect "
          "real captures from a device.")
