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

package com.android.settings.vpn2;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.Looper;

import androidx.test.annotation.UiThreadTest;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settingslib.RestrictedPreference;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@RunWith(AndroidJUnit4.class)
public class AppManagementFragmentTest {
    private static final String FAKE_PACKAGE_NAME = "com.fake.package.name";
    private static final String ADVANCED_VPN_GROUP_PACKAGE_NAME = "com.advanced.package.name";

    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    private AppManagementFragment mFragment;
    private Context mContext;
    private FakeFeatureFactory mFakeFeatureFactory;
    private RestrictedPreference mPreferenceForget;

    @Before
    @UiThreadTest
    public void setUp() {
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }

        mFragment = spy(new AppManagementFragment());
        mContext = spy(ApplicationProvider.getApplicationContext());
        mPreferenceForget = new RestrictedPreference(mContext);

        mFakeFeatureFactory = FakeFeatureFactory.setupForTest();
        mFragment.init(ADVANCED_VPN_GROUP_PACKAGE_NAME,
                mFakeFeatureFactory.getAdvancedVpnFeatureProvider(), mPreferenceForget);
        when(mFakeFeatureFactory.mAdvancedVpnFeatureProvider.getAdvancedVpnPackageName())
                .thenReturn(ADVANCED_VPN_GROUP_PACKAGE_NAME);
        when(mFakeFeatureFactory.mAdvancedVpnFeatureProvider.isAdvancedVpnSupported(any()))
                .thenReturn(true);
    }

    @Test
    public void updateRestrictedViews_isAdvancedVpn_hidesForgetPreference() {
        when(mFakeFeatureFactory.mAdvancedVpnFeatureProvider.isAdvancedVpnRemovable())
                .thenReturn(false);
        mFragment.updateRestrictedViews();
        assertThat(mPreferenceForget.isVisible()).isFalse();
    }

    @Test
    public void updateRestrictedViews_isNotAdvancedVpn_showsForgetPreference() {
        when(mFakeFeatureFactory.mAdvancedVpnFeatureProvider.isAdvancedVpnRemovable())
                .thenReturn(false);
        mFragment.init(FAKE_PACKAGE_NAME,
                mFakeFeatureFactory.getAdvancedVpnFeatureProvider(), mPreferenceForget);
        mFragment.updateRestrictedViews();
        assertThat(mPreferenceForget.isVisible()).isTrue();
    }

    @Test
    public void updateRestrictedViews_isAdvancedVpnRemovable_showsForgetPreference() {
        when(mFakeFeatureFactory.mAdvancedVpnFeatureProvider.isAdvancedVpnRemovable())
                .thenReturn(true);
        mFragment.init(FAKE_PACKAGE_NAME,
                mFakeFeatureFactory.getAdvancedVpnFeatureProvider(), mPreferenceForget);
        mFragment.updateRestrictedViews();
        assertThat(mPreferenceForget.isVisible()).isTrue();
    }
}
