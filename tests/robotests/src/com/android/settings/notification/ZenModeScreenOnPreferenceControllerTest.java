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

package com.android.settings.notification;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.NotificationManager;
import android.content.Context;
import android.support.v14.preference.SwitchPreference;

import com.android.settings.TestConfig;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.util.ReflectionHelpers;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class ZenModeScreenOnPreferenceControllerTest {
    private ZenModeScreenOnPreferenceController mController;

    @Mock
    private ZenModeBackend mBackend;
    @Mock
    private NotificationManager mNotificationManager;
    @Mock
    private SwitchPreference mockPref;
    @Mock
    private NotificationManager.Policy mPolicy;

    private Context mContext;
    private final boolean MOCK_PRIORITY_SCREEN_ON_SETTING = false;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        ShadowApplication shadowApplication = ShadowApplication.getInstance();
        shadowApplication.setSystemService(Context.NOTIFICATION_SERVICE, mNotificationManager);

        mContext = shadowApplication.getApplicationContext();
        when(mNotificationManager.getNotificationPolicy()).thenReturn(mPolicy);

        mController = new ZenModeScreenOnPreferenceController(mContext, mock(Lifecycle.class));
        ReflectionHelpers.setField(mController, "mBackend", mBackend);
    }

    @Test
    public void updateState() {
        final SwitchPreference mockPref = mock(SwitchPreference.class);
        when(mBackend.isEffectAllowed(NotificationManager.Policy.SUPPRESSED_EFFECT_SCREEN_ON))
                .thenReturn(MOCK_PRIORITY_SCREEN_ON_SETTING);
        mController.updateState(mockPref);

        verify(mockPref).setChecked(MOCK_PRIORITY_SCREEN_ON_SETTING);
    }

    @Test
    public void onPreferenceChanged_EnableScreenOn() {
        boolean allow = true;
        mController.onPreferenceChange(mockPref, allow);

        verify(mBackend).saveVisualEffectsPolicy(
                NotificationManager.Policy.SUPPRESSED_EFFECT_SCREEN_ON, allow);
    }

    @Test
    public void onPreferenceChanged_DisableScreenOn() {
        boolean allow = false;
        mController.onPreferenceChange(mockPref, allow);

        verify(mBackend).saveVisualEffectsPolicy(
                NotificationManager.Policy.SUPPRESSED_EFFECT_SCREEN_ON, allow);
    }
}