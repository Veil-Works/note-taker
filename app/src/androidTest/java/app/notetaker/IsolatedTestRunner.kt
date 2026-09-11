package app.notetaker

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner

/** Instrumentation never starts the organizer against the phone owner's database. */
class IsolatedTestRunner : AndroidJUnitRunner() {
    override fun newApplication(cl: ClassLoader, className: String, context: Context): Application =
        super.newApplication(cl, Application::class.java.name, context)
}
