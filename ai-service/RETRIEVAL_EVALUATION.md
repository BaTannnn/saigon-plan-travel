# RAG Retrieval Evaluation

## Purpose

This evaluator measures retrieval quality before grounded LLM generation. It
uses 20 manually curated cases in `tests/retrieval_cases.json`. The cases cover
history, museums, architecture, religion, markets, local culture, nature,
family activities, urban sightseeing, photography, and mixed preferences.

Evaluation results are used to justify retrieval configuration choices instead
of selecting Top-K, MMR lambda, or retrieval strategies purely by intuition.

## Current baseline

The production recommendation retrieval flow remains:

```text
dense retrieval
→ place-level deduplication
→ MMR
```

The evaluator compares two configurations using the same production retrieval
functions:

```text
semantic baseline: dense retrieval → place-level deduplication
current pipeline:  dense retrieval → place-level deduplication → MMR
```

MMR lambda remains configurable in the evaluator. Based on the current
20-case experiment, production uses `0.9` while semantic ranking remains the
evaluation baseline.

## Measured experiment

| Configuration | Recall@5 | Recall@10 | Recall@15 | MRR | ILD@5 | ILD@10 | ILD@15 |
|---|---:|---:|---:|---:|---:|---:|---:|
| Semantic | 0.8934 | 0.9760 | 1.0000 | 0.9750 | 0.2623 | 0.2801 | 0.2875 |
| MMR lambda=0.5 | 0.6446 | 0.8678 | 0.9381 | 0.9750 | 0.3174 | 0.3044 | 0.3018 |
| MMR lambda=0.7 | 0.8434 | 0.9229 | 0.9795 | 0.9750 | 0.2909 | 0.2941 | 0.2956 |
| MMR lambda=0.9 | 0.9101 | 0.9593 | 0.9962 | 0.9750 | 0.2648 | 0.2840 | 0.2898 |

Among the tested values on the current evaluation set, `0.9` provides the
best relevance-diversity trade-off: it has the strongest Recall@5, remains
close to semantic ranking at larger K values, and keeps a small diversity gain
over the semantic baseline. This is not a claim that `0.9` is globally optimal;
the conclusion applies only to the tested values, corpus, and evaluation cases.

## Metrics

### Recall@K

Recall@K is the fraction of expected relevant places found in the first `K`
retrieved places. The evaluator reports Recall@5, Recall@10, and Recall@15,
averaged across all cases.

### MRR

Reciprocal Rank is `1 / rank` for the first relevant retrieved place, or `0`
when no relevant place is retrieved. Mean Reciprocal Rank (MRR) averages this
value across all cases.

### ILD@K

Intra-List Diversity measures how different the retrieved places are from one
another. For every unordered pair in the first `K` places, the evaluator
calculates `1 - cosine_similarity`, then averages those dissimilarities. A list
with fewer than two places has ILD `0.0` because it contains no pair to compare.

The evaluator reports ILD@5, ILD@10, and ILD@15. A missing embedding fails
clearly instead of silently excluding the place.

Every case must contain at least one expected relevant place. An empty ground
truth is invalid and fails clearly because it cannot produce a meaningful
Recall or Reciprocal Rank value.

## How to run

From `ai-service`, with PostgreSQL running and Gemini credentials configured:

```bash
python -m scripts.evaluate_retrieval
```

To compare several MMR lambda values:

```bash
python -m scripts.evaluate_retrieval --mmr-lambdas 0.5 0.7 0.9
```

The command prints aggregate Recall@K, MRR, and ILD@K for each configuration.
Add `--show-cases` to include per-case rankings.

To also write a machine-readable result:

```bash
python -m scripts.evaluate_retrieval \
  --mmr-lambdas 0.5 0.7 0.9 \
  --output evaluation-results.json
```

Dataset loading rejects duplicate IDs, empty queries, empty or duplicate
relevant slugs, and expected slugs that are absent from the current corpus.

The deterministic metric unit tests do not need PostgreSQL or a Gemini API key.
The live evaluator does require both because it runs real production embedding
and vector retrieval code; it does not fabricate retrieval results.
