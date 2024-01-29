package com.android.settings.spa.app.specialaccess

import android.content.Context
import android.platform.test.annotations.RequiresFlagsDisabled
import android.platform.test.annotations.RequiresFlagsEnabled
import android.platform.test.flag.junit.CheckFlagsRule
import android.platform.test.flag.junit.DeviceFlagsValueProvider
import androidx.preference.Preference
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4

import com.android.settings.flags.Flags
import com.google.common.truth.Truth.assertThat

import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.spy
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class VoiceActivationAppsPreferenceControllerTest {

    @get:Rule
    val checkFlagsRule: CheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule()

    private val context: Context = spy(ApplicationProvider.getApplicationContext()) {
        doNothing().whenever(mock).startActivity(any())
    }

    private val matchedPreference = Preference(context).apply { key = preferenceKey }

    private val misMatchedPreference = Preference(context).apply { key = testPreferenceKey }

    private val controller = VoiceActivationAppsPreferenceController(context, preferenceKey)

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_VOICE_ACTIVATION_APPS_IN_SETTINGS)
    fun getAvailabilityStatus_enableVoiceActivationApps_returnAvailable() {
        assertThat(controller.isAvailable).isTrue()
    }

    @Test
    @RequiresFlagsDisabled(Flags.FLAG_ENABLE_VOICE_ACTIVATION_APPS_IN_SETTINGS)
    fun getAvailableStatus_disableVoiceActivationApps_returnConditionallyUnavailable() {
        assertThat(controller.isAvailable).isFalse()
    }

    @Test
    fun handlePreferenceTreeClick_keyMatched_returnTrue() {
        assertThat(controller.handlePreferenceTreeClick(matchedPreference)).isTrue()
    }

    @Test
    fun handlePreferenceTreeClick_keyMisMatched_returnFalse() {
        assertThat(controller.handlePreferenceTreeClick(misMatchedPreference)).isFalse()
    }

    companion object {
        private const val preferenceKey: String = "voice_activation_apps"
        private const val testPreferenceKey: String = "test_key"
    }
}