## 1.3.1 — Transparent application icon

- Use the transparent application mark in launcher, splash and existing branding surfaces.
- Preserve alpha in legacy and adaptive Android icons; refresh themed icon resources.

# Zmiany

## 1.5.1
- **Polskie jednoliterowe spójniki i przyimki** („i”, „w”, „z”, „a”, „o”, „u”) zostają małe – nie zmieniają się w angielskie „I”, gdy włączony jest też angielski.

## 1.5.0
- **Najlepsza podpowiedź na środku paska** (jak w SwiftKey), bez separatorów i kropki – wyróżniona tylko pogrubieniem.
- **Ikony nad klawiaturą wyśrodkowane.**
- **Backspace wraca do słowa**: po dojściu do słowa znów są jego podpowiedzi, a dopisane litery trafiają do tego słowa (nie zapisują się w słowniku jako nowe).
- **Inteligentny przecinek i kropka**: „słowo ,” → „słowo, ”; przecinek i kropka po słowie same dodają spację (po cyfrze nie – „3,5”), a spacja tuż po nich się nie dubluje.
- **Dwa słowa także z literówką** („jakdzirn” → „jak dzień”).
- **Przytrzymanie litery z górnego rzędu daje cyfrę**, nie symbol (symbol w lewym rogu, nadal w okienku).
- **Podpowiedzi następnego słowa zawsze** – także na początku zdania i bez wyuczonych par.
- **Niższy dolny rząd ze spacją.**

## 1.4.0
- **Shift w trakcie słowa**: pierwsze naciśnięcie robi wielką pierwszą literę („kot” → „Kot”), drugie – całe słowo wielkimi („KOT”, dalsze litery też wielkie do końca słowa), trzecie wraca do małych. Shift przy zaznaczonym tekście zmienia jego wielkość liter.
- **Shift trzymany jak na komputerze**: przytrzymaj Shift jednym palcem i pisz drugim – wielkie litery tylko do puszczenia.
- **Wielka litera na początku zdania także w podpowiedziach i autokorekcie** (z rozpoznawaniem skrótów „np.”, „itd.”, „m.in.”), poprawianie „HEllo” → „Hello”, angielskie dni, miesiące i „I'm”.
- **Przytrzymany Backspace od razu usuwa całe słowa** (bez przeciągania).
- **Dwa słowa z jednego**: pominięta spacja („jakdzien” → „jak dzień”) albo litera z dolnego rzędu trafiona zamiast spacji („dzieńndobry” → „dzień dobry”) – w podpowiedziach i autokorekcie.
- **Pasek podpowiedzi bez limitu**: tyle słów, ile pasuje, przewijane palcem, większe i nigdy nie ucinane „…”; najlepsze pierwsze.
- **Shift działa też na słowie tuż przed kursorem** (np. w polach bez podpowiedzi i po pisaniu gestem).
- **Cofnięcie autokorekty z paska** – po poprawce w pasku pojawia się „↶ wpisane słowo”.
- **Znak pociągnięciem** – pociągnij palcem w górę po klawiszu, aby wstawić znak z jego rogu.
- **Przelicznik jednostek w pasku**: km/mi, m/ft, cm/in, kg/lb, g/oz, l/gal, °C/°F, km/h/mph, kW/KM.
- **Zmienne w skrótach tekstowych**: {data}, {godzina}, {dzień}, {schowek}, {kursor}.
- **Domykanie nawiasów i cudzysłowów** (opcja).
- **Dyktowanie w ustawieniach naprawione**: stan mikrofonu odświeża się po udzieleniu zgody, po trwałej odmowie aplikacja otwiera ustawienia systemu, „Zezwól w aplikacji” z klawiatury pokazuje prawdziwe okno zgody. Nowa grupa „Dyktowanie” (stan, ustawienia rozpoznawania mowy), pełny kod języka dla usługi mowy (pl-PL) i czytelne komunikaty błędów.
- Nowe przekształcenia tekstu: bez ogonków, od tyłu, do góry nogami, porządek w odstępach.

## 1.3.0
- **4× większy słownik polski** (200 tys. form zamiast 50 tys.) i ponad 130 tys. słów angielskich, z poprawnymi skrótami (I'm, don't…). Pobierane języki mają teraz do 150 tys. słów.
- **Przytrzymany Backspace usuwa całe słowa** – po kilku literach przechodzi na słowa (można wyłączyć).
- **Symbole na literach jak w SwiftKey** – Q %, W ^, A @, S #, K (… pod przytrzymaniem i w rogu klawisza; cyfry wtedy w osobnym rzędzie albo jako drugi znak.
- **Klawisz emoji obok ?123** (między ?123 a przecinkiem) albo połączony z ?123: dotknięcie – symbole, przytrzymanie – emoji, z ikoną dzieloną po skosie.
- **Zmiana języka spacją** – przytrzymaj spację i przesuń w bok. Zwykłe przesunięcie spacji przesuwa kursor (także między wierszami). Przy kilku językach naraz spacja pokazuje „PL · EN”.
- **Szybkie ustawienia od nowa**: przytrzymaj kafelek, a pojawią się wszystkie opcje (motywy, akcenty, rozmiar, wibracja, dźwięk, język…) – wybierasz, przesuwając palec. Nowe kafelki: język, klawisz emoji, symbole na literach, litery w akcencie.
- **Dowolny kolor akcentu** (barwa, nasycenie, jasność) i opcja liter na klawiszach w kolorze akcentu.
- **Klawiatura przylega do dołu ekranu** – pod klawiszami tylko tyle miejsca, ile wymaga system (do wyboru: automatycznie, pełny pasek, brak).
- **Narzędzia tekstu zależne od pola**: ozdobne style tylko w zwykłych polach tekstu; wiele nowych wstawek – daty i godziny w języku pisania („20 października 2026”, dzień tygodnia, jutro, wczoraj, numer tygodnia, ISO, Unix), Twoje skróty i wpisy schowka, znaki, liczba losowa, UUID.
- **Panel symboli** z kategoriami: interpunkcja, matematyka, strzałki, waluty, kształty, grecki, kaomoji.
- **Pasek narzędzi** widać od razu po otwarciu klawiatury; nowe przyciski do wyboru w ustawieniach: wklej, cofnij, ponów, zaznacz wszystko, języki, rozmiar, incognito, symbole.
- **Klawiatura zależna od kontekstu**: w polach haseł rząd cyfr, w adresach i e-mailach bez emoji, z „/” lub „@” i znakami adresu pod przytrzymaniem.
- Usunięto moduł tłumaczenia (klawiatura nie łączy się już z usługą tłumaczeń).
- Poprawki: przyciski paska narzędzi, szybkich ustawień i kategorii emoji reagują od pierwszego dotknięcia; ręcznie otwarty pasek narzędzi chowa się, gdy zaczynasz pisać.

## 1.2.0
- **Płaska czarna klawiatura** – same znaki na czerni, bez pól i obramowań (także Enter i Shift; włączony Shift pokazuje kropka). Jednym dotknięciem w szybkich ustawieniach klawiatury.
- **18 motywów**, w tym gradientowe (Ocean, Zachód, Zorza, Mgławica, Żar) i jasne (Mięta, Niebo, Brzoskwinia, Płaski jasny). Motyw dzienny i nocny przełączany z trybem ciemnym systemu, gradient we własnym motywie.
- **Kształt klawiszy 3D** (z cieniem), rząd strzałek pod klawiszami, 5 rodzajów dźwięku klawiszy (systemowy, klik, miękki, maszyna do pisania, bańka).
- **Zmiana rozmiaru przeciąganiem** – w szybkich ustawieniach „Rozmiar”: przeciągnij pasek, a klawisze rosną lub maleją na żywo.
- **Statystyki**: tempo pisania (słowa na minutę), mapa cieplna klawiszy, najczęstsze słowa.
- **Czytelniejsze ustawienia**: każda opcja ma opis, co robi, każda sekcja – krótkie wprowadzenie, a pasek u dołu przenosi od razu do wybranej sekcji.
- Notatki o nowej wersji bez znaczników Markdown.

## 1.1.0
- **Pisanie gestem** – przeciągnij palcem po literach słowa, ze śladem palca, kontekstem poprzedniego słowa i polskimi znakami bez przytrzymania („zolw” → „żółw”). Kolejny gest sam dodaje spację.
- **Inteligencja**: wykrywanie języka, w którym właśnie piszesz (pierwszeństwo jego słów), nauka z cofniętych poprawek (Backspace po autokorekcie zapamiętuje Twoje słowo), poprawianie wielkości liter („i” → „I”).
- **Narzędzia tekstu**: wielkość liter (zaznaczenia lub słowa), 13 ozdobnych stylów pisania (𝐁𝐨𝐥𝐝, 𝘐𝘵𝘢𝘭𝘪𝘤, 𝓢𝓬𝓻𝓲𝓹𝓽, 𝔻𝕠𝕦𝕓𝕝𝕖, Ⓒⓘⓡⓒⓛⓔ, ꜱᴍᴀʟʟ…), data i godzina, licznik znaków i słów, czyszczenie pola.
- **Własny motyw**: dowolne kolory (barwa, nasycenie, jasność, gotowe próbki), obraz w tle klawiatury z przyciemnieniem, krycie klawiszy, 5 czcionek.
- **Gesty do konfiguracji**: spacja w bok / w górę / przytrzymanie, przeciągnięcie w dół, przytrzymanie Enter i przecinka – 14 czynności do wyboru.
- **Klawiatura dzielona** w poziomie (albo zawsze) z regulowaną przerwą, 3 lub 5 podpowiedzi w pasku, typograficzne cudzysłowy, spacja po „, ! ?”.
- Poprawka: zamknięcie klawiatury przy przełączaniu na inną mogło zakończyć się błędem aplikacji.

## 1.0.0
Pierwsze wydanie OpenKeyboard.

- Klawiatura wielojęzyczna: 30 języków, podpowiedzi i autokorekta ze wszystkich włączonych naraz, pisanie bez znaków diakrytycznych.
- Przewidywanie następnego słowa i nauka słów (lokalnie), skróty tekstowe, kalkulator i emoji w pasku podpowiedzi.
- Panele: emoji z wyszukiwarką i odcieniami skóry, historia schowka, kursor i zaznaczanie, dyktowanie, tłumacz, szybkie ustawienia.
- Gesty: kursor spacją, usuwanie słów z Backspace, przeciągnięcie w dół chowa klawiaturę.
- Tryb jednej ręki, klawiatura pływająca, 7 motywów i 10 akcentów.
- Systemowe sprawdzanie pisowni, kopia zapasowa, aktualizacje z GitHuba w aplikacji.
