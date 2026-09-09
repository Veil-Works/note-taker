# Note-taker — Product and Architecture Brief

## 1. Purpose of This Document

This document describes the current product idea, intended functionality, architectural direction, and staged implementation plan for the **Note-taker** application.

It is intended to serve as a persistent source of truth for an AI coding agent or developer implementing the application incrementally.

The application should **not** be built all at once.

The intended implementation strategy is:

1. Build a simple but useful local application first.
2. Keep each development stage independently functional.
3. Add structure before adding intelligence.
4. Add AI only after the core product works well without it.
5. Start as a **fat client**.
6. Evolve into a **hybrid local + cloud application** later.
7. Avoid premature backend infrastructure, authentication, synchronization, and unnecessary abstractions.

The project should remain understandable to a developer who has some Java experience but is still learning Kotlin, Android development, Jetpack Compose, Room, coroutines, and modern Android architecture.

---

# 2. Core Product Idea

The application is a **personal note-taking and lightweight knowledge-management tool**.

The primary interaction should remain extremely simple:

- the user quickly writes a note;
- the note is stored locally;
- the user can later search and organize notes;
- over time the application helps identify structure, relationships, recurring concepts, and useful summaries;
- later, AI can assist with semantic understanding and organization.

The long-term evolution is approximately:

```text
notes
→ tags
→ explicit relations
→ entities and entity types
→ structured queries
→ semantic search
→ AI-assisted analysis
→ knowledge graph
→ multimodal input
→ synchronization
→ hybrid/cloud functionality
```

The application must remain useful even if all AI functionality is disabled or unavailable.

---

# 3. Product Philosophy

## 3.1 Local-first

The first versions must work entirely on-device.

The user should be able to:

- create notes;
- read notes;
- edit notes;
- delete notes;
- search notes;
- organize notes;

without:

- an account;
- internet access;
- a cloud backend;
- external APIs;
- remote AI services.

Cloud functionality is a later extension, not the foundation of the product.

---

## 3.2 Progressive Intelligence

The application should not begin as an "AI app".

Early versions should rely on simple, deterministic, understandable mechanisms wherever practical:

- text search;
- tags;
- aliases;
- explicit links;
- keyword matching;
- rule-based suggestions;
- metadata filters.

Later versions may add:

- embeddings;
- semantic search;
- automatic clustering;
- automatic linking;
- automatic summarization;
- natural-language queries;
- local LLM inference;
- optional remote LLM inference.

---

## 3.3 User Control

AI should assist the user rather than silently mutate the user's knowledge base.

Whenever possible, automatically inferred information should initially be presented as suggestions, for example:

- suggested tag;
- suggested related note;
- suggested entity;
- suggested category;
- suggested link.

The user should be able to accept or reject these suggestions.

---

## 3.4 Incremental Implementation

Each development stage should result in a usable application.

Do not implement future infrastructure simply because it might become useful later.

However, local data models should avoid obvious choices that would make future synchronization unnecessarily difficult.

---

# 4. Initial Platform Direction

The first version is intended to be an **Android fat client**.

Expected technology direction:

- Kotlin
- Jetpack Compose for UI
- SQLite as the underlying database
- Room as the persistence abstraction
- Kotlin coroutines for asynchronous work
- ViewModel-based state management
- repository abstraction between application logic and persistence

The user has some Java experience but is still learning:

- Kotlin;
- Android application structure;
- Jetpack Compose;
- Room;
- SQLite;
- coroutines;
- modern Android architectural patterns.

Therefore, implementation should optimize for **clarity and maintainability**, not maximum abstraction.

Guidelines:

- prefer explicit code over clever code;
- avoid unnecessary framework dependencies;
- avoid premature dependency injection frameworks;
- avoid unnecessary generics or metaprogramming;
- avoid architecture astronautics;
- explain non-obvious Android/Kotlin concepts in comments;
- use descriptive names;
- keep files and classes focused;
- keep the code understandable to someone learning the stack.

---

# 5. Architectural Direction

The application starts as a **fat client**.

All essential functionality initially lives on the Android device.

A preferred conceptual structure is:

```text
Jetpack Compose UI
        ↓
ViewModel / UI state
        ↓
Application use cases / services
        ↓
Repository interfaces
        ↓
Local repository implementations
        ↓
Room
        ↓
SQLite
```

The UI should not directly depend on Room or SQLite.

The application/domain logic should not be tightly coupled to the persistence implementation.

This matters because later the application may evolve into:

```text
UI
↓
ViewModel
↓
Use cases
↓
Repositories
├── Local database
└── Sync / cloud data source
```

The goal is not to implement cloud abstractions now, but to avoid making them impossible later.

---

# 6. Core Data Model

The initial data model should remain small.

## 6.1 Note

A note is the primary object in the application.

Possible fields:

```text
id
content
createdAt
updatedAt
deletedAt (optional)
version (optional but potentially useful later)
```

Recommended properties:

- stable UUID identifier;
- creation timestamp;
- modification timestamp;
- textual content.

A note should initially be primarily text-based.

Future versions may support:

- title;
- attachments;
- images;
- audio;
- handwriting;
- source metadata.

These should not block the first implementation.

---

## 6.2 Tag

Tags provide explicit user-controlled organization.

Possible fields:

```text
id
name
createdAt
```

A note can have multiple tags.

A tag can belong to multiple notes.

This implies a many-to-many relationship.

Example:

```text
Note
"Test multi-objective reward shaping for the traffic experiment."

Tags
- traffic
- reinforcement-learning
- experiment
```

---

## 6.3 Note-Tag Relation

A simple relation table associates notes with tags.

Conceptually:

```text
NoteTag
noteId
tagId
```

---

## 6.4 Note-to-Note Relation

Later versions should allow explicit links between notes.

Possible relation fields:

```text
id
sourceNoteId
targetNoteId
createdAt
```

V3 links are untyped. Do not add a relationship-type field or selector.
The priority is connecting notes around common themes, such as a project,
training, or running. Tags provide thematic grouping; explicit links connect
individual notes within that context. Relationship types can be reconsidered
in a later version if a concrete need appears.

---

# 7. Stage-by-Stage Development Roadmap

## Stage 1 — Minimal Local Notes Application

### Goal

Produce the smallest genuinely useful application.

### Required functionality

- create a note;
- display all notes;
- open a note;
- edit a note;
- delete a note;
- persist notes locally;
- basic text search.

### Required architecture

- Jetpack Compose UI;
- ViewModel;
- repository;
- Room database.

### Explicitly excluded

Do not implement:

- tags;
- AI;
- embeddings;
- accounts;
- cloud storage;
- sync;
- authentication;
- knowledge graph;
- attachments;
- collaboration.

### Success criterion

The user can use the app every day as a simple local notebook.

---

# 8. Stage 2 — Tags and Manual Organization

### Goal

Add explicit knowledge organization.

### Required functionality

- create tags;
- attach tags to notes;
- remove tags from notes;
- display tags;
- filter notes by tag;
- show all notes associated with a tag.

### Example

```text
Note:
"Try a slower learning rate for the MARL traffic signal experiment."

Tags:
- MARL
- traffic
- experiment
```

### Success criterion

The user can manually build a useful thematic structure around notes.

---

# 9. Stage 3 — Explicit Links Between Notes

### Goal

Allow the user to connect notes around common themes, such as a project,
training, or running, using simple, untyped links.

### Required functionality

- link one note to another;
- display linked notes;
- navigate between linked notes;
- remove links.

Use existing tags to group notes by theme and explicit links to connect
individual notes. V3 does not include relationship types or a type selector.
Typed relationships are deferred until there is a concrete need for them.

### Success criterion

The note collection begins behaving like a lightweight knowledge network.

---

# 10. Stage 4 — Search Improvements

### Goal

Make retrieval significantly better without AI.

V4 scope:

- combine existing content search and a tag filter with a date filter;
- Today, Last 7 days, Last month, and inclusive custom calendar ranges;
- choose creation or edit date as the date-filter basis;
- sort by recently edited, newest created, oldest created, or alphabetical note preview;
- quick access to the last seven days and the five most-used tags with note counts;
- retain filter/sort choices across screen recreation;
- clear filters and restore default sorting after successful note capture.

Date ranges use the device's local calendar. Last month means the previous
calendar month. Tag popularity counts notes across the entire notebook.
Dedicated note titles remain a future feature; search uses existing note content.

Examples:

```text
traffic + experiment
```

or:

```text
notes tagged "running" from the last month
```

This stage should remain deterministic.

---

# 11. Stage 5 — Lightweight Automatic Tag Suggestions

### Goal

Suggest existing tags from note text using predictable, local rules. V5 uses
tag names and user-editable keywords/aliases; it does not require an LLM.

### Required functionality

- Add, edit, and remove keywords/aliases associated with an existing tag.
- Match note text against both tag names and their configured keywords/aliases.
- Evaluate suggestions after a note is saved or its edits are saved, without
  delaying quick capture.
- Display matching tags as suggestion chips in the note editor.
- Exclude tags already attached to the note and show each suggested tag only once.
- Let the user accept or dismiss each suggestion, or adjust its underlying
  keywords/aliases. Existing manual tag selection remains available.
- Accepting a suggestion attaches the existing tag. Never attach a suggested
  tag automatically or create a new tag from a match.

### Matching rules

- Ignore capitalization and match whole words or phrases at word boundaries.
  For example, the alias `run` must not match the substring in `brunch`.
- Support multi-word aliases such as `bench press` and `Alpha rollout`.
- Trim alias input, reject blank aliases, and deduplicate aliases within a tag
  without regard to capitalization.
- Allow an alias to belong to multiple tags; a phrase may suggest more than
  one relevant tag.
- Keep results deterministic. Fuzzy matching, semantic inference, and learned
  recommendations are outside this version's scope.

### Example configuration

| Existing tag | User-configured keywords/aliases |
| --- | --- |
| `running` | `running`, `jog`, `5k`, `marathon` |
| `strength-training` | `bench press`, `deadlift`, `squat` |
| `project-alpha` | `Project Alpha`, `Alpha rollout` |

These are examples, not a built-in vocabulary. Suggestions depend on the user's
existing tags and configured aliases.

With `bench press` configured for both `bench-press` and `strength-training`:

```text
User writes:
"Bench press 80 kg, 5x5 today."

Application suggests:
bench-press
strength-training
```

### Dismissal and persistence

- A dismissed suggestion stays dismissed while the note text is unchanged,
  including after reopening the editor or restarting the app.
- Editing tags alone does not reset dismissals. When saved note text changes,
  reevaluate suggestions; previously dismissed tags may be suggested again.
- Store aliases and dismissal state locally and preserve existing notes, tags,
  and connections through any required database migration.

### Success criteria

- A configured word or phrase suggests the expected existing tags regardless
  of capitalization, without matching inside unrelated words.
- Attached tags are not suggested, and multiple matching aliases produce only
  one chip per tag.
- Accepting attaches the tag; dismissing leaves note tags unchanged and stays
  effective for unchanged text across app restarts.
- Alias edits affect subsequent evaluations, while dismissals for unchanged
  note text remain respected.
- Capture and manual organization continue to work entirely offline. The app
  does not infer themes from vocabulary that has not been configured.

---

# 12. Stage 6 — Entities and Entity Types

### Goal

Move beyond flat tags toward structured concepts.

Example entities:

```text
Bench press
Running
VRP
Horizon project
NSGA-II
```

Possible entity types:

```text
Exercise
Project
Research topic
Algorithm
Person
Place
Task
Idea
```

Possible entity fields:

```text
id
canonicalName
type
createdAt
```

Possible aliases:

```text
bench
bench press
barbell bench press
```

could refer to the same entity.

### Why this matters

Tags answer:

```text
"What labels belong to this note?"
```

Entities eventually answer:

```text
"What concepts does this note refer to?"
```

This is an important conceptual transition toward a knowledge system.

---

# 13. Stage 7 — Structured Queries

### Goal

Allow the user to retrieve information based on meaning encoded in application structure.

Examples:

```text
Show my exercise notes.
Show all VRP ideas.
Show notes related to the Horizon project.
Show running notes from the last month.
```

The first implementation does not need natural-language interpretation.

The UI may expose structured filters.

Natural-language queries can be added later.

---

# 14. Stage 8 — Semantic Search

### Goal

Retrieve notes by meaning rather than exact keywords.

Possible functionality:

- create embeddings for note content;
- compare note embeddings;
- search by semantic similarity;
- suggest similar notes.

Example:

A search for:

```text
ways to improve urban signal control
```

may retrieve a note containing:

```text
MARL reward shaping for intersections
```

even if the exact query words are absent.

### Architectural rule

Semantic search should be implemented as a separate capability.

The rest of the application must continue working if semantic search is unavailable.

---

# 15. Stage 9 — AI-Assisted Analysis

### Goal

Use AI for high-value interpretation rather than basic storage.

Potential features:

- summarize selected notes;
- summarize all notes on a topic;
- extract candidate tags;
- extract entities;
- propose relations;
- cluster similar notes;
- identify contradictions;
- identify recurring ideas;
- generate topic overviews.

Example:

```text
"Summarize my current ideas about VRP optimization."
```

The system gathers relevant notes and produces a summary.

---

# 16. Stage 10 — Natural-Language Querying

### Goal

Allow the user to ask questions conversationally.

Examples:

```text
What ideas did I have about traffic signal control last month?

What exercises have I written about recently?

Summarize everything related to the Horizon project.

What unresolved ideas do I have about reinforcement learning?
```

This layer should sit above the existing structured data and search systems.

The LLM should not become the only method of retrieving data.

---

# 17. Stage 11 — Knowledge Graph

### Goal

Represent relationships between concepts, notes, and entities explicitly.

Possible graph structure:

```text
Note → Tag
Note → Entity
Note → Note
Entity → Entity
Entity → Type
```

Example:

```text
NSGA-II
    ↓ is_a
Evolutionary algorithm

VRP
    ↓ optimized_by
NSGA-II

Research note #42
    ↓ mentions
VRP
```

The graph should emerge from useful product needs, not exist merely because knowledge graphs are technically interesting.

A graph database is not required initially.

The graph can be represented using ordinary relational tables until there is a concrete reason to introduce specialized graph infrastructure.

---

# 18. Stage 12 — Multimodal Notes

Potential future input types:

- voice notes;
- photographs;
- screenshots;
- scanned pages;
- handwritten notes;
- files.

Possible processing:

```text
audio → transcription → note
image → OCR / vision extraction → note
document → extracted text → note
```

Multimodality should be treated as an input layer feeding the same underlying knowledge system.

---

# 19. Stage 13 — Sync-Ready Local Model

Before building a cloud backend, the local application should become synchronization-friendly.

Potential requirements:

- stable UUIDs;
- reliable updated timestamps;
- soft deletion or deletion records;
- version metadata;
- local change log;
- sync state;
- deterministic conflict strategy.

Example conceptual state:

```text
LOCAL_ONLY
SYNCED
MODIFIED
DELETED
CONFLICT
```

Do not implement this prematurely.

Introduce it when synchronization becomes an actual development goal.

---

# 20. Stage 14 — Hybrid Architecture

Only after the local application is mature should the project evolve from fat client to hybrid.

Possible cloud functionality:

- cross-device synchronization;
- cloud backup;
- optional remote AI inference;
- account system;
- web access;
- shared knowledge spaces;
- collaboration.

The architecture may evolve toward:

```text
Android Client
├── Local Room database
├── Local search
├── Local application logic
├── Optional local AI
└── Sync client
        ↓
Backend API
├── Authentication
├── Synchronization
├── Cloud persistence
└── Optional AI services
```

The local database remains important even after the backend exists.

The application should ideally continue to function offline.

---

# 21. AI Architecture Principles

AI must be modular.

Avoid spreading LLM calls throughout UI or domain classes.

A future AI layer might conceptually expose capabilities such as:

```text
suggestTags(note)
extractEntities(note)
findRelatedNotes(note)
summarize(notes)
answerQuestion(query)
```

These should be treated as application services.

Possible implementations may later include:

- deterministic local logic;
- local embedding model;
- local LLM;
- remote LLM API.

The rest of the application should not care which implementation is active.

---

# 22. Local vs Remote AI

The initial product should not require a decision here.

Possible future options:

## Local AI

Advantages:

- privacy;
- offline operation;
- no per-request API cost;
- user data stays on device.

Disadvantages:

- device performance constraints;
- model size;
- battery usage;
- potentially weaker models.

## Remote AI

Advantages:

- stronger models;
- easier model updates;
- lower device requirements.

Disadvantages:

- privacy concerns;
- API costs;
- network dependency;
- backend/API-key management.

## Hybrid AI

Potential long-term approach:

- local search and embeddings;
- local lightweight classification;
- remote LLM only for complex reasoning or summaries.

---

# 23. UX Principles

The application should optimize for **low-friction capture**.

The most important interaction is:

```text
open app
→ write thought
→ save
```

This should be extremely fast.

AI or organization features should not make note capture slower.

Possible main interface:

```text
Notes
Search
Tags / Topics
```

Later:

```text
Ask
Graph
```

The exact navigation structure is not finalized.

---

# 24. Note Capture

A future quick-capture experience may include:

- persistent "new note" button;
- home-screen widget;
- Android share target;
- notification quick action;
- voice capture.

These are not required for the MVP but align strongly with the product philosophy.

---

# 25. Privacy Direction

Because the application may contain:

- research notes;
- personal thoughts;
- work information;
- unpublished ideas;

privacy should be treated as an important product property.

The initial local-only architecture naturally provides strong privacy.

When cloud or remote AI features are eventually introduced:

- sending data remotely should be explicit;
- the user should understand what leaves the device;
- local-only operation should remain possible where practical;
- remote AI should preferably be optional.

---

# 26. Export and Data Ownership

The user should not be trapped inside the application.

A future export mechanism should support at least one simple open format.

Potential formats:

- Markdown;
- JSON;
- Markdown + metadata.

Example:

```text
notes/
    2026-09-08-reward-shaping.md
    2026-09-08-bench-press.md
metadata.json
```

Import/export is not mandatory for the first MVP but should eventually become a core ownership feature.

---

# 27. Deliberately Deferred Decisions

The following questions should remain open until the relevant stage:

- exact cloud provider;
- backend language/framework;
- authentication provider;
- synchronization protocol;
- conflict-resolution strategy;
- local embedding model;
- local LLM;
- remote LLM provider;
- vector database;
- graph database;
- monetization model;
- iOS support;
- desktop client;
- web client;
- Kotlin Multiplatform;
- subscription infrastructure.

Do not select technologies for these areas merely to make the architecture look complete.

---

# 28. Things the Coding Agent Should Avoid

The implementation agent should actively avoid:

- building the full roadmap at once;
- creating a backend for the MVP;
- adding authentication before it is needed;
- introducing microservices;
- introducing Docker for the local Android MVP;
- introducing a graph database early;
- introducing a vector database before semantic search exists;
- adding AI merely for marketing value;
- overengineering repository abstractions;
- premature generic architecture;
- unnecessary dependency injection frameworks;
- hiding simple logic behind excessive layers;
- replacing understandable code with generated complexity.

---

# 29. Code Quality Expectations

The implementation should be suitable both as a working product and as a codebase the user can learn from.

Therefore:

- prefer small classes/functions;
- use meaningful names;
- explain Android-specific behavior when non-obvious;
- keep UI state explicit;
- keep business logic outside composables where practical;
- do not put database calls directly inside composables;
- avoid giant ViewModels;
- avoid giant repository classes;
- maintain clear boundaries between persistence, application logic, and presentation;
- write tests for non-trivial application logic as it appears.

---

# 30. Development Method

The coding agent should work milestone by milestone.

For each milestone:

1. define the exact feature;
2. identify affected architecture;
3. implement the minimum required code;
4. keep the project compiling;
5. run tests;
6. manually verify the feature;
7. refactor only where a concrete need has appeared;
8. document important architectural decisions.

Large speculative refactors should be avoided.

---

# 31. Initial Milestone Sequence

The recommended first implementation sequence is:

## Milestone 0 — Project Skeleton

Create a minimal Android application using:

- Kotlin;
- Jetpack Compose;
- a single app module.

Verify:

- project builds;
- emulator/device runs it;
- a basic Compose screen renders.

No database yet.

---

## Milestone 1 — In-Memory Notes

Implement:

- Note model;
- list of notes;
- create note;
- edit note;
- delete note.

Initially store notes only in memory.

Purpose:

Learn and validate:

- Compose;
- state;
- ViewModel;
- navigation;
- data flow.

Do not introduce Room until the basic UI/data flow is understood.

---

## Milestone 2 — Room Persistence

Replace in-memory storage with Room.

Implement:

- Room entity;
- DAO;
- database;
- repository implementation;
- persistence across app restarts.

The UI behavior should remain essentially unchanged.

---

## Milestone 3 — Search

Implement basic text search.

Keep it deterministic and local.

---

## Milestone 4 — Tags

Add:

- Tag entity;
- note-tag relationship;
- tag creation;
- tag assignment;
- tag filtering.

---

## Milestone 5 — Note Links

Add explicit note-to-note relations.

---

## Milestone 6 — Better Organization

Add:

- sorting;
- filtering;
- recent notes;
- basic topic views.

---

## Milestone 7 — Automatic Suggestions

Implement the v5 tag-name and editable-alias matching specified in Stage 5,
including suggestion chips, explicit acceptance, and persistent dismissals.
Automatic relationship suggestions remain deferred.

---

## Milestone 8 — Entities

Introduce structured entities and entity types.

---

## Milestone 9 — Semantic Search

Add embeddings and semantic retrieval.

---

## Milestone 10 — AI Layer

Add modular AI capabilities.

Only at this point should LLM functionality become a meaningful part of the product.

---

# 32. Current Scope Boundary

At the current stage, the practical implementation target is:

```text
Android
+
Kotlin
+
Jetpack Compose
+
ViewModel
+
Room
+
local-first architecture
```

The immediate product target is:

```text
simple local note-taking app
```

The broader vision should guide architecture but should **not** dictate premature implementation.

---

# 33. Final Product Vision

The long-term goal is not merely another notes application.

The intended product is a **personal knowledge companion** that starts from frictionless note capture and gradually helps the user turn scattered observations into a structured body of knowledge.

At maturity, the application should be able to help answer questions such as:

```text
What ideas have I had about this topic?

How has my thinking changed over time?

Which notes seem related even though I never linked them?

What unresolved questions keep recurring?

Summarize everything I know about this project.

What have I written about this person, concept, exercise, or research topic?

Which ideas contradict each other?

What should I revisit?
```

The key principle is:

> Capture first. Structure second. Intelligence third.

The application should earn complexity gradually.
