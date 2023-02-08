package godau.fynn.dsbdirect.util

import android.app.Application
import com.google.android.material.color.DynamicColors

class PavoApplication: Application() {
    override fun onCreate() {
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}