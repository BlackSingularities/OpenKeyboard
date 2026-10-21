# OpenKeyboard

**Otwarta, wielojęzyczna klawiatura dla Androida** – domyślnie czarna i minimalistyczna, z inteligentnymi podpowiedziami, autokorektą i mnóstwem narzędzi. Bez reklam i śledzenia: wszystko, czego się uczy, zostaje na telefonie.

[![Build & Release](../../actions/workflows/android.yml/badge.svg)](../../actions/workflows/android.yml)

## Pobieranie

Najnowszy plik jest zawsze w **[Releases → Latest](../../releases/latest)**. Każdy merge do `main` buduje i publikuje nową wersję, a aplikacja sama proponuje aktualizację i instaluje ją jednym dotknięciem.

1. Pobierz `OpenKeyboard-x.y.z.apk` i zainstaluj (Android 10 lub nowszy).
2. Otwórz aplikację i przejdź przez „Pierwsze kroki”: włącz klawiaturę i ustaw ją jako domyślną.

## Funkcje

### Pisanie
| Funkcja | Opis |
|---|---|
| **Wiele języków naraz** | Podpowiedzi i autokorekta ze wszystkich włączonych języków jednocześnie – bez przełączania. 30 języków (łacinka, cyrylica, greka), układy QWERTY, QWERTZ, AZERTY, Colemak, Dvorak, nordyckie, turecki. |
| **Duży słownik** | 200 tys. form polskich i 130 tys. słów angielskich w aplikacji (generator: `tools/build_dicts.py`), pobierane języki do 150 tys. słów. |
| **Bez polskich znaków** | „zolw” → „żółw”, „dont” → „don't”. Znaki diakrytyczne pod przytrzymaniem i jako podpowiedź w rogu klawisza. |
| **Autokorekta** | Ważona odległość edycji: sąsiednie klawisze, przestawione i podwojone litery. Cztery poziomy siły, Backspace cofa poprawkę. |
| **Nauka** | Nowe słowa i pary słów (przewidywanie następnego słowa) zapamiętywane lokalnie. Przytrzymanie podpowiedzi blokuje słowo. |
| **Inteligentna interpunkcja** | Kropka po spacji, wielka litera na początku zdania, „,.!?” przyklejane do słowa. |
| **Pisanie gestem** | Przeciągnij palcem po literach słowa – dopasowanie kształtu ścieżki do słownika, ze śladem palca i kontekstem. |
| **Wykrywanie języka** | Słowa języka, w którym właśnie piszesz, dostają pierwszeństwo. Cofnięta poprawka już nie wraca. |
| **Narzędzia tekstu** | Wielkość liter, 13 ozdobnych stylów Unicode (𝐁𝐨𝐥𝐝, 𝓢𝓬𝓻𝓲𝓹𝓽, Ⓒⓘⓡⓒⓛⓔ…) w zwykłych polach tekstu, daty i godziny w języku pisania, skróty, schowek, licznik słów. |
| **Kalkulator w pasku** | `12*7+3` → podpowiedź `= 87`. |
| **Skróty tekstowe** | `pzdr` + spacja → „Pozdrawiam”. |
| **Emoji w podpowiedziach** | „kot” → 🐱, po polsku i angielsku. |

### Narzędzia w klawiaturze
- **Emoji** – pełna lista Unicode z odcieniami skóry, ostatnio używane, wyszukiwarka (PL/EN), kaomoji i symbole.
- **Schowek** – historia z przypinaniem, świeżo skopiowany tekst w pasku podpowiedzi, dane poufne pomijane.
- **Kursor i zaznaczanie** – strzałki z powtarzaniem, zaznaczanie, kopiuj/wytnij/wklej, cofnij/ponów.
- **Gesty** – przesuwanie palca po spacji (kursor), przytrzymana spacja w bok (zmiana języka), przytrzymany Backspace i przesunięcie w lewo (usuwanie słów), w dół (schowanie).
- **Symbole na literach jak w SwiftKey** (Q %, A @, S #…), klawisz emoji obok ?123 albo połączony z nim (przytrzymanie – emoji).
- **Dyktowanie** z podglądem na żywo, **panel symboli** (interpunkcja, matematyka, strzałki, waluty, kształty, grecki, kaomoji).
- **Tryb jednej ręki**, **klawiatura pływająca** (przesuwana nakładka), klawiatura numeryczna, szybkie ustawienia (przytrzymaj kafelek – wszystkie opcje pod palcem).
- **Incognito** automatycznie w polach haseł i w trybie prywatnym przeglądarek.
- **Sprawdzanie pisowni** dla całego systemu na tych samych słownikach.

### Wygląd i ustawienia
- 18 motywów (w tym płaski czarny bez pól i obramowań oraz gradientowe), 10 akcentów i dowolny kolor akcentu (także dla liter), motyw dzienny i nocny, klawisze pełne / z obwódką / płaskie / 3D.
- Rząd strzałek, 5 rodzajów dźwięku klawiszy, zmiana rozmiaru przeciąganiem, statystyki z mapą cieplną.
- Własny motyw: dowolne kolory, obraz w tle klawiatury, krycie klawiszy, 5 czcionek.
- Konfigurowalne gesty (spacja w bok / w górę / przytrzymanie, przeciągnięcie w dół, przytrzymanie Enter i przecinka), klawiatura dzielona w poziomie, 3 lub 5 podpowiedzi.
- Wysokość, odstępy, zaokrąglenie, wielkość znaków, siła wibracji, dźwięk, czas przytrzymania – z podglądem na żywo.
- Interfejs ustawień w stylu OpenViewPDF, po polsku i angielsku.
- Kopia zapasowa (ustawienia, słownik, skróty, przypięte wpisy schowka) do pliku JSON.
- Aktualizacje prosto z GitHub Releases w aplikacji.

## Źródła danych
- **[FrequencyWords](https://github.com/hermitdave/FrequencyWords)** (Hermit Dave) – 50 tys. najczęstszych słów na język, CC-BY-SA 4.0. Polski i angielski wbudowane, pozostałe pobierane w aplikacji.
- **[Unicode emoji-test.txt](https://unicode.org/emoji/)** – emoji, nazwy, odcienie skóry.
- **[MyMemory](https://mymemory.translated.net/)** – tłumaczenia (tylko na żądanie).

## Architektura

```
app/src/main/java/com/blacksingularities/openkeyboard/
  ime/        usługa klawiatury (InputMethodService + Compose), logika wpisywania, klawisze, panele
  keyboard/   układy klawiszy i motywy
  dict/       słowniki, silnik podpowiedzi i autokorekty, model użytkownika
  emoji/      dane emoji i słowa kluczowe
  features/   schowek, kalkulator, tłumacz
  spell/      systemowe sprawdzanie pisowni
  ui/         aplikacja ustawień (styl OpenViewPDF)
  update/     aktualizacje z GitHub Releases
```

- **Kotlin · Jetpack Compose · Material 3**, Android 10+ (API 29).
- Budowanie: `./gradlew :app:assembleRelease`, testy: `./gradlew testDebugUnitTest`.
- Podpis wydań: sekrety `SIGNING_KEYSTORE_BASE64`, `SIGNING_STORE_PASSWORD`, `SIGNING_KEY_ALIAS`, `SIGNING_KEY_PASSWORD` (ten sam klucz między wersjami – inaczej aktualizacja w aplikacji się nie zainstaluje).

## Licencja
MIT – zobacz [LICENSE](LICENSE). Projekt i wykonanie: [redzikowski.com](https://redzikowski.com).

---

<a href="https://buycoffee.to/adamredzikowski" target="_blank"><img src="https://buycoffee.to/static/img/share/share-button-dark--pl.png" style="width: 351px; height: 92px" width="351" height="92" alt="Postaw kawę dla adamredzikowski na buycoffee.to"></a>
