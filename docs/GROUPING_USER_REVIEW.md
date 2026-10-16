# User grouping review — 2026-09-29

User feedback from the emulator after rules 12 and the 172-note fixture were loaded.
These are observed quality problems, not new benchmark results. Note numbers below
refer to `sample-notes.json` followed by `expanded-notes.json`; matches are based on
fixture contents and the prior expanded audit, not a fresh database export.

| Reported problem | Interpretation / evaluation criterion | Relevant fixture examples |
| --- | --- | --- |
| Balcony rosemary pot note appears under Basil | Reject rosemary-only notes from Basil. General plant-care similarity is insufficient for species-specific membership. A broader plant-care topic could legitimately contain both species. | #120 concerns drainage of the rosemary pot; Basil previously contained #65, #120, #121. |
| Cancel is doubtful | A repeated action verb does not establish a useful shared subject. The meeting cancellation and cloud-trial cancellation should not create this automatic topic solely from the common action. | #93 and #136. |
| Chapter makes no sense | The label is an incidental textual unit rather than a useful description of the common subject. Review membership and consider Reading notes as a candidate name; that replacement is proposed, not user-approved. | #143, #145, #168 in the previous audit. |
| Path connects running and cycling | Path is not an informative topic. Review whether a purposeful cross-sport route-planning topic is actually useful; do not infer approval for it from shared route vocabulary. The feedback does not imply banning all cross-sport overlaps. | #106 (change recovery-run route) and #129 (safer cycling route). |
| Running gear includes purely running-related notes | Require actual equipment content for Running gear, including original core notes. Training, pace, distance, and recovery alone do not support this qualifier. | #35 is a plain running log; #39/#40 concern shoes, #46 concerns socks/bottle. These illustrate the scope distinction, not a complete reviewed membership set. |
| Chat notes are not about chatting | The topic name mistakes a mentioned distraction for the main subject. These notes concern protecting focused coding time. Focused work is a possible replacement label, not a user-approved final name. | #28: more coding before opening chat; #94: protect a morning coding block by closing chat. |
| Two walking notes are unjustifiably assigned to Meetings | Mentions of meetings as context or an outcome do not make meetings the note's main subject. Reject these memberships under the user's intended scope. | #72: try a walk after lunch instead of coffee to improve afternoon attention; #161: walking between calls improves attention at the next meeting. |
| Idea for meetings belongs to Meetings, not Meeting planning | Preserve the positive Meetings membership while distinguishing it from planning a meeting. This is a scope clarification, not a claim that both memberships were observed. | #57: capture meeting decisions as short notes and connect recurring project names. |
| Meeting planning may not deserve a separate topic | The user questions the subdivision and suggests a single Meetings topic. Treat this as a granularity preference to evaluate, not a finalized instruction to merge. A meeting that includes planning is not necessarily a note about planning meetings. | Previous Meeting planning members: #26, #91, #92, #171. Review alongside Meetings, especially #57. |
| Phrases mixes piano, French, and note ideas | Reject this mixed group: a shared word or nearby vocabulary does not establish a coherent subject across musical phrasing, language practice, and note capture. Review separate subjects rather than merely renaming the combined group. | Previous members: #62, #63 (note ideas), #147 (piano), #151/#152/#153 (French/language study). |
| A running-socks note is assigned to Running | The user flags equipment-related content in the activity topic. Evaluate Running as activity/training rather than automatically including every running-adjacent subject. Do not assume a parent-topic membership is desirable just because Running gear is valid. | Possible matches: #46 (buy running socks and a bottle) or #109 (socks stopped heel rubbing). The exact note was not identified; these are candidates, not two confirmed reports. |
| Other Running members may also be inappropriate | Re-audit the full member list for incidental running context and neighboring subjects. The user suspects additional errors but has not identified individual notes. | No additional confirmed note IDs yet; do not treat every current member as wrong. |
| A sandwich-for-a-long-run note appears under Running gear | Food preparation is not equipment. Reject this membership even though the note mentions a long run and uses a preparation/packing verb. Food preparation is a possible subject; membership in Running remains a separate scope decision. | #108: “Pack a sandwich before Sunday's long run. Having lunch ready would avoid another afternoon of random snacks.” |

## Scope preferences captured from this review

- The current user expectation is narrower than an all-inclusive activity umbrella:
  equipment and food do not automatically belong to Running just because they are
  related to running. The socks example is a specific objection; additional Running
  memberships still need review.
- Incidental context must not override the main subject: walking notes mentioning
  meetings are not meeting notes, and focus notes mentioning chat are not chat notes.
- Prefer useful, understandable subjects over repeated fragments such as Chapter,
  Path, Phrases, or Cancel.
- A single Meetings topic may be preferable to overlapping Meetings and Meeting
  planning topics. This remains a proposal to evaluate with the user.
- Keep positive cases as well as exclusions: #57 should remain in Meetings. A fix
  that eliminates it together with the two walking false positives is a regression.

## Implications for the next comparisons

- Measure topic-label scope independently of note similarity and group coherence.
- Include confusable nearby subjects: rosemary versus basil, and running versus
  equipment for running.
- Include food preparation versus running equipment, focused work versus chat,
  walking versus meetings, and musical versus linguistic uses of “phrase.”
- Penalize unhelpful labels derived from repeated verbs or generic nouns.
- Distinguish a misleading label from an invalid member set. Renaming alone cannot
  repair species-specific or equipment-specific false memberships.
- Treat useful granularity as a user preference. Shared vocabulary or an available
  umbrella label does not automatically make a cross-domain group useful.
- Preserve these cases as development regressions. They are already observed and
  must not be described as an untouched test set.

The identity reconciliation implementation addresses reuse of saved IDs, not these
discovery/naming failures. No scoring thresholds, memberships, or topics were edited
in response to this feedback. Additional observations can be appended here before
the laptop comparisons are designed.
