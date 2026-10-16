// Run with node scripts/embeddings/test_review_ui.cjs. No browser dependencies.
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');

async function scenario(autosaveDuringNavigation) {
  const requests = [];
  const context = vm.createContext({
    structuredClone, setTimeout, clearTimeout,
    document: {getElementById: () => ({textContent: '', classList: {toggle() {}}})},
    window: {scrollTo() {}},
    fetch: (_, options) => new Promise(resolve => requests.push({
      payload: JSON.parse(options.body),
      resolve: () => resolve({ok: true, json: async () => ({revision: requests.length, review: JSON.parse(options.body).review})})
    }))
  });
  const source = fs.readFileSync(__dirname + '/review_ui/app.js', 'utf8');
  vm.runInContext(source.slice(0, source.indexOf("$('previous').onclick")), context);
  vm.runInContext(`
    study = {studyId:'test', token:'test', reviews:{}};
    current = {id:'source'}; draft = blank(); draft.comment = 'first'; dirty = true;
    sidebar = () => {};
    render = g => { current = g; draft = blank(); dirty = false; };
    navigation = navigate({id:'destination'});
  `, context);
  await new Promise(setImmediate);
  assert.equal(requests.length, 1);
  vm.runInContext("draft.comment = 'latest'; dirty = true; editSerial++;", context);
  if (autosaveDuringNavigation) vm.runInContext('flush();', context);
  requests[0].resolve();
  await new Promise(setImmediate);
  assert.equal(vm.runInContext('current.id', context), 'source', 'must not leave before latest edit is saved');
  assert.equal(requests.length, 2);
  assert.equal(requests[1].payload.review.comment, 'latest');
  requests[1].resolve();
  await vm.runInContext('navigation', context);
  assert.equal(vm.runInContext('current.id', context), 'destination');
  assert.equal(vm.runInContext('study.reviews.source.review.comment', context), 'latest');
}

(async () => {
  await scenario(false);
  await scenario(true);
  console.log('Passed: navigation drains edits and queued autosaves during in-flight requests.');
})().catch(error => { console.error(error); process.exitCode = 1; });
