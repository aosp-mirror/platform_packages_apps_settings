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

package com.android.settings.network;

import static android.net.ConnectivityManager.PRIVATE_DNS_MODE_OFF;
import static android.net.ConnectivityManager.PRIVATE_DNS_MODE_OPPORTUNISTIC;
import static android.net.ConnectivityManager.PRIVATE_DNS_MODE_PROVIDER_HOSTNAME;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import android.content.Context;
import android.provider.Settings;
import android.widget.Button;

import com.android.settings.R;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.shadow.ShadowHelpUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(shadows = ShadowHelpUtils.class)
public class PrivateDnsModeDialogFragmentTest {

    private static final String HOST_NAME = "192.168.1.1";
    private static final String INVALID_HOST_NAME = "...,";

    private Context mContext;
    private PrivateDnsModeDialogFragment mFragment;
    private Button mSaveButton;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = RuntimeEnvironment.application;
        mSaveButton = new Button(mContext);

        mFragment = spy(new PrivateDnsModeDialogFragment());
        doReturn(mContext).when(mFragment).getContext();
        mFragment.onCreateDialog(null);
        mFragment.mSaveButton = mSaveButton;
    }

    @Test
    public void testOnCheckedChanged_dnsModeOff_disableEditText() {
        mFragment.onCheckedChanged(null, R.id.private_dns_mode_off);

        assertThat(mFragment.mMode).isEqualTo(PRIVATE_DNS_MODE_OFF);
        assertThat(mFragment.mEditText.isEnabled()).isFalse();
    }

    @Test
    public void testOnCheckedChanged_dnsModeOpportunistic_disableEditText() {
        mFragment.onCheckedChanged(null, R.id.private_dns_mode_opportunistic);

        assertThat(mFragment.mMode).isEqualTo(PRIVATE_DNS_MODE_OPPORTUNISTIC);
        assertThat(mFragment.mEditText.isEnabled()).isFalse();
    }

    @Test
    public void testOnCheckedChanged_dnsModeProvider_enableEditText() {
        mFragment.onCheckedChanged(null, R.id.private_dns_mode_provider);

        assertThat(mFragment.mMode).isEqualTo(PRIVATE_DNS_MODE_PROVIDER_HOSTNAME);
        assertThat(mFragment.mEditText.isEnabled()).isTrue();
    }

    @Test
    public void testOnCreateDialog_containsCorrectData() {
        Settings.Global.putString(mContext.getContentResolver(),
                PrivateDnsModeDialogFragment.MODE_KEY, PRIVATE_DNS_MODE_OPPORTUNISTIC);
        Settings.Global.putString(mContext.getContentResolver(),
                PrivateDnsModeDialogFragment.HOSTNAME_KEY, HOST_NAME);

        mFragment.onCreateDialog(null);

        assertThat(mFragment.mEditText.getText().toString()).isEqualTo(HOST_NAME);
        assertThat(mFragment.mRadioGroup.getCheckedRadioButtonId()).isEqualTo(
                R.id.private_dns_mode_opportunistic);
    }

    @Test
    public void testOnCheckedChanged_switchMode_saveButtonHasCorrectState() {
        // Set invalid hostname
        mFragment.mEditText.setText(INVALID_HOST_NAME);

        mFragment.onCheckedChanged(null, R.id.private_dns_mode_opportunistic);
        assertThat(mSaveButton.isEnabled()).isTrue();

        mFragment.onCheckedChanged(null, R.id.private_dns_mode_provider);
        assertThat(mSaveButton.isEnabled()).isFalse();

        mFragment.onCheckedChanged(null, R.id.private_dns_mode_off);
        assertThat(mSaveButton.isEnabled()).isTrue();
    }
}
