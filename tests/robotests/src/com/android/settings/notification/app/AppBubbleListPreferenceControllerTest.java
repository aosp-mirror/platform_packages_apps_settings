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

import static android.app.NotificationChannel.DEFAULT_ALLOW_BUBBLE;
import static android.app.NotificationManager.BUBBLE_PREFERENCE_ALL;
import static android.app.NotificationManager.BUBBLE_PREFERENCE_NONE;
import static android.app.NotificationManager.BUBBLE_PREFERENCE_SELECTED;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.ParceledListSlice;
import android.content.pm.ShortcutInfo;
import android.os.UserManager;
import android.service.notification.ConversationChannelWrapper;

import androidx.preference.PreferenceCategory;

import com.android.settings.notification.AppBubbleListPreferenceController;
import com.android.settings.notification.NotificationBackend;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowApplication;

import java.util.ArrayList;
import java.util.List;


@RunWith(RobolectricTestRunner.class)
public class AppBubbleListPreferenceControllerTest {

    private Context mContext;
    @Mock
    private NotificationBackend mBackend;
    @Mock
    private NotificationManager mNm;
    @Mock
    private UserManager mUm;

    private AppBubbleListPreferenceController mController;
    private ParceledListSlice<ConversationChannelWrapper> mConvoList;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ShadowApplication shadowApplication = ShadowApplication.getInstance();
        shadowApplication.setSystemService(Context.NOTIFICATION_SERVICE, mNm);
        shadowApplication.setSystemService(Context.USER_SERVICE, mUm);
        mContext = RuntimeEnvironment.application;

        List<ConversationChannelWrapper> convoList = new ArrayList<>();
        convoList.add(getConvo(-1, "default"));
        convoList.add(getConvo(1, "selected"));
        convoList.add(getConvo(0, "excluded"));
        mConvoList = new ParceledListSlice<>(convoList);
        when(mBackend.getConversations(anyString(), anyInt())).thenReturn(mConvoList);
        mController = new AppBubbleListPreferenceController(mContext, mBackend);
    }

    ConversationChannelWrapper getConvo(int bubbleChannelPref, String channelId) {
        ConversationChannelWrapper ccw = new ConversationChannelWrapper();
        NotificationChannel channel = mock(NotificationChannel.class);
        when(channel.getId()).thenReturn(channelId);
        when(channel.getAllowBubbles()).thenReturn(bubbleChannelPref);
        when(channel.canBubble()).thenReturn(bubbleChannelPref == 1);
        ccw.setNotificationChannel(channel);
        ccw.setPkg("pkg");
        ccw.setUid(1);
        ccw.setShortcutInfo(mock(ShortcutInfo.class));
        return ccw;
    }

    @Test
    public void isAvailable_BUBBLE_PREFERENCE_NONE_false() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        appRow.bubblePreference = BUBBLE_PREFERENCE_NONE;
        mController.onResume(appRow, null, null, null, null, null);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_BUBBLE_PREFERENCE_SELECTED_true() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        appRow.bubblePreference = BUBBLE_PREFERENCE_SELECTED;
        mController.onResume(appRow, null, null, null, null, null);

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_BUBBLE_PREFERENCE_ALL_true() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        appRow.bubblePreference = BUBBLE_PREFERENCE_ALL;
        mController.onResume(appRow, null, null, null, null, null);

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void filterAndSortConversations_BUBBLE_PREFERENCE_SELECTED_filtersAllowedBubbles() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        appRow.bubblePreference = BUBBLE_PREFERENCE_SELECTED;
        mController.onResume(appRow, null, null, null, null, null);

        List<ConversationChannelWrapper> result =
                mController.filterAndSortConversations(mConvoList.getList());
        assertThat(result.size()).isEqualTo(1);
        assertThat(result.get(0).getNotificationChannel().getId())
                .isEqualTo("selected");
    }

    @Test
    public void filterAndSortConversations_BUBBLE_PREFERENCE_ALL_filtersExcludedBubbles() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        appRow.bubblePreference = BUBBLE_PREFERENCE_ALL;
        mController.onResume(appRow, null, null, null, null, null);

        List<ConversationChannelWrapper> result =
                mController.filterAndSortConversations(mConvoList.getList());
        assertThat(result.size()).isEqualTo(1);
        assertThat(result.get(0).getNotificationChannel().getId())
                .isEqualTo("excluded");
    }

    @Test
    public void clickConversationPref_updatesChannel() {
        NotificationBackend.AppRow appRow = new NotificationBackend.AppRow();
        appRow.bubblePreference = BUBBLE_PREFERENCE_ALL;
        appRow.pkg = "PKG";
        mController.onResume(appRow, null, null, null, null, null);
        mController.mPreference = new PreferenceCategory(mContext);

        ConversationChannelWrapper ccw = mConvoList.getList().get(0);
        AppBubbleListPreferenceController.ConversationPreference pref =
                (AppBubbleListPreferenceController.ConversationPreference)
                mController.createConversationPref(ccw);
        pref.onClick(null);

        verify(ccw.getNotificationChannel()).setAllowBubbles(DEFAULT_ALLOW_BUBBLE);
        verify(mBackend).updateChannel(anyString(), anyInt(), any(NotificationChannel.class));
    }
}
