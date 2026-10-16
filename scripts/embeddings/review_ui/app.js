'use strict';
const $ = id => document.getElementById(id);
const el = (tag, text, cls) => {
  const node = document.createElement(tag);
  if (text !== undefined) node.textContent = text;
  if (cls) node.className = cls;
  return node;
};
let study, current, draft, timer, saveChain = Promise.resolve(), dirty = false, navigating = false, editSerial = 0;
const blank = () => ({
  preferred: null, labels: {'1': {clarity:null, fit:null}, '2': {clarity:null, fit:null}},
  coherence:null, incorrectNoteIds:[], comment:'', suggestedLabel:'', complete:false
});
function status(text, error = false) {
  $('save-status').textContent = text;
  $('save-status').classList.toggle('error', error);
}
function completed(g) { return Boolean(study.reviews[g.id]?.review.complete); }
function visible() {
  const query = $('search').value.trim().toLowerCase();
  return study.groups.filter(g =>
    ($('corpus').value === 'all' || g.corpus === $('corpus').value) &&
    ($('filter').value === 'all' || completed(g) === ($('filter').value === 'complete')) &&
    (!query || [g.id, ...Object.values(g.proposals).flat(), ...g.notes.map(n=>n.text)].join(' ').toLowerCase().includes(query)));
}
function sidebar() {
  const count = study.groups.filter(completed).length;
  $('progress').textContent = count + ' / ' + study.groups.length + ' reviewed';
  $('results').hidden = count !== study.groups.length;
  $('topics').replaceChildren();
  for (const g of visible()) {
    const button = el('button', undefined, 'topic-button' + (current?.id === g.id ? ' active' : ''));
    if (completed(g)) button.append(el('span', '✓', 'done'));
    button.append(el('span', 'Topic ' + String(g.number).padStart(2, '0')));
    button.append(el('small', (g.corpus === 'development' ? 'Development' : 'Transfer') + ' · ' + g.notes.length + ' notes'));
    button.onclick = () => navigate(g);
    $('topics').append(button);
  }
}
function edited() {
  dirty = true; editSerial++;
  if (draft.complete && validation()) draft.complete = false;
  status('Saving…');
  clearTimeout(timer);
  timer = setTimeout(() => flush().catch(showError), 250);
}
function showError(error) { status(error.message || 'Could not save. Try again.', true); }
async function flush() {
  clearTimeout(timer);
  if (!dirty || !current) return saveChain;
  const group = current, snapshot = structuredClone(draft), serial = editSerial;
  dirty = false;
  const task = saveChain.catch(() => {}).then(async () => {
    const revision = study.reviews[group.id]?.revision ?? 0;
    const response = await fetch('/api/review', {
      method:'POST', headers:{'Content-Type':'application/json', 'X-Review-Token':study.token},
      body:JSON.stringify({studyId:study.studyId, groupId:group.id, revision, review:snapshot})
    });
    const record = await response.json();
    if (!response.ok) throw new Error(record.error || 'Could not save locally.');
    study.reviews[group.id] = record;
    if (current?.id === group.id && editSerial === serial) status('Saved locally');
    sidebar();
  }).catch(error => {
    if (current?.id === group.id) dirty = true;
    throw error;
  });
  saveChain = task;
  return task;
}
function choices(container, name, options, selected, change) {
  container.replaceChildren();
  for (const [value, label] of options) {
    const wrap = el('label', undefined, 'choice');
    const radio = el('input');
    radio.type = 'radio'; radio.name = name; radio.value = String(value);
    radio.checked = selected === value;
    radio.onchange = () => { change(value); edited(); };
    wrap.append(radio, el('span', label));
    container.append(wrap);
  }
}
// Edits and autosave timers can enqueue another snapshot while a request is
// pending. Drain that work before replacing the form or exporting its data.
async function settleSaves() {
  while (true) {
    const pending = flush(), tail = saveChain;
    await pending;
    if (!dirty && tail === saveChain) return;
  }
}
function ratings(card, slot, kind, title, options) {
  const field = el('fieldset', undefined, 'rating'), row = el('div', undefined, 'choices');
  field.append(el('legend', title), row);
  choices(row, 'proposal-' + slot + '-' + kind, options, draft.labels[slot][kind], value => draft.labels[slot][kind] = value);
  card.append(field);
}
function render(g) {
  current = g;
  $('summary').hidden = true; $('review').hidden = !g; $('empty').hidden = Boolean(g);
  if (!g) { sidebar(); return; }
  draft = structuredClone(study.reviews[g.id]?.review || blank());
  dirty = false; editSerial++;
  $('topic-title').textContent = 'Topic ' + String(g.number).padStart(2, '0');
  $('corpus-name').textContent = g.corpus === 'development' ? 'Development notebook' : 'Transfer notebook';
  $('member-count').textContent = g.notes.length + ' member notes';
  $('validation').textContent = '';
  status(study.reviews[g.id] ? 'Saved locally' : 'Not reviewed yet');
  $('proposals').replaceChildren();
  for (const slot of ['1','2']) {
    const candidates = g.proposals[slot], card = el('section', undefined, 'proposal');
    card.append(el('h3', 'Proposal ' + slot), el('div', candidates[0] || 'No label proposed', 'candidate' + (candidates.length ? '' : ' missing')));
    if (candidates.length) {
      ratings(card, slot, 'clarity', 'Is the leading label clear?', [[1,'Confusing'],[2,'Okay'],[3,'Clear']]);
      ratings(card, slot, 'fit', 'Does it describe the whole group?', [[1,'Poor fit'],[2,'Partial fit'],[3,'Good fit']]);
      if (candidates.length > 1) {
        const details = el('details'), list = el('ul');
        for (const name of candidates.slice(1)) list.append(el('li', name));
        details.append(el('summary', 'Other candidates'), list); card.append(details);
      }
    } else card.append(el('p', 'This method did not produce a supported label for the group.', 'instruction'));
    $('proposals').append(card);
  }
  choices($('preference'), 'preferred', [['1','Proposal 1'],['2','Proposal 2'],['tie','Equally good'],['neither','Neither works']], draft.preferred, value => draft.preferred = value);
  choices($('coherence'), 'coherence', [['good','Useful, coherent topic'],['mixed','Mixed or overly broad'],['unsure','Unsure']], draft.coherence, value => draft.coherence = value);
  $('suggested-label').value = draft.suggestedLabel; $('comment').value = draft.comment;
  $('notes').replaceChildren();
  for (const note of g.notes) {
    const row = el('article', undefined, 'note'), text = el('div'), flag = el('label', undefined, 'flag'), checkbox = el('input');
    text.append(el('div', 'NOTE ' + note.id, 'note-id'), el('p', note.text));
    checkbox.type = 'checkbox'; checkbox.checked = draft.incorrectNoteIds.includes(note.id);
    row.classList.toggle('flagged', checkbox.checked);
    checkbox.onchange = () => {
      draft.incorrectNoteIds = checkbox.checked ? [...new Set([...draft.incorrectNoteIds,note.id])] : draft.incorrectNoteIds.filter(id=>id!==note.id);
      row.classList.toggle('flagged', checkbox.checked); edited();
    };
    flag.append(checkbox, el('span', 'Does not belong')); row.append(text, flag); $('notes').append(row);
  }
  sidebar();
}
function validation() {
  if (!draft.preferred || !draft.coherence) return 'Choose a preferred proposal and rate group coherence.';
  for (const slot of ['1','2']) {
    if (current.proposals[slot].length && (!draft.labels[slot].clarity || !draft.labels[slot].fit)) return 'Rate clarity and fit for both available proposals.';
  }
  return '';
}
async function navigate(g) {
  if (navigating) return;
  navigating = true;
  try { await settleSaves(); render(g); window.scrollTo({top:0}); }
  catch (error) { showError(error); }
  finally { navigating = false; }
}
async function advance(direction, finish) {
  if (navigating || !current) return;
  if (finish) {
    const message = validation();
    if (message) { $('validation').textContent = message; return; }
    draft.complete = true; edited();
  }
  const list = visible(), position = list.findIndex(g=>g.id===current.id);
  let target = list[position+direction];
  if (finish && !target) target = study.groups.find(g=>g.id!==current.id && !completed(g));
  await navigate(target || current);
}
async function refilter() {
  if (navigating) return;
  navigating = true;
  try {
    await settleSaves();
    const list = visible();
    if (!list.some(g=>g.id===current?.id)) render(list[0]);
    else sidebar();
  } catch (error) { showError(error); }
  finally { navigating = false; }
}
$('previous').onclick = () => advance(-1, false);
$('next').onclick = () => advance(1, false);
$('finish').onclick = () => advance(1, true);
$('comment').oninput = () => { draft.comment = $('comment').value; edited(); };
$('suggested-label').oninput = () => { draft.suggestedLabel = $('suggested-label').value; edited(); };
for (const id of ['corpus','filter']) $(id).onchange = refilter;
$('search').oninput = refilter;
$('export').onclick = async event => {
  event.preventDefault();
  try { await settleSaves(); window.location.href = '/api/export'; } catch (error) { showError(error); }
};
$('results').onclick = async () => {
  try {
    await settleSaves();
    const response = await fetch('/api/summary'), data = await response.json();
    if (!response.ok) throw new Error(data.error);
    $('review').hidden = true; $('summary').hidden = false; $('summary').replaceChildren(el('h2','Review results'));
    $('summary').append(el('p',data.completed + ' topics reviewed; ' + data.flaggedMemberships + ' memberships flagged.'));
    const table = el('table'), header = el('tr');
    for (const name of ['Method','Preferred','Clarity / 3','Group fit / 3']) header.append(el('th',name));
    table.append(header);
    const names = {A_current:'Current term ranking',D_hybrid_semantic:'Hybrid phrases + semantic ranking'};
    for (const [method, scores] of Object.entries(data.methods)) {
      const row = el('tr');
      for (const text of [names[method],scores.preferred,scores.clarityMean?.toFixed(2) ?? '—',scores.fitMean?.toFixed(2) ?? '—']) row.append(el('td',String(text)));
      table.append(row);
    }
    $('summary').append(table,el('p','These ratings cover the observed notebooks; they do not establish performance on new collections.'));
  } catch (error) { showError(error); }
};
window.addEventListener('beforeunload', event => {
  if (dirty || $('save-status').textContent === 'Saving…') { event.preventDefault(); event.returnValue = ''; }
});
(async () => {
  try {
    const response = await fetch('/api/data');
    if (!response.ok) throw new Error('Could not load the local review.');
    study = await response.json();
    render(study.groups.find(g=>!completed(g)) || study.groups[0]);
  } catch (error) { $('fatal').hidden = false; $('fatal').textContent = error.message; }
})();
