---
name: interester
description: Compare interest in topics across Wikipedia language editions (monthly pageviews), check how much to trust a trend, and make a one-page PDF report with a chart. Use when a user asks which topic, course, or language audience is growing, wants to compare interest between languages or topics, or asks for a short report for founders or a product team.
compatibility: Needs Java 25+ and internet access to wikimedia.org and wikidata.org.
---

# Wikipedia interest analysis

Our tool fetches the data and does all the math. **You never calculate numbers yourself.**
Your job: turn the question into one command, run it, and explain the JSON in plain words.

Run every command from this skill's folder (the folder with this file).

## 1. Setup (once)
If `target/interester.jar` does not exist, build it:
```
./mvnw -q package -DskipTests
```
(`mvnw.cmd` on Windows.) Build fails with "release version 25 not supported" → tell the user to install Java 25.

## 2. Build the command
```
java -jar target/interester.jar analyze --topic "<English article title>" --langs <codes>
```
- The user says "report", "PDF", "chart" or "share" → use `report` (answer + PDF + PNG chart).
  Otherwise use `analyze` (answer in chat only).
- `--topic`: the **English Wikipedia article title**, in quotes. Repeat for more topics:
  `--topic "Astronomy" --topic "Amateur astronomy"`. Pick a general, existing article.
  For ideas like "learning English" use the closest article ("English language") and tell the user it is a proxy.
- `--langs`: Wikipedia language codes, comma-separated, max 10. Any Wikipedia language code works. Language, not country:
  uk = Ukrainian, cs = Czech, pl = Polish, de = German, sk = Slovak, tr = Turkish, sv = Swedish,
  da = Danish, el = Greek, ja = Japanese, ko = Korean, zh = Chinese, pt = Portuguese, et = Estonian.
- `--months 36` for 3 years (12–120, default 24 = "last two years").
- `--end 2026-06` only if the user names an end month. Default is the last complete month.
- `report` only: at most 12 topic × language pairs. `--out <folder>` if the user wants the files somewhere else (default `./out`).

If the user did not say which languages, **ask**. Do not guess.

## 3. Read the JSON
The tool prints one JSON object on stdout. Ignore other lines such as `Picked up JAVA_TOOL_OPTIONS`.

`"status": "error"` → do what `error.hint` says (fix the args and run again).
`API_ERROR` → wait 60 seconds, run the same command once more. Still failing → tell the user.

`"status": "ok"` → `summary` is the verdict, already written from the numbers. Use it as your answer.
For each item in `results`:

| Field | Use it like this |
|---|---|
| `found: false` | No article in that language. Tell the user. Follow `error.hint` only if you know the real title in that language. |
| `growthVsWikiPct` | **Main number.** Change of the topic's share of all views in that language (last 12 months vs the 12 before). Above +10% = grows faster than Wikipedia. −10% to +10% = moves with Wikipedia: **no signal**, neither good nor bad. Below −10% = interest falls faster than Wikipedia. It is a percent change, say "share fell 47%", not "percentage points". |
| `growthPct` | Raw change of views. Wikipedia traffic falls overall, so a negative number alone is not bad news. Always show it next to `wikiGrowthPct`. |
| `monthlyMedian` | Typical (median) views per month over the whole period. Tells how big the audience is. |
| `last12`, `previous12` | Total views of 12 months (not per month). Usually no need to show them. |
| `trend.direction` | rising / falling / flat. Use when there is no `growthVsWikiPct` (period under 24 months). |
| `seasonalPeak` | Month that peaks every year (`09` = September). Good time for launches. |
| `confidence` + `reasons` | How much to trust the result. **Always** say the confidence and explain it with the `reasons[].text`. Use only those reasons, never add your own. |

- `ranking` is already sorted best first. Use it. Do not re-sort.
- `notes` are important facts (e.g. period moved, title redirected). Mention them.
- `files.pdf` and `files.chart` (report only) are absolute paths. Give them to the user.

## 4. Answer
Fill in this template. Nothing more: no extra sections, no own recommendations.
With `report` the PDF is the full report, so the chat answer stays this short.

```
**Answer:** <the `summary` lines, as they are>
<only if you used a proxy article: I used the Wikipedia article "<article>" as a proxy for "<user's idea>".>
<only if more than one result: **Order:** one line per result in `ranking` order:
 "<Language>: share <growthVsWikiPct>% → grows faster than Wikipedia / moves with Wikipedia, no signal / falls faster than Wikipedia".
 This is the "which first and why". Do not add more reasons.>

| Language | Views/month | Growth | Whole wiki | Share change | Confidence |
|---|---|---|---|---|---|
<one row per result, in `ranking` order; then "no article" rows for `found: false`>

**Trust:**
- <Language>: <confidence>. <its reasons[].text, as they are>

**Limits:**
- Views show interest, not willingness to pay.
- A language edition is not a country: <Language> Wikipedia readers can live anywhere.
- Only the one article is counted, not related pages.

<report only: **Files:** <files.pdf>, <files.chart>>
```

Rules:
- Only numbers from the JSON, rounded to whole percents. Do not compare them yourself ("largest", "fastest").
- Share change between −10% and +10% is "moves with Wikipedia, no signal", never a decline or a growth.
- Do not judge `r2` yourself. Talk about **interest**, never markets or revenue.

## 5. Follow-ups
Run the same command again with changed args. Do not reuse old numbers for the new question.
- "Add Slovak" → add `sk` to `--langs`.
- "Make it 3 years" → `--months 36`.
- "Now as a PDF" → same args with `report`.
- "Compare with X" → add `--topic "X"`.

## Examples
- "Intermittent fasting in Polish vs Czech, last two years" →
  `analyze --topic "Intermittent fasting" --langs pl,cs` (Polish has no article: say so).
- "Is interest in astronomy growing in Ukrainian? Can we trust it?" →
  `analyze --topic "Astronomy" --langs uk`
- "Interest in learning English in de, pl, tr, uk, short report" →
  `report --topic "English language" --langs de,pl,tr,uk`

More detail about every field and rule: `doc/contract.md` (read only if something above is unclear).
