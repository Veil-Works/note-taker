# Unified topics

## User behavior

Topics replace both manual tags and automatic groups. There is one library,
picker, detail page, and filtering system. Aliases support matching internally;
manual alias/keyword editing is no longer exposed. Existing aliases and names
retained during topic merges continue to help search and suggestions. A note may
belong to several topics. Topic types are retired: there is no type picker,
management screen, or type suffix on topic names. Legacy type data remains in the
database for compatibility and does not affect display or block merges.

## Quick assignment when capturing a note

Start a new note with whitespace-separated topic prefixes:

```text
#Atlas #orion Review the shared release plan.
#"Running shoes" #Shopping Try a wider toe box.
```

Names match case-insensitively: `#ATLAS` reuses Atlas, keeping its existing spelling
and identity. Repeated names attach only once. Missing topics are created, and
all prefix assignments are manual, including when the topic was discovered
automatically. Prefixes are removed from the saved note text; a body is required.
Topic creation, note saving, and assignments commit in one database transaction.

Unquoted names support letters, numbers, underscores, and hyphens (40 characters
maximum); quote names containing spaces. Only leading prefixes are interpreted.
Hashtags later in the body, Markdown headings such as `# Heading`, and existing-note
edits remain ordinary text. A draft containing only topic names remains open with
an explanation instead of saving an empty note.

## Note detail panels

The note editor shows the text, a compact topic summary, and two entry points:

- **Topics:** automatic explanations and Keep/Remove actions, manual selection,
  creation, suggestions, and topic management.
- **Connections:** one deduplicated list combining semantic matches, shared-topic
  connections, and manual links. Each note shows a short reason. Manual links come
  first, then semantic similarity and shared-topic overlap. Five entries are shown
  initially, with Show all for the remainder. Keep linked saves a manual link;
  Remove link removes that saved link, so an automatic match can still appear.

Each panel replaces the editor body instead of expanding every tool below the note.
Back to note (or system Back) returns to the text; Done saves and closes from any
panel. Topic chips also open the Topics panel.

The list header has no Topics or Browse topics button. The dedicated Topics
destination handles management; Filters contains topic selection alongside dates
and sorting. Topic selections apply with Apply and are discarded with Cancel.
Selected topics and frequently used topic shortcuts remain available in the list.

## Memberships

Memberships retain their origin:

- Manual: stays until explicitly removed, even when the note's content changes.
- Automatic: recomputed from current saved text; Keep changes it to manual.
- Excluded: Remove records a per-note exclusion that survives text edits and restarts.
  Explicitly adding the note again clears the exclusion and suggestion dismissal.

Renaming, editing aliases, confirming or removing a membership,
and merging retain the topic identity. Renaming does not freeze automatic membership.
Untouched discovered topics are provisional and disappear when no memberships remain.
A topic created by the user remains even when empty.

## Merging

Open the destination topic, choose Merge topic, select the source, then confirm.
The destination ID and name survive. The source name becomes an alias; existing
aliases are deduplicated. Legacy type differences do not prevent merging.

Manual memberships stay manual and automatic memberships stay automatic. If one
side explicitly includes a note and the other excludes it, explicit inclusion wins.
Otherwise exclusions and dismissals win over automatic membership. Discovery keys
and previously merged IDs redirect to the destination. Active source filters follow
that redirect, including after state restoration. Filters for deleted or expired
topics are cleared. Note content, timestamps, and
manual note links are unchanged.

A merge does not turn automatic assignments into training examples. User-confirmed
notes alone supply positive examples. Future inferred memberships may still change.

## Discovery and identity

Each run clusters current note text and independently evaluates membership in each
candidate group, allowing overlaps. Existing user-defined topic matches can assign
several topics to a note as well.

Discovery recognizes recurring names throughout short notes, including several
projects in one sentence. Generic groups grow from coherent cores using up to five
fixed representative notes, so one newly admitted note cannot pull in a chain of
weaker matches. Labels describe recurring subjects or semantically matched purposes.
Automatic explanations reference a supporting note or the explicit subject.

Near-duplicate candidates are combined. Untouched provisional topics can be renamed
or consolidated when their memberships substantially survive in the same candidate;
old identities redirect to the survivor. Customized topics are never automatically
deleted or renamed by this consolidation. Renaming a topic does not lock its members.

The organizer first checks a stored fingerprint of the group's contents. Otherwise
it looks for substantial membership overlap with existing topics. Overlap from
changed notes must still agree with their previously cached meaning. Current
user-defined matches also supply identity evidence. Matching names alone do not
cause a merge; name collisions get a numeric suffix.

Generated names of provisional topics are excluded from exact-name attachment.
Automatic memberships can identify previous groups but never become positive
training examples. Per-note exclusions always block inferred assignment; closely
matching negative examples also block discovery-based assignment.

Fingerprints preserve an explicit merge when its constituent groups are rediscovered.
When a group changes, overlap helps retain its identity. Identity matching remains
heuristic for heavily rewritten notebooks or groups with no surviving overlap.
Deleted topic fingerprints suppress immediate recreation of the same group. Deleting
a topic is not a global prohibition on discovering any related subject in future notes.

## Storage and migration

Room schema 7 extends the existing `tags` identity with a `provisional` flag. The
internal `Tag` class remains for compatibility with existing editor/filter code;
all product-facing wording uses Topics. Memberships use `note_tags`,
`automatic_tags`, and `organization_rejections`. The graph Topic value is a read
projection of these same records, not a second organization system.

The 6→7 migration copies every old group into this identity store and removes
`topics` and `topic_notes`. It preserves existing tag IDs, names, types, aliases,
dismissals, rejections, manual links, and timestamps. Conflicting group IDs/names
are disambiguated, never silently merged. Customized groups retain their identities.
Legacy explicitly merged groups become manual memberships because their old UI
promised to keep the combined membership; ordinary and renamed groups remain
automatic memberships. All earlier migrations chain through this migration.

Discovery writes and queue completion happen in one transaction. The initial
generation guard rejects stale results. Topic creation/pruning triggers enqueue
changes within the transaction, which are consumed by completing its final
generation; this prevents a self-triggering organization loop.
