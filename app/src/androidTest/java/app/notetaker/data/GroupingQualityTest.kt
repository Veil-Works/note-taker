package app.notetaker.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import app.notetaker.organization.*
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class GroupingQualityTest {
    @Test fun incidental_words_do_not_name_travel_and_debugging_topics() {
        LocalTextEmbedding(ApplicationProvider.getApplicationContext<Context>()).use { model ->
            val examples = listOf(
                "Camera" to listOf(
                    "Book flights and a hotel for the summer holiday. Remember the camera.",
                    "Reserve accommodation near the airport for our vacation. Pack the camera.",
                ),
                "Notebook" to listOf(
                    "Debug duplicate invoices caused by repeated payment webhooks. Write the findings in the notebook.",
                    "Fix the payment retry bug before releasing the billing service. Record the result in the notebook.",
                ),
            )
            for ((incidental, texts) in examples) {
                val groups = TopicDiscovery(model).discover(texts.mapIndexed { i, text -> DiscoveryNote("$i", text, model.embed(text)) })
                assertTrue("Expected a useful group for $texts", groups.any { it.members.size == 2 })
                assertTrue("Incidental label: $groups", groups.none { it.name.contains(incidental, true) })
                android.util.Log.i("GroupingQuality", "$incidental regression: ${groups.map { it.name }}")
            }
        }
    }
    @Test fun existing_sample_notebook_recovers_missing_notes_and_cross_project_memberships() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val fixture = JSONArray(InstrumentationRegistry.getInstrumentation().context.assets.open("sample-notes.json").bufferedReader().use { it.readText() })
        LocalTextEmbedding(context).use { model ->
            val notes = (0 until fixture.length()).map { index -> DiscoveryNote("${index + 1}", fixture.getString(index), model.embed(fixture.getString(index))) }
            val groups = TopicDiscovery(model).discover(notes)
            groups.forEach { group -> android.util.Log.i("GroupingQuality", JSONObject().put("name", group.name).put("members", JSONArray(group.members.sortedBy { it.toInt() })).toString()) }
            assertTrue(groups.none { it.name.equals("Oats", true) })
            assertTrue(groups.any { it.name == "Groceries" && it.members.containsAll(listOf("43", "50")) })
            fun containing(id: String) = groups.filter { id in it.members }
            assertTrue(containing("8").any { it.name == "Orion" })
            assertTrue(containing("23").map { it.name }.containsAll(listOf("Orion", "Atlas", "Beacon")))
            assertTrue(containing("27").map { it.name }.containsAll(listOf("Orion", "Atlas")))
            assertTrue(containing("41").any { "35" in it.members })
            assertTrue(containing("42").map { it.name }.containsAll(listOf("Running", "Meal planning")))
            assertTrue(containing("57").map { it.name }.containsAll(listOf("Meetings", "Note-taking")))
            assertTrue(groups.single { it.name == "Running gear" }.members.containsAll(listOf("39", "40", "46")))
            assertTrue(groups.none { it.name == "Running shoes" || it.name == "Meeting planning" })
            assertTrue(groups.single { it.name == "Running" }.members.intersect(setOf("44", "50", "72")).isEmpty())
            assertFalse("63" in groups.single { it.name == "Workout gear" }.members)
            assertTrue(containing("51").any { "52" in it.members })
            assertTrue(groups.none { it.name == "Session" || it.name == "Rice" || it.name == "Related notes" })
            assertTrue(groups.single { it.name == "Beacon" }.members.containsAll(listOf("17", "18", "19", "20", "21", "22")))
            assertTrue(containing("67").any { it.members.containsAll(listOf("68", "69")) })
        }
    }
}
