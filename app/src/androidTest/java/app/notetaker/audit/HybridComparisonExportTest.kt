package app.notetaker.audit

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import app.notetaker.organization.DiscoveryNote
import app.notetaker.organization.LocalTextEmbedding
import app.notetaker.organization.TopicDiscovery
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/** Export live production baselines and retrieval vectors. Never opens the journal database. */
class HybridComparisonExportTest {
    @Test fun export_three_comparison_corpora() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("hybridAudit") == "true")
        val context = ApplicationProvider.getApplicationContext<Context>()
        val assets = InstrumentationRegistry.getInstrumentation().context.assets
        fun read(path: String): List<String> {
            val items = JSONArray(assets.open(path).bufferedReader().use { it.readText() })
            return (0 until items.length()).map(items::getString)
        }
        val original = read("sample-notes.json")
        val corpora = linkedMapOf("original72" to original,
            "expanded172" to original + read("expanded-notes.json"),
            "heldout40" to read("hybrid/heldout-notes.json"))
        val output = JSONObject()
        LocalTextEmbedding(context).use { model ->
            val cache = mutableMapOf<String, FloatArray>()
            corpora.forEach { (name, texts) ->
                val start = System.nanoTime()
                val notes = texts.mapIndexed { i, text -> DiscoveryNote("${i + 1}", text, cache.getOrPut(text) { model.embed(text) }) }
                val groups = TopicDiscovery(model).discover(notes)
                val serializedNotes = JSONArray()
                notes.forEach { serializedNotes.put(JSONObject().put("id", it.id).put("text", it.text).put("vector", JSONArray(it.vector.toList()))) }
                val serializedGroups = JSONArray()
                groups.forEach { serializedGroups.put(JSONObject().put("key", it.key).put("label", it.name)
                    .put("named", it.namedSubject).put("members", JSONArray(it.members.sortedBy(String::toInt)))) }
                output.put(name, JSONObject().put("notes", serializedNotes).put("groups", serializedGroups)
                    .put("baselineMs", (System.nanoTime() - start) / 1_000_000).put("embeddingVersion", model.version))
            }
        }
        File(context.filesDir, "hybrid-comparison-inputs.json").writeText(output.toString())
    }
}
