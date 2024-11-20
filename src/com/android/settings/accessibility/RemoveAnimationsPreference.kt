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

package com.android.settings.accessibility

import android.annotation.DrawableRes
import android.content.Context
import android.provider.Settings
import com.android.settings.R
import com.android.settingslib.datastore.HandlerExecutor
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.KeyedObserver
import com.android.settingslib.datastore.NoOpKeyedObservable
import com.android.settingslib.datastore.SettingsGlobalStore
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.SwitchPreference
import com.android.settingslib.preference.SwitchPreferenceBinding

class RemoveAnimationsPreference :
    SwitchPreference(
        KEY,
        R.string.accessibility_disable_animations,
        R.string.accessibility_disable_animations_summary
    ),
    SwitchPreferenceBinding,
    PreferenceLifecycleProvider {

    private var mSettingsKeyedObserver: KeyedObserver<String?>? = null

    override val icon: Int
        @DrawableRes get() = R.drawable.ic_accessibility_animation

    override fun onStart(context: PreferenceLifecycleContext) {
        mSettingsKeyedObserver = object : KeyedObserver<String?> {
            override fun onKeyChanged(key: String?, reason: Int) {
                context.notifyPreferenceChange(KEY)
            }
        }
        mSettingsKeyedObserver?.let {
            for (key in TOGGLE_ANIMATION_KEYS) {
                SettingsGlobalStore.get(context).addObserver(key, it, HandlerExecutor.main)
            }
        }
    }

    override fun onStop(context: PreferenceLifecycleContext) {
        mSettingsKeyedObserver?.let {
            SettingsGlobalStore.get(context).removeObserver(it)
            mSettingsKeyedObserver = null
        }
    }

    override fun storage(context: Context): KeyValueStore = RemoveAnimationsStorage(context)

    private class RemoveAnimationsStorage(private val context: Context) :
        NoOpKeyedObservable<String>(), KeyValueStore {
        override fun contains(key: String) = key == KEY

        override fun <T : Any> getValue(key: String, valueType: Class<T>) =
            when {
                key == KEY && valueType == Boolean::class.javaObjectType ->
                    !isAnimationEnabled(context) as T

                else -> null
            }

        override fun <T : Any> setValue(key: String, valueType: Class<T>, value: T?) {
            if (key == KEY && value is Boolean) {
                setAnimationEnabled(context, !value)
            }
        }
    }

    companion object {
        // This KEY must match the key used in accessibility_color_and_motion.xml for this
        // preference, at least until the entire screen is migrated to Catalyst and that XML
        // is deleted. Use any key from the set of 3 toggle animation keys.
        const val KEY = Settings.Global.ANIMATOR_DURATION_SCALE

        const val ANIMATION_ON_VALUE: Float = 1.0f
        const val ANIMATION_OFF_VALUE: Float = 0.0f

        val TOGGLE_ANIMATION_KEYS: List<String> = listOf(
            Settings.Global.WINDOW_ANIMATION_SCALE, Settings.Global.TRANSITION_ANIMATION_SCALE,
            Settings.Global.ANIMATOR_DURATION_SCALE
        )

        fun isAnimationEnabled(context: Context): Boolean {
            // This pref treats animation as enabled if *any* of the animation types are enabled.
            for (animationSetting in TOGGLE_ANIMATION_KEYS) {
                val animationValue: Float? =
                    SettingsGlobalStore.get(context).getFloat(animationSetting)
                // Animation is enabled by default, so treat null as enabled.
                if (animationValue == null || animationValue > ANIMATION_OFF_VALUE) {
                    return true
                }
            }
            return false
        }

        fun setAnimationEnabled(context: Context, enabled: Boolean) {
            val value = if (enabled) ANIMATION_ON_VALUE else ANIMATION_OFF_VALUE;
            for (animationSetting in TOGGLE_ANIMATION_KEYS) {
                SettingsGlobalStore.get(context).setFloat(animationSetting, value)
            }
        }
    }
}