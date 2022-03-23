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

package com.android.settings.notification;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.ParceledListSlice;
import android.service.notification.ConversationChannelWrapper;

import com.android.settings.testutils.shadow.ShadowNotificationBackend;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowNotificationBackend.class)
public class ConversationListSummaryPreferenceControllerTest {

    private ConversationListSummaryPreferenceController mController;
    private Context mContext;
    @Mock
    NotificationBackend mBackend;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mController = new ConversationListSummaryPreferenceController(mContext, "key");
        mController.setBackend(mBackend);
    }

    @Test
    public void getSummary_noPriorityConversations() {
        List<ConversationChannelWrapper> convos = new ArrayList<>();
        when(mBackend.getConversations(true)).thenReturn(
                new ParceledListSlice<>(convos));

        assertThat(mController.getSummary().toString()).contains("No");
    }

    @Test
    public void getSummary_somePriorityConversations() {
        List<ConversationChannelWrapper> convos = new ArrayList<>();
        convos.add(mock(ConversationChannelWrapper.class));
        convos.add(mock(ConversationChannelWrapper.class));
        when(mBackend.getConversations(true)).thenReturn(
                new ParceledListSlice<>(convos));

        assertThat(mController.getSummary().toString()).contains("2");
        assertThat(mController.getSummary().toString()).contains("conversations");
    }
}
