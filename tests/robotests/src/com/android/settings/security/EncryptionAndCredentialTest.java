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

package com.android.settings.security;

import static com.android.settings.security.EncryptionAndCredential.SEARCH_INDEX_DATA_PROVIDER;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.os.UserManager;
import android.provider.SearchIndexableResource;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.search.BaseSearchIndexProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowApplication;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class EncryptionAndCredentialTest {

    @Mock
    private UserManager mUserManager;
    @Mock
    private DevicePolicyManager mDevicePolicyManager;

    private Context mContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ShadowApplication application = ShadowApplication.getInstance();
        application.setSystemService(Context.DEVICE_POLICY_SERVICE, mDevicePolicyManager);
        application.setSystemService(Context.USER_SERVICE, mUserManager);
        mContext = RuntimeEnvironment.application;
    }

    @Test
    public void getMetricsCategory_shouldReturnEncryptionAndCredential() {
        EncryptionAndCredential fragment = new EncryptionAndCredential();
        assertThat(fragment.getMetricsCategory()).isEqualTo(MetricsEvent.ENCRYPTION_AND_CREDENTIAL);
    }

    @Test
    public void getNonIndexableKeys_pageIsDisabled_shouldReturnAllKeysAsNonIndexable() {
        when(mUserManager.isAdminUser()).thenReturn(false);

        final List<SearchIndexableResource> index =
                SEARCH_INDEX_DATA_PROVIDER.getXmlResourcesToIndex(mContext, true /* enabled */);
        final List<String> expectedKeys = new ArrayList<>();
        for (SearchIndexableResource res : index) {
            expectedKeys.addAll(((BaseSearchIndexProvider) SEARCH_INDEX_DATA_PROVIDER)
                    .getNonIndexableKeysFromXml(mContext, res.xmlResId, true /* suppressAll */));
        }
        final List<String> keys = SEARCH_INDEX_DATA_PROVIDER.getNonIndexableKeys(mContext);

        assertThat(keys).containsExactlyElementsIn(expectedKeys);
    }
}
