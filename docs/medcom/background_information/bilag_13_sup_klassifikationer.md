---
title: "SUP-specifikation, version 2.0 - Bilag 13: SUP-klassifikationer"
bilag: 13
version: "2.0"
status: "Udkast"
dato: "2003-06-12"
kilde_pdf: "bilag_13_sup-klassifikationer_udk_120603.pdf"
format: "agent-egnet markdown"
---

# SUP-specifikation, version 2.0 - Bilag 13: SUP-klassifikationer

> **Agentnote:** Dette dokument er konverteret fra den oprindelige PDF. Tekst og kode følger kildens ordlyd så tæt som praktisk muligt. Billedressourcer og Mermaid-rekonstruktioner er hjælpemidler til fortolkning; ved uoverensstemmelse har kildeteksten forrang.

## Kildetekst

<!-- Kildeside 1 -->

SUP-specifikation, version 2.0

Bilag 13


SUP-klassifikationer


Udkast af 12. juni 2003


Udarbejdet for


SUP-Styregruppen


© Uddrag af indholdet kan gengives med tydelig kildeangivelse

<!-- Kildeside 2 -->

Bilag 13: SUP-klassifikationer
Udkast 12-06-03

Side 2 af 6
Indholdsfortegnelse

1
Notater ...................................................................................... 3
2
Prioritet..................................................................................... 3
3
Forløbsstatus / Kontaktperiode.............................................. 4
4
Enhed ........................................................................................ 4
5
Sikkerhedskode........................................................................ 4
6
Medicintyper ............................................................................ 4
7
Administrationsmåde .............................................................. 5
8
Lægemiddelform...................................................................... 5
9
Observationer og fund............................................................. 5
10
Afslutningsårsag................................................................... 5
11
Prøveresultater ..................................................................... 5
11.1
Patologisvar ........................................................................................ 6
12
Administrative karakteristika............................................. 6
13
Fødesystem............................................................................ 6

<!-- Kildeside 3 -->

Bilag 13: SUP-klassifikationer
Udkast 12-06-03

Side 3 af 6
Klassifikationer i SUP, version 2.0


I SUP anvendes som udgangspunkt SKS-klassifikationer samt en række andre
officielle klassifikationer jf. Domænemodellen [Bilag 2].

I dette notat beskrives alene, hvilke klassifikationer der anvendes i felter, hvor
der kan være tvivl. Desuden er der opstillet en række små SUP-klassifikationer.
Vedr. anvendelse af klassifikationer mere generelt henvises til SUP-specifikationens afsnit 4.15.

I det følgende er koder og kodetekster anført med fed skrift. Under de klassificerede begreber anføres en definition, hvor dette skønnes nødvendigt.


1
Notater

Der findes ikke en fælles klassifikation over notattyper og navne. Det lokale
navn på notatet anføres både som kodetekst til attributten "Notat overskrift"  og
på første linie (efterfulgt af linieskift) i den brødtekst, der udgør notatet. Primærkoden udfyldes ikke. Klassifikationstypen angives som "Ingen".


2
Prioritet

Feltet prioritet anvendes for mange hændelsestyper. Feltet kan p.t. udfyldes
med én af følgende tre koder:

XSUP00P1
Akut
Proceduren skal udføres snarest muligt.
XSUP00P2
Fremskyndet / subakut
Proceduren skal udføres akut i dagarbejdstiden på førstkommende hverdag.
XSUP00P3
Planlagt
Proceduren skal udføres i henhold til producentens almindelige rutiner
for indkaldelse af patienter. Ordet "Elektiv" anvendes flere steder i stedet
for "Planlagt".

<!-- Kildeside 4 -->

Bilag 13: SUP-klassifikationer
Udkast 12-06-03

Side 4 af 6
3
Forløbsstatus / Kontaktperiode

Der anvendes forløbsstatus/kontaktperiodetyper:

D
Død
I
Indlagt
A
Ambulant, der også omfatter:
Deldøgn

Forambulant

Efterambulant

Almindelig ambulant
Skadestuepatient
H
Henvist
L
Lægepraksis, herunder:

Almen lægepraksis

Speciallægepraksis
S
Inaktiv

Klassifikationen skal justeres, når der foreligger en ny, vedtaget forløbsmodel.


4
Enhed

Her tænkes på måleenheder ved f.eks. medicinering og prøveresultater. Enheden anføres som fri tekst indtil videre. Der findes ikke nogen altomfattende,
officiel klassifikation over enheder, men for laboratorieprøver bør anvendes
enhederne fra IUPAC. Tilsvarende bør for medicin anvendes forkortelser fra
Lægemiddelstyrelsens specialitetstakst (LMS).


5
Sikkerhedskode

Der er ikke p.t. fastlagt en klassifikation for sikkerhedskoden.


6
Medicintyper

Ved medicinordinationer og medicingivning anvendes følgende medicintyper:

XSUP00MF
Fast ordination
XSUP00ME
Engangsdosis
XSUP00MS
Efter skema
XSUP00MP
P.N.-medicin

<!-- Kildeside 5 -->

Bilag 13: SUP-klassifikationer
Udkast 12-06-03

Side 5 af 6
7
Administrationsmåde

Her anvendes SKS-Behandlingsklassifikationens kapitel BZA. Hvis denne ikke
benyttes i det lokale system, kan den lokale klassifikation anvendes.


8
Lægemiddelform

Forkortelsen for form fra Lægemiddelstyrelsens specialitetstakst bør anvendes.


9
Observationer og fund

Da SKS-undersøgelsesklassifikationen ikke er færdig, anvendes p.t. følgende
SUP-koder:

XSUP00PU
Puls
XSUP00BT
Blodtryk
XSUP00TP
Temperatur
XSUP00HJ
Højde
XSUP00VG
Vægt

Ved andre strukturerede observationer og fund anbringes den lokale kodetekst i
kodeteksten til primærkoden jf. specifikationens afs. * .


## 10 Afslutningsårsag

Hvis der findes en lokal klassifikation, anvendes denne.


## 11 Prøveresultater

Ved laboratoriesvar bør anvendes IUPAC's korte koder. Ellers anvendes lokal
kode.

Ved binære svar på formen "Negativ / Positiv" kan anvendes:


XSUP00B0

Negativ

XSUP00B1

Positiv

XSUP00BU

Uoplyst

<!-- Kildeside 6 -->

Bilag 13: SUP-klassifikationer
Udkast 12-06-03

Side 6 af 6

Undersøgelsesproceduren skal i så fald være udfyldt.

Mikrobiologsvar oprettes p.t. som et særskilt notat med overskriften "Mikrobiologisvar" og selve svaret i brødteksten.


### 11.1 Patologisvar

Primærkoden for et patologisvar er:


XSUP00PA
Patologisvar

Undersøgelsesproceduren angives efter følgende klassifikation:


XSUP00PH
Histologi

XSUP00PC
Cytologi

XSUP00PF
Frysemikroskopi

XSUP00PS
Section

SNOMED-koderne for selve resultatet anvendes som angivet i domænemodel
og elementstrukturen. Supplerende akser anføres som tillægskoder.

## 12 Administrative karakteristika

Egen læge klassificeret efter Sygesikringens ydernummerklassifikation.
Kommunekoden klassificeret efter Kommunekodeklassifikationen.

Startdatoen er for begge kontaktperiodens startdato eller en senere ændringsdato.

For øvrige administrative karakteristika anvendes de lokale klassifikationer.

## 13 Fødesystem

Ved "Fødesystem" forstår man det system, hvorfra forløbets data er udtrukket
fra. Bemærk, at de udtrukne data oprindeligt kan være registreret i et andet
system.

Klassifikationen over fødesystemer består af strenge med 10 karakterer, hvor
de første 5 er en kode for leverandøren og de sidste 5 er en kode for systemet.
Koderne skal udarbejdes af de enkelte leverandører, men godkendes af SUPorganisationen. Det skal tilstræbes, at koderne er selvforklarende, da de må
forventes ofte at blive fremvist uden en kodetekst.
