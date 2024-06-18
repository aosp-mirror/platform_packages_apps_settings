/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.wifi.calling;

import static com.android.settings.SettingsActivity.EXTRA_SHOW_FRAGMENT;

import static com.google.common.truth.Truth.assertThat;

import static junit.framework.Assert.assertEquals;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.platform.test.flag.junit.SetFlagsRule;
import android.telephony.CarrierConfigManager;
import android.telephony.NetworkRegistrationInfo;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.telephony.ims.ImsMmTelManager;
import android.view.View;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.internal.telephony.flags.Flags;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.network.ims.MockWifiCallingQueryImsState;
import com.android.settings.network.ims.WifiCallingQueryImsState;
import com.android.settings.testutils.shadow.ShadowFragment;
import com.android.settings.widget.SettingsMainSwitchBar;
import com.android.settings.widget.SettingsMainSwitchPreference;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

@Config(shadows = ShadowFragment.class)
@RunWith(RobolectricTestRunner.class)
public class WifiCallingSettingsForSubTest {
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    private static final int SUB_ID = 2;

    private static final String SWITCH_BAR = "wifi_calling_switch_bar";
    private static final String BUTTON_WFC_MODE = "wifi_calling_mode";
    private static final String BUTTON_WFC_ROAMING_MODE = "wifi_calling_roaming_mode";
    private static final String PREFERENCE_NO_OPTIONS_DESC = "no_options_description";
    private static final String TEST_EMERGENCY_ADDRESS_CARRIER_APP =
            "com.android.settings/.wifi.calling.TestEmergencyAddressCarrierApp";

    private TestFragment mFragment;
    private Context mContext;
    private final PersistableBundle mBundle = new PersistableBundle();

    private MockWifiCallingQueryImsState mQueryImsState;
    private SettingsMainSwitchBar mSwitchBar;

    @Mock
    private static CarrierConfigManager sCarrierConfigManager;
    @Mock
    private CarrierConfigManager mMockConfigManager;
    @Mock
    private ImsMmTelManager mImsMmTelManager;
    @Mock
    private TelephonyManager mTelephonyManager;
    @Mock
    private PreferenceScreen mPreferenceScreen;
    @Mock
    private SettingsActivity mActivity;
    @Mock
    private View mView;
    @Mock
    private SettingsMainSwitchPreference mSwitchBarPreference;
    @Mock
    private LinkifyDescriptionPreference mDescriptionView;
    @Mock
    private ListWithEntrySummaryPreference mButtonWfcMode;
    @Mock
    private ListWithEntrySummaryPreference mButtonWfcRoamingMode;
    @Mock
    private Preference mUpdateAddress;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        mContext = RuntimeEnvironment.application;
        doReturn(mContext.getTheme()).when(mActivity).getTheme();

        mFragment = spy(new TestFragment());
        mFragment.setSwitchBar(mSwitchBarPreference);
        doReturn(mActivity).when(mFragment).getActivity();
        doReturn(mContext).when(mFragment).getContext();
        doReturn(mock(Intent.class)).when(mActivity).getIntent();
        doReturn(mContext.getResources()).when(mFragment).getResources();
        doReturn(mPreferenceScreen).when(mFragment).getPreferenceScreen();
        doReturn(mTelephonyManager).when(mTelephonyManager).createForSubscriptionId(anyInt());
        final Bundle bundle = new Bundle();
        when(mFragment.getArguments()).thenReturn(bundle);
        doNothing().when(mFragment).addPreferencesFromResource(anyInt());
        doNothing().when(mFragment).finish();
        doReturn(mView).when(mFragment).getView();

        mSwitchBar = new SettingsMainSwitchBar(mContext);
        doReturn(mSwitchBar).when(mView).findViewById(R.id.switch_bar);

        mQueryImsState = new MockWifiCallingQueryImsState(mContext, SUB_ID);

        doReturn(mImsMmTelManager).when(mFragment).getImsMmTelManager();
        mQueryImsState.setIsProvisionedOnDevice(true);
        mQueryImsState.setIsEnabledByPlatform(true);
        mQueryImsState.setIsEnabledByUser(true);
        mQueryImsState.setIsTtyOnVolteEnabled(true);
        doReturn(ImsMmTelManager.WIFI_MODE_WIFI_PREFERRED)
                .when(mImsMmTelManager).getVoWiFiModeSetting();
        doReturn(ImsMmTelManager.WIFI_MODE_WIFI_PREFERRED)
                .when(mImsMmTelManager).getVoWiFiRoamingModeSetting();

        doReturn(mBundle).when(sCarrierConfigManager).getConfigForSubId(anyInt());
        setDefaultCarrierConfigValues();

        doReturn(sCarrierConfigManager).when(mActivity).getSystemService(
                CarrierConfigManager.class);
        doReturn(mContext.getResources()).when(mFragment).getResourcesForSubId();
        doNothing().when(mFragment).startActivityForResult(any(Intent.class), anyInt());

        mFragment.onAttach(mContext);
        mFragment.onCreate(null);
        mFragment.onActivityCreated(null);
        mSetFlagsRule.disableFlags(Flags.FLAG_CARRIER_ENABLED_SATELLITE_FLAG);
    }

    private void setDefaultCarrierConfigValues() {
        mBundle.putBoolean(
                CarrierConfigManager.KEY_USE_WFC_HOME_NETWORK_MODE_IN_ROAMING_NETWORK_BOOL, false);
        mBundle.putBoolean(CarrierConfigManager.KEY_EDITABLE_WFC_MODE_BOOL, true);
        mBundle.putBoolean(CarrierConfigManager.KEY_EDITABLE_WFC_ROAMING_MODE_BOOL, true);
        mBundle.putString(
                CarrierConfigManager.KEY_WFC_EMERGENCY_ADDRESS_CARRIER_APP_STRING,
                TEST_EMERGENCY_ADDRESS_CARRIER_APP);
    }

    @Test
    public void getHelpResource_shouldReturn0() {
        assertThat(mFragment.getHelpResource()).isEqualTo(0);
    }

    @Test
    public void onResume_provisioningAllowed_shouldNotFinish() {
        // Call onResume while provisioning is allowed.
        mFragment.onResume();

        // Verify that finish() is not called.
        verify(mFragment, never()).finish();
    }

    @Test
    public void onResume_provisioningDisallowed_shouldFinish() {
        // Call onResume while provisioning is disallowed.
        mQueryImsState.setIsProvisionedOnDevice(false);
        mFragment.onResume();

        // Verify that finish() is called
        verify(mFragment).finish();
    }

    @Test
    public void onResumeOnPause_provisioningCallbackRegistration() throws Exception {
        // Verify that provisioning callback is registered after call to onResume().
        mFragment.onResume();
        verify(mFragment).registerProvisioningChangedCallback();

        // Verify that provisioning callback is unregistered after call to onPause.
        mFragment.onPause();
        verify(mFragment).unregisterProvisioningChangedCallback();
    }

    @Test
    public void onResume_useWfcHomeModeConfigFalseAndEditable_shouldShowWfcRoaming() {
        // Call onResume to update the WFC roaming preference.
        mFragment.onResume();

        // Check that WFC roaming preference is shown.
        verify(mButtonWfcRoamingMode, times(1)).setVisible(true);
    }

    @Test
    public void onResume_useWfcHomeModeConfigTrueAndEditable_shouldHideWfcRoaming() {
        mBundle.putBoolean(
                CarrierConfigManager.KEY_USE_WFC_HOME_NETWORK_MODE_IN_ROAMING_NETWORK_BOOL, true);
        mBundle.putBoolean(CarrierConfigManager.KEY_EDITABLE_WFC_ROAMING_MODE_BOOL, true);

        // Call onResume to update the WFC roaming preference.
        mFragment.onResume();

        // Check that WFC roaming preference is hidden.
        verify(mButtonWfcRoamingMode, times(1)).setVisible(false);
    }

    @Test
    public void onResume_useWfcHomeModeConfigFalseAndNotEditable_shouldHideWfcRoaming() {
        mBundle.putBoolean(
                CarrierConfigManager.KEY_USE_WFC_HOME_NETWORK_MODE_IN_ROAMING_NETWORK_BOOL, false);
        mBundle.putBoolean(CarrierConfigManager.KEY_EDITABLE_WFC_ROAMING_MODE_BOOL, false);

        // Call onResume to update the WFC roaming preference.
        mFragment.onResume();

        // Check that WFC roaming preference is hidden.
        verify(mButtonWfcRoamingMode, times(1)).setVisible(false);
    }

    @Test
    public void onResume_overrideWfcRoamingModeWhileUsingNTN_shouldDisableWfcRoaming() {
        mSetFlagsRule.enableFlags(Flags.FLAG_CARRIER_ENABLED_SATELLITE_FLAG);
        mBundle.putBoolean(
                CarrierConfigManager.KEY_USE_WFC_HOME_NETWORK_MODE_IN_ROAMING_NETWORK_BOOL, false);
        mBundle.putBoolean(CarrierConfigManager.KEY_EDITABLE_WFC_ROAMING_MODE_BOOL, true);
        mBundle.putBoolean(
                CarrierConfigManager.KEY_OVERRIDE_WFC_ROAMING_MODE_WHILE_USING_NTN_BOOL, true);

        // Phone connected to non-terrestrial network
        NetworkRegistrationInfo nri = new NetworkRegistrationInfo.Builder()
                .setIsNonTerrestrialNetwork(true)
                .build();
        ServiceState ss = new ServiceState();
        ss.addNetworkRegistrationInfo(nri);
        doReturn(ss).when(mTelephonyManager).getServiceState();

        // Call onResume to update the WFC roaming preference.
        mFragment.onResume();

        // Check that WFC roaming preference is visible but disabled
        verify(mButtonWfcRoamingMode, times(1)).setEnabled(false);
        verify(mButtonWfcRoamingMode, times(1)).setVisible(true);
    }

    @Test
    public void onResume_useWfcHomeModeConfigTrueAndNotEditable_shouldHideWfcRoaming() {
        mBundle.putBoolean(
                CarrierConfigManager.KEY_USE_WFC_HOME_NETWORK_MODE_IN_ROAMING_NETWORK_BOOL, true);
        mBundle.putBoolean(CarrierConfigManager.KEY_EDITABLE_WFC_ROAMING_MODE_BOOL, false);

        // Call onResume to update the WFC roaming preference.
        mFragment.onResume();

        // Check that WFC roaming preference is hidden.
        verify(mButtonWfcRoamingMode, times(1)).setVisible(false);
    }

    @Test
    public void onPreferenceChange_useWfcHomeModeConfigFalse_shouldNotSetWfcRoaming() {
        // Call onResume to update carrier config values.
        mFragment.onResume();

        // Set the WFC home mode.
        mFragment.onPreferenceChange(mButtonWfcMode,
                String.valueOf(ImsMmTelManager.WIFI_MODE_CELLULAR_PREFERRED));

        // Check that only WFC home mode is set.
        verify(mImsMmTelManager, times(1)).setVoWiFiModeSetting(
                eq(ImsMmTelManager.WIFI_MODE_CELLULAR_PREFERRED));
        verify(mImsMmTelManager, never()).setVoWiFiRoamingModeSetting(
                eq(ImsMmTelManager.WIFI_MODE_CELLULAR_PREFERRED));
    }

    @Test
    public void onPreferenceChange_useWfcHomeModeConfigTrue_shouldSetWfcRoaming() {
        mBundle.putBoolean(
                CarrierConfigManager.KEY_USE_WFC_HOME_NETWORK_MODE_IN_ROAMING_NETWORK_BOOL, true);

        // Call onResume to update carrier config values.
        mFragment.onResume();

        // Set the WFC home mode.
        mFragment.onPreferenceChange(mButtonWfcMode,
                String.valueOf(ImsMmTelManager.WIFI_MODE_CELLULAR_PREFERRED));

        // Check that both WFC home mode and roaming mode are set.
        verify(mImsMmTelManager, times(1)).setVoWiFiModeSetting(
                eq(ImsMmTelManager.WIFI_MODE_CELLULAR_PREFERRED));
        verify(mImsMmTelManager, times(1)).setVoWiFiRoamingModeSetting(
                eq(ImsMmTelManager.WIFI_MODE_CELLULAR_PREFERRED));
    }

    @Test
    public void onSwitchChanged_enableSetting_shouldLaunchWfcDisclaimerFragment() {
        final ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);

        mFragment.onCheckedChanged(null, true);

        // Check the WFC disclaimer fragment is launched.
        verify(mFragment).startActivityForResult(intentCaptor.capture(),
                eq(WifiCallingSettingsForSub.REQUEST_CHECK_WFC_DISCLAIMER));
        final Intent intent = intentCaptor.getValue();
        assertThat(intent.getStringExtra(EXTRA_SHOW_FRAGMENT))
                .isEqualTo(WifiCallingDisclaimerFragment.class.getName());
    }

    @Test
    public void onActivityResult_finishWfcDisclaimerFragment_shouldLaunchCarrierActivity() {
        final ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);

        // Emulate the WfcDisclaimerActivity finish.
        mFragment.onActivityResult(WifiCallingSettingsForSub.REQUEST_CHECK_WFC_DISCLAIMER,
                Activity.RESULT_OK, null);

        // Check the WFC emergency address activity is launched.
        verify(mFragment).startActivityForResult(intentCaptor.capture(),
                eq(WifiCallingSettingsForSub.REQUEST_CHECK_WFC_EMERGENCY_ADDRESS));
        final Intent intent = intentCaptor.getValue();
        assertEquals(intent.getComponent(), ComponentName.unflattenFromString(
                TEST_EMERGENCY_ADDRESS_CARRIER_APP));
    }

    @Test
    public void onActivityResult_finishCarrierActivity_shouldShowWfcPreference() {
        ReflectionHelpers.setField(mFragment, "mButtonWfcMode", mButtonWfcMode);
        ReflectionHelpers.setField(mFragment, "mButtonWfcRoamingMode", mButtonWfcRoamingMode);
        ReflectionHelpers.setField(mFragment, "mUpdateAddress", mUpdateAddress);

        mFragment.onActivityResult(WifiCallingSettingsForSub.REQUEST_CHECK_WFC_EMERGENCY_ADDRESS,
                Activity.RESULT_OK, null);

        // Check the WFC preferences is added.
        verify(mButtonWfcMode).setVisible(true);
        verify(mButtonWfcRoamingMode).setVisible(true);
        verify(mUpdateAddress).setVisible(true);
        // Check the WFC enable request.
        verify(mImsMmTelManager).setVoWiFiSettingEnabled(true);
    }

    @Test
    public void onSwitchChanged_disableSetting_shouldNotLaunchWfcDisclaimerFragment() {
        mFragment.onCheckedChanged(null, false);

        // Check the WFC disclaimer fragment is not launched.
        verify(mFragment, never()).startActivityForResult(any(Intent.class), anyInt());
    }

    protected class TestFragment extends WifiCallingSettingsForSub {
        private SettingsMainSwitchPreference mSwitchPref;

        protected void setSwitchBar(SettingsMainSwitchPreference switchPref) {
            mSwitchPref = switchPref;
        }

        @Override
        public <T extends Preference> T findPreference(CharSequence key) {
            if (SWITCH_BAR.equals(key)) {
                return (T) mSwitchPref;
            }
            if (BUTTON_WFC_MODE.equals(key)) {
                return (T) mButtonWfcMode;
            }
            if (BUTTON_WFC_ROAMING_MODE.equals(key)) {
                return (T) mButtonWfcRoamingMode;
            }
            if (PREFERENCE_NO_OPTIONS_DESC.equals(key)) {
                return (T) mDescriptionView;
            }
            return (T) mock(ListWithEntrySummaryPreference.class);
        }

        @Override
        protected Object getSystemService(final String name) {
            switch (name) {
                case Context.TELEPHONY_SERVICE:
                    return mTelephonyManager;
                case Context.CARRIER_CONFIG_SERVICE:
                    return sCarrierConfigManager;
                default:
                    return null;
            }
        }

        @Override
        TelephonyManager getTelephonyManagerForSub(int subId) {
            return mTelephonyManager;
        }

        @Override
        WifiCallingQueryImsState queryImsState(int subId) {
            return mQueryImsState;
        }

        @Override
        void showAlert(Intent intent) {
        }
    }
}
