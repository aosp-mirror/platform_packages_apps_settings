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

package com.android.settings.enterprise;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.content.Context;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public final class AlwaysOnVpnCurrentUserPreferenceControllerTest {

    private static final String VPN_SET_DEVICE = "VPN set";
    private static final String VPN_SET_PERSONAL = "VPN set in personal profile";
    private static final String KEY_ALWAYS_ON_VPN_PRIMARY_USER = "always_on_vpn_primary_user";

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    private FakeFeatureFactory mFeatureFactory;

    private AlwaysOnVpnCurrentUserPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mFeatureFactory = FakeFeatureFactory.setupForTest();
        mController = new AlwaysOnVpnCurrentUserPreferenceController(mContext);
        when(mContext.getString(R.string.enterprise_privacy_always_on_vpn_device))
                .thenReturn(VPN_SET_DEVICE);
        when(mContext.getString(R.string.enterprise_privacy_always_on_vpn_personal))
                .thenReturn(VPN_SET_PERSONAL);
    }

    @Test
    public void testUpdateState() {
        final Preference preference = new Preference(mContext, null, 0, 0);

        when(mFeatureFactory.enterprisePrivacyFeatureProvider.isAlwaysOnVpnSetInCurrentUser())
                .thenReturn(true);

        when(mFeatureFactory.enterprisePrivacyFeatureProvider.isInCompMode()).thenReturn(false);
        mController.updateState(preference);
        assertThat(preference.getTitle()).isEqualTo(VPN_SET_DEVICE);

        when(mFeatureFactory.enterprisePrivacyFeatureProvider.isInCompMode()).thenReturn(true);
        mController.updateState(preference);
        assertThat(preference.getTitle()).isEqualTo(VPN_SET_PERSONAL);
    }

    @Test
    public void testIsAvailable() {
        when(mFeatureFactory.enterprisePrivacyFeatureProvider.isAlwaysOnVpnSetInCurrentUser())
                .thenReturn(false);
        assertThat(mController.isAvailable()).isFalse();

        when(mFeatureFactory.enterprisePrivacyFeatureProvider.isAlwaysOnVpnSetInCurrentUser())
                .thenReturn(true);
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void testHandlePreferenceTreeClick() {
        assertThat(mController.handlePreferenceTreeClick(new Preference(mContext, null, 0, 0)))
                .isFalse();
    }

    @Test
    public void testGetPreferenceKey() {
        assertThat(mController.getPreferenceKey()).isEqualTo(KEY_ALWAYS_ON_VPN_PRIMARY_USER);
    }
}
