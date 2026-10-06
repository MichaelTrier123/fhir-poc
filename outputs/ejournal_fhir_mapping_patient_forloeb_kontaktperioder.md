# Første mapping: e-journal → FHIR R4

> **Historisk arbejdsudkast.** Projektet er efterfølgende ændret til SUP → FHIR med LPR3-sporbarhed. EPS/IPS-valgene her er ikke længere gældende. Den tidligere vurdering af `Prioritet = Planlagt` som uden direkte betydning var for snæver: LPR3 bruger netop *planlagt* som kontaktprioritet. Se den nye SUP/LPR3-analyse før dette dokument bruges.

**Omfang:** 1 patient i det lokale udtræk, 23 forløb og 46 kontaktperioder.  
**Grundlag:** `docs/fhir_ips_eps_denmark_mapping_guide.md` og de 26 JSON-filer i patientens lokale udtræksmappe.  
**Status:** Analyse af felter og mulige FHIR-mål. Der er endnu ikke dannet eller valideret FHIR-ressourcer. Personnummer, navn og kilde-ID'er er udeladt fra dette dokument.

## Tre FHIR-begreber

- En **ressource** er en afgrænset informationsenhed, fx `Patient` eller `Encounter`.
- En **profil** er et sæt ekstra krav til en ressource. En dansk `Patient` og en EPS-`Patient` kan stille forskellige krav. En profil-URL i `meta.profile` er en påstand om overholdelse og skal først tilføjes efter validering.
- Et **EPS-dokument** er senere en `Composition` i en dokument-`Bundle`. Et sæt gyldige enkeltressourcer er endnu ikke et gyldigt EPS-dokument.

FHIR skelner desuden mellem `id` (teknisk ressource-ID), `identifier` (identifikator fra virkeligheden eller kildesystemet), `status` (livscyklus) og `class` (kontaktens type/ramme).

## Foreløbige ressource- og profilvalg

| Kildeobjekt | Kandidat | Dansk profil | EPS-rolle | Beslutning |
|---|---|---|---|---|
| Personoplysninger i forløbsoversigten | `Patient` | DK Core Patient 3.7.0. MedCom Core Patient 4.0.0 er relevant ved MedCom-kommunikation, men dette er endnu ikke en sådan udveksling. | EPS Patient 1.0.0-ballot kræves i et fremtidigt EPS-dokument. | Kilde har kun personnummer og samlet navn. Profilernes forenelighed er ikke valideret. |
| `Forloeb[]` | `EpisodeOfCare` | FHIR R4-basis; der er ikke identificeret en særskilt EpisodeOfCare-profil i de valgte DK Core/MedCom Core profillister. | Mulig kontekst; ikke automatisk en EPS-sektion. | **Blokeret:** `EpisodeOfCare.status` er obligatorisk, og kilden giver ingen sikker status. |
| `Kontaktperioder[]` | `Encounter` | DK Core Encounter 3.7.0. MedCom Core Encounter 4.0.0 vurderes særskilt, hvis MedCom-udveksling bliver relevant. | Mulig kontekst for kliniske oplysninger; ikke automatisk en EPS-sektion. | `class` kan foreslås for 44 poster; to mangler klasse. `status` er ikke angivet direkte. |

`EpisodeOfCare` er en relation mellem patient og ansvarlig organisation over en periode, som kan rumme flere `Encounter`-poster. Det passer som **arbejdshypotese** til strukturen her: 22 forløb har tilsammen 46 kontaktperioder, mens ét forløb ingen har. Kun 2 af de 22 forløbs starttidspunkter er identiske med deres første kontaktperiodes starttidspunkt; de to niveauer bør derfor ikke sammenlægges.

## Patient og udtræksmetadata

| Kildefelt | Observeret | FHIR-mål / håndtering | Afklaring |
|---|---|---|---|
| `PersonNummer` | Én streng i formatet seks cifre, bindestreg, fire cifre | Kandidat til `Patient.identifier` med dansk CPR-system efter fjernelse af bindestreg. | Bekræft, at det faktisk er et gyldigt CPR-nummer og ikke blot et test- eller lokalt nummer. DK Core CPR-identifikatoren kræver `system = urn:oid:1.2.208.176.1.2` og højst 10 tegn i værdien. |
| `Navn` | Ét samlet navn; ingen særskilte navnefelter | `Patient.name.text`. | Del ikke automatisk i `given` og `family`. MedCom Core Patient kræver officielt efternavn ved MedCom-udveksling; det kan ikke udledes sikkert af denne streng. |
| `NumberOfForloeb` | 23, stemmer med arrayet | Kontrol af udtræk; ikke et `Patient`-felt. | Ingen. |
| `AntalCave` | 0, stemmer med tom cave-liste | Kontrol af udtræk. | Må ikke blive til udsagnet “ingen allergier”. |
| `HarSpaerretForloeb` | `false` | Kilde-/adgangsmetadata. | Undersøg eventuelle regler for spærring før videreformidling. Ikke en klinisk FHIR-status. |
| `UkendtExist` | `false` | Kilde-/kvalitetsmetadata. | Feltets præcise betydning er ikke dokumenteret her. |
| `ErrorMessage` | `null` | API-/fejlmetadata. | Ikke patientdata. |
| `Forloeb` | 23 objekter | Input til mulig `EpisodeOfCare`. | Se næste tabel. |

Kilden giver ikke selvstændige felter for fødselsdato, køn, adresse, kontaktoplysninger eller `Patient.active`. De udfyldes ikke ved gæt eller udledning fra personnummeret.

## `Forloeb[]` felt for felt

| Kildefelt | FHIR-mål / håndtering | Sikkerhed og åbent punkt |
|---|---|---|
| `IdNoegle.Noegle` | Stabil kilde-UUID; kandidat til `EpisodeOfCare.identifier.value` og intern sammenkobling. | Identifikatorens officielle namespace/system mangler. Et lokalt FHIR-`id` kan dannes deterministisk, men er ikke det samme som kilde-`identifier`. |
| `IdNoegle.Database` | Ingen mapping; `null` i alle 23. | Afvent betydning ved andre udtræk. |
| `IdNoegle.VaerdispringNoegle` | Ingen mapping; `null` i alle 23. | Afvent betydning ved andre udtræk. |
| `Identifikation` | Teknisk kildefelt; streng med serialiseret `IdNoegle`-objekt. | Ikke en uafhængig identifikator og bør ikke duplikeres i FHIR. |
| `DatoFra` | Kandidat til `EpisodeOfCare.period.start`. | Bekræft, at det er begyndelsen på behandlingsansvar/forløb og ikke blot oprettelsesdato. |
| `DatoTil` | Kandidat til `EpisodeOfCare.period.end`, hvis til stede. | `null` i alle 23. Det beviser ikke, at forløbene er aktive. |
| `DatoOpdateret` | Bevar som kildens opdateringstid i transformationsspor/proveniens. | Må ikke automatisk sættes i `meta.lastUpdated`, som beskriver FHIR-ressourcens egen version. |
| `AfdelingNavn` | Kandidat til `Organization.name` for afdeling og senere `EpisodeOfCare.managingOrganization`. | Bekræft, at afdelingen faktisk har det ansvar, FHIR-feltet beskriver. |
| `AfdelingKode` | Kandidat til afdelingens `Organization.identifier.value`. | Kodesystem/namespace ukendt. |
| `AfdelingMapningKode` | Alternativ/oversat afdelingskode. | Kodesystem og relation til `AfdelingKode` ukendt; antag ikke SOR uden dokumentation. |
| `SygehusNavn` | Kandidat til overordnet `Organization.name`. | Organisationshierarki skal afklares. |
| `SygehusKode` | Kandidat til overordnet `Organization.identifier.value`; én mangler. | Kodesystem/namespace ukendt. |
| `SygehusMapningKode` | Alternativ/oversat sygehuskode. | Kodesystem og relation til `SygehusKode` ukendt. |
| `Sektor`, `SektorKode` | Organisations-/kildeklassifikation. | Ikke automatisk `EpisodeOfCare.type` eller et klinisk kodefelt. Kodemængde og betydning skal dokumenteres. |
| `DiagnoseKode` | Ingen `Condition` dannes fra dette felt alene. | Alle 23 værdier er `diag_udef`, altså ingen brugbar diagnosekode. |
| `DiagnoseNavn` | Ingen mapping; `null` i alle 23. | Ingen diagnose kan udledes. |
| `AntalDiagnoser` | Kontroltal; sum 1. | Ikke selv en diagnose. |
| `AntalEpikriser` | Kontroltal; sum 2. | Ikke selv et dokument. |
| `AntalKontaktperioder` | Kontroltal; sum 46 og stemmer for hvert forløb. | Bruges til kvalitetskontrol og sammenkobling. |
| `AntalNotater` | Kontroltal; sum 0. | Betyder kun, at dette udtræk ikke har sådanne særskilte poster. |
| `AntalProcedurer` | Kontroltal; sum 0. | Må ikke tolkes som “ingen procedurer udført”. |
| `Privatmarkering`, `Skjult` | Potentielle adgangs-/visningsregler; her `None` og `false`. | Kræver kilde- og adgangssemantik før eventuel FHIR-sikkerhedsmærkning eller eksport. |
| `Vaerdispring`, `Varsling` | Kildens særlige arbejds-/adgangsmetadata; `Varsling` er `null`. | Ingen sikker klinisk FHIR-mapping endnu. |

`EpisodeOfCare.patient` kan referere til den ene `Patient`. `EpisodeOfCare.status` er påkrævet i FHIR R4 og har ikke en generel “unknown”-værdi. Derfor bør der **ikke** dannes færdige `EpisodeOfCare`-ressourcer, før forløbenes livscyklus er afklaret.

## `Kontaktperioder[]` felt for felt

| Kildefelt | FHIR-mål / håndtering | Sikkerhed og åbent punkt |
|---|---|---|
| Filnavnets forløbs-UUID | Knytter kontaktfilen til `Forloeb[].IdNoegle.Noegle`; senere kandidat til `Encounter.episodeOfCare`. | Alle 22 filer matcher et forløb. Referencen laves kun, hvis et gyldigt `EpisodeOfCare` dannes. |
| `Noegle` | Kandidat til `Encounter.identifier.value`; 46 unikke UUID'er. | Kildens identifier-system/namespace skal fastlægges. |
| `DatoFra` | Kandidat til `Encounter.period.start`. | Afklar, om tidspunktet er faktisk kontaktstart eller planlagt tidspunkt. |
| `DatoTil` | Kandidat til `Encounter.period.end`, når angivet. | Mangler for 4 af 46. Ingen slutdato er ikke ensbetydende med `in-progress`. |
| `Status` | `Encounter.class`: `Ambulant` → mulig `AMB`; `Indlagt` → mulig `IMP`. | Dette felt beskriver kontaktens ramme, **ikke** `Encounter.status`. 43 ambulante, 1 indlagt og 2 `null`; de to sidste mangler sikker klasse. Terminologimapping skal dokumenteres. |
| `Prioritet` | Kandidat til LPR3-kontaktprioritet og efter terminologivurdering til `Encounter.priority`. | `Planlagt` forekommer 44 gange. Det betyder ikke `Encounter.status = planned`; præcis FHIR-kodning skal dokumenteres. |
| `EnhedsInformation.Afdeling` | Kandidat til afdelingens `Organization.name`. | Kan blive `Encounter.serviceProvider` efter ansvar og organisation er afklaret. |
| `EnhedsInformation.AfdelingsKode` | Kandidat til afdelingens `Organization.identifier.value`. | Namespace ukendt. |
| `EnhedsInformation.Institution` | Kandidat til overordnet `Organization.name`. | Hierarki/ansvar ukendt. |
| `EnhedsInformation.Kode` | Organisationskode fra kilden. | System og betydning ukendt. |
| `EnhedsInformation.SygehusKode` | Kandidat til overordnet organisationsidentifikator. | System og betydning ukendt. |
| `Fritekst` | Ingen mapping; tom streng i alle 46. | Ingen klinisk tekst at overføre. |
| `AfslutningsAarsag` | Ingen mapping; `null` i alle 46. | Hvis senere udfyldt, kræver det semantisk analyse før `Encounter.hospitalization.dischargeDisposition` eller andet mål vælges. |
| `LaegeligAnsvarlig`, `LaegeligAnsvarligTitel` | Ingen mapping; `null` i alle 46. | Senere mulige `Practitioner`/`PractitionerRole`-referencer efter identitet og rolle er afklaret. |
| `RegistreretAf` | Ingen mapping; `null` i alle 46. | Registrator er ikke nødvendigvis behandler. |
| `Valgt` | Ingen klinisk mapping; `false` i alle 46. | Ligner et kilde-/UI-valgflag; betydning bør bekræftes. |

`Encounter.subject` kan referere til `Patient`. DK Core Encounter kræver `Encounter.status` og `Encounter.class`. Kildens `Status` giver sandsynligvis `class`, men ingen kontakt har en eksplicit FHIR-livscyklusstatus. FHIR R4 tillader `Encounter.status = unknown` som sidste udvej; det er en mulig konservativ transformationsregel, **ikke** en påstand om, at en kontakt stadig er åben. Reglen skal besluttes og dokumenteres, og de to poster uden klasse kræver særskilt håndtering.

## Åbne beslutninger før konvertering

1. **Kildesemantik:** Hvad betyder forløbets `DatoFra` og `DatoTil`, og findes der en kilde til forløbets faktiske status?
2. **Kontaktens tids- og statussemantik:** Er `DatoFra`/`DatoTil` faktisk eller planlagt kontakt? Kan afsluttet status dokumenteres, eller skal `unknown` anvendes?
3. **Identifikatorer og organisationer:** Hvilke officielle systemer hører til `AfdelingKode`, `AfdelingMapningKode`, `SygehusKode`, `SygehusMapningKode` og `EnhedsInformation.Kode`? Hvilken enhed har behandlingsansvar?
4. **Patientidentitet:** Er `PersonNummer` et gyldigt CPR-nummer i udtrækket? Kan officielt efternavn fås som særskilt kildefelt?
5. **Adgangsregler:** Hvordan skal `Privatmarkering`, `Skjult`, `HarSpaerretForloeb` og `Vaerdispring` håndteres ved videreformidling?
6. **EPS-afgrænsning:** Hvilke af disse administrative kontekstressourcer skal overhovedet med i det fremtidige patient summary? De 46 kontakter må ikke automatisk blive til en klinisk EPS-sektion.

## Kilder

- Projektets `fhir_ips_eps_denmark_mapping_guide.md` (baseline 2026-10-06).
- [FHIR R4 Patient](https://hl7.org/fhir/R4/patient.html), [EpisodeOfCare](https://hl7.org/fhir/R4/episodeofcare.html) og [Encounter](https://hl7.org/fhir/R4/encounter.html).
- [DK Core Patient](https://hl7.dk/fhir/core/StructureDefinition-dk-core-patient.html), [CPR Identifier](https://hl7.dk/fhir/core/StructureDefinition-dk-core-cpr-identifier.html) og [DK Core Encounter](https://hl7.dk/fhir/core/StructureDefinition-dk-core-encounter.html), version 3.7.0.
- [MedCom Core Patient](https://medcomfhir.dk/ig/core/StructureDefinition-medcom-core-patient.html) og [MedCom Core Encounter](https://medcomfhir.dk/ig/core/StructureDefinition-medcom-core-encounter.html), version 4.0.0.
- [EPS Patient](https://hl7.eu/fhir/eps/StructureDefinition-patient-eu-eps.html) og [EPS Bundle](https://hl7.eu/fhir/eps/StructureDefinition-bundle-eu-eps.html), version 1.0.0-ballot.
