package com.android.settings.spa.app.specialaccess

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VoiceActivationAppsTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    private val listModel = VoiceActivationAppsListModel(context)

    @Test
    fun pageTitleResId() {
        assertThat(listModel.pageTitleResId).isEqualTo(R.string.voice_activation_apps_title)
    }

    @Test
    fun switchTitleResId() {
        assertThat(listModel.switchTitleResId).isEqualTo(R.string.permit_voice_activation_apps)
    }

    @Test
    fun footerResId() {
        assertThat(listModel.footerResId)
            .isEqualTo(R.string.allow_voice_activation_apps_description)
    }

    @Test
    fun appOp() {
        assertThat(listModel.appOp).isEqualTo(AppOpsManager.OP_RECEIVE_SANDBOX_TRIGGER_AUDIO)
    }

    @Test
    fun permission() {
        assertThat(listModel.permission).isEqualTo(
            Manifest.permission.RECEIVE_SANDBOX_TRIGGER_AUDIO)
    }

    @Test
    fun setModeByUid() {
        assertThat(listModel.setModeByUid).isTrue()
    }
}