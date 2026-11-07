# Polityka prywatności OpenKeyboard

Obowiązuje od wersji 1.7.0. Kontakt: kontakt@redzikowski.com

## Bez zbierania danych
OpenKeyboard nie ma reklam, analityki ani kont. Nie wysyła tego, co piszesz, nikomu: ani autorowi, ani firmom trzecim.

## Co zostaje w telefonie
Wyuczone słowa i pary słów, skróty, historia schowka, statystyki pisania, raporty awarii i ustawienia. Te dane są wyłączone z kopii zapasowej Androida w chmurze i z przenoszenia na inne urządzenie (`dataExtractionRules`). Przeniesiesz je tylko własnym plikiem kopii z aplikacji.

## Połączenia z internetem
Są tylko dwa:
- pobranie słownika języka, gdy o nie poprosisz (`raw.githubusercontent.com`, listy FrequencyWords),
- sprawdzenie i pobranie aktualizacji (`redzikowski.com`).

Wysyłana jest wyłącznie wersja aplikacji (nagłówek `User-Agent`). Opcja **Całkowicie offline** w ustawieniach wyłącza oba połączenia.

## Uprawnienia
| Uprawnienie | Do czego |
|---|---|
| `RECORD_AUDIO` | Dyktowanie. Dźwięk rozpoznaje usługa mowy zainstalowana w telefonie. |
| `INTERNET` | Słowniki i aktualizacje, jak wyżej. |
| `REQUEST_INSTALL_PACKAGES` | Instalacja aktualizacji OpenKeyboard. |
| `POST_NOTIFICATIONS` | Informacja o zainstalowanej aktualizacji. |
| `VIBRATE` | Reakcja klawiszy. |

## Pola haseł
W polach haseł i w trybie prywatnym przeglądarek klawiatura niczego nie zapamiętuje. Dane poufne nie trafiają do historii schowka.

## Raporty awarii
Raporty są zapisywane tylko w telefonie (najwyżej 5 ostatnich). Wysyłasz je sam, wybierając aplikację, i dopiero wtedy ktokolwiek je zobaczy.
