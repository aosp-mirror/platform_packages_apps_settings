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

package com.android.settings.development.mediadrm;

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
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
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
public class ForceSwSecureCryptoFallbackPreferenceControllerTest {

    private static final String PREF_KEY = "force_swcrypto_fallback";
    private static final UUID WIDEVINE_UUID =
        new UUID(0xEDEF8BA979D64ACEL, 0xA3C827DCD51D21EDL);
    private static final String TAG = "ForceSwSecureCryptoFallbackPreferenceControllerTest";

    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private Context mContext;
    private ForceSwSecureCryptoFallbackPreferenceController mController;
    private SwitchPreference mPreference;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mController = new ForceSwSecureCryptoFallbackPreferenceController(mContext, PREF_KEY);
        mPreference = new SwitchPreference(mContext);
        WidevineProperties.forcel3_enabled(false);
    }

    @Test
    @EnableFlags(Flags.FLAG_FORCE_L3_ENABLED)
    public void updateState_flagEnabled_checkPreference() {
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
    }

    @Test
    @DisableFlags(Flags.FLAG_FORCE_L3_ENABLED)
    public void updateState_flagDisabled_checkPreference() {
        mController.updateState(mPreference);
        assertThat(mPreference.isEnabled()).isFalse();
    }

    @Test
    @EnableFlags(Flags.FLAG_FORCE_L3_ENABLED)
    public void updateState_checkWidevine() throws Exception {
        try (MediaDrm drm = new MediaDrm(WIDEVINE_UUID)) {
            assumeTrue(drm.getPropertyString("securityLevel").equals("L1"));
            mController.updateState(mPreference);
            assertThat(mPreference.isEnabled()).isTrue();
        } catch (UnsupportedSchemeException ex) {
            assumeNoException(ex);
        }

        // L3 enforced
        mController.setChecked(true);
        mController.updateState(mPreference);
        assertThat(mPreference.isEnabled()).isTrue();
        assertThat(mPreference.isChecked()).isTrue();
        try (MediaDrm drm = new MediaDrm(WIDEVINE_UUID)) {
            assertThat(drm.getPropertyString("securityLevel")).isEqualTo("L3");
        } catch (UnsupportedSchemeException ex) {
            assumeNoException(ex);
        }

        // Switch back to L1
        mController.setChecked(false);
        mController.updateState(mPreference);

        try (MediaDrm drm = new MediaDrm(WIDEVINE_UUID)) {
            assertThat(drm.getPropertyString("securityLevel")).isEqualTo("L1");
        } catch (UnsupportedSchemeException ex) {
            assumeNoException(ex);
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_FORCE_L3_ENABLED)
    public void updateState_checkWhenWidevineReady() throws Exception {
        try (MediaDrm drm = new MediaDrm(WIDEVINE_UUID)) {
            if (drm.getPropertyString("securityLevel").equals("L1")) {
                String version = drm.getPropertyString(MediaDrm.PROPERTY_VERSION);
                mController.updateState(mPreference);
                if (Integer.parseInt(version.split("\\.", 2)[0]) >= 19) {
                    assertThat(mPreference.isEnabled()).isTrue();
                } else {
                    assertThat(mPreference.isEnabled()).isFalse();
                }
            }
        } catch (UnsupportedSchemeException ex) {
            assumeNoException(ex);
        }
    }
}
