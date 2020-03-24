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

import static android.app.NotificationManager.Policy.CONVERSATION_SENDERS_ANYONE;
import static android.app.NotificationManager.Policy.CONVERSATION_SENDERS_IMPORTANT;
import static android.app.NotificationManager.Policy.CONVERSATION_SENDERS_NONE;

import static com.android.settings.notification.zen.ZenModePriorityConversationsPreferenceController.KEY_ALL;
import static com.android.settings.notification.zen.ZenModePriorityConversationsPreferenceController.KEY_IMPORTANT;
import static com.android.settings.notification.zen.ZenModePriorityConversationsPreferenceController.KEY_NONE;

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
public class ZenModePriorityConversationsPreferenceControllerTest {

    private ZenModePriorityConversationsPreferenceController mController;

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
        mController = new ZenModePriorityConversationsPreferenceController(
                mContext, "test_key", mock(Lifecycle.class), mNotifBackend);
        ReflectionHelpers.setField(mController, "mBackend", mZenBackend);

        when(mMockPrefCategory.getContext()).thenReturn(mContext);
        when(mPreferenceScreen.findPreference(mController.getPreferenceKey()))
                .thenReturn(mMockPrefCategory);
        captureRadioButtons();
    }

    @Test
    public void displayPreference_radioButtonsCreatedOnlyOnce() {
        when(mMockPrefCategory.findPreference(any())).thenReturn(mock(Preference.class));

        // radio buttons were already created, so don't re-create them
        mController.displayPreference(mPreferenceScreen);
        verify(mMockPrefCategory, never()).addPreference(any());
    }

    @Test
    public void clickAllConversations() {
        RadioButtonPreference allConversationsRb = getButton(KEY_ALL);
        allConversationsRb.onClick();

        verify(mZenBackend).saveConversationSenders(CONVERSATION_SENDERS_ANYONE);
    }

    @Test
    public void clickImportantConversations() {
        RadioButtonPreference importantConversationsRb = getButton(KEY_IMPORTANT);
        importantConversationsRb.onClick();

        verify(mZenBackend).saveConversationSenders(CONVERSATION_SENDERS_IMPORTANT);
    }

    @Test
    public void clickNoConversations() {
        RadioButtonPreference noConversationsRb = getButton(KEY_NONE);
        noConversationsRb.onClick();

        verify(mZenBackend)
                .saveConversationSenders(CONVERSATION_SENDERS_NONE);
    }

    private void captureRadioButtons() {
        ArgumentCaptor<RadioButtonPreference> rbCaptor =
                ArgumentCaptor.forClass(RadioButtonPreference.class);
        mController.displayPreference(mPreferenceScreen);

        // verifies 3 buttons were added
        verify(mMockPrefCategory, times(3)).addPreference(rbCaptor.capture());
        mRadioButtonPreferences = rbCaptor.getAllValues();
        assertThat(mRadioButtonPreferences.size()).isEqualTo(3);

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