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

import static org.robolectric.Shadows.shadowOf;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.security.AppUriAuthenticationPolicy;
import android.security.Credentials;
import android.security.KeyChain;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowActivity;

@RunWith(RobolectricTestRunner.class)
public class RequestManageCredentialsTest {

    private static final AppUriAuthenticationPolicy AUTHENTICATION_POLICY =
            new AppUriAuthenticationPolicy.Builder()
                    .addAppAndUriMapping("com.android.test", Uri.parse("test.com"), "testAlias")
                    .build();

    private RequestManageCredentials mActivity;

    private ShadowActivity mShadowActivity;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void onCreate_intentActionNotManageCredentials_finishActivity() {
        final Intent intent = new Intent("android.security.ANOTHER_ACTION");

        initActivity(intent);

        assertThat(mActivity).isNotNull();
        assertThat(mActivity.isFinishing()).isTrue();
    }

    @Test
    public void onCreate_authenticationPolicyProvided_startActivity() {
        final Intent intent = new Intent(Credentials.ACTION_MANAGE_CREDENTIALS);
        intent.putExtra(KeyChain.EXTRA_AUTHENTICATION_POLICY, AUTHENTICATION_POLICY);

        initActivity(intent);

        assertThat(mActivity).isNotNull();
        assertThat(mActivity.isFinishing()).isFalse();
        assertThat((RecyclerView) mActivity.findViewById(R.id.apps_list)).isNotNull();
        assertThat((LinearLayout) mActivity.findViewById(R.id.button_panel)).isNotNull();
        assertThat((Button) mActivity.findViewById(R.id.allow_button)).isNotNull();
        assertThat((Button) mActivity.findViewById(R.id.dont_allow_button)).isNotNull();
    }

    @Test
    public void onCreate_dontAllowButtonClicked_finishActivity() {
        final Intent intent = new Intent(Credentials.ACTION_MANAGE_CREDENTIALS);
        intent.putExtra(KeyChain.EXTRA_AUTHENTICATION_POLICY, AUTHENTICATION_POLICY);

        initActivity(intent);

        Button dontAllowButton = mActivity.findViewById(R.id.dont_allow_button);
        assertThat(dontAllowButton.hasOnClickListeners()).isTrue();
        dontAllowButton.performClick();
        assertThat(mActivity.isFinishing()).isTrue();
        assertThat(mShadowActivity.getResultCode()).isEqualTo(Activity.RESULT_CANCELED);
    }

    private void initActivity(@NonNull Intent intent) {
        mActivity = Robolectric.buildActivity(RequestManageCredentials.class, intent).setup().get();
        mShadowActivity = shadowOf(mActivity);
    }

}
