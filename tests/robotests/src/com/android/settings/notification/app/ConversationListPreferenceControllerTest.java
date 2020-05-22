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

package com.android.settings.notification.app;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.NotificationChannel;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.os.Bundle;
import android.provider.Settings;
import android.service.notification.ConversationChannelWrapper;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import com.android.settings.applications.AppInfoBase;
import com.android.settings.notification.NotificationBackend;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowApplication;

import java.util.ArrayList;

@RunWith(RobolectricTestRunner.class)
public class ConversationListPreferenceControllerTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock
    private NotificationBackend mBackend;

    private TestPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ShadowApplication shadowApplication = ShadowApplication.getInstance();
        mContext = RuntimeEnvironment.application;
        mController = new TestPreferenceController(mContext, mBackend);
    }

    @Test
    public void isAvailable() {
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void testPopulateList_hideIfNoConversations() {
        PreferenceCategory outerContainer = mock(PreferenceCategory.class);

        mController.populateList(new ArrayList<>(), outerContainer);

        verify(outerContainer).setVisible(false);
        verify(outerContainer, never()).addPreference(any());
    }

    @Test
    public void testPopulateList_validConversations() {
        final PreferenceManager preferenceManager = new PreferenceManager(mContext);
        PreferenceScreen ps = preferenceManager.createPreferenceScreen(mContext);
        PreferenceCategory outerContainer = spy(new PreferenceCategory(mContext));
        ps.addPreference(outerContainer);

        ConversationChannelWrapper ccw = new ConversationChannelWrapper();
        ccw.setNotificationChannel(mock(NotificationChannel.class));
        ccw.setPkg("pkg");
        ccw.setUid(1);
        ccw.setShortcutInfo(mock(ShortcutInfo.class));

        ArrayList<ConversationChannelWrapper> list = new ArrayList<>();
        list.add(ccw);

        mController.populateList(list, outerContainer);
        verify(outerContainer, times(1)).addPreference(any());
    }

    @Test
    public void populateConversations() {
        PreferenceCategory container = mock(PreferenceCategory.class);

        ConversationChannelWrapper ccw = new ConversationChannelWrapper();
        ccw.setNotificationChannel(mock(NotificationChannel.class));
        ccw.setPkg("pkg");
        ccw.setUid(1);
        ccw.setShortcutInfo(mock(ShortcutInfo.class));

        ConversationChannelWrapper ccwDemoted = new ConversationChannelWrapper();
        NotificationChannel demoted = new NotificationChannel("a", "a", 2);
        demoted.setDemoted(true);
        ccwDemoted.setNotificationChannel(demoted);
        ccwDemoted.setPkg("pkg");
        ccwDemoted.setUid(1);
        ccwDemoted.setShortcutInfo(mock(ShortcutInfo.class));

        ArrayList<ConversationChannelWrapper> list = new ArrayList<>();
        list.add(ccw);
        list.add(ccwDemoted);

        mController.populateConversations(list, container);

        verify(container, times(1)).addPreference(any());
    }

    @Test
    public void getSummary_withGroup() {
        ConversationChannelWrapper ccw = new ConversationChannelWrapper();
        NotificationChannel channel = new NotificationChannel("a", "child", 2);
        ccw.setNotificationChannel(channel);
        ccw.setPkg("pkg");
        ccw.setUid(1);
        ccw.setShortcutInfo(mock(ShortcutInfo.class));
        ccw.setGroupLabel("group");
        ccw.setParentChannelLabel("parent");

        assertThat(mController.getSummary(ccw).toString()).contains(ccw.getGroupLabel());
        assertThat(mController.getSummary(ccw).toString()).contains(ccw.getParentChannelLabel());
    }

    @Test
    public void getSummary_noGroup() {
        ConversationChannelWrapper ccw = new ConversationChannelWrapper();
        NotificationChannel channel = new NotificationChannel("a", "child", 2);
        ccw.setNotificationChannel(channel);
        ccw.setPkg("pkg");
        ccw.setUid(1);
        ccw.setShortcutInfo(mock(ShortcutInfo.class));
        ccw.setParentChannelLabel("parent");

        assertThat(mController.getSummary(ccw).toString()).isEqualTo(ccw.getParentChannelLabel());
    }

    @Test
    public void getTitle_withShortcut() {
        ConversationChannelWrapper ccw = new ConversationChannelWrapper();
        NotificationChannel channel = new NotificationChannel("a", "child", 2);
        ccw.setNotificationChannel(channel);
        ccw.setPkg("pkg");
        ccw.setUid(1);
        ShortcutInfo si = mock(ShortcutInfo.class);
        when(si.getLabel()).thenReturn("conversation name");
        ccw.setShortcutInfo(si);
        ccw.setGroupLabel("group");
        ccw.setParentChannelLabel("parent");

        assertThat(mController.getTitle(ccw).toString()).isEqualTo(si.getLabel());
    }

    @Test
    public void getTitle_noShortcut() {
        ConversationChannelWrapper ccw = new ConversationChannelWrapper();
        NotificationChannel channel = new NotificationChannel("a", "child", 2);
        ccw.setNotificationChannel(channel);
        ccw.setPkg("pkg");
        ccw.setUid(1);
        ccw.setParentChannelLabel("parent");

        assertThat(mController.getTitle(ccw).toString()).isEqualTo(
                ccw.getNotificationChannel().getName());
    }

    @Test
    public void testGetSubSettingLauncher() {
        ConversationChannelWrapper ccw = new ConversationChannelWrapper();
        NotificationChannel channel = new NotificationChannel("a", "child", 2);
        channel.setConversationId("parent", "convo id");
        ccw.setNotificationChannel(channel);
        ccw.setPkg("pkg");
        ccw.setUid(1);
        ccw.setParentChannelLabel("parent label");
        Intent intent = mController.getSubSettingLauncher(ccw, "title").toIntent();

        Bundle extras = intent.getExtras();
        assertThat(extras.getString(AppInfoBase.ARG_PACKAGE_NAME)).isEqualTo(ccw.getPkg());
        assertThat(extras.getInt(AppInfoBase.ARG_PACKAGE_UID)).isEqualTo(ccw.getUid());
        assertThat(extras.getString(Settings.EXTRA_CHANNEL_ID)).isEqualTo(
                ccw.getNotificationChannel().getId());
        assertThat(extras.getString(Settings.EXTRA_CONVERSATION_ID)).isEqualTo(
                ccw.getNotificationChannel().getConversationId());
    }

    private final class TestPreferenceController extends ConversationListPreferenceController {

        private TestPreferenceController(Context context, NotificationBackend backend) {
            super(context, backend);
        }

        @Override
        boolean matchesFilter(ConversationChannelWrapper conversation) {
            return true;
        }

        @Override
        public String getPreferenceKey() {
            return "test";
        }

        @Override
        Preference getSummaryPreference() {
            return null;
        }
    }
}
