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
import static android.provider.Settings.Global.PRIVATE_DNS_MODE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.appcompat.app.AlertDialog;

import com.android.settings.R;
import com.android.settings.testutils.shadow.ShadowOs;
import com.android.settingslib.CustomDialogPreferenceCompat.CustomPreferenceDialogFragment;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowOs.class)
public class PrivateDnsModeDialogPreferenceTest {

    private static final String HOST_NAME = "dns.example.com";
    private static final String INVALID_HOST_NAME = "...,";

    private PrivateDnsModeDialogPreference mPreference;

    private Context mContext;
    private Button mSaveButton;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        ReflectionHelpers.setStaticField(android.system.OsConstants.class, "AF_INET", 2);
        ReflectionHelpers.setStaticField(android.system.OsConstants.class, "AF_INET6", 10);

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
        // Don't set settings to the default value ("opportunistic") as that
        // risks masking failure to read the mode from settings.
        Settings.Global.putString(mContext.getContentResolver(),
                PrivateDnsModeDialogPreference.MODE_KEY, PRIVATE_DNS_MODE_OFF);
        Settings.Global.putString(mContext.getContentResolver(),
                PrivateDnsModeDialogPreference.HOSTNAME_KEY, HOST_NAME);

        final LayoutInflater inflater = LayoutInflater.from(mContext);
        final View view = inflater.inflate(R.layout.private_dns_mode_dialog,
                new LinearLayout(mContext), false);
        mPreference.onBindDialogView(view);

        assertThat(mPreference.mEditText.getText().toString()).isEqualTo(HOST_NAME);
        assertThat(mPreference.mRadioGroup.getCheckedRadioButtonId()).isEqualTo(
                R.id.private_dns_mode_off);
    }

    @Test
    public void testOnCheckedChanged_switchMode_saveButtonHasCorrectState() {
        final String[] INVALID_HOST_NAMES = new String[] {
                INVALID_HOST_NAME,
                "2001:db8::53",  // IPv6 string literal
                "192.168.1.1",   // IPv4 string literal
        };

        for (String invalid : INVALID_HOST_NAMES) {
            // Set invalid hostname
            mPreference.mEditText.setText(invalid);

            mPreference.onCheckedChanged(null, R.id.private_dns_mode_off);
            assertThat(mSaveButton.isEnabled()).named("off: " + invalid).isTrue();

            mPreference.onCheckedChanged(null, R.id.private_dns_mode_opportunistic);
            assertThat(mSaveButton.isEnabled()).named("opportunistic: " + invalid).isTrue();

            mPreference.onCheckedChanged(null, R.id.private_dns_mode_provider);
            assertThat(mSaveButton.isEnabled()).named("provider: " + invalid).isFalse();
        }
    }

    @Test
    public void testOnClick_positiveButtonClicked_saveData() {
        // Set the default settings to OFF
        final ContentResolver contentResolver = mContext.getContentResolver();
        Settings.Global.putString(contentResolver, PRIVATE_DNS_MODE, PRIVATE_DNS_MODE_OFF);

        mPreference.mMode = ConnectivityManager.PRIVATE_DNS_MODE_OPPORTUNISTIC;
        mPreference.onClick(null, DialogInterface.BUTTON_POSITIVE);

        // Change to OPPORTUNISTIC
        assertThat(Settings.Global.getString(contentResolver, PRIVATE_DNS_MODE)).isEqualTo(
                PRIVATE_DNS_MODE_OPPORTUNISTIC);
    }

    @Test
    public void testOnClick_negativeButtonClicked_doNothing() {
        // Set the default settings to OFF
        final ContentResolver contentResolver = mContext.getContentResolver();
        Settings.Global.putString(contentResolver, PRIVATE_DNS_MODE, PRIVATE_DNS_MODE_OFF);

        mPreference.mMode = ConnectivityManager.PRIVATE_DNS_MODE_OPPORTUNISTIC;
        mPreference.onClick(null, DialogInterface.BUTTON_NEGATIVE);

        // Still equal to OFF
        assertThat(Settings.Global.getString(contentResolver,
                Settings.Global.PRIVATE_DNS_MODE)).isEqualTo(
                ConnectivityManager.PRIVATE_DNS_MODE_OFF);
    }
}
