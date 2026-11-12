"""The mark: the single definition of this app's identity, a capsule with a check punched through it.

Both generators import it (`make-launcher-icon.py` emits it as `VectorDrawable` path data,
`make-feature-graphic.py` rasterises it with PIL), so whatever you put here cannot drift into being
two different marks.

The way to replace it: draw or generate a concept image, run `python3 art/trace-mark.py <image>`,
and paste the curves it prints over MARK and HOLE below. Nothing else in the pipeline changes.
**Commit the concept image beside this file** and record where it came from in `art/README.md`.

**Use art you have the right to ship.** An icon traced from an emoji font, a stock icon or someone
else's mark is a licence obligation at best and a rejected Play upload at worst. Art you generated
from your own prompt, or drew, is clean.

The geometry is two subpaths: the shape, and the hole punched out of it. Coordinates are in
arbitrary units around an origin at the mark's centre; callers fit them to their own canvas with
`fit()`. y grows downward, as it does in both SVG and PIL.
"""

import re
from dataclasses import dataclass

# The icon's two colours — take both from the app's generated scheme (`scripts/gen_scheme.py`) rather
# than picking them for the icon, so the identity and the app agree. They are the *only* copy:
# make-launcher-icon.py writes ic_launcher_background.xml from GROUND, so recolouring the identity is
# an edit here and a re-run, never a second value somewhere else to keep in step. (It used to be
# two — this file and a hand-written background drawable — and an app built from the template found
# them disagreeing the first time its palette moved.)
#
# ⚠ **A dark tone of a warm hue is brown.** The first app built from this template tried its
# `primary` darkened for contrast as the ground and it read as mud; then a gradient, whose middle is
# the same mud at 44dp. What survived was a flat mid-tone role (`surfaceVariant`) under a light-ish
# ink (`primary`). Judge the ground at launcher size on a real home screen, light and dark.
GROUND = (0x29, 0x69, 0x55)  # the light scheme's `primary`
MARK_INK = (0xFB, 0xF9, 0xF1)  # the light scheme's `surface`


@dataclass(frozen=True)
class Subpath:
    """One closed contour, as cubic path data.

    `d` is always wound CLOCKWISE on screen (positive shoelace area with y downward). A hole is
    stored the same way and reversed at read time by `nodes()`, so it is subtracted by the non-zero
    fill rule that both SVG and VectorDrawable use by default. That is what makes the hole a real
    hole rather than a shape painted in the background colour — and it is why it survives the
    monochrome layer, where Android tints everything one flat colour and a painted hole would vanish.
    """

    d: str
    hole: bool = False


MARK = (
    "M-2.83,-62.23C15.14,-80.19 44.26,-80.19 62.23,-62.23"
        "C80.19,-44.26 80.19,-15.14 62.23,2.83"
        "C42.43,22.63 22.63,42.43 2.83,62.23"
        "C-15.14,80.19 -44.26,80.19 -62.23,62.23"
        "C-80.19,44.26 -80.19,15.14 -62.23,-2.83"
        "C-42.43,-22.63 -22.63,-42.43 -2.83,-62.23"
        "Z"
)

# A check punched through it, as one outline so the corner is not a double hole.
HOLE = (
    "M-19.4,-2.6C-15.31,1.49 -11.23,5.58 -7.14,9.67"
        "C2.33,-0.37 11.8,-10.42 21.27,-20.46"
        "C23.73,-23.07 27.85,-23.19 30.46,-20.73"
        "C33.07,-18.27 33.19,-14.15 30.73,-11.54"
        "C19.73,0.13 8.73,11.79 -2.27,23.46"
        "C-3.48,24.74 -5.15,25.47 -6.9,25.5"
        "C-8.66,25.53 -10.35,24.84 -11.6,23.6"
        "C-17.26,17.93 -22.93,12.26 -28.6,6.6"
        "C-31.13,4.06 -31.13,-0.06 -28.6,-2.6"
        "C-26.06,-5.13 -21.94,-5.13 -19.4,-2.6"
        "Z"
)

PARTS = (
    Subpath(MARK),
    Subpath(HOLE, hole=True),
)

_NUM = re.compile(r"-?\d*\.?\d+(?:e-?\d+)?")


def _points(d):
    """Path data -> the flat control-point list: 1 start point plus 3 per cubic segment."""
    pts = []
    for chunk in re.finditer(r"[MC]([^MCZz]*)", d):
        nums = [float(v) for v in _NUM.findall(chunk.group(1))]
        pts += list(zip(nums[0::2], nums[1::2]))
    return pts


def nodes(e, tf=lambda p: p):
    """Control points, caller-transformed, wound for fill or hole."""
    pts = _points(e.d)
    if e.hole:
        pts.reverse()
    return [tf(p) for p in pts]


def path_data(e, tf=lambda p: p, precision=2):
    """SVG/VectorDrawable path data for one subpath."""
    n = nodes(e, tf)

    def f(p):
        return f"{round(p[0], precision):g},{round(p[1], precision):g}"

    out = [f"M{f(n[0])}"]
    for i in range(1, len(n), 3):
        out.append("C" + " ".join(f(n[i + j]) for j in range(3)))
    return "".join(out) + "Z"


def outline(e, tf=lambda p: p, steps=32):
    """The same subpath flattened to a polygon, for renderers that can't draw curves."""
    n = nodes(e, tf)
    pts = []
    for i in range(1, len(n), 3):
        (x0, y0), (x1, y1), (x2, y2), (x3, y3) = n[i - 1], n[i], n[i + 1], n[i + 2]
        for s in range(steps):
            t = s / steps
            u = 1 - t
            pts.append((
                u * u * u * x0 + 3 * u * u * t * x1 + 3 * u * t * t * x2 + t * t * t * x3,
                u * u * u * y0 + 3 * u * u * t * y1 + 3 * u * t * t * y2 + t * t * t * y3,
            ))
    return pts


def bounds(parts=PARTS):
    """Bounding box of the silhouette. Holes are ignored — a hole cannot extend the mark."""
    xs, ys = [], []
    for e in parts:
        if e.hole:
            continue
        for x, y in outline(e):
            xs.append(x)
            ys.append(y)
    return min(xs), min(ys), max(xs), max(ys)


def fit(canvas, art_size, parts=PARTS):
    """A transform placing the mark centred on a square canvas at a given longest-side size.

    Returns a function mapping mark coordinates to canvas coordinates.
    """
    x0, y0, x1, y1 = bounds(parts)
    scale = art_size / max(x1 - x0, y1 - y0)
    cx, cy = (x0 + x1) / 2, (y0 + y1) / 2
    return lambda p: (canvas / 2 + (p[0] - cx) * scale, canvas / 2 + (p[1] - cy) * scale)
