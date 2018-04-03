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
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import com.android.settings.R;
import com.android.settingslib.CustomDialogPreference.CustomPreferenceDialogFragment;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.util.ReflectionHelpers;

@RunWith(SettingsRobolectricTestRunner.class)
public class PrivateDnsModeDialogPreferenceTest {

    private static final String HOST_NAME = "dns.example.com";
    private static final String INVALID_HOST_NAME = "...,";

    private PrivateDnsModeDialogPreference mPreference;

    private Context mContext;
    private Button mSaveButton;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = RuntimeEnvironment.application;
        mSaveButton = new Button(mContext);

        final CustomPreferenceDialogFragment fragment = mock(CustomPreferenceDialogFragment.class);
        final AlertDialog dialog = mock(AlertDialog.class);
        when(fragment.getDialog()).thenReturn(dialog);
        when(dialog.getButton(anyInt())).thenReturn(mSaveButton);

        mPreference = new PrivateDnsModeDialogPreference(mContext);
        ReflectionHelpers.setField(mPreference, "mFragment", fragment);

        final LayoutInflater inflater = LayoutInflater.from(mContext);
        final View view = inflater.inflate(R.layout.private_dns_mode_dialog,
                new LinearLayout(mContext), false);

        mPreference.onBindDialogView(view);
    }

    @Test
    public void testOnCheckedChanged_dnsModeOff_disableEditText() {
        mPreference.onCheckedChanged(null, R.id.private_dns_mode_off);

        assertThat(mPreference.mMode).isEqualTo(PRIVATE_DNS_MODE_OFF);
        assertThat(mPreference.mEditText.isEnabled()).isFalse();
    }

    @Test
    public void testOnCheckedChanged_dnsModeOpportunistic_disableEditText() {
        mPreference.onCheckedChanged(null, R.id.private_dns_mode_opportunistic);

        assertThat(mPreference.mMode).isEqualTo(PRIVATE_DNS_MODE_OPPORTUNISTIC);
        assertThat(mPreference.mEditText.isEnabled()).isFalse();
    }

    @Test
    public void testOnCheckedChanged_dnsModeProvider_enableEditText() {
        mPreference.onCheckedChanged(null, R.id.private_dns_mode_provider);

        assertThat(mPreference.mMode).isEqualTo(PRIVATE_DNS_MODE_PROVIDER_HOSTNAME);
        assertThat(mPreference.mEditText.isEnabled()).isTrue();
    }

    @Test
    public void testOnBindDialogView_containsCorrectData() {
        Settings.Global.putString(mContext.getContentResolver(),
                PrivateDnsModeDialogPreference.MODE_KEY, PRIVATE_DNS_MODE_OPPORTUNISTIC);
        Settings.Global.putString(mContext.getContentResolver(),
                PrivateDnsModeDialogPreference.HOSTNAME_KEY, HOST_NAME);

        final LayoutInflater inflater = LayoutInflater.from(mContext);
        final View view = inflater.inflate(R.layout.private_dns_mode_dialog,
                new LinearLayout(mContext), false);
        mPreference.onBindDialogView(view);

        assertThat(mPreference.mEditText.getText().toString()).isEqualTo(HOST_NAME);
        assertThat(mPreference.mRadioGroup.getCheckedRadioButtonId()).isEqualTo(
                R.id.private_dns_mode_opportunistic);
    }

    @Test
    public void testOnCheckedChanged_switchMode_saveButtonHasCorrectState() {
        // Set invalid hostname
        mPreference.mEditText.setText(INVALID_HOST_NAME);

        mPreference.onCheckedChanged(null, R.id.private_dns_mode_opportunistic);
        assertThat(mSaveButton.isEnabled()).isTrue();

        mPreference.onCheckedChanged(null, R.id.private_dns_mode_provider);
        assertThat(mSaveButton.isEnabled()).isFalse();

        mPreference.onCheckedChanged(null, R.id.private_dns_mode_off);
        assertThat(mSaveButton.isEnabled()).isTrue();
    }
}
