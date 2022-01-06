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
import android.app.NotificationChannelGroup;
import android.app.people.ConversationChannel;
import android.app.people.IPeopleManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.SpannedString;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;
import com.android.settings.applications.AppInfoBase;
import com.android.settings.notification.NotificationBackend;
import com.android.settingslib.widget.LayoutPreference;

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
public class RecentConversationsPreferenceControllerTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock
    private NotificationBackend mBackend;
    @Mock
    private IPeopleManager mPs;

    private RecentConversationsPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ShadowApplication shadowApplication = ShadowApplication.getInstance();
        mContext = RuntimeEnvironment.application;
        mController = new RecentConversationsPreferenceController(mContext, mBackend, mPs);
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

        ConversationChannel ccw = new ConversationChannel(mock(ShortcutInfo.class), 6,
                new NotificationChannel("hi", "hi", 4),
                new NotificationChannelGroup("hi", "hi"), 7,
                false);

        ArrayList<ConversationChannel> list = new ArrayList<>();
        list.add(ccw);

        mController.populateList(list, outerContainer);
        // one for the preference, one for the button ro clear all
        verify(outerContainer, times(2)).addPreference(any());
    }

    @Test
    public void populateConversations_blocked() {
        PreferenceCategory container = mock(PreferenceCategory.class);

        ConversationChannel ccw = new ConversationChannel(mock(ShortcutInfo.class), 6,
                new NotificationChannel("hi", "hi", 4),
                new NotificationChannelGroup("hi", "hi"), 7,
                false);

        ConversationChannel ccw2 = new ConversationChannel(mock(ShortcutInfo.class), 6,
                new NotificationChannel("hi", "hi", 0),
                new NotificationChannelGroup("hi", "hi"), 7,
                false);

        NotificationChannelGroup blockedGroup = new NotificationChannelGroup("hi", "hi");
        blockedGroup.setBlocked(true);
        ConversationChannel ccw3 = new ConversationChannel(mock(ShortcutInfo.class), 6,
                new NotificationChannel("hi", "hi", 4),
                blockedGroup, 7,
                false);

        ArrayList<ConversationChannel> list = new ArrayList<>();
        list.add(ccw);
        list.add(ccw2);
        list.add(ccw3);

        mController.populateConversations(list, container);

        verify(container, times(1)).addPreference(any());
    }

    @Test
    public void getSummary_withGroup() {
        ShortcutInfo si = mock(ShortcutInfo.class);
        when(si.getLabel()).thenReturn("person");
        ConversationChannel ccw = new ConversationChannel(mock(ShortcutInfo.class), 6,
                new NotificationChannel("hi", "channel", 4),
                new NotificationChannelGroup("hi", "group"), 7,
                true);

        assertThat(mController.getSummary(ccw).toString()).contains(
                ccw.getNotificationChannelGroup().getName());
        assertThat(mController.getSummary(ccw).toString()).contains(
                ccw.getNotificationChannel().getName());
    }

    @Test
    public void getSummary_noGroup() {
        ShortcutInfo si = mock(ShortcutInfo.class);
        when(si.getLabel()).thenReturn("person");
        ConversationChannel ccw = new ConversationChannel(mock(ShortcutInfo.class), 6,
                new NotificationChannel("hi", "channel", 4),
                null, 7,
                true);

        assertThat(mController.getSummary(ccw).toString()).isEqualTo(
                ccw.getNotificationChannel().getName());
    }

    @Test
    public void getTitle_withShortcut() {
        ShortcutInfo si = mock(ShortcutInfo.class);
        when(si.getLabel()).thenReturn("person");
        ConversationChannel ccw = new ConversationChannel(si, 6,
                new NotificationChannel("hi", "channel", 4),
                new NotificationChannelGroup("hi", "group"), 7,
                true);

        assertThat(mController.getTitle(ccw).toString()).isEqualTo(si.getLabel());
    }

    @Test
    public void testGetSubSettingLauncher() {
        ShortcutInfo si = mock(ShortcutInfo.class);
        when(si.getId()).thenReturn("person");
        when(si.getPackage()).thenReturn("pkg");
        ConversationChannel ccw = new ConversationChannel(si, 6,
                new NotificationChannel("hi", "channel", 4),
                new NotificationChannelGroup("hi", "group"), 7,
                true);

        Intent intent = mController.getSubSettingLauncher(ccw, "title").toIntent();

        Bundle extras = intent.getExtras();
        assertThat(extras.getString(AppInfoBase.ARG_PACKAGE_NAME)).isEqualTo(
                ccw.getShortcutInfo().getPackage());
        assertThat(extras.getInt(AppInfoBase.ARG_PACKAGE_UID)).isEqualTo(ccw.getUid());
        assertThat(extras.getString(Settings.EXTRA_CHANNEL_ID)).isEqualTo(
                ccw.getNotificationChannel().getId());
        assertThat(extras.getString(Settings.EXTRA_CONVERSATION_ID)).isEqualTo(
                ccw.getShortcutInfo().getId());
    }

    @Test
    public void testCreatesChannel() {
        ShortcutInfo si = mock(ShortcutInfo.class);
        when(si.getId()).thenReturn("person");
        when(si.getPackage()).thenReturn("pkg");
        ConversationChannel ccw = new ConversationChannel(si, 6,
                new NotificationChannel("hi", "channel", 4),
                new NotificationChannelGroup("hi", "group"), 7,
                true);

        Preference pref = mController.createConversationPref(new PreferenceCategory(mContext),
                ccw, 100);
        try {
            pref.performClick();
        } catch (RuntimeException e) {
            // expected since it tries to launch an activity
        }
        verify(mBackend).createConversationNotificationChannel(
                si.getPackage(), ccw.getUid(), ccw.getNotificationChannel(), si.getId());
    }

    @Test
    public void testRemoveConversation() throws Exception {
        ShortcutInfo si = mock(ShortcutInfo.class);
        when(si.getId()).thenReturn("person");
        when(si.getPackage()).thenReturn("pkg");
        ConversationChannel ccw = new ConversationChannel(si, 6,
                new NotificationChannel("hi", "channel", 4),
                new NotificationChannelGroup("hi", "group"), 7,
                false);

        RecentConversationPreference pref =
                (RecentConversationPreference) mController.createConversationPref(
                        new PreferenceCategory(mContext), ccw, 100);
        final View view = View.inflate(mContext, pref.getLayoutResource(), null);
        PreferenceViewHolder holder = spy(PreferenceViewHolder.createInstanceForTests(view));
        View delete = View.inflate(mContext, pref.getSecondTargetResId(), null);
        when(holder.findViewById(pref.getClearId())).thenReturn(delete);

        pref.onBindViewHolder(holder);
        pref.getClearView().performClick();

        verify(mPs).removeRecentConversation(
                si.getPackage(), UserHandle.getUserId(ccw.getUid()), si.getId());
    }

    @Test
    public void testRemoveConversations() throws Exception {
        ShortcutInfo si = mock(ShortcutInfo.class);
        when(si.getId()).thenReturn("person");
        when(si.getPackage()).thenReturn("pkg");
        ConversationChannel ccw = new ConversationChannel(si, 6,
                new NotificationChannel("hi", "hi", 4),
                new NotificationChannelGroup("hi", "group"), 7,
                false);

        ConversationChannel ccw2 = new ConversationChannel(si, 6,
                new NotificationChannel("bye", "bye", 4),
                new NotificationChannelGroup("hi", "group"), 7,
                true);

        PreferenceCategory group = new PreferenceCategory(mContext);
        PreferenceScreen screen = new PreferenceManager(mContext).createPreferenceScreen(mContext);
        screen.addPreference(group);

        RecentConversationPreference pref = mController.createConversationPref(
                        new PreferenceCategory(mContext), ccw, 100);
        final View view = View.inflate(mContext, pref.getLayoutResource(), null);
        PreferenceViewHolder holder = spy(PreferenceViewHolder.createInstanceForTests(view));
        View delete = View.inflate(mContext, pref.getSecondTargetResId(), null);
        when(holder.findViewById(pref.getClearId())).thenReturn(delete);
        group.addPreference(pref);

        RecentConversationPreference pref2 = mController.createConversationPref(
                        new PreferenceCategory(mContext), ccw2, 100);
        final View view2 = View.inflate(mContext, pref2.getLayoutResource(), null);
        PreferenceViewHolder holder2 = spy(PreferenceViewHolder.createInstanceForTests(view2));
        View delete2 = View.inflate(mContext, pref2.getSecondTargetResId(), null);
        when(holder2.findViewById(pref.getClearId())).thenReturn(delete2);
        group.addPreference(pref2);

        LayoutPreference clearAll = mController.getClearAll(group);
        group.addPreference(clearAll);

        clearAll.findViewById(R.id.conversation_settings_clear_recents).performClick();

        verify(mPs).removeAllRecentConversations();
        assertThat((Preference) group.findPreference("hi:person")).isNull();
        assertThat((Preference) group.findPreference("bye:person")).isNotNull();
    }

    @Test
    public void testNonremoveableConversation() throws Exception {
        ShortcutInfo si = mock(ShortcutInfo.class);
        when(si.getId()).thenReturn("person");
        when(si.getPackage()).thenReturn("pkg");
        ConversationChannel ccw = new ConversationChannel(si, 6,
                new NotificationChannel("hi", "channel", 4),
                new NotificationChannelGroup("hi", "group"), 7,
                true);

        RecentConversationPreference pref =
                (RecentConversationPreference) mController.createConversationPref(
                        new PreferenceCategory(mContext), ccw, 100);
        assertThat(pref.hasClearListener()).isFalse();
    }

    @Test
    public void testPopulateList_onlyNonremoveableConversations() {
        final PreferenceManager preferenceManager = new PreferenceManager(mContext);
        PreferenceScreen ps = preferenceManager.createPreferenceScreen(mContext);
        PreferenceCategory outerContainer = spy(new PreferenceCategory(mContext));
        ps.addPreference(outerContainer);

        ConversationChannel ccw = new ConversationChannel(mock(ShortcutInfo.class), 6,
                new NotificationChannel("hi", "hi", 4),
                new NotificationChannelGroup("hi", "hi"), 7,
                true /* hasactivenotifs */);

        ArrayList<ConversationChannel> list = new ArrayList<>();
        list.add(ccw);

        mController.populateList(list, outerContainer);
        // one for the preference, none for 'clear all'
        verify(outerContainer, times(1)).addPreference(any());
    }

    @Test
    public void testSpans() {
        ShortcutInfo si = mock(ShortcutInfo.class);
        when(si.getLabel()).thenReturn(new SpannedString("hello"));
        ConversationChannel ccw = new ConversationChannel(si, 6,
                new NotificationChannel("hi", "hi", 4),
                null, 7,
                true /* hasactivenotifs */);
        ShortcutInfo si2 = mock(ShortcutInfo.class);
        when(si2.getLabel()).thenReturn("hello");
        ConversationChannel ccw2 = new ConversationChannel(si2, 6,
                new NotificationChannel("hi2", "hi2", 4),
                null, 7,
                true /* hasactivenotifs */);
        // no crash
        mController.mConversationComparator.compare(ccw, ccw2);
    }

    @Test
    public void testNullSpans() {
        ConversationChannel ccw = new ConversationChannel(mock(ShortcutInfo.class), 6,
                new NotificationChannel("hi", "hi", 4),
                null, 7,
                true /* hasactivenotifs */);
        ConversationChannel ccw2 = new ConversationChannel(mock(ShortcutInfo.class), 6,
                new NotificationChannel("hi2", "hi2", 4),
                null, 7,
                true /* hasactivenotifs */);
        // no crash
        mController.mConversationComparator.compare(ccw, ccw2);
    }
}
