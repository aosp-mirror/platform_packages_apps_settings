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
    fun modelResourceIdAndProperties() {
        assertThat(listModel.pageTitleResId).isEqualTo(R.string.voice_activation_apps_title)
        assertThat(listModel.switchTitleResId).isEqualTo(R.string.permit_voice_activation_apps)
        assertThat(listModel.footerResId).isEqualTo(R.string.allow_voice_activation_apps_description)
        assertThat(listModel.appOp).isEqualTo(AppOpsManager.OP_RECEIVE_SANDBOX_TRIGGER_AUDIO)
        assertThat(listModel.permission).isEqualTo(
            Manifest.permission.RECEIVE_SANDBOX_TRIGGER_AUDIO
        )
        assertThat(listModel.setModeByUid).isTrue()
    }
}