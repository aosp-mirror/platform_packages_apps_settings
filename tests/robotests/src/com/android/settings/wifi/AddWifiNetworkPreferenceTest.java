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
package com.android.settings.wifi;

import static android.os.UserManager.DISALLOW_ADD_WIFI_CONFIG;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.UserManager;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowRestrictedPreference.class)
public class AddWifiNetworkPreferenceTest {

    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Spy
    Context mContext = ApplicationProvider.getApplicationContext();
    @Mock
    private UserManager mUserManager;

    private AddWifiNetworkPreference mPreference;

    @Before
    public void setUp() {
        when(mContext.getSystemService(UserManager.class)).thenReturn(mUserManager);

        mPreference = spy(new AddWifiNetworkPreference(mContext));
    }

    @Test
    public void checkRestrictionAndSetDisabled_disabledByAdmin_keepEnabledForClicks() {
        when(mPreference.isDisabledByAdmin()).thenReturn(true);

        mPreference.checkRestrictionAndSetDisabled();

        assertThat(mPreference.isEnabled()).isTrue();
    }

    @Test
    public void checkRestrictionAndSetDisabled_notDisabledByAdmin_setDisabled() {
        when(mPreference.isDisabledByAdmin()).thenReturn(false);
        when(mUserManager.hasUserRestriction(DISALLOW_ADD_WIFI_CONFIG)).thenReturn(true);

        mPreference.checkRestrictionAndSetDisabled();

        assertThat(mPreference.isEnabled()).isFalse();
    }

    @Test
    public void checkRestrictionAndSetDisabled_noRestriction_setEnabled() {
        when(mPreference.isDisabledByAdmin()).thenReturn(false);
        when(mUserManager.hasUserRestriction(DISALLOW_ADD_WIFI_CONFIG)).thenReturn(false);

        mPreference.checkRestrictionAndSetDisabled();

        assertThat(mPreference.isEnabled()).isTrue();
    }
}
