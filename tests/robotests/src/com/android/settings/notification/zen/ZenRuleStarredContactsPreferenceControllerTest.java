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

package com.android.settings.notification.zen;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.service.notification.ZenPolicy;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

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

@RunWith(RobolectricTestRunner.class)
public class ZenRuleStarredContactsPreferenceControllerTest extends ZenRuleCustomPrefContrTestBase {

    private ZenRuleStarredContactsPreferenceController mCallsController;
    private ZenRuleStarredContactsPreferenceController mMessagesController;
    private static int CURR_CONTROLLER = ZenPolicy.PRIORITY_CATEGORY_CALLS;

    @Mock
    private ZenModeBackend mBackend;
    @Mock
    private NotificationManager mNotificationManager;
    @Mock
    private Preference mockPref;
    @Mock
    private PreferenceScreen mPreferenceScreen;
    @Mock
    private Intent testIntent;
    @Mock
    private ComponentName mComponentName;
    private Context mContext;

    @Override
    AbstractZenCustomRulePreferenceController getController() {
        if (CURR_CONTROLLER == ZenPolicy.PRIORITY_CATEGORY_MESSAGES) {
            return mMessagesController;
        } else {
            return mCallsController;
        }
    }

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        ShadowApplication shadowApplication = ShadowApplication.getInstance();
        shadowApplication.setSystemService(Context.NOTIFICATION_SERVICE, mNotificationManager);

        mContext = RuntimeEnvironment.application;
        when(testIntent.resolveActivity(any())).thenReturn(mComponentName);

        mCallsController = new ZenRuleStarredContactsPreferenceController(
                mContext, mock(Lifecycle.class), ZenPolicy.PRIORITY_CATEGORY_CALLS,
                "zen_mode_starred_contacts_callers");
        when(mBackend.getAutomaticZenRule(RULE_ID)).thenReturn(mRule);
        ReflectionHelpers.setField(mCallsController, "mBackend", mBackend);
        ReflectionHelpers.setField(mCallsController, "mStarredContactsIntent", testIntent);
        when(mPreferenceScreen.findPreference(mCallsController.getPreferenceKey()))
                .thenReturn(mockPref);
        mCallsController.displayPreference(mPreferenceScreen);

        mMessagesController = new ZenRuleStarredContactsPreferenceController(
                mContext, mock(Lifecycle.class), ZenPolicy.PRIORITY_CATEGORY_MESSAGES,
                "zen_mode_starred_contacts_messages");
        ReflectionHelpers.setField(mMessagesController, "mBackend", mBackend);
        ReflectionHelpers.setField(mMessagesController, "mStarredContactsIntent", testIntent);
        when(mPreferenceScreen.findPreference(mMessagesController.getPreferenceKey()))
                .thenReturn(mockPref);
        mMessagesController.displayPreference(mPreferenceScreen);
    }

    @Test
    public void isAvailable_noCallers() {
        CURR_CONTROLLER = ZenPolicy.PRIORITY_CATEGORY_CALLS;
        updateControllerZenPolicy(new ZenPolicy.Builder()
                .allowCalls(ZenPolicy.PEOPLE_TYPE_NONE)
                .build());
        assertThat(mCallsController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_anyCallers() {
        CURR_CONTROLLER = ZenPolicy.PRIORITY_CATEGORY_CALLS;
        updateControllerZenPolicy(new ZenPolicy.Builder()
                .allowCalls(ZenPolicy.PEOPLE_TYPE_ANYONE)
                .build());

        assertThat(mCallsController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_starredCallers() {
        CURR_CONTROLLER = ZenPolicy.PRIORITY_CATEGORY_CALLS;
        updateControllerZenPolicy(new ZenPolicy.Builder()
                .allowCalls(ZenPolicy.PEOPLE_TYPE_STARRED)
                .build());

        assertThat(mCallsController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_noMessages() {
        CURR_CONTROLLER = ZenPolicy.PRIORITY_CATEGORY_MESSAGES;
        updateControllerZenPolicy(new ZenPolicy.Builder()
                .allowMessages(ZenPolicy.PEOPLE_TYPE_NONE)
                .build());
        assertThat(mCallsController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_anyMessages() {
        CURR_CONTROLLER = ZenPolicy.PRIORITY_CATEGORY_MESSAGES;
        updateControllerZenPolicy(new ZenPolicy.Builder()
                .allowMessages(ZenPolicy.PEOPLE_TYPE_ANYONE)
                .build());

        assertThat(mMessagesController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_starredMessageContacts() {
        CURR_CONTROLLER = ZenPolicy.PRIORITY_CATEGORY_MESSAGES;
        updateControllerZenPolicy(new ZenPolicy.Builder()
                .allowMessages(ZenPolicy.PEOPLE_TYPE_STARRED)
                .build());

        assertThat(mMessagesController.isAvailable()).isTrue();
    }

    @Test
    public void nullPreference_displayPreference() {
        when(mPreferenceScreen.findPreference(mMessagesController.getPreferenceKey()))
                .thenReturn(null);

        // should not throw a null pointer
        mMessagesController.displayPreference(mPreferenceScreen);
    }
}
