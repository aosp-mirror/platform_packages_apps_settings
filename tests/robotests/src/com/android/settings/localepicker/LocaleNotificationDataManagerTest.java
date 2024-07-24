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

package com.android.settings.localepicker;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;

import android.content.Context;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.Map;
import java.util.Set;

@RunWith(RobolectricTestRunner.class)
public class LocaleNotificationDataManagerTest {
    private Context mContext;
    private LocaleNotificationDataManager mDataManager;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        mDataManager = new LocaleNotificationDataManager(mContext);
    }

    @After
    public void tearDown() {
        mDataManager.clearLocaleNotificationMap();
    }

    @Test
    public void testPutGetNotificationInfo() {
        String locale = "en-US";
        Set<Integer> uidSet = Set.of(101);
        NotificationInfo info = new NotificationInfo(uidSet, 1, 1, 100L, 1000);

        mDataManager.putNotificationInfo(locale, info);
        NotificationInfo expected = mDataManager.getNotificationInfo(locale);

        assertThat(info.equals(expected)).isTrue();
        assertThat(expected.getNotificationId()).isEqualTo(info.getNotificationId());
        assertThat(expected.getDismissCount()).isEqualTo(info.getDismissCount());
        assertThat(expected.getNotificationCount()).isEqualTo(info.getNotificationCount());
        assertThat(expected.getUidCollection()).isEqualTo(info.getUidCollection());
        assertThat(expected.getLastNotificationTimeMs()).isEqualTo(
                info.getLastNotificationTimeMs());
    }

    @Test
    public void testRemoveNotificationInfo() {
        String locale = "en-US";
        Set<Integer> uidSet = Set.of(101);
        NotificationInfo info = new NotificationInfo(uidSet, 1, 1, 100L, 1000);

        mDataManager.putNotificationInfo(locale, info);
        assertThat(mDataManager.getNotificationInfo(locale)).isEqualTo(info);
        mDataManager.removeNotificationInfo(locale);
        assertThat(mDataManager.getNotificationInfo(locale)).isNull();
    }

    @Test
    public void testGetNotificationMap() {
        String enUS = "en-US";
        Set<Integer> uidSet1 = Set.of(101, 102);
        NotificationInfo info1 = new NotificationInfo(uidSet1, 1, 1, 1000L, 1234);
        String jaJP = "ja-JP";
        Set<Integer> uidSet2 = Set.of(103, 104);
        NotificationInfo info2 = new NotificationInfo(uidSet2, 1, 0, 2000L, 5678);
        mDataManager.putNotificationInfo(enUS, info1);
        mDataManager.putNotificationInfo(jaJP, info2);

        Map<String, NotificationInfo> map = mDataManager.getLocaleNotificationInfoMap();

        assertThat(map.size()).isEqualTo(2);
        assertThat(mDataManager.getNotificationInfo(enUS).equals(map.get(enUS))).isTrue();
        assertThat(mDataManager.getNotificationInfo(jaJP).equals(map.get(jaJP))).isTrue();
    }
}
