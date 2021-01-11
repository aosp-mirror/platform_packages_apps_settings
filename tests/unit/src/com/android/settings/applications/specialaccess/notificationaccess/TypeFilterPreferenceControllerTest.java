/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.applications.specialaccess.notificationaccess;

import static android.service.notification.NotificationListenerService.FLAG_FILTER_TYPE_CONVERSATIONS;
import static android.service.notification.NotificationListenerService.FLAG_FILTER_TYPE_ONGOING;
import static android.service.notification.NotificationListenerService.FLAG_FILTER_TYPE_SILENT;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.ComponentName;
import android.content.Context;
import android.service.notification.NotificationListenerFilter;
import android.util.ArraySet;

import androidx.preference.MultiSelectListPreference;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.notification.NotificationBackend;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Set;

@RunWith(AndroidJUnit4.class)
public class TypeFilterPreferenceControllerTest {

    private Context mContext;
    private TypeFilterPreferenceController mController;
    @Mock
    NotificationBackend mNm;
    ComponentName mCn = new ComponentName("a", "b");

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = ApplicationProvider.getApplicationContext();

        mController = new TypeFilterPreferenceController(mContext, "key");
        mController.setCn(mCn);
        mController.setNm(mNm);
        mController.setUserId(0);
    }

    @Test
    public void updateState_enabled() {
        when(mNm.isNotificationListenerAccessGranted(mCn)).thenReturn(true);
        when(mNm.getListenerFilter(mCn, 0)).thenReturn(new NotificationListenerFilter());
        MultiSelectListPreference pref = new MultiSelectListPreference(mContext);

        mController.updateState(pref);
        assertThat(pref.isEnabled()).isTrue();
    }

    @Test
    public void updateState_disabled() {
        when(mNm.isNotificationListenerAccessGranted(mCn)).thenReturn(false);
        when(mNm.getListenerFilter(mCn, 0)).thenReturn(new NotificationListenerFilter());
        MultiSelectListPreference pref = new MultiSelectListPreference(mContext);

        mController.updateState(pref);
        assertThat(pref.isEnabled()).isFalse();
    }

    @Test
    public void updateState() {
        NotificationListenerFilter nlf = new NotificationListenerFilter(FLAG_FILTER_TYPE_ONGOING
                | FLAG_FILTER_TYPE_SILENT, new ArraySet<>());
        when(mNm.isNotificationListenerAccessGranted(mCn)).thenReturn(true);
        when(mNm.getListenerFilter(mCn, 0)).thenReturn(nlf);

        MultiSelectListPreference pref = new MultiSelectListPreference(mContext);
        mController.updateState(pref);

        assertThat(pref.getValues()).containsExactlyElementsIn(
                new String[] {String.valueOf(FLAG_FILTER_TYPE_ONGOING),
                        String.valueOf(FLAG_FILTER_TYPE_SILENT)});
        assertThat(pref.getSummary()).isNotNull();
    }

    @Test
    public void getSummary() {
        NotificationListenerFilter nlf = new NotificationListenerFilter(FLAG_FILTER_TYPE_ONGOING
                | FLAG_FILTER_TYPE_CONVERSATIONS, new ArraySet<>());
        when(mNm.isNotificationListenerAccessGranted(mCn)).thenReturn(true);
        when(mNm.getListenerFilter(mCn, 0)).thenReturn(nlf);

        MultiSelectListPreference pref = new MultiSelectListPreference(mContext);
        mController.updateState(pref);

        assertThat(mController.getSummary().toString()).ignoringCase().contains("ongoing");
        assertThat(mController.getSummary().toString()).ignoringCase().contains("conversation");
    }

    @Test
    public void onPreferenceChange() {
        NotificationListenerFilter nlf = new NotificationListenerFilter(FLAG_FILTER_TYPE_ONGOING
                | FLAG_FILTER_TYPE_CONVERSATIONS, new ArraySet<>());
        when(mNm.isNotificationListenerAccessGranted(mCn)).thenReturn(true);
        when(mNm.getListenerFilter(mCn, 0)).thenReturn(nlf);

        MultiSelectListPreference pref = new MultiSelectListPreference(mContext);

        mController.onPreferenceChange(pref, Set.of("8", "1", "4"));

        ArgumentCaptor<NotificationListenerFilter> captor =
                ArgumentCaptor.forClass(NotificationListenerFilter.class);
        verify(mNm).setListenerFilter(eq(mCn), eq(0), captor.capture());
        assertThat(captor.getValue().getTypes()).isEqualTo(FLAG_FILTER_TYPE_CONVERSATIONS
                | FLAG_FILTER_TYPE_SILENT | FLAG_FILTER_TYPE_ONGOING);
    }
}
