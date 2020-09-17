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

package com.android.settings.notification.zen;

import static android.app.NotificationManager.Policy.PRIORITY_CATEGORY_MESSAGES;
import static android.app.NotificationManager.Policy.PRIORITY_SENDERS_ANY;
import static android.app.NotificationManager.Policy.PRIORITY_SENDERS_CONTACTS;
import static android.app.NotificationManager.Policy.PRIORITY_SENDERS_STARRED;

import static com.android.settings.notification.zen.ZenModePrioritySendersPreferenceController.KEY_ANY;
import static com.android.settings.notification.zen.ZenModePrioritySendersPreferenceController.KEY_CONTACTS;
import static com.android.settings.notification.zen.ZenModePrioritySendersPreferenceController.KEY_NONE;
import static com.android.settings.notification.zen.ZenModePrioritySendersPreferenceController.KEY_STARRED;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.Context;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.settings.notification.NotificationBackend;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.widget.RadioButtonPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.util.ReflectionHelpers;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class ZenModePrioritySendersPreferenceControllerTest {

    private ZenModePrioritySendersPreferenceController mMessagesController;

    @Mock
    private ZenModeBackend mZenBackend;
    @Mock
    private PreferenceCategory mMockPrefCategory;
    @Mock
    private NotificationManager.Policy mPolicy;
    @Mock
    private PreferenceScreen mPreferenceScreen;
    @Mock
    private NotificationBackend mNotifBackend;

    private List<RadioButtonPreference> mRadioButtonPreferences;
    private ContentResolver mContentResolver;
    private Context mContext;
    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mMessagesController = new ZenModePrioritySendersPreferenceController(
                mContext, "test_key_messages", mock(Lifecycle.class), true);
        ReflectionHelpers.setField(mMessagesController, "mBackend", mZenBackend);

        when(mMockPrefCategory.getContext()).thenReturn(mContext);
        when(mPreferenceScreen.findPreference(mMessagesController.getPreferenceKey()))
                .thenReturn(mMockPrefCategory);
        captureRadioButtons();
    }

    @Test
    public void displayPreference_radioButtonsCreatedOnlyOnce() {
        when(mMockPrefCategory.findPreference(any())).thenReturn(mock(Preference.class));

        // radio buttons were already created, so don't re-create them
        mMessagesController.displayPreference(mPreferenceScreen);
        verify(mMockPrefCategory, never()).addPreference(any());
    }

    @Test
    public void clickAnySenders() {
        // GIVEN current priority message senders are STARRED
        when(mZenBackend.getPriorityMessageSenders()).thenReturn(PRIORITY_SENDERS_STARRED);

        // WHEN user clicks the any senders option
        RadioButtonPreference allSendersRb = getButton(KEY_ANY);
        allSendersRb.onClick();

        // THEN any senders gets saved as priority senders for messages
        verify(mZenBackend).saveSenders(PRIORITY_CATEGORY_MESSAGES, PRIORITY_SENDERS_ANY);
    }

    @Test
    public void clickStarredSenders() {
        // GIVEN current priority message senders are ANY
        when(mZenBackend.getPriorityMessageSenders()).thenReturn(PRIORITY_SENDERS_ANY);

        // WHEN user clicks the starred contacts option
        RadioButtonPreference starredRb = getButton(KEY_STARRED);
        starredRb.onClick();

        // THEN starred contacts gets saved as priority senders for messages
        verify(mZenBackend).saveSenders(PRIORITY_CATEGORY_MESSAGES, PRIORITY_SENDERS_STARRED);
    }

    @Test
    public void clickContactsSenders() {
        // GIVEN current priority message senders are ANY
        when(mZenBackend.getPriorityMessageSenders()).thenReturn(PRIORITY_SENDERS_ANY);

        // WHEN user clicks the contacts only option
        RadioButtonPreference contactsRb = getButton(KEY_CONTACTS);
        contactsRb.onClick();

        // THEN contacts gets saved as priority senders for messages
        verify(mZenBackend).saveSenders(PRIORITY_CATEGORY_MESSAGES, PRIORITY_SENDERS_CONTACTS);
    }

    @Test
    public void clickNoSenders() {
        // GIVEN current priority message senders are ANY
        when(mZenBackend.getPriorityMessageSenders()).thenReturn(PRIORITY_SENDERS_ANY);

        // WHEN user clicks the no senders option
        RadioButtonPreference noSenders = getButton(KEY_NONE);
        noSenders.onClick();

        // THEN no senders gets saved as priority senders for messages
        verify(mZenBackend).saveSenders(PRIORITY_CATEGORY_MESSAGES, ZenModeBackend.SOURCE_NONE);
    }

    @Test
    public void clickSameOptionMultipleTimes() {
        // GIVEN current priority message senders are ANY
        when(mZenBackend.getPriorityMessageSenders()).thenReturn(PRIORITY_SENDERS_ANY);

        // WHEN user clicks the any senders option multiple times again
        RadioButtonPreference anySenders = getButton(KEY_ANY);
        anySenders.onClick();
        anySenders.onClick();
        anySenders.onClick();

        // THEN no senders are saved because this setting is already in effect
        verify(mZenBackend, never()).saveSenders(PRIORITY_CATEGORY_MESSAGES, PRIORITY_SENDERS_ANY);
    }

    private void captureRadioButtons() {
        ArgumentCaptor<RadioButtonPreference> rbCaptor =
                ArgumentCaptor.forClass(RadioButtonPreference.class);
        mMessagesController.displayPreference(mPreferenceScreen);

        // verifies 4 buttons were added
        verify(mMockPrefCategory, times(4)).addPreference(rbCaptor.capture());
        mRadioButtonPreferences = rbCaptor.getAllValues();
        assertThat(mRadioButtonPreferences.size()).isEqualTo(4);

        reset(mMockPrefCategory);
    }

    private RadioButtonPreference getButton(String key) {
        for (RadioButtonPreference pref : mRadioButtonPreferences) {
            if (key.equals(pref.getKey())) {
                return pref;
            }
        }
        return null;
    }
}
