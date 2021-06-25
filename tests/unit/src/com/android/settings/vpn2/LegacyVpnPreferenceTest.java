/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.vpn2;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;

import android.content.Context;
import android.content.res.Resources.NotFoundException;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.internal.net.VpnProfile;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Unittest for LegacyVpnPreference */
@RunWith(AndroidJUnit4.class)
public class LegacyVpnPreferenceTest {
    private static final String PROFILE_KEY = "test_key";
    private static final String PROFILE_NAME = "test_vpn_name";

    private Context mContext;
    private LegacyVpnPreference mLegacyVpnPreference;
    private VpnProfile mVpnProfile;


    @Before
    public void setUp() {
        mContext = spy(ApplicationProvider.getApplicationContext());
        mVpnProfile = new VpnProfile(PROFILE_KEY);
        mVpnProfile.name = PROFILE_NAME;
        try {
            // In Junit, loading the presference at first yields a Resources.NotFoundException
            mLegacyVpnPreference = new LegacyVpnPreference(mContext);
        } catch (NotFoundException exception) {
            mLegacyVpnPreference = new LegacyVpnPreference(mContext);
        }

    }

    @Test
    public void setProfile_successfullyStoresProfile() {
        mLegacyVpnPreference.setProfile(mVpnProfile);
        assertThat(mLegacyVpnPreference.getProfile()).isEqualTo(mVpnProfile);
    }

    @Test
    public void setProfile_updatesPreferenceTitle() {
        mLegacyVpnPreference.setProfile(mVpnProfile);
        assertThat(mLegacyVpnPreference.getTitle()).isEqualTo(PROFILE_NAME);
    }
}
