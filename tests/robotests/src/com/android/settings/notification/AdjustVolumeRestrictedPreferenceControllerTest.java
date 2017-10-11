/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings.notification;

import android.content.Context;
import android.os.UserManager;
import android.support.v7.preference.Preference;

import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.accounts.AccountRestrictionHelper;
import com.android.settingslib.RestrictedPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class AdjustVolumeRestrictedPreferenceControllerTest {

    @Mock
    private AccountRestrictionHelper mAccountHelper;

    private Context mContext;
    private AdjustVolumeRestrictedPreferenceControllerTestable mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ShadowApplication shadowContext = ShadowApplication.getInstance();
        mContext = spy(shadowContext.getApplicationContext());
        mController =
            new AdjustVolumeRestrictedPreferenceControllerTestable(mContext, mAccountHelper);
    }

    @Test
    public void updateState_hasBaseRestriction_shouldDisable() {
        RestrictedPreference preference = mock(RestrictedPreference.class);
        when(mAccountHelper.hasBaseUserRestriction(
            eq(UserManager.DISALLOW_ADJUST_VOLUME), anyInt())).thenReturn(true);

        mController.updateState(preference);

        assertThat(preference.isEnabled()).isFalse();
    }

    @Test
    public void updateState_NoBaseRestriction_shouldCheckRestriction() {
        RestrictedPreference preference = spy(new RestrictedPreference(mContext));

        when(mContext.getSystemService(Context.DEVICE_POLICY_SERVICE)).thenReturn(null);
        when(mAccountHelper.hasBaseUserRestriction(
            eq(UserManager.DISALLOW_ADJUST_VOLUME), anyInt())).thenReturn(false);
        doCallRealMethod().when(mAccountHelper).enforceRestrictionOnPreference(
            eq(preference), eq(UserManager.DISALLOW_ADJUST_VOLUME), anyInt());

        mController.updateState(preference);

        verify(preference).checkRestrictionAndSetDisabled(
            eq(UserManager.DISALLOW_ADJUST_VOLUME), anyInt());
    }

    private class AdjustVolumeRestrictedPreferenceControllerTestable extends
        AdjustVolumeRestrictedPreferenceController {
        AdjustVolumeRestrictedPreferenceControllerTestable(Context context,
            AccountRestrictionHelper helper) {
            super(context, helper);
        }

        @Override
        public String getPreferenceKey() {
            return null;
        }

        @Override
        public boolean handlePreferenceTreeClick(Preference preference) {
            return false;
        }

        @Override
        public boolean isAvailable() {
            return true;
        }
    }

}
