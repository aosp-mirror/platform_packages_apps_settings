/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.notification.modes;

import static android.app.NotificationManager.INTERRUPTION_FILTER_NONE;
import static android.service.notification.ZenPolicy.CONVERSATION_SENDERS_IMPORTANT;
import static android.service.notification.ZenPolicy.PEOPLE_TYPE_ANYONE;
import static android.service.notification.ZenPolicy.PEOPLE_TYPE_CONTACTS;
import static android.service.notification.ZenPolicy.PEOPLE_TYPE_NONE;
import static android.service.notification.ZenPolicy.PEOPLE_TYPE_STARRED;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Flags;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.ShortcutInfo;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.service.notification.ConversationChannelWrapper;
import android.service.notification.ZenPolicy;
import android.view.LayoutInflater;
import android.view.View;

import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;
import com.android.settings.notification.modes.ZenHelperBackend.Contact;
import com.android.settingslib.notification.ConversationIconFactory;
import com.android.settingslib.notification.modes.TestModeBuilder;
import com.android.settingslib.notification.modes.ZenMode;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.MoreExecutors;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.Collection;

@EnableFlags(Flags.FLAG_MODES_UI)
@RunWith(RobolectricTestRunner.class)
public final class ZenModePeopleLinkPreferenceControllerTest {

    private ZenModePeopleLinkPreferenceController mController;
    private CircularIconsPreference mPreference;
    private CircularIconsView mIconsView;

    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private Context mContext;
    @Mock private ZenHelperBackend mHelperBackend;
    @Mock private ConversationIconFactory mConversationIconFactory;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        CircularIconSet.sExecutorService = MoreExecutors.newDirectExecutorService();
        mPreference = new TestableCircularIconsPreference(mContext);

        // Ensure the preference view is bound & measured (needed to add icons).
        View preferenceView = LayoutInflater.from(mContext).inflate(mPreference.getLayoutResource(),
                null);
        mIconsView = checkNotNull(preferenceView.findViewById(R.id.circles_container));
        mIconsView.setUiExecutor(MoreExecutors.directExecutor());
        preferenceView.measure(View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY));
        PreferenceViewHolder holder = PreferenceViewHolder.createInstanceForTests(preferenceView);
        mPreference.onBindViewHolder(holder);

        mController = new ZenModePeopleLinkPreferenceController(
                mContext, "something", mHelperBackend, mConversationIconFactory);

        setUpContacts(ImmutableList.of(), ImmutableList.of());
        setUpImportantConversations(ImmutableList.of());

        when(mHelperBackend.getContactPhoto(any())).then(
                (Answer<Drawable>) invocationOnMock -> photoOf(invocationOnMock.getArgument(0)));
        when(mConversationIconFactory.getConversationDrawable((ShortcutInfo) any(), any(), anyInt(),
                anyBoolean())).thenReturn(new ColorDrawable(Color.BLACK));
    }

    @Test
    public void updateState_dnd_enabled() {
        ZenMode dnd = TestModeBuilder.MANUAL_DND_ACTIVE;
        mController.updateState(mPreference, dnd);
        assertThat(mPreference.isEnabled()).isTrue();
    }

    @Test
    public void updateState_specialDnd_disabled() {
        ZenMode specialDnd = TestModeBuilder.manualDnd(INTERRUPTION_FILTER_NONE, true);
        mController.updateState(mPreference, specialDnd);
        assertThat(mPreference.isEnabled()).isFalse();
    }

    @Test
    public void updateState_disabled() {
        ZenMode zenMode = new TestModeBuilder()
                .setEnabled(false)
                .build();

        mController.updateState(mPreference, zenMode);

        assertThat(mPreference.isEnabled()).isFalse();
    }

    @Test
    public void updateState_setsSummary() {
        mController.updateState(mPreference, TestModeBuilder.EXAMPLE);

        assertThat(mPreference.getSummary()).isNotNull();
        assertThat(mPreference.getSummary().toString()).isNotEmpty();
    }

    @Test
    public void updateState_starredCallsNoMessages_displaysStarredContacts() {
        setUpContacts(ImmutableList.of(1, 2, 3, 4), ImmutableList.of(2, 3));
        ZenMode mode = new TestModeBuilder()
                .setZenPolicy(new ZenPolicy.Builder()
                        .allowCalls(PEOPLE_TYPE_STARRED)
                        .allowMessages(PEOPLE_TYPE_NONE)
                        .build())
                .build();

        mController.updateState(mPreference, mode);

        assertThat(mIconsView.getDisplayedIcons()).isNotNull();
        assertThat(mIconsView.getDisplayedIcons().icons()).hasSize(2);
        assertThat(mIconsView.getDisplayedIcons().icons().stream()
                .map(ColorDrawable.class::cast)
                .map(d -> d.getColor()).toList())
                .containsExactly(2, 3).inOrder();
    }

    @Test
    public void updateState_starredCallsContactMessages_displaysAllContacts() {
        setUpContacts(ImmutableList.of(1, 2, 3, 4), ImmutableList.of(2, 3));
        ZenMode mode = new TestModeBuilder()
                .setZenPolicy(new ZenPolicy.Builder()
                        .allowCalls(PEOPLE_TYPE_STARRED)
                        .allowMessages(PEOPLE_TYPE_CONTACTS)
                        .build())
                .build();

        mController.updateState(mPreference, mode);

        assertThat(mIconsView.getDisplayedIcons()).isNotNull();
        assertThat(mIconsView.getDisplayedIcons().icons()).hasSize(4);
        assertThat(mIconsView.getDisplayedIcons().icons().stream()
                .map(ColorDrawable.class::cast)
                .map(d -> d.getColor()).toList())
                .containsExactly(1, 2, 3, 4).inOrder();
    }

    @Test
    public void updateState_anyoneCallsContactMessages_displaysAnyonePlaceholder() {
        setUpContacts(ImmutableList.of(1, 2, 3, 4), ImmutableList.of(2, 3));
        ZenMode mode = new TestModeBuilder()
                .setZenPolicy(new ZenPolicy.Builder()
                        .allowCalls(PEOPLE_TYPE_ANYONE)
                        .allowMessages(PEOPLE_TYPE_CONTACTS)
                        .build())
                .build();

        mController.updateState(mPreference, mode);

        assertThat(mIconsView.getDisplayedIcons()).isNotNull();
        assertThat(mIconsView.getDisplayedIcons().icons()).hasSize(1);
        verify(mHelperBackend, never()).getContactPhoto(any());
    }

    @Test
    public void updateState_noContactsButImportantConversations_displaysConversations() {
        setUpContacts(ImmutableList.of(), ImmutableList.of());
        setUpImportantConversations(ImmutableList.of(1, 2, 3));
        ZenMode mode = new TestModeBuilder()
                .setZenPolicy(new ZenPolicy.Builder()
                        .allowCalls(PEOPLE_TYPE_CONTACTS)
                        .allowMessages(PEOPLE_TYPE_CONTACTS)
                        .allowConversations(CONVERSATION_SENDERS_IMPORTANT)
                        .build())
                .build();

        mController.updateState(mPreference, mode);

        assertThat(mIconsView.getDisplayedIcons()).isNotNull();
        assertThat(mIconsView.getDisplayedIcons().icons()).hasSize(3);
        verify(mConversationIconFactory, times(3)).getConversationDrawable((ShortcutInfo) any(),
                any(), anyInt(), anyBoolean());
    }

    private void setUpContacts(Collection<Integer> allIds, Collection<Integer> starredIds) {
        when(mHelperBackend.getAllContacts()).thenReturn(ImmutableList.copyOf(
                allIds.stream()
                        .map(id -> new Contact(id, "#" + id, Uri.parse("photo://" + id)))
                        .toList()));

        when(mHelperBackend.getStarredContacts()).thenReturn(ImmutableList.copyOf(
                starredIds.stream()
                        .map(id -> new Contact(id, "#" + id, Uri.parse("photo://" + id)))
                        .toList()));
    }

    private void setUpImportantConversations(Collection<Integer> ids) {
        when(mHelperBackend.getImportantConversations()).thenReturn(ImmutableList.copyOf(
                ids.stream()
                        .map(id -> {
                            ConversationChannelWrapper channel = new ConversationChannelWrapper();
                            channel.setNotificationChannel(
                                    new NotificationChannel(id.toString(), id.toString(),
                                            NotificationManager.IMPORTANCE_DEFAULT));
                            return channel;
                        })
                        .toList()));
    }

    private static ColorDrawable photoOf(Contact contact) {
        return new ColorDrawable((int) contact.id());
    }
}