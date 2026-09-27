# Contract

The AI agent runs our jar. Input = command-line args. Output = one JSON object on stdout.
Nothing else goes to stdout. Logs go to stderr.

```
java -jar target/interester.jar <command> [args]
```

## Commands
| Command | What it does |
|---|---|
| `analyze` | Stats + comparison for N topics × N langs. Prints JSON. |
| `report`  | Same as `analyze` + creates chart PNG and one-page PDF. Prints the same JSON + file paths. |

## Args (same for both commands)
| Arg | Required | Example | Rule |
|---|---|---|---|
| `--topic` | yes, repeatable | `--topic "Intermittent fasting" --topic Astronomy` | Title of the **English** Wikipedia article. |
| `--langs` | yes | `--langs pl,cs,sk` | Wikipedia language codes, comma-separated. Max 10. |
| `--months` | no, default `24` | `--months 36` | 12–120. Counted back from `--end`. |
| `--end` | no, default last complete month | `--end 2026-07` | `YYYY-MM`. Never the current month. |
| `--article` | no, repeatable | `--article cs:Šachy` | Manual title for one language. Used when Wikidata finds nothing. Applies to a single `--topic` only. |
| `--out` | `report` only, default `./out` | `--out ./out` | Folder for PNG + PDF. Created if missing. |

`report` limit: topics × langs ≤ 12, so the PDF stays one page. More → `INVALID_ARGS`.

Why `--topic` is repeatable and not comma-separated: titles can contain commas (`Washington, D.C.`).

## Output: `analyze`
```json
{
  "status" : "ok",
  "summary" : [ "No growing interest found. Best result: Ukrainian (uk), share of Wikipedia views -48.0%. Test demand another way before investing.", "Views peak every year in September. Plan launches and campaigns for it." ],
  "query" : {
    "topics" : [ "Astronomy" ],
    "langs" : [ "uk" ],
    "from" : "2024-08",
    "to" : "2026-07",
    "months" : 24
  },
  "results" : [ {
    "topic" : "Astronomy",
    "lang" : "uk",
    "article" : "Астрономія",
    "found" : true,
    "last12" : 6723,
    "previous12" : 16993,
    "growthPct" : -60.4,
    "wikiGrowthPct" : -23.8,
    "growthVsWikiPct" : -48.0,
    "monthlyMedian" : 629,
    "trend" : {
      "direction" : "falling",
      "slopePctPerMonth" : -8.5,
      "r2" : 0.4
    },
    "spikes" : [ {
      "month" : "2024-09",
      "views" : 4687
    } ],
    "seasonalPeak" : "09",
    "confidence" : "high",
    "reasons" : [ {
      "code" : "DIFFERS_FROM_WIKI",
      "text" : "Does worse than Ukrainian Wikipedia overall (-23.8%). Its share of all views changed -48.0%."
    }, {
      "code" : "SEASONAL",
      "text" : "Views peak every September."
    } ]
  } ],
  "ranking" : [ {
    "rank" : 1,
    "topic" : "Astronomy",
    "lang" : "uk",
    "growthVsWikiPct" : -48.0,
    "confidence" : "high"
  } ],
  "notes" : [ ]
}
```
Real output: `analyze --topic Astronomy --langs uk --end 2026-07` (2026-09-25).

### Field meaning
| Field | Meaning |
|---|---|
| `summary` | The verdict in plain words, built by fixed rules from the numbers (same text as "What to research next" in the PDF). The AI uses it as its answer. |
| `last12`, `previous12` | Sum of views: last 12 months, and the 12 before. Absent if `--months` < 24 (also `growthPct`, `wikiGrowthPct`, `growthVsWikiPct`). |
| `growthPct` | `(last12 / previous12 − 1) × 100`. |
| `wikiGrowthPct` | Same formula for the whole language Wikipedia. |
| `growthVsWikiPct` | Growth of the topic's **share** of all views in that language. Main number for comparing languages. |
| `monthlyMedian` | Median monthly views over the whole period. Measure of volume. |
| `trend.direction` | `rising` \| `falling` \| `flat`. |
| `trend.slopePctPerMonth` | Regression slope as % of the mean, per month. |
| `trend.r2` | 0–1. How steady the trend is. |
| `spikes` | Months with views > 3 × median. |
| `seasonalPeak` | Month (`01`–`12`) that peaks every year, or `null`. |
| `confidence` | `high` \| `medium` \| `low`. |
| `reasons` | Why this confidence. Always present. The AI must mention them. |
| `ranking` | Found results sorted by `growthVsWikiPct`, high to low. Under 24 months: by trend slope. |
| `notes` | Facts about the data the AI should know. |

Rule: a field with no value is **left out** of the JSON (never `null`).

### Reason codes
Thresholds are in `TopicAnalyzer` (checked on 7 real series).
| Code | Effect | Meaning |
|---|---|---|
| `LOW_VOLUME` | ↓↓ | Median < 100 views/month. Always `low`. |
| `NOISY_TREND` | ↓ | R² < 0.3. |
| `SPIKE_DRIVEN` | ↓ | Spike months replaced by the median → growth changes sign or loses more than half. |
| `SHORT_PERIOD` | ↓ | Less than 24 months. |
| `FOLLOWS_WIKI` | ↓ | \|growthVsWikiPct\| < 10: topic just moves with the whole Wikipedia. |
| `STEADY_TREND` | ↑ | R² ≥ 0.6. |
| `DIFFERS_FROM_WIKI` | ↑ | \|growthVsWikiPct\| ≥ 10: topic clearly differs (better or worse). |
| `SEASONAL` | info | Same top month (≥ 1.5 × that year's median) in ≥ 2/3 of the years, at least 2. |

With 24+ months (and views in both years) every result has `FOLLOWS_WIKI` or `DIFFERS_FROM_WIKI`.
Confidence: `LOW_VOLUME` or 2+ ↓ → `low`. One ↓, or no ↑ → `medium`. Otherwise `high`.
Trend `flat` = slope under 1% of the mean per month. With `--months` > 24, growth uses the last 24 months only.

## Not found (normal result, not an error)
```json
{
  "topic": "Intermittent fasting",
  "lang": "pl",
  "found": false,
  "error": {
    "code": "ARTICLE_NOT_FOUND",
    "message": "No pl Wikipedia article linked to 'Intermittent fasting'.",
    "hint": "Tell the user. Or retry with --article pl:<title in that language> if you know one."
  }
}
```
Real case: Polish Wikipedia has no "Intermittent fasting" article (checked 2026-09-25).

## Errors (whole command failed)
```json
{
  "status": "error",
  "error": {
    "code": "INVALID_ARGS",
    "message": "Unknown language code 'ua'.",
    "hint": "Use Wikipedia codes: uk = Ukrainian, cs = Czech."
  }
}
```
| Code | Exit code | When |
|---|---|---|
| — (ok) | 0 | Success, even if some results are `found: false`. |
| `INVALID_ARGS` | 2 | Bad or missing args. |
| `TOPIC_NOT_FOUND` | 2 | No English article with this title. Hint: check the exact English title. |
| `API_ERROR` | 3 | Wikipedia/Wikidata down or rate limit. Hint: retry later. |
| `INTERNAL_ERROR` | 1 | Bug in our code. Stack trace goes to stderr. Hint: do not retry. |

Every error has a `hint`. It tells the AI what to do next.

## Output: `report`
Same JSON as `analyze`, plus:
```json
"files": {
  "chart": "/abs/path/out/astronomy-uk-2024-09-2026-08.png",
  "pdf":   "/abs/path/out/astronomy-uk-2024-09-2026-08.pdf"
}
```
- Paths are absolute. The same command overwrites the same files.
- File name = topics + langs + period.
- PDF (one A4 page): "What to research next" bullets, chart, numbers table (with yearly peak and confidence),
  notes, limits, source. Reason texts are only in the JSON.
- Chart: one line per topic × language, as an index (average of the first 12 months = 100), so big and small
  wikis fit one chart.
- The PDF text is made from the data by fixed rules (no AI). The AI still writes its own answer from the JSON.

## The 3 example prompts as commands
1. "Intermittent fasting in Polish vs Czech, last two years"
   ```
   analyze --topic "Intermittent fasting" --langs pl,cs
   ```
   → `pl` comes back `found: false`. The AI says so, and can offer a related topic.
2. "Astronomy course — is interest growing in Ukrainian, can we trust it?"
   ```
   analyze --topic Astronomy --langs uk
   ```
3. "Learning English in our selected languages + short report"
   ```
   report --topic "English language" --langs de,pl,tr,uk
   ```
   If the user did not name languages, the AI asks first.
   Why not "English as a second or foreign language": it has no `pl` or `uk` article (checked 2026-09-25).
   "English language" exists in all 4. The AI must say it is a proxy for "learning English".

Follow-ups:
- "Add Slovak" → same command, `--langs pl,cs,sk`.
- "Make it 3 years" → same command, `--months 36`.

## Wikipedia APIs we call (for Task 3)
Every request sends `User-Agent: interester/1.0 (<contact>)`.
- Title per language (Wikidata):
  `https://www.wikidata.org/w/api.php?action=wbgetentities&sites=enwiki&titles=<T>&props=sitelinks&normalize=1&format=json`
  No sitelink for a language = not found.
- Article views:
  `https://wikimedia.org/api/rest_v1/metrics/pageviews/per-article/<lang>.wikipedia/all-access/user/<Title>/monthly/<YYYYMM01>/<YYYYMMDD last day>`
  404 = no data.
- Whole-language views:
  `https://wikimedia.org/api/rest_v1/metrics/pageviews/aggregate/<lang>.wikipedia/all-access/user/monthly/<YYYYMM0100>/<YYYYMMDD00 last day>`

**Trap:** the end date must be the **last day** of the end month.
With end `20260801` the API returns August with only 1 day (218 views instead of ~7,000).
