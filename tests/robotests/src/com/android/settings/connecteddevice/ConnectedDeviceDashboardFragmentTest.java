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
 * limitations under the License
 */
package com.android.settings.connecteddevice;

import android.content.Context;
import android.content.pm.PackageManager;
import android.nfc.NfcAdapter;
import android.nfc.NfcManager;
import android.provider.SearchIndexableResource;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.nfc.NfcPreferenceController;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.dashboard.SummaryLoader;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.XmlTestUtils;
import com.android.settingslib.drawer.CategoryKey;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.List;

import static android.content.Context.NFC_SERVICE;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class ConnectedDeviceDashboardFragmentTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    Context mContext;

    @Mock
    private PackageManager mManager;

    private FakeFeatureFactory mFeatureFactory;
    private SmsMirroringFeatureProvider mFeatureProvider;
    private ConnectedDeviceDashboardFragment mFragment;
    private TestSmsMirroringPreferenceController mSmsMirroringPreferenceController;

    private static final class TestSmsMirroringPreferenceController
            extends SmsMirroringPreferenceController implements PreferenceControllerMixin {

        private boolean mIsAvailable;

        public TestSmsMirroringPreferenceController(Context context) {
            super(context);
        }

        @Override
        public boolean isAvailable() {
            return mIsAvailable;
        }
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        FakeFeatureFactory.setupForTest(mContext);
        mFeatureFactory = (FakeFeatureFactory) FakeFeatureFactory.getFactory(mContext);
        mFeatureProvider = mFeatureFactory.smsMirroringFeatureProvider;

        mFragment = new ConnectedDeviceDashboardFragment();
        when(mContext.getPackageManager()).thenReturn(mManager);

        mSmsMirroringPreferenceController = new TestSmsMirroringPreferenceController(mContext);
        when(mFeatureProvider.getController(mContext)).thenReturn(
                mSmsMirroringPreferenceController);
    }

    @Test
    public void testCategory_isConnectedDevice() {
        assertThat(mFragment.getCategoryKey()).isEqualTo(CategoryKey.CATEGORY_DEVICE);
    }

    @Test
    public void testSearchIndexProvider_shouldIndexResource() {
        final List<SearchIndexableResource> indexRes =
                mFragment.SEARCH_INDEX_DATA_PROVIDER.getXmlResourcesToIndex(mContext,
                        true /* enabled */);

        assertThat(indexRes).isNotNull();
        assertThat(indexRes.get(0).xmlResId).isEqualTo(mFragment.getPreferenceScreenResId());
    }

    @Test
    public void testSearchIndexProvider_NoNfc_KeyAdded() {
        when(mManager.hasSystemFeature(PackageManager.FEATURE_NFC)).thenReturn(false);
        final List<String> keys = mFragment.SEARCH_INDEX_DATA_PROVIDER.getNonIndexableKeys(
                mContext);

        assertThat(keys).isNotNull();
        assertThat(keys).contains(NfcPreferenceController.KEY_TOGGLE_NFC);
        assertThat(keys).contains(NfcPreferenceController.KEY_ANDROID_BEAM_SETTINGS);
    }

    @Test
    public void testSearchIndexProvider_NFC_KeyNotAdded() {
        when(mManager.hasSystemFeature(PackageManager.FEATURE_NFC)).thenReturn(true);
        final List<String> keys = mFragment.SEARCH_INDEX_DATA_PROVIDER.getNonIndexableKeys(
                mContext);

        assertThat(keys).isNotNull();
        assertThat(keys).doesNotContain(NfcPreferenceController.KEY_TOGGLE_NFC);
        assertThat(keys).doesNotContain(NfcPreferenceController.KEY_ANDROID_BEAM_SETTINGS);
    }

    @Test
    public void testSearchIndexProvider_NoSmsMirroring_KeyAdded() {
        when(mFeatureProvider.shouldShowSmsMirroring(mContext)).thenReturn(false);
        mSmsMirroringPreferenceController.mIsAvailable = false;

        final List<String> keys = mFragment.SEARCH_INDEX_DATA_PROVIDER.getNonIndexableKeys(
                mContext);

        assertThat(keys).isNotNull();
        assertThat(keys).contains(mSmsMirroringPreferenceController.getPreferenceKey());
    }

    @Test
    public void testSearchIndexProvider_SmsMirroring_KeyNotAdded() {
        when(mFeatureProvider.shouldShowSmsMirroring(mContext)).thenReturn(true);
        mSmsMirroringPreferenceController.mIsAvailable = true;

        final List<String> keys = mFragment.SEARCH_INDEX_DATA_PROVIDER.getNonIndexableKeys(
                mContext);

        assertThat(keys).isNotNull();
        assertThat(keys).doesNotContain(mSmsMirroringPreferenceController.getPreferenceKey());
    }

    @Test
    public void testNonIndexableKeys_existInXmlLayout() {
        final Context context = RuntimeEnvironment.application;
        when(mManager.hasSystemFeature(PackageManager.FEATURE_NFC)).thenReturn(false);
        final List<String> niks = ConnectedDeviceDashboardFragment.SEARCH_INDEX_DATA_PROVIDER
                .getNonIndexableKeys(mContext);
        final int xmlId = (new ConnectedDeviceDashboardFragment()).getPreferenceScreenResId();

        final List<String> keys = XmlTestUtils.getKeysFromPreferenceXml(context, xmlId);

        assertThat(keys).containsAllIn(niks);
    }

    @Test
    public void testSummaryProvider_hasNfc_shouldReturnNfcSummary() {
        final NfcManager nfcManager = mock(NfcManager.class);
        final SummaryLoader summaryLoader = mock(SummaryLoader.class);

        when(mContext.getApplicationContext()).thenReturn(mContext);
        when(mContext.getSystemService(NFC_SERVICE)).thenReturn(nfcManager);
        when(nfcManager.getDefaultAdapter()).thenReturn(mock(NfcAdapter.class));

        SummaryLoader.SummaryProvider provider =
                new ConnectedDeviceDashboardFragment.SummaryProvider(mContext, summaryLoader);

        provider.setListening(false);

        verifyZeroInteractions(summaryLoader);

        provider.setListening(true);

        verify(mContext).getString(R.string.connected_devices_dashboard_summary);
    }

    @Test
    public void testSummaryProvider_noNfc_shouldReturnNoNfcSummary() {
        final SummaryLoader summaryLoader = mock(SummaryLoader.class);

        when(mContext.getApplicationContext()).thenReturn(mContext);
        when(mContext.getSystemService(NFC_SERVICE)).thenReturn(null);

        SummaryLoader.SummaryProvider provider =
                new ConnectedDeviceDashboardFragment.SummaryProvider(mContext, summaryLoader);

        provider.setListening(false);

        verifyZeroInteractions(summaryLoader);

        provider.setListening(true);

        verify(mContext).getString(R.string.connected_devices_dashboard_no_nfc_summary);
    }
}
