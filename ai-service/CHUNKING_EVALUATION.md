# P3 Qualitative Chunking Experiment

P3 is a small offline experiment for inspecting chunk boundaries. It does not
use Gemini embeddings, retrieval metrics, PostgreSQL, pgvector, or production
data mutation. The production pipeline remains:

```text
JSON corpus
→ canonical Markdown in memory
→ MarkdownHeaderTextSplitter
→ RecursiveCharacterTextSplitter 800/100 only for oversized sections
→ embedding
→ pgvector
```

## Step 1 — Sample dataset

The experiment uses six representative places from the existing project
corpus. The list is explicit in `scripts/evaluate_chunking.py`:

| Place | Representative knowledge |
|---|---|
| `dinh-doc-lap` | history and architecture |
| `buu-dien-trung-tam-sai-gon` | urban architecture |
| `bao-tang-my-thuat-tphcm` | art and museum content |
| `cho-ben-thanh` | market and local culture |
| `thao-cam-vien-sai-gon` | nature and leisure |
| `chua-ba-thien-hau` | religion and architecture |

No external articles, blog posts, or generated documents are used.

The measured source-section lengths use characters (`len`): minimum about 263,
median about 366, p95 about 433, and maximum about 480. All current semantic
sections are below the production 800-character fallback threshold. Therefore,
the current Markdown Header pipeline normally preserves the four natural
sections `OVERVIEW`, `BACKGROUND`, `HIGHLIGHTS`, and `EXPERIENCE` without
secondary recursive splitting. The 800-character splitter remains a safety
fallback for future larger sections; it is not claimed to be mathematically
optimal.

## Step 2 — Configurations

The same canonical Place Markdown is inspected under three configurations:

### A. RecursiveCharacterTextSplitter 500/50

```python
RecursiveCharacterTextSplitter(
    chunk_size=500,
    chunk_overlap=50,
)
```

This generic splitter is applied directly to the full canonical Place document.

### B. RecursiveCharacterTextSplitter 800/100

```python
RecursiveCharacterTextSplitter(
    chunk_size=800,
    chunk_overlap=100,
)
```

This is also applied directly to the full canonical Place document.

### C. Production Markdown Header pipeline

The experiment calls the existing production `split_place_corpus()` function:

```text
MarkdownHeaderTextSplitter
→ RecursiveCharacterTextSplitter 800/100 only for an oversized section
```

The generated report includes the preserved semantic `section` metadata for
these chunks. Production code is reused, not duplicated or changed.

## Step 3 — Qualitative evaluation

Generate the report from `ai-service`:

```bash
python -m scripts.evaluate_chunking \
  --output chunking-samples.md
```

Without `--output`, the same `chunking-samples.md` path is used by default.
The command works without a Gemini API key, database, or network access.

For every place and configuration, the report contains the actual chunks plus:

- number of chunks;
- minimum chunk length;
- average chunk length;
- maximum chunk length.

Lengths are always characters. The report also contains an aggregate summary
and this manual 1–3 rubric:

| Criterion | Recursive 500/50 | Recursive 800/100 | Markdown Header |
|---|---:|---:|---:|
| Avoids unnatural cuts | 1–3 | 1–3 | 1–3 |
| Topic coherence | 1–3 | 1–3 | 1–3 |
| Context preservation | 1–3 | 1–3 | 1–3 |
| Independently understandable | 1–3 | 1–3 | 1–3 |
| Reasonable chunk count/size | 1–3 | 1–3 | 1–3 |

`1 = poor`, `2 = acceptable`, and `3 = good`. The script does not assign
scores automatically; the developer reads and scores the generated chunks.

## Project-defense explanation

The corpus is already normalized into semantic sections such as Overview,
Background, Highlights, and Experience. Therefore,
`MarkdownHeaderTextSplitter` is used to preserve these natural semantic
boundaries. It is compared with generic `RecursiveCharacterTextSplitter`
configurations on representative corpus samples, and chunk coherence, context
preservation, independent readability, and chunk size are evaluated manually.
Recursive splitting with an 800-character threshold is retained as a fallback
for future oversized sections.

This small qualitative experiment validates the technical fit of the production
strategy for the current corpus. It does not prove global optimality. The P1
retrieval evaluator and its Recall, MRR, ILD, and MMR experiments remain
separate and unchanged.
