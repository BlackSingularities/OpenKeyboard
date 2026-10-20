"""
Buduje wbudowane słowniki (app/src/main/assets/dict/*.dict) z list częstości FrequencyWords
(github.com/hermitdave/FrequencyWords, CC-BY-SA 4.0, napisy OpenSubtitles 2018).

Użycie:  python tools/build_dicts.py <katalog z pl_full.txt i en_full.txt>
Plik wyjściowy: gzip, w każdej linii „słowo<TAB>liczba”, od najczęstszych.
"""
import gzip
import os
import sys

OUT = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "assets", "dict")

# Ile słów i od jakiej częstości. Polski jest mocno fleksyjny – potrzebuje dużo więcej form.
LIMITS = {"pl": (200_000, 20), "en": (130_000, 12)}

SINGLE = {"pl": set("a i o u w z"), "en": set("a i")}
EN_ACCENTS = set("éèàçñöüäáíóúâêîôû")
EN_CAPS = (
    "monday tuesday wednesday thursday friday saturday sunday january february march april june july "
    "august september october november december english american british french german spanish italian "
    "polish russian chinese japanese european christmas easter god jesus london paris america england "
    "europe canada australia germany france italy spain poland russia china japan mexico africa asia "
    "india ireland scotland london york texas california florida google facebook instagram youtube "
    "iphone android microsoft amazon netflix twitter whatsapp tv ok"
).split()
# Listy z napisów nie mają apostrofów – najczęstsze skróty dodajemy ręcznie (z umowną częstością),
# a ich formy bez apostrofu usuwamy, żeby „dont” poprawiało się na „don't”.
EN_CONTRACTIONS = {
    "I'm": 2600000, "don't": 2500000, "it's": 2400000, "that's": 1100000, "you're": 900000, "can't": 800000,
    "I'll": 700000, "didn't": 600000, "I've": 500000, "he's": 500000, "what's": 450000, "won't": 400000,
    "I'd": 400000, "let's": 400000, "doesn't": 350000, "we're": 350000, "there's": 350000, "isn't": 300000,
    "they're": 300000, "she's": 300000, "wasn't": 200000, "haven't": 200000, "you'll": 200000, "aren't": 150000,
    "couldn't": 150000, "wouldn't": 150000, "you've": 150000, "we'll": 150000, "who's": 80000, "we've": 80000,
    "you'd": 80000, "shouldn't": 60000, "they'll": 60000, "hasn't": 50000, "ain't": 50000, "weren't": 40000,
    "he'll": 40000, "that'll": 30000, "she'll": 20000, "y'all": 10000, "they've": 30000, "where's": 30000,
    "how's": 20000, "here's": 40000, "it'll": 30000, "mustn't": 5000, "needn't": 3000,
}
EN_DROP = {
    "im", "dont", "didnt", "doesnt", "isnt", "wasnt", "havent", "arent", "couldnt", "wouldnt", "shouldnt",
    "hasnt", "werent", "youre", "theyre", "ive", "youve", "weve", "youll", "theyll", "thats", "whats",
    "theres", "ain", "didn", "doesn", "isn", "wasn", "haven", "aren", "couldn", "wouldn", "shouldn", "hasn",
    "weren", "don", "ll", "ve", "re",
}
EN_FIXED = {"i": "I", "i'm": "I'm", "i'll": "I'll", "i've": "I've", "i'd": "I'd", "tv": "TV", "ok": "OK", "iphone": "iPhone"}


def is_word(lang, w):
    if not w or len(w) > 40 or not w[0].isalpha():
        return False
    if len(w) == 1 and w not in SINGLE[lang]:
        return False
    if not all(c.isalpha() or c in "'-" for c in w):
        return False
    if w.endswith("-") or "--" in w or "''" in w:
        return False
    if lang == "en" and not all(c.isascii() or c in EN_ACCENTS for c in w):
        return False
    return True


def build(lang, src):
    limit, min_count = LIMITS[lang]
    out = []
    seen = set()
    with open(src, encoding="utf-8", errors="replace") as f:
        for line in f:
            w, _, c = line.rstrip("\n").rpartition(" ")
            if not c.isdigit():
                continue
            count = int(c)
            if count < min_count:
                break
            if "�" in w or not is_word(lang, w):
                continue
            if lang == "en":
                if w in EN_DROP:
                    continue
                if w in EN_FIXED:
                    w = EN_FIXED[w]
                elif w in EN_CAPS:
                    w = w[0].upper() + w[1:]
            if w in seen:
                continue
            seen.add(w)
            out.append((w, count))
            if len(out) >= limit:
                break
    if lang == "en":
        out = [(w, c) for w, c in out if w not in EN_CONTRACTIONS] + list(EN_CONTRACTIONS.items())
        out.sort(key=lambda x: -x[1])
    path = os.path.join(OUT, f"{lang}.dict")
    with gzip.open(path, "wt", encoding="utf-8", compresslevel=9) as g:
        for w, c in out:
            g.write(f"{w}\t{c}\n")
    print(lang, len(out), "słów, ostatnie:", out[-1], os.path.getsize(path) // 1024, "KB")


if __name__ == "__main__":
    src = sys.argv[1]
    for lang in LIMITS:
        build(lang, os.path.join(src, f"{lang}_full.txt"))
