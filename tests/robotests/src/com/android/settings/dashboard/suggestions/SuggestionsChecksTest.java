/*
 * Copyright (C) 2017 Google Inc.
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

package com.android.settings.dashboard.suggestions;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;

import com.android.settings.Settings;
import com.android.settings.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settingslib.drawer.Tile;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class SuggestionsChecksTest {

    @Mock
    private Context mContext;
    @Mock
    private PackageManager mPackageManager;
    @Mock
    private FingerprintManager mFingerprintManager;
    @Mock
    private DevicePolicyManager mDevicePolicyManager;

    private SuggestionsChecks mSuggestionsChecks;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        when(mContext.getApplicationContext()).thenReturn(mContext);
        mSuggestionsChecks = new SuggestionsChecks(mContext);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        when(mContext.getSystemService(eq(Context.DEVICE_POLICY_SERVICE)))
                .thenReturn(mDevicePolicyManager);
        when(mDevicePolicyManager.getKeyguardDisabledFeatures(any(), anyInt()))
                .thenReturn(0);
        when(mContext.getSystemService(FingerprintManager.class)).thenReturn(mFingerprintManager);
    }

    @Test
    public void testFingerprintEnrollmentIntroductionIsCompleteWhenFingerprintAdded() {
        stubFingerprintSupported(true);
        when(mFingerprintManager.hasEnrolledFingerprints()).thenReturn(true);
        Tile tile = createFingerprintTile();
        assertThat(mSuggestionsChecks.isSuggestionComplete(tile)).isTrue();
    }

    @Test
    public void testFingerprintEnrollmentIntroductionIsNotCompleteWhenNoFingerprintAdded() {
        stubFingerprintSupported(true);
        when(mFingerprintManager.hasEnrolledFingerprints()).thenReturn(false);
        Tile tile = createFingerprintTile();
        assertThat(mSuggestionsChecks.isSuggestionComplete(tile)).isFalse();
    }


    @Test
    public void testFingerprintEnrollmentIntroductionIsCompleteWhenFingerprintNotSupported() {
        stubFingerprintSupported(false);
        Tile tile = createFingerprintTile();
        assertThat(mSuggestionsChecks.isSuggestionComplete(tile)).isTrue();
    }

    @Test
    public void testFingerprintEnrollmentIntroductionIsCompleteWhenFingerprintDisabled() {
        stubFingerprintSupported(true);
        when(mFingerprintManager.hasEnrolledFingerprints()).thenReturn(false);
        when(mDevicePolicyManager.getKeyguardDisabledFeatures(any(), anyInt()))
                .thenReturn(DevicePolicyManager.KEYGUARD_DISABLE_FINGERPRINT);

        Tile tile = createFingerprintTile();
        assertThat(mSuggestionsChecks.isSuggestionComplete(tile)).isTrue();
    }

    private void stubFingerprintSupported(boolean enabled) {
        when(mPackageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT))
                .thenReturn(enabled);
    }

    private Tile createFingerprintTile() {
        Tile tile = new Tile();
        tile.intent = new Intent();
        tile.intent.setComponent(new ComponentName(mContext,
                Settings.FingerprintEnrollSuggestionActivity.class));
        return tile;
    }
}
