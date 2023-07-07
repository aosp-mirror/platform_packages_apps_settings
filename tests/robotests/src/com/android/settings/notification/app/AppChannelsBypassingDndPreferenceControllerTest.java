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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.ParceledListSlice;

import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.notification.NotificationBackend;
import com.android.settingslib.PrimarySwitchPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowApplication;

import java.util.ArrayList;
import java.util.Collections;

@RunWith(RobolectricTestRunner.class)
public class AppChannelsBypassingDndPreferenceControllerTest {

    @Mock
    private NotificationBackend mBackend;

    private NotificationBackend.AppRow mAppRow;
    private AppChannelsBypassingDndPreferenceController mController;

    private PreferenceScreen mPreferenceScreen;
    private PreferenceCategory mCategory;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        Context context = ApplicationProvider.getApplicationContext();

        mAppRow = new NotificationBackend.AppRow();
        mAppRow.uid = 42;
        mAppRow.pkg = "com.example.exampling";

        mController = new AppChannelsBypassingDndPreferenceController(context, mBackend);
        mController.onResume(mAppRow, null, null, null, null, null, new ArrayList<>());

        PreferenceManager preferenceManager = new PreferenceManager(context);
        mPreferenceScreen = preferenceManager.createPreferenceScreen(context);
        mCategory = new PreferenceCategory(context);
        mCategory.setKey(AppChannelsBypassingDndPreferenceController.KEY);
        mPreferenceScreen.addPreference(mCategory);
    }

    @Test
    public void displayPreference_showsAllAndChannels() {
        when(mBackend.getGroups(eq(mAppRow.pkg), eq(mAppRow.uid))).thenReturn(
                buildGroupList(true, true, false));

        mController.displayPreference(mPreferenceScreen);
        ShadowApplication.runBackgroundTasks();

        assertThat(mCategory.getPreferenceCount()).isEqualTo(4); // "All" + 3 channels
        assertThat(mCategory.getPreference(0).getTitle().toString()).isEqualTo(
                "Allow all notifications");
        assertThat(mCategory.getPreference(1).getTitle().toString()).isEqualTo("Channel 1");
        assertThat(mCategory.getPreference(2).getTitle().toString()).isEqualTo("Channel 2");
        assertThat(mCategory.getPreference(3).getTitle().toString()).isEqualTo("Channel 3");
    }

    @Test
    public void displayPreference_canToggleAllInterrupt() {
        when(mBackend.getGroups(eq(mAppRow.pkg), eq(mAppRow.uid))).thenReturn(
                buildGroupList(true, true, false));

        mController.displayPreference(mPreferenceScreen);
        ShadowApplication.runBackgroundTasks();

        assertThat(mCategory.getPreference(0).isEnabled()).isTrue();
    }

    @Test
    public void displayPreference_canToggleInterruptIfChannelEnabled() {
        when(mBackend.getGroups(eq(mAppRow.pkg), eq(mAppRow.uid))).thenReturn(
                buildGroupList(true, false, true));

        mController.displayPreference(mPreferenceScreen);
        ShadowApplication.runBackgroundTasks();

        assertThat(((PrimarySwitchPreference) mCategory.getPreference(
                1)).isSwitchEnabled()).isTrue();
        assertThat(((PrimarySwitchPreference) mCategory.getPreference(
                2)).isSwitchEnabled()).isFalse();
        assertThat(((PrimarySwitchPreference) mCategory.getPreference(
                3)).isSwitchEnabled()).isTrue();
    }

    @Test
    public void displayPreference_appBlocked_cannotToggleAllOrChannelInterrupts() {
        mAppRow.banned = true;
        when(mBackend.getGroups(eq(mAppRow.pkg), eq(mAppRow.uid))).thenReturn(
                buildGroupList(true, false, true));

        mController.displayPreference(mPreferenceScreen);
        ShadowApplication.runBackgroundTasks();

        assertThat(mCategory.getPreference(0).isEnabled()).isFalse();
        assertThat(((PrimarySwitchPreference) mCategory.getPreference(
                1)).isSwitchEnabled()).isFalse();
        assertThat(((PrimarySwitchPreference) mCategory.getPreference(
                2)).isSwitchEnabled()).isFalse();
        assertThat(((PrimarySwitchPreference) mCategory.getPreference(
                3)).isSwitchEnabled()).isFalse();
    }

    private static ParceledListSlice<NotificationChannelGroup> buildGroupList(
            boolean... enabledByChannel) {
        NotificationChannelGroup group = new NotificationChannelGroup("group", "The Group");
        for (int i = 0; i < enabledByChannel.length; i++) {
            group.addChannel(new NotificationChannel("channel-" + (i + 1), "Channel " + (i + 1),
                    enabledByChannel[i] ? NotificationManager.IMPORTANCE_DEFAULT
                            : NotificationManager.IMPORTANCE_NONE));
        }
        return new ParceledListSlice<>(Collections.singletonList(group));
    }
}
