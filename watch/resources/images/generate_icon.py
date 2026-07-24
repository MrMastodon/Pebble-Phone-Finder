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

phone_color = (40, 40, 45, 255)
screen_color = (235, 235, 240, 255)
accent_color = (255, 149, 0, 255)

# Phone silhouette, shifted slightly left/down to leave clear room top-right
# for the sound-wave ticks.
d.rounded_rectangle([3, 5, 14, 23], radius=2, fill=phone_color)
d.rounded_rectangle([4, 7, 13, 19], radius=1, fill=screen_color)
d.ellipse([7, 20, 10, 23], fill=(20, 20, 24, 255))

# Sound wave ticks radiating from the phone's top-right corner (a "ringing"
# glyph) - short diagonal strokes read more clearly than arcs at 25px.
d.line([17, 6, 20, 3], fill=accent_color, width=2)
d.line([19, 10, 23, 8], fill=accent_color, width=2)
d.line([16, 2, 18, 0], fill=accent_color, width=2)

img.save("icon.png")
