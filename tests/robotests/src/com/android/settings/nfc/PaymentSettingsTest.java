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
 *
 */

package com.android.settings.nfc;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.os.UserHandle;
import android.os.UserManager;

import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = PaymentSettingsTest.ShadowPaymentBackend.class)
public class PaymentSettingsTest {

    static final String PAYMENT_KEY = "nfc_payment";
    static final String FOREGROUND_KEY = "nfc_foreground";

    private Context mContext;

    @Mock
    private PackageManager mPackageManager;

    @Mock
    private UserManager mUserManager;

    @Mock
    private UserInfo mUserInfo;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        doReturn(mUserManager).when(mContext).getSystemService(UserManager.class);
        when(mUserManager.getUserInfo(UserHandle.myUserId())).thenReturn(mUserInfo);
    }

    @Test
    public void getNonIndexableKey_noNFC_allKeysAdded() {
        when(mPackageManager.hasSystemFeature(PackageManager.FEATURE_NFC)).thenReturn(false);

        final List<String> niks =
                PaymentSettings.SEARCH_INDEX_DATA_PROVIDER.getNonIndexableKeys(mContext);

        assertThat(niks).contains(PAYMENT_KEY);
        assertThat(niks).contains(FOREGROUND_KEY);
    }

    @Test
    public void getNonIndexableKey_NFC_foregroundKeyAdded() {
        when(mPackageManager.hasSystemFeature(PackageManager.FEATURE_NFC)).thenReturn(true);

        final List<String> niks =
                PaymentSettings.SEARCH_INDEX_DATA_PROVIDER.getNonIndexableKeys(mContext);

        assertThat(niks).contains(FOREGROUND_KEY);
    }

    @Test
    public void getNonIndexableKey_primaryUser_returnsTrue() {
        when(mPackageManager.hasSystemFeature(PackageManager.FEATURE_NFC)).thenReturn(true);

        final List<String> niks =
                PaymentSettings.SEARCH_INDEX_DATA_PROVIDER.getNonIndexableKeys(mContext);

        assertThat(niks).containsExactly(FOREGROUND_KEY);
    }

    @Test
    public void getNonIndexableKey_guestUser_returnsFalse() {
        when(mPackageManager.hasSystemFeature(PackageManager.FEATURE_NFC)).thenReturn(true);
        when(mUserInfo.isGuest()).thenReturn(true);

        final List<String> niks =
                PaymentSettings.SEARCH_INDEX_DATA_PROVIDER.getNonIndexableKeys(mContext);

        assertThat(niks).containsAllOf(FOREGROUND_KEY, PAYMENT_KEY);
    }

    @Test
    public void isShowEmptyImage_hasVisiblePreference_returnFalse() {
        final PaymentSettings paymentSettings = new PaymentSettings();
        final PreferenceManager preferenceManager = new PreferenceManager(mContext);
        final PreferenceScreen screen = preferenceManager.createPreferenceScreen(mContext);
        final Preference preference1 = new Preference(mContext);
        screen.addPreference(preference1);
        final Preference preference2 = new Preference(mContext);
        screen.addPreference(preference2);

        assertThat(paymentSettings.isShowEmptyImage(screen)).isFalse();
    }

    @Test
    public void isShowEmptyImage_hasNoVisiblePreference_returnTrue() {
        final PaymentSettings paymentSettings = new PaymentSettings();
        final PreferenceManager preferenceManager = new PreferenceManager(mContext);
        final PreferenceScreen screen = preferenceManager.createPreferenceScreen(mContext);
        final Preference preference1 = new Preference(mContext);
        preference1.setVisible(false);
        screen.addPreference(preference1);
        final Preference preference2 = new Preference(mContext);
        screen.addPreference(preference2);
        preference2.setVisible(false);

        assertThat(paymentSettings.isShowEmptyImage(screen)).isTrue();
    }

    @Implements(PaymentBackend.class)
    public static class ShadowPaymentBackend {
        private ArrayList<PaymentBackend.PaymentAppInfo> mAppInfos;

        public void __constructor__(Context context) {
            mAppInfos = new ArrayList<>();
            mAppInfos.add(new PaymentBackend.PaymentAppInfo());
        }

        @Implementation
        protected List<PaymentBackend.PaymentAppInfo> getPaymentAppInfos() {
            return mAppInfos;
        }
    }
}