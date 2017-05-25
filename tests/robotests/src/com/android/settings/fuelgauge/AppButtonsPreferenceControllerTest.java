/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.fuelgauge;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.ActivityManager;
import android.app.Application;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.UserManager;
import android.widget.Button;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.TestConfig;
import com.android.settings.enterprise.DevicePolicyManagerWrapper;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settingslib.applications.AppUtils;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.applications.instantapps.InstantAppDataProvider;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class AppButtonsPreferenceControllerTest {
    private static final String PACKAGE_NAME = "com.android.settings";
    private static final String RESOURCE_STRING = "string";
    private static final boolean ALL_USERS = false;
    private static final boolean DISABLE_AFTER_INSTALL = true;
    private static final int REQUEST_UNINSTALL = 0;
    private static final int REQUEST_REMOVE_DEVICE_ADMIN = 1;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private SettingsActivity mSettingsActivity;
    @Mock
    private TestFragment mFragment;
    @Mock
    private Lifecycle mLifecycle;
    @Mock
    private ApplicationsState mState;
    @Mock
    private ApplicationsState.AppEntry mAppEntry;
    @Mock
    private ApplicationInfo mAppInfo;
    @Mock
    private PackageManager mPackageManger;
    @Mock
    private DevicePolicyManagerWrapper mDpm;
    @Mock
    private ActivityManager mAm;
    @Mock
    private UserManager mUserManager;
    @Mock
    private Application mApplication;
    @Mock
    private PackageInfo mPackageInfo;
    @Mock
    private Button mUninstallButton;
    @Mock
    private Button mForceStopButton;

    private Intent mUninstallIntent;
    private AppButtonsPreferenceController mController;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        FakeFeatureFactory.setupForTest(mSettingsActivity);
        doReturn(mUserManager).when(mSettingsActivity).getSystemService(Context.USER_SERVICE);
        doReturn(mPackageManger).when(mSettingsActivity).getPackageManager();
        doReturn(mAm).when(mSettingsActivity).getSystemService(Context.ACTIVITY_SERVICE);
        doReturn(mAppEntry).when(mState).getEntry(anyString(), anyInt());
        doReturn(mApplication).when(mSettingsActivity).getApplication();
        when(mSettingsActivity.getResources().getString(anyInt())).thenReturn(RESOURCE_STRING);

        mController = spy(new AppButtonsPreferenceController(mSettingsActivity, mFragment,
                mLifecycle, PACKAGE_NAME, mState, mDpm, mUserManager, mPackageManger,
                REQUEST_UNINSTALL, REQUEST_REMOVE_DEVICE_ADMIN));
        doReturn(false).when(mController).isFallbackPackage(anyString());

        mAppEntry.info = mAppInfo;
        mAppInfo.packageName = PACKAGE_NAME;
        mAppInfo.flags = 0;
        mPackageInfo.packageName = PACKAGE_NAME;
        mPackageInfo.applicationInfo = mAppInfo;

        mController.mUninstallButton = mUninstallButton;
        mController.mForceStopButton = mForceStopButton;
        mController.mPackageInfo = mPackageInfo;

        final ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        Answer<Void> callable = new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Exception {
                mUninstallIntent = captor.getValue();
                return null;
            }
        };
        doAnswer(callable).when(mFragment).startActivityForResult(captor.capture(), anyInt());
    }

    @Test
    public void testRetrieveAppEntry_hasAppEntry_notNull()
            throws PackageManager.NameNotFoundException {
        doReturn(mPackageInfo).when(mPackageManger).getPackageInfo(anyString(), anyInt());

        mController.retrieveAppEntry();

        assertThat(mController.mAppEntry).isNotNull();
        assertThat(mController.mPackageInfo).isNotNull();
    }


    @Test
    public void testRetrieveAppEntry_noAppEntry_null() throws PackageManager.NameNotFoundException {
        doReturn(null).when(mState).getEntry(eq(PACKAGE_NAME), anyInt());
        doReturn(mPackageInfo).when(mPackageManger).getPackageInfo(anyString(), anyInt());

        mController.retrieveAppEntry();

        assertThat(mController.mAppEntry).isNull();
        assertThat(mController.mPackageInfo).isNull();
    }

    @Test
    public void testRetrieveAppEntry_throwException_null() throws
            PackageManager.NameNotFoundException {
        doReturn(mAppEntry).when(mState).getEntry(anyString(), anyInt());
        doThrow(new PackageManager.NameNotFoundException()).when(mPackageManger).getPackageInfo(
                anyString(), anyInt());

        mController.retrieveAppEntry();

        assertThat(mController.mAppEntry).isNotNull();
        assertThat(mController.mPackageInfo).isNull();
    }

    @Test
    public void testUpdateUninstallButton_isSystemApp_handleAsDisableableButton() {
        doReturn(false).when(mController).handleDisableable(any());
        mAppInfo.flags |= ApplicationInfo.FLAG_SYSTEM;

        mController.updateUninstallButton();

        verify(mController).handleDisableable(any());
        verify(mUninstallButton).setEnabled(false);
    }

    @Test
    public void testIsAvailable_nonInstantApp() throws Exception {
        mController.mAppEntry = mAppEntry;
        ReflectionHelpers.setStaticField(AppUtils.class, "sInstantAppDataProvider",
                new InstantAppDataProvider() {
                    @Override
                    public boolean isInstantApp(ApplicationInfo info) {
                        return false;
                    }
                });
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void testIsAvailable_instantApp() throws Exception {
        mController.mAppEntry = mAppEntry;
        ReflectionHelpers.setStaticField(AppUtils.class, "sInstantAppDataProvider",
                new InstantAppDataProvider() {
                    @Override
                    public boolean isInstantApp(ApplicationInfo info) {
                        return true;
                    }
                });
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void testUpdateUninstallButton_isDeviceAdminApp_setButtonDisable() {
        doReturn(true).when(mController).handleDisableable(any());
        mAppInfo.flags |= ApplicationInfo.FLAG_SYSTEM;
        doReturn(true).when(mDpm).packageHasActiveAdmins(anyString());

        mController.updateUninstallButton();

        verify(mController).handleDisableable(any());
        verify(mUninstallButton).setEnabled(false);
    }

    @Test
    public void testUpdateUninstallButton_isProfileOrDeviceOwner_setButtonDisable() {
        doReturn(true).when(mDpm).isDeviceOwnerAppOnAnyUser(anyString());

        mController.updateUninstallButton();

        verify(mUninstallButton).setEnabled(false);
    }

    @Test
    public void testUpdateUninstallButton_isDeviceProvisioningApp_setButtonDisable() {
        doReturn(true).when(mDpm).isDeviceOwnerAppOnAnyUser(anyString());
        when(mSettingsActivity.getResources().getString(anyInt())).thenReturn(PACKAGE_NAME);

        mController.updateUninstallButton();

        verify(mUninstallButton).setEnabled(false);
    }

    @Test
    public void testUpdateUninstallButton_isUninstallInQueue_setButtonDisable() {
        doReturn(true).when(mDpm).isUninstallInQueue(any());

        mController.updateUninstallButton();

        verify(mUninstallButton).setEnabled(false);
    }

    @Test
    public void testUpdateUninstallButton_isHomeAppAndBundled_setButtonDisable() {
        mAppInfo.flags |= ApplicationInfo.FLAG_SYSTEM;
        mController.mHomePackages.add(PACKAGE_NAME);

        mController.updateUninstallButton();

        verify(mUninstallButton).setEnabled(false);
    }

    @Test
    public void testUpdateForceStopButton_HasActiveAdmins_setButtonDisable() {
        doReturn(true).when(mDpm).packageHasActiveAdmins(anyString());

        mController.updateForceStopButton();

        verify(mController).updateForceStopButtonInner(false);
    }

    @Test
    public void testUpdateForceStopButton_AppNotStopped_setButtonEnable() {
        mController.updateForceStopButton();

        verify(mController).updateForceStopButtonInner(true);
    }

    @Test
    public void testUninstallPkg_intentSent() {
        mController.uninstallPkg(PACKAGE_NAME, ALL_USERS, DISABLE_AFTER_INSTALL);

        verify(mFragment).startActivityForResult(any(), eq(REQUEST_UNINSTALL));
        assertThat(
                mUninstallIntent.getBooleanExtra(Intent.EXTRA_UNINSTALL_ALL_USERS, true))
                .isEqualTo(ALL_USERS);
        assertThat(mUninstallIntent.getAction()).isEqualTo(Intent.ACTION_UNINSTALL_PACKAGE);
        assertThat(mController.mDisableAfterUninstall).isEqualTo(DISABLE_AFTER_INSTALL);
    }

    @Test
    public void testForceStopPackage_methodInvokedAndUpdated() {
        final ApplicationsState.AppEntry appEntry = mock(ApplicationsState.AppEntry.class);
        doReturn(appEntry).when(mState).getEntry(anyString(), anyInt());
        doNothing().when(mController).updateForceStopButton();

        mController.forceStopPackage(PACKAGE_NAME);

        verify(mAm).forceStopPackage(PACKAGE_NAME);
        assertThat(mController.mAppEntry).isSameAs(appEntry);
        verify(mController).updateForceStopButton();
    }

    @Test
    public void testHandleDisableable_isHomeApp_notControllable() {
        mController.mHomePackages.add(PACKAGE_NAME);

        final boolean controllable = mController.handleDisableable(mUninstallButton);

        verify(mUninstallButton).setText(R.string.disable_text);
        assertThat(controllable).isFalse();

    }

    @Test
    public void testHandleDisableable_isAppEnabled_controllable() {
        mAppEntry.info.enabled = true;
        mAppEntry.info.enabledSetting = PackageManager.COMPONENT_ENABLED_STATE_DEFAULT;
        doReturn(false).when(mController).isSystemPackage(any(), any(), any());

        final boolean controllable = mController.handleDisableable(mUninstallButton);

        verify(mUninstallButton).setText(R.string.disable_text);
        assertThat(controllable).isTrue();

    }

    @Test
    public void testHandleDisableable_isAppDisabled_controllable() {
        mAppEntry.info.enabled = false;
        mAppEntry.info.enabledSetting = PackageManager.COMPONENT_ENABLED_STATE_DEFAULT;
        doReturn(false).when(mController).isSystemPackage(any(), any(), any());

        final boolean controllable = mController.handleDisableable(mUninstallButton);

        verify(mUninstallButton).setText(R.string.enable_text);
        assertThat(controllable).isTrue();
    }

    @Test
    public void testRefreshUi_packageNull_shouldNotCrash() {
        mController.mPackageName = null;

        // Should not crash in this method
        assertThat(mController.refreshUi()).isFalse();
    }

    /**
     * The test fragment which implements
     * {@link ButtonActionDialogFragment.AppButtonsDialogListener}
     */
    private static class TestFragment extends Fragment implements
            ButtonActionDialogFragment.AppButtonsDialogListener {

        @Override
        public void handleDialogClick(int type) {
            // Do nothing
        }
    }
}
