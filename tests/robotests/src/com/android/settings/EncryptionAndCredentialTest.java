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
 * limitations under the License
 */

package com.android.settings;

import static android.app.admin.DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE;
import static android.app.admin.DevicePolicyManager.ENCRYPTION_STATUS_INACTIVE;
import static com.android.settings.EncryptionAndCredential.SEARCH_INDEX_DATA_PROVIDER;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.when;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.os.UserManager;
import android.provider.SearchIndexableResource;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import java.util.ArrayList;
import java.util.List;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class EncryptionAndCredentialTest {

    @Mock
    private UserManager mUserManager;
    @Mock
    private DevicePolicyManager mDevicePolicyManager;

    private ShadowApplication mApplication;
    private Context mContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mApplication = ShadowApplication.getInstance();
        mApplication.setSystemService(Context.DEVICE_POLICY_SERVICE, mDevicePolicyManager);
        mApplication.setSystemService(Context.USER_SERVICE, mUserManager);
        mContext = mApplication.getApplicationContext();
    }

    @Test
    public void getMetricsCategory_shouldReturnEncryptionAndCredential() {
        EncryptionAndCredential fragment = new EncryptionAndCredential();
        assertThat(fragment.getMetricsCategory()).isEqualTo(MetricsEvent.ENCRYPTION_AND_CREDENTIAL);
    }

    // Search provider tests
    @Test
    public void getXmlResourcesToIndex_shouldReturnAllXmls() {
        final List<SearchIndexableResource> index =
                SEARCH_INDEX_DATA_PROVIDER.getXmlResourcesToIndex(
                        mContext, true /* enabled */);

        assertThat(index).hasSize(3);
    }

    @Test
    public void getNonIndexableKeys_pageIsDisabled_shouldReturnAllKeysAsNonIndexable() {
        when(mUserManager.isAdminUser()).thenReturn(false);

        final List<SearchIndexableResource> index =
                SEARCH_INDEX_DATA_PROVIDER.getXmlResourcesToIndex(mContext, true /* enabled */);
        final List<String> expectedKeys = new ArrayList<>();
        for (SearchIndexableResource res : index) {
            expectedKeys.addAll(((BaseSearchIndexProvider) SEARCH_INDEX_DATA_PROVIDER)
                    .getNonIndexableKeysFromXml(mContext, res.xmlResId));
        }
        final List<String> keys = SEARCH_INDEX_DATA_PROVIDER.getNonIndexableKeys(mContext);

        assertThat(keys).containsExactlyElementsIn(expectedKeys);
    }

    @Test
    public void getNonIndexableKeys_deviceEncrypted_shouldReturnUnencryptedKeys() {
        when(mUserManager.isAdminUser()).thenReturn(true);
        when(mDevicePolicyManager.getStorageEncryptionStatus()).thenReturn(
                ENCRYPTION_STATUS_ACTIVE);

        final List<String> expectedKeys = new ArrayList<>();
        expectedKeys.addAll(((BaseSearchIndexProvider) SEARCH_INDEX_DATA_PROVIDER)
                .getNonIndexableKeysFromXml(mContext, R.xml.security_settings_unencrypted));
        final List<String> keys = SEARCH_INDEX_DATA_PROVIDER.getNonIndexableKeys(mContext);

        assertThat(keys).containsExactlyElementsIn(expectedKeys);
    }

    @Test
    public void getNonIndexableKeys_deviceNotEncrypted_shouldReturnEncryptedKeys() {
        when(mUserManager.isAdminUser()).thenReturn(true);
        when(mDevicePolicyManager.getStorageEncryptionStatus())
                .thenReturn(ENCRYPTION_STATUS_INACTIVE);

        final List<String> expectedKeys = new ArrayList<>();
        expectedKeys.addAll(((BaseSearchIndexProvider) SEARCH_INDEX_DATA_PROVIDER)
                .getNonIndexableKeysFromXml(mContext, R.xml.security_settings_encrypted));
        final List<String> keys = SEARCH_INDEX_DATA_PROVIDER.getNonIndexableKeys(mContext);

        assertThat(keys).containsExactlyElementsIn(expectedKeys);
    }
}
