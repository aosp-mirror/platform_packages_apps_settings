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

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.DISABLED_DEPENDENT_SETTING;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Bundle;
import android.service.notification.NotificationListenerFilter;
import android.service.notification.NotificationListenerService;
import android.util.ArraySet;

import androidx.preference.CheckBoxPreference;
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
    ServiceInfo mSi = new ServiceInfo();

    private static class TestTypeFilterPreferenceController extends TypeFilterPreferenceController {

        public TestTypeFilterPreferenceController(Context context, String key) {
            super(context, key);
        }

        @Override
        protected int getType() {
            return 32;
        }
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = ApplicationProvider.getApplicationContext();

        mController = new TestTypeFilterPreferenceController(mContext, "key");
        mController.setCn(mCn);
        mController.setNm(mNm);
        mController.setServiceInfo(mSi);
        mController.setUserId(0);
        mController.setTargetSdk(Build.VERSION_CODES.CUR_DEVELOPMENT + 1);
    }

    @Test
    public void testAvailable_notGranted() {
        when(mNm.isNotificationListenerAccessGranted(any())).thenReturn(false);
        mController.setTargetSdk(Build.VERSION_CODES.CUR_DEVELOPMENT + 1);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(DISABLED_DEPENDENT_SETTING);
    }

    @Test
    public void testAvailable_lowTargetSdk_noCustomizations() {
        when(mNm.isNotificationListenerAccessGranted(any())).thenReturn(true);
        mController.setTargetSdk(Build.VERSION_CODES.S);
        when(mNm.getListenerFilter(mCn, 0)).thenReturn(new NotificationListenerFilter());

        assertThat(mController.getAvailabilityStatus()).isEqualTo(DISABLED_DEPENDENT_SETTING);
    }

    @Test
    public void testAvailable_lowTargetSdk_customizations() {
        when(mNm.isNotificationListenerAccessGranted(any())).thenReturn(true);
        mController.setTargetSdk(Build.VERSION_CODES.S);
        NotificationListenerFilter nlf = new NotificationListenerFilter();
        nlf.setTypes(FLAG_FILTER_TYPE_CONVERSATIONS);
        when(mNm.getListenerFilter(mCn, 0)).thenReturn(nlf);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void testAvailable_highTargetSdk_noCustomizations() {
        when(mNm.isNotificationListenerAccessGranted(any())).thenReturn(true);
        mController.setTargetSdk(Build.VERSION_CODES.CUR_DEVELOPMENT + 1);
        when(mNm.getListenerFilter(mCn, 0)).thenReturn(new NotificationListenerFilter());

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void updateState_enabled_noMetaData() {
        when(mNm.isNotificationListenerAccessGranted(mCn)).thenReturn(true);
        when(mNm.getListenerFilter(mCn, 0)).thenReturn(new NotificationListenerFilter());
        CheckBoxPreference pref = new CheckBoxPreference(mContext);

        mController.updateState(pref);
        assertThat(pref.isEnabled()).isTrue();
    }

    @Test
    public void updateState_enabled_metaData_notTheDisableFilter() {
        mSi.metaData = new Bundle();
        mSi.metaData.putCharSequence("test", "value");
        when(mNm.isNotificationListenerAccessGranted(mCn)).thenReturn(true);
        when(mNm.getListenerFilter(mCn, 0)).thenReturn(new NotificationListenerFilter());
        CheckBoxPreference pref = new CheckBoxPreference(mContext);

        mController.updateState(pref);
        assertThat(pref.isEnabled()).isTrue();
    }

    @Test
    public void updateState_enabled_metaData_disableFilter_notThisField() {
        mSi.metaData = new Bundle();
        mSi.metaData.putCharSequence(NotificationListenerService.META_DATA_DISABLED_FILTER_TYPES, 
                "1|alerting");
        when(mNm.isNotificationListenerAccessGranted(mCn)).thenReturn(true);
        when(mNm.getListenerFilter(mCn, 0)).thenReturn(new NotificationListenerFilter());
        CheckBoxPreference pref = new CheckBoxPreference(mContext);

        mController.updateState(pref);
        assertThat(pref.isEnabled()).isTrue();
    }

    @Test
    public void updateState_enabled_metaData_disableFilter_thisField_stateIsChecked() {
        mSi.metaData = new Bundle();
        mSi.metaData.putCharSequence(NotificationListenerService.META_DATA_DISABLED_FILTER_TYPES,
                "conversations|2|32");
        when(mNm.isNotificationListenerAccessGranted(mCn)).thenReturn(true);
        when(mNm.getListenerFilter(mCn, 0)).thenReturn(
                new NotificationListenerFilter(32, new ArraySet<>()));
        CheckBoxPreference pref = new CheckBoxPreference(mContext);

        mController.updateState(pref);
        assertThat(pref.isEnabled()).isTrue();
    }

    @Test
    public void updateState_disabled() {
        when(mNm.isNotificationListenerAccessGranted(mCn)).thenReturn(false);
        when(mNm.getListenerFilter(mCn, 0)).thenReturn(new NotificationListenerFilter());
        CheckBoxPreference pref = new CheckBoxPreference(mContext);

        mController.updateState(pref);
        assertThat(pref.isEnabled()).isFalse();
    }

    @Test
    public void updateState_disabled_metaData_disableFilter_thisField_stateIsNotChecked() {
        mSi.metaData = new Bundle();
        mSi.metaData.putCharSequence(NotificationListenerService.META_DATA_DISABLED_FILTER_TYPES,
                "1|2|32");
        when(mNm.isNotificationListenerAccessGranted(mCn)).thenReturn(true);
        NotificationListenerFilter before = new NotificationListenerFilter(4, new ArraySet<>());
        when(mNm.getListenerFilter(mCn, 0)).thenReturn(before);
        CheckBoxPreference pref = new CheckBoxPreference(mContext);

        mController.updateState(pref);
        assertThat(pref.isChecked()).isFalse();
        assertThat(pref.isEnabled()).isFalse();
    }

    @Test
    public void updateState_checked() {
        NotificationListenerFilter nlf = new NotificationListenerFilter(mController.getType(),
                new ArraySet<>());
        when(mNm.isNotificationListenerAccessGranted(mCn)).thenReturn(true);
        when(mNm.getListenerFilter(mCn, 0)).thenReturn(nlf);

        CheckBoxPreference pref = new CheckBoxPreference(mContext);
        mController.updateState(pref);

        assertThat(pref.isChecked()).isTrue();
    }

    @Test
    public void updateState_unchecked() {
        NotificationListenerFilter nlf = new NotificationListenerFilter(mController.getType() - 1,
                new ArraySet<>());
        when(mNm.isNotificationListenerAccessGranted(mCn)).thenReturn(true);
        when(mNm.getListenerFilter(mCn, 0)).thenReturn(nlf);

        CheckBoxPreference pref = new CheckBoxPreference(mContext);
        mController.updateState(pref);

        assertThat(pref.isChecked()).isFalse();
    }

    @Test
    public void onPreferenceChange_true() {
        NotificationListenerFilter nlf = new NotificationListenerFilter(FLAG_FILTER_TYPE_ONGOING
                | FLAG_FILTER_TYPE_CONVERSATIONS, new ArraySet<>());
        when(mNm.isNotificationListenerAccessGranted(mCn)).thenReturn(true);
        when(mNm.getListenerFilter(mCn, 0)).thenReturn(nlf);

        CheckBoxPreference pref = new CheckBoxPreference(mContext);

        mController.onPreferenceChange(pref, true);

        ArgumentCaptor<NotificationListenerFilter> captor =
                ArgumentCaptor.forClass(NotificationListenerFilter.class);
        verify(mNm).setListenerFilter(eq(mCn), eq(0), captor.capture());
        assertThat(captor.getValue().getTypes()).isEqualTo(FLAG_FILTER_TYPE_CONVERSATIONS
                | FLAG_FILTER_TYPE_ONGOING | mController.getType());
    }

    @Test
    public void onPreferenceChange_false() {
        NotificationListenerFilter nlf = new NotificationListenerFilter(FLAG_FILTER_TYPE_ONGOING
                | FLAG_FILTER_TYPE_CONVERSATIONS | mController.getType(), new ArraySet<>());
        when(mNm.isNotificationListenerAccessGranted(mCn)).thenReturn(true);
        when(mNm.getListenerFilter(mCn, 0)).thenReturn(nlf);

        CheckBoxPreference pref = new CheckBoxPreference(mContext);

        mController.onPreferenceChange(pref, false);

        ArgumentCaptor<NotificationListenerFilter> captor =
                ArgumentCaptor.forClass(NotificationListenerFilter.class);
        verify(mNm).setListenerFilter(eq(mCn), eq(0), captor.capture());
        assertThat(captor.getValue().getTypes()).isEqualTo(FLAG_FILTER_TYPE_CONVERSATIONS
                | FLAG_FILTER_TYPE_ONGOING);
    }
}
