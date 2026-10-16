# Laptop embedding comparison

Small observed development fixtures; no held-out accuracy, no Android performance, scope replacements include proposals.

Environment: macOS-27.0.1-arm64-arm-64bit; Python 3.12.14. CPU only. FP32 candidates: 4 threads, batch 16. Bundled ONNX: 2 threads, sequential per-note app pooling/chunking. Timing includes Python runtime overhead; one run, not a controlled mobile comparison.

| Model | Weight MiB | Dev AUC / AP | Dev recall@5 / @10 | Transfer AUC / AP | Transfer recall@5 / @10 | Scope margins > 0 | Warm median / p95 ms | Peak RSS MiB |
|---|---:|---|---|---|---|---:|---|---:|
| production-onnx | 22.0 | 0.902 / 0.962 | 0.803 / 0.939 | 0.878 / 0.991 | 0.855 / 0.971 | 11/15 | 1.1 / 1.3 | 291 |
| minilm | 86.7 | 0.916 / 0.968 | 0.788 / 0.939 | 0.887 / 0.991 | 0.855 / 0.971 | 10/15 | 2.7 / 3.3 | 435 |
| bge-small | 127.3 | 0.937 / 0.975 | 0.864 / 0.894 | 0.864 / 0.988 | 0.783 / 0.920 | 11/15 | 4.9 / 5.0 | 476 |
| granite-small | 90.9 | 0.951 / 0.980 | 0.803 / 0.985 | 0.893 / 0.991 | 0.841 / 0.964 | 9/15 | 5.4 / 5.5 | 669 |

Scope counts are diagnostic contrasts, not classification accuracy. Unknown note pairs are excluded from pair metrics, not treated as negatives. Recall counts directed known-positive links, excluding self matches.

## Scope contrasts

| Note | Preferred / rejected (some preferred labels are proposed) | production-onnx | minilm | bge-small | granite-small |
|---|---|---:|---:|---:|---:|
| 120 | Rosemary / Basil | +0.123 | +0.150 | +0.051 | +0.099 |
| 35 | Running / Running gear | +0.073 | +0.109 | +0.067 | +0.036 |
| 103 | Running / Running gear | +0.020 | +0.037 | +0.041 | +0.017 |
| 108 | Meal preparation / Running gear | +0.256 | +0.252 | +0.118 | +0.085 |
| 28 | Focused work / Chat | -0.172 | -0.180 | -0.088 | -0.096 |
| 94 | Focused work / Chat | -0.155 | -0.157 | -0.094 | +0.007 |
| 72 | Walking / Meetings | -0.262 | -0.280 | +0.008 | -0.049 |
| 161 | Walking / Meetings | -0.306 | -0.286 | -0.021 | -0.047 |
| 57 | Meetings / Meeting planning | +0.044 | +0.032 | -0.065 | -0.034 |
| 145 | Reading notes / Chapter | +0.005 | +0.003 | +0.102 | +0.015 |
| 147 | Piano practice / Phrases | +0.284 | +0.254 | +0.191 | +0.061 |
| 151 | French practice / Phrases | +0.068 | +0.046 | +0.002 | +0.019 |
| 63 | Note-taking / Phrases | +0.019 | +0.037 | +0.005 | -0.016 |
| 46 | Running gear / Running | +0.008 | +0.003 | +0.036 | -0.012 |
| 109 | Running gear / Running | +0.024 | -0.009 | +0.045 | +0.027 |

Positive margin means greater cosine similarity to the preferred label. Full scores, evidence qualifiers, pair scores, pinned artifacts and timings are in [results JSON](EMBEDDING_COMPARISON_RESULTS.json). See [protocol](EMBEDDING_BENCHMARK_PROTOCOL.md) for licensing and evaluation constraints.
