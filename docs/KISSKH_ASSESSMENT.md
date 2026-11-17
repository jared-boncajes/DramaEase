# KissKH integration assessment — https://kisskh.do

Assessment date: 2026-11-17. Method: plain HTTPS requests from the build sandbox. I did **not** attempt to get past any
protection on the site. Everything below is either *verified* (observed) or explicitly marked *not verified*.

## What was verified

| Check | Result |
|---|---|
| Is `https://kisskh.do` reachable? | **Yes** — TLS connects and the server (Cloudflare) answers. |
| Does it serve pages to a normal HTTP client? | **No.** `GET /robots.txt` returned **HTTP 403** with header `cf-mitigated: challenge`: a Cloudflare *managed challenge* (anti-bot / browser check). |
| Documented, authorized API? | **None found.** Nothing reachable without passing the challenge; no developer documentation, terms-of-API or partner programme was discoverable. |
| Licensing / permission to embed or re-stream | **Not verified, and no evidence of any.** I found nothing indicating the site licenses its video for third-party apps. |

## What was NOT verified (and why)

Everything behind the challenge: home page structure, catalog, episode lists, player type, subtitle formats, direct
episode URLs, whether an Android TV browser/WebView passes the challenge. Reading any of that would mean bypassing or
automating around an anti-bot safeguard (or using undocumented endpoints), which is out of scope for this project, so
**none of it was attempted and nothing about it is claimed.**

## Capability matrix for `KissKhContentProvider`

| Capability | Status | Reason |
|---|---|---|
| Automatic catalog / browse from the site | ❌ Not implemented | Anti-bot challenge; no documented API. |
| Search the site | ❌ Not implemented | Same. (Search inside *your saved list* works.) |
| Episode lists / thumbnails / metadata from the site | ❌ Not implemented | Same. Metadata comes only from what you type into your list file. |
| Native Media3 playback of KissKH video | ❌ Not possible / not attempted | Would require extracting streams without authorization; the app never does this. |
| Subtitles from KissKH | ❌ Not available natively | Subtitles exist only inside the website's own player. |
| Direct episode links | ✅ Supported **as links you provide** | You (or a family helper) paste episode page addresses into `kisskh_library.json`. Only `https://kisskh.do` and its subdomains are accepted. |
| Netflix-style browsing, favorites, Continue Watching over that saved list | ✅ Implemented | Local list + Room database. Needs a laptop/TV test. |
| Opening an episode on the website | ✅ Implemented — **works only if the TV has a browser** | Uses a standard `ACTION_VIEW` intent. Many Google TV / Android TV devices ship **without** a web browser; a browser must be installed (from the Play Store) before this works. Whether the site's challenge passes in that browser is **not verified**. |
| Playback progress for website playback | ❌ Impossible | The app cannot observe what happens in another app. It records only "page opened" (shown as *Opened before*), never "watched", and never shows a progress bar for it. |

## How the family list works

1. Copy `docs/kisskh_library.example.json`, fill in titles and real page addresses (only `https://kisskh.do/...`).
2. Name it `kisskh_library.json` and push it to the TV:
   `adb push kisskh_library.json /sdcard/Android/data/com.dramaease.tv/files/`
3. In DramaEase: Settings → "Where dramas come from" → KissKH list. Pressing **Watch** opens the page in the TV's browser.

Invalid entries (missing title, non-kisskh.do links) are silently skipped, never "fixed".

## Legal / rights note

Household use of a site does not by itself give anyone the right to re-publish or repackage its streams. I could not
establish that kisskh.do holds licences for what it shows, so DramaEase deliberately links out instead of extracting
or re-streaming anything. Check what applies where you live before relying on the site.

## Path to a fuller integration

A native catalog/player is only appropriate if the site operator (or rights holders) provide a documented API or written
permission. If that ever exists, implement it inside `KissKhContentProvider` — the UI needs no changes.
