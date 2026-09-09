package app.notetaker

import android.app.Application

class NoteTakerApplication : Application() {
    val container: AppContainer by lazy { DefaultAppContainer(this) }
}

