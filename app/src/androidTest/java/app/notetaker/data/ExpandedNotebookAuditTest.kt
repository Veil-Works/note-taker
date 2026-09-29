package app.notetaker.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import app.notetaker.organization.DiscoveryNote
import app.notetaker.organization.LocalTextEmbedding
import app.notetaker.organization.TopicDiscovery
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

/** Diagnostic output for editorial review; completion is not a grouping-quality score. */
class ExpandedNotebookAuditTest {
    @Test fun report_expanded_notebook_discovery_without_existing_topic_history() {
        val assets = InstrumentationRegistry.getInstrumentation().context.assets
        val texts = listOf("sample-notes.json", "expanded-notes.json").flatMap { file ->
            val array = JSONArray(assets.open(file).bufferedReader().use { it.readText() })
            (0 until array.length()).map(array::getString)
        }
        assertEquals(172, texts.size)
        LocalTextEmbedding(ApplicationProvider.getApplicationContext<Context>()).use { model ->
            val notes = texts.mapIndexed { i, text -> DiscoveryNote("${i + 1}", text, model.embed(text)) }
            val start = System.nanoTime()
            val groups = TopicDiscovery(model).discover(notes)
            groups.forEach { group ->
                android.util.Log.i("ExpandedAudit", JSONObject().put("name", group.name)
                    .put("namedSubject", group.namedSubject)
                    .put("members", JSONArray(group.members.sortedBy(String::toInt))).toString())
            }
            android.util.Log.i("ExpandedAudit", "Discovery milliseconds: ${(System.nanoTime() - start) / 1_000_000}")
        }
    }
}
