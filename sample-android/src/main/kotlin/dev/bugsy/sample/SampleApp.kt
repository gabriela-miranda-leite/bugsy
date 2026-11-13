package dev.bugsy.sample

import android.app.Application
import com.bugsy.core.Bugsy
import com.bugsy.core.BugsyConfig
import com.bugsy.core.Trigger

class SampleApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Bugsy.initialize(BugsyConfig(trigger = Trigger.FloatingButton))
    }
}
