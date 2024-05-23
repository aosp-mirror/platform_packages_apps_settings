/*
* Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.development.widevine;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assume.assumeTrue;
import static org.junit.Assume.assumeNoException;

import android.content.Context;
import android.media.MediaDrm;
import android.media.UnsupportedSchemeException;
import android.media.NotProvisionedException;
import android.sysprop.WidevineProperties;
import android.util.Log;
import android.content.Context;

import com.android.settings.media_drm.Flags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.preference.SwitchPreference;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.UUID;

@RunWith(AndroidJUnit4.class)
public class ForceL3FallbackPreferenceControllerTest {

    private static final String PREF_KEY = "force_l3_fallback";
    private static final UUID WIDEVINE_UUID =
        new UUID(0xEDEF8BA979D64ACEL, 0xA3C827DCD51D21EDL);
    private static final String TAG = "ForceL3FallbackPreferenceControllerTest";

    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private Context mContext;
    private ForceL3FallbackPreferenceController mController;
    private SwitchPreference mPreference;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mController = new ForceL3FallbackPreferenceController(mContext, PREF_KEY);
        mPreference = new SwitchPreference(mContext);
        WidevineProperties.forcel3_enabled(false);
    }

    @Test
    public void updateState_flagEnabled_checkPreference() {
        mSetFlagsRule.enableFlags(Flags.FLAG_FORCE_L3_ENABLED);
        mController.updateState(mPreference);
        assertThat(mPreference.isEnabled()).isTrue();
        assertThat(mPreference.isChecked()).isFalse();
        assertThat(WidevineProperties.forcel3_enabled().orElse(false)).isFalse();

        // Toggle to true
        mController.setChecked(true);
        mController.updateState(mPreference);
        assertThat(WidevineProperties.forcel3_enabled().orElse(false)).isTrue();
        assertThat(mPreference.isEnabled()).isTrue();
        assertThat(mPreference.isChecked()).isTrue();

        // Toggle to false
        mController.setChecked(false);
        mController.updateState(mPreference);
        assertThat(WidevineProperties.forcel3_enabled().orElse(false)).isFalse();
        assertThat(mPreference.isEnabled()).isTrue();
        assertThat(mPreference.isChecked()).isFalse();

        // Test flag rollback
        mController.setChecked(true);
        mController.updateState(mPreference);
        assertThat(mPreference.isChecked()).isTrue();
        assertThat(WidevineProperties.forcel3_enabled().orElse(false)).isTrue();
        mSetFlagsRule.disableFlags(Flags.FLAG_FORCE_L3_ENABLED);
        mController.updateState(mPreference);
        assertThat(mPreference.isEnabled()).isFalse();
        assertThat(mPreference.isChecked()).isFalse();
        assertThat(WidevineProperties.forcel3_enabled().orElse(false)).isFalse();
    }

    @Test
    public void updateState_flagDisabled_checkPreference() {
        mSetFlagsRule.disableFlags(Flags.FLAG_FORCE_L3_ENABLED);
        mController.updateState(mPreference);
        assertThat(mPreference.isEnabled()).isFalse();
    }

    @Test
    public void updateState_checkWidevine() throws Exception {
        MediaDrm drm;
        try {
            drm = new MediaDrm(WIDEVINE_UUID);
            assumeTrue(drm.getPropertyString("securityLevel").equals("L1"));
            mSetFlagsRule.enableFlags(Flags.FLAG_FORCE_L3_ENABLED);
            drm.close();
        } catch (UnsupportedSchemeException ex) {
            assumeNoException(ex);
        }

        // L3 enforced
        mController.setChecked(true);
        mController.updateState(mPreference);
        assertThat(WidevineProperties.forcel3_enabled().orElse(false)).isTrue();
        assertThat(mPreference.isEnabled()).isTrue();
        assertThat(mPreference.isChecked()).isTrue();
        drm = new MediaDrm(WIDEVINE_UUID);
        assertThat(drm.getPropertyString("securityLevel")).isEqualTo("L3");

        // Switch back to L1
        mController.setChecked(false);
        mController.updateState(mPreference);
        drm.close();
        drm = new MediaDrm(WIDEVINE_UUID);
        assertThat(drm.getPropertyString("securityLevel")).isEqualTo("L1");
    }
}
