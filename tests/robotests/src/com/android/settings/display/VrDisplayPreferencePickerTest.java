/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.display;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import android.content.ContentResolver;
import android.os.UserHandle;
import android.provider.Settings;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.testutils.shadow.ShadowSecureSettings;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class VrDisplayPreferencePickerTest {

    private VrDisplayPreferencePicker mPicker;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mPicker = spy(new VrDisplayPreferencePicker());
        doReturn(RuntimeEnvironment.application).when(mPicker).getContext();
    }

    @Test
    public void verifyMetricsConstant() {
        assertThat(mPicker.getMetricsCategory())
                .isEqualTo(MetricsProto.MetricsEvent.VR_DISPLAY_PREFERENCE);
    }

    @Test
    public void getCandidates_shouldReturnTwoCandidates() {
        List<VrDisplayPreferencePicker.VrCandidateInfo> candidates = mPicker.getCandidates();

        assertThat(candidates.size()).isEqualTo(2);
        assertThat(candidates.get(0).getKey())
                .isEqualTo(VrDisplayPreferencePicker.PREF_KEY_PREFIX + 0);
        assertThat(candidates.get(1).getKey())
                .isEqualTo(VrDisplayPreferencePicker.PREF_KEY_PREFIX + 1);
    }

    @Test
    @Config(shadows = ShadowSecureSettings.class)
    public void getKey_shouldGetFromSettingsProvider() {
        final ContentResolver cr = RuntimeEnvironment.application.getContentResolver();
        Settings.Secure.putIntForUser(cr, Settings.Secure.VR_DISPLAY_MODE, 1,
                UserHandle.myUserId());

        assertThat(mPicker.getDefaultKey())
                .isEqualTo(VrDisplayPreferencePicker.PREF_KEY_PREFIX + 1);
    }
}
