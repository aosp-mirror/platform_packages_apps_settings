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

package com.android.settings.deviceinfo;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.os.PersistableBundle;
import android.os.UserManager;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionInfo;
import android.telephony.TelephonyManager;

import com.android.internal.telephony.Phone;
import com.android.settings.R;
import com.android.settings.TestConfig;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.shadow.ShadowPhoneFactory;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.util.ReflectionHelpers;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION,
        shadows = {
                ShadowPhoneFactory.class
        })
public class SimStatusTest {
    @Mock
    private CarrierConfigManager mCarrierConfigManager;
    @Mock
    private Phone mPhone;
    @Mock
    private SubscriptionInfo mSubscriptionInfo;
    @Mock
    private TelephonyManager mTelephonyManager;
    @Mock
    private PreferenceScreen mMockScreen;
    @Mock
    private Preference mMockImsRegistrationStatePreference;
    @Mock
    private UserManager mMockUserManager;
    @Mock
    private Activity mMockActivity;

    private Context mContext;
    private Resources mResources;
    private PersistableBundle mBundle;
    private SimStatus mFragment;

    private String mImsRegSummaryText;
    private boolean mImsRegRemoved;
    private boolean mResourceUpdated;
    private List<SubscriptionInfo> mSelectableSubInfos = new ArrayList<>();

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        mResources = spy(mContext.getResources());
        mBundle = new PersistableBundle();
        mFragment = spy(new SimStatus());

        doReturn(mContext).when(mFragment).getContext();
        doReturn(mMockScreen).when(mFragment).getPreferenceScreen();
        doReturn(mMockImsRegistrationStatePreference).when(mFragment).findPreference(
                SimStatus.KEY_IMS_REGISTRATION_STATE);
        doReturn(mMockActivity).when(mFragment).getActivity();
        doReturn(mMockUserManager).when(mContext).getSystemService(Context.USER_SERVICE);
        doReturn(mResources).when(mContext).getResources();
        doReturn(0).when(mResources).getIdentifier(anyString(), anyString(), anyString());
        doReturn(false).when(mResources).getBoolean(0);

        doNothing().when(mFragment).addPreferencesFromResource(anyInt());

        ReflectionHelpers.setField(mFragment, "mCarrierConfigManager", mCarrierConfigManager);
        ReflectionHelpers.setField(mFragment, "mPhone", mPhone);
        ReflectionHelpers.setField(mFragment, "mRes", mResources);
        ReflectionHelpers.setField(mFragment, "mSir", mSubscriptionInfo);
        ReflectionHelpers.setField(mFragment, "mTelephonyManager", mTelephonyManager);
        mSelectableSubInfos.add(mSubscriptionInfo);
        ReflectionHelpers.setField(mFragment, "mSelectableSubInfos", mSelectableSubInfos);

        when(mSubscriptionInfo.getSubscriptionId()).thenReturn(0);
        when(mCarrierConfigManager.getConfigForSubId(anyInt())).thenReturn(mBundle);
        when(mMockActivity.createPackageContext(anyString(), anyInt())).thenReturn(mContext);

        ShadowPhoneFactory.setPhone(mPhone);
    }

    @Test
    public void updateImsRegistrationState_imsRegistered_shouldSetSummaryToRegisterd() {
        when(mTelephonyManager.isImsRegistered(anyInt())).thenReturn(true);

        mFragment.updateImsRegistrationState();

        // Check "Registered" is set in the summary text
        verify(mMockImsRegistrationStatePreference).setSummary(mContext.getString(
                R.string.ims_reg_status_registered));
    }

    @Test
    public void updateImsRegistrationState_imsNotRegistered_shouldSetSummaryToNotRegisterd() {
        when(mTelephonyManager.isImsRegistered(anyInt())).thenReturn(false);

        mFragment.updateImsRegistrationState();

        // Check "Not registered" is set in the summary text
        verify(mMockImsRegistrationStatePreference).setSummary(mContext.getString(
                R.string.ims_reg_status_not_registered));
    }

    @Test
    public void updatePreference_configTrue_shouldNotRemoveImsStatusPreference() {
        mBundle.putBoolean(CarrierConfigManager.KEY_SHOW_IMS_REGISTRATION_STATUS_BOOL, true);

        mFragment.updatePreference(false);

        // Check the preference is not removed if the config is true
        verify(mMockScreen, never()).removePreference(mMockImsRegistrationStatePreference);
    }

    @Test
    public void updatePreference_configFalse_shouldRemoveImsStatusPreference() {
        mBundle.putBoolean(CarrierConfigManager.KEY_SHOW_IMS_REGISTRATION_STATUS_BOOL, false);

        mFragment.updatePreference(false);

        // Check the preference is removed if the config is false
        verify(mMockScreen).removePreference(mMockImsRegistrationStatePreference);
    }

    @Test
    public void updatePreference_tabChanged_shouldRemoveAndAddPreferences() {
        mFragment.updatePreference(true);

        // Check all preferences are removed once and added again
        verify(mMockScreen).removeAll();
        verify(mFragment).addPreferencesFromResource(R.xml.device_info_sim_status);
    }

    @Test
    public void doTabChanged_shouldRemoveAndAddPreferences() {
        mFragment.doTabChanged("0");

        // Check all preferences are removed once and added again
        verify(mMockScreen).removeAll();
        verify(mFragment).addPreferencesFromResource(R.xml.device_info_sim_status);
    }
}
