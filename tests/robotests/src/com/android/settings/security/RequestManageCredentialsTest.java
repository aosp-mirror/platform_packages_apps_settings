/*
 * Copyright (C) 2020 The Android Open Source Project
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.UserHandle;
import android.security.AppUriAuthenticationPolicy;
import android.security.Credentials;
import android.security.IKeyChainService;
import android.security.KeyChain;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class RequestManageCredentialsTest {

    private static final AppUriAuthenticationPolicy AUTHENTICATION_POLICY =
            new AppUriAuthenticationPolicy.Builder()
                    .addAppAndUriMapping("com.android.test", Uri.parse("test.com"), "testAlias")
                    .build();

    private RequestManageCredentials mActivity;

    @Mock
    private DevicePolicyManager mDevicePolicyManager;

    @Mock
    private KeyChain.KeyChainConnection mKeyChainConnection;

    @Mock
    private IKeyChainService mKeyChainService;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void onCreate_intentActionNotManageCredentials_finishActivity()
            throws Exception {
        final Intent intent = new Intent(Credentials.ACTION_MANAGE_CREDENTIALS + "_bad");
        intent.putExtra(KeyChain.EXTRA_AUTHENTICATION_POLICY, AUTHENTICATION_POLICY);
        setupActivityWithAction(intent);
        when(mDevicePolicyManager.getDeviceOwnerUser()).thenReturn(null);
        when(mDevicePolicyManager.getProfileOwner()).thenReturn(null);
        when(mActivity.getLaunchedFromPackage()).thenReturn("com.example.credapp");

        mActivity.onCreate(null);

        assertThat(mActivity).isNotNull();
        assertThat(mActivity.isFinishing()).isTrue();
    }

    @Test
    public void onCreate_noAuthenticationPolicy_finishActivity()
            throws Exception {
        final Intent intent = new Intent(Credentials.ACTION_MANAGE_CREDENTIALS);
        setupActivityWithAction(intent);
        when(mDevicePolicyManager.getDeviceOwnerUser()).thenReturn(null);
        when(mDevicePolicyManager.getProfileOwner()).thenReturn(null);
        when(mActivity.getLaunchedFromPackage()).thenReturn("com.example.credapp");

        mActivity.onCreate(null);

        assertThat(mActivity).isNotNull();
        assertThat(mActivity.isFinishing()).isTrue();
    }

    @Test
    public void onCreate_invalidAuthenticationPolicy_finishActivity()
            throws Exception {
        final Intent intent = new Intent(Credentials.ACTION_MANAGE_CREDENTIALS);
        intent.putExtra(KeyChain.EXTRA_AUTHENTICATION_POLICY, "Invalid policy");

        setupActivityWithAction(intent);
        when(mDevicePolicyManager.getDeviceOwnerUser()).thenReturn(null);
        when(mDevicePolicyManager.getProfileOwner()).thenReturn(null);
        when(mActivity.getLaunchedFromPackage()).thenReturn("com.example.credapp");

        mActivity.onCreate(null);

        assertThat(mActivity).isNotNull();
        assertThat(mActivity.isFinishing()).isTrue();
    }

    @Test
    public void onCreate_runOnManagedProfile_finishActivity()
            throws Exception {
        final Intent intent = new Intent(Credentials.ACTION_MANAGE_CREDENTIALS);
        intent.putExtra(KeyChain.EXTRA_AUTHENTICATION_POLICY, AUTHENTICATION_POLICY);

        setupActivityWithAction(intent);
        when(mDevicePolicyManager.getDeviceOwnerUser()).thenReturn(null);
        when(mDevicePolicyManager.getProfileOwner()).thenReturn(new ComponentName("pkg", "cls"));
        when(mActivity.getLaunchedFromPackage()).thenReturn("com.example.credapp");

        mActivity.onCreate(null);

        assertThat(mActivity).isNotNull();
        assertThat(mActivity.isFinishing()).isTrue();
    }

    @Test
    public void onCreate_runOnManagedDevice_finishActivity()
            throws Exception {
        final Intent intent = new Intent(Credentials.ACTION_MANAGE_CREDENTIALS);
        intent.putExtra(KeyChain.EXTRA_AUTHENTICATION_POLICY, AUTHENTICATION_POLICY);

        setupActivityWithAction(intent);
        when(mDevicePolicyManager.getDeviceOwnerUser()).thenReturn(UserHandle.SYSTEM);
        when(mDevicePolicyManager.getProfileOwner()).thenReturn(null);
        when(mActivity.getLaunchedFromPackage()).thenReturn("com.example.credapp");

        mActivity.onCreate(null);

        assertThat(mActivity).isNotNull();
        assertThat(mActivity.isFinishing()).isTrue();
    }

    @Test
    public void onCreate_authenticationPolicyProvided_startActivity() throws Exception {
        final Intent intent = new Intent(Credentials.ACTION_MANAGE_CREDENTIALS);
        intent.putExtra(KeyChain.EXTRA_AUTHENTICATION_POLICY, AUTHENTICATION_POLICY);
        setupActivityWithAction(intent);

        when(mDevicePolicyManager.getDeviceOwnerUser()).thenReturn(null);
        when(mDevicePolicyManager.getProfileOwner()).thenReturn(null);
        when(mActivity.getLaunchedFromPackage()).thenReturn("com.example.credapp");

        mActivity.onCreate(null);

        assertThat(mActivity).isNotNull();
        assertThat(mActivity.isFinishing()).isFalse();
        assertThat((RecyclerView) mActivity.findViewById(R.id.apps_list)).isNotNull();
        assertThat((LinearLayout) mActivity.findViewById(R.id.button_panel)).isNotNull();
        assertThat((Button) mActivity.findViewById(R.id.allow_button)).isNotNull();
        assertThat((Button) mActivity.findViewById(R.id.dont_allow_button)).isNotNull();
    }

    private void setupActivityWithAction(Intent intent) throws Exception {
        mActivity = spy(Robolectric.buildActivity(RequestManageCredentials.class, intent).get());
        doReturn(mKeyChainConnection).when(mActivity).getKeyChainConnection(eq(mActivity), any());
        doReturn(mDevicePolicyManager).when(mActivity).getSystemService(DevicePolicyManager.class);
        when(mKeyChainConnection.getService()).thenReturn(mKeyChainService);
    }
}
