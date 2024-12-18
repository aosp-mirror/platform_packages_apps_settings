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

package com.android.settings.applications.specialaccess.notificationaccess;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.AppOpsManager;
import android.app.Flags;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.annotations.RequiresFlagsDisabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settingslib.RestrictedSwitchPreference;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class ApprovalPreferenceControllerTest {

    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule(
            SetFlagsRule.DefaultInitValueType.DEVICE_DEFAULT);
    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    private Context mContext;
    private FakeFeatureFactory mFeatureFactory;
    @Mock
    private NotificationAccessDetails mFragment;
    private ApprovalPreferenceController mController;
    @Mock
    NotificationManager mNm;
    @Mock
    AppOpsManager mAppOpsManager;
    @Mock
    PackageManager mPm;
    PackageInfo mPkgInfo;
    ComponentName mCn = new ComponentName("a", "b");

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mFeatureFactory = FakeFeatureFactory.setupForTest();
        mContext = spy(ApplicationProvider.getApplicationContext());
        doReturn(mContext).when(mFragment).getContext();

        mPkgInfo = new PackageInfo();
        mPkgInfo.applicationInfo = mock(ApplicationInfo.class);
        when(mPkgInfo.applicationInfo.loadLabel(mPm)).thenReturn("LABEL");

        mController = new ApprovalPreferenceController(mContext, "key");
        mController.setCn(mCn);
        mController.setNm(mNm);
        mController.setParent(mFragment);
        mController.setPkgInfo(mPkgInfo);
    }

    @Test
    public void updateState_enabled() {
        when(mAppOpsManager.noteOpNoThrow(anyInt(), anyInt(), anyString())).thenReturn(
                AppOpsManager.MODE_ALLOWED);
        when(mNm.isNotificationListenerAccessGranted(mCn)).thenReturn(true);
        RestrictedSwitchPreference pref = new RestrictedSwitchPreference(
                mContext);
        pref.setAppOps(mAppOpsManager);

        mController.updateState(pref);

        assertThat(pref.isEnabled()).isTrue();
    }

    @Test
    public void updateState_invalidCn_disabled() {
        ComponentName longCn = new ComponentName("com.example.package",
                com.google.common.base.Strings.repeat("Blah", 150));
        mController.setCn(longCn);
        when(mAppOpsManager.noteOpNoThrow(anyInt(), anyInt(), anyString())).thenReturn(
                AppOpsManager.MODE_ALLOWED);
        RestrictedSwitchPreference pref = new RestrictedSwitchPreference(
                mContext);
        pref.setAppOps(mAppOpsManager);

        mController.updateState(pref);

        assertThat(pref.isEnabled()).isFalse();
    }

    @Test
    @RequiresFlagsDisabled(android.security.Flags.FLAG_EXTEND_ECM_TO_ALL_SETTINGS)
    public void updateState_checked() {
        when(mAppOpsManager.noteOpNoThrow(anyInt(), anyInt(), anyString())).thenReturn(
                AppOpsManager.MODE_ALLOWED);
        when(mNm.isNotificationListenerAccessGranted(mCn)).thenReturn(true);
        RestrictedSwitchPreference pref = new RestrictedSwitchPreference(
                mContext);
        pref.setAppOps(mAppOpsManager);

        mController.updateState(pref);
        assertThat(pref.isChecked()).isTrue();
        assertThat(pref.isEnabled()).isTrue();
    }

    @Test
    @RequiresFlagsDisabled(android.security.Flags.FLAG_EXTEND_ECM_TO_ALL_SETTINGS)
    public void restrictedSettings_appOpsDisabled() {
        Assert.assertFalse(android.security.Flags.extendEcmToAllSettings());
        when(mAppOpsManager.noteOpNoThrow(anyInt(), anyInt(), anyString()))
                .thenReturn(AppOpsManager.MODE_ERRORED);
        doReturn(mAppOpsManager).when(mContext).getSystemService(Context.APP_OPS_SERVICE);
        when(mNm.isNotificationListenerAccessGranted(mCn)).thenReturn(false);
        RestrictedSwitchPreference pref = new RestrictedSwitchPreference(
                mContext);
        pref.setAppOps(mAppOpsManager);
        mController.setSettingIdentifier(AppOpsManager.OPSTR_ACCESS_NOTIFICATIONS);

        mController.updateState(pref);

        verify(mAppOpsManager).noteOpNoThrow(anyInt(), anyInt(), anyString());
        assertThat(pref.isEnabled()).isFalse();
    }

    @Test
    @RequiresFlagsDisabled(android.security.Flags.FLAG_EXTEND_ECM_TO_ALL_SETTINGS)
    public void restrictedSettings_serviceAlreadyEnabled() {
        when(mAppOpsManager.noteOpNoThrow(anyInt(), anyInt(), anyString())).thenReturn(
                AppOpsManager.MODE_ERRORED);
        when(mNm.isNotificationListenerAccessGranted(mCn)).thenReturn(true);
        RestrictedSwitchPreference pref = new RestrictedSwitchPreference(
                mContext);
        pref.setAppOps(mAppOpsManager);

        mController.updateState(pref);
        assertThat(pref.isEnabled()).isTrue();
    }

    @Test
    public void enable() {
        mController.enable(mCn);
        verify(mFeatureFactory.metricsFeatureProvider).action(
                mContext,
                MetricsProto.MetricsEvent.APP_SPECIAL_PERMISSION_NOTIVIEW_ALLOW,
                "a");

        verify(mNm).setNotificationListenerAccessGranted(mCn, true);
    }

    @Test
    @EnableFlags(Flags.FLAG_MODES_API)
    public void disable() {
        when(mNm.isNotificationPolicyAccessGrantedForPackage(anyString())).thenReturn(false);
        mController.disable(mCn);
        verify(mFeatureFactory.metricsFeatureProvider).action(
                mContext,
                MetricsProto.MetricsEvent.APP_SPECIAL_PERMISSION_NOTIVIEW_ALLOW,
                "a");

        verify(mNm).removeAutomaticZenRules(eq(mCn.getPackageName()), eq(true));
        verify(mNm).setNotificationListenerAccessGranted(mCn, false);
    }
}
