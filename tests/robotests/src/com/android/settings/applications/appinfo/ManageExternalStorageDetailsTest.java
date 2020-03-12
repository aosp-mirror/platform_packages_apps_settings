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
 * limitations under the License
 */

package com.android.settings.applications.appinfo;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.app.AppOpsManager;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;

import androidx.preference.SwitchPreference;

import com.android.settings.applications.AppStateAppOpsBridge.PermissionState;
import com.android.settings.applications.AppStateManageExternalStorageBridge;
import com.android.settings.testutils.shadow.ShadowUserManager;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

import java.util.HashMap;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowUserManager.class})
public class ManageExternalStorageDetailsTest {

    @Mock
    private AppOpsManager mAppOpsManager;
    @Mock
    private SwitchPreference mSwitchPref;
    @Mock
    private MetricsFeatureProvider mMetricsFeatureProvider;
    @Mock
    private AppStateManageExternalStorageBridge mBridge;

    private ManageExternalStorageDetails mFragment;

    private final HashMap<Integer, Integer> mUidToOpModeMap = new HashMap<>();

    @Before
    public void setUp() {
        // Reset the global trackers
        mUidToOpModeMap.clear();

        //Start the mockin'
        MockitoAnnotations.initMocks(this);

        mFragment = new ManageExternalStorageDetails();
        ReflectionHelpers.setField(mFragment, "mAppOpsManager", mAppOpsManager);
        ReflectionHelpers.setField(mFragment, "mSwitchPref", mSwitchPref);
        ReflectionHelpers.setField(mFragment, "mBridge", mBridge);
        ReflectionHelpers.setField(mFragment, "mMetricsFeatureProvider",
                mMetricsFeatureProvider);

        mockAppOpsOperations();
    }

    @Test
    public void onPreferenceChange_enableManageExternalStorage_shouldTriggerAppOpsManager() {
        // Inject mock package details
        final int mockUid = 23333;
        final String mockPkgName = "com.mock.pkg.1";
        PackageInfo pkgInfo = mock(PackageInfo.class);
        pkgInfo.applicationInfo = new ApplicationInfo();
        pkgInfo.applicationInfo.uid = mockUid;

        ReflectionHelpers.setField(mFragment, "mPackageInfo", pkgInfo);
        ReflectionHelpers.setField(mFragment, "mPackageName", mockPkgName);

        // Set the initial state to be disabled
        injectPermissionState(false);

        // Simulate a preference change
        mFragment.onPreferenceChange(mSwitchPref, /* newValue */ true);

        // Verify that mAppOpsManager was called to allow the app-op
        verify(mAppOpsManager, times(1))
                .setUidMode(anyInt(), anyInt(), anyInt());
        assertThat(mUidToOpModeMap).containsExactly(mockUid, AppOpsManager.MODE_ALLOWED);

        // Verify the mSwitchPref was enabled
        ArgumentCaptor<Boolean> acSetEnabled = ArgumentCaptor.forClass(Boolean.class);
        verify(mSwitchPref, times(1)).setEnabled(acSetEnabled.capture());
        assertThat(acSetEnabled.getAllValues()).containsExactly(true);

        // Verify that mSwitchPref was toggled to on
        ArgumentCaptor<Boolean> acSetChecked = ArgumentCaptor.forClass(Boolean.class);
        verify(mSwitchPref, times(1)).setChecked(acSetChecked.capture());
        assertThat(acSetChecked.getAllValues()).containsExactly(true);
    }

    @Test
    public void onPreferenceChange_disableManageExternalStorage_shouldTriggerAppOpsManager() {
        // Inject mock package details
        final int mockUid = 24444;
        final String mockPkgName = "com.mock.pkg.2";
        PackageInfo pkgInfo = mock(PackageInfo.class);
        pkgInfo.applicationInfo = new ApplicationInfo();
        pkgInfo.applicationInfo.uid = mockUid;

        ReflectionHelpers.setField(mFragment, "mPackageInfo", pkgInfo);
        ReflectionHelpers.setField(mFragment, "mPackageName", mockPkgName);

        // Set the initial state to be enabled
        injectPermissionState(true);

        // Simulate a preference change
        mFragment.onPreferenceChange(mSwitchPref, /* newValue */ false);

        // Verify that mAppOpsManager was called to deny the app-op
        verify(mAppOpsManager, times(1))
                .setUidMode(anyInt(), anyInt(), anyInt());
        assertThat(mUidToOpModeMap).containsExactly(mockUid, AppOpsManager.MODE_ERRORED);

        // Verify the mSwitchPref was enabled
        ArgumentCaptor<Boolean> acSetEnabled = ArgumentCaptor.forClass(Boolean.class);
        verify(mSwitchPref, times(1)).setEnabled(acSetEnabled.capture());
        assertThat(acSetEnabled.getAllValues()).containsExactly(true);

        // Verify that mSwitchPref was toggled to off
        ArgumentCaptor<Boolean> acSetChecked = ArgumentCaptor.forClass(Boolean.class);
        verify(mSwitchPref, times(1)).setChecked(acSetChecked.capture());
        assertThat(acSetChecked.getAllValues()).containsExactly(false);
    }

    private void injectPermissionState(boolean enabled) {
        PermissionState state = new PermissionState(null, null);
        state.permissionDeclared = true;
        state.appOpMode = enabled ? AppOpsManager.MODE_ALLOWED : AppOpsManager.MODE_ERRORED;
        ReflectionHelpers.setField(mFragment, "mPermissionState", state);
    }

    private void mockAppOpsOperations() {
        Answer<Void> answerSetUidMode = invocation -> {
            int code = invocation.getArgument(0);
            int uid = invocation.getArgument(1);
            int mode = invocation.getArgument(2);

            if (code != AppOpsManager.OP_MANAGE_EXTERNAL_STORAGE) {
                return null;
            }

            mUidToOpModeMap.put(uid, mode);

            return null;
        };

        doAnswer(answerSetUidMode).when(mAppOpsManager)
                .setUidMode(anyInt(), anyInt(), anyInt());

        Answer<PermissionState> answerPermState = invocation -> {
            String packageName = invocation.getArgument(0);
            int uid = invocation.getArgument(1);
            PermissionState res = new PermissionState(packageName, null);
            res.permissionDeclared = false;

            if (mUidToOpModeMap.containsKey(uid)) {
                res.permissionDeclared = true;
                res.appOpMode = mUidToOpModeMap.get(uid);
            }
            return res;
        };

        doAnswer(answerPermState).when(mBridge)
                .getManageExternalStoragePermState(nullable(String.class), anyInt());
    }
}
