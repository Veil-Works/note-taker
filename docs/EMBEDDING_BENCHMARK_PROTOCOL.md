# Laptop embedding comparison

Protocol fixed before the first candidate inference, 2026-09-29.

## Commercial product requirement

The app is intended for sale. Every shipped model, tokenizer, runtime, and other
dependency must permit the intended commercial use and distribution. Research-only,
non-commercial, or unlicensed weights are excluded unless suitable rights are
obtained separately. Preserve license texts, applicable notices, exact revisions,
and any required modification notices for exported/quantized weights.

The first comparison uses MiniLM (Apache-2.0), BGE-small (MIT) and Granite small
English R2 (Apache-2.0). These licenses permit commercial distribution subject to
their conditions. This selection does not require releasing the app's proprietary
code under either license. Model-specific licensing evidence and hashes are retained
alongside `scripts/embeddings/model-manifest.json`.

Sources: [MiniLM model card](https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2),
[BGE model card](https://huggingface.co/BAAI/bge-small-en-v1.5),
[Granite model card](https://huggingface.co/ibm-granite/granite-embedding-small-english-r2),
[Apache 2.0 terms](https://www.apache.org/licenses/LICENSE-2.0),
[MIT terms](https://opensource.org/license/mit).

Weight licensing and training-data provenance are separate facts. IBM reports
commercial-friendly data governance and avoidance of MS MARCO in this model's
training. That is useful provenance, not a universal legal guarantee. BGE's publisher
explicitly permits commercial use of its released models. Do not infer that a
training-dataset restriction automatically transfers to weights, or that permissive
weight licensing settles every possible third-party claim. No external training
dataset is downloaded, redistributed or used for fine-tuning in this experiment.

Custom-license candidates such as EmbeddingGemma are deferred from this first round,
not declared inherently non-commercial. Review their exact redistribution terms
before future inclusion. Commercial suitability is an ongoing product constraint,
not a claim that the entire app has received a complete distribution audit.

## Experimental boundary

- Run on this laptop, using CPU float32 and four PyTorch threads for all three
  Sentence Transformers models. Record the environment, versions and timing.
- Use each pinned model's published tokenizer and pooling, normalized vectors,
  and no retrieval query prefix for this symmetric clustering/similarity task.
- Keep inference local and disable remote custom model code. Model preparation
  downloads artifacts but never reads or uploads notes. Benchmark execution is offline.
- Add the bundled quantized ONNX MiniLM as a separate production-representation
  reference. Mirror the app's tokenization, chunking, per-chunk normalization and
  weighted aggregation; verify tokenizer parity with concrete cases. Do not label
  desktop float32 inference as identical to production quantization.
- Save vectors locally and record model/configuration/content hashes. Recompute
  every run rather than reuse cached vectors. Use fresh worker
  processes for isolated model timings and maximum resident memory. Record loading,
  encoding, and warm single-note latency separately; do not translate laptop timing
  into Android battery or latency claims.

## Measurements

1. Selected-pair separation: ROC AUC and average precision over the 33 positive /
   13 negative pairs already recorded for the 172-note notebook. Their class balance
   is artificial; neither metric is a production accuracy estimate.
2. Known-positive neighbor recall at 5 and 10 within the full notebook. Unannotated
   neighbors are not counted as negatives and precision is not estimated.
3. The same diagnostics on the previously observed 40-note transfer corpus, using
   its subject memberships and explicit negative pairs. Its historical file name
   says heldout, but it is now an observed regression set.
4. Label-scope contrasts from the user review. Report every preferred/rejected
   label cosine and margin, alongside the basis/uncertainty of the annotation.
   Some replacement labels are proposed; do not present the win count as human
   approval or calibrated membership accuracy.
5. Errors of special interest: cancellation versus subject similarity, piano versus
   language phrases, rosemary versus basil, running versus equipment, meal preparation
   versus running gear, focused work versus chat, and walking versus meetings.

No threshold is selected on these results and no clustering algorithm or app behavior
is changed in this phase. Model-specific threshold calibration, topic discovery,
label validation, multilingual evaluation and a new human-reviewed untouched test
set follow separately. A winner on these development probes is a candidate for those
experiments, not a production replacement.
