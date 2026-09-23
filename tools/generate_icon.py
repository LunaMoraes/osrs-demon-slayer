"""Draw the original Kasugai Crow toolbar emblem as an optimized PNG."""

from pathlib import Path
from PIL import Image, ImageDraw


SCALE = 4
SIZE = 48
image = Image.new("RGBA", (SIZE * SCALE, SIZE * SCALE), (0, 0, 0, 0))
draw = ImageDraw.Draw(image)


def box(x1, y1, x2, y2):
    return tuple(round(value * SCALE) for value in (x1, y1, x2, y2))


def points(*coords):
    return [(round(x * SCALE), round(y * SCALE)) for x, y in coords]


draw.ellipse(box(1.5, 1.5, 46.5, 46.5), fill="#211b1d", outline="#d3a762", width=2 * SCALE)
draw.ellipse(box(5, 5, 43, 43), outline="#76343b", width=SCALE)
draw.polygon(points((9, 31), (4, 25), (14, 26), (6, 18), (20, 25), (16, 35)), fill="#111215")
draw.polygon(points((12, 31), (9, 10), (27, 21), (30, 35), (22, 39)), fill="#333338")
draw.polygon(points((11, 11), (25, 22), (19, 24)), fill="#5a5557")
draw.polygon(points((14, 18), (26, 24), (21, 29)), fill="#777070")
draw.polygon(points((15, 26), (26, 29), (22, 34)), fill="#5b5354")
draw.polygon(points((23, 21), (31, 16), (37, 22), (34, 29), (30, 33), (26, 28)), fill="#141619")
draw.polygon(points((35, 23), (44, 26), (35, 28)), fill="#d3a762")
draw.ellipse(box(32, 20, 34.5, 22.5), fill="#d7454f")
draw.line(points((21, 36), (19, 41)), fill="#b28654", width=SCALE)
draw.line(points((27, 35), (26, 41)), fill="#b28654", width=SCALE)
draw.arc(box(4, 4, 44, 44), 215, 300, fill="#d3a762", width=SCALE)

output = Path(__file__).resolve().parents[1] / "src/main/resources/com/demonslayer/crow.png"
output.parent.mkdir(parents=True, exist_ok=True)
image.resize((SIZE, SIZE), Image.Resampling.LANCZOS).save(output, optimize=True)
print(output)
