/*
 * Copyright 2024 The Android Open Source Project
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

package com.android.settings.development.linuxterminal;

import static com.android.settings.development.linuxterminal.EnableLinuxTerminalPreferenceController.TERMINAL_PACKAGE_NAME_RESID;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import androidx.preference.PreferenceScreen;

import com.android.settings.widget.SettingsMainSwitchPreference;
import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.ParameterizedRobolectricTestRunner;

import java.util.Arrays;
import java.util.List;

/** Tests {@link EnableLinuxTerminalPreferenceController} */
@RunWith(ParameterizedRobolectricTestRunner.class)
public class EnableLinuxTerminalPreferenceControllerTest {

    /** Defines parameters for parameterized test */
    @ParameterizedRobolectricTestRunner.Parameters(
            name = "isPrimaryUser={0}, installed={1}, enabled={2}")
    public static List<Object[]> params() {
        return Arrays.asList(
                new Object[] {true, true, false},
                new Object[] {true, true, true},
                new Object[] {false, false, false},
                new Object[] {false, true, false},
                new Object[] {false, true, true});
    }

    @ParameterizedRobolectricTestRunner.Parameter(0)
    public boolean mIsPrimaryUser;

    @ParameterizedRobolectricTestRunner.Parameter(1)
    public boolean mInstalled;

    @ParameterizedRobolectricTestRunner.Parameter(2)
    public boolean mEnabled;

    @Mock private Context mContext;
    @Mock private Context mUserContext;
    @Mock private SettingsMainSwitchPreference mPreference;
    @Mock private PreferenceScreen mPreferenceScreen;
    @Mock private PackageManager mPackageManager;
    @Mock private PackageInfo mPackageInfo;

    private String mTerminalPackageName = "com.android.virtualization.terminal";
    private EnableLinuxTerminalPreferenceController mController;

    @Before
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);
        doReturn(mTerminalPackageName)
                .when(mUserContext)
                .getString(eq(TERMINAL_PACKAGE_NAME_RESID));

        doReturn(mPackageManager).when(mUserContext).getPackageManager();
        doReturn(mInstalled ? mPackageInfo : null)
                .when(mPackageManager)
                .getPackageInfo(eq(mTerminalPackageName), anyInt());
        doReturn(
                        mEnabled
                                ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                                : PackageManager.COMPONENT_ENABLED_STATE_DEFAULT)
                .when(mPackageManager)
                .getApplicationEnabledSetting(eq(mTerminalPackageName));

        mController =
                new EnableLinuxTerminalPreferenceController(mContext, mUserContext, mIsPrimaryUser);

        doReturn(mPreference)
                .when(mPreferenceScreen)
                .findPreference(eq(mController.getPreferenceKey()));
        mController.displayPreference(mPreferenceScreen);
        mController.updateState(mPreference);
    }

    @Test
    public void isAvailable_returnTrue() {
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void onCheckedChanged_whenChecked_turnOnTerminal() {
        assumeTrue(mInstalled);

        mController.onCheckedChanged(/* buttonView= */ null, /* isChecked= */ true);

        verify(mPackageManager)
                .setApplicationEnabledSetting(
                        mTerminalPackageName,
                        PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                        /* flags= */ 0);
    }

    @Test
    public void onCheckedChanged_whenUnchecked_turnOffTerminal() {
        assumeTrue(mInstalled);

        mController.onCheckedChanged(/* buttonView= */ null, /* isChecked= */ false);

        verify(mPackageManager)
                .setApplicationEnabledSetting(
                        mTerminalPackageName,
                        PackageManager.COMPONENT_ENABLED_STATE_DEFAULT,
                        /* flags= */ 0);

        verify(mPackageManager).clearApplicationUserData(mTerminalPackageName, null);
    }

    @Test
    public void updateState_enabled() {
        verify(mPreference).setEnabled(/* enabled= */ true);
    }

    @Test
    public void updateState_whenEnabled_checked() {
        assumeTrue(mEnabled);

        verify(mPreference).setChecked(/* checked= */ true);
    }

    @Test
    public void updateState_whenDisabled_unchecked() {
        assumeFalse(mEnabled);

        verify(mPreference).setChecked(/* checked= */ false);
    }

    @Test
    public void updateState_withProfileWhenAllowed_enabledByAdmin() {
        assumeFalse(mIsPrimaryUser);
        assumeTrue(mInstalled);

        verify(mPreference).setDisabledByAdmin(eq(null));
    }

    @Test
    public void updateState_withProfileWhenNotAllowed_disabledByAdmin() {
        assumeFalse(mIsPrimaryUser);
        assumeFalse(mInstalled);

        verify(mPreference).setDisabledByAdmin(any(EnforcedAdmin.class));
    }
}
