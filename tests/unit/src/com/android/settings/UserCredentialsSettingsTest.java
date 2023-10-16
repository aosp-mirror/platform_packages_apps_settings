/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.Looper;
import android.os.Process;
import android.view.View;
import android.widget.TextView;

import androidx.test.annotation.UiThreadTest;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.testutils.ResourcesUtils;
import com.android.settings.wifi.helper.SavedWifiHelper;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class UserCredentialsSettingsTest {
    static final String TEST_ALIAS = "test_alias";
    static final String TEST_USER_BY_NAME = "test_used_by_name";

    static final String TEXT_PURPOSE_SYSTEM = "credential_for_vpn_and_apps";
    static final String TEXT_PURPOSE_WIFI = "credential_for_wifi";
    static final String TEXT_PURPOSE_WIFI_IN_USE = "credential_for_wifi_in_use";

    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Spy
    final Context mContext = ApplicationProvider.getApplicationContext();
    @Mock
    SavedWifiHelper mSavedWifiHelper;
    @Mock
    View mView;

    UserCredentialsSettings mSettings;
    UserCredentialsSettings.Credential mSysCredential =
            new UserCredentialsSettings.Credential(TEST_ALIAS, Process.SYSTEM_UID);
    UserCredentialsSettings.Credential mWifiCredential =
            new UserCredentialsSettings.Credential(TEST_ALIAS, Process.WIFI_UID);
    List<String> mUsedByNames = Arrays.asList(TEST_USER_BY_NAME);
    TextView mPurposeView = new TextView(ApplicationProvider.getApplicationContext());
    TextView mUsedByTitleView = new TextView(ApplicationProvider.getApplicationContext());
    TextView mUsedByContentView = new TextView(ApplicationProvider.getApplicationContext());

    @Before
    @UiThreadTest
    public void setUp() {
        when(mSavedWifiHelper.isCertificateInUse(any(String.class))).thenReturn(false);
        when(mSavedWifiHelper.getCertificateNetworkNames(any(String.class)))
                .thenReturn(new ArrayList<>());
        when(mView.getTag()).thenReturn(mWifiCredential);

        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
        mSettings = spy(new UserCredentialsSettings());
        when(mSettings.getContext()).thenReturn(mContext);
        mSettings.mSavedWifiHelper = mSavedWifiHelper;
        doNothing().when(mSettings)
                .showCredentialDialogFragment(any(UserCredentialsSettings.Credential.class));
    }

    @Test
    @UiThreadTest
    public void onClick_noCredentialInTag_doNothing() {
        when(mView.getTag()).thenReturn(null);

        mSettings.onClick(mView);

        verify(mSavedWifiHelper, never()).getCertificateNetworkNames(any(String.class));
        verify(mSettings, never())
                .showCredentialDialogFragment(any(UserCredentialsSettings.Credential.class));
    }

    @Test
    @UiThreadTest
    public void onClick_credentialInNotUse_notSetUsedByNamesThenShowDialog() {
        mWifiCredential.setInUse(false);
        when(mView.getTag()).thenReturn(mWifiCredential);

        mSettings.onClick(mView);

        verify(mSavedWifiHelper, never()).getCertificateNetworkNames(any(String.class));
        verify(mSettings)
                .showCredentialDialogFragment(any(UserCredentialsSettings.Credential.class));
    }

    @Test
    @UiThreadTest
    public void onClick_credentialInUse_setUsedByNamesThenShowDialog() {
        mWifiCredential.setInUse(true);
        when(mView.getTag()).thenReturn(mWifiCredential);
        when(mSavedWifiHelper.getCertificateNetworkNames(any(String.class)))
                .thenReturn(mUsedByNames);

        mSettings.onClick(mView);

        verify(mSavedWifiHelper).getCertificateNetworkNames(any(String.class));
        assertThat(mWifiCredential.getUsedByNames()).isEqualTo(mUsedByNames);
        verify(mSettings)
                .showCredentialDialogFragment(any(UserCredentialsSettings.Credential.class));
    }

    @Test
    @UiThreadTest
    public void updatePurposeView_getSystemCert_setTextCorrectly() {
        mSettings.updatePurposeView(mPurposeView, mSysCredential);

        assertThat(mPurposeView.getText()).isEqualTo(getResString(TEXT_PURPOSE_SYSTEM));
    }

    @Test
    @UiThreadTest
    public void updatePurposeView_getWifiCert_setTextCorrectly() {
        mWifiCredential.setInUse(false);

        mSettings.updatePurposeView(mPurposeView, mWifiCredential);

        assertThat(mPurposeView.getText()).isEqualTo(getResString(TEXT_PURPOSE_WIFI));
    }

    @Test
    @UiThreadTest
    public void updatePurposeView_isWifiCertInUse_setTextCorrectly() {
        mWifiCredential.setInUse(true);

        mSettings.updatePurposeView(mPurposeView, mWifiCredential);

        assertThat(mPurposeView.getText()).isEqualTo(getResString(TEXT_PURPOSE_WIFI_IN_USE));
    }

    @Test
    @UiThreadTest
    public void updateUsedByViews_noUsedByName_hideViews() {
        mWifiCredential.setUsedByNames(new ArrayList<>());

        mSettings.updateUsedByViews(mUsedByTitleView, mUsedByContentView, mWifiCredential);

        assertThat(mUsedByTitleView.getVisibility()).isEqualTo(View.GONE);
        assertThat(mUsedByContentView.getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    @UiThreadTest
    public void updateUsedByViews_hasUsedByName_showViews() {
        mWifiCredential.setUsedByNames(mUsedByNames);

        mSettings.updateUsedByViews(mUsedByTitleView, mUsedByContentView, mWifiCredential);

        assertThat(mUsedByTitleView.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(mUsedByContentView.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(mUsedByContentView.getText().toString().contains(TEST_USER_BY_NAME)).isTrue();
    }

    static String getResString(String name) {
        return ResourcesUtils.getResourcesString(ApplicationProvider.getApplicationContext(), name);
    }
}
