# Asset-manifest

| Fil | PDF-side | Indhold | Billed-/diagram-beskrivelse |
|---|---:|---|---|
| `suploader_funktionalitet_slet.mmd` | - | TransaktionsType=Slet | Slet anvender CPR og udtræk-id som samlet nøgle; matchende forløb, underliggende hændelser og CAVE slettes, og PERSON slettes kun hvis ingen forløb resterer. |
| `suploader_funktionalitet_slet_specielt.mmd` | - | TransaktionsType=SletSpecielt | SletSpecielt afgrænser sletning til forløbs-id’er i XML’en; CAVE slettes på CPR+udtræk-id, og medsendte CAVE kan indsættes bagefter. PERSON XML opdateres. |
| `suploader_funktionalitet_opdater.mmd` | - | TransaktionsType=Opdater | Opdater erstatter hele datasættet for kombinationen CPR+udtræk-id: eksisterende forløb/CAVE slettes og alle XML-data indsættes igen. |
| `suploader_funktionalitet_opdater_specielt.mmd` | - | TransaktionsType=OpdaterSpecielt | OpdaterSpecielt opdaterer kun de forløb, der er nævnt i XML’en, men CAVE håndteres på CPR+udtræk-id; derefter indsættes både opdaterede og nye forløb/CAVE. |
