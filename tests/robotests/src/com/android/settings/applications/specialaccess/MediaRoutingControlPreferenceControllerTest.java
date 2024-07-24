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

package com.android.settings.applications.specialaccess;

import static com.android.settingslib.spa.framework.util.SpaIntentKt.KEY_DESTINATION;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.platform.test.annotations.RequiresFlagsDisabled;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;

import androidx.preference.Preference;

import com.android.media.flags.Flags;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.spa.SpaActivity;
import com.android.settings.spa.app.specialaccess.MediaRoutingControlAppListProvider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class MediaRoutingControlPreferenceControllerTest {

    private static final String PREFERENCE_KEY = "test_preference_key";
    private static final String DIFFERENT_PREFERENCE_KEY = "other_preference_key";

    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Mock
    private Context mContext;
    @Mock
    private PackageManager mPackageManager;

    private MediaRoutingControlPreferenceController mTestController;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        mTestController = new MediaRoutingControlPreferenceController(
                mContext, PREFERENCE_KEY);
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_PRIVILEGED_ROUTING_FOR_MEDIA_ROUTING_CONTROL)
    public void getAvailabilityStatus_withFlagEnabled_shouldReturnTrue() {
        assertThat(mTestController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.AVAILABLE);
    }

    @Test
    @RequiresFlagsDisabled(Flags.FLAG_ENABLE_PRIVILEGED_ROUTING_FOR_MEDIA_ROUTING_CONTROL)
    public void getAvailabilityStatus_withFlagDisabled_shouldReturnFalse() {
        assertThat(mTestController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void handlePreferenceTreeClick_withDifferentPreference_shouldReturnFalse() {
        Preference preference = mock(Preference.class);
        when(preference.getKey()).thenReturn(DIFFERENT_PREFERENCE_KEY);

        assertThat(mTestController.handlePreferenceTreeClick(preference)).isFalse();
    }

    @Test
    public void handlePreferenceTreeClick_withMediaRoutingPreference_shouldReturnTrue() {
        Preference preference = mock(Preference.class);
        when(preference.getKey()).thenReturn(PREFERENCE_KEY);

        assertThat(mTestController.handlePreferenceTreeClick(preference)).isTrue();
    }

    @Test
    public void handlePreferenceTreeClick_withDifferentPreference_shouldNotStartSpaActivity() {
        Preference preference = mock(Preference.class);
        when(preference.getKey()).thenReturn(DIFFERENT_PREFERENCE_KEY);

        mTestController.handlePreferenceTreeClick(preference);

        verify(mContext, never()).startActivity(any(Intent.class));
    }

    @Test
    public void handlePreferenceTreeClick_withMediaRoutingPreference_shouldStartSpaActivity() {
        Preference preference = mock(Preference.class);
        when(preference.getKey()).thenReturn(PREFERENCE_KEY);

        mTestController.handlePreferenceTreeClick(preference);

        ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(mContext).startActivity(intentCaptor.capture());
        final Intent intent = intentCaptor.getValue();
        assertThat(intent.getComponent().getClassName()).isEqualTo(SpaActivity.class.getName());
        assertThat(intent.getStringExtra(KEY_DESTINATION)).isEqualTo(
                MediaRoutingControlAppListProvider.INSTANCE.getAppListRoute());
    }
}
