# Optional local LLM: three-way comparison

First run completed: [findings and timings](HYBRID_COMPARISON_FIRST_PASS.md).

## Product decision

The base app continues to work offline with bundled MiniLM and deterministic rules.
The generative model is an **optional separate download**, enabled by user choice.
There are two planned user-facing choices: Standard and Enhanced organization.
The label-only variant below is an experiment, not necessarily a third setting.

This change prepares and runs the comparison. Download/settings UI and production
hybrid membership writes are not enabled by this prototype. No experimental model
output is applied to the user's journal.

### Required behavior for eventual app integration

- Fresh installs select Standard; neither download nor enable the LLM automatically.
- Explain model download size and local processing before the user downloads it.
- Download to a temporary file, verify pinned size and SHA-256, then enable atomically.
- Support cancellation, interrupted-download retry, insufficient storage, and deletion.
- A partial, corrupt, missing, incompatible, or failing model leaves Standard usable.
- Run organization asynchronously after saving a note; cache by text, model, prompt,
  and relevant topic state. Bound context, output, memory, and pending work.
- Switching back or deleting the model preserves all note text and manual choices;
  recompute inferred memberships with the selected engine. Distinguish cached outputs
  from user-confirmed decisions.
- Maintain stable topic IDs independently of generated labels. Repair the existing
  identity-overlap bug before allowing either engine to rewrite saved topics.
- Model downloads use the network; inference and note contents stay on the device.
- Include model/runtime licenses and relevant notices in the final distribution.

## Experimental arms

| Arm | Membership | Labels |
| --- | --- | --- |
| A: rules | Existing production rules 11 | Existing production labels |
| B: labels | Exactly A's members; asserted by the runner | Qwen labels each generic group |
| C: hybrid | MiniLM retrieval supplies candidate notes; Qwen proposes groups | Qwen supplies labels with memberships |

All arms preserve explicitly discovered named-subject groups from the baseline.
The prototype records C's additional proposals separately; it does not resolve
collisions with those names as an app identity writer would need to do.

Arm B may decline to name an incoherent group with an empty label, in which case
its existing label remains and the fallback is recorded. Invalid responses also
fall back. B cannot fix membership errors by construction.

For C, each generic baseline group is presented with up to three additional MiniLM
neighbors. Each previously ungrouped note is also covered by a nearest-neighbor
batch of at most eight notes. Those extra batches allow discoveries that A omitted.
The baseline batches are capped at 24 notes, with an explicit error for larger
cores rather than silently truncating them. Larger-journal chunking is future work.

C can return up to three overlapping groups per batch or abstain. Each group needs
at least two distinct supplied note IDs. The runner rejects duplicate IDs, unknown
IDs, malformed output, and excessive labels. Truncated responses are failures.
The prompt requests 1–5 words; the validator tolerates six and rejects longer labels.
It removes only exact duplicate proposals, retaining overlaps for evaluation.
There is deliberately no semantic repair that might conceal model errors.

On an invalid C response, its existing-core batch falls back to that baseline
group; an ungrouped-neighborhood failure creates no group. Valid but semantically
wrong outputs remain visible in the results. Structured JSON is not a correctness
guarantee.

## Model and runtime

- Model: [Qwen3-0.6B](https://huggingface.co/Qwen/Qwen3-0.6B), non-thinking mode.
- Artifact: official [Q8_0 GGUF](https://huggingface.co/Qwen/Qwen3-0.6B-GGUF),
  **639,446,688 bytes** (about 610 MiB), Apache-2.0.
- Revision: `23749fefcc72300e3a2ad315e1317431b06b590a`.
- SHA-256: `9465e63a22add5354d9bb4b99e90117043c7124007664907259bd16d043bb031`.
- Runtime: llama.cpp, MIT, commit `391fac16460f15233a7740550d858ac96df3419d`.
- Runtime built for Android ARM64 with NDK 28.2.13676358 and CMake 3.22.1;
  CPU execution, four threads, one slot, 2,048 context tokens, batch/microbatch 128,
  prompt-cache RAM disabled, context shifting disabled.
- Generation: temperature 0, seed 731, thinking disabled, JSON-schema constrained;
  at most 80 output tokens for labels or 300 for grouping.
- Prompt contract version: `hybrid-comparison-v1`.

The [manifest](../scripts/hybrid/model-manifest.json) pins the artifact, integrity,
and experiment configuration. Q8 is the initial official-artifact baseline; smaller
quantizations should be compared separately before selecting download size.

The native server is a research harness reached through an adb localhost forward.
It is not an application architecture that requires a server or cloud service.
The experimental model and binary live under `.local/hybrid` and emulator
`/data/local/tmp/notetaker-hybrid`, outside the app APK and journal database.

## Corpora and review

1. Original 72-note fixture: verify previously useful groupings survive.
2. Expanded 172-note fixture: investigate the known overlap, scope, and identity cases.
3. [40 held-out notes](../fixtures/hybrid/heldout-notes.json): new subjects and different
   wording, authored before model inference. [Editorial references](../fixtures/hybrid/heldout-review.json)
   never enter prompts or retrieval. This is still a small synthetic set authored
   during development, not an independent production-quality benchmark.

The Kotlin exporter computes the real production baseline and MiniLM vectors in
an isolated emulator test. It never opens the journal database. The Python runner
orchestrates requests; generative inference also executes on the emulator. No model
inference uses a remote provider or the host laptop GPU.

Measure separately:

- Desired and undesirable same-topic pairs, with original check limitations retained.
- Label fit for all members, specificity, and invented or vague subjects.
- Missing subjects, redundant groups, and changes to previously good results.
- Format failures, truncation, repeated IDs, abstentions, and fallbacks.
- Request latency and output token counts, with caching and runtime configuration shown.
- Memory pressure and failure recovery; later measure battery and latency on phones.

Pair checks alone cannot determine a winner: labels-only necessarily ties the rules
on those checks, and splitting everything can lower undesirable pairs while losing
useful groups. A novel output outside the reference subjects is not automatically
wrong. Manual review must supplement the counts.

The prototype does not yet evaluate persistent identity changes, app-engine
switching, repeated-seed variability, or hardware battery use. Existing 72→172
snapshots test separate discovery runs, not incremental LLM state persistence.

## Reproduction

Prepare optional artifacts and runtime (downloads approximately 610 MiB of model
weights plus native build tools if needed):

```sh
./scripts/hybrid/prepare-emulator.sh
```

Export baselines and note vectors using the current debug app and an isolated test:

```sh
./scripts/dev gradle assembleDebugAndroidTest
adb -s emulator-5554 install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb -s emulator-5554 shell am instrument -w \
  -e class app.notetaker.audit.HybridComparisonExportTest -e hybridAudit true \
  app.notetaker.test/app.notetaker.IsolatedTestRunner
adb -s emulator-5554 exec-out run-as app.notetaker cat files/hybrid-comparison-inputs.json \
  > .local/hybrid/comparison-inputs.json
adb -s emulator-5554 shell am force-stop app.notetaker
```

In a separate terminal, run `./scripts/hybrid/start-server.sh`. Then:

```sh
python3 scripts/hybrid/compare.py
python3 -m unittest discover -s scripts/hybrid -p 'test_*.py'
```

The runner saves each request, raw response, validation result, timing, prompt/model
identity, and a summary under `.local/hybrid/comparison-v1-bounded`. Calls are cached
by their complete request and model manifest. Reruns resume from recorded calls.
`--max-new-calls 3` runs a bounded smoke check; omission runs the full comparison.
Failures are cached as observed outcomes, not silently retried until successful.

Stop the experimental server by its emulator PID after evaluation and remove the
adb forward. Launch the ordinary app to return to the unchanged journal. Never use
`connectedDebugAndroidTest` for this run; the direct instrumentation path preserves
installed app data.

## Initial runtime issue

An initial 4,096-context run inherited llama.cpp's **8,192 MiB prompt-cache budget**,
far larger than the emulator's approximately 2.4 GiB RAM. Later requests slowed to
55–74 seconds and the emulator exhausted its swap. That run was stopped and its
partial outputs retained under `.local/hybrid/comparison-v1`.

The reported comparison uses a separate output directory and manifest with context
2,048 and cache RAM disabled. The regular app process was also stopped to release
its memory. These changes were made together; the initial slowdown cannot be
attributed quantitatively to only one of them. No phone-performance claim follows
from emulator timings.
