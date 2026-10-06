# SUP-modelatlas

Åbn [index.html](index.html) i en browser. Atlaset fungerer offline uden installation, server eller eksterne biblioteker. Søgning, indhold, kildesider og navigation er indlejret i HTML-filen. Del hele mappen for også at bevare eksportfilerne; direkte links til original Markdown kræver repositoryets mappestruktur.

## Reference og kliniske kilder

- Struktur: alle 22 XSD-filer i `../sup_documentation/Schema`. Skemaerne angiver namespace `http://www.medcom.dk/SUP_30`.
- Klinisk dokumentation: alle Markdown-dokumenter i `../background_information`, med kildesideankre. Versionsmærkede domænemodeller bruges til direkte kliniske koblinger. XSD-annotationer vises særskilt.
- Dokumentation fra ældre versioner udgør ikke bevis for aktuelle leverancekrav. Faglig kontrol af koblinger og versionsforskelle er nødvendig før endelig mapping.

## Opgørelse

Der findes **154 forskellige globale elementnavne**, fordelt på **167 globale deklarationer**. Otte globale navne findes som flere servicevarianter. Derudover findes fire lokale elementdeklarationer: i alt **171 elementdeklarationer** og **155 forskellige elementnavne**. Der er **357 elementreferencer**, **187 attributdeklarationer** og **117 forskellige attributnavne**. Antal navne er ikke et antal kliniske begreber.

Genbrug tælles som direkte `ref`-forekomster. Hver fil læses én gang; includes skaber ikke nye optællinger. Referencer opløses i den pågældende fils include-kontekst og fortrinsvis i egen fil. Ens globale navne i forskellige services overskriver ikke hinanden. Lokale deklarationer identificeres ved fil og syntaktisk sti. Derfor er CSV og JSON egnede som sporbare inventarlister til en senere mapping.

## Indhold

- Overblik og indgange til centrale kliniske objekter.
- Søgning og filtre for elementer, attributter, datatyper og grupper.
- Underfelter, alternative grupper, lokal kardinalitet, obligatoriske attributter, datatypebegrænsninger og original XSD.
- Direkte anvendelser, servicevarianter og ens navne i forskellige kontekster.
- Kliniske kildeafsnit og søgning i alle baggrundsdokumenters kildesider.
- Dokumentationshuller, versionsforbehold og tekniske kontrolfund.
- Arbejdsramme for transition til FHIR; ingen endelig ressource- eller profilmapping.

Direkte klinisk kilde eller XSD-annotation er knyttet til 86 af 171 elementdeklarationer og 138 af 187 attributdeklarationer. Resterende deklarationer er med i atlaset og markeret som uden direkte definition. Referencer følges til deres definition; fælles attributgrupper åbnes via gruppelinks. Komplet struktur er dermed dækket, mens klinisk fortolkning stadig har dokumentationshuller. Automatisk kildekobling er ikke klinisk validering.

`atlas.json` indeholder alle deklarationer, referencer, kliniske kildekoblinger, kildesider og SHA-256-manifester. `elements.csv` og `attributes.csv` indeholder deklarationer uden referencesteder; referencer findes i JSON. `summary.json` indeholder optællinger.

## Genskab og kontroller

Kør `python scripts/build_sup_atlas.py` fra repositoryroden med Python 3.9 eller nyere. Kun standardbiblioteket anvendes. Output er deterministisk for samme input. Generatoren kontrollerer deklarations-ID'er, ejerreferencer og referenceoptælling.

`scripts/verify_sup_atlas.cjs` foretager browserkontrol med den bundtede Playwright-runtime: alle deklarationssider, faner, søgning og skærmbilleder ved desktop- og mobilbredde. Runtime-stien kan tilpasses på andre maskiner.
