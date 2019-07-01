/*
 * Copyright (C) 2018 The Android Open Source Project
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

import static android.provider.Settings.Global.ZEN_MODE;
import static android.provider.Settings.Global.ZEN_MODE_ALARMS;
import static android.provider.Settings.Global.ZEN_MODE_IMPORTANT_INTERRUPTIONS;
import static android.provider.Settings.Global.ZEN_MODE_NO_INTERRUPTIONS;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;

import androidx.preference.ListPreference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.util.ReflectionHelpers;

import androidx.preference.ListPreference;
import androidx.preference.PreferenceScreen;

@RunWith(RobolectricTestRunner.class)
public class ZenModePriorityCallsPreferenceControllerTest {

    private ZenModePriorityCallsPreferenceController mController;

    @Mock
    private ZenModeBackend mBackend;
    @Mock
    private NotificationManager mNotificationManager;
    @Mock
    private ListPreference mockPref;
    @Mock
    private NotificationManager.Policy mPolicy;
    @Mock
    private PreferenceScreen mPreferenceScreen;
    private ContentResolver mContentResolver;
    private Context mContext;

    /**
     * Array Values Key
     * 0: anyone
     * 1: contacts
     * 2: starred
     * 3: none
     */
    private String[] mValues;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        ShadowApplication shadowApplication = ShadowApplication.getInstance();
        shadowApplication.setSystemService(Context.NOTIFICATION_SERVICE, mNotificationManager);

        mContext = RuntimeEnvironment.application;
        mValues = mContext.getResources().getStringArray(R.array.zen_mode_contacts_values);
        mContentResolver = RuntimeEnvironment.application.getContentResolver();
        when(mNotificationManager.getNotificationPolicy()).thenReturn(mPolicy);

        when(mBackend.getPriorityCallSenders())
                .thenReturn(NotificationManager.Policy.PRIORITY_SENDERS_STARRED);
        when(mBackend.getAlarmsTotalSilenceCallsMessagesSummary(
                NotificationManager.Policy.PRIORITY_CATEGORY_CALLS)).thenCallRealMethod();
        when(mBackend.getContactsSummary(NotificationManager.Policy.PRIORITY_CATEGORY_CALLS))
                .thenCallRealMethod();

        mController = new ZenModePriorityCallsPreferenceController(mContext, mock(Lifecycle.class));
        ReflectionHelpers.setField(mController, "mBackend", mBackend);

        when(mPreferenceScreen.findPreference(mController.getPreferenceKey())).thenReturn(mockPref);
        mController.displayPreference(mPreferenceScreen);
    }

    @Test
    public void updateState_TotalSilence() {
        Settings.Global.putInt(mContentResolver, ZEN_MODE, ZEN_MODE_NO_INTERRUPTIONS);

        when(mBackend.isPriorityCategoryEnabled(
                NotificationManager.Policy.PRIORITY_CATEGORY_CALLS))
                .thenReturn(false);
        final ListPreference mockPref = mock(ListPreference.class);
        mController.updateState(mockPref);

        verify(mockPref).setEnabled(false);
        verify(mockPref).setSummary(R.string.zen_mode_from_none_calls);
    }

    @Test
    public void updateState_AlarmsOnly() {
        Settings.Global.putInt(mContentResolver, ZEN_MODE, ZEN_MODE_ALARMS);

        final ListPreference mockPref = mock(ListPreference.class);
        mController.updateState(mockPref);

        verify(mockPref).setEnabled(false);
        verify(mockPref).setSummary(R.string.zen_mode_from_none_calls);
    }

    @Test
    public void updateState_Priority() {
        Settings.Global.putInt(mContentResolver, ZEN_MODE, ZEN_MODE_IMPORTANT_INTERRUPTIONS);

        when(mBackend.isPriorityCategoryEnabled(
                NotificationManager.Policy.PRIORITY_CATEGORY_CALLS))
                .thenReturn(true);

        mController.updateState(mockPref);

        verify(mockPref).setEnabled(true);
        verify(mockPref).setSummary(R.string.zen_mode_from_starred);
    }

    @Test
    public void onPreferenceChange_setSelectedContacts_any() {
        Settings.Global.putInt(mContentResolver, ZEN_MODE, ZEN_MODE_IMPORTANT_INTERRUPTIONS);
        when(mBackend.getPriorityCallSenders()).thenReturn(
                NotificationManager.Policy.PRIORITY_SENDERS_ANY);
        mController.updateState(mockPref);
        verify(mockPref).setValue(mValues[mController.getIndexOfSendersValue(
                ZenModeBackend.ZEN_MODE_FROM_ANYONE)]);
    }

    @Test
    public void onPreferenceChange_setSelectedContacts_none() {
        Settings.Global.putInt(mContentResolver, ZEN_MODE, ZEN_MODE_IMPORTANT_INTERRUPTIONS);
        when(mBackend.getPriorityCallSenders()).thenReturn(ZenModeBackend.SOURCE_NONE);
        mController.updateState(mockPref);
        verify(mockPref).setValue(mValues[mController.getIndexOfSendersValue(
                ZenModeBackend.ZEN_MODE_FROM_NONE)]);
    }

    @Test
    public void onPreferenceChange_setSelectedContacts_starred() {
        Settings.Global.putInt(mContentResolver, ZEN_MODE, ZEN_MODE_IMPORTANT_INTERRUPTIONS);
        when(mBackend.getPriorityCallSenders()).thenReturn(
                NotificationManager.Policy.PRIORITY_SENDERS_STARRED);
        mController.updateState(mockPref);
        verify(mockPref).setValue(mValues[mController.getIndexOfSendersValue(
                ZenModeBackend.ZEN_MODE_FROM_STARRED)]);
    }

    @Test
    public void onPreferenceChange_setSelectedContacts_contacts() {
        Settings.Global.putInt(mContentResolver, ZEN_MODE, ZEN_MODE_IMPORTANT_INTERRUPTIONS);
        when(mBackend.getPriorityCallSenders()).thenReturn(
                NotificationManager.Policy.PRIORITY_SENDERS_CONTACTS);
        mController.updateState(mockPref);
        verify(mockPref).setValue(mValues[mController.getIndexOfSendersValue(
                ZenModeBackend.ZEN_MODE_FROM_CONTACTS)]);
    }
}