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
package com.android.settings.display

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_SHOW_BRIGHTNESS_DIALOG
import android.content.Intent.EXTRA_BRIGHTNESS_DIALOG_IS_FULL_WIDTH
import android.hardware.display.BrightnessInfo
import android.hardware.display.DisplayManager
import android.hardware.display.DisplayManager.DisplayListener
import android.os.UserManager
import android.provider.Settings.System
import androidx.preference.Preference
import com.android.settings.PreferenceRestrictionMixin
import com.android.settings.R
import com.android.settings.Utils
import com.android.settings.core.SettingsBaseActivity
import com.android.settingslib.RestrictedPreference
import com.android.settingslib.datastore.AbstractKeyedDataObservable
import com.android.settingslib.datastore.DataChangeReason
import com.android.settingslib.datastore.HandlerExecutor
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.KeyedObserver
import com.android.settingslib.datastore.SettingsSystemStore
import com.android.settingslib.display.BrightnessUtils.GAMMA_SPACE_MAX
import com.android.settingslib.display.BrightnessUtils.GAMMA_SPACE_MIN
import com.android.settingslib.display.BrightnessUtils.convertLinearToGammaFloat
import com.android.settingslib.metadata.FloatPersistentPreference
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.preference.PreferenceBinding
import com.android.settingslib.transition.SettingsTransitionHelper
import java.text.NumberFormat

// LINT.IfChange
class BrightnessLevelPreference :
    PreferenceMetadata,
    FloatPersistentPreference,
    PreferenceBinding,
    PreferenceRestrictionMixin,
    PreferenceSummaryProvider,
    Preference.OnPreferenceClickListener {

    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.brightness

    override val keywords: Int
        get() = R.string.keywords_display_brightness_level

    override fun getSummary(context: Context): CharSequence? =
        NumberFormat.getPercentInstance().format(context.brightness)

    override fun isEnabled(context: Context) = super<PreferenceRestrictionMixin>.isEnabled(context)

    override val restrictionKeys
        get() = arrayOf(UserManager.DISALLOW_CONFIG_BRIGHTNESS)

    override val useAdminDisabledSummary: Boolean
        get() = true

    override fun intent(context: Context): Intent? =
        Intent(ACTION_SHOW_BRIGHTNESS_DIALOG)
            .setPackage(Utils.SYSTEMUI_PACKAGE_NAME)
            .putExtra(
                SettingsBaseActivity.EXTRA_PAGE_TRANSITION_TYPE,
                SettingsTransitionHelper.TransitionType.TRANSITION_NONE,
            )
            .putExtra(EXTRA_BRIGHTNESS_DIALOG_IS_FULL_WIDTH, true)

    override fun createWidget(context: Context) = RestrictedPreference(context)

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        preference.onPreferenceClickListener = this
        preference.isPersistent = false
    }

    override fun getReadPermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override fun getWritePermit(context: Context, value: Float?, callingPid: Int, callingUid: Int) =
        ReadWritePermit.DISALLOW

    override val sensitivityLevel
        get() = SensitivityLevel.NO_SENSITIVITY

    override fun storage(context: Context): KeyValueStore = BrightnessStorage(context)

    private class BrightnessStorage(private val context: Context) :
        AbstractKeyedDataObservable<String>(),
        KeyValueStore,
        KeyedObserver<String>,
        DisplayListener {

        override fun contains(key: String) = key == KEY

        @Suppress("UNCHECKED_CAST")
        override fun <T : Any> getValue(key: String, valueType: Class<T>) =
            context.brightness.toFloat() as T

        override fun <T : Any> setValue(key: String, valueType: Class<T>, value: T?) {}

        override fun onFirstObserverAdded() {
            SettingsSystemStore.get(context)
                .addObserver(System.SCREEN_AUTO_BRIGHTNESS_ADJ, this, HandlerExecutor.main)

            context.displayManager.registerDisplayListener(
                this,
                HandlerExecutor.main,
                /* eventFlags= */ 0,
                DisplayManager.PRIVATE_EVENT_TYPE_DISPLAY_BRIGHTNESS,
            )
        }

        override fun onLastObserverRemoved() {
            SettingsSystemStore.get(context).removeObserver(System.SCREEN_AUTO_BRIGHTNESS_ADJ, this)

            context.displayManager.unregisterDisplayListener(this)
        }

        override fun onKeyChanged(key: String, reason: Int) {
            notifyChange(KEY, DataChangeReason.UPDATE)
        }

        override fun onDisplayAdded(displayId: Int) {}

        override fun onDisplayRemoved(displayId: Int) {}

        override fun onDisplayChanged(displayId: Int) {
            notifyChange(KEY, DataChangeReason.UPDATE)
        }
    }

    override fun onPreferenceClick(preference: Preference): Boolean {
        val context = preference.context
        val options =
            ActivityOptions.makeCustomAnimation(
                context,
                android.R.anim.fade_in,
                android.R.anim.fade_out,
            )
        context.startActivityForResult(preference.key, intent(context), 0, options.toBundle())
        return true
    }

    companion object {
        const val KEY = "brightness"

        private val Context.displayManager: DisplayManager
            get() = getSystemService(DisplayManager::class.java)!!

        private val Context.brightness: Double
            get() {
                val info: BrightnessInfo? = display.brightnessInfo
                val value =
                    info?.run {
                        convertLinearToGammaFloat(brightness, brightnessMinimum, brightnessMaximum)
                    }
                return getPercentage(value?.toDouble() ?: 0.0)
            }

        private fun getPercentage(value: Double): Double =
            when {
                value > GAMMA_SPACE_MAX -> 1.0
                value < GAMMA_SPACE_MIN -> 0.0
                else -> (value - GAMMA_SPACE_MIN) / (GAMMA_SPACE_MAX - GAMMA_SPACE_MIN)
            }
    }
}
// LINT.ThenChange(BrightnessLevelPreferenceController.java)
