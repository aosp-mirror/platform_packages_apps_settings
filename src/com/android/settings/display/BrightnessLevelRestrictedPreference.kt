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
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import android.provider.Settings.System
import androidx.preference.Preference
import com.android.settings.R
import com.android.settings.Utils
import com.android.settings.core.SettingsBaseActivity
import com.android.settingslib.RestrictedLockUtilsInternal
import com.android.settingslib.RestrictedPreference
import com.android.settingslib.datastore.HandlerExecutor
import com.android.settingslib.datastore.KeyedObserver
import com.android.settingslib.datastore.SettingsSystemStore
import com.android.settingslib.display.BrightnessUtils.GAMMA_SPACE_MAX
import com.android.settingslib.display.BrightnessUtils.GAMMA_SPACE_MIN
import com.android.settingslib.display.BrightnessUtils.convertLinearToGammaFloat
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceRestrictionProvider
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.preference.PreferenceBinding
import com.android.settingslib.transition.SettingsTransitionHelper
import java.text.NumberFormat

// LINT.IfChange
class BrightnessLevelRestrictedPreference :
    PreferenceMetadata,
    PreferenceBinding,
    PreferenceRestrictionProvider,
    PreferenceSummaryProvider,
    PreferenceLifecycleProvider,
    Preference.OnPreferenceClickListener {

    private var brightnessObserver: KeyedObserver<String>? = null
    private var displayListener: DisplayListener? = null

    override val key: String
        get() = "brightness"

    override val title: Int
        get() = R.string.brightness

    override val keywords: Int
        get() = R.string.keywords_display_brightness_level

    override fun getSummary(context: Context) =
        NumberFormat.getPercentInstance().format(getCurrentBrightness(context))

    override fun isEnabled(context: Context) =
        !UserManager.get(context)
            .hasBaseUserRestriction(UserManager.DISALLOW_CONFIG_BRIGHTNESS, Process.myUserHandle())

    override fun isRestricted(context: Context) =
        RestrictedLockUtilsInternal.checkIfRestrictionEnforced(
            context,
            UserManager.DISALLOW_CONFIG_BRIGHTNESS,
            UserHandle.myUserId(),
        ) != null

    override fun createWidget(context: Context) = RestrictedPreference(context)

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        if (preference is RestrictedPreference) preference.useAdminDisabledSummary(true)
        preference.onPreferenceClickListener = this
    }

    override fun onStart(context: PreferenceLifecycleContext) {
        val observer =
            object : KeyedObserver<String> {
                override fun onKeyChanged(key: String, reason: Int) {
                    context.notifyPreferenceChange(this@BrightnessLevelRestrictedPreference)
                }
            }
        brightnessObserver = observer
        SettingsSystemStore.get(context)
            .addObserver(System.SCREEN_AUTO_BRIGHTNESS_ADJ, observer, HandlerExecutor.main)

        val listener =
            object : DisplayListener {
                override fun onDisplayAdded(displayId: Int) {}

                override fun onDisplayRemoved(displayId: Int) {}

                override fun onDisplayChanged(displayId: Int) {
                    context.notifyPreferenceChange(this@BrightnessLevelRestrictedPreference)
                }
            }
        displayListener = listener
        context
            .getSystemService(DisplayManager::class.java)
            .registerDisplayListener(
                listener,
                HandlerExecutor.main,
                DisplayManager.EVENT_FLAG_DISPLAY_BRIGHTNESS,
            )
    }

    override fun onStop(context: PreferenceLifecycleContext) {
        brightnessObserver?.let {
            SettingsSystemStore.get(context).removeObserver(System.SCREEN_AUTO_BRIGHTNESS_ADJ, it)
            brightnessObserver = null
        }

        displayListener?.let {
            context.getSystemService(DisplayManager::class.java).unregisterDisplayListener(it)
            displayListener = null
        }
    }

    override fun onPreferenceClick(preference: Preference): Boolean {
        val context = preference.context
        val intent =
            Intent(ACTION_SHOW_BRIGHTNESS_DIALOG)
                .setPackage(Utils.SYSTEMUI_PACKAGE_NAME)
                .putExtra(
                    SettingsBaseActivity.EXTRA_PAGE_TRANSITION_TYPE,
                    SettingsTransitionHelper.TransitionType.TRANSITION_NONE,
                )
                .putExtra(EXTRA_BRIGHTNESS_DIALOG_IS_FULL_WIDTH, true)
        val options =
            ActivityOptions.makeCustomAnimation(
                context,
                android.R.anim.fade_in,
                android.R.anim.fade_out,
            )
        context.startActivityForResult(preference.key, intent, 0, options.toBundle())
        return true
    }

    private fun getCurrentBrightness(context: Context): Double {
        val info: BrightnessInfo? = context.display.brightnessInfo
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
// LINT.ThenChange(BrightnessLevelPreferenceController.java)
