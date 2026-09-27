# Decisions

## How it works
```
Client → question → AI agent → reads SKILL.md → runs our jar (command + args)
                                                  ├─ validate args
                                                  ├─ resolve titles      (Wikidata)
                                                  ├─ fetch views + cache (Wikipedia API)
                                                  ├─ clean data          (drop incomplete month)
                                                  ├─ calculate           (stats + confidence)
                                                  └─ print JSON  |  report: PNG + PDF
AI ← JSON → AI writes the answer to the client
```
- Our app never sees the client's question. Only the command.
- Our app never calls an AI. The AI calls our app.
- One command run = one call. The app prints JSON and exits.
- The AI writes the final answer, not our code.

## Business cases
| Case | Command shape |
|---|---|
| Add a language | 1 topic × many langs |
| Add a course | 1 topic × 1 lang, focus on trust |
| Add a theme to a course | many topics × 1 lang |

## Commands (only two — fewer is better for Haiku)
- `analyze` — stats + comparison for N topics × N langs. Prints JSON.
- `report` — same input, creates chart PNG + one-page PDF. Prints JSON with file paths.

Follow-ups ("add Slovak", "make it 3 years") = same command, changed args.
No session files — the AI remembers the conversation.

## Statistics
- **growth %** — last 12 months vs previous 12.
- **trend** — `rising | falling | flat` (Commons Math regression: slope, R²).
- **vs whole Wikipedia** — topic growth compared with the language's total views.
- **median** — volume measure, spike detection (month > 3× median = spike).
- **seasonality** — repeating peaks (e.g. every September).
- **confidence** — `low | medium | high` + list of reasons. Checks:
  enough views, steady trend (R²), growth not from one spike, beats whole-Wikipedia trend.

## Report (PDF)
- One A4 page, max 12 topic × language pairs.
- Chart shows an index (first 12 months = 100), not raw views: a small wiki is visible next to a big one.
- Bullet text is built by fixed rules from the numbers, not by an AI. Same data → same PDF.

## Data rules
- Wikipedia + Wikidata only. Other sources (Google Trends) → README roadmap.
- Topic comes in English. Wikidata finds the title per language.
- Monthly data, `agent=user` (humans, no bots).
- Drop the current / incomplete month.
- Cache: in-memory `HashMap`, lives for one command run only (no files, no DB).
  Same request inside one run goes to the API once. Follow-ups (new command) fetch again.
- Send a `User-Agent` header (Wikimedia requires it). Wikimedia rate-limits bursts (HTTP 429) → retry with pauses.

## Environment variables (all optional)
| Var | Use |
|---|---|
| `INTERESTER_CONTACT` | Email/URL added to User-Agent (Wikimedia asks for contact info). |
| `HTTPS_PROXY` | Used if set. Needed in sandboxed agents (the JDK ignores it by default). |

## Stack
- Plain Java 25, Maven. No Spring, no Spring AI.
- Maven wrapper (`mvnw`) + Shade plugin → one runnable jar. Jar is built by the user, not committed.
- `java.net.http.HttpClient` — HTTP.
- Jackson — JSON.
- Apache Commons Math — regression.
- JFreeChart — charts.
- OpenPDF — PDF.
- picocli — command-line args (optional).
- stdout = JSON only. Logs → stderr or off.

## Layout
This project folder **is** the skill folder.
```
interester/
├── SKILL.md       ← instructions for the AI
├── README.md      ← for humans
├── pom.xml, mvnw
├── doc/           ← our planning docs
└── src/
```

## Real data examples (checked 2026-09-24/25)
- `en` "Intermittent fasting": ~7–10k views/month in 2026. The "218 in Aug" seen earlier was **our bug**:
  end date `20260801` returns only 1 day of August. Always end on the last day of the month.
- `pl` has **no** "Intermittent fasting" article (example prompt 1!) → must return `found: false` with a hint.
- "English as a second or foreign language" has no `pl`/`uk` article → use "English language" as a proxy.
- `uk` "Астрономія", Aug 2024–Jul 2026: 6,723 vs 16,993 → **−60.4%**.
  Whole uk Wikipedia: **−23.8%**. September spikes (school year). Without spikes still ≈ −59%.

## Known limits (must appear in README and reports)
- Views ≠ willingness to pay.
- Language ≠ country.
- Redirects are counted separately.
- Wikipedia traffic overall is falling.
