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

package com.android.settings.enterprise;

import static android.app.admin.DevicePolicyManager.DEVICE_OWNER_TYPE_DEFAULT;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.provider.SearchIndexableResource;

import androidx.test.core.app.ApplicationProvider;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.drawer.CategoryKey;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class EnterprisePrivacySettingsTest extends AbsBasePrivacySettingsPreference {
    private static final ComponentName DEVICE_OWNER_COMPONENT =
            new ComponentName("com.android.foo", "bar");

    @Mock
    private DevicePolicyManager mDevicePolicyManager;
    @Mock
    private PrivacySettingsPreference mPrivacySettingsPreference;
    private FakeFeatureFactory mFeatureFactory;
    private EnterprisePrivacySettings mSettings;
    private Context mContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(ApplicationProvider.getApplicationContext());
        mFeatureFactory = FakeFeatureFactory.setupForTest();
        mSettings = new EnterprisePrivacySettings();
        mSettings.mPrivacySettingsPreference = mPrivacySettingsPreference;

        when(mContext.getSystemService(DevicePolicyManager.class)).thenReturn(mDevicePolicyManager);
        when(mDevicePolicyManager.isDeviceManaged()).thenReturn(true);
        when(mDevicePolicyManager.getDeviceOwnerComponentOnAnyUser())
                .thenReturn(DEVICE_OWNER_COMPONENT);
        when(mDevicePolicyManager.getDeviceOwnerType(DEVICE_OWNER_COMPONENT))
                .thenReturn(DEVICE_OWNER_TYPE_DEFAULT);
    }

    @Test
    public void verifyConstants() {
        when(mPrivacySettingsPreference.getPreferenceScreenResId())
                .thenReturn(R.xml.enterprise_privacy_settings);

        assertThat(mSettings.getMetricsCategory())
                .isEqualTo(MetricsEvent.ENTERPRISE_PRIVACY_SETTINGS);
        assertThat(mSettings.getLogTag()).isEqualTo("EnterprisePrivacySettings");
        assertThat(mSettings.getCategoryKey()).isEqualTo(CategoryKey.CATEGORY_ENTERPRISE_PRIVACY);
        assertThat(mSettings.getPreferenceScreenResId())
                .isEqualTo(R.xml.enterprise_privacy_settings);
    }

    @Test
    public void isPageEnabled_hasDeviceOwner_shouldReturnTrue() {
        when(mFeatureFactory.enterprisePrivacyFeatureProvider.hasDeviceOwner())
                .thenReturn(true);

        assertThat(EnterprisePrivacySettings.isPageEnabled(mContext))
                .isTrue();
    }

    @Test
    public void isPageEnabled_noDeviceOwner_shouldReturnFalse() {
        when(mDevicePolicyManager.isDeviceManaged()).thenReturn(false);
        when(mFeatureFactory.enterprisePrivacyFeatureProvider.hasDeviceOwner())
                .thenReturn(false);

        assertThat(EnterprisePrivacySettings.isPageEnabled(mContext))
                .isFalse();
    }

    @Test
    public void getPreferenceControllers() {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        controllers.add(new NetworkLogsPreferenceController(mContext));
        when(mPrivacySettingsPreference.createPreferenceControllers(anyBoolean()))
                .thenReturn(controllers);

        final List<AbstractPreferenceController> privacyControllers =
                mSettings.createPreferenceControllers(mContext);

        assertThat(privacyControllers).isNotNull();
        assertThat(privacyControllers.size()).isEqualTo(1);
        assertThat(controllers.get(0)).isInstanceOf(NetworkLogsPreferenceController.class);
    }

    @Test
    public void
            getSearchIndexProviderPreferenceControllers_returnsEnterpriseSearchIndexPreferenceControllers() {
        final List<AbstractPreferenceController> controllers =
            EnterprisePrivacySettings.SEARCH_INDEX_DATA_PROVIDER
                .getPreferenceControllers(mContext);

        verifyEnterprisePreferenceControllers(controllers);
    }

    @Test
    public void getXmlResourcesToIndex_returnsEnterpriseXmlResources() {
        final List<SearchIndexableResource> searchIndexableResources =
                EnterprisePrivacySettings.SEARCH_INDEX_DATA_PROVIDER
                        .getXmlResourcesToIndex(mContext, true);

        verifyEnterpriseSearchIndexableResources(searchIndexableResources);
    }
}
