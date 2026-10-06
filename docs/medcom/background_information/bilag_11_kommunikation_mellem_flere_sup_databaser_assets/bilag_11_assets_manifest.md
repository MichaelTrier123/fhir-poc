# Asset-manifest - Bilag 11: Kommunikation mellem flere SUP-databaser

Kilde: `bilag_11_kommunikation_mellem_-flere_sup-databaser_udk.pdf`

## Originale billedressourcer
- `bilag_11_scenario_browser_b.png` - PDF-side 4. Scenario 3.2.1: En bruger i Amt A anvender Browser-B i Amt B for at tilgå SUP-B. Data vises via den fremmede webapplikation, og brugeren må håndtere journalisering i eget miljø.
- `bilag_11_scenario_browser_a_til_sup_b.png` - PDF-side 5. Scenario 3.2.2, simpel variant: Brugeren i Amt A anvender Browser-A, som finder og kalder SUP-B i Amt B. Browser-A er brugerens fælles adgang til den fremmede database.
- `bilag_11_scenario_browser_a_med_journalisering.png` - PDF-side 6. Udvidet variant: Browser-A tilgår SUP-B og gemmer relevante viste oplysninger i en lokal journaliseringskomponent, så de senere kan genfindes.
- `bilag_11_scenario_sup_a_med_journaliseringsdata.png` - PDF-side 7. Mest ambitiøse variant: Browser-A tilgår SUP-B, og journaliseringsdata lagres i SUP-A. Egne og fremmede data skal holdes adskilt, så andre opslag i SUP-A fortsat kun returnerer de oprindelige lokale data.

## Mermaid-kilder
- `bilag_11_scenario_browser_b.mmd` - Mermaid-rekonstruktion/visualisering: scenario browser b.
- `bilag_11_scenario_browser_a_til_sup_b.mmd` - Mermaid-rekonstruktion/visualisering: scenario browser a til sup b.
- `bilag_11_scenario_browser_a_med_journalisering.mmd` - Mermaid-rekonstruktion/visualisering: scenario browser a med journalisering.
- `bilag_11_scenario_sup_a_med_journaliseringsdata.mmd` - Mermaid-rekonstruktion/visualisering: scenario sup a med journaliseringsdata.
