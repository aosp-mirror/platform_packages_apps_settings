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

package com.android.settings.notification;

import static com.android.settings.accessibility.AccessibilityUtil.State.OFF;
import static com.android.settings.accessibility.AccessibilityUtil.State.ON;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.platform.test.flag.junit.SetFlagsRule;
import android.provider.Settings;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.server.notification.Flags;
import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class PoliteNotificationFilterControllerTest {
    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private static final String PREFERENCE_KEY = "preference_key";
    private static final int POLITE_NOTIFICATIONS_ALL = 0;
    private static final int POLITE_NOTIFICATIONS_CONVERSATIONS = 1;
    private static final int POLITE_NOTIFICATIONS_DISABLED = 2;

    @Mock
    private PreferenceScreen mScreen;

    private PoliteNotificationFilterController mController;
    private Preference mPreference;
    private Context mContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);

        mController = new PoliteNotificationFilterController(mContext, PREFERENCE_KEY);
        mPreference = new Preference(mContext);
        mPreference.setKey(mController.getPreferenceKey());
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
    }

    @Test
    public void isAvailable_flagEnabled_shouldReturnTrue() {
        // TODO: b/291907312 - remove feature flags
        mSetFlagsRule.enableFlags(Flags.FLAG_POLITE_NOTIFICATIONS);
        assertThat(mController.isAvailable()).isTrue();
        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.AVAILABLE);
    }

    @Test
    public void isAvailable_flagDisabled_shouldReturnFalse() {
        // TODO: b/291907312 - remove feature flags
        mSetFlagsRule.disableFlags(Flags.FLAG_POLITE_NOTIFICATIONS);
        assertThat(mController.isAvailable()).isFalse();
        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void updateState_politeNotificationDisabled() {
        final ListPreference preference = mock(ListPreference.class);
        Settings.System.putInt(mContext.getContentResolver(),
                Settings.System.NOTIFICATION_COOLDOWN_ENABLED, OFF);
        mController.updateState(preference);

        verify(preference).setValue(Integer.toString(POLITE_NOTIFICATIONS_DISABLED));
        assertThat(mController.getSummary().toString()).isEqualTo(
                mContext.getString(R.string.notification_polite_disabled_summary));
    }

    @Test
    public void updateState_politeNotificationEnabled_applyAllApps() {
        final ListPreference preference = mock(ListPreference.class);
        Settings.System.putInt(mContext.getContentResolver(),
                Settings.System.NOTIFICATION_COOLDOWN_ENABLED, ON);
        Settings.System.putInt(mContext.getContentResolver(),
                Settings.System.NOTIFICATION_COOLDOWN_ALL, ON);
        mController.updateState(preference);

        verify(preference).setValue(Integer.toString(POLITE_NOTIFICATIONS_ALL));
        assertThat(mController.getSummary().toString()).isEqualTo(
                mContext.getString(R.string.notification_polite_all_apps_summary));
    }

    @Test
    public void updateState_politeNotificationEnabled_applyOnlyConversations() {
        final ListPreference preference = mock(ListPreference.class);
        Settings.System.putInt(mContext.getContentResolver(),
                Settings.System.NOTIFICATION_COOLDOWN_ENABLED, ON);
        Settings.System.putInt(mContext.getContentResolver(),
                Settings.System.NOTIFICATION_COOLDOWN_ALL, OFF);
        mController.updateState(preference);

        verify(preference).setValue(Integer.toString(POLITE_NOTIFICATIONS_CONVERSATIONS));
        assertThat(mController.getSummary().toString()).isEqualTo(
                mContext.getString(R.string.notification_polite_conversations_summary));
    }

    @Test
    public void onPreferenceChanged_firstItemSelected_shouldEnableForAll() {
        mController.displayPreference(mScreen);
        mController.onPreferenceChange(mPreference, "0");

        assertThat(Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.NOTIFICATION_COOLDOWN_ENABLED, OFF)).isEqualTo(ON);
        assertThat(Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.NOTIFICATION_COOLDOWN_ALL, OFF)).isEqualTo(ON);
    }

    @Test
    public void onPreferenceChanged_secondItemSelected_shouldEnableForConversationsOnly() {
        mController.displayPreference(mScreen);
        mController.onPreferenceChange(mPreference, "1");

        assertThat(Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.NOTIFICATION_COOLDOWN_ENABLED, OFF)).isEqualTo(ON);
        assertThat(Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.NOTIFICATION_COOLDOWN_ALL, OFF)).isEqualTo(OFF);
    }

    @Test
    public void onPreferenceChanged_thirdItemSelected_shouldDisable() {
        mController.displayPreference(mScreen);
        mController.onPreferenceChange(mPreference, "2");

        assertThat(Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.NOTIFICATION_COOLDOWN_ENABLED, OFF)).isEqualTo(OFF);
    }

}
