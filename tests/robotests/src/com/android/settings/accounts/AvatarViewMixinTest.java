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

package com.android.settings.accounts;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.accounts.Account;
import android.content.Context;
import android.widget.ImageView;

import com.android.settings.homepage.SettingsHomepageActivity;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

@RunWith(SettingsRobolectricTestRunner.class)
public class AvatarViewMixinTest {
    private static final String DUMMY_ACCOUNT = "test@domain.com";
    private static final String DUMMY_DOMAIN = "domain.com";

    private Context mContext;
    private ImageView mImageView;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mImageView = new ImageView(mContext);
    }

    @Test
    public void hasAccount_useDefaultAccountData_returnFalse() {
        final AvatarViewMixin avatarViewMixin = new AvatarViewMixin(mContext, mImageView);
        assertThat(avatarViewMixin.hasAccount()).isFalse();
    }

    @Test
    @Config(shadows = ShadowAccountFeatureProviderImpl.class)
    public void hasAccount_useShadowAccountData_returnTrue() {
        final AvatarViewMixin avatarViewMixin = new AvatarViewMixin(mContext, mImageView);
        assertThat(avatarViewMixin.hasAccount()).isTrue();
    }

    @Test
    public void onStart_useMockAvatarViewMixin_shouldBeExecuted() {
        final AvatarViewMixin mockAvatar = spy(new AvatarViewMixin(mContext, mImageView));

        final ActivityController controller = Robolectric.buildActivity(
                SettingsHomepageActivity.class).create();
        final SettingsHomepageActivity settingsHomepageActivity =
                (SettingsHomepageActivity) controller.get();
        settingsHomepageActivity.getLifecycle().addObserver(mockAvatar);
        controller.start();

        verify(mockAvatar).onStart();
    }

    @Implements(AccountFeatureProviderImpl.class)
    public static class ShadowAccountFeatureProviderImpl {

        @Implementation
        public Account[] getAccounts(Context context) {
            Account[] accounts = {new Account(DUMMY_ACCOUNT, DUMMY_DOMAIN)};
            return accounts;
        }
    }
}
