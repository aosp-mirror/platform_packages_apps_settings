/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.network;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.res.Resources;
import android.provider.Settings;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class AdaptiveConnectivityPreferenceControllerTest {

    private static final String PREF_KEY = "adaptive_connectivity";

    private Context mContext;
    @Mock private Resources mResources;
    private AdaptiveConnectivityPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        doReturn(mResources).when(mContext).getResources();
        mController = new AdaptiveConnectivityPreferenceController(mContext, PREF_KEY);
    }

    @Test
    public void isAvailable_supportAdaptiveConnectivity_shouldReturnTrue() {
        when(mResources.getBoolean(R.bool.config_show_adaptive_connectivity))
                .thenReturn(true);

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_notSupportAdaptiveConnectivity_shouldReturnFalse() {
        when(mResources.getBoolean(R.bool.config_show_adaptive_connectivity))
                .thenReturn(false);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void getSummary_adaptiveConnectivityEnabled_shouldShowOn() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ADAPTIVE_CONNECTIVITY_ENABLED, 1);

        assertThat(mController.getSummary()).isEqualTo(mContext.getString(R.string.switch_on_text));
    }

    @Test
    public void getSummary_adaptiveConnectivityEnabled_shouldShowOff() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ADAPTIVE_CONNECTIVITY_ENABLED, 0);

        assertThat(mController.getSummary())
                .isEqualTo(mContext.getString(R.string.switch_off_text));
    }
}
