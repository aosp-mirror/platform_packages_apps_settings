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

import static android.app.NotificationChannel.NEWS_ID;
import static android.app.NotificationChannel.PROMOTIONS_ID;
import static android.app.NotificationChannel.RECS_ID;
import static android.app.NotificationChannel.SOCIAL_MEDIA_ID;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.app.NotificationChannel;
import android.content.Context;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.service.notification.Flags;

import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.SmallTest;

import com.android.settings.notification.NotificationBackend;
import com.android.settingslib.PrimarySwitchPreference;

import com.google.common.collect.ImmutableMap;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
@SmallTest
@EnableFlags(Flags.FLAG_NOTIFICATION_CLASSIFICATION)
public class BundleListPreferenceControllerTest {
    private Context mContext;
    @Mock
    private NotificationBackend mBackend;
    private NotificationBackend.AppRow mAppRow;
    private BundleListPreferenceController mController;
    private PreferenceManager mPreferenceManager;
    private PreferenceScreen mPreferenceScreen;
    private PreferenceCategory mGroupList;
    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mContext = ApplicationProvider.getApplicationContext();

        mAppRow = new NotificationBackend.AppRow();
        mAppRow.pkg = "pkg";
        mAppRow.uid = 1111111;
        NotificationBackend.NotificationsSentState
                sentA = new NotificationBackend.NotificationsSentState();
        sentA.avgSentDaily = 2;
        sentA.avgSentWeekly = 10;
        NotificationBackend.NotificationsSentState
                sentB = new NotificationBackend.NotificationsSentState();
        sentB.avgSentDaily = 0;
        sentB.avgSentWeekly = 2;
        mAppRow.sentByChannel = ImmutableMap.of(
                PROMOTIONS_ID, sentA, NEWS_ID, sentA, SOCIAL_MEDIA_ID, sentB, RECS_ID, sentB);
        mController = new BundleListPreferenceController(mContext, mBackend);
        mController.onResume(mAppRow, null, null, null, null, null, null);
        mPreferenceManager = new PreferenceManager(mContext);
        mPreferenceScreen = mPreferenceManager.createPreferenceScreen(mContext);
        mGroupList = new PreferenceCategory(mContext);
        mPreferenceScreen.addPreference(mGroupList);

        when(mBackend.getChannel(mAppRow.pkg, mAppRow.uid, PROMOTIONS_ID)).thenReturn(
                new NotificationChannel(PROMOTIONS_ID, PROMOTIONS_ID, 2));
        when(mBackend.getChannel(mAppRow.pkg, mAppRow.uid, NEWS_ID)).thenReturn(
                new NotificationChannel(NEWS_ID, NEWS_ID, 2));
        when(mBackend.getChannel(mAppRow.pkg, mAppRow.uid, SOCIAL_MEDIA_ID)).thenReturn(
                new NotificationChannel(SOCIAL_MEDIA_ID, SOCIAL_MEDIA_ID, 2));
        when(mBackend.getChannel(mAppRow.pkg, mAppRow.uid, RECS_ID)).thenReturn(
                new NotificationChannel(RECS_ID, RECS_ID, 2));
    }

    @Test
    public void isAvailable_null() {
        mController.onResume(null, null, null, null, null, null, null);
        assertThat(mController.isAvailable()).isFalse();
        mAppRow.banned = true;
    }

    @Test
    public void isAvailable_banned() {
        mAppRow.banned = true;
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_locked() {
        mAppRow.lockedImportance = true;
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_system() {
        mAppRow.systemApp = true;
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable() {
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void updateState() {
        mController.updateState(mGroupList);
        assertThat(mGroupList.getPreferenceCount()).isEqualTo(4);
        assertThat(mGroupList.findPreference(PROMOTIONS_ID).getTitle()).isEqualTo(PROMOTIONS_ID);
        assertThat(mGroupList.findPreference(NEWS_ID).getTitle()).isEqualTo(NEWS_ID);
        assertThat(mGroupList.findPreference(SOCIAL_MEDIA_ID).getTitle())
                .isEqualTo(SOCIAL_MEDIA_ID);
        assertThat(mGroupList.findPreference(RECS_ID).getTitle()).isEqualTo(RECS_ID);
    }

    @Test
    public void updateState_updateChildren() {
        mController.updateState(mGroupList);
        assertThat(mGroupList.getPreferenceCount()).isEqualTo(4);

        when(mBackend.getChannel(mAppRow.pkg, mAppRow.uid, PROMOTIONS_ID)).thenReturn(
                new NotificationChannel(PROMOTIONS_ID, PROMOTIONS_ID, 2));

        mController.updateState(mGroupList);
        assertThat(mGroupList.getPreferenceCount()).isEqualTo(4);

        assertThat(((PrimarySwitchPreference) mGroupList.findPreference(NEWS_ID)).isChecked())
                .isEqualTo(false);
        assertThat(((PrimarySwitchPreference) mGroupList.findPreference(NEWS_ID)).isChecked())
                .isEqualTo(false);
    }
}
