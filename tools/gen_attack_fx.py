#!/usr/bin/env python3
"""
Gera as folhas de animação de ataque do PokeWorld: 3 quadros x 1 linha (3x1), 128x128 por quadro.
  fx_<elemento>_direct_3x1.png  -> projétil/golpe direto (aponta para a direita; o jogo rotaciona)
  fx_<elemento>_area_3x1.png    -> explosão em área (cresce e some ao longo dos 3 quadros)
Uso: python3 tools/gen_attack_fx.py   (escreve em app/src/main/res/drawable)
Para trocar a arte, basta substituir os PNGs pelos seus (mesmo nome, 3 quadros lado a lado).
"""
import math, os, random
from PIL import Image, ImageDraw, ImageFilter

F = 128           # tamanho de cada quadro (px)
S = 3             # super-amostragem
OUT = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "res", "drawable")

def layer():
    return Image.new("RGBA", (F * S, F * S), (0, 0, 0, 0))

def rgba(c, a=255):
    return (c[0], c[1], c[2], a)

def blur(img, r):
    return img.filter(ImageFilter.GaussianBlur(r * S))

def glow(img, color, r=6, strength=1.0):
    """Halo colorido ao redor do que já foi desenhado."""
    a = img.split()[3]
    halo = Image.new("RGBA", img.size, rgba(color, 0))
    halo.putalpha(a.point(lambda v: int(v * strength)))
    halo = blur(halo, r)
    out = Image.new("RGBA", img.size, (0, 0, 0, 0))
    out.alpha_composite(halo)
    out.alpha_composite(img)
    return out

def disc(d, cx, cy, r, fill):
    d.ellipse([(cx - r) * S, (cy - r) * S, (cx + r) * S, (cy + r) * S], fill=fill)

def poly(d, pts, fill):
    d.polygon([(x * S, y * S) for x, y in pts], fill=fill)

def line(d, pts, width, fill):
    d.line([(x * S, y * S) for x, y in pts], fill=fill, width=int(width * S), joint="curve")

def rot(pts, ang, cx=64, cy=64):
    ca, sa = math.cos(ang), math.sin(ang)
    return [(cx + (x - cx) * ca - (y - cy) * sa, cy + (x - cx) * sa + (y - cy) * ca) for x, y in pts]

def lerp(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))

def finish(img):
    return img.resize((F, F), Image.LANCZOS)

def sheet(frames, name):
    s = Image.new("RGBA", (F * len(frames), F), (0, 0, 0, 0))
    for i, f in enumerate(frames):
        s.paste(f, (i * F, 0))
    s.save(os.path.join(OUT, name))

# ---------------------------------------------------------------- paletas
PAL = {
    "fire":     [(120, 20, 10), (255, 80, 20), (255, 170, 30), (255, 245, 170)],
    "water":    [(20, 60, 160), (50, 130, 240), (120, 200, 255), (235, 250, 255)],
    "electric": [(150, 110, 0), (255, 210, 20), (255, 245, 110), (255, 255, 235)],
    "plant":    [(20, 90, 30), (60, 170, 60), (130, 220, 90), (225, 255, 190)],
    "ice":      [(40, 110, 170), (110, 200, 240), (190, 240, 255), (250, 255, 255)],
    "rock":     [(70, 55, 40), (130, 105, 75), (180, 155, 120), (225, 210, 180)],
    "poison":   [(70, 20, 100), (150, 60, 190), (205, 120, 235), (245, 205, 255)],
    "psychic":  [(150, 20, 110), (240, 80, 170), (255, 150, 215), (255, 230, 245)],
    "dragon":   [(30, 20, 120), (90, 70, 230), (60, 200, 240), (225, 245, 255)],
    "normal":   [(110, 110, 120), (190, 190, 200), (235, 235, 240), (255, 255, 255)],
}

# ================================================================ DIRETO (projétil apontando para a direita)
def flame_shape(d, cx, cy, size, pal, wob):
    """Gota de fogo com cauda ondulante para a esquerda."""
    for k, col in enumerate(pal[:3] + [pal[3]]):
        r = size * (1 - k * 0.2)
        tail = [(cx + r * 0.9, cy)]
        for i in range(0, 9):
            t = i / 8
            tx = cx - t * r * 2.4
            amp = r * (0.9 - 0.7 * t)
            ty = cy - amp * (1 if i % 2 == 0 else 0.75) + math.sin(t * 6 + wob) * 3
            tail.append((tx, ty))
        tail.append((cx - r * 2.6, cy + math.sin(wob) * 2))
        for i in range(8, -1, -1):
            t = i / 8
            tx = cx - t * r * 2.4
            amp = r * (0.9 - 0.7 * t)
            ty = cy + amp * (1 if i % 2 == 0 else 0.75) + math.sin(t * 6 + wob + 1.7) * 3
            tail.append((tx, ty))
        poly(d, tail, rgba(col))
        disc(d, cx + r * 0.1, cy, r * 0.95, rgba(col))

def direct(elem, frame):
    pal = PAL[elem]
    im = layer(); d = ImageDraw.Draw(im)
    cx, cy = 78, 64
    wob = frame * 2.1
    if elem == "fire":
        flame_shape(d, cx, cy, 20, pal, wob)
    elif elem == "water":
        for i in range(5):                                   # rastro de gotas
            disc(d, cx - 22 - i * 11, cy + math.sin(wob + i) * 5, 6 - i * 0.9, rgba(pal[1], 200 - i * 35))
        disc(d, cx, cy, 22, rgba(pal[0])); disc(d, cx, cy, 19, rgba(pal[1])); disc(d, cx - 3, cy - 2, 14, rgba(pal[2], 230))
        disc(d, cx - 8, cy - 9, 6, rgba(pal[3])); disc(d, cx + 6 + frame, cy + 7, 2.5, rgba(pal[3], 200))
    elif elem == "electric":
        disc(d, cx + 4, cy, 17, rgba(pal[1])); disc(d, cx + 4, cy, 12, rgba(pal[2])); disc(d, cx + 4, cy, 6, rgba(pal[3]))
        rnd = random.Random(frame * 7 + 3)
        for b in range(3):                                   # raios em zigue-zague para trás
            x, y = cx - 6, cy + (b - 1) * 7
            pts = [(x, y)]
            for i in range(5):
                x -= 11; y += rnd.uniform(-11, 11); pts.append((x, y))
            line(d, pts, 4.5, rgba(pal[2])); line(d, pts, 2, rgba(pal[3]))
    elif elem == "plant":
        for i in range(3):                                   # 3 folhas girando
            ang = wob + i * 2.094
            ox, oy = cx + math.cos(ang) * 11, cy + math.sin(ang) * 11
            leaf = [(ox - 14, oy), (ox - 4, oy - 8), (ox + 9, oy - 6), (ox + 16, oy), (ox + 9, oy + 6), (ox - 4, oy + 8)]
            poly(d, rot(leaf, ang + 0.6, ox, oy), rgba(pal[1 + (i % 2)]))
            line(d, rot([(ox - 12, oy), (ox + 13, oy)], ang + 0.6, ox, oy), 1.6, rgba(pal[0]))
        for i in range(4): disc(d, cx - 24 - i * 9, cy + math.sin(wob + i) * 6, 3.2 - i * 0.5, rgba(pal[2], 190))
    elif elem == "ice":
        for i in range(4): disc(d, cx - 22 - i * 10, cy + math.sin(wob + i * 1.3) * 7, 3 - i * 0.4, rgba(pal[3], 210))
        shard = [(cx + 30, cy), (cx, cy - 12), (cx - 26, cy), (cx, cy + 12)]
        poly(d, shard, rgba(pal[0])); poly(d, [(cx + 26, cy), (cx, cy - 9), (cx - 20, cy), (cx, cy + 9)], rgba(pal[1]))
        poly(d, [(cx + 22, cy), (cx, cy - 9), (cx - 4, cy)], rgba(pal[3], 235))
        s = 4 + frame * 2; line(d, [(cx - s, cy - 18), (cx - s, cy - 28)], 2, rgba(pal[3])); line(d, [(cx - s - 5, cy - 23), (cx - s + 5, cy - 23)], 2, rgba(pal[3]))
    elif elem == "rock":
        for i in range(4): disc(d, cx - 24 - i * 9 + frame * 2, cy + 10 + math.sin(wob + i) * 6, 5 - i, rgba(pal[2], 150 - i * 25))
        ang = frame * 0.9
        b = [(cx - 20, cy - 8), (cx - 8, cy - 20), (cx + 10, cy - 18), (cx + 22, cy - 4), (cx + 14, cy + 15), (cx - 4, cy + 21), (cx - 20, cy + 10)]
        b = rot(b, ang, cx, cy)
        poly(d, b, rgba(pal[0])); poly(d, [(cx + (x - cx) * 0.86, cy + (y - cy) * 0.86) for x, y in b], rgba(pal[1]))
        poly(d, rot([(cx - 10, cy - 10), (cx + 2, cy - 14), (cx + 8, cy - 4), (cx - 4, cy)], ang, cx, cy), rgba(pal[2], 220))
    elif elem == "poison":
        for i in range(4): disc(d, cx - 22 - i * 10, cy + math.sin(wob + i) * 6, 5 - i, rgba(pal[1], 190 - i * 35))
        disc(d, cx, cy, 20, rgba(pal[0])); disc(d, cx, cy, 17, rgba(pal[1])); disc(d, cx - 3, cy - 3, 10, rgba(pal[2], 220))
        for i, (ox, oy, rr) in enumerate([(-10, -14, 5), (8, -16, 4), (12, 8, 5), (-12, 12, 4)]):
            disc(d, cx + ox + math.sin(wob + i) * 2, cy + oy, rr, rgba(pal[2], 230)); disc(d, cx + ox - 1, cy + oy - 1, rr * 0.4, rgba(pal[3]))
    elif elem == "psychic":
        for k in range(3):
            r = 10 + ((frame + k) % 3) * 8
            d.ellipse([(cx - r) * S, (cy - r) * S, (cx + r) * S, (cy + r) * S], outline=rgba(pal[1 + (k % 2)], 230 - k * 50), width=int(3 * S))
        disc(d, cx, cy, 11, rgba(pal[2])); disc(d, cx, cy, 6, rgba(pal[3]))
        for i in range(4):
            a = wob + i * 1.57; disc(d, cx + math.cos(a) * 26, cy + math.sin(a) * 26, 2.8, rgba(pal[3], 230))
    elif elem == "dragon":
        for i in range(5): disc(d, cx - 24 - i * 9, cy + math.sin(wob + i) * 5, 5.5 - i, rgba(pal[2], 200 - i * 35))
        # presa em forma de crescente
        out = [(cx + 28, cy)]
        for i in range(0, 13):
            a = -1.5 + i * (3.0 / 12); out.append((cx + math.cos(a) * 30 - 6, cy + math.sin(a) * 30))
        inn = []
        for i in range(12, -1, -1):
            a = -1.4 + i * (2.8 / 12); inn.append((cx + math.cos(a) * 20 - 2, cy + math.sin(a) * 20))
        poly(d, out + inn, rgba(pal[1])); poly(d, out[:-3] + inn[3:], rgba(pal[2], 235))
        disc(d, cx + 10, cy, 7, rgba(pal[3])); disc(d, cx + 10, cy, 3.5 + frame * 0.6, rgba(pal[2]))
    else:  # normal
        for i in range(4): disc(d, cx - 22 - i * 10, cy + math.sin(wob + i) * 6, 4.5 - i * 0.8, rgba(pal[2], 210 - i * 40))
        pts = []
        for i in range(10):
            a = i * math.pi / 5 + frame * 0.35; rr = 24 if i % 2 == 0 else 10
            pts.append((cx + math.cos(a) * rr, cy + math.sin(a) * rr))
        poly(d, pts, rgba(pal[1])); poly(d, [(cx + (x - cx) * 0.7, cy + (y - cy) * 0.7) for x, y in pts], rgba(pal[3]))
    im = glow(im, pal[1], r=5, strength=0.9)
    return finish(im)

# ================================================================ ÁREA (explosão centrada; cresce nos 3 quadros)
def area(elem, frame):
    pal = PAL[elem]
    im = layer(); d = ImageDraw.Draw(im)
    cx = cy = 64
    t = (frame + 1) / 3.0                 # 0.33 / 0.66 / 1.0
    R = 18 + 30 * t                       # raio da explosão
    fade = [255, 235, 150][frame]
    rnd = random.Random(sum(map(ord, elem)) * 13 + frame * 17)
    if elem == "fire":
        for k, col in enumerate(pal[:3]):
            pts = []
            for i in range(24):
                a = i * math.tau / 24; rr = R * (1 - k * 0.22) * (1.0 + (0.28 if i % 2 else -0.05) * rnd.uniform(0.6, 1.2))
                pts.append((cx + math.cos(a) * rr, cy + math.sin(a) * rr))
            poly(d, pts, rgba(col, fade))
        disc(d, cx, cy, R * 0.35, rgba(pal[3], fade))
    elif elem == "water":
        for k in range(2):
            r = R * (1 - k * 0.38)
            d.ellipse([(cx - r) * S, (cy - r) * S, (cx + r) * S, (cy + r) * S], outline=rgba(pal[1 + k], fade), width=int((7 - k * 2) * S))
        disc(d, cx, cy, R * 0.28, rgba(pal[2], int(fade * 0.8)))
        for i in range(10):
            a = i * math.tau / 10 + rnd.uniform(-0.2, 0.2); rr = R * (0.8 + 0.35 * rnd.random())
            disc(d, cx + math.cos(a) * rr, cy + math.sin(a) * rr, 3 + rnd.random() * 3, rgba(pal[2 + (i % 2)], fade))
    elif elem == "electric":
        disc(d, cx, cy, R * 0.45, rgba(pal[1], fade)); disc(d, cx, cy, R * 0.25, rgba(pal[3], fade))
        for i in range(8):
            a = i * math.tau / 8 + frame * 0.2; x, y = cx, cy; pts = [(x, y)]
            for j in range(4):
                x += math.cos(a) * R / 3.4 + rnd.uniform(-5, 5); y += math.sin(a) * R / 3.4 + rnd.uniform(-5, 5); pts.append((x, y))
            line(d, pts, 5, rgba(pal[1], fade)); line(d, pts, 2.2, rgba(pal[3], fade))
    elif elem == "plant":
        disc(d, cx, cy, R * 0.22, rgba(pal[1], int(fade * 0.7)))
        for i in range(10):
            a = i * math.tau / 10 + frame * 0.55; rr = R * (0.55 + 0.45 * ((i % 3) / 2))
            ox, oy = cx + math.cos(a) * rr, cy + math.sin(a) * rr
            leaf = [(ox - 9, oy), (ox - 2, oy - 5), (ox + 6, oy - 4), (ox + 11, oy), (ox + 6, oy + 4), (ox - 2, oy + 5)]
            poly(d, rot(leaf, a + 1.2, ox, oy), rgba(pal[1 + (i % 2)], fade))
    elif elem == "ice":
        disc(d, cx, cy, R * 0.3, rgba(pal[2], int(fade * 0.8)))
        for i in range(6):
            a = i * math.tau / 6 + 0.26 * frame; L = R
            tip = (cx + math.cos(a) * L, cy + math.sin(a) * L)
            poly(d, [tip, (cx + math.cos(a + 0.22) * L * 0.35, cy + math.sin(a + 0.22) * L * 0.35), (cx, cy),
                     (cx + math.cos(a - 0.22) * L * 0.35, cy + math.sin(a - 0.22) * L * 0.35)], rgba(pal[1], fade))
            poly(d, [tip, (cx + math.cos(a + 0.1) * L * 0.3, cy + math.sin(a + 0.1) * L * 0.3), (cx, cy)], rgba(pal[3], int(fade * 0.9)))
        for i in range(8):
            a = rnd.uniform(0, math.tau); rr = R * rnd.uniform(0.5, 1.1); disc(d, cx + math.cos(a) * rr, cy + math.sin(a) * rr, 2.4, rgba(pal[3], fade))
    elif elem == "rock":
        d.ellipse([(cx - R) * S, (cy - R * 0.55 + 8) * S, (cx + R) * S, (cy + R * 0.55 + 8) * S], fill=rgba(pal[1], int(fade * 0.45)))
        for i in range(9):
            a = i * math.tau / 9 + rnd.uniform(-0.2, 0.2); rr = R * (0.45 + 0.5 * t) * rnd.uniform(0.7, 1.1)
            ox, oy = cx + math.cos(a) * rr, cy + math.sin(a) * rr - 6 * math.sin(t * 3)
            sz = 5 + rnd.random() * 6
            b = [(ox - sz, oy - sz * 0.4), (ox - sz * 0.3, oy - sz), (ox + sz, oy - sz * 0.5), (ox + sz * 0.7, oy + sz * 0.8), (ox - sz * 0.6, oy + sz * 0.7)]
            poly(d, rot(b, rnd.uniform(0, 3), ox, oy), rgba(pal[0 + (i % 2)], fade))
    elif elem == "poison":
        for i in range(9):
            a = i * math.tau / 9; rr = R * (0.15 + 0.55 * rnd.random() * t + 0.2); pr = 9 + 12 * t * rnd.random()
            disc(d, cx + math.cos(a) * rr, cy + math.sin(a) * rr, pr, rgba(pal[1 + (i % 2)], int(fade * 0.8)))
        disc(d, cx, cy, R * 0.4, rgba(pal[0], int(fade * 0.8)))
        for i in range(7):
            a = rnd.uniform(0, math.tau); rr = R * rnd.uniform(0.2, 1.0); disc(d, cx + math.cos(a) * rr, cy + math.sin(a) * rr, 3 + rnd.random() * 3, rgba(pal[3], fade))
    elif elem == "psychic":
        for k in range(3):
            r = R * (1 - k * 0.28)
            d.ellipse([(cx - r) * S, (cy - r) * S, (cx + r) * S, (cy + r) * S], outline=rgba(pal[1 + (k % 2)], max(60, fade - k * 40)), width=int(4 * S))
        disc(d, cx, cy, R * 0.22, rgba(pal[3], fade))
        for i in range(8):
            a = i * math.tau / 8 + frame * 0.4; rr = R * 0.95
            x, y = cx + math.cos(a) * rr, cy + math.sin(a) * rr
            poly(d, [(x, y - 5), (x + 2, y - 1), (x + 5, y), (x + 2, y + 1), (x, y + 5), (x - 2, y + 1), (x - 5, y), (x - 2, y - 1)], rgba(pal[3], fade))
    elif elem == "dragon":
        d.ellipse([(cx - R) * S, (cy - R) * S, (cx + R) * S, (cy + R) * S], outline=rgba(pal[1], fade), width=int(8 * S))
        d.ellipse([(cx - R * 0.7) * S, (cy - R * 0.7) * S, (cx + R * 0.7) * S, (cy + R * 0.7) * S], outline=rgba(pal[2], int(fade * 0.8)), width=int(4 * S))
        for i in range(3):                                   # marcas de garra
            ox = (i - 1) * 14 * t + 4; line(d, [(cx + ox - 12 * t, cy - 24 * t), (cx + ox + 12 * t, cy + 24 * t)], 5, rgba(pal[3], fade))
        disc(d, cx, cy, R * 0.2, rgba(pal[2], fade))
    else:  # normal
        for k, col in enumerate([pal[1], pal[2], pal[3]]):
            pts = []
            for i in range(16):
                a = i * math.tau / 16 + frame * 0.2; rr = (R if i % 2 == 0 else R * 0.5) * (1 - k * 0.25)
                pts.append((cx + math.cos(a) * rr, cy + math.sin(a) * rr))
            poly(d, pts, rgba(col, fade))
        d.ellipse([(cx - R * 1.05) * S, (cy - R * 1.05) * S, (cx + R * 1.05) * S, (cy + R * 1.05) * S], outline=rgba(pal[2], int(fade * 0.6)), width=int(3 * S))
    im = glow(im, pal[1], r=4, strength=0.8)
    return finish(im)

# ================================================================ TILE (um quadrado do chão; o jogo repete em grade 3x3, 5x5, cruz, anel...)
def tile(elem, frame):
    """Efeito que preenche UM tile (estilo PokeTibia): ex. veneno = bolhas roxas subindo e estourando."""
    pal = PAL[elem]
    im = layer(); d = ImageDraw.Draw(im)
    rnd = random.Random(sum(map(ord, elem)) * 31 + frame * 5)
    t = frame / 2.0
    # base: mancha translúcida no chão
    d.ellipse([22 * S, 34 * S, 106 * S, 104 * S], fill=rgba(pal[0], 70))
    if elem == "poison":
        for i in range(9):
            bx = 22 + (i * 37 + 13 * frame) % 84; by = 100 - ((i * 23 + frame * 24) % 80)
            r = 5 + (i % 3) * 3
            disc(d, bx, by, r, rgba(pal[1], 210)); disc(d, bx, by, r * 0.62, rgba(pal[2], 235)); disc(d, bx - r * 0.3, by - r * 0.3, r * 0.22, rgba(pal[3]))
        for i in range(3):
            x = 34 + i * 30; disc(d, x, 98 - frame * 4, 4, rgba(pal[2], 200))
    elif elem == "fire":
        for i in range(4):
            x = 28 + i * 24 + (frame - 1) * 3; hgt = 44 + 18 * ((i + frame) % 3)
            poly(d, [(x - 12, 108), (x - 6, 108 - hgt * 0.55), (x + (frame - 1) * 4, 108 - hgt), (x + 7, 108 - hgt * 0.5), (x + 12, 108)], rgba(pal[1]))
            poly(d, [(x - 7, 108), (x, 108 - hgt * 0.72), (x + 7, 108)], rgba(pal[2])); poly(d, [(x - 3, 108), (x, 108 - hgt * 0.4), (x + 3, 108)], rgba(pal[3]))
    elif elem == "water":
        for k in range(3):
            r = 16 + k * 14 + frame * 6
            d.ellipse([(64 - r) * S, (72 - r * 0.62) * S, (64 + r) * S, (72 + r * 0.62) * S], outline=rgba(pal[1 + (k % 2)], 220 - k * 50), width=int(4 * S))
        for i in range(6):
            a = i * 1.047 + frame; disc(d, 64 + math.cos(a) * 36, 66 - abs(math.sin(a * 2)) * 24 - frame * 4, 4, rgba(pal[2]))
    elif elem == "electric":
        for b in range(5):
            x, y = 24 + b * 20, 104; pts = [(x, y)]
            for i in range(4):
                x += rnd.uniform(-9, 9); y -= 20; pts.append((x, y))
            line(d, pts, 5, rgba(pal[1])); line(d, pts, 2.2, rgba(pal[3]))
        disc(d, 64, 70, 6 + frame * 2, rgba(pal[3], 220))
    elif elem == "plant":
        for i in range(6):
            x = 22 + i * 17; hgt = 30 + 16 * ((i + frame) % 3)
            line(d, [(x, 108), (x + (frame - 1) * 5, 108 - hgt * 0.5), (x + (frame - 1) * 9, 108 - hgt)], 5, rgba(pal[0]))
            lf = [(x + (frame - 1) * 9 - 10, 108 - hgt), (x + (frame - 1) * 9, 108 - hgt - 9), (x + (frame - 1) * 9 + 10, 108 - hgt)]
            poly(d, lf, rgba(pal[2]))
        for i in range(5): disc(d, 28 + (i * 29 + frame * 17) % 76, 90 - (i * 13 + frame * 14) % 60, 3.5, rgba(pal[3], 220))
    elif elem == "ice":
        for i in range(5):
            x = 24 + i * 20; hgt = 36 + 20 * ((i + frame) % 3)
            poly(d, [(x - 9, 108), (x, 108 - hgt), (x + 9, 108)], rgba(pal[1])); poly(d, [(x - 3, 108), (x, 108 - hgt), (x + 9, 108)], rgba(pal[2])); poly(d, [(x - 1, 100), (x, 108 - hgt), (x + 3, 100)], rgba(pal[3]))
        for i in range(5): disc(d, 20 + (i * 31 + frame * 19) % 90, 20 + (i * 17 + frame * 23) % 70, 2.6, rgba(pal[3]))
    elif elem == "rock":
        for i in range(6):
            x = 26 + (i * 41) % 80; y = 40 + ((i * 29 + frame * 26) % 62); sz = 9 + (i % 3) * 3
            b = [(x - sz, y - sz * 0.3), (x - sz * 0.2, y - sz), (x + sz, y - sz * 0.4), (x + sz * 0.7, y + sz * 0.8), (x - sz * 0.6, y + sz * 0.7)]
            poly(d, b, rgba(pal[0])); poly(d, [(x + (px - x) * .7, y + (py - y) * .7) for px, py in b], rgba(pal[1 + (i % 2)]))
        for i in range(4): disc(d, 30 + i * 22, 104, 6 + frame * 2, rgba(pal[2], 110))
    elif elem == "psychic":
        for k in range(2):
            r = 30 - k * 12
            d.ellipse([(64 - r) * S, (72 - r * 0.7) * S, (64 + r) * S, (72 + r * 0.7) * S], outline=rgba(pal[1 + k], 230), width=int(3 * S))
        for i in range(6):
            a = i * 1.047 + frame * 0.5; x, y = 64 + math.cos(a) * 24, 72 + math.sin(a) * 17
            poly(d, [(x, y - 7), (x + 2, y - 2), (x + 7, y), (x + 2, y + 2), (x, y + 7), (x - 2, y + 2), (x - 7, y), (x - 2, y - 2)], rgba(pal[3]))
        disc(d, 64, 72, 6, rgba(pal[2], 230))
    elif elem == "dragon":
        for i in range(5):
            x = 26 + i * 19; hgt = 40 + 20 * ((i + frame) % 3)
            poly(d, [(x - 10, 108), (x - 4, 108 - hgt * 0.6), (x + (frame - 1) * 5, 108 - hgt), (x + 6, 108 - hgt * 0.5), (x + 10, 108)], rgba(pal[1]))
            poly(d, [(x - 5, 108), (x, 108 - hgt * 0.7), (x + 5, 108)], rgba(pal[2]))
        for i in range(5): disc(d, 24 + (i * 27 + frame * 13) % 80, 70 - (i * 19 + frame * 16) % 50, 3, rgba(pal[3]))
    else:  # normal
        for i in range(6):
            a = i * 1.047 + frame * 0.3; rr = 22 + 10 * t
            x, y = 64 + math.cos(a) * rr, 72 + math.sin(a) * rr * 0.7
            pts = []
            for j in range(10):
                aa = j * math.pi / 5; r2 = 9 if j % 2 == 0 else 4; pts.append((x + math.cos(aa) * r2, y + math.sin(aa) * r2))
            poly(d, pts, rgba(pal[3], 235))
        disc(d, 64, 72, 10 + frame * 3, rgba(pal[2], 150))
    im = glow(im, pal[1], r=3, strength=0.6)
    return finish(im)

# ================================================================ ÍCONES DE STATUS (flutuam sobre o Pokémon)
ST_PAL = {"burn": "fire", "poison": "poison", "sleep": "psychic", "paralyze": "electric", "freeze": "ice"}
def status_icon(kind, frame):
    pal = PAL[ST_PAL[kind]]
    im = layer(); d = ImageDraw.Draw(im)
    wob = frame * 2.1
    if kind == "burn":
        for i in range(3):
            x = 40 + i * 24; hgt = 40 + 18 * ((i + frame) % 3)
            poly(d, [(x - 10, 100), (x - 5, 100 - hgt * .55), (x + (frame - 1) * 4, 100 - hgt), (x + 6, 100 - hgt * .5), (x + 10, 100)], rgba(pal[1]))
            poly(d, [(x - 5, 100), (x, 100 - hgt * .7), (x + 5, 100)], rgba(pal[2]))
    elif kind == "poison":
        for i in range(4):
            bx = 34 + i * 20; by = 96 - ((i * 19 + frame * 22) % 62); r = 7 + (i % 2) * 3
            disc(d, bx, by, r, rgba(pal[1], 220)); disc(d, bx - r * .3, by - r * .3, r * .3, rgba(pal[3]))
        disc(d, 64, 40, 12, rgba(pal[3], 235)); disc(d, 59, 38, 3.2, rgba(pal[0])); disc(d, 69, 38, 3.2, rgba(pal[0])); poly(d, [(60, 48), (68, 48), (64, 53)], rgba(pal[0]))
    elif kind == "sleep":
        for i in range(3):
            x = 44 + i * 20 + frame * 3; y = 90 - i * 22 - frame * 4; sz = 10 + i * 4
            line(d, [(x - sz, y - sz), (x + sz, y - sz), (x - sz, y + sz), (x + sz, y + sz)], 4, rgba(pal[3], 235 - i * 30))
    elif kind == "paralyze":
        rnd = random.Random(frame * 11 + 5)
        for b in range(3):
            x, y = 34 + b * 30, 100; pts = [(x, y)]
            for i in range(4):
                x += rnd.uniform(-10, 10); y -= 17; pts.append((x, y))
            line(d, pts, 5, rgba(pal[1])); line(d, pts, 2, rgba(pal[3]))
    else:  # freeze
        poly(d, [(30, 100), (38, 40), (64, 24), (90, 40), (98, 100)], rgba(pal[1], 150)); poly(d, [(40, 96), (46, 48), (64, 34), (82, 48), (88, 96)], rgba(pal[2], 120))
        line(d, [(48, 44), (46, 80)], 3, rgba(pal[3], 230)); disc(d, 78 + frame * 2, 52, 3.2, rgba(pal[3]))
    im = glow(im, pal[1], r=4, strength=0.9)
    return finish(im)

if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    for e in PAL:
        sheet([direct(e, i) for i in range(3)], "fx_%s_direct_3x1.png" % e)
        sheet([area(e, i) for i in range(3)], "fx_%s_area_3x1.png" % e)
        sheet([tile(e, i) for i in range(3)], "fxt_%s_3x1.png" % e)
    for k in ST_PAL:
        sheet([status_icon(k, i) for i in range(3)], "st_%s_3x1.png" % k)
    print("ok:", len(PAL) * 3 + len(ST_PAL), "folhas em", os.path.abspath(OUT))
