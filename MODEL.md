# Offline organization model

V7 bundles the English sentence-transformers/all-MiniLM-L6-v2 model, quantized to
int8 for ARM64, with ONNX Runtime Android 1.23.2. No network inference or runtime
model downloads occur. The model and vocabulary are committed as assets.

Source: https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2

Upstream revision: `1110a243fdf4706b3f48f1d95db1a4f5529b4d41`

Model: https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2/resolve/1110a243fdf4706b3f48f1d95db1a4f5529b4d41/onnx/model_qint8_arm64.onnx

Vocabulary: https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2/resolve/1110a243fdf4706b3f48f1d95db1a4f5529b4d41/vocab.txt

License: Apache 2.0 (declared in the upstream model card). License text ships in
`app/src/main/assets/MINILM-LICENSE.txt`. The vocabulary and quantized ONNX export
are unmodified; preprocessing and chunk aggregation are implemented in this app.

- `minilm.onnx`: SHA-256 `4278337fd0ff3c68bfb6291042cad8ab363e1d9fbc43dcb499fe91c871902474`
- `minilm-vocab.txt`: SHA-256 `07eced375cec144d27c900241f3e339478dec958f92fddbc551f295c992038a3`

## Inference

BERT uncased basic tokenization and greedy WordPiece use the bundled vocabulary.
Inputs are split into at most 254 word pieces plus CLS/SEP. Attention masks are
all ones because each chunk runs independently without padding. Token embeddings
are mean-pooled and normalized; chunk embeddings are weighted by their word-piece
counts and normalized again. This covers the whole note, although averaging can
lose distinctions in notes with several unrelated subjects. Outputs contain 384
floats. Only changed note text is re-embedded; caches include the model version.

## Initial phone evaluation

An eight-note synthetic benchmark covered weight training, software debugging,
travel, and running. Loading the model plus embedding all eight took 286 ms on
the connected phone. Related pairs scored 0.439–0.685; unrelated pairs in this
small fixture scored at most 0.230. These are similarity scores, not calibrated
probabilities. This is a smoke evaluation, not a general accuracy guarantee.

V7 uses 0.38 for related-note candidates and 0.42 for provisional topic grouping,
with every member required to match the group. Learned concept attachment needs
two manually labeled examples at 0.55 or one at 0.82, and must exceed the closest
negative example by 0.08. Exact unique phrase matches use a separate rule.
Thresholds should be revisited with a larger representative evaluation set.
English shorthand, ambiguity, and mixed subjects remain limitations; other
languages have not been validated. User-added labels are the only positive
learning examples. Automatically inferred labels never reinforce themselves.

## Topic labels

Labels use local rules: shared category evidence (such as Dogs and Running),
recurring project subjects, then phrases appearing in multiple notes. Custom
names are preserved. The experimental generative model and native runtime have
been removed; the bundled MiniLM embedding model still supplies connections.
Upgrading removes the experimental model files and cached generated labels,
then regenerates automatic topic names without changing note contents.
