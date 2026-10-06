# SUP → FHIR R4 med LPR3-understøttelse

**Samlet mappingrapport · version 1.1 · 6. oktober 2026**

## 1. Formål og læsevejledning

Rapporten undersøger, hvordan følgende otte SUP-elementer kan repræsenteres i FHIR R4: `Aflever_patientdata` → `Person` → (`CaveOplysninger`, `Patientforloeb` → `Kontaktperiode`, `Notat`, `Udfoert_procedure`, `Diagnose`). Målet er en FHIR-model, hvor oplysninger og relationer, der er vigtige i LPR3, kan bevares og kontrolleres. Der skal **ikke** dannes eller sendes en LPR3-indberetning.

En **FHIR-ressource** er et standardiseret dataobjekt, fx `Patient` eller `Encounter`. En **profil** indsnævrer reglerne for en ressource til et bestemt anvendelsesområde; DK Core er den danske basisprofil. Et FHIR-dokument kan være teknisk gyldigt og stadig mangle de oplysninger, der skal til for at beskrive et LPR3-forløb. Rapporten vurderer derfor *FHIR-mapping* og *LPR3-dækning* hver for sig.

**Kildestatus:** De uploadede XSD'er beskriver SUP-struktur og tilladte datatyper. Syv lokale e-journal-udtræk i JSON bruges alene som eksempler på faktiske felter og variation. Der foreligger hverken rå SUP-XML eller dokumentation af den transformation, som har skabt JSON. En tilsyneladende kobling mellem XSD-felt og JSON-felt er derfor en **hypotese**, indtil den kan kontrolleres i den oprindelige datakæde. Der gengives ingen persondata.

**Normativ baseline:** FHIR R4 (4.0.1), DK Core 3.7.0 og Sundhedsdatastyrelsens LPR-indberetningsvejledning 2025, version 5.1. LPR3-guiden er anvendt til begreber og dækningskontrol, selv om denne løsning ikke indberetter. De officielle kilder er samlet i [afsnit 13](#13-kilder).

### Sådan læses mappingtabellerne

Den sidste kolonne forklarer konkret, hvad der kan overføres, og hvilke oplysninger eller beslutninger der mangler. En FHIR-kandidat er ikke en færdig mapping, før disse forbehold er afklaret.

## 2. Struktur og foreslåede FHIR-ressourcer

```text
SUP-meddelelse: Aflever_patientdata                  → udtræks-/proveniensmetadata
└─ Person                                             → Patient
   ├─ CaveOplysninger*                                → Flag; AllergyIntolerance kun efter indholdskontrol
   └─ Patientforloeb*                                 → EpisodeOfCare-kandidat
      ├─ Kontaktperiode*                              → Encounter
      ├─ Notat*                                       → DocumentReference-kandidat
      ├─ Udfoert_procedure*                           → Procedure
      └─ Diagnose*                                    → Condition; evt. Encounter.diagnosis
```

Stjernen betyder *nul til mange*. XSD'en tillader også andre hændelser i `Patientforloeb`, men de er uden for denne rapport. `Aflever_patientdata` er meddelelsens indpakning, ikke et klinisk forløb. FHIR-relationerne mellem de valgte ressourcer er forslag, som kun skal oprettes, når kilden faktisk bærer relationen.

**Vigtig modelgrænse:** SUPs `Patientforloeb` kan være et lokalt/teknisk forløb, mens LPR3's *forløbselement* er en selvstændig registreringsenhed med egne regler. XSD-dokumentationen siger ligefrem, at hændelser uden kildeforløb kan samles i et teknisk oprettet forløb. `Patientforloeb` → `EpisodeOfCare` er derfor en nyttig FHIR-kandidat, men ikke et bevis for, at objektet er ét LPR3-forløbselement.

## 3. Fælles regler for alle otte elementer

### 3.1 ID og sporbarhed

`Aflever_patientdata/@Identifikation` identificerer **udtrækket**. `Patientforloeb/@Identifikation` identificerer **forløbet**. `Haendelse/@Identifikation` identificerer **hændelsen** i kontakt, notat, procedure eller diagnose. De må ikke byttes om. Ved FHIR-mapping kan kilde-ID'er bevares som `identifier` på den tilsvarende ressource, med et dokumenteret `Identifier.system` for kilde og ID-type. `Resource.id` er et server-ID, som ikke bør antages identisk med kilde-ID. Meddelelses-ID og transaktionstype hører til et særskilt udtræks-/proveniensspor.

### 3.2 Koder og organisationer

I `SUPKlassifikation.xsd` består `KodetVaerdi` af valgfri `@Kode` og `@Kodetekst` samt `Klassificering/Klassifikation`, hvor `@Forkortelse` og `@Navn` selv er valgfrie. En `SammensatKodetVaerdi` har én `Primaerkode` og nul til mange `Tillaegskode`. Grundreglen er at bevare **kode, tekst, klassifikation, kodens rolle og rækkefølge**. Sæt kun `Coding.system`, når klassifikationens faktiske kodeværk og URI er afklaret. En tekst, der ligner en SKS-kode, er ikke i sig selv bevis for kodeværket. Tillægskoder kan have anden betydning end alternative koder; de må ikke uden videre lægges som parallelle `Coding`-elementer i samme `CodeableConcept`.

`Organisatorisk_Enhed` har valgfri `@KodeType` (`sorkode` eller `sygehusafdelingsnummer`), `@Kode` og navnetekster. En `Organization` kan oprettes på verificeret ID eller som begrænset tekstlig repræsentation. Kun `KodeType=sorkode` giver grundlag for at behandle koden som SOR; ældre afdelingsnummer må ikke omdøbes til SOR. LPR3's krav om ansvarlig SOR-enhed skal kontrolleres særskilt.

### 3.3 Tider og hændelsesmetadata

`SUPdateTime` kan være dato, dato/tid eller de bogstavelige mangelsymboler `_` og `$?`. Symbolerne er **ikke** FHIR-datoer. Datopræcision og eventuel tidszone skal bevares; et klokkeslæt må ikke opfindes for en ren dato. Hvis et påkrævet FHIR-felt ikke kan udfyldes sikkert, skal posten sættes til afklaring frem for at få en opdigtet værdi.

De fire valgte hændelsestyper har et påkrævet `Haendelse`-element (`SUPHaendelse.xsd`):


| SUP-felt | FHIR-kandidat / regel | Konkret vurdering og mangler |
|---|---|---|
| `Haendelse/@Identifikation` | Den tilsvarende ressources `identifier` med kildesystem og type. | Kilde-ID skal have et entydigt namespace; samme ID-tekst kan forekomme i flere systemer. |
| `Haendelse/@FriTekst` | Ressourcespecifik tekst, hvis betydning er kendt; ellers bevar råt. | XSD angiver ikke fritekstens betydning for hver hændelsestype. |
| `Haendelse/@Registreringstidspunkt` | Registreringstid i provenienssporet; ikke automatisk klinisk hændelsestid. | Registreringstid er ikke nødvendigvis tidspunktet for selve behandlingen eller kontakten. |
| `Haendelse/@Tilstede_tidspunkt` | Bevar råt, indtil feltets betydning er fastlagt. | XSD forklarer ikke, hvilken klinisk eller administrativ situation dette tidspunkt angiver. |
| `Haendelse/@Ugyldighedstidspunkt` | Bevar ugyldighedsoplysning; ikke automatisk FHIR `entered-in-error`. | Ugyldighedens årsag mangler; en ugyldig hændelse er ikke nødvendigvis registreret ved en fejl. |
| `Haendelse/HaendelseRegistreretAf/Registrerings_Enhed` og `@Registrerende_behandler` | Registrerende organisation/person i proveniens, hvis identiteter kan opløses. | Enhed og behandler kræver identificerbare aktører, før der kan oprettes FHIR-referencer. |
| `Haendelse/Sikkerhedskode/KodetVaerdi` | Informationssikkerheds-/adgangskontekst; kræver særskilt fortolkning. | Kodeværk og adgangsbetydning mangler; sikkerhedskoden er ikke en klinisk kode. |

`@Registreringstidspunkt`, klinisk starttid og udtrækstid er forskellige tidspunkter. De må ikke samles i ét FHIR-datofelt.

## 4. `Aflever_patientdata` — meddelelsesniveau

**XSD:** `SUPAfleverPatientdataService.xsd` og attributgruppen `AfleverPatientdataAttributter` i `SUPFaellesAttributter.xsd`.


| SUP-felt | FHIR-mapping / beslutning | Konkret vurdering og mangler |
|---|---|---|
| `Person` (påkrævet) | Indgang til én `Patient` og de tilknyttede data. | SUP har én Person pr. udtræk; patientidentiteten vurderes særskilt i næste tabel. |
| `@VersionsNummer` (påkrævet; 2.0–3.0) | Bevar som skema-/meddelelsesversion. Ikke `Patient.meta.versionId`. | Versionsnummeret beskriver SUP-formatet, ikke en version af patientens FHIR-ressource. |
| `@Identifikation` (påkrævet) | Unikt ID for patientdataudtræk; bevar som meddelelses-ID. | Dette ID gælder udtrækket og må ikke bruges som patient- eller forløbs-ID. |
| `@ForsendelsesTid` (påkrævet) | Afsendelsestid for udtræk; ikke klinisk observationstid. | Forsendelsestidspunktet kan ligge længe efter de kliniske hændelser. |
| `@AfsenderSystem` (påkrævet) | Systemproveniens. Kan senere indgå i `Provenance.agent`, hvis provenance-modellen etableres. | Afsendersystemets identitet og rolle skal beskrives, før det kan blive en Provenance-aktør. |
| `@TransaktionsType` (påkrævet: `Opdater`, `OpdaterSpecielt`, `Slet`, `SletSpecielt`) | Synkroniseringsinstruktion; kræver selvstændige regler for opdatering/sletning. Må ikke oversættes direkte til klinisk status. | Opdatering og sletning kræver egne synkroniseringsregler; de siger intet om klinisk status. |

**Læringspunkt:** En SUP-meddelelse og en FHIR `Bundle` er begge indpakninger, men det gør ikke alle SUP-attributter til `Bundle`-felter. Første leverbare FHIR-model kan beskrive patientressourcerne og bevare meddelelsesmetadata i et adskilt ingest-/proveniensspor; valg af FHIR-transaktionsformat kommer senere.

## 5. `Person` — patienten

**XSD:** `Person` i `SUPAfleverPatientdataService.xsd`; `PersonAttributter` i `SUPFaellesAttributter.xsd`. **FHIR-kandidat:** `Patient`, med [DK Core Patient](https://hl7.dk/fhir/core/StructureDefinition-dk-core-patient.html) som foretrukken profil, når dens krav er kontrolleret.


| SUP-felt | FHIR-kandidat / regel | Konkret vurdering og mangler |
|---|---|---|
| `@CPRnummer` (påkrævet) | `Patient.identifier` med korrekt dansk CPR-identifikatorsystem og DK Core-regler. Valider CPR-format og håndter erstatningsnumre/ændret CPR særskilt; XSD garanterer kun en streng. | XSD kræver kun en streng; CPR-gyldighed, erstatningsnumre og ændret CPR skal kontrolleres særskilt. |
| `@Navn` | `Patient.name.text` som sikker tekstlig repræsentation; opdeling i for-/efternavn kræver pålidelig parsing. | Navnet kan bevares som tekst; sikker opdeling i navnekomponenter kræver mere struktur. |
| `@Adresse` | `Patient.address.text`; struktureret adresse kun hvis delene er kendte. | Adressen kan bevares som tekst; vej, nummer, postnummer m.m. kan ikke udledes sikkert. |
| `@Kommunekode`, `@Kommune` | Bopælskommune og tekst; behold eventuelt som del af verificeret adresse-/kommunekontekst. Ingen sikker standardplacering for historisk kommunefelt uden profilbeslutning. | Kommunekode og kommunenavn kræver verificering af betydning og historik før struktureret placering. |
| `@KommuneTilflytningsdato` | Historik om bopælskommune; ikke automatisk `Patient.address.period.start`, da periodens rækkevidde er uklar. | Tilflytning til kommunen er ikke nødvendigvis start på den konkrete adresse. |
| `@Koen` (`M`,`K`,`U`) | `Patient.gender` kan først fastlægges, når SUP-feltets betydning er verificeret mod FHIR administrative gender og DK Core. `U` må ikke gættes til en bestemt kønsoplysning. | Betydningen af M, K og U skal verificeres mod FHIR og DK Core; U må ikke fortolkes ved gæt. |
| `@Foedselsdato` | `Patient.birthDate`, hvis datoen er reel og gyldig; mangelsymboler udelades. | Kun en gyldig dato kan bruges; SUP-mangelsymboler er ikke FHIR-datoer. |
| `@TelefonNummer` | `Patient.telecom` (`system=phone`), hvis værdien er et telefonnummer. | Kontrollér at værdien er et telefonnummer; XSD validerer hverken format eller landekode. |
| `@Paaroerende` | Kan blive `RelatedPerson` eller kontaktoplysning, men én fri tekststreng giver ikke nødvendigvis identitet, relation eller kontaktdata. Bevar råt indtil afklaring. | En fri tekst kan mangle både relationstype, identitet og kontaktoplysninger om den pårørende. |
| `@EgenLaegesNavn`, `@EgenLaegesYdernr`, `@EgenLaegeStartDato` | Mulig `Patient.generalPractitioner` til identificeret læge/organisation; navn og ydernummer skal afstemmes, og startdato er ikke et direkte felt på referencen. | Navn og ydernummer skal pege på samme læge; startdatoen har ingen direkte plads i referencen. |
| `Patientforloeb*`, `CaveOplysninger*` | Relationer til ressourcerne nedenfor. Fravær af `CaveOplysninger` betyder ukendt, ikke “ingen allergier”. | Elementernes placering under `Person` viser patientrelationen; en tom caveliste siger intet sikkert om allergier. |

**LPR3:** Patientidentitet er grundlaget for alle relationer, men CPR-strengen alene løser ikke historik om erstatningsnummer eller CPR-skift. Persondata må ikke udledes af ID'et, fx fødselsdato eller køn, når eksplicit kildeværdi mangler.

## 6. `CaveOplysninger` — advarsler med ukendt klinisk type

**XSD:** `SUPFaellesAttributter.xsd`. **FHIR-kandidat:** `Flag` som generel patientadvarsel. `AllergyIntolerance` er kun relevant, når indholdet dokumenteret beskriver allergi/intolerance. FHIR `Flag` kræver blandt andet `status`, `code` og `subject`; XSD'en har ingen eksplicit status.


| SUP-felt | FHIR-kandidat / regel | Konkret vurdering og mangler |
|---|---|---|
| `@Tekst` (påkrævet) | `Flag.code.text` for en faktisk advarsel; klinisk kategorisering kræver gennemgang af teksten. | Teksten kan beskrive allergi, anden risiko eller administrativ advarsel; typen skal vurderes. |
| `@Dato` (valgfri streng) | Potentiel `Flag.period.start`, men kun hvis datoens betydning og format er kendt. | Dato er en fri streng, og XSD fortæller ikke, om den er oprettelses- eller startdato. |
| `Organisatorisk_Enhed` (valgfri) | Kilde-/ansvarlig organisation; relationen til `Flag.author` kræver semantisk kontrol. | Enheden kan være kilde eller ansvarlig; det er uklart, om den er FHIR-forfatter. |
| Status/aktivitet | **Findes ikke i XSD.** FHIR `Flag.status` kan ikke udfyldes alene fra SUP-skemaet. | SUP-XSD har ingen aktiv/inaktiv-værdi; den påkrævede FHIR-status kræver en anden kilde. |

I de syv JSON-udtræk findes 88 caveposter; alle har `Beskrivelse`, `DatoFra`, `EnhedsInformation` og `Aktiv`, og `Aktiv` er sand i 45 og falsk i 43 poster. `Beskrivelse` ≈ `@Tekst` og `DatoFra` ≈ `@Dato` er plausible, men **ikke dokumenterede** JSON↔SUP-koblinger. Hvis `Aktiv` stammer fra en pålidelig kilde og betyder aktuel advarselsstatus, kan den bruges til `Flag.status=active/inactive`. Det kan ikke besluttes på XSD-grundlaget. Vi bør ikke producere `AllergyIntolerance` ved simpel fritekstsøgning.

## 7. `Patientforloeb` — forløb og LPR3-forløbselement

**XSD:** `SUPPatientforloeb.xsd`. **FHIR-kandidat:** `EpisodeOfCare`. FHIR-ressourcen kræver `status` og `patient`; SUP-XSD'en har ikke et dedikeret statusfelt. `EpisodeOfCare` beskriver desuden ikke automatisk LPR3's forløbslabel og forløbsmarkører.


| SUP-felt | FHIR-kandidat / regel | Konkret vurdering og mangler |
|---|---|---|
| `@Identifikation` (påkrævet) | `EpisodeOfCare.identifier` med entydigt kildesystem. | Kilde-ID skal kombineres med fødesystemet for at undgå sammenfald mellem systemer. |
| `OprindeligAnsvarligEnhed/Organisatorisk_Enhed` (påkrævet) | Potentiel `EpisodeOfCare.managingOrganization`; “oprindelig” ansvarlig er ikke nødvendigvis nuværende LPR3-forløbsansvar. | Oprindeligt ansvar er ikke nødvendigvis aktuelt LPR3-forløbsansvar eller verificeret SOR-enhed. |
| `@Starttidspunkt`, `@Sluttidspunkt` | `EpisodeOfCare.period.start/end`, når værdierne er gyldige; slut mangler naturligt for åbne forløb. | SUP tillader manglende starttid; LPR3 kræver start for et nyt forløbselement. |
| `@SammenknytningsIdentifikation` | XSD-dokumentationen beskriver ID på hovedforløbet for et LPR3-underforløb. Bevar eksplicit relation og opløs den til kildeforløb; `EpisodeOfCare` har ikke et enkelt standardfelt for netop denne hoved-/underforløbsrelation. | Hoved-/underforløbsrelationen har ikke et enkelt tilsvarende EpisodeOfCare-felt. |
| `@Teknisk_forloeb="X"` | Marker forløbet som teknisk kildebeholder. Må ikke automatisk tælles som selvstændigt LPR3-forløbselement. | Et teknisk forløb kan samle løse hændelser og må ikke automatisk blive et LPR3-forløbselement. |
| `@Foedesystem` (påkrævet) | Kilde-/ID-namespace og proveniens; hjælper med at undgå ID-kollision. | Fødesystemet skal bruges som kildemetadata og del af ID-namespace, ikke som klinisk oplysning. |
| `@Udtraekstidspunkt` | Udtrækstid, ikke `EpisodeOfCare.period` eller klinisk afslutning. | Udtrækstidspunktet beskriver dataoverførslen og ikke forløbets tidsramme. |
| `Kontaktperiode`, `Notat`, `Udfoert_procedure`, `Diagnose` | Hændelser under kildeforløbet. Opret kun de FHIR-referencer, som den konkrete relation understøtter. | SUP-tilknytning til forløb dokumenterer ikke i sig selv relationer mellem hændelser og kontakter. |
| Forløbsstatus, forløbslabel, startmarkør, afslutningsmåde, eventuel referencetype | Ikke dedikerede felter i dette XSD-element. Kan ikke rekonstrueres sikkert uden andre SUP-klasser/kodeværker eller upstream-data. | Forløbsstatus, label, startmarkør m.m. kræver yderligere data; FHIR- og LPR3-dækning er derfor ufuldstændig. |

**Eksempel uden patientdata:** Et SUP-forløb med startdato og ansvarlig enhed kan give en `EpisodeOfCare`-kandidat med `identifier`, `period` og `managingOrganization`. Det siger ikke, om forløbet har den LPR3-label og den startmarkør, der kræves i LPR3. FHIR-validering alene besvarer ikke det spørgsmål. Hvis status ikke kan dokumenteres, kan der heller ikke uden videre dannes en fuldgyldig `EpisodeOfCare`.

LPR3 kræver et starttidspunkt ved oprettelse af forløbselement; i SUP er `@Starttidspunkt` valgfrit. Også denne forskel skal fremgå af en dækningskontrol. Se LPR3-vejledningens kapitel 4, især afsnit 4.1.5.4 og 4.1.7.

## 8. `Kontaktperiode` — patientkontakt

**XSD:** `SUPKontaktperiode.xsd`. **FHIR-kandidat:** `Encounter`, eventuelt [DK Core Encounter](https://hl7.dk/fhir/core/StructureDefinition-dk-core-encounter.html). FHIR R4 kræver både `Encounter.status` og `Encounter.class`. Disse to felter udtrykker forskellige ting: *livscyklus* versus *kontaktklasse*.


| SUP-felt | FHIR-kandidat / regel | Konkret vurdering og mangler |
|---|---|---|
| `Haendelse` (påkrævet) | Fælles hændelsesregler i §3; kilde-ID → `Encounter.identifier`. | ID kan bevares; øvrige Haendelse-felter kræver de fælles fortolkninger i §3. |
| `@StartTidspunkt` (påkrævet), `@Afslutningstidspunkt` | `Encounter.period.start/end` ved gyldige datoer. Afslutningstidspunkt alene beviser ikke `Encounter.status=finished`, før kildens livscyklusregler er kendt. | Tiderne kan overføres, men slutdato alene fastslår ikke Encounter-livscyklusstatus. |
| `ForloebsStatus/KodetVaerdi` (påkrævet) | Kildekode skal fortolkes med sin klassifikation. Kan dække kontakttype/administrativ status; ingen universel mapping til `Encounter.status`, `class` og LPR3-kontakttype. | Kodeværket er ukendt, og feltet kan ikke bruges ens til status, klasse og LPR3-kontakttype. |
| `Indikation/KodetVaerdi` | Potentiel kontaktårsag (`Encounter.reasonCode`) efter kode-/semantikkontrol. | Det skal bekræftes, at koden beskriver årsagen til netop denne kontakt. |
| `Prioritet/KodetVaerdi` | `Encounter.priority` hvis den er **kontaktens** akut-/planlagt-prioritet; krydswalk til DK Core-kodeværdi skal verificeres. | Kontrollér at dette er kontaktprioritet, og verificér krydswalk til dansk FHIR-kodeværk. |
| `AfslutningsAarsag/KodetVaerdi` | Mulig `Encounter.hospitalization.dischargeDisposition` i relevante tilfælde; “afslutningsårsag” er bredere, så ingen generel regel. | En generel afslutningsårsag er ikke nødvendigvis en udskrivningsdisposition. |
| `Laegelig_Ansvarlig_for_kontaktperiode/Laegelig_kontaktansvarlig_Enhed` (påkrævet) | `Encounter.serviceProvider` hvis dette faktisk er kontaktansvarlig enhed; kontrollér SOR-ID. | Rollen som kontaktansvarlig og en eventuel SOR-kode skal kontrolleres. |
| `Laegelig_Ansvarlig_for_kontaktperiode/Laegelig_ansvarlig_behandler` (valgfri) | `Encounter.participant.individual` med rolle/tid, hvis person kan identificeres. | Behandlerens ID, deltagelsesrolle og eventuelle tidsperiode mangler eller skal verificeres. |
| `Laegelig_Ansvarlig_for_kontaktperiode/@Stamsted` (påkrævet) | Bevar; kan være lokalitetsoplysning, men er ikke automatisk `Encounter.location`. | Stamstedets betydning og identitet skal afklares før en Location-reference. |
| `Kontaktperiode_afsluttet_af` (valgfri behandler, påkrævet enhed når elementet findes) | Proveniens/afsluttende ansvar; ingen direkte generel Encounter-egenskab. | Afsluttende aktør er ikke et generelt Encounter-felt; behold ansvarshistorikken. |
| `Rekv_enhed_til_kontaktperiode` (valgfri behandler, påkrævet enhed når elementet findes) | Rekvirerende enhed; mulig relation til bestilling, men bestillingsressourcen ligger uden for afgrænsningen. | Bestillingen er uden for de otte klasser; derfor kan henvisningsrelationen ikke opløses endnu. |
| `Henvisning/@Rekvisition_Identifikation`, `Kontaktgrundlag/@Ordination_Identifikation` | Bevar kilde-ID'er; kan senere opløses mod `ServiceRequest` eller anden bestilling. Opret ikke en opdigtet reference. | Målressourcerne skal findes på ID, før FHIR-referencer til bestilling kan dannes. |
| Relation til `Patientforloeb` | `Encounter.episodeOfCare` når forløbet er repræsenteret og relationen holder. | SUP-forløbet kan være teknisk; kontrollér hvilken EpisodeOfCare kontakten skal pege på. |

**LPR3-konflikt:** LPR3 skelner mellem kontakttype og prioritet. Fx er *ambulant* og *indlagt* ikke to LPR3-kontakttypekoder; de hører begge under fremmøde, mens akut/planlagt er et andet felt. I JSON ses `Status` både som `Ambulant`, `Indlagt`, `Virtuel kontakt`, `Død`, `Admission`, `Outpatient`, `EpisodeOfCareStatus...Active/Finished` og enkelte åbenlyst fejlplacerede værdier. `Status` er udfyldt i 958 af 1.434 kontaktposter; `Prioritet` i 819. Én simpel `Status`→`Encounter.status`-regel ville derfor give fejl. Kodeværk, feltoprindelse og variant skal afklares pr. post.

## 9. `Notat` — klinisk tekst eller dokumentreference

**XSD:** `SUPNotat.xsd`. **FHIR-kandidat:** `DocumentReference` for et selvstændigt notat/dokument, hvis dokumentstatus og indhold kan bestemmes. [DK Core Minimal DocumentReference](https://hl7.dk/fhir/core/StructureDefinition-dk-core-minimaldocumentreference.html) er rettet mod dokumentdeling; den vælges kun, hvis anvendelsen og alle profilkrav passer. `Composition` er en anden model for et egentligt FHIR-dokument og vælges ikke alene fordi teksten kaldes et notat.


| SUP-felt | FHIR-kandidat / regel | Konkret vurdering og mangler |
|---|---|---|
| `Haendelse` (påkrævet) | Kilde-ID → `DocumentReference.identifier`; fælles metadataregler i §3. | Notatets kilde-ID kan bevares, mens hændelsesmetadata kræver særskilt fortolkning. |
| `@Overskrift` | `DocumentReference.description` eller titel i dokumentmetadata, hvis betydningen fastholdes. | Det skal afklares, om overskriften er notatets titel eller blot en visningstekst. |
| `@KonstateringsTidspunkt` (påkrævet) | Potentiel `DocumentReference.context.period`/klinisk dato; `DocumentReference.date` er registreringstid, ikke automatisk konstateringstid. | Konstateringstid er ikke nødvendigvis dokumentets oprettelses- eller registreringstid. |
| `@Broedtekst` | Kan lægges i `DocumentReference.content.attachment.data` som korrekt kodet tekst med `contentType`, hvis notatet er selvstændigt indhold og må gengives. | Indholdet kræver dokumentbeslutning, korrekt MIME-type og kodning; tekst må ikke blot kopieres til metadata. |
| `@Objektreference` | `DocumentReference.content.attachment.url` kun hvis referencen faktisk kan hentes og dens indholdstype/adgang er kendt. | Referencen skal kunne opløses med kendt adgang og indholdstype, før den kan blive Attachment.url. |
| `Notat_type/KodetVaerdi` | `DocumentReference.type` efter terminologikontrol. | Notattypen skal kobles til et verificeret kodeværk, før den kan angives som FHIR-type. |
| `Notat_rekvireret_af` | Bestiller-/proveniensrelation; kan ikke automatisk blive `author`. | Rekvirent og forfatter har forskellige roller; rekvirenten må ikke sættes som author uden belæg. |
| `Notat_produceret_af/Producerende_Behandler` (valgfri) | `DocumentReference.author` ved verificeret identitet. | Behandlerens identitet og faktiske forfatterrolle skal kunne eftervises. |
| `Notat_produceret_af/Producerende_Enhed` (påkrævet) | `DocumentReference.author` som organisation, hvis enheden faktisk er forfatter/udsteder. | Producerende enhed kan være organisatorisk udsteder snarere end tekstens forfatter. |
| `Notat_produceret_af/@ProcedureSted` | Lokal kontekst; ingen sikker direkte mapping. | ProcedureSted er en fri tekst uden påvist FHIR-location eller anden standardplacering. |
| `Notat_til_procedure/@Udfoert_procedure_identifikation` | Bevar eksplicit notat→procedure-relation; FHIR-repræsentation besluttes, når dokumentmodellen fastlægges. | Relationen kan bevares på ID, men den valgte dokumentmodel bestemmer FHIR-linket. |
| `Procedurekode/KodetVaerdi` | Kode, der er knyttet til notatet; må ikke automatisk oprette en udført `Procedure`. | Notatets procedurekode beviser ikke, at en selvstændig procedure blev udført. |
| Dokumentstatus | `DocumentReference.status` er påkrævet i FHIR, men findes ikke som dedikeret SUP-notatfelt. | DocumentReference kræver status; notatets XSD har ikke et felt, der fastslår den. |

I JSON findes 1.917 notatposter; 1.915 har `Broedtekst`. Det bekræfter, at tekstindhold er almindeligt, men ikke at alle poster er selvstændige dokumenter med kendt dokumentstatus.

## 10. `Udfoert_procedure` — faktisk udført handling

**XSD:** `SUPProcedureProces.xsd`. **FHIR-kandidat:** `Procedure` i FHIR R4. `Procedure.status` og `Procedure.subject` er påkrævede; SUP-elementet hedder *udført*, men det alene fastlægger ikke alle livscyklus- og fejlregistreringstilstande.


| SUP-felt | FHIR-kandidat / regel | Konkret vurdering og mangler |
|---|---|---|
| `Haendelse` (påkrævet) | Kilde-ID → `Procedure.identifier`; fælles regler i §3. | Procedure-ID kan bevares, men hændelsesmetadata skal fortolkes som beskrevet i §3. |
| `@Starttidspunkt` (påkrævet), `@Sluttidspunkt` | `Procedure.performedDateTime` eller `performedPeriod`, afhængigt af kildepræcision og faktisk forløb. | Brug et tidspunkt eller en periode efter kildepræcision; mangelsymboler kan ikke overføres. |
| `@Afslutningstidspunkt` | Administrativ afslutning; ikke automatisk slut på `performedPeriod`. | Administrativ afslutning kan afvige fra tidspunktet, hvor proceduren faktisk sluttede. |
| `@Objektreference` | Bevar som dokument-/objektreference; sikker FHIR-relation kræver måltype og adgang. | Objektreferencens måltype, adgang og betydning er ikke defineret. |
| `Art/KodetVaerdi` | Kildens procedureart; mulig `Procedure.category` efter kode-/semantikkontrol. | Procedureartens kodeværk og forhold til FHIR-kategori er ikke fastlagt. |
| `Procedure_kode/SammensatKodetVaerdi` (påkrævet) | Primær kode → `Procedure.code` efter verificeret klassifikation; tillægskoder skal modelleres efter deres faktiske betydning. | Primærkode og tillægskoder har forskellige roller; klassifikation og tillægskoders betydning mangler. |
| `Indikation/KodetVaerdi` | Potentiel `Procedure.reasonCode`, hvis indikationen beskriver årsag. | Indikationen skal faktisk være en årsag til proceduren, før den kan bruges som reasonCode. |
| `Prioritet/KodetVaerdi` | Procedureprioritet; `Procedure` har ikke samme direkte prioritetselement som `Encounter`. Bevar råt eller brug aftalt udvidelse efter analyse. | Procedure har intet almindeligt prioritetselement; en eventuel udvidelse kræver fælles beslutning. |
| `AfslutningsAarsag/KodetVaerdi` | Mulig relevans for `Procedure.statusReason`, men kun hvis årsagen faktisk forklarer procedurens status. | Afslutningsårsagen kan vedrøre administration og ikke procedurens FHIR-status. |
| `Udloesende_haendelse` med ID til rekvisition/ordination/planlagt procedure | Mulig `Procedure.basedOn` efter opløsning af bestillingsressource; disse tre klasser mappes ikke her. | De nævnte bestillingsklasser mappes ikke her; deres ID kan endnu ikke blive sikre FHIR-referencer. |
| `Udfoert_procedure_rekvireret_af` (valgfri) | Rekvirerende person/enhed; ikke automatisk udførende. | Rekvirenten bestiller proceduren og må ikke forveksles med den udførende aktør. |
| `Udfoert_procedure_produceret_af/Producerende_Behandler` (valgfri) | `Procedure.performer.actor` ved verificeret identitet. | Identificér behandleren og bekræft udførende rolle, før performer-referencen dannes. |
| `Udfoert_procedure_produceret_af/Producerende_Enhed` (påkrævet) | `Procedure.performer.actor` som organisation, hvis den faktisk udførte; kontrollér LPR3-procedureansvar/SOR særskilt. | Bekræft at enheden udførte proceduren; LPR3-procedureansvar kræver desuden korrekt SOR. |
| `Udfoert_procedure_produceret_af/@Udfoerelsessted` | Potentiel `Procedure.location`, når stedet kan identificeres. | Fri tekst om udførelsessted skal opløses til et identificeret sted før Location-reference. |
| `Udfoert_procedure_afsluttet_af` (valgfri) | Afsluttende aktør/enhed i proveniens; ikke automatisk performer. | Afsluttende aktør kan være administrativ og må ikke automatisk blive performer. |
| `Procedure.status`, kontakt-/forløbslink | Status kræver livscyklusregel. `Procedure.encounter` kræver dokumenteret kontaktrelation; ellers bevar kildeforløbsrelationen separat. | FHIR-status og relation til kontakt eller forløb mangler en verificeret kilde-/livscyklusregel. |

I de syv JSON-udtræk er der 938 procedureposter; `ProcedureArt` er udfyldt i 842. LPR3 tillader procedure på en kontakt og i visse tilfælde direkte på et forløbselement uden patientkontakt. Derfor må en procedure ikke tvinges på en vilkårlig `Encounter`. En FHIR `Procedure` med tom `encounter` kan være korrekt, men relationen til kildeforløbet skal så stadig bevares i den samlede løsning.

## 11. `Diagnose` — klinisk diagnose og kontaktdiagnose

**XSD:** `SUPVurdering.xsd`. **FHIR-kandidat:** `Condition`, med [DK Core Condition](https://hl7.dk/fhir/core/StructureDefinition-dk-core-condition.html) når profilen passer. DK Core beskriver LPR3-diagnoser som `encounter-diagnosis`; regionale forløbsdiagnoser kan være `problem-list-item`. Det er netop derfor, at en SUP-diagnose under `Patientforloeb` ikke automatisk er en LPR3-kontaktdiagnose.


| SUP-felt | FHIR-kandidat / regel | Konkret vurdering og mangler |
|---|---|---|
| `Haendelse` (påkrævet) | Kilde-ID → `Condition.identifier`; fælles regler i §3. | Diagnose-ID kan bevares, men hændelsesmetadata skal fortolkes efter de fælles regler i §3. |
| `@Diagnosetidspunkt` (påkrævet) | Potentiel `Condition.recordedDate` eller `onset[x]` afhængigt af om kilden mener registrering, konstatering eller sygdomsstart. Ingen automatisk oversættelse. | Det er uklart, om tiden angiver registrering, diagnosticering eller sygdommens begyndelse. |
| `@Afsluttidspunkt` | Kildediagnosens afslutning; ikke automatisk `Condition.abatement[x]`, der kan betyde at sygdommen ophørte. | En afsluttet registrering betyder ikke nødvendigvis, at patientens sygdom er ophørt. |
| `Art/KodetVaerdi` (påkrævet) | Klassifikation af diagnoseart. Kan muligvis skelne aktions-, bi- og henvisningsdiagnose; kræver kodet, kildeverificeret regel. | Kodeværk og diagnoseart skal verificeres; en henvisningsdiagnose er ikke automatisk kontaktdiagnose. |
| `DiagnoseKode/SammensatKodetVaerdi` (påkrævet) | Primær diagnosekode → `Condition.code` med verificeret kodeværk. Tillægskoder bevares med særskilt rolle. | Primærkode og tillægskoder skal holdes adskilt; kodeværk og tillægskodernes betydning mangler. |
| `Diagnosticering_udfoert_af/Ansvarlig_Behandler` (valgfri) | Potentiel `Condition.asserter`, hvis vedkommende faktisk har stillet diagnosen. | Behandlerens identitet og rolle som den, der faktisk stillede diagnosen, skal kontrolleres. |
| `Diagnosticering_udfoert_af/Ansvarlig_Enhed` (påkrævet) | Diagnostisk ansvarlig organisation; bevar, men der er ikke et generelt direkte `Condition`-felt for enheden. | Enheden skal bevares som ansvarskontekst; Condition har ikke et enkelt felt for denne rolle. |
| `Diagnosticering_udfoert_af/@Diagnosested` | Sted for diagnosticering; skal fortolkes før evt. location-relation. | Diagnosested er fri tekst; betydning og sted-ID mangler før en struktureret relation. |
| `Diagnose_afsluttet_af` (valgfri) | Afsluttende person/enhed i proveniens; ikke automatisk tegn på klinisk helbredelse. | Afsluttende enhed/person dokumenterer registreringsansvar, ikke nødvendigvis helbredelse. |
| `Diagn_ref_til_proc/@Procedure_identifikation` | Bevar eksplicit diagnose→procedure-relation; FHIR-link kræver fastlagt betydning og målressource. | Relationens kliniske betydning og målprocedurens identitet skal fastlægges før et FHIR-link. |
| Kontaktkobling og rolle som aktions-/bidiagnose | Kun når præcis `Encounter` og kodebetydning er sikker: `Condition.encounter`, `Condition.category=encounter-diagnosis` og evt. `Encounter.diagnosis` med dokumenteret rolle. | SUP har ingen sikker diagnose→kontakt-kobling; uden den kan LPR3-rollen ikke fastslås. |

I JSON findes 423 diagnoseposter; `DiagnoseArt` er udfyldt i 390, `DiagnoseKode` i 421. Observerede tekstværdier omfatter `Aktionsdiagnose`, `Bidiagnose`, `Henvisningdiagnose` og `Henvisningsdiagnose` samt sjældne afvigelser. De to henvisningsstavemåder og de blandede værdier viser behov for et godkendt kodeværkskrydswalk. LPR3 kræver én aktionsdiagnose på afsluttet kontakt og kan have bidiagnoser; det kan ikke kontrolleres ved alene at tælle alle diagnoser under et SUP-forløb.

LPR3 har desuden betingede krav, fx sideangivelse for visse diagnoser. Det valgte SUP-`Diagnose` har ikke et dedikeret sidefelt. En eventuel tillægskode må kun fortolkes som sideangivelse, hvis dens kodeværk og rolle er dokumenteret. Se LPR3-vejledningens kapitel 6, især afsnit 6.1.2–6.1.5.

## 12. LPR3-dækning og næste beslutninger

### 12.1 Dækningsmatrix

| LPR3-spørgsmål | Hvad de otte SUP-elementer giver | Status for denne rapport |
|---|---|---|
| Hvem er patienten? | `Person/@CPRnummer`, demografi. | Delvist: identitetsregler og CPR-historik mangler. |
| Hvilket forløbselement? | `Patientforloeb` med ID, tider, oprindelig enhed, evt. hovedforløbs-ID. | Delvist: tekniske forløb, nuværende SOR-ansvar, forløbslabel, startmarkør og referencetype kræver ekstra kilde/regel. |
| Hvilken kontakt? | `Kontaktperiode` med hændelses-ID, tider, ansvarlig enhed, statuskode og evt. prioritet. | Delvist: LPR3-kontakttype, prioritet og FHIR-status kræver verificeret krydswalk. |
| Hvilke kontaktdiagnoser? | `Diagnose` med kode, art og tid under et forløb. | Utilstrækkeligt: sikker diagnose→kontakt-kobling og aktions-/bidiagnoseregel mangler. |
| Hvilke udførte procedurer? | `Udfoert_procedure` med kode, tid og producerende enhed. | Delvist: procedure→kontakt/forløb og koderoller kræver afklaring. |
| Kliniske noter og cave? | `Notat` og `CaveOplysninger`. | FHIR-kandidater findes; dokument-/advarselsstatus og klinisk type mangler i XSD. |

**Ingen LPR3-konformitetskonklusion endnu.** Dækningen ovenfor er en gap-analyse. Den dokumenterer, hvad der kan bevares, og hvad der ikke kan afgøres med de otte klasser og de tilgængelige JSON-udtræk. Der skal ikke konstrueres forløbslabel, startmarkør, kontakttype, SOR-kode eller aktionsdiagnose for at få et grønt valideringsresultat.

### 12.2 Beslutningslog til næste iteration

| Prioritet | Beslutning / dokumentation der mangler | Hvorfor det blokerer |
|---|---|---|
| 1 | Beskriv transformationen fra SUP-XML eller kildesystem til JSON, med konkrete eksempler på `Status`, `Prioritet`, `DiagnoseArt`, `Aktiv` og relationer. | Vi kan ellers ikke skelne kildeværdier fra API-afledte værdier. |
| 1 | Fastlæg identiteter og kardinalitet: kilde-ID for patient, forløb, kontakt, procedure, notat og diagnose; tidsmæssig og eksplicit relation mellem diagnose/procedure og kontakt. | Nødvendigt for stabile FHIR-referencer og LPR3-kontroldækning. |
| 1 | Fremskaf kodeværker/kodelister og godkend krydswalk for forløbsstatus, kontakttype, prioritet, diagnoseart og procedure-/diagnosekoder. | Nødvendigt for både FHIR-koder og LPR3-begreber. |
| 1 | Find kilde til forløbslabel, startmarkør, afslutningsmåde, aktuel SOR-enhed og forløbsreferencetype. | Disse LPR3-felter kan ikke udledes sikkert af de afgrænsede XSD-elementer. |
| 2 | Beslut hvilke SUP-forløb der er tekniske beholdere, og om ét SUP-forløb kan dække flere LPR3-forløbselementer. | Afgør om `EpisodeOfCare` er 1:1, 1:mange eller kun en visning. |
| 2 | Fastlæg statusregler for `EpisodeOfCare`, `Encounter`, `Procedure`, `DocumentReference` og `Flag`. | Flere af disse FHIR-ressourcer har påkrævet status uden et entydigt SUP-felt. |
| 2 | Vælg dokumentmodel for notater og fortolkning af cave, inkl. adgang til `Objektreference`. | Afgør om `DocumentReference`/`Flag` kan produceres sikkert. |

### 12.3 Anbefalet produkt og kontrolflow

**Denne ene rapport er beslutningsgrundlaget.** Ved næste iteration opdateres tabellerne med kolonner for verificeret JSON-sti, kodeværk/URI, valgt profil, regel-ID og et anonymiseret eksempel. Hver regel får en af tilstandene **afklaret**, **betinget** eller **blokeret**. Der oprettes ikke et separat mappingregneark i denne omgang.

En senere mapper bør udføre fire adskilte kontroller: (1) XSD-/inputvalidering og mangelsymboler, (2) FHIR R4- og profilvalidering, (3) LPR3-dækning efter de dokumenterede begrebsregler, (4) tabsrapport for felter og relationer, der ikke kunne repræsenteres. Kontrol 3 er en **understøttelseskontrol**, ikke en LPR3-indberetning eller en attest for indberetningsklarhed.

## 13. Kilder

**Lokale XSD'er:** `C:\dev\fhir-poc\docs\schemas\SUPAfleverPatientdataService.xsd`, `SUPFaellesAttributter.xsd`, `SUPPatientforloeb.xsd`, `SUPKontaktperiode.xsd`, `SUPNotat.xsd`, `SUPProcedureProces.xsd`, `SUPVurdering.xsd` samt fælles `SUPHaendelse.xsd`, `SUPKlassifikation.xsd`, `SUPOrganisation.xsd`, `SUPBehandlere.xsd`, `SUPTyper.xsd`. Eksempeltal er optalt i syv lokale e-journal-JSON-udtræk; de er ikke normative kilder til SUP-semantik.

- [Sundhedsdatastyrelsen: LPR3, vejledninger og bilag](https://sundhedsdatastyrelsen.dk/indberetning/patientregistrering/indberetning-lpr3) og [LPR-indberetningsvejledning 2025 v. 5.1](https://cdn1.gopublic.dk/sundhedsdatastyrelsen/Media/638955095432142451/LPR-indberetningsvejledning_2025_v.5.1.pdf).
- [FHIR R4: Encounter](https://hl7.org/fhir/R4/encounter.html), [Procedure](https://hl7.org/fhir/R4/procedure.html), [Condition](https://hl7.org/fhir/R4/condition.html), [DocumentReference](https://hl7.org/fhir/R4/documentreference.html), [Flag](https://hl7.org/fhir/R4/flag.html) og [EpisodeOfCare](https://hl7.org/fhir/R4/episodeofcare.html).
- [DK Core 3.7.0: Patient](https://hl7.dk/fhir/core/StructureDefinition-dk-core-patient.html), [Encounter](https://hl7.dk/fhir/core/StructureDefinition-dk-core-encounter.html), [Condition](https://hl7.dk/fhir/core/StructureDefinition-dk-core-condition.html) og [Minimal DocumentReference](https://hl7.dk/fhir/core/StructureDefinition-dk-core-minimaldocumentreference.html).
