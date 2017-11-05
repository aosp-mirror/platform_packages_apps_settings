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

package com.android.settings.deviceinfo.imei;

import static android.telephony.TelephonyManager.PHONE_TYPE_CDMA;
import static android.telephony.TelephonyManager.PHONE_TYPE_GSM;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.os.UserManager;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.telephony.TelephonyManager;

import com.android.settings.TestConfig;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class AbstractImeiInfoPreferenceControllerTest {

    @Mock
    private Preference mPreference;
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private TelephonyManager mTelephonyManager;
    @Mock
    private UserManager mUserManager;
    @Mock
    private Fragment mFragment;

    private Context mContext;
    private AbstractImeiInfoPreferenceControllerImpl mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        doReturn(mUserManager).when(mContext).getSystemService(UserManager.class);
        mController = new AbstractImeiInfoPreferenceControllerImpl(mContext, mFragment);
        ReflectionHelpers.setField(mController, "mTelephonyManager", mTelephonyManager);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
        when(mPreference.getKey()).thenReturn(mController.getPreferenceKey());
    }

    @Test
    public void displayPreference_cdmaPhone_shouldSetCdmaTitleAndMeid() {
        mController = spy(mController);
        final String meid = "125132215123";
        when(mTelephonyManager.getPhoneType()).thenReturn(PHONE_TYPE_CDMA);
        doReturn(meid).when(mController).getMeid();

        mController.displayPreference(mScreen);

        verify(mPreference).setTitle(mController.getTitleForCdmaPhone());
        verify(mPreference).setSummary(meid);
    }

    @Test
    public void displayPreference_gsmPhone_shouldSetGsmTitleAndImei() {
        final String imei = "125132215123";
        when(mTelephonyManager.getPhoneType()).thenReturn(PHONE_TYPE_GSM);
        when(mTelephonyManager.getImei(anyInt())).thenReturn(imei);

        mController.displayPreference(mScreen);

        verify(mPreference).setTitle(mController.getTitleForGsmPhone());
        verify(mPreference).setSummary(imei);
    }

    @Test
    public void handlePreferenceTreeClick_shouldStartDialogFragment() {
        when(mFragment.getChildFragmentManager()).thenReturn(
                mock(FragmentManager.class, Answers.RETURNS_DEEP_STUBS));
        when(mPreference.getTitle()).thenReturn("SomeTitle");
        mController.displayPreference(mScreen);

        mController.handlePreferenceTreeClick(mPreference);

        verify(mFragment).getChildFragmentManager();
    }

    public class AbstractImeiInfoPreferenceControllerImpl extends
            AbstractImeiInfoPreferenceController {

        public AbstractImeiInfoPreferenceControllerImpl(Context context, Fragment fragment) {
            super(context, fragment);
        }

        @Override
        public String getPreferenceKey() {
            return "foobar";
        }

        @Override
        protected String getTitleForCdmaPhone() {
            return "foo";
        }

        @Override
        protected String getTitleForGsmPhone() {
            return "bar";
        }

        @Override
        protected int getSimSlot() {
            return 0;
        }
    }
}
