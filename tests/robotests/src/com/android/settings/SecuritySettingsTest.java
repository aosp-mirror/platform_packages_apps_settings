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

package com.android.settings;

import android.content.Context;
import android.content.IContentProvider;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.provider.Settings;

import com.android.settings.dashboard.SummaryLoader;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.ShadowSecureSettings;
import com.android.settingslib.drawer.DashboardCategory;
import com.android.settingslib.drawer.Tile;
import com.android.settingslib.drawer.TileUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import java.util.Map;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class SecuritySettingsTest {

    private static final String MOCK_SUMMARY = "summary";

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock
    private DashboardCategory mDashboardCategory;
    @Mock
    private SummaryLoader mSummaryLoader;

    private SecuritySettings.SummaryProvider mSummaryProvider;

    @Implements(com.android.settingslib.drawer.TileUtils.class)
    public static class ShadowTileUtils {
        @Implementation
        public static String getTextFromUri(Context context, String uriString,
                Map<String, IContentProvider> providerMap, String key) {
            return MOCK_SUMMARY;
        }
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        FakeFeatureFactory.setupForTest(mContext);
        mSummaryProvider = new SecuritySettings.SummaryProvider(mContext, mSummaryLoader);
    }

    @Test
    public void testSummaryProvider_notListening() {
        mSummaryProvider.setListening(false);

        verifyNoMoreInteractions(mSummaryLoader);
    }

    @Test
    @Config(shadows = {
            ShadowSecureSettings.class,
    })
    public void testSummaryProvider_packageVerifierDisabled() {
        // Package verifier state is set to disabled.
        ShadowSecureSettings.putInt(null, Settings.Secure.PACKAGE_VERIFIER_STATE, -1);
        mSummaryProvider.setListening(true);

        verify(mSummaryLoader, times(1)).setSummary(any(), isNull(String.class));
    }

    @Test
    @Config(shadows = {
            ShadowSecureSettings.class,
    })
    public void testSummaryProvider_hasFingerPrint_hasStaticSummary() {
        // Package verifier state is set to disabled.
        ShadowSecureSettings.putInt(null, Settings.Secure.PACKAGE_VERIFIER_STATE, -1);
        final FingerprintManager fpm = mock(FingerprintManager.class);
        when(mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_FINGERPRINT))
                .thenReturn(true);

        // Cast to Object to workaround a robolectric casting bug
        when((Object) mContext.getSystemService(FingerprintManager.class)).thenReturn(fpm);
        when(fpm.isHardwareDetected()).thenReturn(true);

        mSummaryProvider.setListening(true);

        verify(mContext).getString(R.string.security_dashboard_summary);
    }


    @Test
    public void testGetPackageVerifierSummary_nullInput() {
        assertThat(mSummaryProvider.getPackageVerifierSummary(null)).isNull();

        when(mDashboardCategory.getTilesCount()).thenReturn(0);

        assertThat(mSummaryProvider.getPackageVerifierSummary(mDashboardCategory)).isNull();
    }

    @Test
    public void testGetPackageVerifierSummary_noMatchingTile() {
        when(mDashboardCategory.getTilesCount()).thenReturn(1);
        when(mDashboardCategory.getTile(0)).thenReturn(new Tile());

        assertThat(mSummaryProvider.getPackageVerifierSummary(mDashboardCategory)).isNull();
    }

    @Test
    @Config(shadows = {
            ShadowTileUtils.class,
    })
    public void testGetPackageVerifierSummary_matchingTile() {
        when(mDashboardCategory.getTilesCount()).thenReturn(1);
        Tile tile = new Tile();
        tile.key = SecuritySettings.KEY_PACKAGE_VERIFIER_STATUS;
        Bundle bundle = new Bundle();
        bundle.putString(TileUtils.META_DATA_PREFERENCE_SUMMARY_URI, "content://host/path");
        tile.metaData = bundle;
        when(mDashboardCategory.getTile(0)).thenReturn(tile);

        assertThat(mSummaryProvider.getPackageVerifierSummary(mDashboardCategory))
                .isEqualTo(MOCK_SUMMARY);
    }
}
