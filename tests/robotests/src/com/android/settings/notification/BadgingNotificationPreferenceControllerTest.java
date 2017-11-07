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

import static android.provider.Settings.Secure.NOTIFICATION_BADGING;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.TwoStatePreference;

import com.android.settings.TestConfig;
import com.android.settings.search.InlinePayload;
import com.android.settings.search.InlineSwitchPayload;
import com.android.settings.testutils.shadow.ShadowSecureSettings;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION_O)
public class BadgingNotificationPreferenceControllerTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private PreferenceScreen mScreen;

    private BadgingNotificationPreferenceController mController;
    private Preference mPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mController = new BadgingNotificationPreferenceController(mContext);
        mPreference = new Preference(RuntimeEnvironment.application);
        mPreference.setKey(mController.getPreferenceKey());
        when(mScreen.findPreference(mPreference.getKey())).thenReturn(mPreference);
    }

    @Test
    public void display_configIsTrue_shouldDisplay() {
        when(mContext.getResources().
                getBoolean(com.android.internal.R.bool.config_notificationBadging))
                .thenReturn(true);
        mController.displayPreference(mScreen);

        assertThat(mPreference.isVisible()).isTrue();
    }

    @Test
    public void display_configIsFalse_shouldNotDisplay() {
        when(mContext.getResources().
                getBoolean(com.android.internal.R.bool.config_notificationBadging))
                .thenReturn(false);

        mController.displayPreference(mScreen);

        assertThat(mPreference.isVisible()).isFalse();
    }

    @Test
    public void updateState_preferenceSetCheckedWhenSettingIsOn() {
        final TwoStatePreference preference = mock(TwoStatePreference.class);
        final Context context = ShadowApplication.getInstance().getApplicationContext();
        Settings.Secure.putInt(context.getContentResolver(), NOTIFICATION_BADGING, 1);

        mController = new BadgingNotificationPreferenceController(context);
        mController.updateState(preference);

        verify(preference).setChecked(true);
    }

    @Test
    public void updateState_preferenceSetUncheckedWhenSettingIsOff() {
        final TwoStatePreference preference = mock(TwoStatePreference.class);
        final Context context = ShadowApplication.getInstance().getApplicationContext();
        Settings.Secure.putInt(context.getContentResolver(), NOTIFICATION_BADGING, 0);

        mController = new BadgingNotificationPreferenceController(context);
        mController.updateState(preference);

        verify(preference).setChecked(false);
    }

    @Test
    public void testPreferenceController_ProperResultPayloadType() {
        assertThat(mController.getResultPayload()).isInstanceOf(InlineSwitchPayload.class);
    }

    @Test
    @Config(shadows = ShadowSecureSettings.class)
    public void testSetValue_updatesCorrectly() {
        int newValue = 0;
        ContentResolver resolver = mContext.getContentResolver();
        Settings.Secure.putInt(resolver, Settings.Secure.NOTIFICATION_BADGING, 1);

        ((InlinePayload) mController.getResultPayload()).setValue(mContext, newValue);
        int updatedValue = Settings.Secure.getInt(resolver, Settings.Secure.NOTIFICATION_BADGING,
                1);

        assertThat(updatedValue).isEqualTo(newValue);
    }

    @Test
    @Config(shadows = ShadowSecureSettings.class)
    public void testGetValue_correctValueReturned() {
        int currentValue = 1;
        ContentResolver resolver = mContext.getContentResolver();
        Settings.Secure.putInt(resolver, Settings.Secure.NOTIFICATION_BADGING, currentValue);

        int newValue = ((InlinePayload) mController.getResultPayload()).getValue(mContext);

        assertThat(newValue).isEqualTo(currentValue);
    }
}
