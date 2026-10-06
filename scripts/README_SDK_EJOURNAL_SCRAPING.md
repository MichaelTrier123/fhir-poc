# SDK eJournal scraper

This folder contains a PowerShell script for downloading eJournal data from the SDK demo environment.

The scraper uses an existing `forloebsoversigt.json` file and retrieves related resources such as:

- `caveoplysninger`
- `diagnoser`
- `notater`
- `procedurer`
- `kontaktperioder`
- `epikriser`

## 1. Retrieve session values from the browser

Open the SDK eJournal page in Chrome:

```text
https://demo.sdkdev.dk/borger/min-side/min-sundhedsjournal/journal-fra-sygehus/
```

Open **Developer Tools** → **Network**.

Trigger a request in the journal UI, for example the `filter` request.

Right-click the request and select:

```text
Copy
→ Copy as cURL
```

From the copied request, locate these three values.

### Cookie

Find the value supplied with:

```text
-b '...'
```

Use the entire cookie string.

### XSRF token

Find:

```text
-H 'x-xsrf-token: ...'
```

### Conversation UUID

Find:

```text
-H 'conversation-uuid: ...'
```

## 2. Set PowerShell environment variables

In the PowerShell window where the scraper will run:

```powershell
$env:EJOURNAL_COOKIE = '<entire cookie value>'

$env:EJOURNAL_XSRF = '<x-xsrf-token>'

$env:EJOURNAL_CONVERSATION_UUID = '<conversation-uuid>'
```

Example structure:

```powershell
$env:EJOURNAL_COOKIE = 'sdk-user-accept-cookies=true; ss-id=...; XSRF-TOKEN=...; sdk-usertype=borger; ...'

$env:EJOURNAL_XSRF = '...'

$env:EJOURNAL_CONVERSATION_UUID = '...'
```

The `XSRF-TOKEN` inside the cookie should match `EJOURNAL_XSRF`.

## 3. Run the scraper

Place the current overview response in:

```text
forloebsoversigt.json
```

Then run:

```powershell
.\scrape_sdk_data.ps1 `
    -OverviewPath '.\forloebsoversigt.json'
```

The CPR is read automatically from `PersonNummer` in the overview.

Output is written to:

```text
ejournal-<cpr>/
```

For example:

```text
ejournal-1110109996/
    forloebsoversigt-1110109996.json
    caveoplysninger-1110109996.json
    diagnoser-<key>.json
    notater-<key>.json
    procedurer-<key>.json
    kontaktperioder-<key>.json
    epikriser-<key>.json
```

To overwrite and re-download existing resources:

```powershell
.\scrape_sdk_data.ps1 `
    -OverviewPath '.\forloebsoversigt.json' `
    -ForceRefresh
```

## Session expiry

If the scraper returns `401` or `403`, retrieve a fresh browser request and update:

```text
EJOURNAL_COOKIE
EJOURNAL_XSRF
EJOURNAL_CONVERSATION_UUID
```

These values belong to the active browser session and may expire.

## Important

The cookie and XSRF token are session credentials.

Do not:

- commit them to Git
- store them in the scraper
- include them in shared documentation
- reuse values from another browser session
