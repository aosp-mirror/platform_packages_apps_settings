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

package com.android.settings.notification.app;

import static android.app.NotificationManager.IMPORTANCE_DEFAULT;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import android.app.NotificationChannel;
import android.content.Context;
import android.content.pm.ParceledListSlice;
import android.content.pm.ShortcutInfo;
import android.service.notification.ConversationChannelWrapper;

import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.notification.NotificationBackend;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowApplication;

import java.util.ArrayList;
import java.util.Arrays;

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
public class AppConversationListPreferenceControllerTest {

    private Context mContext;

    @Mock
    private NotificationBackend mBackend;

    private AppConversationListPreferenceController mController;
    private NotificationBackend.AppRow mAppRow;
    private PreferenceCategory mPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = ApplicationProvider.getApplicationContext();

        mAppRow = new NotificationBackend.AppRow();
        mAppRow.uid = 42;
        mAppRow.pkg = "com.example.exampling";

        mController = new AppConversationListPreferenceController(mContext, mBackend);
        mController.onResume(mAppRow, null, null, null, null, null, new ArrayList<>());

        mPreference = new PreferenceCategory(mContext);
        PreferenceManager preferenceManager = new PreferenceManager(mContext);
        PreferenceScreen preferenceScreen = preferenceManager.createPreferenceScreen(mContext);
        preferenceScreen.addPreference(mPreference);
    }

    @Test
    public void updateState_someConversations_loadsConversations() {
        when(mBackend.getConversations(eq(mAppRow.pkg), eq(mAppRow.uid))).thenReturn(
                conversationList(
                        conversationChannel("1", "msg", "Mario", "Messages", "M", false),
                        conversationChannel("2", "msg", "Luigi", "Messages", "L", false)
                ));

        mController.updateState(mPreference);
        ShadowApplication.runBackgroundTasks();

        assertThat(mPreference.isVisible()).isTrue();
        assertThat(mPreference.getPreferenceCount()).isEqualTo(2);
        assertThat(mPreference.getPreference(0).getTitle().toString()).isEqualTo("Mario");
        assertThat(mPreference.getPreference(0).getSummary().toString()).isEqualTo("Messages");
        assertThat(mPreference.getPreference(1).getTitle().toString()).isEqualTo("Luigi");
        assertThat(mPreference.getPreference(1).getSummary().toString()).isEqualTo("Messages");
    }

    @Test
    public void updateState_someConversations_loadsNotDemotedOnly() {
        when(mBackend.getConversations(eq(mAppRow.pkg), eq(mAppRow.uid)))
                .thenReturn(conversationList(
                        conversationChannel("1", "msg", "Mario", "Messages", "M", false),
                        conversationChannel("2", "msg", "Luigi", "Messages", "L", true)
                ));

        mController.updateState(mPreference);
        ShadowApplication.runBackgroundTasks();

        assertThat(mPreference.isVisible()).isTrue();
        assertThat(mPreference.getPreferenceCount()).isEqualTo(1);
        assertThat(mPreference.getPreference(0).getTitle().toString()).isEqualTo("Mario");
    }

    @Test
    public void updateState_noConversations_hides() {
        when(mBackend.getConversations(eq(mAppRow.pkg), eq(mAppRow.uid)))
                .thenReturn(conversationList());

        mController.updateState(mPreference);

        assertThat(mPreference.isVisible()).isFalse();
        assertThat(mPreference.getPreferenceCount()).isEqualTo(0);
    }

    @Test
    public void updateState_refreshes() {
        when(mBackend.getConversations(eq(mAppRow.pkg), eq(mAppRow.uid)))
                .thenReturn(conversationList());
        mController.updateState(mPreference);
        assertThat(mPreference.isVisible()).isFalse();

        // Empty -> present
        when(mBackend.getConversations(eq(mAppRow.pkg), eq(mAppRow.uid)))
                .thenReturn(conversationList(
                        conversationChannel("1", "msg", "Mario", "Messages", "M", false)
                ));
        mController.updateState(mPreference);
        assertThat(mPreference.isVisible()).isTrue();
        assertThat(mPreference.getPreferenceCount()).isEqualTo(1);

        // Present -> empty
        when(mBackend.getConversations(eq(mAppRow.pkg), eq(mAppRow.uid)))
                .thenReturn(conversationList());
        mController.updateState(mPreference);
        assertThat(mPreference.isVisible()).isFalse();
        assertThat(mPreference.getPreferenceCount()).isEqualTo(0);
    }

    private static ParceledListSlice<ConversationChannelWrapper> conversationList(
            ConversationChannelWrapper... channels) {
        return new ParceledListSlice<>(Arrays.asList(channels));
    }

    private ConversationChannelWrapper conversationChannel(String id, String parentId,
            String shortcutName, String parentLabel, String shortcutId, boolean demoted) {
        NotificationChannel channel = new NotificationChannel(id, "Channel", IMPORTANCE_DEFAULT);
        channel.setDemoted(demoted);
        channel.setConversationId(parentId, shortcutId);
        ShortcutInfo shortcutInfo = new ShortcutInfo.Builder(mContext, shortcutId).setLongLabel(
                shortcutName).build();

        ConversationChannelWrapper ccw = new ConversationChannelWrapper();
        ccw.setPkg(mAppRow.pkg);
        ccw.setUid(mAppRow.uid);
        ccw.setNotificationChannel(channel);
        ccw.setShortcutInfo(shortcutInfo);
        ccw.setParentChannelLabel(parentLabel);
        return ccw;
    }
}
