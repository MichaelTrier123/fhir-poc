# Opgave 2a — LPR2 og LPR3: modeloversigt og forskelle

**Arbejdsrapport · 6. oktober 2026**

## Formål og læsenøgle

Denne rapport beskriver LPR2 og LPR3, før SUP og FHIR lægges ovenpå. Den skal være et stabilt sammenligningsgrundlag for opgave 2b (SUP kontra LPR2/LPR3) og 2c (ny, bredere FHIR-standard).

**Patientforløb** betyder her det faktiske kliniske forløb. **Forløbselement** er et bestemt objekt i LPR3. En **kontakt** i LPR2 og en **Kontakt** i LPR3 er ikke automatisk samme tidsenhed. Det er analysens vigtigste skel.

## 1. Kildegrundlag og præcis afgrænsning

| Kilde | Hvad den bruges til |
|---|---|
| [Fællesindhold 2019, vejledning](https://sundhedsdatastyrelsen.dk/media/15215/FI_VEJLEDNING.pdf), især kap. 2–3 | LPR2-begreber og registreringsprincipper. |
| [Fællesindhold 2019, teknisk del](https://sundhedsdatastyrelsen.dk/media/15216/FI_TEKNISK.pdf), især afsnit 5.2 og 5.4 | LPR2's indberetningsstrukturer og felter. Dokumentet beskriver især kontakter afsluttet efter 31. december 2018 samt uafsluttede kontakter. |
| [LPR-indberetningsvejledning 2025 v. 5.1](https://cdn1.gopublic.dk/sundhedsdatastyrelsen/Media/638955095432142451/LPR-indberetningsvejledning_2025_v.5.1.pdf) | LPR3's begreber og anvendelse. |
| [Bilag 1: Logisk datamodel og regler v. 5.1](https://cdn1.gopublic.dk/sundhedsdatastyrelsen/Media/638955095266548367/Bilag1_model_og_regler_v.5.1.pdf) | LPR3-klasser, egenskaber, UML-relationer og modelnære regler. |
| [Bilag 1a: Resultatindberetninger](https://cdn1.gopublic.dk/sundhedsdatastyrelsen/Media/638955095308962718/Bilag1a_RI-specs_v.5.1.pdf) og [Bilag 1b: Forløbsmarkører](https://cdn1.gopublic.dk/sundhedsdatastyrelsen/Media/638955095345173512/Bilag1b_forl%C3%B8bsmark%C3%B8rer_v.5.1.pdf) | Specialiseringer af LPR3's generelle struktur. |

**Hvad “fuld model” betyder i denne rapport:** Alle indberetningsstrukturer og deres felter i den sidste tekniske LPR2-udgave samt alle navngivne klasser og egenskaber i LPR3's logiske model er med. De mange kodeafhængige valideringsregler og alle aktuelle SKS-værdier gengives ikke linje for linje. De er et selvstændigt, versionsstyret lag. Hvis vores SUP-data indeholder ældre LPR2-årgange, skal de relevante historiske Fællesindhold-udgaver undersøges særskilt. Denne rapport er derfor ikke en rekonstruktion af samtlige LPR2-årgange.

Sundhedsdatastyrelsens [oversigt over LPR3-dokumentation](https://sundhedsdatastyrelsen.dk/indberetning/patientregistrering/indberetning-lpr3) angiver v. 5.1 som den offentliggjorte vejledning. Siden omtaler også kontakttypen hjemmehospitalisering, oprettet i juli 2026, men understreger, at den først må bruges, når de endelige aftaler træder i kraft ved næste publicering.

## 2. Grundmodellen: to måder at skære samme virkelighed op på

    LPR2: Patient → kontakt → besøgsdatoer, diagnoser, procedurer, ventestatus

    LPR3: Patient → forløbselement → kontakter, markører, henvisninger,
                                 procedurer og resultatindberetninger
                              ↔ andre forløbselementer via typede referencer

LPR2-vejledningen siger udtrykkeligt, at indberetningen sker som patientkontakter: en indlæggelse eller en ambulant kontakt. Et lokalt patientforløb opdeles i sådanne kontakter ved indberetning. Én ambulant kontakt kan rumme flere besøg og ydelser over længere tid. Diagnoserne beskriver kontakten. Se [LPR2-vejledningens kapitel 2](https://sundhedsdatastyrelsen.dk/media/15215/FI_VEJLEDNING.pdf).

LPR3 indfører **Forløbselement** som overordnet, ansvarsbærende ramme. Ét faktisk sygdoms- eller helbredsforløb kan omfatte flere successive eller samtidige forløbselementer. Hvert element har en ansvarlig SOR-enhed, et forløbslabel og et tidsrum. Konkrete kontakter, markører og andre hændelser ligger i eller knyttes til denne ramme. Se [LPR3 bilag 1, klasser 02–06](https://cdn1.gopublic.dk/sundhedsdatastyrelsen/Media/638955095266548367/Bilag1_model_og_regler_v.5.1.pdf).

## 3. Hele LPR2's tekniske indberetningsstruktur

Følgende er en struktur- og feltfortegnelse, ikke en FHIR-mapping. Feltnavnene følger [Fællesindholdets tekniske del, afsnit 5.2 og 5.4](https://sundhedsdatastyrelsen.dk/media/15216/FI_TEKNISK.pdf).

| Struktur | Betydning | Felter i 2019-layoutet |
|---|---|---|
| **INDUD** | Kontaktens grundpost | SGH sygehus; AFD afdeling; PATTYPE patienttype; CPRNR; STARTDATO; INDLÆGTIME; MIANSKA startminut; KOMNR bopælskommune; HENVISDTO; INDMÅDE; HENVISNMÅDE; SLUTDATO; UDTIME; AFSLUTMÅDE; UDSKRTILSGH; KONTÅRS; FRITVALG; HENVSGH. |
| **SKSKO** | Fælles kodet struktur for diagnoser, procedurer og tillægskoder | ART, KODE, PROCDTO, PROCAFD, PROCTIM, PROCMIN. |
| **BESØG** | Dato for ambulant besøg under en kontakt | DTOBES. |
| **PASSV** | Historisk passiv ventetid og afslået behandlingstilbud | DTOAFTLB, BEHANDTILSGH i 2019-layoutet; de egentlige passive ventetidsfelter er historiske. |
| **VENTE** | Tidsrum med ventestatus | VENTESTATUS, DATOSTVENTE, DATOSLVENTE. |
| **BOBST** | Barnets obstetriske oplysninger | FLERNR, VÆGT, LÆNGDE. |
| **MOBST** | Moderens obstetriske oplysninger | PARITET, BESJORD, BESLÆGE, BESSPEC. |
| **PSYKI** | Psykiatrioplysninger | INDVILK startvilkår. |
| **STEDF** | Geografisk stedfæstelse | PRÆCISION, UTM, XKOORD, YKOORD. |
| **SLUT%** | Filens slutmarkering | Ingen kliniske felter; en transportstruktur. |

**Historiske felter, der stadig nævnes i layoutet:** DISTKOD (til 2000), BEHDAGE (til 2001), DTOFORU og DTOENBH (til 2003) i INDUD; PERSKAT, YDESTED og PSYKYD (til 2000) i BESØG; ÅRSAGPAS, DTOSTPAS og DTOSLPAS (til 2003) i PASSV; SIDMEN (til 2001) i MOBST; INDFRA og UDSKRTIL (til 2000) i PSYKI. De er ikke gældende 2019-felter, men kan være relevante for historiske SUP-data. Se [teknisk del, afsnit 5.4](https://sundhedsdatastyrelsen.dk/media/15216/FI_TEKNISK.pdf).

**SKSKO er en vigtig fortolkningsnøgle.** ART kan blandt andet være A (aktionsdiagnose), B (bidiagnose), H (henvisningsdiagnose), V/P/D (operationernes rolle), blank art (anden procedure) eller + (tillægskode). Tillægskoden følger sin primærkode. Både ART, kode og rækkefølge bærer betydning. Se [teknisk del, afsnit 5.2](https://sundhedsdatastyrelsen.dk/media/15216/FI_TEKNISK.pdf).

Skade, fødsel, abort, cancer, forgiftning, psykiatri, funktionsevne og neonatal hørescreening er også dele af LPR2's registrering. De fremstår ikke alle som særskilte strukturer, men kan være udtrykt ved kombinationer af grundfelter, SKS-koder, tillægskoder og særlige felter. Det skal huskes, når vi senere siger, at noget er “nyt” i LPR3.

## 4. Hele LPR3's logiske klassestruktur

[LPR3 bilag 1](https://cdn1.gopublic.dk/sundhedsdatastyrelsen/Media/638955095266548367/Bilag1_model_og_regler_v.5.1.pdf) beskriver en basisstruktur, en struktur for resultatindberetning og anvendte datatyper. Nummereringen springer over 08. Aktionsdiagnose og bidiagnose er specialiseringer af Diagnose. Tabellen medtager klasserne og deres beskrevne egenskaber; objektidentifikatorer og referencer er fælles relationsmekanismer.

| Klasse | Egenskaber | Betydning og relation |
|---|---|---|
| **01 Patient** | id, navn | Patientidentitet. Bilaget omtaler altID, men angiver, at muligheden er afskaffet; den behandles ikke som en aktiv egenskab. |
| **01.A Bopael** | landekode, kommunekode, adresselinje, startdato, slutdato | Tidsafgrænsede bopæls- og opholdsoplysninger. |
| **02 Forloebselement** | ansvarligEnhed, forloebslabel, starttidspunkt, sluttidspunkt, afslutningsmaade | Den periode, hvor en SOR-enhed har overordnet forløbsansvar. |
| **03 Reference** | type | Typet relation mellem forløbselementer, eksempelvis i samme eller et nyt sygdomsforløb. |
| **04 Forloebsmarkoer** | kode, tidspunkt | Klassificeret milepæl eller status i et forløbselement. |
| **05 Kontakt** | ansvarligEnhed, type, prioritet, starttidspunkt, startbehandling, sluttidspunkt | Afgrænset patientkontakt under et forløbselement. |
| **06 Henvisning** | aarsag, maade, fritvalg, henvisendeInstans, tidspunkt | Henvisning til et forløbselement eller eventuelt en kontakt. Årsagen kan være diagnose eller procedure. |
| **07 Kontaktaarsag** | kode | Baggrund for akut kontakt. |
| **09 Opholdsadresse** | enhed, fravaer, starttidspunkt, sluttidspunkt | Fysisk ophold eller aftalt fravær under en kontakt. |
| **10 Betalingsoplysning** | betalingsaftale, betaler, specialiseringsniveau, starttidspunkt, sluttidspunkt | Tidsafgrænsede administrative betalingsforhold. |
| **11 Procedure** | kode, sideangivelse, handlingsspec, anvendtKontrast, personalekategori, indikation, producent, starttidspunkt, sluttidspunkt | Handling under kontakt eller direkte under forløbselement mellem kontakter. |
| **12 Diagnose** | art, kode, sideangivelse, senereAfkraeftet | Aktions- eller bidiagnose som konklusion for en kontakt. |
| **12.A Aktionsdiagnose** | Specialisering af Diagnose | Væsentligste årsag til undersøgelser og behandlinger på kontakten. |
| **12.B Bidiagnose** | Specialisering af Diagnose | Medvirkende eller komplicerende tilstand for kontakten. |
| **13 Metastase** | kode | Kræftspecifikt aspekt under en diagnose. |
| **14 Lokalrecidiv** | kode | Kræftspecifikt aspekt under en diagnose. |
| **15 Resultatindberetning** | navn, ansvarligEnhed, status | Samling af resultater med en udløsende hændelse: forløbselement, markør, kontakt, diagnose eller procedure. |
| **16 Resultat** | type, vaerdi, tidspunkt | Enkeltværdi under en resultatindberetning; datatype og enhed afhænger af specifikationen. |

LPR3 skelner mellem **forløbsansvarlig enhed**, **kontaktansvarlig enhed**, **procedurens producent** og **fysisk opholdsenhed**. De må ikke slås sammen, blot fordi en konkret registrering har samme SOR-kode i flere roller. LPR3's tidsgranularitet er minutter; start er inklusive og slut eksklusive. Se [bilag 1, introduktionen og klasser 02, 05, 09 og 11](https://cdn1.gopublic.dk/sundhedsdatastyrelsen/Media/638955095266548367/Bilag1_model_og_regler_v.5.1.pdf).

### Resultatdelen

Resultatindberetning → Resultat bruges i version 5.1 til 13 specifikationer: canceranmeldelse; fødsel for mor og barn; abort; skade; neonatal hørescreening; personligt alarm- og pejlesystem; implantat; kirurgisk komplikation; tvangsforanstaltning; høreapparat; alkoholbehandling; stofmisbrugsbehandling. Hver specifikation angiver trigger, påkrævede resultater, værdityper, komplethed og frister. Det er ikke en liste over 13 helt nye kliniske begreber; flere fagområder fandtes i LPR2 i en anden form. Se [bilag 1a](https://cdn1.gopublic.dk/sundhedsdatastyrelsen/Media/638955095308962718/Bilag1a_RI-specs_v.5.1.pdf).

## 5. Forskel for forskel

| Tema | LPR2 | LPR3 | Konsekvens for senere mapping |
|---|---|---|---|
| **Overordnet struktur** | Kontakt er indberetningens centrum. | Forløbselement med eget ansvar er overordnet kontakt. | En LPR2-kontakt identificerer ikke automatisk forløbselement, label og reference. |
| **Kontaktens tidsenhed** | En ambulant kontakt kan rumme mange besøg over lang tid. | Kontakter er særskilte objekter med type, prioritet og start/slut. | En BESØG-dato giver ikke sikkert kontaktens start, slut, type eller ansvar. |
| **Forløbets udvikling** | Sammenhæng kan i nogle tilfælde udledes af henvisnings-, afslutnings- og venteoplysninger. | Eksplicitte elementer, typede referencer, label og tidsstemplede markører. | Udledt sammenhæng er svagere evidens end eksplicit registreret relation. |
| **Henvisning** | Kontaktfelter og H-diagnose i SKSKO. | Selvstændigt Henvisning-objekt; årsag kan være diagnose eller procedure. | LPR2's H-diagnose er ikke en tredje LPR3-diagnoseart. |
| **Ventetid** | VENTE med perioder og ventestatus. | Forløbsmarkører udtrykker hændelser og status; nogle markørpar afgrænser perioder. | Ingen generel kode-for-kode oversættelse uden semantisk og tidslig kontrol. |
| **Diagnoser** | SKSKO med A, B og H. | Diagnose på Kontakt er aktions- eller bidiagnose; senereAfkraeftet kan angives. | Henvisningsdiagnose flytter begrebsmæssigt til Henvisning. |
| **Procedurer** | SKSKO med tidspunkt og producerende afsnit i kontaktens sammenhæng. | Selvstændigt Procedure-objekt med flere attributter; kan også ligge mellem kontakter. | Relation og ekstra granularitet kan ikke altid rekonstrueres. |
| **Organisation** | Sygehus/afdeling og producerende afsnit. | SOR-enheder i forskellige ansvars- og opholdsroller. | Kræver historisk organisationsopslag og eksplicit rollevalg. |
| **Fysisk ophold** | Ikke en generel serie af opholdsperioder under kontakten. | Opholdsadresse og fravær kan skifte under samme kontakt. | Kontaktansvar er ikke bevis for patientens fysiske sted. |
| **Betaling** | Blandt andet fritvalg på kontakt; øvrige forhold via gældende registreringspraksis. | Tidsafgrænset Betalingsoplysning med aftale, betaler og specialiseringsniveau. | FRITVALG kan ikke alene udfylde den nye klasse. |
| **Områdespecifikke oplysninger** | Faste særstrukturer og SKS-/tillægskoder. | Generisk resultatstruktur med specifikationer og særlige diagnoseaspekter. | Samme faglige information kan have skiftet form uden at være ny eller tabt. |
| **Kodegyldighed** | SKSKO-art, rækkefølge og SKS-koder bærer betydning. | Feltbestemte kodelister og regler for gyldighed på relevante tidspunkter. | En gyldig historisk SKS-kode er ikke automatisk tilladt som LPR3-værdi. |

### Et eksempel på hvorfor tidsmodellen betyder noget

En patient henvises 1. marts, møder ambulant 10. og 20. marts og opereres 5. april. LPR2 kan have en længere ambulant kontakt med to besøgsdatoer og en særskilt indlæggelseskontakt for operationen. LPR3 kræver stillingtagen til forløbselement(er), konkrete kontakter, henvisning, markører, ansvar og procedure. Datoerne alene fortæller ikke, om forløbsansvaret skiftede, eller hvordan elementerne skal knyttes sammen.

Et konkret LPR3-krav viser forskellen: Et forløbselement skal have en **startmarkør** senest ved afslutning eller første tilknyttede kontakt/procedure, og markørens dato skal svare til forløbselementets startdato. Det kan ikke udfyldes sikkert ved blot at omdøbe et gammelt kontaktinterval. Se [bilag 1, regel 02.04](https://cdn1.gopublic.dk/sundhedsdatastyrelsen/Media/638955095266548367/Bilag1_model_og_regler_v.5.1.pdf).

## 6. Hvad findes tydeligere på den ene side?

**Eksplicit som selvstændige LPR3-begreber:** Forløbselement og label; typede forløbsreferencer; tidsstemplede markører; kontaktansvar adskilt fra forløbsansvar; tidsafgrænset ophold/fravær og betaling; procedure direkte på forløbselement; procedureindikation; senereAfkraeftet; Resultatindberetning → Resultat. Dette betyder ikke, at alle underliggende oplysninger var ukendte i LPR2. Flere kunne være kodet eller lokalt registreret på anden måde.

**Særligt for LPR2's repræsentation:** Den lange ambulante kontakt med BESØG-datoer; patienttype som hovedskel mellem indlagt og ambulant; SKSKO/ART og rækkefølgebundne tillægskoder; VENTE-perioder; faste obstetrik-, psykiatri- og stedfæstelsesstrukturer; historiske PASSV-felter. Flere begreber fortsætter i LPR3, men på andre objekter eller med andre krav.

**SUP, som først analyseres i 2b:** Notater er en del af vores SUP-arbejde, men ingen af de ovennævnte LPR2-indberetningsstrukturer eller LPR3-klasser er et generelt Notat-objekt. Det siger ikke, at EPJ-systemer mangler notater: LPR er en indberetningsmodel, ikke hele journalmodellen.

## 7. Kontrolspørgsmål til opgave 2b

1. Hvad betyder SUP Patientforloeb konkret: lokal klinisk gruppering, et LPR2-kontaktforløb eller noget der faktisk identificerer LPR3-forløbselementer?
2. Hvad betyder SUP Kontaktperiode: et konkret fremmøde, et administrativt interval eller en status? Kan ét objekt dække flere LPR3-kontakter?
3. Hvilke oplysninger findes faktisk om henvisningens tidspunkt, afsender, årsag og relation til tidligere forløb?
4. Kan SUP adskille forløbsansvar, kontaktansvar, procedureproducent og fysisk ophold? Hvilke historiske identifikatorer kan knyttes til SOR?
5. Er en SUP-diagnose en aktions-/bidiagnose på en bestemt kontakt, en henvisningsdiagnose eller en løbende forløbsdiagnose?
6. Kan SKS-primærkode, tillægskoder, rækkefølge og kodeversion bevares?
7. Har SUP eksplicitte markører og referencetyper, eller kun datoer/status, som kunne forveksles med dem?
8. Hvilke SUP-notater og EPJ-oplysninger ligger uden for både LPR2 og LPR3 og skal have selvstændig plads i den nye standard?

Dette er undersøgelsesspørgsmål, ikke påstande om konkrete SUP-felter. I 2b skal hvert svar dokumenteres mod de uploadede MedCom-specifikationer.

## 8. Arbejdsprincip for en senere FHIR-standard

LPR3 er et godt fælles **begrebs- og relationsgrundlag**, men det er en indberetningsmodel. Den kommende standard skal både bevare gamle SUP-data og kunne rumme rigere EPJ-indhold. Derfor må vi skelne mellem **kildens faktiske værdi**, **fortolket LPR-begreb** og **foreslået FHIR-udtryk**. Hvor kilden ikke dokumenterer en påkrævet LPR3-egenskab, skal den stå som ukendt eller kræve afklaring; vi må ikke fremstille en afledt værdi som direkte observeret.

**Næste leverance, opgave 2b:** En SUP-inventarliste med ét element eller én attribut pr. række, præcis betydning fra MedCom-dokumentationen, relation til LPR2 og LPR3 samt en forklaring af hvert semantisk gab.
