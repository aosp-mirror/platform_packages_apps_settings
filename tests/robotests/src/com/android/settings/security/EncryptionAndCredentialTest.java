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

package com.android.settings.security;

import static com.google.common.truth.Truth.assertThat;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.os.Bundle;
import android.os.UserManager;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.androidx.fragment.FragmentController;

@RunWith(RobolectricTestRunner.class)
public class EncryptionAndCredentialTest {

    @Mock
    private UserManager mUserManager;
    @Mock
    private DevicePolicyManager mDevicePolicyManager;

    private Context mContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ShadowApplication application = ShadowApplication.getInstance();
        application.setSystemService(Context.DEVICE_POLICY_SERVICE, mDevicePolicyManager);
        application.setSystemService(Context.USER_SERVICE, mUserManager);
        mContext = RuntimeEnvironment.application;
    }

    @Test
    public void getMetricsCategory_shouldReturnEncryptionAndCredential() {
        EncryptionAndCredential fragment = new EncryptionAndCredential();
        assertThat(fragment.getMetricsCategory()).isEqualTo(MetricsEvent.ENCRYPTION_AND_CREDENTIAL);
    }

    @Test
    public void isSelectable_encryptionPreferenceStatus_isNotSelectable() {
        final PreferenceFragmentCompat fragment =
                FragmentController.of(new TestFragment(), new Bundle())
                .create()
                .start()
                .resume()
                .get();
        final Preference preference =
                fragment.findPreference("encryption_and_credentials_encryption_status");

        assertThat(preference.isSelectable()).isFalse();
    }

    public static class TestFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            addPreferencesFromResource(com.android.settings.R.xml.encryption_and_credential);
        }
    }
}
