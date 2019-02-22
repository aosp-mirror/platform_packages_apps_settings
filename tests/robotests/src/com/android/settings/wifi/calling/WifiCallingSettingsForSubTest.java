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
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
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
    private TestFragment mFragment;
    private Context mContext;
    private TextView mEmptyView;

    @Mock private ImsManager mImsManager;
    @Mock private TelephonyManager mTelephonyManager;
    @Mock private PreferenceScreen mPreferenceScreen;
    @Mock private SettingsActivity mActivity;
    @Mock private SwitchBar mSwitchBar;
    @Mock private ToggleSwitch mToggleSwitch;
    @Mock private View mView;
    @Mock private ImsConfig mImsConfig;

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
        doNothing().when(mFragment).finish();
        doReturn(mView).when(mFragment).getView();

        mEmptyView = new TextView(mContext);
        doReturn(mEmptyView).when(mView).findViewById(android.R.id.empty);

        ReflectionHelpers.setField(mSwitchBar, "mSwitch", mToggleSwitch);
        doReturn(mSwitchBar).when(mView).findViewById(R.id.switch_bar);

        doReturn(mImsManager).when(mFragment).getImsManager();
        doReturn(mImsConfig).when(mImsManager).getConfigInterface();
        doReturn(true).when(mImsManager).isWfcProvisionedOnDevice();

        mFragment.onAttach(mContext);
        mFragment.onCreate(null);
        mFragment.onActivityCreated(null);
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

    protected class TestFragment extends WifiCallingSettingsForSub {
        @Override
        protected Object getSystemService(final String name) {
            if (Context.TELEPHONY_SERVICE.equals(name)) {
                return mTelephonyManager;
            }
            return null;
        }
    }
}
