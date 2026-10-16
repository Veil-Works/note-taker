# Laptop topic review

Start from the repository root:

    .local/embeddings/venv/bin/python scripts/embeddings/review.py

Open http://127.0.0.1:8765/ in a browser. The server uses Python's standard library; the screen has no third-party frontend dependencies or external assets.

The review contains the fixed Granite + complete-link groups: 32 development topics and 11 topics from the previously observed transfer notebook. It compares current term ranking and hybrid source/noun phrases with semantic ranking. Method identities are hidden during review. Each topic independently randomizes the two proposal positions, and the order is persisted across restarts.

For each topic:

1. Read all member notes. Flag any note that does not belong in the group.
2. Rate each available leading label for clarity and how well it describes the whole group. Other candidates are expandable alternatives; the scores apply to the leading label.
3. Choose the better proposal, equally good, or neither. Rate whether the member set makes a useful topic.
4. Optionally suggest a better label or leave a comment. Click **Mark reviewed & next** to include the topic in the completed-review summary.

Ratings, text and flags autosave locally. **Next topic** lets you skip an incomplete topic without losing the saved draft. The sidebar filters by notebook and reviewed/pending status, and searches the proposed labels and note text. A missing proposal receives no clarity/fit rating. **Export review** downloads blinded JSON with the notes and saved reviews.

When all 43 topics are complete, **Reveal results** shows method preference counts and mean clarity/fit scores. An explicit local summary is also available:

    .local/embeddings/venv/bin/python scripts/embeddings/review.py --summary

Only completed topics enter the summary. Partial drafts are preserved but do not count as completed evidence. The blind mapping is private to the local server and is omitted from the screen's data and export. Previously seen reports can still make the review less than fully blind; this tool hides method attribution, not the underlying wording.

Saved files live in .local/embeddings/reviews/<study-id>/reviews.json, with the stable proposal mapping in blind-key.json. These files are ignored by Git. The study ID incorporates the model revision, clustering settings, corpus, memberships, note text and label alternatives. Changing these creates a separate review instead of carrying annotations to changed groups. Two-tab revision conflicts are rejected rather than silently overwriting ratings. Save errors remain visible in the screen.

The service binds only to localhost. It serves only its fixed UI/API routes and keeps note text and review data local. Browser testing writes to a separate .local/embeddings/ui-qa/reviews directory; its artificial ratings must not be used as human evaluation.

These ratings evaluate the current observed notebooks. They do not establish untouched generalization or change the app's grouping rules. Incorrect memberships and suggested names are evaluation annotations, not case-specific algorithm exceptions.

The completed 43-group review is analyzed in [LAPTOP_TOPIC_REVIEW_FINDINGS.md](LAPTOP_TOPIC_REVIEW_FINDINGS.md). [LAPTOP_TOPIC_REVIEW_RESULTS.json](LAPTOP_TOPIC_REVIEW_RESULTS.json) preserves the normalized annotations and source hashes. The analysis distinguishes preference for an empty proposal from a successful label and compares rating means on the same groups.
