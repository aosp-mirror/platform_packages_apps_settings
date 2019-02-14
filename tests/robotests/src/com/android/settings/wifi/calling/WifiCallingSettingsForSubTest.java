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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.telephony.CarrierConfigManager;
import android.telephony.TelephonyManager;
import android.telephony.ims.ProvisioningManager;
import android.view.View;
import android.widget.TextView;

import androidx.preference.ListPreference;
import androidx.preference.PreferenceScreen;

import com.android.ims.ImsConfig;
import com.android.ims.ImsException;
import com.android.ims.ImsManager;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.widget.SwitchBar;
import com.android.settings.widget.ToggleSwitch;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
public class WifiCallingSettingsForSubTest {
    private static final String BUTTON_WFC_MODE = "wifi_calling_mode";
    private static final String BUTTON_WFC_ROAMING_MODE = "wifi_calling_roaming_mode";

    private TestFragment mFragment;
    private Context mContext;
    private TextView mEmptyView;
    private final PersistableBundle mBundle = new PersistableBundle();

    @Mock private static CarrierConfigManager sCarrierConfigManager;
    @Mock private ImsManager mImsManager;
    @Mock private TelephonyManager mTelephonyManager;
    @Mock private PreferenceScreen mPreferenceScreen;
    @Mock private SettingsActivity mActivity;
    @Mock private SwitchBar mSwitchBar;
    @Mock private ToggleSwitch mToggleSwitch;
    @Mock private View mView;
    @Mock private ImsConfig mImsConfig;
    @Mock private ListPreference mButtonWfcMode;
    @Mock private ListPreference mButtonWfcRoamingMode;

    @Before
    public void setUp() throws NoSuchFieldException, ImsException {
        MockitoAnnotations.initMocks(this);

        mContext = RuntimeEnvironment.application;
        doReturn(mContext.getTheme()).when(mActivity).getTheme();

        mFragment = spy(new TestFragment());
        doReturn(mActivity).when(mFragment).getActivity();
        doReturn(mContext).when(mFragment).getContext();
        doReturn(mock(Intent.class)).when(mActivity).getIntent();
        doReturn(mContext.getResources()).when(mFragment).getResources();
        doReturn(mPreferenceScreen).when(mFragment).getPreferenceScreen();
        doReturn(mTelephonyManager).when(mTelephonyManager).createForSubscriptionId(anyInt());
        final Bundle bundle = new Bundle();
        when(mFragment.getArguments()).thenReturn(bundle);
        doNothing().when(mFragment).addPreferencesFromResource(anyInt());
        doReturn(mock(ListPreference.class)).when(mFragment).findPreference(any());
        doReturn(mButtonWfcMode).when(mFragment).findPreference(BUTTON_WFC_MODE);
        doReturn(mButtonWfcRoamingMode).when(mFragment).findPreference(BUTTON_WFC_ROAMING_MODE);
        doNothing().when(mFragment).finish();
        doReturn(mView).when(mFragment).getView();

        mEmptyView = new TextView(mContext);
        doReturn(mEmptyView).when(mView).findViewById(android.R.id.empty);

        ReflectionHelpers.setField(mSwitchBar, "mSwitch", mToggleSwitch);
        doReturn(mSwitchBar).when(mView).findViewById(R.id.switch_bar);

        doReturn(mImsManager).when(mFragment).getImsManager();
        doReturn(mImsConfig).when(mImsManager).getConfigInterface();
        doReturn(true).when(mImsManager).isWfcProvisionedOnDevice();
        doReturn(true).when(mImsManager).isWfcEnabledByUser();
        doReturn(true).when(mImsManager).isNonTtyOrTtyOnVolteEnabled();
        doReturn(ImsConfig.WfcModeFeatureValueConstants.WIFI_PREFERRED)
                .when(mImsManager).getWfcMode(anyBoolean());

        doReturn(mBundle).when(sCarrierConfigManager).getConfigForSubId(anyInt());
        setDefaultCarrierConfigValues();

        mFragment.onAttach(mContext);
        mFragment.onCreate(null);
        mFragment.onActivityCreated(null);
    }

    private void setDefaultCarrierConfigValues() {
        mBundle.putBoolean(
                CarrierConfigManager.KEY_USE_WFC_HOME_NETWORK_MODE_IN_ROAMING_NETWORK_BOOL, false);
        mBundle.putBoolean(CarrierConfigManager.KEY_EDITABLE_WFC_MODE_BOOL, true);
        mBundle.putBoolean(CarrierConfigManager.KEY_EDITABLE_WFC_ROAMING_MODE_BOOL, true);
    }

    @Test
    public void getHelpResource_shouldReturn0() {
        assertThat(mFragment.getHelpResource()).isEqualTo(0);
    }

    @Test
    public void onResume_provisioningAllowed_shouldNotFinish() throws ImsException {
        // Call onResume while provisioning is allowed.
        mFragment.onResume();

        // Verify that finish() is not called.
        verify(mFragment, never()).finish();
    }

    @Test
    public void onResume_provisioningDisallowed_shouldFinish() {
        // Call onResume while provisioning is disallowed.
        doReturn(false).when(mImsManager).isWfcProvisionedOnDevice();
        mFragment.onResume();

        // Verify that finish() is called
        verify(mFragment).finish();
    }

    @Test
    public void onResumeOnPause_provisioningCallbackRegistration() throws ImsException {
        // Verify that provisioning callback is registered after call to onResume().
        mFragment.onResume();
        verify(mImsConfig).addConfigCallback(any(ProvisioningManager.Callback.class));

        // Verify that provisioning callback is unregistered after call to onPause.
        mFragment.onPause();
        verify(mImsConfig).removeConfigCallback(any());
    }

    @Test
    public void onResume_useWfcHomeModeConfigFalseAndEditable_shouldShowWfcRoaming() {
        // Call onResume to update the WFC roaming preference.
        mFragment.onResume();

        // Check that WFC roaming preference is shown.
        verify(mPreferenceScreen, times(1)).addPreference(mButtonWfcRoamingMode);
        verify(mPreferenceScreen, never()).removePreference(mButtonWfcRoamingMode);
    }

    @Test
    public void onResume_useWfcHomeModeConfigTrueAndEditable_shouldHideWfcRoaming() {
        mBundle.putBoolean(
                CarrierConfigManager.KEY_USE_WFC_HOME_NETWORK_MODE_IN_ROAMING_NETWORK_BOOL, true);
        mBundle.putBoolean(CarrierConfigManager.KEY_EDITABLE_WFC_ROAMING_MODE_BOOL, true);

        // Call onResume to update the WFC roaming preference.
        mFragment.onResume();

        // Check that WFC roaming preference is hidden.
        verify(mPreferenceScreen, never()).addPreference(mButtonWfcRoamingMode);
        verify(mPreferenceScreen, times(1)).removePreference(mButtonWfcRoamingMode);
    }

    @Test
    public void onResume_useWfcHomeModeConfigFalseAndNotEditable_shouldHideWfcRoaming() {
        mBundle.putBoolean(
                CarrierConfigManager.KEY_USE_WFC_HOME_NETWORK_MODE_IN_ROAMING_NETWORK_BOOL, false);
        mBundle.putBoolean(CarrierConfigManager.KEY_EDITABLE_WFC_ROAMING_MODE_BOOL, false);

        // Call onResume to update the WFC roaming preference.
        mFragment.onResume();

        // Check that WFC roaming preference is hidden.
        verify(mPreferenceScreen, never()).addPreference(mButtonWfcRoamingMode);
        verify(mPreferenceScreen, times(1)).removePreference(mButtonWfcRoamingMode);
    }

    @Test
    public void onResume_useWfcHomeModeConfigTrueAndNotEditable_shouldHideWfcRoaming() {
        mBundle.putBoolean(
                CarrierConfigManager.KEY_USE_WFC_HOME_NETWORK_MODE_IN_ROAMING_NETWORK_BOOL, true);
        mBundle.putBoolean(CarrierConfigManager.KEY_EDITABLE_WFC_ROAMING_MODE_BOOL, false);

        // Call onResume to update the WFC roaming preference.
        mFragment.onResume();

        // Check that WFC roaming preference is hidden.
        verify(mPreferenceScreen, never()).addPreference(mButtonWfcRoamingMode);
        verify(mPreferenceScreen, times(1)).removePreference(mButtonWfcRoamingMode);
    }

    @Test
    public void onPreferenceChange_useWfcHomeModeConfigFalse_shouldNotSetWfcRoaming() {
        // Call onResume to update carrier config values.
        mFragment.onResume();

        // Set the WFC home mode.
        mFragment.onPreferenceChange(mButtonWfcMode,
                String.valueOf(ImsConfig.WfcModeFeatureValueConstants.CELLULAR_PREFERRED));

        // Check that only WFC home mode is set.
        verify(mImsManager, times(1)).setWfcMode(
                eq(ImsConfig.WfcModeFeatureValueConstants.CELLULAR_PREFERRED),
                eq(false));
        verify(mImsManager, never()).setWfcMode(
                eq(ImsConfig.WfcModeFeatureValueConstants.CELLULAR_PREFERRED),
                eq(true));
    }

    @Test
    public void onPreferenceChange_useWfcHomeModeConfigTrue_shouldSetWfcRoaming() {
        mBundle.putBoolean(
                CarrierConfigManager.KEY_USE_WFC_HOME_NETWORK_MODE_IN_ROAMING_NETWORK_BOOL, true);

        // Call onResume to update carrier config values.
        mFragment.onResume();

        // Set the WFC home mode.
        mFragment.onPreferenceChange(mButtonWfcMode,
                String.valueOf(ImsConfig.WfcModeFeatureValueConstants.CELLULAR_PREFERRED));

        // Check that both WFC home mode and roaming mode are set.
        verify(mImsManager, times(1)).setWfcMode(
                eq(ImsConfig.WfcModeFeatureValueConstants.CELLULAR_PREFERRED),
                eq(false));
        verify(mImsManager, times(1)).setWfcMode(
                eq(ImsConfig.WfcModeFeatureValueConstants.CELLULAR_PREFERRED),
                eq(true));
    }

    protected class TestFragment extends WifiCallingSettingsForSub {
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
    }
}
