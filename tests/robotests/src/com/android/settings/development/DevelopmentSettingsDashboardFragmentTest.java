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

package com.android.settings.development;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.content.Context;
import android.hardware.biometrics.BiometricManager;
import android.hardware.biometrics.Flags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.provider.SearchIndexableResource;
import android.provider.Settings;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.biometrics.IdentityCheckBiometricErrorDialog;
import com.android.settings.password.ConfirmDeviceCredentialActivity;
import com.android.settings.testutils.shadow.ShadowAlertDialogCompat;
import com.android.settings.testutils.shadow.ShadowUserManager;
import com.android.settings.widget.SettingsMainSwitchBar;
import com.android.settingslib.development.AbstractEnableAdbPreferenceController;
import com.android.settingslib.development.DevelopmentSettingsEnabler;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowBiometricManager;
import org.robolectric.shadows.androidx.fragment.FragmentController;
import org.robolectric.util.ReflectionHelpers;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        ShadowAlertDialogCompat.class,
        ShadowUserManager.class,
        ShadowUserManager.class,
        ShadowBiometricManager.class,
})
public class DevelopmentSettingsDashboardFragmentTest {
    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private Context mContext;
    private ShadowUserManager mShadowUserManager;
    private ShadowBiometricManager mShadowBiometricManager;
    private DevelopmentSettingsDashboardFragment mDashboard;
    private SettingsMainSwitchBar mSwitchBar;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mSwitchBar = new SettingsMainSwitchBar(mContext);
        mDashboard = spy(new DevelopmentSettingsDashboardFragment());
        ReflectionHelpers.setField(mDashboard, "mSwitchBar", mSwitchBar);
        mShadowUserManager = Shadow.extract(mContext.getSystemService(Context.USER_SERVICE));
        mShadowUserManager.setIsAdminUser(true);
        mShadowBiometricManager = Shadow.extract(mContext.getSystemService(
                Context.BIOMETRIC_SERVICE));
        mShadowBiometricManager.setCanAuthenticate(false);
        //TODO(b/352603684): Should be Authenticators.MANDATORY_BIOMETRICS,
        // but it is not supported by ShadowBiometricManager
        mShadowBiometricManager.setAuthenticatorType(
                BiometricManager.Authenticators.BIOMETRIC_STRONG);
    }

    @After
    public void tearDown() {
        ShadowEnableDevelopmentSettingWarningDialog.reset();
    }

    @Test
    public void shouldNotHaveHelpResource() {
        assertThat(mDashboard.getHelpResource()).isEqualTo(0);
    }

    @Test
    public void shouldLogAsFeatureFlagPage() {
        assertThat(mDashboard.getMetricsCategory())
                .isEqualTo(MetricsProto.MetricsEvent.DEVELOPMENT);
    }

    @Test
    public void searchIndex_shouldIndexFromPrefXml() {
        final List<SearchIndexableResource> index =
                DevelopmentSettingsDashboardFragment.SEARCH_INDEX_DATA_PROVIDER
                        .getXmlResourcesToIndex(RuntimeEnvironment.application, true);

        assertThat(index.size()).isEqualTo(1);
        assertThat(index.get(0).xmlResId).isEqualTo(R.xml.development_settings);
    }

    @Test
    @Ignore
    public void searchIndex_pageDisabledBySetting_shouldAddAllKeysToNonIndexable() {
        final Context appContext = RuntimeEnvironment.application;
        DevelopmentSettingsEnabler.setDevelopmentSettingsEnabled(appContext, false);

        final List<String> nonIndexableKeys =
                DevelopmentSettingsDashboardFragment.SEARCH_INDEX_DATA_PROVIDER
                        .getNonIndexableKeys(appContext);

        assertThat(nonIndexableKeys).contains("enable_adb");
    }

    @Test
    @Ignore
    public void searchIndex_pageDisabledForNonAdmin_shouldAddAllKeysToNonIndexable() {
        final Context appContext = RuntimeEnvironment.application;
        DevelopmentSettingsEnabler.setDevelopmentSettingsEnabled(appContext, true);
        mShadowUserManager.setIsAdminUser(false);
        mShadowUserManager.setIsDemoUser(false);

        final List<String> nonIndexableKeys =
                DevelopmentSettingsDashboardFragment.SEARCH_INDEX_DATA_PROVIDER
                        .getNonIndexableKeys(appContext);

        assertThat(nonIndexableKeys).contains("enable_adb");
    }

    @Test
    @Ignore
    @Config(shadows = {
            ShadowPictureColorModePreferenceController.class,
            ShadowAdbPreferenceController.class,
            ShadowClearAdbKeysPreferenceController.class,
            ShadowWirelessDebuggingPreferenceController.class
    })
    public void searchIndex_pageEnabled_shouldNotAddKeysToNonIndexable() {
        final Context appContext = RuntimeEnvironment.application;
        DevelopmentSettingsEnabler.setDevelopmentSettingsEnabled(appContext, true);

        final List<String> nonIndexableKeys =
                DevelopmentSettingsDashboardFragment.SEARCH_INDEX_DATA_PROVIDER
                        .getNonIndexableKeys(appContext);

        assertThat(nonIndexableKeys).doesNotContain("development_prefs_screen");
    }

    @Test
    @Ignore
    @Config(shadows = ShadowEnableDevelopmentSettingWarningDialog.class)
    public void onSwitchChanged_sameState_shouldDoNothing() {
        when(mDashboard.getContext()).thenReturn(mContext);
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0);

        mDashboard.onCheckedChanged(null, false /* isChecked */);
        assertThat(ShadowEnableDevelopmentSettingWarningDialog.mShown).isFalse();
    }

    @Test
    @Ignore
    @Config(shadows = ShadowEnableDevelopmentSettingWarningDialog.class)
    public void onSwitchChanged_turnOn_shouldShowWarningDialog() {
        when(mDashboard.getContext()).thenReturn(mContext);
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0);

        mDashboard.onCheckedChanged(null, true /* isChecked */);
        assertThat(ShadowEnableDevelopmentSettingWarningDialog.mShown).isTrue();
    }

    @Test
    @Config(shadows = ShadowEnableDevelopmentSettingWarningDialog.class)
    @EnableFlags(Flags.FLAG_MANDATORY_BIOMETRICS)
    public void onSwitchChanged_turnOn_shouldLaunchBiometricPromptIfMandatoryBiometricsEffective() {
        when(mDashboard.getContext()).thenReturn(mContext);
        doNothing().when(mDashboard).startActivityForResult(any(),
                eq(DevelopmentSettingsDashboardFragment.REQUEST_BIOMETRIC_PROMPT));

        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0);
        mShadowBiometricManager.setCanAuthenticate(true);
        mDashboard.onCheckedChanged(null, true /* isChecked */);

        assertThat(mSwitchBar.isChecked()).isFalse();
        verify(mDashboard).startActivityForResult(any(),
                eq(DevelopmentSettingsDashboardFragment.REQUEST_BIOMETRIC_PROMPT));
        assertThat(ShadowEnableDevelopmentSettingWarningDialog.mShown).isFalse();
    }

    @Test
    @Config(shadows = ShadowEnableDevelopmentSettingWarningDialog.class)
    @EnableFlags(Flags.FLAG_MANDATORY_BIOMETRICS)
    public void onActivityResult_requestBiometricPrompt_shouldShowWarningDialog() {
        when(mDashboard.getContext()).thenReturn(mContext);

        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0);
        mDashboard.onActivityResult(DevelopmentSettingsDashboardFragment.REQUEST_BIOMETRIC_PROMPT,
                Activity.RESULT_OK, null);
        mDashboard.onCheckedChanged(null, true /* isChecked */);

        assertThat(mSwitchBar.isChecked()).isTrue();
        assertThat(ShadowEnableDevelopmentSettingWarningDialog.mShown).isTrue();
    }

    @Test
    @Config(shadows = ShadowIdentityCheckBiometricErrorDialog.class)
    @Ignore("b/354820314")
    @EnableFlags(Flags.FLAG_MANDATORY_BIOMETRICS)
    public void onActivityResult_requestBiometricPrompt_showErrorDialog() {
        when(mDashboard.getContext()).thenReturn(mContext);

        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0);
        mDashboard.onActivityResult(DevelopmentSettingsDashboardFragment.REQUEST_BIOMETRIC_PROMPT,
                ConfirmDeviceCredentialActivity.BIOMETRIC_LOCKOUT_ERROR_RESULT, null);

        assertThat(mSwitchBar.isChecked()).isFalse();
        assertThat(ShadowIdentityCheckBiometricErrorDialog.sShown).isTrue();
    }

    @Test
    @Ignore
    @Config(shadows = ShadowEnableDevelopmentSettingWarningDialog.class)
    public void onSwitchChanged_turnOff_shouldTurnOff() {
        when(mDashboard.getContext()).thenReturn(mContext);
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 1);

        mDashboard.onCheckedChanged(null, false /* isChecked */);

        assertThat(ShadowEnableDevelopmentSettingWarningDialog.mShown).isFalse();
        assertThat(DevelopmentSettingsEnabler.isDevelopmentSettingsEnabled(mContext)).isFalse();
    }

    @Test
    @Ignore
    @Config(shadows = ShadowDisableDevSettingsDialogFragment.class)
    public void onSwitchChanged_turnOff_andOffloadIsNotDefaultValue_shouldShowWarningDialog() {
        final BluetoothA2dpHwOffloadPreferenceController controller =
                mock(BluetoothA2dpHwOffloadPreferenceController.class);
        when(mDashboard.getContext()).thenReturn(mContext);
        when(mDashboard.getDevelopmentOptionsController(
                BluetoothA2dpHwOffloadPreferenceController.class)).thenReturn(controller);
        when(controller.isDefaultValue()).thenReturn(false);
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 1);

        mDashboard.onCheckedChanged(null, false /* isChecked */);

        AlertDialog dialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        assertThat(dialog).isNotNull();
        ShadowAlertDialogCompat shadowDialog = ShadowAlertDialogCompat.shadowOf(dialog);
        assertThat(shadowDialog.getTitle()).isEqualTo(
                mContext.getString(R.string.bluetooth_disable_hw_offload_dialog_title));
        assertThat(shadowDialog.getMessage()).isEqualTo(
                mContext.getString(R.string.bluetooth_disable_hw_offload_dialog_message));
    }

    @Test
    public void onOemUnlockDialogConfirmed_shouldCallControllerOemConfirmed() {
        final OemUnlockPreferenceController controller = mock(OemUnlockPreferenceController.class);
        doReturn(controller).when(mDashboard)
                .getDevelopmentOptionsController(OemUnlockPreferenceController.class);
        mDashboard.onOemUnlockDialogConfirmed();
        verify(controller).onOemUnlockConfirmed();
    }

    @Test
    public void onOemUnlockDialogConfirmed_shouldCallControllerOemDismissed() {
        final OemUnlockPreferenceController controller = mock(OemUnlockPreferenceController.class);
        doReturn(controller).when(mDashboard)
                .getDevelopmentOptionsController(OemUnlockPreferenceController.class);
        mDashboard.onOemUnlockDialogDismissed();
        verify(controller).onOemUnlockDismissed();
    }

    @Test
    public void onAdbDialogConfirmed_shouldCallControllerDialogConfirmed() {
        final AdbPreferenceController controller = mock(AdbPreferenceController.class);
        doReturn(controller).when(mDashboard)
                .getDevelopmentOptionsController(AdbPreferenceController.class);
        mDashboard.onEnableAdbDialogConfirmed();

        verify(controller).onAdbDialogConfirmed();
    }

    @Test
    public void onAdbDialogDismissed_shouldCallControllerOemDismissed() {
        final AdbPreferenceController controller = mock(AdbPreferenceController.class);
        doReturn(controller).when(mDashboard)
                .getDevelopmentOptionsController(AdbPreferenceController.class);
        mDashboard.onEnableAdbDialogDismissed();

        verify(controller).onAdbDialogDismissed();
    }

    @Test
    public void onAdbClearKeysDialogConfirmed_shouldCallControllerDialogConfirmed() {
        final ClearAdbKeysPreferenceController controller =
                mock(ClearAdbKeysPreferenceController.class);
        doReturn(controller).when(mDashboard)
                .getDevelopmentOptionsController(ClearAdbKeysPreferenceController.class);
        mDashboard.onAdbClearKeysDialogConfirmed();

        verify(controller).onClearAdbKeysConfirmed();
    }

    @Test
    public void onDisableLogPersistDialogConfirmed_shouldCallControllerDialogConfirmed() {
        final LogPersistPreferenceController controller =
                mock(LogPersistPreferenceController.class);
        doReturn(controller).when(mDashboard)
                .getDevelopmentOptionsController(LogPersistPreferenceController.class);
        mDashboard.onDisableLogPersistDialogConfirmed();

        verify(controller).onDisableLogPersistDialogConfirmed();
    }

    @Test
    public void onDisableLogPersistDialogRejected_shouldCallControllerDialogRejected() {
        final LogPersistPreferenceController controller =
                mock(LogPersistPreferenceController.class);
        doReturn(controller).when(mDashboard)
                .getDevelopmentOptionsController(LogPersistPreferenceController.class);
        mDashboard.onDisableLogPersistDialogRejected();

        verify(controller).onDisableLogPersistDialogRejected();
    }

    @Test
    public void shouldSkipForInitialSUW_returnTrue() {
        assertThat(mDashboard.shouldSkipForInitialSUW()).isTrue();
    }

    @Implements(EnableDevelopmentSettingWarningDialog.class)
    public static class ShadowEnableDevelopmentSettingWarningDialog {

        static boolean mShown;

        public static void reset() {
            mShown = false;
        }

        @Implementation
        protected static void show(DevelopmentSettingsDashboardFragment host) {
            mShown = true;
        }
    }

    @Implements(IdentityCheckBiometricErrorDialog.class)
    public static class ShadowIdentityCheckBiometricErrorDialog {
        static boolean sShown;
        @Implementation
        public static void showBiometricErrorDialog(FragmentActivity fragmentActivity,
                Utils.BiometricStatus errorCode) {
            sShown = true;
        }
    }

    @Implements(DisableDevSettingsDialogFragment.class)
    public static class ShadowDisableDevSettingsDialogFragment {

        @Implementation
        public static void show(DevelopmentSettingsDashboardFragment host) {
            DisableDevSettingsDialogFragment mFragment =
                    spy(DisableDevSettingsDialogFragment.newInstance());
            FragmentController.setupFragment(mFragment, FragmentActivity.class,
                    0 /* containerViewId */, null /* bundle */);
        }
    }

    @Implements(PictureColorModePreferenceController.class)
    public static class ShadowPictureColorModePreferenceController {
        @Implementation
        protected boolean isAvailable() {
            return true;
        }
    }

    @Implements(AbstractEnableAdbPreferenceController.class)
    public static class ShadowAdbPreferenceController {
        @Implementation
        protected boolean isAvailable() {
            return true;
        }
    }

    @Implements(ClearAdbKeysPreferenceController.class)
    public static class ShadowClearAdbKeysPreferenceController {
        @Implementation
        protected boolean isAvailable() {
            return true;
        }
    }

    @Implements(WirelessDebuggingPreferenceController.class)
    public static class ShadowWirelessDebuggingPreferenceController {
        @Implementation
        protected boolean isAvailable() {
            return true;
        }
    }
}
