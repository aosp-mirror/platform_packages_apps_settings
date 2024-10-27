/*
 * Copyright (C) 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.display.darkmode

import android.app.UiModeManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.PowerManager
import androidx.preference.Preference
import com.android.settings.R
import com.android.settings.flags.Flags
import com.android.settingslib.PrimarySwitchPreference
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.NoOpKeyedObservable
import com.android.settingslib.metadata.BooleanValue
import com.android.settingslib.metadata.PersistentPreference
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import com.android.settingslib.preference.PreferenceScreenBinding
import com.android.settingslib.preference.PreferenceScreenCreator
import java.util.WeakHashMap

// LINT.IfChange
@ProvidePreferenceScreen
class DarkModeScreen :
    PreferenceScreenCreator,
    PreferenceScreenBinding,
    PersistentPreference<Boolean>,
    BooleanValue,
    PreferenceSummaryProvider,
    PreferenceLifecycleProvider {

    /**
     * States for different screens.
     *
     * The "Dark mode" appears in several screens. And in Android split-screen mode, more than one
     * "Dark mode" settings could be displayed at the same time. As [PreferenceScreenCreator] works
     * like singleton, we need to register different broadcast receivers for different screens.
     */
    private val fragmentStates = WeakHashMap<PreferenceLifecycleContext, FragmentState>()

    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.dark_ui_mode

    override val keywords: Int
        get() = R.string.keywords_dark_ui_mode

    override fun isFlagEnabled(context: Context) = Flags.catalystDarkUiMode()

    override fun fragmentClass() = DarkModeSettingsFragment::class.java

    override fun hasCompleteHierarchy() = false

    override fun getPreferenceHierarchy(context: Context) = preferenceHierarchy(this) {}

    override fun storage(context: Context): KeyValueStore = DarkModeStorage(context)

    override fun createWidget(context: Context) = PrimarySwitchPreference(context)

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        if (preference is DarkModePreference) preference.setCatalystEnabled(true)
        val context = preference.context
        val primarySwitchPreference = preference as PrimarySwitchPreference
        primarySwitchPreference.isSwitchEnabled = !context.isPowerSaveMode()
        primarySwitchPreference.isChecked = context.isDarkMode()
    }

    override fun isEnabled(context: Context) = !context.isPowerSaveMode()

    override fun getSummary(context: Context): CharSequence? {
        val active = context.isDarkMode()
        return when {
            !context.isPowerSaveMode() -> AutoDarkTheme.getStatus(context, active)
            active -> context.getString(R.string.dark_ui_mode_disabled_summary_dark_theme_on)
            else -> context.getString(R.string.dark_ui_mode_disabled_summary_dark_theme_off)
        }
    }

    override fun onStart(context: PreferenceLifecycleContext) {
        val broadcastReceiver =
            object : BroadcastReceiver() {
                override fun onReceive(receiverContext: Context, intent: Intent) {
                    context.notifyPreferenceChange(this@DarkModeScreen)
                }
            }
        context.registerReceiver(
            broadcastReceiver,
            IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED),
        )

        val darkModeObserver = DarkModeObserver(context)
        darkModeObserver.subscribe { context.notifyPreferenceChange(this@DarkModeScreen) }

        fragmentStates[context] = FragmentState(broadcastReceiver, darkModeObserver)
    }

    override fun onStop(context: PreferenceLifecycleContext) {
        fragmentStates.remove(context)?.run {
            context.unregisterReceiver(broadcastReceiver)
            darkModeObserver.unsubscribe()
        }
    }

    private class FragmentState(
        val broadcastReceiver: BroadcastReceiver,
        val darkModeObserver: DarkModeObserver,
    )

    /**
     * Abstract storage for dark mode settings.
     *
     * The underlying storage is manipulated by [UiModeManager] but we do not need to worry about
     * the details. Additionally, the observer is for UI purpose only right now, so use
     * [NoOpKeyedObservable].
     */
    @Suppress("UNCHECKED_CAST")
    private class DarkModeStorage(private val context: Context) :
        NoOpKeyedObservable<String>(), KeyValueStore {

        override fun contains(key: String) = key == KEY

        override fun <T : Any> getValue(key: String, valueType: Class<T>) =
            when {
                key == KEY && valueType == Boolean::class.javaObjectType ->
                    context.isDarkMode() as T
                else -> null
            }

        override fun <T : Any> setValue(key: String, valueType: Class<T>, value: T?) {
            if (key == KEY && value is Boolean) {
                context.getSystemService(UiModeManager::class.java)?.setNightModeActivated(value)
            }
        }
    }

    companion object {
        const val KEY = "dark_ui_mode"

        private fun Context.isPowerSaveMode() =
            getSystemService(PowerManager::class.java)?.isPowerSaveMode == true

        private fun Context.isDarkMode() =
            (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_YES) != 0
    }
}
// LINT.ThenChange(../DarkUIPreferenceController.java)
