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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.os.UserManager;
import android.provider.Settings;

import com.android.settings.TestConfig;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class ZenModeMessagesTest {
    private ZenModeMessagesSettings mMessages;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ZenModeBackend mBackend;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Activity mActivity;
    @Mock
    private UserManager mUserManager;
    @Mock
    private NotificationManager mNotificationManager;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        when(mActivity.getSystemService(Context.USER_SERVICE)).thenReturn(mUserManager);
        when(mActivity.getSystemService(Context.NOTIFICATION_SERVICE))
                .thenReturn(mNotificationManager);
        FakeFeatureFactory.setupForTest();

        mMessages = new ZenModeMessagesSettings();
        mMessages.onAttach((Context)mActivity);

        ReflectionHelpers.setField(mMessages, "mBackend", mBackend);
    }

    @Test
    public void getDefaultKeyReturnsBasedOnZen() {
        when(mBackend.getSendersKey(NotificationManager.Policy.PRIORITY_CATEGORY_MESSAGES))
                .thenCallRealMethod();
        when(mBackend.getZenMode()).thenReturn(Settings.Global.ZEN_MODE_NO_INTERRUPTIONS);
        assertThat(mMessages.getDefaultKey())
                .isEqualTo(mBackend.getKeyFromSetting(mBackend.SOURCE_NONE));

        when(mBackend.getZenMode()).thenReturn(Settings.Global.ZEN_MODE_ALARMS);
        assertThat(mMessages.getDefaultKey())
                .isEqualTo(mBackend.getKeyFromSetting(mBackend.SOURCE_NONE));

        when(mBackend.getZenMode()).thenReturn(Settings.Global.ZEN_MODE_IMPORTANT_INTERRUPTIONS);
        when(mBackend.isPriorityCategoryEnabled(
                NotificationManager.Policy.PRIORITY_CATEGORY_MESSAGES))
                .thenReturn(true);
        when(mBackend.getPriorityMessageSenders())
                .thenReturn(NotificationManager.Policy.PRIORITY_SENDERS_ANY);
        assertThat(mMessages.getDefaultKey())
                .isEqualTo(mBackend.getKeyFromSetting(
                        NotificationManager.Policy.PRIORITY_SENDERS_ANY));
    }

    @Test
    public void setAnySender() {
        String key = mBackend.getKeyFromSetting(NotificationManager.Policy.PRIORITY_SENDERS_ANY);
        mMessages.setDefaultKey(key);
        verify(mBackend).saveSenders(NotificationManager.Policy.PRIORITY_CATEGORY_MESSAGES,
                mBackend.getSettingFromPrefKey(key));
    }

    @Test
    public void setNoSender() {
        String key = mBackend.getKeyFromSetting(ZenModeBackend.SOURCE_NONE);
        mMessages.setDefaultKey(key);
        verify(mBackend).saveSenders(NotificationManager.Policy.PRIORITY_CATEGORY_MESSAGES,
                mBackend.getSettingFromPrefKey(key));
    }

    @Test
    public void setStarredSenders() {
        String key = mBackend.getKeyFromSetting(
                NotificationManager.Policy.PRIORITY_SENDERS_STARRED);
        mMessages.setDefaultKey(key);
        verify(mBackend).saveSenders(NotificationManager.Policy.PRIORITY_CATEGORY_MESSAGES,
                mBackend.getSettingFromPrefKey(key));
    }

    @Test
    public void setContactsOnlySenders() {
        String key = mBackend.getKeyFromSetting(
                NotificationManager.Policy.PRIORITY_SENDERS_CONTACTS);
        mMessages.setDefaultKey(key);
        verify(mBackend).saveSenders(NotificationManager.Policy.PRIORITY_CATEGORY_MESSAGES,
                mBackend.getSettingFromPrefKey(key));
    }
}