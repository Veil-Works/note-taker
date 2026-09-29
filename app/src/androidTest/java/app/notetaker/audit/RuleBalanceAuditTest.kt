package app.notetaker.audit

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import app.notetaker.organization.LocalTextEmbedding
import app.notetaker.organization.TextEmbedding
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.Assume.assumeTrue
import java.io.File

/** Isolated policy comparison; results require editorial review, not a pass/fail quality score. */
class RuleBalanceAuditTest {
    @Test fun compare_rules_on_original_and_expanded_notebooks() {
        val args = InstrumentationRegistry.getArguments()
        assumeTrue("Opt-in analysis: pass -e auditRules true", args.getString("auditRules") == "true")
        val selected = args.getString("policies")?.split(",")?.toSet()
        val context = ApplicationProvider.getApplicationContext<Context>()
        val assets = InstrumentationRegistry.getInstrumentation().context.assets
        val texts = listOf("sample-notes.json", "expanded-notes.json").flatMap { file ->
            val array = JSONArray(assets.open(file).bufferedReader().use { it.readText() })
            (0 until array.length()).map(array::getString)
        }
        val policies = listOf(
            AuditPolicy("baseline"),
            AuditPolicy("no_purpose_preference", purposePreference = false),
            AuditPolicy("no_literal_bypass", literalBypass = false),
            AuditPolicy("contextual_clauses", contextualClauses = true),
            AuditPolicy("moderate_combination", memberFloor = .24, literalBypass = false, reserveLabels = false, contextualClauses = true, purposePreference = false),
            AuditPolicy("no_clauses", clauses = false),
            AuditPolicy("no_lexical_relaxation", lexicalRelaxation = false),
            AuditPolicy("strict_scope", memberFloor = .30, literalBypass = false),
            AuditPolicy("no_label_reservation", reserveLabels = false),
            AuditPolicy("combined", lexicalRelaxation = false, memberFloor = .26, literalBypass = false, reserveLabels = false, contextualClauses = true),
            AuditPolicy("combined_no_bridge_merges", lexicalRelaxation = false, memberFloor = .26, literalBypass = false, reserveLabels = false, contextualClauses = true, bridgeMerges = false),
        )
        val output = JSONArray()
        val trace = StringBuilder()
        LocalTextEmbedding(context).use { model ->
            val cached = object : TextEmbedding {
                override val version = model.version
                val vectors = mutableMapOf<String, FloatArray>()
                override fun embed(text: String) = vectors.getOrPut(text) { model.embed(text) }
            }
            val all = texts.mapIndexed { i, text -> DiscoveryNote("${i + 1}", text, cached.embed(text)) }
            for (size in listOf(72, 172)) {
                for (policy in policies.filter { selected == null || it.name in selected }) {
                    trace.appendLine("RUN $size ${policy.name}")
                    val groups = AuditDiscovery(cached, policy) { if (policy.name == "baseline") trace.appendLine(it) }.discover(all.take(size))
                    if (policy.name == "baseline") {
                        val production = app.notetaker.organization.TopicDiscovery(cached).discover(all.take(size).map {
                            app.notetaker.organization.DiscoveryNote(it.id, it.text, it.vector)
                        })
                        assertEquals(production.map { it.name to it.members }, groups.map { it.name to it.members })
                    }
                    val items = JSONArray()
                    groups.forEach { group -> items.put(JSONObject().put("name", group.name).put("named", group.namedSubject)
                        .put("members", JSONArray(group.members.sortedBy(String::toInt)))) }
                    output.put(JSONObject().put("size", size).put("policy", policy.name).put("groups", items))
                    android.util.Log.i("RuleBalanceAudit", "$size ${policy.name}: ${groups.size} groups")
                }
            }
            val vectors = JSONObject()
            cached.vectors.forEach { (text, vector) -> vectors.put(text, JSONArray(vector.toList())) }
            File(context.filesDir, "rule-audit-vectors.json").writeText(vectors.toString())
        }
        File(context.filesDir, "rule-audit-results.json").writeText(output.toString(2))
        File(context.filesDir, "rule-audit-trace.txt").writeText(trace.toString())
    }
}
