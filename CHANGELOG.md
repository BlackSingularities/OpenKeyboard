# Zmiany

## 1.6.9

- Dopasowane kolorystycznie tła ikon aplikacji i spójne zasoby graficzne.

## 1.6.7

- Nowa strona aplikacji z opisem funkcji.
- Jednolita informacja o autorze (Adam Rędzikowski · redzikowski.com) i przycisk „Postaw kawę”.
- Usunięte odnośniki do repozytorium z interfejsu.

## 1.6.1
- **Ustawienia bez martwych opcji**: opcje, które w danej chwili nic nie zmieniają, są przygaszone i nieaktywne, z podpisem, co je włącza – np. rodzaj i głośność dźwięku przy wyłączonym dźwięku, siła wibracji bez wibracji, autokorekta i podpowiedzi emoji bez podpowiedzi, ślad gestu bez pisania gestem, pojemność schowka bez historii, szerokości trybu jednej ręki i klawiatury pływającej, przerwa klawiatury dzielonej, klawisz i gest zmiany języka, gdy nie ma czego przełączać, opcje nauki przy „Zawsze incognito”, przytrzymanie przecinka bez klawisza przecinka.
- Wybór rodzaju dźwięku nie włącza już dźwięku sam z siebie.

## 1.6.0
- **Wiele języków naraz bez przełączania**: język, w którym piszesz, rozpoznaje się sam – z ostatnich słów, z tekstu, który już jest w polu, a w nowym polu z ostatniego języka w danej aplikacji. Od niego zależą podpowiedzi, cudzysłowy, daty, przelicznik jednostek i dyktowanie (na Androidzie 14+ usługa mowy sama przełącza się między włączonymi językami).
- **Jeden układ dla języków o tym samym alfabecie**: pod przytrzymaniem są znaki wszystkich włączonych języków (np. ą i ä na „a”), klawisz i gest zmiany języka przełączają już tylko między alfabetami (łacinka ↔ cyrylica), a spacja pokazuje języki działające razem.
- **Więcej podpowiedzi na pasku**: szerokość wg słowa zamiast sztywnych trzech pól – widać kilka słów naraz, reszta po przewinięciu w bok (wygaszona krawędź pokazuje, że jest ich więcej).
- **Dłuższy kontekst**: klawiatura uczy się trójek słów, a nie tylko par, i korzysta z tekstu w polu – imiona i nazwy, które już padły, są podpowiadane od pierwszych liter, a słowa, które padły po tych samych słowach, są przewidywane.
- **Słownik działa także tam, gdzie aplikacja go wyłącza** (np. Duolingo) – bez przeskakiwania. Pola udające hasło („widoczne hasło”) mają podpowiedzi, ale bez nauki słów. Dawne zachowanie: opcja „Bez podpowiedzi, gdy aplikacja prosi”.
- **„Zapomnij” po przytrzymaniu podpowiedzi** – pasek pyta o potwierdzenie; słowo wyuczone znika ze słownika, słowo ze słownika ogólnego trafia do zablokowanych. W słowniku użytkownika można poprawić pisownię słowa.
- **TalkBack**: klawisze, podpowiedzi i przyciski paska mają opisy i działają dwukrotnym dotknięciem.
- **Pierwsze uruchomienie**: języki pisania wg języków systemu (i angielski) zamiast zawsze polskiego.

## 1.5.2
- **Mądrzejsza autokorekta**: poprawia większy bełkot w dłuższych słowach (więcej literówek i pominiętych liter) i rozdziela sklejone słowa także po jednoliterowym słowie i z literą zamiast spacji („wbtej” → „w tej”, „ibrozumiec” → „i rozumieć”).

## 1.5.1
- **Podpowiedź następnego słowa nie znika po pierwszej literze** – pasujące przewidziane słowa zostają na początku paska.
- **Poprawne polskie cudzysłowy**: zamykający „”” wstawia się także zaraz po słowie (wcześniej zostawał prosty `"`); niemiecki, czeski, słowacki i litewski zamykają „“”.
- **Dowolna liczba podpowiedzi**: usunięta opcja liczby podpowiedzi – pasek pokazuje tyle słów, ile znajdzie silnik (do 50), najlepsze na środku, kolejne po przewinięciu.
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

## 1.3.1
- Przezroczysty znak aplikacji w ikonie, ekranie startowym i pozostałych miejscach z logo.
- Zachowana przezroczystość w starszych i adaptacyjnych ikonach Androida; odświeżone ikony motywowe.

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
