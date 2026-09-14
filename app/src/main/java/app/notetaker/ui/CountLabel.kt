package app.notetaker.ui

/** Short English counts used in notebook summaries. */
fun countLabel(count: Int, singular: String, plural: String = "${singular}s"): String =
    "$count ${if (count == 1) singular else plural}"
