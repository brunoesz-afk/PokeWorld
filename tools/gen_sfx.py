#!/usr/bin/env python3
"""Gera os efeitos sonoros curtos do PokeWorld (WAV 22,05 kHz mono) em app/src/main/res/raw/sfx_*.wav.
Sintetizados do zero (sem direitos autorais); troque por seus próprios .wav com o mesmo nome se quiser."""
import numpy as np, os, wave
SR = 22050
OUT = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "res", "raw")
rng = np.random.default_rng(7)

def t(d): return np.linspace(0, d, int(SR * d), endpoint=False)
def env(n, a=0.01, r=0.1, curve=2.0):
    e = np.ones(n); na = max(1, int(SR * a)); nr = max(1, int(SR * r))
    e[:na] = np.linspace(0, 1, na); e[-nr:] *= np.linspace(1, 0, nr) ** curve
    return e
def noise(d): return rng.uniform(-1, 1, int(SR * d))
def lp(x, k):  # média móvel = passa-baixa simples
    k = max(1, int(k)); return np.convolve(x, np.ones(k) / k, mode="same")
def sweep(d, f0, f1, wave="sin"):
    tt = t(d); f = np.linspace(f0, f1, len(tt)); ph = 2 * np.pi * np.cumsum(f) / SR
    return np.sign(np.sin(ph)) * 0.6 if wave == "sq" else np.sin(ph)
def tone(d, f): return np.sin(2 * np.pi * f * t(d))
def norm(x, g=0.8): return x / (np.max(np.abs(x)) + 1e-9) * g
def save(name, x):
    x = np.clip(x, -1, 1); pcm = (x * 32767).astype(np.int16)
    with wave.open(os.path.join(OUT, "sfx_%s.wav" % name), "wb") as w:
        w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR); w.writeframes(pcm.tobytes())

def fire():     d = .55; n = noise(d); return norm(lp(n, 6) * env(len(n), .02, .4) + .4 * sweep(d, 220, 90) * env(len(n), .01, .4))
def water():    d = .5;  s = sweep(d, 500, 1100); return norm((s * (1 + .5 * np.sin(2 * np.pi * 22 * t(d)))) * env(len(s), .02, .35) * .7 + lp(noise(d), 3) * .25 * env(len(s), .05, .3))
def electric(): d = .45; s = sweep(d, 1800, 300, "sq"); return norm((s * (np.sign(np.sin(2 * np.pi * 60 * t(d))) * .5 + .5) + noise(d) * .3) * env(len(s), .005, .3))
def plant():    d = .4;  n = noise(d); return norm(lp(n, 2) * (np.sin(2 * np.pi * 14 * t(d)) * .4 + .6) * env(len(n), .03, .3) + .2 * sweep(d, 700, 350) * env(len(n), .02, .3))
def ice():      d = .5;  s = sum(np.sin(2 * np.pi * f * t(d)) for f in (1760, 2217, 2637)) / 3; return norm(s * env(len(s), .003, .45, 3) + .15 * noise(d) * env(len(s), .001, .1))
def rock():     d = .5;  n = noise(d); return norm(lp(n, 12) * env(len(n), .003, .35, 3) + .7 * sweep(d, 110, 45) * env(len(n), .003, .4, 3))
def poison():   d = .55; s = sweep(d, 260, 120); return norm((s * (1 + .8 * np.sin(2 * np.pi * 9 * t(d)))) * env(len(s), .05, .4) * .6 + lp(noise(d), 5) * .3 * env(len(s), .1, .3))
def psychic():  d = .6;  s = np.sin(2 * np.pi * (600 + 200 * np.sin(2 * np.pi * 6 * t(d))) * t(d)) + .5 * np.sin(2 * np.pi * 900 * t(d)); return norm(s * env(len(s), .1, .4))
def dragon():   d = .7;  s = sweep(d, 140, 55, "sq") * .6 + lp(noise(d), 8) * .7; return norm(s * env(len(s), .02, .5))
def normal():   d = .22; n = noise(d); return norm((lp(n, 4) * .7 + .5 * sweep(d, 300, 120)) * env(len(n), .002, .18, 3))
def hit():      d = .14; n = noise(d); return norm(lp(n, 5) * env(len(n), .001, .12, 3) + .5 * sweep(d, 200, 80) * env(len(n), .001, .12))
def crit():     d = .3;  return norm(hit() [:int(SR * .14)].tolist() and np.concatenate([hit(), .8 * sweep(.16, 900, 1500) * env(int(SR * .16), .002, .14)]))
def faint():    d = .7;  s = sweep(d, 420, 90); return norm(s * env(len(s), .01, .5))
def levelup():  return norm(np.concatenate([tone(.12, f) * env(int(SR * .12), .005, .08) for f in (523, 659, 784, 1047)]))
def capture():  return norm(np.concatenate([tone(.08, 700) * env(int(SR * .08), .002, .06), np.zeros(int(SR * .1)), tone(.08, 700) * env(int(SR * .08), .002, .06), np.zeros(int(SR * .1)), tone(.25, 1047) * env(int(SR * .25), .005, .2)]))
def buy():      return norm(np.concatenate([tone(.07, 1319) * env(int(SR * .07), .002, .05), tone(.18, 1760) * env(int(SR * .18), .002, .15)]))
def evolve():   d = 1.0; s = sweep(d, 300, 1500) * env(int(SR * d), .05, .5) * .7 + .3 * sweep(d, 450, 2250) * env(int(SR * d), .05, .5); return norm(s)
def click():    d = .05; return norm(tone(d, 1200) * env(int(SR * d), .001, .04))
def status():   d = .3;  return norm(sweep(d, 700, 350, "sq") * env(int(SR * d), .005, .25) * .7)
def heal():     return norm(np.concatenate([tone(.1, f) * env(int(SR * .1), .004, .08) for f in (784, 988, 1175)]))
def boost():    return norm(np.concatenate([tone(.09, f) * env(int(SR * .09), .004, .07) for f in (440, 554, 659, 880, 1109)]))

for fn in (fire, water, electric, plant, ice, rock, poison, psychic, dragon, normal, hit, faint, levelup, capture, buy, evolve, click, status, heal, boost):
    save(fn.__name__, fn())
# crit: pancada + brilho
save("crit", np.concatenate([hit(), .8 * sweep(.16, 900, 1500) * env(int(SR * .16), .002, .14)]) / 1.0)
print("ok: sons em", os.path.abspath(OUT))
