# FHIR Patient Summary Mapping Guide — Denmark / EPS / IPS

> **Purpose:** Agent-oriented starting point for mapping healthcare data from an external API into FHIR resources suitable for a Danish implementation of a patient summary.
>
> **Scope of this document:** Profile/implementation-guide selection, conformance hierarchy, mapping workflow, and a high-level resource-family overview. This document intentionally does **not** deep-dive into individual resource element mappings yet.
>
> **Baseline date:** 2026-10-06

---

## 1. Executive decision

For this project, use the following priority order:

1. **FHIR R4 (4.0.1)** is the required FHIR release.
2. **Danish conformance takes precedence for Danish data:**
   - Prefer **MedCom Core** profiles when a relevant MedCom Core profile exists and is appropriate for the exchange context.
   - Otherwise prefer **DK Core** profiles.
3. For the patient-summary specification:
   - Prefer a **Danish EPS/IPS profile** if an official one becomes available.
   - At the time of writing, no published Danish EPS/IPS implementation guide has been identified in the current DK Core or MedCom Core implementation guides.
   - Therefore use the **HL7 Europe European Patient Summary (EPS)** as the patient-summary target.
4. Use the **International Patient Summary (IPS)** only when:
   - EPS has no applicable rule/profile,
   - EPS explicitly inherits or delegates to IPS,
   - or IPS is needed as a compatibility/reference layer.

### Selected patient-summary target

**HL7 Europe Patient Summary (EPS), package `hl7.fhir.eu.eps#1.0.0-ballot`, FHIR R4.**

This is currently a ballot/draft implementation guide. It is nevertheless preferred over the international IPS for this project because the project is Denmark/EU-oriented and the stated selection rule is:

**Danish EPS/IPS > European EPS > International IPS**

### Important implementation consequence

Do **not** assume that an EPS profile and a DK Core/MedCom Core profile have a direct inheritance relationship.

The agent must treat conformance as an **intersection of requirements**:

- Danish profile requirements for Danish identifiers, organizations, practitioners, terminology, etc.
- EPS patient-summary requirements for inclusion, document structure, sections, obligations, and cross-border semantics.

Only claim multiple profiles in `meta.profile` when the concrete instance actually validates against all claimed profiles.

If the two profile families cannot be satisfied simultaneously, the agent must flag a **profile conflict** rather than silently choosing one or inventing a local profile.

---

## 2. Standards and implementation guides

| Layer | Selected specification | Package / version | FHIR version | Role in this project |
|---|---|---|---|---|
| Base standard | HL7 FHIR | R4 / 4.0.1 | R4 | Base resource model |
| Danish base | DK Core | `hl7.fhir.dk.core#3.7.0` | R4 | Default Danish profiling and Danish identifiers/terminology |
| Danish MedCom base | DK MedCom Core | `medcom.fhir.dk.core#4.0.0` | R4 | Preferred where an applicable MedCom Core profile exists |
| EU base | HL7 Europe Base and Core | EPS dependency | R4 | European base profiles used by EPS |
| Patient summary | HL7 Europe Patient Summary | `hl7.fhir.eu.eps#1.0.0-ballot` | R4 | Primary patient-summary model |
| International fallback | International Patient Summary | current published `hl7.fhir.uv.ips#2.0.1`; EPS ballot depends on IPS 2.0.0 | R4 | International fallback/reference |

### Version note

The currently published IPS version is newer than the IPS version declared by the current EPS ballot:

- Current published IPS: `2.0.1`
- EPS 1.0.0-ballot dependency: `hl7.fhir.uv.ips#2.0.0`

For strict validation of the EPS ballot, use the dependency versions declared by that EPS package unless the validator/tooling resolves an approved compatible patch version. Do not silently substitute profile versions during validation.

---

## 3. Current Danish status

### DK Core

DK Core is the Danish base implementation guide. It is explicitly designed to be inherited/used by more use-case-specific specifications.

Current baseline:

- Package: `hl7.fhir.dk.core#3.7.0`
- FHIR: R4 / 4.0.1
- Current published release as of this document
- Covers Danish representations such as Patient, Practitioner, PractitionerRole, Organization, Condition, Observation, DiagnosticReport, ServiceRequest, identifiers, extensions, and terminology.

DK Core is **not itself a patient-summary specification**.

### MedCom Core

MedCom Core defines common FHIR profiles used across MedCom standards.

Current baseline:

- Package: `medcom.fhir.dk.core#4.0.0`
- FHIR: R4 / 4.0.1
- Depends on DK Core
- Provides common MedCom expectations for resources such as Patient, Encounter, Practitioner, Organization, and related exchange infrastructure.

MedCom Core is also **not itself a complete patient-summary specification**.

### Danish EPS / IPS status

No official, published Danish FHIR implementation guide specifically defining a Danish **European Patient Summary** or **International Patient Summary** was identified in the current DK Core or MedCom Core publication sets.

Therefore:

```text
No Danish EPS/IPS profile available
        ↓
Use HL7 Europe EPS
        ↓
Apply DK Core / MedCom Core requirements to Danish content
        ↓
Use IPS only as EPS dependency/fallback
```

This decision should be revisited if HL7 Denmark, MedCom, the Danish Health Data Authority, or another authoritative Danish body publishes a national EPS/EEHRxF patient-summary IG.

---

## 4. Target conformance hierarchy

The agent should use this decision hierarchy for every mapped concept.

```text
FHIR R4 resource
    │
    ├─ Is there an applicable MedCom Core profile?
    │      ├─ YES → prefer MedCom Core
    │      │          (and satisfy its DK Core ancestry/dependencies)
    │      └─ NO
    │
    ├─ Is there an applicable DK Core profile?
    │      ├─ YES → use DK Core
    │      └─ NO → use base FHIR R4 as Danish baseline
    │
    ├─ Is the information part of a Patient Summary?
    │      ├─ YES → apply EPS requirements
    │      │
    │      └─ Does EPS define a concrete resource profile?
    │             ├─ YES → validate compatibility with Danish profile
    │             └─ NO → follow EPS obligation / EU Core / IPS rule
    │
    └─ If EPS is silent → consult IPS 2.x
```

### Never do this

The agent must not:

- replace a DK Core/MedCom profile with a generic international profile merely because the international profile is easier;
- claim conformance to an EPS profile without validating it;
- claim conformance to multiple profiles simply by adding canonical URLs to `meta.profile`;
- invent Danish extensions or local code systems when an official Danish artifact exists;
- copy data into an EPS section solely because a source field has a similar display name;
- infer absent clinical facts from missing source data.

---

## 5. European Patient Summary: high-level structure

The EPS is represented as a **FHIR document**.

Primary entry profiles:

- **Bundle (EPS)** — the document Bundle.
- **Composition (EPS)** — the clinical document structure and section organizer.

At document level, think in this form:

```text
Bundle (type=document)
└── Composition (EPS)
    ├── subject → Patient
    ├── author → Practitioner / PractitionerRole / Organization
    ├── custodian / organizational context
    └── sections
         ├── problems
         ├── allergies and intolerances
         ├── medication summary
         ├── procedures
         ├── medical devices
         ├── immunizations
         ├── diagnostic results
         ├── vital signs
         ├── pregnancy-related information
         ├── functional status
         ├── plan of care
         ├── advance directives
         ├── travel history
         └── other EPS-defined sections
```

The exact cardinalities, required sections, terminology, and missing-data rules must be taken from the EPS Composition profile during the resource-specific phase.

---

## 6. EPS resource families — high-level overview

The current EPS ballot defines or constrains the following resource families.

| Clinical/administrative concept | Main FHIR resource(s) | EPS treatment | Danish mapping stance |
|---|---|---|---|
| Patient identity/demographics | `Patient` | EPS Patient profile | Prefer DK/MedCom Patient requirements; also satisfy EPS where compatible |
| Document container | `Bundle` | EPS Bundle profile | Use EPS Bundle |
| Patient summary document | `Composition` | EPS Composition profile | Use EPS Composition |
| Allergies/intolerances | `AllergyIntolerance` | EPS obligations on EU Core | Apply Danish constraints if available; satisfy EPS obligations |
| Problems/diagnoses | `Condition` | EPS obligations on EU Core | Prefer MedCom/DK profile where applicable; satisfy EPS obligations |
| Alerts | `Flag` | EPS obligations on EU Core Flag/Alert | Use EPS/EU requirements; add Danish constraints if applicable |
| Immunizations | `Immunization` | EPS obligations on EU Core | Use Danish terminology where required; satisfy EPS |
| Medication definition | `Medication` | EPS obligations on EU Core | Danish medicinal-product coding first where mandated; satisfy EPS |
| Medication orders | `MedicationRequest` | EPS obligations on EU Core | Danish/MedCom profile if applicable; satisfy EPS |
| Medication use | `MedicationStatement` | Dedicated EPS profile | Use EPS profile unless a Danish profile creates stricter compatible requirements |
| Organization | `Organization` | EPS obligations on EU Core | Prefer MedCom Core / DK Core Organization; use Danish organization identifiers |
| Practitioner | `Practitioner` | EPS obligations on EU Core | Prefer MedCom Core / DK Core Practitioner |
| Practitioner role | `PractitionerRole` | EPS obligations on EU Core | Prefer MedCom Core / DK Core PractitionerRole |
| Medical device | `Device` | Dedicated EPS profile | EPS + Danish identifiers/terminology where relevant |
| Device use | `DeviceUseStatement` | Dedicated EPS profile | EPS + Danish compatibility checks |
| Diagnostic results | `DiagnosticReport` | Dedicated EPS profile | Prefer DK Core DiagnosticReport requirements where compatible |
| Procedures | `Procedure` | Dedicated EPS profile | EPS; Danish code systems/identifiers where applicable |
| Advance directives | `Consent` | Dedicated EPS profile | Use EPS semantics; do not treat generic consent as advance directive automatically |
| Pregnancy status | `Observation` | Dedicated EPS profile | EPS |
| Expected delivery date | `Observation` | Dedicated EPS profile | EPS |
| Gestational age | `Observation` | Dedicated EPS profile | EPS |
| Pregnancy outcome | `Observation` | Dedicated EPS profile | EPS |
| Travel history / country visited | `Observation` | Dedicated EPS profile | EPS |
| Vital signs | `Observation` | Referenced in EPS examples/model | Prefer DK Core Observation/vital-sign profiles where applicable |
| Care planning | `CarePlan` | Used by EPS model/examples | Apply EPS section semantics and Danish profile if one exists |

This table is deliberately conceptual. Resource-specific cardinalities and code bindings belong in the next phase.

---

## 7. EPS vs IPS

### Why EPS is primary

EPS is designed for the European context and aligns with:

- EU cross-border patient-summary use cases;
- EHDS / EEHRxF direction;
- European base/core profiles;
- IPS as an international foundation.

The EPS guide states that it aims for conformance with IPS while adding European-specific requirements.

### Important EPS differences from IPS

At a high level, the EPS introduces European-specific constraints and content beyond IPS. Current EPS material includes additional emphasis/profiled content such as:

- procedures;
- medical devices;
- advance directives;
- pregnancy-specific observations;
- travel history;
- European obligations and EU Core alignment.

The EPS variance documentation should be consulted before making any assumption that an IPS-valid document is automatically EPS-valid.

### IPS role

Use IPS for:

- concepts delegated by EPS;
- fallback when EPS is silent;
- international compatibility analysis;
- understanding the semantic foundation of patient-summary sections.

Do not use IPS as the first-choice target when an EPS rule exists.

---

## 8. Mapping strategy for an external API

The agent should map **semantics first**, not JSON field names.

### Phase A — Source API understanding

For every source field/object, capture:

```yaml
source:
  path: ""
  label: ""
  description: ""
  datatype: ""
  cardinality: ""
  nullable: true
  code_system: ""
  units: ""
  identifier_namespace: ""
  lifecycle/status_semantics: ""
  timestamps:
    recorded: ""
    effective: ""
    updated: ""
  references: []
  examples: []
```

Do not start FHIR mapping until the source semantics are understood.

### Phase B — Clinical concept classification

Classify each source concept into a semantic category, for example:

```text
person identity
organization
healthcare professional
diagnosis/problem
allergy
medication
procedure
observation/result
diagnostic report
immunization
device
encounter
care plan
advance directive
document metadata
```

### Phase C — Candidate FHIR resource

Choose the base FHIR R4 resource based on meaning.

Examples:

```text
diagnosis/problem        → Condition
measured laboratory item → Observation
lab report/panel         → DiagnosticReport + Observation
current medication use   → MedicationStatement
prescription/order       → MedicationRequest
performed procedure      → Procedure
allergy                  → AllergyIntolerance
vaccination event        → Immunization
```

Do not collapse clinically distinct source concepts into one resource type for convenience.

### Phase D — Danish profile selection

For the chosen resource:

1. Check MedCom Core.
2. Check DK Core.
3. Select the narrowest applicable official Danish profile.
4. Record canonical URL and version.
5. Record required Danish identifiers, code systems, and extensions.

### Phase E — EPS inclusion decision

Determine whether the mapped data belongs in the European Patient Summary.

Record:

```yaml
eps:
  included: true
  section: ""
  eps_profile: ""
  eps_obligation_profile: ""
  required_by_eps: false
  reason: ""
```

If not part of EPS, the resource may still be valid Danish FHIR data, but it should not automatically be included in the patient-summary document.

### Phase F — Profile compatibility check

For resources subject to both Danish and EPS requirements, classify compatibility:

```text
COMPATIBLE
  Instance can validate against both.

COMPATIBLE_WITH_MAPPING
  Same clinical concept, but coding/identifier/field transformations are required.

DOCUMENT_ONLY_COMPATIBILITY
  Resource remains Danish-profiled; EPS requirements are primarily document/section-level.

CONFLICT
  Requirements cannot be satisfied simultaneously.

UNRESOLVED
  Specification interpretation required.
```

### Phase G — Terminology mapping

Terminology must be handled independently of structural mapping.

For every coded value, capture:

```yaml
terminology:
  source_system: ""
  source_code: ""
  source_display: ""
  target_system: ""
  target_code: ""
  target_display: ""
  mapping_relation: equivalent | wider | narrower | related | no-map
  mapping_basis: ""
  confidence: high | medium | low
```

Do not silently convert free text to SNOMED CT/LOINC/NPU/ATC or another terminology without a documented mapping source.

### Phase H — Build patient-summary document

After individual resources are mapped and validated:

1. Create EPS `Composition`.
2. Populate the applicable sections.
3. Reference the mapped resources.
4. Create EPS document `Bundle`.
5. Include all resources required for a self-contained FHIR document.
6. Validate the complete document against the EPS package and the applicable Danish profiles.

---

## 9. Agent output contract for future mappings

For every source object/resource analyzed later, the agent should produce a record in this form:

```yaml
mapping:
  source_object: ""
  clinical_concept: ""

  fhir:
    resource_type: ""
    selected_profile:
      canonical: ""
      package: ""
      version: ""
    secondary_profiles: []

  denmark:
    medcom_profile: ""
    dk_core_profile: ""
    identifiers: []
    terminology: []
    extensions: []

  eps:
    included: false
    section: ""
    profile: ""
    obligation_profile: ""

  ips_fallback:
    used: false
    profile: ""
    reason: ""

  status:
    compatibility: COMPATIBLE | COMPATIBLE_WITH_MAPPING | DOCUMENT_ONLY_COMPATIBILITY | CONFLICT | UNRESOLVED
    confidence: high | medium | low

  notes: []
  open_questions: []
```

This structure should be machine-readable and stable enough to feed a later implementation agent.

---

## 10. Conformance rules for the implementation agent

The following rules are normative for this project unless explicitly overridden.

### Rule 1 — R4 only

All produced resources must be compatible with **FHIR R4 / 4.0.1**.

Do not introduce R5-only resources or elements.

### Rule 2 — Danish-first profiling

Where a relevant MedCom Core or DK Core profile exists, it must be considered before a generic international profile.

### Rule 3 — MedCom over DK Core where applicable

MedCom Core may further constrain or build on DK Core. Use MedCom Core when the use case/resource is covered appropriately.

### Rule 4 — EPS over IPS

For the patient-summary layer:

```text
Danish EPS/IPS, if published and applicable
> European EPS
> International IPS
```

### Rule 5 — No false profile claims

`meta.profile` is a conformance claim.

Only include a profile canonical when the resource is intended and verified to conform to it.

### Rule 6 — Preserve provenance

When transforming data:

- retain source identifiers where allowed;
- retain source timestamps;
- distinguish source-recorded time from clinical/effective time;
- preserve original coded values when a mapping is lossy;
- record mapping uncertainty.

### Rule 7 — Do not infer negative clinical statements

Missing source data does not mean:

- no allergies;
- no medication;
- no diagnoses;
- no procedures.

EPS/IPS missing-data and empty-section semantics must be followed explicitly.

### Rule 8 — Clinical status is not API status

A source API object's status such as `"active"`, `"closed"`, `"deleted"`, or `"archived"` must not automatically be mapped to a FHIR clinical status.

Interpret source lifecycle semantics first.

### Rule 9 — Document context matters

A valid standalone resource does not automatically make a valid EPS document.

EPS document construction, Composition sections, Bundle inclusion, references, narrative, and missing-data requirements must be validated separately.

### Rule 10 — Flag ambiguity

When multiple FHIR resources plausibly represent the same source concept, the agent should return an explicit mapping decision with reasoning and confidence rather than guessing silently.

---

## 11. Validation strategy

Validation should happen at three levels.

### Level 1 — Base FHIR

Validate against FHIR R4 syntax and invariants.

### Level 2 — Danish profiles

Validate each resource against its selected:

- MedCom Core profile, or
- DK Core profile.

### Level 3 — EPS document

Validate:

- EPS-specific resource profiles/obligations;
- EPS Composition;
- EPS document Bundle;
- references and document closure;
- applicable terminology bindings.

A resource may pass Level 2 and still fail Level 3.

---

## 12. Package baseline for tooling

Suggested package set for a validator / local implementation environment:

```text
hl7.fhir.r4.core#4.0.1
hl7.fhir.dk.core#3.7.0
medcom.fhir.dk.core#4.0.0
hl7.fhir.eu.eps#1.0.0-ballot
```

Then allow the package manager to resolve EPS-declared dependencies, including:

```text
hl7.fhir.eu.base#2.0.0
hl7.fhir.eu.extensions.r4#1.3.0
hl7.fhir.uv.ips#2.0.0
hl7.fhir.uv.ipa#1.1.0
```

For a separate international IPS compatibility check, also consider the current published IPS package:

```text
hl7.fhir.uv.ips#2.0.1
```

Do not mix EPS dependency versions and newer IPS profiles in the same conformance claim without testing compatibility.

---

## 13. Canonical references

### FHIR R4

- Specification: https://hl7.org/fhir/R4/

### DK Core

- IG: https://hl7.dk/fhir/core/
- Package: `hl7.fhir.dk.core#3.7.0`
- Canonical IG URL: `http://hl7.dk/fhir/core/ImplementationGuide/hl7.fhir.dk.core`

### MedCom Core

- IG: https://medcomfhir.dk/ig/core/
- Package: `medcom.fhir.dk.core#4.0.0`
- Canonical IG URL: `http://medcomfhir.dk/ig/core/ImplementationGuide/medcom.fhir.dk.core`

### European Patient Summary

- Published ballot IG: https://hl7.eu/fhir/eps/
- Package: `hl7.fhir.eu.eps#1.0.0-ballot`
- Canonical IG URL: `http://hl7.eu/fhir/eps/ImplementationGuide/hl7.fhir.eu.eps`
- EPS Bundle canonical: `http://hl7.eu/fhir/eps/StructureDefinition/bundle-eu-eps`
- EPS Composition canonical: `http://hl7.eu/fhir/eps/StructureDefinition/composition-eu-eps`

### International Patient Summary

- Current published IG: https://hl7.org/fhir/uv/ips/
- Current package: `hl7.fhir.uv.ips#2.0.1`
- Canonical IG URL: `http://hl7.org/fhir/uv/ips/ImplementationGuide/hl7.fhir.uv.ips`

---

## 14. Known risks / open points

### EPS maturity

The selected EPS version is a ballot/draft version, not a final normative release.

The implementation should therefore:

- pin the package version;
- avoid depending on CI-build-only behavior;
- document deviations;
- expect profile changes between ballot and final releases.

### Danish national EPS evolution

Denmark may later publish a national EPS/EEHRxF implementation guide.

If that happens, the hierarchy must be reevaluated and the national guide should normally supersede direct use of generic EPS profiles.

### Profile intersection

The central technical risk is that EPS profiles derive from EU/IPS profile families while Danish resources derive from DK Core/MedCom families.

A future project-specific IG may eventually be appropriate to formally define intersections, but this project must **not invent such profiles during mapping analysis**.

First identify and document compatibility gaps.

### Terminology

Cross-border EPS terminology and Danish national terminology may not always align one-to-one.

Terminology mappings must therefore be explicit artifacts rather than implicit code substitutions.

---

## 15. Next phase

The next task should be a resource-by-resource mapping matrix.

Recommended order:

1. `Patient`
2. `Organization`
3. `Practitioner` / `PractitionerRole`
4. `Condition`
5. `AllergyIntolerance`
6. Medication resources
7. `Observation`
8. `DiagnosticReport`
9. `Procedure`
10. `Immunization`
11. `Device` / `DeviceUseStatement`
12. `CarePlan`
13. EPS-specific pregnancy/travel/advance-directive resources
14. `Composition`
15. document `Bundle`

For each, document:

- source API object(s);
- selected Danish profile;
- EPS profile/obligation;
- IPS fallback profile where relevant;
- mandatory elements;
- must-support/obligation rules;
- terminology;
- identifiers;
- references;
- mapping gaps;
- example mapping;
- validation command/result.

---

## 16. Decision summary for agents

Use this compact rule set:

```text
FHIR release = R4 / 4.0.1

IF applicable MedCom Core profile exists:
    use MedCom Core
ELSE IF applicable DK Core profile exists:
    use DK Core
ELSE:
    use base FHIR R4

FOR patient-summary inclusion:
    IF official Danish EPS/IPS profile exists:
        use it
    ELSE:
        use HL7 Europe EPS

IF EPS is silent:
    use IPS as fallback/reference

NEVER:
    claim profile conformance without validation
    infer clinical negatives from missing data
    replace Danish identifiers/terminology with generic alternatives
    invent local profiles during mapping analysis
```

---

## 17. Source authority order

When specifications disagree or appear ambiguous, consult sources in this order:

1. Applicable Danish legislation/regulatory requirements.
2. Official Danish implementation guide/profile selected for the use case.
3. HL7 Europe EPS.
4. HL7 Europe Base/Core.
5. International IPS.
6. Base FHIR R4.
7. Examples and non-normative implementation notes.

Examples are evidence of intended use, not a substitute for formal profile definitions.

---

**Status:** Initial architecture / profile-selection guide.  
**Next revision:** After concrete source API resources are known and the resource-by-resource mapping phase begins.
