#!/usr/bin/env python3
"""Regenerates icon.png: a 25x25 app icon for the Pebble menu icon (and,
since this app isn't appstore-published, the phone-side app list too).
See the "Where does the watch app's icon come from?" section of the root
README. Requires Pillow: pip install Pillow
"""
from PIL import Image, ImageDraw

size = 25
img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
d = ImageDraw.Draw(img)

bg_color = (28, 32, 44, 255)       # solid dark navy background (opaque)
phone_color = (235, 235, 240, 255)
screen_color = (28, 32, 44, 255)
accent_color = (255, 149, 0, 255)  # orange "alarm" accent

# Full opaque background square: some renderers don't composite a
# transparent PNG background correctly, which can show up as an empty/blank
# icon - an opaque background is also just the more common style for
# small color-platform app icons.
d.rounded_rectangle([0, 0, size - 1, size - 1], radius=4, fill=bg_color)

# Phone silhouette in a light color for contrast against the dark background.
d.rounded_rectangle([6, 4, 17, 21], radius=2, fill=phone_color)
d.rounded_rectangle([7, 6, 16, 17], radius=1, fill=screen_color)
d.ellipse([10, 18, 13, 21], fill=phone_color)

# Sound wave ticks radiating from the phone's top-right corner (a "ringing"
# glyph) - short diagonal strokes read more clearly than arcs at 25px.
d.line([18, 5, 21, 2], fill=accent_color, width=2)
d.line([20, 9, 24, 7], fill=accent_color, width=2)
d.line([17, 1, 19, -1], fill=accent_color, width=2)

img.save("icon.png")
