# Opgave 2b — SUP 3.0 sammenholdt med LPR2 og LPR3

**Arbejdsrapport, 6. oktober 2026.** Formålet er at forstå, hvad SUP faktisk siger, før vi designer en ny FHIR-standard. Denne rapport beskriver kildebegreber og gab; den er ikke en specifikation til LPR3-indberetning. Den læses sammen med [2a: LPR2/LPR3-modelanalysen](lpr2-lpr3-modelanalyse-2a.md) og den tidligere [SUP→FHIR-mapping](sup_til_fhir_lpr3_mappingrapport.md). Hvor de tidligere analyser kun byggede på XSD, gælder præciseringerne i denne rapport.

## 1. Hvad sammenlignes?

**Tre forskellige slags model:** LPR2 er især en kontaktcentreret indberetningsstruktur; LPR3 er en eksplicit model af forløbselement, kontakt, henvisning, markør, procedure, diagnose m.m.; SUP er en kommunikationsmodel for patientdata fra EPJ/PAS. SUP's egne begreber må derfor ikke sidestilles med LPR's alene på navnelighed. [MedCom beskriver selv SUP som en kommunikationsmetode](https://svn.medcom.dk/svn/releases/Standarder/Sundhedsjournal/Dokumentation/SUP.pdf#page=12).

**Kildehierarki:** De uploadede XSD-filer i `C:\dev\fhir-poc\docs\schemas` bestemmer, hvad SUP 3.0 strukturelt kan bære. [MedComs SUP 3.0-facitliste, dokumentversion 1.9](https://svn.medcom.dk/svn/releases/Standarder/Sundhedsjournal/Dokumentation/SUP.pdf), definerer aktuel brug, feltbetydning og en del krav, som XSD ikke validerer. De uploadede `bilag_02_v2_2_domaenemodel.md`, `bilag_01_v2_2_elementstruktur.md` og `bilag_13_sup_klassifikationer.md` forklarer ældre, især historiske, begreber; de kan ikke alene fastslå 3.0-praksis. LPR2 sammenlignes med [Fællesindholdets tekniske del](https://sundhedsdatastyrelsen.dk/media/15216/FI_TEKNISK.pdf); LPR3 med [bilag 1, version 5.1](https://cdn1.gopublic.dk/sundhedsdatastyrelsen/Media/638955095266548367/Bilag1_model_og_regler_v.5.1.pdf), [indberetningsvejledningen](https://cdn1.gopublic.dk/sundhedsdatastyrelsen/Media/638955095432142451/LPR-indberetningsvejledning_2025_v.5.1.pdf) og [resultatspecifikationerne](https://cdn1.gopublic.dk/sundhedsdatastyrelsen/Media/638955095308962718/Bilag1a_RI-specs_v.5.1.pdf).

**Vigtig skelnen:** En XSD kan tillade et felt, selv om SUP-databasen ikke efterspørger det. Et obligatorisk XML-felt kan indeholde `_`, når data mangler ifølge facitlisten. Et teknisk påkrævet element er dermed ikke nødvendigvis en kendt klinisk værdi. En SUP-tid kan være et skøn, fordi facitlisten tillader nærmeste kendte tidspunkt for forløbets start eller slut. Disse forhold skal videreføres som datakvalitet/proveniens, ikke skjules. [SUP-facitlisten, side 20 og 24](https://svn.medcom.dk/svn/releases/Standarder/Sundhedsjournal/Dokumentation/SUP.pdf#page=20).

MedComs [standardoversigt, opdateret 30. april 2026](https://medcom.dk/standarder/xml/e-journalstandarder/), angiver fortsat SUP 3.0 som gældende anbefaling og siger samtidig, at SUP 2.1 endnu kan anvendes, indtil der aftales udfasningsdato. Denne rapport analyserer de uploadede **3.0-XSD'er**; historiske 2.1-udtræk kræver særskilt versionskontrol.

## 2. Hvor meget af SUP findes faktisk i e-journal?

`SUPPatientforloeb.xsd` tillader **18 hændelsestyper**. SUP 3.0-vejledningens introduktion siger, at **12** anvendes, og at kun **fire** anbefales afsendt og vises i e-journal: `Kontaktperiode`, `Diagnose`, `Udfoert_procedure` og `Notat`. De øvrige otte i denne tolvliste kan opbevares, men vises ikke. PDF'en er dog **internt uens**: dens detaljerede facitliste beskriver også `Effekt_af_behandling`, `Komplikation_bivirkning`, `Maal` og `Problem`, som ikke står i introduktionens tolvliste. `Booking_af_procedure` og `Planlagt_procedure` findes i XSD og ældre domænemodel, men ikke i den aktuelle facitlistes oversigt eller fundne detaljetabeller. Vi kan derfor sikkert konkludere, hvad XSD accepterer, og hvad e-journal viser; den operationelle status for de seks øvrige typer skal afklares særskilt. [SUP 3.0, side 12–13, 20 og 49–56](https://svn.medcom.dk/svn/releases/Standarder/Sundhedsjournal/Dokumentation/SUP.pdf#page=12).

| Lag | Hændelsestyper | Betydning for ny standard |
|---|---|---|
| Vist i e-journal | Kontaktperiode, Diagnose, Udfoert_procedure, Notat | Dokumentér feltbetydning og LPR-gab først. Disse fire er den faktiske kerne i denne datakilde. |
| Beskrevet i aktuel facitliste, men ikke vist | Administrativ_karakteristikum, Medicinordination, Medicingivning, Ordination, Rekvisition, Anamnestisk_oplysning, Observation_fund, Proeveresultat | Skemaets og facitlistens mulighed er ikke dokumentation for, at patientudtrækket indeholder data. De kan være relevante for en bredere FHIR-standard og for EPJ-data. |
| Ekstra i detaljeret 3.0-facitliste, men ikke i tolvliste | Effekt_af_behandling, Komplikation_bivirkning, Maal, Problem | PDF'en giver feltbeskrivelser, men angiver ikke klart, om de hører til den praktisk anvendte tolvliste. Undersøg faktisk levering. |
| I XSD og ældre model, uden tilsvarende 3.0-facitlistefund | Booking_af_procedure, Planlagt_procedure | Bevar som skemamuligheder; få aktuel brugsstatus bekræftet før designprioritering. |

De 22 XSD-filer indeholder også service-/transportvarianter som `SUPForloebsService.xsd` og `SUPAkutService.xsd`. De er ikke 22 kliniske domæneklasser. Denne analyse tager `Aflever_patientdata` fra `SUPAfleverPatientdataService.xsd` og den tilhørende `SUPPatientforloeb.xsd` som hovedtræ. `SUPEnkelthaendelsePatientforloeb.xsd` mangler eksempelvis 3.0-attributten `SammenknytningsIdentifikation`; varianterne må ikke blandes uden særskilt versionering.

**Andre kildekonflikter at kende:** `Person/Patientforloeb` er `0..*` i den uploadede XSD, men `1..*` i 3.0-facitlisten. `CaveOplysninger/@Dato` er `xsd:string` i XSD, men facitlisten kræver en ISO 8601-tid for en faktisk registreringsdato. `Kontaktperiode/@StartTidspunkt` beskrives som kontakt-/statusstart i den ældre domænemodel, mens 3.0-facitlisten taler om oprettelse/registrering og tillader en nærliggende tid. Bilag A viser **XSD-kardinalitet**; den er ikke i sig selv fuld forretningsvalidering. Disse uoverensstemmelser bør afklares med MedCom eller konkrete afsendersystemer før en bindende profilregel.

## 3. Fælles byggesten: betydning og fejlkilder

| SUP-felt eller struktur | Hvad kilden faktisk siger | LPR2/LPR3 og problemstilling |
|---|---|---|
| `Aflever_patientdata/@Identifikation` | Et 16-tegns system-ID, som indgår i håndtering af udtræk/opdateringer. | Transportidentitet, ikke patient-, kontakt- eller forløbsidentitet. |
| `@VersionsNummer`, `@ForsendelsesTid`, `@AfsenderSystem`, `@TransaktionsType` | Skemaversion; dokumentets oprettelsestid; genererende system; fuld/delta opdatering eller sletning. | Ingen kliniske LPR-objekter. Der kræves separat synkroniserings- og proveniensmodel. |
| `Haendelse/@Identifikation` | Unik hændelsesidentitet inden for det relaterede SUP-forløb. | Bør bevares sammen med forløbs- og systemnamespace; er ikke et LPR3-objekt-ID uden dokumenteret kobling. |
| `Haendelse/@FriTekst` | Hændelsesafhængig klartekst/XHTML; eksempelvis henvisningstekst på kontakt. | Kan indeholde LPR-relevant information, men fri tekst er ikke i sig selv kodet henvisningsårsag, diagnose eller markør. |
| `@Registreringstidspunkt` | Hvornår hændelsen blev registreret i kildesystemet. | Adskilt fra klinisk tidspunkt. |
| `@Tilstede_tidspunkt` | Hvornår elementet blev tilføjet kildesystemets database. | Teknisk tidsstempel, ikke patientkontaktens eller diagnosens tid. |
| `@Ugyldighedstidspunkt` | Fra hvornår hændelseselementet er ugyldigt på grund af opdatering; tomt betyder gyldigt. | Opdaterings-/historiksemantik. Det er ikke automatisk “fejlregistreret” i en klinisk statusmodel. |
| `HaendelseRegistreretAf` | Registrerende behandlernavn og registreringsenhed. | Registrerende rolle er forskellig fra ansvarlig, producerende, henvisende og fysisk ophold. |
| `Sikkerhedskode` | Reserveret til fremtidige adgangsbetingelser; værdisæt ikke fastlagt. | Intet sikkert LPR-felt eller FHIR-sikkerhedsmærke kan udledes. |
| `KodetVaerdi` → `Klassificering/Klassifikation` | Kode, kodetekst, klassifikationens forkortelse og navn. | Kun en faktisk kendt klassifikation og version kan begrunde en LPR2/LPR3-kodeoversættelse. Tekst alene er ikke nok. |
| `SammensatKodetVaerdi` | Primærkode med mulige tillægskoder. | Ligner LPR2's SKS-primær-/tillægskoder. LPR3's sideangivelse eller andet felt må ikke udledes af en vilkårlig tillægskode. |
| `Organisatorisk_Enhed/@KodeType`, `@Kode` | `KodeType` angiver SHAK eller SOR; hvis den mangler, antager SUP-facitlisten SHAK. `@Kode` kan endda være nærmeste kendte enhed. | En SHAK-kode er ikke automatisk SOR; “nærmeste kendte” er ikke nødvendigvis den faktiske LPR3-ansvarsenhed. Institution og afdeling som tekst er hjælp, ikke et entydigt ID. |
| `AnsvarligPerson/@Identifikation`, `@Navn`, `@Titel` | Identitet, navn og titel for en aktør i en navngiven rolle. | Rollen kommer fra wrapperen, fx produceret/rekvireret/afsluttet; samme personklasse gør ikke rollerne ens. |

Kilde: [SUP 3.0-facitliste, side 22–27](https://svn.medcom.dk/svn/releases/Standarder/Sundhedsjournal/Dokumentation/SUP.pdf#page=22), lokale `SUPHaendelse.xsd`, `SUPKlassifikation.xsd`, `SUPOrganisation.xsd` og `SUPBehandlere.xsd`.

## 4. Patient, cave og forløb

| SUP-felt | Betydning i SUP | Sammenligning og gab |
|---|---|---|
| `Person/@CPRnummer` | Én person pr. udtræk, CPR med bindestreg. Facitlisten viser også format for erstatnings-CPR, men e-journal modtager det kun efter særskilt aftale. | Svarer begrebsmæssigt til LPR2 CPRNR og LPR3 Patient-id. Erstatningsnumre kræver eksplicit identifikatorstrategi; tidligere rapport beskrev dette for groft. |
| `@Navn`, `@Adresse` | Fuld navn og adresse som samlet tekst. | LPR3 Bopael har tidsafgrænset, struktureret adresse. SUP-teksten giver ikke samme historik eller struktur. |
| `@Kommunekode`, `@Kommune`, `@KommuneTilflytningsdato` | Bopælskommune, kommunenavn og flyttedato til kommunen. | LPR2 KOMNR er beslægtet; LPR3 Bopael kræver start/slut og evt. land/adresselinje. Kommunetilflytning er ikke startdato for en bestemt adresse. |
| `@Koen`, `@Foedselsdato` | Køn på hændelsens starttidspunkt og fødselsdato. | Kønsværdien er tidskontekstbundet; den bør ikke uden videre bruges som en evig patientegenskab. |
| `@TelefonNummer`, `@Paaroerende`, `@EgenLaegesNavn`, `@EgenLaegesYdernr`, `@EgenLaegeStartDato` | Telefonnummer/egen læge ved udtræk; pårørende i samlet tekst. | Journal- og kontaktinformation, som ikke er kernefelter i LPR2/LPR3-modellerne. Særligt værdifuld for en bredere standard, men ikke fuldt struktureret. |
| `CaveOplysninger/@Tekst`, `@Dato`, `Organisatorisk_Enhed` | Advarselstekst; `Dato` er **registreringsdato**, ikke nødvendigvis klinisk start; enhed er valgfri. Kan handle om allergi, pacemaker, vigtig disposition m.m. | Findes ikke som generel cave-klasse i LPR2 eller LPR3. Kan ikke automatisk omdannes til allergi, aktiv advarsel eller diagnose. Den tidligere SUP→FHIR-rapport lod datoens betydning stå åben; den er nu afklaret af domænedokumentationen. |
| `Patientforloeb/@Identifikation` | Entydigt SUP-forløbs-ID til opdatering; UUID anbefales. | Et SUP-forløb kan være en kilde-/visningsgruppe, ikke nødvendigvis ét LPR3 Forloebselement. |
| `@SammenknytningsIdentifikation` | Henviser til `Identifikation` på et andet SUP-forløb, så kontakter kan vises samlet i en LPR2-lignende forløbsvisning. | Dette er **ikke** automatisk LPR3's typede `Reference`. Formålet er sammenstilling i e-journal, og referencetype mangler. |
| `@Starttidspunkt`, `@Sluttidspunkt` | Forløbsstart/-lukning; hvis præcis tid mangler, tillader facitlisten nærmeste tidspunkt. | LPR3 Forloebselement kræver eget starttidspunkt og ansvar; en SUP-tid kan være et skøn. Skøn må ikke udlægges som observeret LPR3-start. |
| `@Teknisk_forloeb="X"` | Teknisk oprettet forløb ved udtræk fra kontaktregistrerende system. | En teknisk beholder er ikke et selvstændigt sygdoms-/LPR3-forløbselement. |
| `@Foedesystem`, `@Udtraekstidspunkt` | Oprindeligt datasystem og tidspunkt for udtræk af forløbet. | Proveniens og ID-namespace, ikke klinisk tidslinje. |
| `OprindeligAnsvarligEnhed` | Den oprindelige organisatoriske enhed knyttet til SUP-forløbet. | LPR3 Forloebselement har ansvarligEnhed for sin egen ansvarstid. “Oprindelig” dokumenterer ikke efterfølgende ansvarsskift. |

Kilde: [SUP 3.0-facitliste, side 22–25](https://svn.medcom.dk/svn/releases/Standarder/Sundhedsjournal/Dokumentation/SUP.pdf#page=22), lokale `SUPFaellesAttributter.xsd` og `bilag_02_v2_2_domaenemodel.md`, afsnit 3.4.1, 3.4.4–3.4.5.

### Læringseksempel: SUP-forløb er ikke nok til LPR3-forløb

En SUP-post kan have ID, startdato og oprindelig enhed, men stadig mangle LPR3-forløbslabel, påkrævet startmarkør, afslutningsmåde, eksplicitte typede referencer og præcise ansvarsskift. At skabe disse værdier fra en diagnosekode eller første kontakt ville være en **afledt hypotese**, ikke en kildeværdi. [LPR3 bilag 1, Forloebselement og Forloebsmarkoer](https://cdn1.gopublic.dk/sundhedsdatastyrelsen/Media/638955095266548367/Bilag1_model_og_regler_v.5.1.pdf).

## 5. De fire hændelsestyper, som vises i e-journal

### 5.1 Kontaktperiode

SUP-dokumentationen definerer den både som **kontaktperiode ved kontaktregistrering** og **status ved forløbsregistrering**. Den kan dække en indlæggelse eller et længere ambulant forløb og udelader skift af ansvarlig afdeling. Det er netop derfor, en SUP Kontaktperiode ikke automatisk er én LPR3 Kontakt. [SUP 3.0, side 12](https://svn.medcom.dk/svn/releases/Standarder/Sundhedsjournal/Dokumentation/SUP.pdf#page=12); lokal domænemodel, afsnit 7.2.1.

| Felt | Faktisk SUP-betydning | LPR2/LPR3-konsekvens |
|---|---|---|
| `@StartTidspunkt`, `@Afslutningstidspunkt` | I den ældre domænemodel kontaktens/statusperiodens start og slut. **3.0-facitlisten beskriver start som oprettelse/registrering af kontakten og tillader nærmeste faktiske registreringstid, hvis tiden ikke kendes.** | Der er et semantisk spænd mellem kilderne. Tidspunktet må ikke uden videre behandles som præcis fysisk kontaktstart i LPR3. Et langt ambulant interval kan desuden rumme flere LPR3-kontakter. |
| `ForloebsStatus/KodetVaerdi` | SUP-klassificeret status-/kontaktperiodetype; 3.0-kvalifikatorer er udvidet med LPR3-kontakttyper. | Kodeafhængig oversættelse. `Indlagt`/`Ambulant` fra ældre model og `Fysisk fremmøde` i LPR3 er ikke ét fælles statusbegreb. |
| `Indikation` | Diagnose-/problemkode som begrundelse, ofte henvisningsdiagnose. | Kan pege mod LPR2 H-diagnose eller LPR3 Henvisning.aarsag; er ikke automatisk LPR3 Kontaktaarsag. |
| `Prioritet` | Akut, fremskyndet/subakut eller planlagt prioritet. | Relaterer til LPR3 Kontakt.prioritet efter konkret kodemapping. |
| `AfslutningsAarsag` | Årsag til periodens afslutning, fx udskrivningsmåde. | Kan ligne LPR2 AFSLUTMÅDE; LPR3 har andre felter/regler og ingen universel 1:1-oversættelse. |
| `Laegelig_Ansvarlig_for_kontaktperiode` inkl. `@Stamsted` | Lægeligt ansvarlig enhed/behandler; stamsted er stedet med lægeligt ansvar under perioden. | LPR3 Kontakt.ansvarligEnhed kan være beslægtet, men ikke nødvendigvis samme rolle, især ved historisk SHAK eller skift. Stamsted er ikke fysisk `Opholdsadresse`. |
| `Kontaktperiode_afsluttet_af`, `Rekv_enhed_til_kontaktperiode` | Afsluttende og rekvirerende enhed/person. | Roller bevares separat; de må ikke erstatte kontaktansvar. |
| `Henvisning/@Rekvisition_Identifikation`, `Kontaktgrundlag/@Ordination_Identifikation` | ID på udløsende rekvisition/ordination i SUP. | Ikke LPR3 Henvisning-objekt med tidspunkt, måde, afsender og årsag; kun relation til en anden SUP-hændelse. |

**Konkrete kodeværdier:** SUP 3.0's kvalifikatorliste angiver `I` = indlagt, `A` = ambulant, `H` = henvist, `L` = læge-/speciallægepraksis, `S` = inaktiv og `D` = død som SUP-statusser. Den har også LPR3-inspirerede `F` = fysisk fremmøde, `U` = udekontakt, `V` = virtuel kontakt og `DI` = diagnoseindberetning. Prioritet er et andet værdisæt: `XSUP00P1` akut, `XSUP00P2` fremskyndet/subakut og `XSUP00P3` planlagt. Det er et praktisk eksempel på, at **status-/typekoden og prioritet skal mappes hver for sig**. [SUP 3.0-kvalifikatorlisten, side 61–62](https://svn.medcom.dk/svn/releases/Standarder/Sundhedsjournal/Dokumentation/SUP.pdf#page=61).

### 5.2 Diagnose

SUP 3.0 beskriver diagnosen som den på hændelsens start gældende **forløbsdiagnose**, eller aktionsdiagnose ved kontaktregistrering. Det er en anden placering end LPR3's aktions- og bidiagnoser på en bestemt kontakt. [SUP 3.0, side 12](https://svn.medcom.dk/svn/releases/Standarder/Sundhedsjournal/Dokumentation/SUP.pdf#page=12).

| Felt | Faktisk SUP-betydning | LPR2/LPR3-konsekvens |
|---|---|---|
| `@Diagnosetidspunkt` | Tid hvor diagnosen blev stillet; ikke sygdommens begyndelse. | Må ikke forveksles med LPR3 Kontakt.start eller klinisk sygdomsdebut. |
| `@Afsluttidspunkt` | Slut på registreret diagnose i forløbsmodellen. | Ikke bevis for at sygdommen ophørte eller at LPR3-diagnosen blev afkræftet. |
| `Art/KodetVaerdi` | Diagnoseart; den ældre klassifikation rummer bl.a. aktions-, bi- og henvisningsdiagnose. | LPR2 SKSKO.ART er beslægtet. LPR3 Diagnose har aktions-/bidiagnose; henvisningsdiagnose hører begrebsmæssigt til Henvisning. |
| `DiagnoseKode/SammensatKodetVaerdi` | Primær diagnosekode med mulige tillægskoder. | Kode, klassifikation, tillægsrolle og historisk gyldighed skal kontrolleres før LPR3-feltmapping. |
| `Diagnosticering_udfoert_af` inkl. `@Diagnosested` | Enhed/person der diagnosticerede, samt sted. | Ikke automatisk LPR3-kontaktansvarlig enhed. |
| `Diagnose_afsluttet_af` | Afsluttende enhed/person. | Registreringshistorik, ikke nødvendigvis klinisk helbredelse. |
| `Diagn_ref_til_proc/@Procedure_identifikation` | Eksplicit reference til procedure. | Bevar relationen; den gør ikke diagnosen til en kontaktdiagnose. |

### 5.3 Udført procedure

SUP skelner mellem rekvisition, ordination, planlægning, booking og faktisk udførelse. Den udførte procedure har derfor egne tider og aktørroller. Kilder: lokal `C:\dev\fhir-poc\docs\medcom\background_information\bilag_02_v2_2_domaenemodel.md`, afsnit 10.7.16, og [SUP 3.0-facitlisten](https://svn.medcom.dk/svn/releases/Standarder/Sundhedsjournal/Dokumentation/SUP.pdf).

| Felt | Faktisk SUP-betydning | LPR2/LPR3-konsekvens |
|---|---|---|
| `@Starttidspunkt`, `@Sluttidspunkt` | Udførelsesinterval; 3.0-facitlisten tillader nærmeste kendte starttidspunkt, hvis præcis start ikke kendes. | Beslægtet med LPR2 SKSKO.PROCDTO/-TIM og LPR3 Procedure.start/slut, men skøn og præcision skal bevares. |
| `@Afslutningstidspunkt` | Afslutning af et længerevarende procedureforhold, ikke nødvendigvis den praktiske handlings slut. | Må ikke kopieres til LPR3 Procedure.sluttidspunkt uden regel. |
| `Art` | Procedureart, især relevant for operation. | LPR2 SKSKO.ART (V/P/D m.fl.) er beslægtet; LPR3 procedurekode og øvrige egenskaber følger andre regler. |
| `Procedure_kode/SammensatKodetVaerdi` | Primær procedurekode med tillægskoder. | SKS-struktur kan bevares; LPR3 sideangivelse, handlingsspecifikation, kontrast m.m. kræver særskilt afkodning. |
| `Indikation`, `Prioritet`, `AfslutningsAarsag` | Begrundelse, prioritet og afslutningsårsag for procedure. | LPR3 har `Procedure.indikation`, men værditype og kodeværk skal afstemmes. Prioritet/årsag er ikke automatisk LPR3-felter. |
| `Udloesende_haendelse` | ID på rekvisition, ordination eller planlagt procedure. | Dokumenterer intern SUP-kæde, ikke i sig selv LPR3-henvisning. |
| `Udfoert_procedure_rekvireret_af`, `_produceret_af`, `_afsluttet_af` | Bestillende, udførende og afsluttende enhed/person; udførelsessted ligger på producerende rolle. | LPR3 Procedure.producent er især beslægtet med producerende enhed. Roller og SHAK/SOR må ikke sammenblandes. |
| `@Objektreference` | Reference til tilhørende objekt/beskrivelse. | Ikke automatisk et LPR3-objekt eller en tilgængelig dokument-URL. |

### 5.4 Notat

Notat er en selvstændig SUP-hændelse med fri tekst, fx udskrivningsbrev eller ambulantnotat. Der er **intet generelt Notat-objekt i LPR2's eller LPR3's indberetningsmodel**. Den nye FHIR-standard bør derfor ikke miste notater ved at begrænse sig til LPR3-klasser. [SUP 3.0, side 13](https://svn.medcom.dk/svn/releases/Standarder/Sundhedsjournal/Dokumentation/SUP.pdf#page=13).

| Felt | Faktisk SUP-betydning | Problemstilling |
|---|---|---|
| `@Overskrift`, `@Broedtekst` | Titel og notatets egentlige tekst; brødtekst kan være formateret. | Kræver dokument-/tekstmodel, tegnsæt, XHTML-håndtering og adgangsvurdering. |
| `@KonstateringsTidspunkt` | Klinisk konstateringstid knyttet til notatet. | Ikke nødvendigvis skrive-, underskrifts- eller publiceringstid. |
| `@Objektreference` | Ekstern objektreference. | Skal kunne opløses og adgangsstyres før den bruges som dokumentlink. |
| `Notat_type` | Kode for notattypen. | Kræver klassifikation og version, ikke kun fritekstoverskrift. |
| `Notat_rekvireret_af`, `Notat_produceret_af` inkl. `@ProcedureSted` | Bestillende og producerende aktører, samt sted i den producerende kontekst. | Rekvirent er ikke automatisk forfatter; sted er ikke LPR3 Opholdsadresse. |
| `Notat_til_procedure/@Udfoert_procedure_identifikation`, `Procedurekode` | Relation til udført procedure samt kode tilknyttet notat. | Koden beviser ikke, at en særskilt procedurehændelse findes; relationen skal opløses på ID. |

## 6. De øvrige 14 SUP-hændelsestyper: begreb og placering

Tabellen sammenfatter alle typer, som hovedtræet tillader. De konkrete direkte felter står i det fulde XSD-inventar i bilag A nedenfor. Fælles `Haendelse`, koder og aktørwrappere fortolkes som i afsnit 3, ikke som nye begreber for hver række.

| SUP-hændelse | Egen betydning i SUP og vigtige felter | Nærmeste LPR2/LPR3-område; hvor ækvivalensen bryder |
|---|---|---|
| `Administrativ_karakteristikum` | Administrativt forhold med start/slut/afslut, karakteristikumkode, årsag, afslutningsårsag, konstaterende/afsluttende aktør og udløsende rekvisition/procedure. | LPR2 har flere administrative kontaktfelter, LPR3 bl.a. Betalingsoplysning og Henvisning. En generisk karakteristikumkode er ikke én bestemt LPR3-klasse. |
| `Medicinordination` | Ordineret lægemiddel med præparat, ATC, dosis/styrke, administrationsmåde, ordinerende/planlagt givende/seponerende aktør og start/slut/seponering. | Ikke en selvstændig generel LPR2/LPR3-hovedklasse. Vigtig for EPJ/FHIR, men må ikke udledes af en LPR-procedurekode alene. |
| `Medicingivning` | Faktisk dosis/givning med tid, dosis/styrke, givende/ordinerende/afsluttende aktør og reference til ordination. | Journalnært begreb; LPR's procedureindberetning beskriver ikke nødvendigvis hver enkelt givning. |
| `Ordination` | Bestilling/beslutning om ikke-medicinsk procedure, med kode, antal/enhed, prioritet og plan-/seponeringsrelation. | LPR3 Procedure beskriver handlingen, ikke hele ordinationslivscyklussen. |
| `Rekvisition` | Anmodning om procedure/prøve med rekvisitionstid, kode, antal/enhed, afsender/modtager og relation til ordination/plan. | LPR3 Henvisning er ikke synonym med hver intern rekvisition. |
| `Anamnestisk_oplysning` | Oplysning fortalt af patient/anden person; konstaterings-/anamnesetid, varighed, kode og kontekst. | Ikke generelt LPR2/LPR3-objekt; kan påvirke diagnose, men er ikke selv en LPR3-diagnose. |
| `Observation_fund` | Klinisk observeret fund med kode, værdi/enhed, observationstid, blodtryksfelter og observerende aktør. | LPR3 Resultat kan på nogle områder bære en måling, men kun efter en konkret RI-specifikation; ikke generel ækvivalens. |
| `Proeveresultat` | Undersøgelses-/laboratoriesvar med prøvetid, svartid, resultat, grænser, unormalitet, rekvirerende/producerende aktør. | LPR3 Resultat er indberetningsspecifikt. Et lokalt prøvesvar er ikke automatisk et LPR3 Resultat. |
| `Booking_af_procedure` | Reserveret tidspunkt, evt. aflysning, kode, rekvirent/producerende/afsluttende aktør og udløsende plan/ordre/rekvisition. | Ingen generel LPR2/LPR3-bookingklasse. |
| `Planlagt_procedure` | Planlagt handling med planlægningstid, evt. afslutning, planlæggende/afsluttende aktør. | LPR3 Procedure kræver faktisk procedurehændelse; plan er ikke udførelse. |
| `Effekt_af_behandling` | Før-/efterværdier og -koder, observeret effekt, behandlingstid og reference til behandlingsprocedure. | Kan kun svare til LPR3 Resultat, når en konkret RI-specifikation kræver netop disse værdier. |
| `Komplikation_bivirkning` | Observeret komplikation/bivirkning med diagnose-/procedurekode, observations-/proceduretid og reference til procedure. | Beslægtet med LPR3 diagnose eller RI for kirurgisk komplikation i bestemte tilfælde; ikke automatisk identisk. |
| `Maal` | Besluttet klinisk mål med ønsket opfyldelsestid, værdi/grænser, problem-/diagnosereference og afslutning. | Journal-/behandlingsplanbegreb uden generelt LPR2/LPR3-modstykke. |
| `Problem` | Klinisk problem med konstatering, værdi, årsag, afslutning og evt. procedurerelation. | Ikke automatisk LPR3 kontaktdiagnose; kan være bredere eller mindre sikkert kodet. |

Kilde til de otte typer i introduktionens tolvliste: [SUP 3.0, side 12–13](https://svn.medcom.dk/svn/releases/Standarder/Sundhedsjournal/Dokumentation/SUP.pdf#page=12). Effekt, komplikation, mål og problem findes også i [den detaljerede XML-facitliste, side 49–56](https://svn.medcom.dk/svn/releases/Standarder/Sundhedsjournal/Dokumentation/SUP.pdf#page=49). Kilde til booking og planlagt procedure er lokale XSD-filer og `bilag_02_v2_2_domaenemodel.md`.

## 7. Gab i begge retninger

| Område | Det SUP kan dokumentere | Det der mangler eller kræver en regel |
|---|---|---|
| LPR3 Forloebselement | SUP-forløbs-ID, start/slut, oprindelig enhed og evt. visningssammenknytning. | Forløbslabel, startmarkør, afslutningsmåde, præcist ansvar over tid og typede referencer er ikke generelle SUP-felter. |
| LPR3 Kontakt | SUP Kontaktperiode med status-/kontakttype, prioritet og ansvarlig rolle. | Én SUP-periode kan dække mere end én LPR3-kontakt; konkret kontaktgrænse og ansvarsskift kan mangle. |
| LPR3 Henvisning og Kontaktaarsag | SUP-indikation og ID-relation til rekvisition/ordination; fri tekst kan findes. | Henvisningstid, henvisende instans, måde, frit valg og specifik akut kontaktårsag er ikke samlet i et LPR3-lignende objekt. |
| LPR3 Forloebsmarkoer | Tider/status og visse administrative SUP-hændelser. | Ingen generel markørklasse med kode og tidspunkt. En status må ikke opfindes som en bestemt markør uden kodet regel. |
| LPR3 Opholdsadresse og Betalingsoplysning | Kontaktansvarlig/stamsted og enkelte administrative karakteristika. | Fysisk ophold/fravær og tidsafgrænset betaling med aftale/betaler/specialiseringsniveau er ikke generelt modelleret. |
| LPR3 Procedure | SUP udført procedure med kode, tider, indikation og producentrolle. | Kontakt- versus forløbstilknytning, side, handlingsspecifikation, kontrast, personalekategori og SOR-korrekt producent kræver kontrol/andre data. |
| LPR3 Diagnose | SUP-diagnose med art, kode, tid og aktør. | Bestemt kontakt, aktions-/bidiagnoserolle og senere afkræftelse kan ikke generelt rekonstrueres. |
| LPR3 Resultatindberetning | SUP har generelle observationer, prøvesvar, effekt og komplikationer i skemaet. | LPR3's navngivne RI, trigger, færdigstatus, specifikke værdisæt og påkrævede enkeltresultater er andre strukturer. |
| SUP ud over LPR | Cave, notat, ordinationer, givninger, booking, plan, observationer, problemer og mål. | De kan ikke forkastes, blot fordi LPR3 ikke har en generel klasse. De er kandidater til EPJ-/FHIR-delen af den nye standard. |

**LPR2-gab:** SUP er heller ikke hele LPR2. Den har ikke selvstændige `BESØG`, `VENTE`, `PASSV`, `BOBST`, `MOBST`, `PSYKI` eller `STEDF`-poster, sådan som de findes i Fællesindholdets tekniske layout. Nogle oplysninger kan ligge som koder, fritekst eller lokale `Administrativ_karakteristikum`-hændelser, men den konkrete ækvivalens kan først vises ved en kode- og datagennemgang. Omvendt indeholder SUP journalnære hændelser, som LPR2-indberetningen ikke modellerer generelt.

## 8. Beslutninger til næste iteration

1. **Hold kildeværdi, udledning og manglende data adskilt.** Et FHIR-element kan godt udtrykke en beregnet relation, men den skal markeres som beregnet og kunne spores til kildeposter. Den må ikke tælle som en direkte LPR3-oplysning.
2. **Versionér kodemappinger.** `ForloebsStatus`, diagnoseart, SKS-primær-/tillægskode og SHAK/SOR kræver konkrete systemer, kodeversioner og gyldighedsdatoer. `bilag_13` alene er historisk.
3. **Afklar faktisk forekomst.** De 14 ikke-viste hændelsestyper i XSD skal undersøges i de konkrete udtræk, før de prioriteres i en ny standard. At en klasse kan valideres, betyder ikke at data leveres.
4. **Modellér forløbsrelationer eksplicit.** Afgør om en SUP-gruppe er teknisk, LPR2-kontaktbaseret eller afsenders egen LPR3-inspirerede struktur, før den oversættes til et FHIR-forløb.
5. **Bevar SUP's merværdi.** Notat og cave skal med i standardens informationsmodel, også selv om de ikke er LPR3-indberetningsobjekter.

### Tre konkrete testcases til senere mapping

- **Lang ambulant SUP-kontakt med to fremmøder:** Kan kilden udpege to selvstændige LPR3-kontakter? Hvis kun interval og to datoer findes, mangler især præcise start-/sluttider og evt. ansvar.
- **SUP-diagnose med `Art=H`:** Bevar som henvisningsdiagnose i kilden; kontroller om den kan knyttes til en konkret LPR3 Henvisning. Opret ikke en LPR3 kontaktdiagnose alene på artkoden.
- **SUP-forløb med `SammenknytningsIdentifikation`:** Vis relationen i kildevisningen. Den bliver først LPR3 Reference, hvis referencetype og faktisk forløbselement-semantik er dokumenteret.

## Bilag A. Komplet direkte XSD-feltinventar

Dette bilag genereres ud fra `SUPAfleverPatientdataService.xsd`, `SUPPatientforloeb.xsd` og deres inkluderede domæneskemaer. Hver række viser de **direkte** elementer og attributter på klassen; fælles `Haendelse`, kode- og aktørtyper er kun opført én gang på deres egen række. Det gør inventaret udtømmende uden at gentage de samme felter 18 gange. Kolonnen “forståelse” i afsnit 3–6 og MedComs facitliste skal bruges sammen med feltlisten; XSD alene fastlægger ikke klinisk betydning.

| Element | Direkte underelementer (XSD-kardinalitet) | Direkte attributter (XSD-krav) | Skema |
|---|---|---|---|
| `Aflever_patientdata` | `Person` (1..1) | `@VersionsNummer` (påkrævet), `@Identifikation` (påkrævet), `@ForsendelsesTid` (påkrævet), `@AfsenderSystem` (påkrævet), `@TransaktionsType` (påkrævet) | `SUPAfleverPatientdataService.xsd` |
| `Person` | `Patientforloeb` (0..*), `CaveOplysninger` (0..*) | `@CPRnummer` (påkrævet), `@Navn` (valgfri), `@Adresse` (valgfri), `@Kommunekode` (valgfri), `@Kommune` (valgfri), `@KommuneTilflytningsdato` (valgfri), `@Koen` (valgfri), `@Foedselsdato` (valgfri), `@TelefonNummer` (valgfri), `@Paaroerende` (valgfri), `@EgenLaegesNavn` (valgfri), `@EgenLaegesYdernr` (valgfri), `@EgenLaegeStartDato` (valgfri) | `SUPAfleverPatientdataService.xsd` |
| `Patientforloeb` | `OprindeligAnsvarligEnhed` (1..1), `Administrativ_karakteristikum` (0..*), `Kontaktperiode` (0..*), `Medicinordination` (0..*), `Medicingivning` (0..*), `Notat` (0..*), `Booking_af_procedure` (0..*), `Ordination` (0..*), `Planlagt_procedure` (0..*), `Rekvisition` (0..*), `Udfoert_procedure` (0..*), `Anamnestisk_oplysning` (0..*), `Observation_fund` (0..*), `Proeveresultat` (0..*), `Diagnose` (0..*), `Effekt_af_behandling` (0..*), `Komplikation_bivirkning` (0..*), `Maal` (0..*), `Problem` (0..*) | `@SammenknytningsIdentifikation` (valgfri), `@Identifikation` (påkrævet), `@Starttidspunkt` (valgfri), `@Sluttidspunkt` (valgfri), `@Teknisk_forloeb` (valgfri), `@Foedesystem` (påkrævet), `@Udtraekstidspunkt` (valgfri) | `SUPPatientforloeb.xsd` |
| `OprindeligAnsvarligEnhed` | `Organisatorisk_Enhed` (1..1) | — | `SUPOrganisation.xsd` |
| `Organisatorisk_Enhed` | — | `@KodeType` (valgfri), `@Kode` (valgfri), `@Institution_tekst` (valgfri), `@Afdeling_tekst` (valgfri) | `SUPOrganisation.xsd` |
| `Administrativ_karakteristikum` | `Haendelse` (1..1), `Karakteristikum_kode` (1..1), `Aarsag` (0..1), `AfslutningsAarsag` (0..1), `Administrativ_karakteristikum_konstateret_af` (1..1), `Administrativ_karakteristikum_afsluttet_af` (0..1), `Udloesende_rekv_eller_procedure` (0..1) | `@Starttidspunkt` (påkrævet), `@SlutTidspunkt` (valgfri), `@AfslutTidspunkt` (valgfri) | `SUPAdministrativeKarakteristika.xsd` |
| `Haendelse` | `HaendelseRegistreretAf` (1..1), `Sikkerhedskode` (0..1) | `@Identifikation` (påkrævet), `@FriTekst` (valgfri), `@Registreringstidspunkt` (valgfri), `@Tilstede_tidspunkt` (valgfri), `@Ugyldighedstidspunkt` (valgfri) | `SUPHaendelse.xsd` |
| `HaendelseRegistreretAf` | `Registrerings_Enhed` (1..1) | `@Registrerende_behandler` (valgfri) | `SUPHaendelse.xsd` |
| `Registrerings_Enhed` | `Organisatorisk_Enhed` (1..1) | — | `SUPOrganisation.xsd` |
| `Sikkerhedskode` | `KodetVaerdi` (1..1) | — | `SUPHaendelse.xsd` |
| `KodetVaerdi` | `Klassificering` (1..1) | `@Kode` (valgfri), `@Kodetekst` (valgfri) | `SUPKlassifikation.xsd` |
| `Klassificering` | `Klassifikation` (1..1) | — | `SUPKlassifikation.xsd` |
| `Klassifikation` | — | `@Forkortelse` (valgfri), `@Navn` (valgfri) | `SUPKlassifikation.xsd` |
| `Karakteristikum_kode` | `SammensatKodetVaerdi` (1..1) | — | `SUPAdministrativeKarakteristika.xsd` |
| `SammensatKodetVaerdi` | `Primaerkode` (1..1), `Tillaegskode` (0..*) | — | `SUPKlassifikation.xsd` |
| `Primaerkode` | `KodetVaerdi` (1..1) | — | `SUPKlassifikation.xsd` |
| `Tillaegskode` | `KodetVaerdi` (1..1) | — | `SUPKlassifikation.xsd` |
| `Aarsag` | `KodetVaerdi` (1..1) | — | `SUPFaellesAttributter.xsd` |
| `AfslutningsAarsag` | `KodetVaerdi` (1..1) | — | `SUPFaellesAttributter.xsd` |
| `Administrativ_karakteristikum_konstateret_af` | `Konstaterende_Behandler` (0..1), `Konstaterende_Enhed` (1..1) | — | `SUPAdministrativeKarakteristika.xsd` |
| `Konstaterende_Behandler` | `AnsvarligPerson` (1..1) | — | `SUPBehandlere.xsd` |
| `AnsvarligPerson` | — | `@Identifikation` (valgfri), `@Navn` (valgfri), `@Titel` (valgfri) | `SUPBehandlere.xsd` |
| `Konstaterende_Enhed` | `Organisatorisk_Enhed` (1..1) | — | `SUPOrganisation.xsd` |
| `Administrativ_karakteristikum_afsluttet_af` | `Afsluttende_Behandler` (0..1), `Afsluttende_Enhed` (1..1) | — | `SUPAdministrativeKarakteristika.xsd` |
| `Afsluttende_Behandler` | `AnsvarligPerson` (1..1) | — | `SUPBehandlere.xsd` |
| `Afsluttende_Enhed` | `Organisatorisk_Enhed` (1..1) | — | `SUPOrganisation.xsd` |
| `Udloesende_rekv_eller_procedure` | — | `@Rekvisition_Procedure_Identifikation` (påkrævet) | `SUPAdministrativeKarakteristika.xsd` |
| `Kontaktperiode` | `Haendelse` (1..1), `ForloebsStatus` (1..1), `Indikation` (0..1), `Prioritet` (0..1), `AfslutningsAarsag` (0..1), `Laegelig_Ansvarlig_for_kontaktperiode` (1..1), `Kontaktperiode_afsluttet_af` (0..1), `Rekv_enhed_til_kontaktperiode` (0..1), `Henvisning` (0..1), `Kontaktgrundlag` (0..1) | `@StartTidspunkt` (påkrævet), `@Afslutningstidspunkt` (valgfri) | `SUPKontaktperiode.xsd` |
| `ForloebsStatus` | `KodetVaerdi` (1..1) | — | `SUPKontaktperiode.xsd` |
| `Indikation` | `KodetVaerdi` (1..1) | — | `SUPFaellesAttributter.xsd` |
| `Prioritet` | `KodetVaerdi` (1..1) | — | `SUPFaellesAttributter.xsd` |
| `Laegelig_Ansvarlig_for_kontaktperiode` | `Laegelig_ansvarlig_behandler` (0..1), `Laegelig_kontaktansvarlig_Enhed` (1..1) | `@Stamsted` (påkrævet) | `SUPKontaktperiode.xsd` |
| `Laegelig_ansvarlig_behandler` | `AnsvarligPerson` (1..1) | — | `SUPBehandlere.xsd` |
| `Laegelig_kontaktansvarlig_Enhed` | `Organisatorisk_Enhed` (1..1) | — | `SUPOrganisation.xsd` |
| `Kontaktperiode_afsluttet_af` | `Afsluttende_Behandler` (0..1), `Afsluttende_Enhed` (1..1) | — | `SUPKontaktperiode.xsd` |
| `Rekv_enhed_til_kontaktperiode` | `Rekvirerende_Behandler` (0..1), `Rekvirerende_Enhed` (1..1) | — | `SUPKontaktperiode.xsd` |
| `Rekvirerende_Behandler` | `AnsvarligPerson` (1..1) | — | `SUPBehandlere.xsd` |
| `Rekvirerende_Enhed` | `Organisatorisk_Enhed` (1..1) | — | `SUPOrganisation.xsd` |
| `Henvisning` | — | `@Rekvisition_Identifikation` (valgfri) | `SUPKontaktperiode.xsd` |
| `Kontaktgrundlag` | — | `@Ordination_Identifikation` (valgfri) | `SUPKontaktperiode.xsd` |
| `Medicinordination` | `Haendelse` (1..1), `Praeparat` (1..1), `Indikation` (0..1), `Type` (1..1), `SeponeringsAarsag` (0..1), `ATC_Kode` (1..1), `Administrationsmaade` (1..1), `Form` (0..1), `Medicinordination_ordineret_af` (1..1), `Medicin_planlagt_givet_af` (0..1), `Medicinordination_seponeret_af` (0..1) | `@Starttidspunkt` (påkrævet), `@Sluttidspunkt` (valgfri), `@Seponeringstidspunkt` (valgfri), `@DoegnDosis` (valgfri), `@EnkeltDosis` (valgfri), `@MaksimalDoegnDosis` (valgfri), `@Enhed` (valgfri), `@Styrke` (valgfri), `@Objektreference` (valgfri) | `SUPMedicinering.xsd` |
| `Praeparat` | `SammensatKodetVaerdi` (1..1) | — | `SUPMedicinering.xsd` |
| `Type` | `KodetVaerdi` (1..1) | — | `SUPMedicinering.xsd` |
| `SeponeringsAarsag` | `KodetVaerdi` (1..1) | — | `SUPMedicinering.xsd` |
| `ATC_Kode` | `KodetVaerdi` (1..1) | — | `SUPMedicinering.xsd` |
| `Administrationsmaade` | `KodetVaerdi` (1..1) | — | `SUPMedicinering.xsd` |
| `Form` | `KodetVaerdi` (1..1) | — | `SUPMedicinering.xsd` |
| `Medicinordination_ordineret_af` | `Ordinerende_Behandler` (0..1), `Ordinerende_Enhed` (1..1) | `@Ordinationssted` (valgfri) | `SUPMedicinering.xsd` |
| `Ordinerende_Behandler` | `AnsvarligPerson` (1..1) | — | `SUPBehandlere.xsd` |
| `Ordinerende_Enhed` | `Organisatorisk_Enhed` (1..1) | — | `SUPOrganisation.xsd` |
| `Medicin_planlagt_givet_af` | `Producerende_Behandler` (0..1), `Producerende_Enhed` (1..1) | — | `SUPMedicinering.xsd` |
| `Producerende_Behandler` | `AnsvarligPerson` (1..1) | — | `SUPBehandlere.xsd` |
| `Producerende_Enhed` | `Organisatorisk_Enhed` (1..1) | — | `SUPOrganisation.xsd` |
| `Medicinordination_seponeret_af` | `Seponerende_Behandler` (0..1), `Seponerende_Enhed` (1..1) | — | `SUPMedicinering.xsd` |
| `Seponerende_Behandler` | `AnsvarligPerson` (1..1) | — | `SUPBehandlere.xsd` |
| `Seponerende_Enhed` | `Organisatorisk_Enhed` (1..1) | — | `SUPOrganisation.xsd` |
| `Medicingivning` | `Haendelse` (1..1), `Type` (1..1), `Praeparat` (1..1), `Indikation` (0..1), `AfslutningsAarsag` (0..1), `ATC_Kode` (0..1), `Administrationsmaade` (0..1), `Form` (0..1), `Medicin_givet_af` (1..1), `Medicingivning_ordineret_af` (0..1), `Medicingivning_afsluttet_af` (1..1), `Medicingivning_foraarsaget_af` (0..1) | `@Starttidspunkt` (påkrævet), `@Afslutningstidspunkt` (valgfri), `@Enkeltdosis` (påkrævet), `@EnhedEnkeltdosis` (påkrævet), `@Styrke` (valgfri), `@Objektreference` (valgfri) | `SUPMedicinering.xsd` |
| `Medicin_givet_af` | `Producerende_Behandler` (0..1), `Producerende_Enhed` (1..1) | `@Procedure_sted` (valgfri) | `SUPMedicinering.xsd` |
| `Medicingivning_ordineret_af` | `Ordinerende_Behandler` (0..1), `Ordinerende_Enhed` (1..1) | — | `SUPMedicinering.xsd` |
| `Medicingivning_afsluttet_af` | `Afsluttende_Enhed` (1..1), `Afsluttende_Behandler` (0..1) | — | `SUPMedicinering.xsd` |
| `Medicingivning_foraarsaget_af` | — | `@Medicinordination_Identifikation` (påkrævet) | `SUPMedicinering.xsd` |
| `Notat` | `Haendelse` (1..1), `Notat_type` (0..1), `Notat_rekvireret_af` (0..1), `Notat_produceret_af` (1..1), `Notat_til_procedure` (0..1), `Procedurekode` (0..1) | `@Overskrift` (valgfri), `@KonstateringsTidspunkt` (påkrævet), `@Broedtekst` (valgfri), `@Objektreference` (valgfri) | `SUPNotat.xsd` |
| `Notat_type` | `KodetVaerdi` (1..1) | — | `SUPNotat.xsd` |
| `Notat_rekvireret_af` | `Rekvirerende_Behandler` (0..1), `Rekvirerende_Enhed` (1..1) | — | `SUPNotat.xsd` |
| `Notat_produceret_af` | `Producerende_Behandler` (0..1), `Producerende_Enhed` (1..1) | `@ProcedureSted` (valgfri) | `SUPNotat.xsd` |
| `Notat_til_procedure` | — | `@Udfoert_procedure_identifikation` (valgfri) | `SUPNotat.xsd` |
| `Procedurekode` | `KodetVaerdi` (1..1) | — | `SUPFaellesAttributter.xsd` |
| `Booking_af_procedure` | `Haendelse` (1..1), `Procedure_kode` (1..1), `Indikation` (1..1), `Prioritet` (1..1), `Booking_af_procedure_produceret_af` (1..1), `Booking_af_procedure_rekvireret_af` (1..1), `Booking_af_procedure_afsluttet_af` (0..1), `Booking_udloest_af` (0..1) | `@Starttidspunkt` (påkrævet), `@Sluttidspunkt` (valgfri), `@Aflysningstidspunkt` (valgfri), `@Afslutningsaarsag` (valgfri) | `SUPProcedureProces.xsd` |
| `Procedure_kode` | `SammensatKodetVaerdi` (1..1) | — | `SUPFaellesAttributter.xsd` |
| `Booking_af_procedure_produceret_af` | `Producerende_Enhed` (1..1), `Producerende_Behandler` (1..1) | `@Udfoerelsessted` (valgfri) | `SUPProcedureProces.xsd` |
| `Booking_af_procedure_rekvireret_af` | `Rekvirerende_Enhed` (1..1), `Rekvirerende_Behandler` (1..1) | — | `SUPProcedureProces.xsd` |
| `Booking_af_procedure_afsluttet_af` | `Afsluttende_Enhed` (1..1), `Afsluttende_Behandler` (1..1) | — | `SUPProcedureProces.xsd` |
| `Booking_udloest_af` | — | `@Planlagt_procedure_identifikation` (valgfri), `@Ordination_identifikation` (valgfri), `@Rekvisition_identifikation` (valgfri) | `SUPProcedureProces.xsd` |
| `Ordination` | `Haendelse` (1..1), `Procedure_kode` (1..1), `Indikation` (0..1), `Prioritet` (0..1), `AfslutningsAarsag` (0..1), `Ordination_ordineret_af` (1..1), `Ordination_planlagt_produceret_af` (0..1), `Ordination_seponeret_af` (0..1), `Planlaegning_af_ordination` (1..1) | `@Starttidspunkt` (påkrævet), `@Sluttidspunkt` (valgfri), `@Seponeringstidspunkt` (valgfri), `@Antal` (valgfri), `@Enhed` (valgfri) | `SUPProcedureProces.xsd` |
| `Ordination_ordineret_af` | `Ordinerende_Behandler` (0..1), `Ordinerende_Enhed` (1..1) | `@Ordinationssted` (valgfri) | `SUPProcedureProces.xsd` |
| `Ordination_planlagt_produceret_af` | `Producerende_Behandler` (0..1), `Producerende_Enhed` (1..1) | — | `SUPProcedureProces.xsd` |
| `Ordination_seponeret_af` | `Seponerende_Behandler` (0..1), `Seponerende_Enhed` (1..1) | — | `SUPProcedureProces.xsd` |
| `Planlaegning_af_ordination` | — | `@Planlagt_procedure_identifikation` (valgfri) | `SUPProcedureProces.xsd` |
| `Planlagt_procedure` | `Haendelse` (1..1), `Procedure_kode` (0..1), `Indikation` (0..1), `Prioritet` (0..1), `AfslutningsAarsag` (0..1), `Planlagt_procedure_planlagt_af` (1..1), `Planlagt_procedure_afsluttet_af` (0..1) | `@Planlaegningstidspunkt` (valgfri), `@Afslutningstidspunkt` (valgfri), `@Objektreference` (valgfri) | `SUPProcedureProces.xsd` |
| `Planlagt_procedure_planlagt_af` | `Planlaeggende_Behandler` (0..1), `Planlaeggende_Enhed` (1..1) | `@Planlaegningssted` (valgfri) | `SUPProcedureProces.xsd` |
| `Planlaeggende_Behandler` | `AnsvarligPerson` (1..1) | — | `SUPBehandlere.xsd` |
| `Planlaeggende_Enhed` | `Organisatorisk_Enhed` (1..1) | — | `SUPOrganisation.xsd` |
| `Planlagt_procedure_afsluttet_af` | `Afsluttende_Behandler` (0..1), `Afsluttende_Enhed` (1..1) | — | `SUPProcedureProces.xsd` |
| `Rekvisition` | `Haendelse` (1..1), `Procedure_kode` (1..1), `Indikation` (0..1), `Prioritet` (0..1), `AfslutningsAarsag` (0..1), `Rekvisition_udloest_af` (0..1), `Rekvisition_rekvireret_af` (1..1), `Rekvisition_planlagt_produceret_af` (1..1), `Rekvisition_faktisk_afsluttet_af` (0..1) | `@Rekvisitionstidspunkt` (påkrævet), `@Afslutningstidspunkt` (valgfri), `@Antal` (valgfri), `@Enhed` (valgfri) | `SUPProcedureProces.xsd` |
| `Rekvisition_udloest_af` | — | `@Ordination_Identifikation` (valgfri), `@Planlagt_procedure_Identifikation` (valgfri) | `SUPProcedureProces.xsd` |
| `Rekvisition_rekvireret_af` | `Rekvirerende_Behandler` (0..1), `Rekvirerende_Enhed` (1..1) | `@Rekvisitionssted` (valgfri) | `SUPProcedureProces.xsd` |
| `Rekvisition_planlagt_produceret_af` | `Producerende_Behandler` (0..1), `Producerende_Enhed` (1..1) | — | `SUPProcedureProces.xsd` |
| `Rekvisition_faktisk_afsluttet_af` | `Afsluttende_Behandler` (0..1), `Afsluttende_Enhed` (1..1) | — | `SUPProcedureProces.xsd` |
| `Udfoert_procedure` | `Haendelse` (1..1), `Art` (0..1), `Procedure_kode` (1..1), `Indikation` (0..1), `Prioritet` (0..1), `AfslutningsAarsag` (0..1), `Udloesende_haendelse` (0..1), `Udfoert_procedure_rekvireret_af` (0..1), `Udfoert_procedure_produceret_af` (1..1), `Udfoert_procedure_afsluttet_af` (0..1) | `@Starttidspunkt` (påkrævet), `@Sluttidspunkt` (valgfri), `@Afslutningstidspunkt` (valgfri), `@Objektreference` (valgfri) | `SUPProcedureProces.xsd` |
| `Art` | `KodetVaerdi` (1..1) | — | `SUPFaellesAttributter.xsd` |
| `Udloesende_haendelse` | — | `@Rekvisition_Identifikation` (valgfri), `@Ordination_Identifikation` (valgfri), `@Planlagt_procedure_Identifikation` (valgfri) | `SUPProcedureProces.xsd` |
| `Udfoert_procedure_rekvireret_af` | `Rekvirerende_Behandler` (0..1), `Rekvirerende_Enhed` (1..1) | — | `SUPProcedureProces.xsd` |
| `Udfoert_procedure_produceret_af` | `Producerende_Behandler` (0..1), `Producerende_Enhed` (1..1) | `@Udfoerelsessted` (valgfri) | `SUPProcedureProces.xsd` |
| `Udfoert_procedure_afsluttet_af` | `Afsluttende_Behandler` (0..1), `Afsluttende_Enhed` (1..1) | — | `SUPProcedureProces.xsd` |
| `Anamnestisk_oplysning` | `Haendelse` (1..1), `Art` (1..1), `AnamnestiskOplysning` (1..1), `Us_procedure` (1..1), `Anamnestisk_oplysning_konstanteret_af` (1..1), `Procedure_hvor_oplysning_blev_givet` (0..1) | `@Konstateringstidspunkt` (påkrævet), `@AnamnestiskTidspunkt` (valgfri), `@Periode` (valgfri), `@EnhedPeriode` (valgfri), `@Varighed` (valgfri), `@EnhedVarighed` (valgfri), `@Objektreference` (valgfri) | `SUPResultat.xsd` |
| `AnamnestiskOplysning` | `SammensatKodetVaerdi` (1..1) | — | `SUPResultat.xsd` |
| `Us_procedure` | `KodetVaerdi` (1..1) | — | `SUPFaellesAttributter.xsd` |
| `Anamnestisk_oplysning_konstanteret_af` | `Konstaterende_Behandler` (0..1), `Konstaterende_Enhed` (1..1) | `@Observationssted` (valgfri) | `SUPResultat.xsd` |
| `Procedure_hvor_oplysning_blev_givet` | — | `@Procedure_identifikation` (valgfri) | `SUPResultat.xsd` |
| `Observation_fund` | `Haendelse` (1..1), `Observationskode` (1..1), `Undersoegelsesprocedure` (0..1), `Udloesende_procedure` (0..1), `Observation_fund_observeret_af` (1..1) | `@Vaerdi` (valgfri), `@EnhedVaerdi` (valgfri), `@ObservationsTidspunkt` (påkrævet), `@SystBT` (valgfri), `@SystBTEnhed` (valgfri), `@DiasBT` (valgfri), `@DiasBTEnhed` (valgfri) | `SUPResultat.xsd` |
| `Observationskode` | `SammensatKodetVaerdi` (1..1) | — | `SUPResultat.xsd` |
| `Undersoegelsesprocedure` | `KodetVaerdi` (1..1) | — | `SUPResultat.xsd` |
| `Udloesende_procedure` | — | `@Procedure_identifikation` (påkrævet) | `SUPResultat.xsd` |
| `Observation_fund_observeret_af` | `Observerende_Behandler` (0..1), `Observerende_Enhed` (1..1) | `@Observationssted` (valgfri) | `SUPResultat.xsd` |
| `Observerende_Behandler` | `AnsvarligPerson` (1..1) | — | `SUPBehandlere.xsd` |
| `Observerende_Enhed` | `Organisatorisk_Enhed` (1..1) | — | `SUPOrganisation.xsd` |
| `Proeveresultat` | `Haendelse` (1..1), `Resultat` (1..1), `Us_procedure` (0..1), `Anatomisk_Lokalisation` (0..1), `Morfologi` (0..1), `Proeveresultat_til_procedure` (0..1), `Proeveresultat_rekvireret_af` (0..1), `Proeveresultat_produceret_af` (1..1) | `@Proevetidspunkt` (påkrævet), `@Svartidspunkt` (valgfri), `@ResultatVaerdi` (valgfri), `@EnhedResultatVaerdi` (valgfri), `@NedreGraense` (valgfri), `@EnhedNedreGraense` (valgfri), `@OevreGraense` (valgfri), `@EnhedOevreGraense` (valgfri), `@Unormalt_resultat` (valgfri), `@Objektreference` (valgfri) | `SUPResultat.xsd` |
| `Resultat` | `SammensatKodetVaerdi` (1..1) | — | `SUPFaellesAttributter.xsd` |
| `Anatomisk_Lokalisation` | `KodetVaerdi` (1..1) | — | `SUPResultat.xsd` |
| `Morfologi` | `KodetVaerdi` (1..1) | — | `SUPResultat.xsd` |
| `Proeveresultat_til_procedure` | — | `@Procedure_identifikation` (påkrævet) | `SUPResultat.xsd` |
| `Proeveresultat_rekvireret_af` | `Rekvirerende_Behandler` (0..1), `Rekvirerende_Enhed` (1..1) | — | `SUPResultat.xsd` |
| `Proeveresultat_produceret_af` | `Producerende_Behandler` (0..1), `Producerende_Enhed` (1..1) | `@ProcedureSted` (valgfri) | `SUPResultat.xsd` |
| `Diagnose` | `Haendelse` (1..1), `Art` (1..1), `DiagnoseKode` (1..1), `Diagnosticering_udfoert_af` (1..1), `Diagnose_afsluttet_af` (0..1), `Diagn_ref_til_proc` (0..1) | `@Diagnosetidspunkt` (påkrævet), `@Afsluttidspunkt` (valgfri) | `SUPVurdering.xsd` |
| `DiagnoseKode` | `SammensatKodetVaerdi` (1..1) | — | `SUPVurdering.xsd` |
| `Diagnosticering_udfoert_af` | `Ansvarlig_Behandler` (0..1), `Ansvarlig_Enhed` (1..1) | `@Diagnosested` (valgfri) | `SUPVurdering.xsd` |
| `Ansvarlig_Behandler` | `AnsvarligPerson` (1..1) | — | `SUPBehandlere.xsd` |
| `Ansvarlig_Enhed` | `Organisatorisk_Enhed` (1..1) | — | `SUPOrganisation.xsd` |
| `Diagnose_afsluttet_af` | `Afsluttende_Behandler` (0..1), `Afsluttende_Enhed` (1..1) | — | `SUPVurdering.xsd` |
| `Diagn_ref_til_proc` | — | `@Procedure_identifikation` (valgfri) | `SUPVurdering.xsd` |
| `Effekt_af_behandling` | `Haendelse` (1..1), `EffektKode` (1..1), `Us_procedure` (0..1), `Beh_procedure` (0..1), `Foerkode` (0..1), `Efterkode` (0..1), `Effekt_af_behandling_observeret_af` (1..1), `Effekt_af_behandling_produceret_af` (1..1), `Behandlings_procedure_der_har_effekten` (0..1) | `@Observationstidspunkt` (påkrævet), `@Behandlingstidspunkt` (valgfri), `@Foervaerdi` (valgfri), `@EnhedFoervaerdi` (valgfri), `@Eftervaerdi` (valgfri), `@EnhedEftervaerdi` (valgfri) | `SUPVurdering.xsd` |
| `EffektKode` | `SammensatKodetVaerdi` (1..1) | — | `SUPVurdering.xsd` |
| `Beh_procedure` | `KodetVaerdi` (1..1) | — | `SUPVurdering.xsd` |
| `Foerkode` | `KodetVaerdi` (1..1) | — | `SUPVurdering.xsd` |
| `Efterkode` | `KodetVaerdi` (1..1) | — | `SUPVurdering.xsd` |
| `Effekt_af_behandling_observeret_af` | `Observerende_Behandler` (0..1), `Observerende_Enhed` (1..1) | `@ObservationsSted` (valgfri) | `SUPVurdering.xsd` |
| `Effekt_af_behandling_produceret_af` | `Producerende_Behandler` (0..1), `Producerende_Enhed` (1..1) | — | `SUPVurdering.xsd` |
| `Behandlings_procedure_der_har_effekten` | — | `@Procedure_identifikation` (påkrævet) | `SUPVurdering.xsd` |
| `Komplikation_bivirkning` | `Haendelse` (1..1), `DiagnoseKode` (1..1), `Procedurekode` (0..1), `Beh_procedure` (0..1), `Komplikation_bivirkning_observeret_af` (1..1), `Komplikation_bivirkning_produceret_af` (1..1), `Procedure_der_kompliceres` (0..1) | `@Observationstidspunkt` (påkrævet), `@Proceduretidspunkt` (valgfri) | `SUPVurdering.xsd` |
| `Komplikation_bivirkning_observeret_af` | `Observerende_Behandler` (0..1), `Observerende_Enhed` (1..1) | `@ObservationsSted` (valgfri) | `SUPVurdering.xsd` |
| `Komplikation_bivirkning_produceret_af` | `Producerende_Behandler` (0..1), `Producerende_Enhed` (1..1) | — | `SUPVurdering.xsd` |
| `Procedure_der_kompliceres` | — | `@Procedure_identifikation` (påkrævet) | `SUPVurdering.xsd` |
| `Maal` | `Haendelse` (1..1), `UnormaltResultat` (1..1), `MaalKode` (1..1), `Problem_diagnose` (0..1), `AfslutningsAarsag` (0..1), `Maal_besluttet_af` (1..1), `Maal_afsluttet_af` (1..1), `Problem_diagnose_som_maalet_gaelder` (0..1) | `@Beslutningstidspunkt` (påkrævet), `@OenskesOpfyldttidspunkt` (valgfri), `@Afslutningstidspunkt` (valgfri), `@Vaerdi` (valgfri), `@EnhedVaerdi` (valgfri), `@NedreGraense` (valgfri), `@EnhedNedregraense` (valgfri), `@OevreGraense` (valgfri), `@EnhedOevreGraense` (valgfri) | `SUPVurdering.xsd` |
| `UnormaltResultat` | `KodetVaerdi` (1..1) | — | `SUPVurdering.xsd` |
| `MaalKode` | `SammensatKodetVaerdi` (1..1) | — | `SUPVurdering.xsd` |
| `Problem_diagnose` | `KodetVaerdi` (1..1) | — | `SUPVurdering.xsd` |
| `Maal_besluttet_af` | `Besluttende_Behandler` (0..1), `Besluttende_Enhed` (1..1) | `@BesluttendeSted` (valgfri) | `SUPVurdering.xsd` |
| `Besluttende_Behandler` | `AnsvarligPerson` (1..1) | — | `SUPBehandlere.xsd` |
| `Besluttende_Enhed` | `Organisatorisk_Enhed` (1..1) | — | `SUPOrganisation.xsd` |
| `Maal_afsluttet_af` | `Afsluttende_Behandler` (0..1), `Afsluttende_Enhed` (1..1) | — | `SUPVurdering.xsd` |
| `Problem_diagnose_som_maalet_gaelder` | — | `@Problem_identifikation` (påkrævet), `@Diagnose_identifikation` (påkrævet) | `SUPVurdering.xsd` |
| `Problem` | `Haendelse` (1..1), `ProblemKode` (1..1), `Aarsag` (0..1), `AfslutningsAarsag` (0..1), `Problem_konstateret_af` (1..1), `Problem_afsluttet_af` (1..1), `Procedure_hvor_man_konstaterer_problem` (0..1) | `@Konstateringstidspunkt` (påkrævet), `@Afslutningstidspunkt` (valgfri), `@Vaerdi` (valgfri), `@Enhed` (valgfri) | `SUPVurdering.xsd` |
| `ProblemKode` | `SammensatKodetVaerdi` (1..1) | — | `SUPVurdering.xsd` |
| `Problem_konstateret_af` | `Konstaterende_Behandler` (0..1), `Konstaterende_Enhed` (1..1) | `@KonstateringsSted` (valgfri) | `SUPVurdering.xsd` |
| `Problem_afsluttet_af` | `Afsluttende_Behandler` (0..1), `Afsluttende_Enhed` (1..1) | — | `SUPVurdering.xsd` |
| `Procedure_hvor_man_konstaterer_problem` | — | `@Procedure_identifikation` (påkrævet) | `SUPVurdering.xsd` |
| `CaveOplysninger` | `Organisatorisk_Enhed` (0..1) | `@Tekst` (påkrævet), `@Dato` (valgfri) | `SUPFaellesAttributter.xsd` |

## Kilder og sporbarhed

- [MedCom, Den Gode SUP-XML Indberetning 3.0, dokumentversion 1.9, 2024-12-02](https://svn.medcom.dk/svn/releases/Standarder/Sundhedsjournal/Dokumentation/SUP.pdf), især side 12–13 og 20–59.
- Lokale SUP 3.0-XSD: `C:\dev\fhir-poc\docs\schemas`; ældre semantik: `C:\dev\fhir-poc\docs\medcom\background_information\bilag_02_v2_2_domaenemodel.md`, `bilag_01_v2_2_elementstruktur.md`, `bilag_13_sup_klassifikationer.md`.
- [Sundhedsdatastyrelsen, LPR2 Fællesindholdet, teknisk del](https://sundhedsdatastyrelsen.dk/media/15216/FI_TEKNISK.pdf).
- [Sundhedsdatastyrelsen, LPR3 bilag 1, model og regler v.5.1](https://cdn1.gopublic.dk/sundhedsdatastyrelsen/Media/638955095266548367/Bilag1_model_og_regler_v.5.1.pdf), [LPR3-vejledning](https://cdn1.gopublic.dk/sundhedsdatastyrelsen/Media/638955095432142451/LPR-indberetningsvejledning_2025_v.5.1.pdf), [bilag 1a RI-specifikationer](https://cdn1.gopublic.dk/sundhedsdatastyrelsen/Media/638955095308962718/Bilag1a_RI-specs_v.5.1.pdf).
