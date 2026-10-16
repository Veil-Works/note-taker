# Fixed Granite-group labeling comparison

Groups and memberships are identical across variants. A = current terms; B = source noun phrases with distinct-note support; C = B candidates semantically reranked with Granite and MMR alternatives. D is a follow-up control combining real contiguous source phrases with noun phrases, using the same semantic ranking. Token control isolates noun extraction with original token-count weighting. No authored categories, label exclusions or note-specific rules. See [protocol](LABEL_COMPARISON_PROTOCOL.md).

Automatic diagnostics below are not label accuracy. C/D optimize semantic scores, so higher scores do not independently prove better labels. Literal support favors observed wording and can penalize a valid paraphrase; a copied phrase can still be a poor topic name. Human review of informativeness and group fit is pending. Means in the first table include only groups with candidates for that variant; the second table compares identical common groups.

| Corpus | Variant | Candidate availability | Unique top labels | Top labels absent verbatim | Mean literal member support | Member cosine | Contrast | Top-5 redundancy |
|---|---|---:|---:|---:|---:|---:|---:|---:|
| development | A_current | 32/32 | 32 | 0 | 0.700 | 0.800 | 0.033 | 0.780 |
| development | B_noun_support | 26/32 | 26 | 0 | 0.733 | 0.810 | 0.040 | 0.785 |
| development | C_semantic_mmr | 26/32 | 26 | 0 | 0.686 | 0.816 | 0.046 | 0.803 |
| development | D_hybrid_semantic | 28/32 | 28 | 0 | 0.684 | 0.823 | 0.049 | 0.807 |
| development | B_token_control | 26/32 | 26 | 0 | 0.733 | 0.808 | 0.038 | 0.785 |
| development | C_semantic_no_mmr | 26/32 | 26 | 0 | 0.686 | 0.816 | 0.046 | 0.805 |
| transfer | A_current | 11/11 | 11 | 0 | 0.697 | 0.804 | 0.051 | 0.781 |
| transfer | B_noun_support | 7/11 | 7 | 0 | 0.643 | 0.800 | 0.042 | 0.805 |
| transfer | C_semantic_mmr | 7/11 | 7 | 0 | 0.643 | 0.806 | 0.050 | 0.805 |
| transfer | D_hybrid_semantic | 10/11 | 10 | 0 | 0.717 | 0.806 | 0.055 | 0.800 |
| transfer | B_token_control | 7/11 | 7 | 0 | 0.643 | 0.807 | 0.049 | 0.805 |
| transfer | C_semantic_no_mmr | 7/11 | 7 | 0 | 0.643 | 0.806 | 0.050 | 0.805 |

## Same-group diagnostics

| Corpus | Variant | Common groups | Literal member support | Member cosine | Contrast |
|---|---|---:|---:|---:|---:|
| development | A_current | 26 | 0.755 | 0.806 | 0.040 |
| development | B_noun_support | 26 | 0.733 | 0.810 | 0.040 |
| development | C_semantic_mmr | 26 | 0.686 | 0.816 | 0.046 |
| development | D_hybrid_semantic | 26 | 0.692 | 0.826 | 0.051 |
| development | B_token_control | 26 | 0.733 | 0.808 | 0.038 |
| transfer | A_current | 7 | 0.679 | 0.809 | 0.050 |
| transfer | B_noun_support | 7 | 0.643 | 0.800 | 0.042 |
| transfer | C_semantic_mmr | 7 | 0.643 | 0.806 | 0.050 |
| transfer | D_hybrid_semantic | 7 | 0.679 | 0.809 | 0.052 |
| transfer | B_token_control | 7 | 0.643 | 0.807 | 0.049 |

## Development: side-by-side labels

| Group | Notes | A: current | B: noun/support | C: semantic | D: hybrid/semantic | Noun/token control |
|---|---:|---|---|---|---|---|
| 1 | 19 | orion | orion | orion | orion | orion |
| 2 | 13 | search | search | atlas search | atlas search | search |
| 3 | 12 | beacon | beacon | beacon | beacon | beacon |
| 4 | 3 | train | train | train | train | train |
| 5 | 7 | chat | chat | chat | chat | chat |
| 6 | 3 | decision | owner | owner | planning | owner |
| 7 | 6 | kg | session | squat session | 80 kg | session |
| 8 | 4 | gym | gym | gym | gym | gym |
| 9 | 3 | rest | pull | pull | pull-ups | rest |
| 10 | 9 | run | pace | km | run | pace |
| 11 | 4 | socks | socks | shoes | running shoes | socks |
| 12 | 7 | bowls | bowls | bowls | meal prep | bowls |
| 13 | 7 | shopping | shopping | shopping | shopping | shopping |
| 14 | 4 | day | (no supported candidate) | (no supported candidate) | (no supported candidate) | (no supported candidate) |
| 15 | 6 | pot | pot | pot | balcony rosemary | pot |
| 16 | 3 | coffee | coffee | coffee | coffee | coffee |
| 17 | 12 | notebook | notebook | notebook | notebook | notebook |
| 18 | 5 | concrete | short notes | short notes | short notes | short notes |
| 19 | 3 | continue | (no supported candidate) | (no supported candidate) | continue | (no supported candidate) |
| 20 | 3 | library | library | library | library | library |
| 21 | 7 | vienna | vienna | vienna | vienna | vienna |
| 22 | 4 | bike | bike | bike light | bike light | bike |
| 23 | 4 | cancel | (no supported candidate) | (no supported candidate) | cancel | (no supported candidate) |
| 24 | 2 | path | path | path | path | path |
| 25 | 2 | drip | (no supported candidate) | (no supported candidate) | (no supported candidate) | (no supported candidate) |
| 26 | 2 | street | street | street | street | street |
| 27 | 3 | card | (no supported candidate) | (no supported candidate) | (no supported candidate) | (no supported candidate) |
| 28 | 3 | argument | argument | argument | argument | argument |
| 29 | 3 | piano | piano | piano | piano | piano |
| 30 | 3 | french | phrases | phrases | phrases | phrases |
| 31 | 2 | pasta | (no supported candidate) | (no supported candidate) | (no supported candidate) | (no supported candidate) |
| 32 | 2 | error | error | error | error | error |

## Member notes and alternatives

### development group 1 (19 notes)

- **A_current:** orion, onboarding, maya, orion onboarding, people
- **B_noun_support:** orion, maya, onboarding, setup, people
- **C_semantic_mmr:** orion, onboarding, orion onboarding, workspace, setup
- **D_hybrid_semantic:** orion, orion onboarding, onboarding, workspace, orion activation
- **B_token_control:** orion, maya, onboarding, people, setup

Members:

- #1: Orion onboarding: people abandon setup when we ask for a workspace name before showing the product.
- #2: Orion todo: sketch a three-step onboarding flow and review it with Maya on Thursday.
- #3: The first Orion screen should show a useful example before asking the user to configure anything.
- #4: Orion activation dropped after we added the team invitation step. Check whether skipping invitations helps.
- #5: Maya suggested moving team invitations until after the first completed task in Orion.
- #6: For Orion, measure time to first useful action instead of counting completed setup fields.
- #7: Orion release checklist: finish keyboard navigation, verify empty states, and update the onboarding screenshots.
- #8: Idea for Orion: let new users try a sample workspace, then replace the sample with their own data.
- #16: Idea: use the same empty-state pattern in Atlas search and Orion onboarding, with one clear next action.
- #23: Friday work plan: review the Orion prototype, investigate the Atlas timeout, then check Beacon receipts.
- #27: Ask Maya to review the Orion copy and ask Jonas to share the Atlas performance trace.
- #73: Orion onboarding test: three people tried to invite a teammate before creating their first task. Put the invitation after the success screen.
- #74: For Orion, let people reopen the sample workspace after setup. One participant deleted the examples before understanding the workflow.
- #75: Orion todo: check the onboarding flow with large text enabled; the final action currently falls below the keyboard.
- #76: Maya's Orion review: the welcome copy promises collaboration, but the first screen only asks for account details.
- #77: Orion activation report should separate people who explored the demo from people who created a real workspace.
- #88: Orion and Atlas both need a useful empty state when someone removes all filters. Review the copy together, but keep the projects separate.
- #89: Ask Maya to test Orion keyboard navigation and Jonas to test Atlas search without a mouse.
- #90: Release planning: finish the Atlas cache fix before the Beacon billing review, then publish the Orion onboarding results.

### development group 2 (13 notes)

- **A_current:** search, atlas, atlas search, documents, document
- **B_noun_support:** search, atlas, atlas search, documents, document
- **C_semantic_mmr:** atlas search, atlas, search, atlas indexing, documents
- **D_hybrid_semantic:** atlas search, atlas, search, atlas indexing, documents
- **B_token_control:** search, atlas, atlas search, documents, document

Members:

- #9: Atlas search: exact project names should rank above loosely related documents.
- #10: Atlas todo: reproduce the search timeout on a workspace with ten thousand documents.
- #11: Search feels slow in Atlas even when the response takes only 400 ms. Keep the previous results visible while loading.
- #12: Atlas search returns duplicate documents when a title and an alias both match.
- #13: Review the Atlas search logs with Jonas. We need examples of queries that return nothing.
- #14: Atlas indexing: a failed document should retry on its own rather than restarting the entire batch.
- #15: The Atlas search filter disappears when I return from a document. Preserve the query and selected project.
- #61: I keep reopening the same document because I remember its project but not its title. Atlas search should handle that.
- #78: Atlas search experiment: show the matching paragraph under each result so people can judge relevance without opening five documents.
- #79: Atlas todo: add a regression case for a deleted document remaining in cached search results.
- #80: Ask Jonas whether Atlas indexing can report the one failed file while the rest of the upload stays searchable.
- #81: Atlas search should remember filters per workspace. Carrying a client filter into a personal workspace produces an unexplained empty screen.
- #82: Compare Atlas search results for “release checklist” and “shipping checklist” using the same saved documents.

### development group 3 (12 notes)

- **A_current:** beacon, payment, subscription, billing, date
- **B_noun_support:** beacon, payment, subscription, billing, receipt
- **C_semantic_mmr:** beacon, payment, beacon payment, billing, subscription
- **D_hybrid_semantic:** beacon, payment, beacon payment, billing, subscription
- **B_token_control:** beacon, payment, subscription, billing, receipt

Members:

- #17: Beacon billing: a payment webhook can arrive twice, so invoice creation needs to be idempotent.
- #18: Beacon todo: test the annual subscription upgrade and verify the prorated invoice.
- #19: A Beacon customer was charged correctly but saw an old plan name on the receipt.
- #20: Beacon launch is blocked by the failed-payment email copy. Ask Lena for the final wording.
- #21: Beacon billing should show the next charge date before the user confirms a plan change.
- #22: Add a retry test for Beacon payment webhooks. A delayed event must not overwrite a newer subscription state.
- #83: Beacon refund test: replay the refund event and confirm that the balance changes only once.
- #84: Beacon todo: include the billing period on exported receipts so finance can reconcile annual plans.
- #85: Lena spotted a confusing Beacon payment warning: an expired card should lead directly to updating payment details.
- #86: Beacon subscription cancellation should explain whether access ends immediately or at the end of the paid period.
- #87: Check Beacon invoices around midnight UTC; the receipt date and charge date should use the same documented timezone.
- #171: Discuss the Beacon receipt export with finance in the planning meeting. Leave with an owner and a sample file to verify.

### development group 4 (3 notes)

- **A_current:** train, agenda, client, make short, tuesday
- **B_noun_support:** train, todo
- **C_semantic_mmr:** train, todo
- **D_hybrid_semantic:** train, todo
- **B_token_control:** train, todo

Members:

- #24: Team todo: reserve a quiet room for the roadmap workshop and send the agenda before Tuesday.
- #138: Expense todo: submit the train receipt from the client visit and attach the meeting date.
- #149: Music idea: make a short playlist of calm instrumental tracks for the train, downloaded before leaving home.

### development group 5 (7 notes)

- **A_current:** chat, short walk, coding, minutes, walk
- **B_noun_support:** chat, short walk, minutes, walk, afternoon
- **C_semantic_mmr:** chat, minutes, short walk, afternoon, walk
- **D_hybrid_semantic:** chat, minutes, coding, short walk, afternoon
- **B_token_control:** chat, short walk, minutes, walk, afternoon

Members:

- #25: Our standup runs long because we solve bugs during status updates. Move debugging into a separate follow-up.
- #28: I get more coding done before opening chat. Try protecting the first ninety minutes of the workday.
- #72: Try a short walk after lunch instead of another coffee. Afternoon meetings feel harder when I stay at the desk all day.
- #94: Protect a morning coding block by closing chat until the first task is finished. Record whether interruptions actually decrease.
- #159: Sleep observation: I stayed up later on the nights I started a new episode after ten. Choose the stopping time before pressing play.
- #160: Charge the phone outside the bedroom tonight. Reaching for the alarm has turned into twenty minutes of scrolling.
- #161: A short walk between calls helped me return to the next meeting with more attention. Put a gap between the two afternoon reviews.

### development group 6 (3 notes)

- **A_current:** decision, owner, decision owner, planning meeting, planning
- **B_noun_support:** owner, decision, planning meeting, planning, meeting
- **C_semantic_mmr:** owner, planning, planning meeting, decision, meeting
- **D_hybrid_semantic:** planning, owner, decision, planning meeting, meeting
- **B_token_control:** owner, decision, planning meeting, planning, meeting

Members:

- #26: Write down the decision and owner at the end of each planning meeting. Too many tasks currently have neither.
- #91: At the next planning meeting, read back the decision before assigning an owner. Last week's notes captured three different interpretations.
- #92: Try a fifteen-minute agenda for the design review: context, decision, owner. Put detailed status updates in the document beforehand.

### development group 7 (6 notes)

- **A_current:** kg, session, 80, 80 kg, bench
- **B_noun_support:** session, kg, set, squat session, sets
- **C_semantic_mmr:** squat session, kg, session, set, sets
- **D_hybrid_semantic:** 80 kg, press 80 kg, squat session, bench press 80, kg
- **B_token_control:** session, kg, set, squat session, sets

Members:

- #29: Strength session: bench press 80 kg for 3 sets of 5. Last set was slow but controlled.
- #30: Next gym session: repeat bench at 80 kg and focus on keeping the shoulder blades stable.
- #31: Squats 90 kg, 3x5, then Romanian deadlifts 60 kg, 3x8. Leave another rep in reserve next time.
- #34: A heavy squat session the night before a long run makes my legs feel flat. Move lower-body training earlier in the week.
- #111: Bench press 80 kg: all three sets of five moved evenly today. Keep the same load for another session.
- #112: Squat session: filmed the final set to check whether depth changes when I get tired.

### development group 8 (4 notes)

- **A_current:** gym, pack, lifting, lifting shoes, towel
- **B_noun_support:** gym, bag, work, todo
- **C_semantic_mmr:** gym, bag, work, todo
- **D_hybrid_semantic:** gym, pack, bag, lifting shoes, lifting
- **B_token_control:** gym, bag, work, todo

Members:

- #32: Gym todo: pack lifting shoes, water bottle, and a clean towel before leaving work.
- #110: Running gear todo: compare small waist belts that hold a phone and keys without bouncing.
- #114: Pack the gym bag tonight: lifting shoes, towel, lock, and water bottle.
- #169: After the bike commute, pack tomorrow's gym clothes while the work bag is already open.

### development group 9 (3 notes)

- **A_current:** rest, pull, pull ups, ups, time
- **B_noun_support:** pull, ups, rest
- **C_semantic_mmr:** pull, ups, rest
- **D_hybrid_semantic:** pull-ups, pull, ups, rest, time
- **B_token_control:** rest, pull, ups

Members:

- #33: Pull-ups felt easier today: 6, 6, and 5 reps. Keep the same rest time next session.
- #38: Intervals tomorrow: warm up, run six short repeats, then cool down. Keep the first repeat controlled.
- #113: Pull-ups: 6, 6, 6 with the same rest between sets. Write the rest interval in the log next time.

### development group 10 (9 notes)

- **A_current:** run, km, easy, pace, run km
- **B_noun_support:** pace, km, river, run
- **C_semantic_mmr:** km, run, pace, river
- **D_hybrid_semantic:** run, km, long run, running, easy
- **B_token_control:** pace, km, river, run

Members:

- #35: Run 7 km Thursday, easy pace along the river. Felt comfortable enough to talk the whole way.
- #36: Sunday long run: 12 km, started too fast and slowed down after the bridge.
- #37: Running goal: build a consistent three-run week before adding more speed work.
- #41: Rainy run this morning was surprisingly quiet. The river path is much less crowded before seven.
- #63: The best ideas on my runs usually disappear before I get home. Capture one sentence as soon as I stop.
- #103: Easy run: 6 km beside the canal, kept the pace relaxed even on the last kilometre.
- #104: Long run was 14 km with an even second half. Starting slower made the final climb feel manageable.
- #105: Running plan: keep Tuesday easy and move the faster session away from Friday squats.
- #107: Running log: windy weather made pace inconsistent, so I used breathing and effort instead of chasing the watch.

### development group 11 (4 notes)

- **A_current:** socks, running, shoes, heel, running shoes
- **B_noun_support:** socks, shoes, 8 km, heel, running
- **C_semantic_mmr:** shoes, running shoes, running socks, socks, running
- **D_hybrid_semantic:** running shoes, shoes, socks, running socks, running
- **B_token_control:** socks, shoes, 8 km, heel, running

Members:

- #39: My running shoes rub the right heel after about 8 km. Try different socks before buying another pair.
- #40: Look for running shoes with a wider toe box. Try them on after work when my feet are less fresh.
- #46: Pick up running socks and a small water bottle before Sunday's long run.
- #109: The new running socks stopped the heel rubbing on an 8 km run. Keep the old shoes for now.

### development group 12 (7 notes)

- **A_current:** bowls, chickpeas, late, lunches, sunday
- **B_noun_support:** bowls, chickpeas, lunches, sunday, lunch
- **C_semantic_mmr:** bowls, lunch, rice, lunches, chickpeas
- **D_hybrid_semantic:** meal prep, bowls, meal, roast, lunches
- **B_token_control:** bowls, chickpeas, lunches, sunday, lunch

Members:

- #42: After a long run I forget to eat until late afternoon. Prepare lunch before heading out on Sunday.
- #44: Buy chickpeas, tomatoes, spinach, and rice for lunches next week.
- #49: Meal prep idea: roast vegetables once and use them in rice bowls and wraps during the week.
- #108: Pack a sandwich before Sunday's long run. Having lunch ready would avoid another afternoon of random snacks.
- #118: Meal prep: make lentil soup on Sunday and freeze two portions for evenings when work runs late.
- #119: Roast a tray of peppers and courgettes for lunches, then add chickpeas when assembling the bowls.
- #156: Test a smaller batch of pancake batter next weekend. The last two pancakes sat too long while the first ones were eaten.

### development group 13 (7 notes)

- **A_current:** shopping, grocery, bin, bin bags, carrots
- **B_noun_support:** shopping, grocery, bin, bin bags, carrots
- **C_semantic_mmr:** shopping, grocery, grocery list, laundry detergent, freezer
- **D_hybrid_semantic:** shopping, grocery, grocery list, laundry detergent, freezer
- **B_token_control:** shopping, grocery, bin, bin bags, carrots

Members:

- #43: Shopping: oats, Greek yogurt, bananas, coffee, and eggs.
- #45: Shopping todo: laundry detergent, bin bags, dish soap, and a replacement kitchen sponge.
- #115: Grocery list: lentils, carrots, onions, yogurt, apples, and coffee filters.
- #116: Before grocery shopping, check the freezer for bread and the cupboard for rice. Buy only what is missing.
- #117: Household shopping: dishwasher tablets, laundry detergent, toilet paper, and bin bags.
- #135: Monthly budget review: separate annual subscriptions from regular groceries so an annual renewal does not look like a food-spending spike.
- #170: Grocery list for meal prep: lentils, tomatoes, carrots, and freezer containers. Make soup before the busy release week.

### development group 14 (4 notes)

- **A_current:** day, kitchen, laptop, open, spare
- **B_noun_support:** (no supported candidate)
- **C_semantic_mmr:** (no supported candidate)
- **D_hybrid_semantic:** (no supported candidate)
- **B_token_control:** (no supported candidate)

Members:

- #47: Need a USB-C cable for the work bag. The spare one at home only charges and cannot connect the monitor.
- #70: The cafe near the station is quiet in the morning and has enough space to open a laptop.
- #132: The kitchen cabinet door catches the drawer below it. Mark the hinge position before trying a small adjustment.
- #133: Buy felt pads for the dining chairs; the floor scratches line up with the two chairs used every day.

### development group 15 (6 notes)

- **A_current:** pot, balcony, balcony rosemary, rosemary, basil
- **B_noun_support:** pot, basil, soil, watering, window
- **C_semantic_mmr:** pot, watering, basil, window, soil
- **D_hybrid_semantic:** balcony rosemary, rosemary, pot, balcony, watering
- **B_token_control:** pot, basil, soil, watering, window

Members:

- #48: Buy a terracotta pot and fresh soil for the balcony rosemary.
- #64: Balcony rosemary looks dry even though the soil is damp. Check how much sun that corner gets.
- #65: Turn the basil pot every few days so it does not keep leaning toward the window.
- #120: The balcony rosemary pot still feels heavy two days after watering. Check the drainage holes before adding more water.
- #121: Basil has new leaves on the window side. Rotate the pot and trim enough for tonight's pasta.
- #122: Buy a shallow tray for the balcony pots so watering does not drip onto the neighbour's chair.

### development group 16 (3 notes)

- **A_current:** coffee, changing, cupboard, experiment, oats
- **B_noun_support:** coffee
- **C_semantic_mmr:** coffee
- **D_hybrid_semantic:** coffee
- **B_token_control:** coffee

Members:

- #50: Check the cupboard before buying more coffee. There are probably two unopened bags behind the oats.
- #157: Coffee experiment: grind slightly coarser tomorrow and keep the water amount unchanged so only one variable moves.
- #158: Clean the coffee grinder catch cup before changing beans. The first cup tasted like a mixture of the old and new roast.

### development group 17 (12 notes)

- **A_current:** notebook, topic, note, automatic, notes
- **B_noun_support:** notebook, topic, note, notes, idea
- **C_semantic_mmr:** notebook, topic, notebook todo, notebook idea, note
- **D_hybrid_semantic:** notebook, topic, notebook todo, notebook idea, automatic
- **B_token_control:** notebook, topic, note, notes, idea

Members:

- #51: App idea: a tiny notebook that finds recurring subjects without making me organize every thought.
- #52: For the notebook app, topic names should explain the connection between notes rather than repeat generic words.
- #53: Idea: show why a note belongs to a topic using a short phrase from a related note.
- #54: Automatic grouping should be patient with vague notes. A wrong confident connection is more distracting than no group yet.
- #55: A shopping note can also be about running. The notebook should allow both topics without merging them.
- #56: Notebook todo: check whether renaming a topic keeps its identity when more related notes arrive.
- #96: Notebook idea: let me inspect the strongest two connections behind an automatic topic before I accept it.
- #97: Notebook todo: verify that editing one sentence removes obsolete topic memberships without losing manual choices.
- #98: For automatic note grouping, a person mentioned once should not outweigh the subject of the whole note.
- #99: The notebook should preserve a renamed topic when a new note uses the old wording. Internal matching can remember the previous name.
- #102: In the notebook topic view, show a useful excerpt for each note instead of repeating the topic name on every row.
- #168: Notebook idea from reading: connect chapter questions with earlier notes, but keep the exact book title visible in the explanation.

### development group 18 (5 notes)

- **A_current:** concrete, short notes, capture, observation, note
- **B_noun_support:** short notes, note, notes
- **C_semantic_mmr:** short notes, note, notes
- **D_hybrid_semantic:** short notes, capture, note, notes, short
- **B_token_control:** short notes, note, notes

Members:

- #57: Idea for meetings: capture decisions as short notes and let recurring project names connect them later.
- #58: Observation: I remember a task better when the note starts with a concrete verb.
- #62: Observation: short notes are easiest to write immediately after a conversation, before details start blending together.
- #95: Keep a shutdown note with the next concrete coding step. It takes too long to reconstruct yesterday's context every morning.
- #100: Try a capture shortcut that opens directly to an empty note with the keyboard ready. Saving two sentences should take one action.

### development group 19 (3 notes)

- **A_current:** continue, use, draft, attention, change
- **B_noun_support:** (no supported candidate)
- **C_semantic_mmr:** (no supported candidate)
- **D_hybrid_semantic:** continue, use
- **B_token_control:** (no supported candidate)

Members:

- #59: People ask fewer questions when a button describes the result, such as Save note, instead of saying Continue.
- #60: A loading animation attracts more attention than a quiet placeholder. Use motion only when it explains a change.
- #101: A label like “Continue” hides what a dialog will do. Use “Delete draft” when the action actually deletes something.

### development group 20 (3 notes)

- **A_current:** library, return library, return, disappears, free
- **B_noun_support:** library
- **C_semantic_mmr:** library
- **D_hybrid_semantic:** library, return the library, return
- **B_token_control:** library

Members:

- #66: Weekend todo: repot the rosemary, clean the balcony table, and return the library book.
- #144: Return the library novel on Saturday and pick up the reserved essay collection.
- #162: On Sunday, plan one evening with no errands or projects. A free slot disappears unless I leave it visible in the calendar.

### development group 21 (7 notes)

- **A_current:** vienna, train, trip, leave afternoon, packing
- **B_noun_support:** vienna, trip, train, packing, vienna trip
- **C_semantic_mmr:** vienna, vienna trip, station, train, trip
- **D_hybrid_semantic:** vienna, vienna trip, train, station, trip
- **B_token_control:** vienna, trip, train, packing, vienna trip

Members:

- #67: Plan a weekend train trip to Vienna. Compare an early Saturday departure with leaving Friday after work.
- #68: Vienna trip: find a place near public transport and leave one afternoon free for walking.
- #69: Book the train before choosing a hotel for Vienna. The return time decides whether Sunday dinner is realistic.
- #123: Vienna weekend: reserve the museum for Saturday morning and leave the afternoon unscheduled.
- #124: Save the Vienna train tickets offline before leaving. The station connection was unreliable last time.
- #125: Vienna packing list: walking shoes, rain jacket, charger, and a small bag for the train.
- #167: For the Vienna trip, take the small camera and use the station wait for a photo walk. Keep the rest of the packing light.

### development group 22 (4 notes)

- **A_current:** bike, bike light, light, commute, check
- **B_noun_support:** bike, bike light, light
- **C_semantic_mmr:** bike light, bike, light
- **D_hybrid_semantic:** bike light, bike, light, check
- **B_token_control:** bike, bike light, light

Members:

- #71: Todo: book a haircut, replace the bike light batteries, and collect the parcel from the pickup point.
- #126: Bike maintenance: the rear brake rubs after I put the wheel back in. Check that the wheel is seated before adjusting the caliper.
- #127: Clean the bike chain this weekend and wipe off excess lubricant before the next commute.
- #128: The front bike light fades halfway home. Recharge it at work and check the battery before Friday's ride.

### development group 23 (4 notes)

- **A_current:** cancel, access, account, email, export
- **B_noun_support:** (no supported candidate)
- **C_semantic_mmr:** (no supported candidate)
- **D_hybrid_semantic:** cancel
- **B_token_control:** (no supported candidate)

Members:

- #93: Cancel the weekly sync when there is no decision to make. A short written update should cover routine progress.
- #136: Cancel the unused cloud storage trial before it renews. Export the two files I still need first.
- #137: Keep warranty receipts in one folder named by appliance and purchase date. Searching email for “receipt” returns too many unrelated orders.
- #165: Security housekeeping: review which old devices still have access to my account and remove the ones I no longer own.

### development group 24 (2 notes)

- **A_current:** path, bridge, canal, river path, turn
- **B_noun_support:** path
- **C_semantic_mmr:** path
- **D_hybrid_semantic:** path
- **B_token_control:** path

Members:

- #106: Try the park loop for the next recovery run; the river path has construction near the bridge.
- #129: Cycling route idea: take the canal path to avoid the junction where cars turn across the bike lane.

### development group 25 (2 notes)

- **A_current:** drip, measure, replacement, spare, yesterday
- **B_noun_support:** (no supported candidate)
- **C_semantic_mmr:** (no supported candidate)
- **D_hybrid_semantic:** (no supported candidate)
- **B_token_control:** (no supported candidate)

Members:

- #130: Keep a spare inner tube and tyre levers in the saddle bag. The pump alone was not enough after yesterday's puncture.
- #131: Home repair todo: measure the bathroom tap washer before buying a replacement for the slow drip.

### development group 26 (2 notes)

- **A_current:** street, changing, photo, photo walk, scene
- **B_noun_support:** street
- **C_semantic_mmr:** street
- **D_hybrid_semantic:** street
- **B_token_control:** street

Members:

- #139: Photography practice: take the same street scene from three positions before changing camera settings.
- #142: Photo walk idea: look for repeated shapes in station windows and bicycle racks rather than another wide street view.

### development group 27 (3 notes)

- **A_current:** card, files, laptop, second, camera
- **B_noun_support:** (no supported candidate)
- **C_semantic_mmr:** (no supported candidate)
- **D_hybrid_semantic:** (no supported candidate)
- **B_token_control:** (no supported candidate)

Members:

- #140: The sunset photos lost detail in the bright sky. Next time compare exposures on location instead of trusting the screen brightness.
- #141: Back up the camera card before deleting blurry shots. Keep the original files until the second copy is verified.
- #166: Set a reminder to test restoring a file from the laptop backup. A green backup icon does not show whether I can recover what I need.

### development group 28 (3 notes)

- **A_current:** argument, chapter, finished, novel, saying
- **B_noun_support:** argument
- **C_semantic_mmr:** argument
- **D_hybrid_semantic:** argument
- **B_token_control:** argument

Members:

- #143: Reading note: the most memorable scene in the novel reveals the argument through what the characters avoid saying.
- #145: For book notes, write one claim and one question after each chapter. Copying long paragraphs is not helping me remember the argument.
- #146: Start a small list of books I actually finished, with one sentence about who might enjoy each one.

### development group 29 (3 notes)

- **A_current:** piano, practice, falls, phrase, record
- **B_noun_support:** piano, practice
- **C_semantic_mmr:** piano, practice
- **D_hybrid_semantic:** piano, practice
- **B_token_control:** piano, practice

Members:

- #147: Piano practice: isolate the two bars where the left hand falls behind, then join them back into the phrase slowly.
- #148: Record the piano piece once at the end of practice. I miss uneven pauses while concentrating on the fingering.
- #150: Replace the worn guitar strings before recording the chord progression. The third string drifts out of tune after a few minutes.

### development group 30 (3 notes)

- **A_current:** french, phrases, conversation, asking, faster
- **B_noun_support:** phrases, conversation
- **C_semantic_mmr:** phrases, conversation
- **D_hybrid_semantic:** phrases, french, conversation
- **B_token_control:** phrases, conversation

Members:

- #151: French practice: save three phrases from the podcast and say them aloud in a sentence of my own.
- #152: I recognize French words on the page faster than in conversation. Replay a short audio clip without reading the transcript first.
- #153: Language study todo: review the phrases for asking someone to repeat or speak more slowly before the conversation session.

### development group 31 (2 notes)

- **A_current:** pasta, reserved, water, time, note
- **B_noun_support:** (no supported candidate)
- **C_semantic_mmr:** (no supported candidate)
- **D_hybrid_semantic:** (no supported candidate)
- **B_token_control:** (no supported candidate)

Members:

- #154: Cook the mushroom pasta with a little reserved cooking water; the sauce was too thick by the time it reached the plate.
- #155: Recipe note: lemon zest made the chickpea salad brighter without needing another spoonful of dressing.

### development group 32 (2 notes)

- **A_current:** error, failed, explanation, fields, uses
- **B_noun_support:** error
- **C_semantic_mmr:** error
- **D_hybrid_semantic:** error, failed
- **B_token_control:** error

Members:

- #163: Accessibility review: the error message uses colour alone. Add a short explanation beside the field that failed.
- #164: The checkout form clears valid fields after a network error. Keep the entered values and let the user retry the failed step.


## Transfer: side-by-side labels

| Group | Notes | A: current | B: noun/support | C: semantic | D: hybrid/semantic | Noun/token control |
|---|---:|---|---|---|---|---|
| 1 | 4 | telescope | telescope | telescope | telescope | telescope |
| 2 | 3 | fabric | fabric | fabric | fabric | fabric |
| 3 | 4 | aquarium | (no supported candidate) | (no supported candidate) | aquarium | (no supported candidate) |
| 4 | 4 | glaze | clay | pottery | pottery | glaze |
| 5 | 3 | configuration | build | build | failed | build |
| 6 | 2 | cancel | (no supported candidate) | (no supported candidate) | (no supported candidate) | (no supported candidate) |
| 7 | 3 | loaf | loaf | loaf | loaf | loaf |
| 8 | 4 | japanese | sentence | sentence | japanese | sentence |
| 9 | 3 | compare | (no supported candidate) | (no supported candidate) | compare | (no supported candidate) |
| 10 | 4 | bedtime | bedtime | bedtime | bedtime | bedtime |
| 11 | 2 | chess | (no supported candidate) | (no supported candidate) | chess | (no supported candidate) |

## Member notes and alternatives

### transfer group 1 (4 notes)

- **A_current:** telescope, measure, cloth, cutting, night
- **B_noun_support:** telescope
- **C_semantic_mmr:** telescope
- **D_hybrid_semantic:** telescope
- **B_token_control:** telescope

Members:

- #1: The telescope image shakes whenever I touch the focus wheel. Tighten the tripod brace before the next observing session.
- #2: Saturn looked sharper after the telescope had been outside for half an hour. Set it up before dinner next time.
- #3: Keep a red torch beside the star chart so I can read it without losing my night vision.
- #31: Sew a padded sleeve for the telescope tube. Measure the circumference with the lens cap attached before cutting the cloth.

### transfer group 2 (3 notes)

- **A_current:** fabric, jacket, seam, cloth, cutting
- **B_noun_support:** fabric, jacket, seam
- **C_semantic_mmr:** fabric, seam, jacket
- **D_hybrid_semantic:** fabric, seam, jacket
- **B_token_control:** fabric, jacket, seam

Members:

- #4: Shorten the jacket sleeves by two centimetres, but baste the new hem before cutting any fabric.
- #5: The sewing machine skips stitches on the thick seam. Try a fresh needle on a scrap of the same cloth.
- #40: Patch the torn jacket pocket using the leftover lining fabric. Sew a sample seam before opening the lining.

### transfer group 3 (4 notes)

- **A_current:** aquarium, check, deployment, temperature, build
- **B_noun_support:** (no supported candidate)
- **C_semantic_mmr:** (no supported candidate)
- **D_hybrid_semantic:** aquarium
- **B_token_control:** (no supported candidate)

Members:

- #7: The aquarium filter flow has weakened. Rinse the removable sponge during the next tank maintenance session.
- #8: Write down the aquarium test results in one place so changes are visible across weeks.
- #9: The fish leave some food untouched in the evening. Record the amount offered and what remains rather than guessing each day.
- #33: Build an aquarium temperature dashboard. The deployment should roll back if the sensor update check fails.

### transfer group 4 (4 notes)

- **A_current:** glaze, clay, pottery, ask, baking
- **B_noun_support:** clay, glaze, pottery
- **C_semantic_mmr:** pottery, glaze, clay
- **D_hybrid_semantic:** pottery, glaze, clay
- **B_token_control:** glaze, clay, pottery

Members:

- #10: The clay bowl dried unevenly near the rim. Cover the thinner edge during the next drying session.
- #11: Label each glaze test tile with the firing schedule as well as the glaze name.
- #12: Trim the foot of the pottery cup before its base becomes too dry to cut cleanly.
- #32: Make a ceramic dish for baking a small loaf. Ask the pottery studio which clay and glaze are suitable for that use.

### transfer group 5 (3 notes)

- **A_current:** configuration, failed, build, test, deployment
- **B_noun_support:** build, configuration
- **C_semantic_mmr:** build, configuration
- **D_hybrid_semantic:** failed, configuration, build, test
- **B_token_control:** build, configuration

Members:

- #13: The deployment failed because the configuration key was missing. Add a validation step before publishing the build.
- #15: A rollback should restore the application and its configuration together. Test that path in staging before the release.
- #38: Let the integration tests run overnight and save the failed test logs with the build number.

### transfer group 6 (2 notes)

- **A_current:** cancel, release, new
- **B_noun_support:** (no supported candidate)
- **C_semantic_mmr:** (no supported candidate)
- **D_hybrid_semantic:** (no supported candidate)
- **B_token_control:** (no supported candidate)

Members:

- #14: Keep the previous release available until the new service passes its health checks.
- #36: Cancel the unused music-streaming subscription before its annual renewal.

### transfer group 7 (3 notes)

- **A_current:** loaf, baking, temperature, time, compare
- **B_noun_support:** loaf
- **C_semantic_mmr:** loaf
- **D_hybrid_semantic:** loaf
- **B_token_control:** loaf

Members:

- #16: The bread dough spread sideways during the final proof. Compare the next loaf after shaping it more tightly.
- #17: Write the room temperature beside the sourdough rise time; the kitchen was much colder this morning.
- #18: Score the loaf just before it goes into the oven and photograph the result after baking.

### transfer group 8 (4 notes)

- **A_current:** japanese, sentence, listening, record, short
- **B_noun_support:** sentence
- **C_semantic_mmr:** sentence
- **D_hybrid_semantic:** japanese, sentence, listening, record
- **B_token_control:** sentence

Members:

- #19: Japanese listening practice: replay one short dialogue until I can hear where each sentence ends.
- #20: Write a Japanese example sentence for each new word instead of memorizing a translation alone.
- #21: Record the same Japanese sentence twice, once after reading it and once after listening to the speaker.
- #34: Read one page of the Japanese short-story collection and record the phrases that only make sense in context.

### transfer group 9 (3 notes)

- **A_current:** compare, game, open, reading, short
- **B_noun_support:** (no supported candidate)
- **C_semantic_mmr:** (no supported candidate)
- **D_hybrid_semantic:** compare
- **B_token_control:** (no supported candidate)

Members:

- #23: The novel switches narrator halfway through the chapter. Mark the change so I can compare what each character knows.
- #24: Keep a short reading note after each finished book: the idea I remember and the question it left open.
- #30: Review the rapid game without an engine first, then compare my notes with the computer analysis.

### transfer group 10 (4 notes)

- **A_current:** bedtime, evening routine, routine, evening, tomorrow
- **B_noun_support:** bedtime, evening routine, routine, evening
- **C_semantic_mmr:** bedtime, evening routine, routine, evening
- **D_hybrid_semantic:** bedtime, evening routine, routine, evening
- **B_token_control:** bedtime, evening routine, routine, evening

Members:

- #25: I went to bed later when the laptop stayed open on the sofa. Put it away before starting the evening routine.
- #26: Set out tomorrow's clothes before winding down so bedtime does not become another round of chores.
- #35: Late telescope sessions keep pushing bedtime back. Choose one observing night and keep the usual evening routine on the others.
- #39: An easy run around the lake felt comfortable today. Keep the next outing short too.

### transfer group 11 (2 notes)

- **A_current:** chess, check, game, let, practice
- **B_noun_support:** (no supported candidate)
- **C_semantic_mmr:** (no supported candidate)
- **D_hybrid_semantic:** chess
- **B_token_control:** (no supported candidate)

Members:

- #28: Chess practice: replay the rook ending and identify the move where I let the king become inactive.
- #29: In the next chess game, check the opponent's forcing moves before choosing an attacking move.

Full rankings, phrase source spans, component scores and hashes: [JSON](LABEL_COMPARISON_RESULTS.json). Parser evidence: [manifest](embedding-licenses/parser-manifest.json). No Android or app discovery change is included.
