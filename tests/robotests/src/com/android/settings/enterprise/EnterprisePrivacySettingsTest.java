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
import static android.app.admin.DevicePolicyManager.DEVICE_OWNER_TYPE_FINANCED;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.os.Bundle;
import android.provider.SearchIndexableResource;
import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;
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
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class EnterprisePrivacySettingsTest extends AbsBasePrivacySettingsPreference {
    private static final ComponentName DEVICE_OWNER_COMPONENT =
            new ComponentName("com.android.foo", "bar");

    @Mock
    private DevicePolicyManager mDevicePolicyManager;
    private FakeFeatureFactory mFeatureFactory;
    private EnterprisePrivacySettings mSettings;
    private Context mContext;
    private TestActivity mActivity;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = ApplicationProvider.getApplicationContext();
        mFeatureFactory = FakeFeatureFactory.setupForTest();
        mSettings = new EnterprisePrivacySettings();
        mSettings.mPrivacySettingsPreference = new PrivacySettingsEnterprisePreference(mContext);

        ActivityController<TestActivity> controller = Robolectric.buildActivity(
                TestActivity.class).create();
        mActivity = controller.get();

        mActivity
                .getSupportFragmentManager()
                .beginTransaction()
                .add(TestActivity.CONTAINER_VIEW_ID, mSettings)
                .commit();
        controller.start();
    }

    @Test
    public void verifyConstants() {
        assertThat(mSettings.getMetricsCategory())
                .isEqualTo(MetricsEvent.ENTERPRISE_PRIVACY_SETTINGS);
        assertThat(mSettings.getLogTag()).isEqualTo("EnterprisePrivacySettings");
        assertThat(mSettings.getCategoryKey()).isEqualTo(CategoryKey.CATEGORY_ENTERPRISE_PRIVACY);
        assertThat(mSettings.getPreferenceScreenResId())
                .isEqualTo(R.xml.enterprise_privacy_settings);
    }

    @Test
    public void isPageEnabled_hasDeviceOwner_returnsTrue() {
        when(mFeatureFactory.enterprisePrivacyFeatureProvider.hasDeviceOwner())
                .thenReturn(true);

        assertThat(EnterprisePrivacySettings.isPageEnabled(mContext))
                .isTrue();
    }

    @Test
    public void isPageEnabled_noDeviceOwner_returnsFalse() {
        when(mDevicePolicyManager.isDeviceManaged()).thenReturn(false);
        when(mFeatureFactory.enterprisePrivacyFeatureProvider.hasDeviceOwner())
                .thenReturn(false);

        assertThat(EnterprisePrivacySettings.isPageEnabled(mContext))
                .isFalse();
    }

    @Test
    public void getPreferenceControllers_returnsEnterprisePreferenceControllers() {
        final List<AbstractPreferenceController> privacyControllers =
                mSettings.createPreferenceControllers(mContext);

        verifyEnterprisePreferenceControllers(privacyControllers);
    }

    @Test
    public void
            getSearchIndexProviderPreferenceControllers_returnsEnterpriseSearchIndexPreferenceControllers() {
        Context context = spy(ApplicationProvider.getApplicationContext());
        setupPrivacyPreference(context, DEVICE_OWNER_TYPE_DEFAULT);

        final List<AbstractPreferenceController> controllers =
            EnterprisePrivacySettings.SEARCH_INDEX_DATA_PROVIDER
                .getPreferenceControllers(context);

        verifyEnterprisePreferenceControllers(controllers);
    }

    @Test
    public void
            getSearchIndexProviderPreferenceControllers_returnsFinancedSearchIndexPreferenceControllers() {
        Context context = spy(ApplicationProvider.getApplicationContext());
        setupPrivacyPreference(context, DEVICE_OWNER_TYPE_FINANCED);

        final List<AbstractPreferenceController> controllers =
                EnterprisePrivacySettings.SEARCH_INDEX_DATA_PROVIDER
                        .getPreferenceControllers(context);

        verifyFinancedPreferenceControllers(controllers);
    }

    @Test
    public void getXmlResourcesToIndex_returnsEnterpriseXmlResources() {
        Context context = spy(ApplicationProvider.getApplicationContext());
        setupPrivacyPreference(context, DEVICE_OWNER_TYPE_DEFAULT);

        final List<SearchIndexableResource> searchIndexableResources =
                EnterprisePrivacySettings.SEARCH_INDEX_DATA_PROVIDER
                        .getXmlResourcesToIndex(context, true);

        verifyEnterpriseSearchIndexableResources(searchIndexableResources);
    }

    @Test
    public void getXmlResourcesToIndex_returnsFinancedXmlResources() {
        Context context = spy(ApplicationProvider.getApplicationContext());
        setupPrivacyPreference(context, DEVICE_OWNER_TYPE_FINANCED);

        final List<SearchIndexableResource> searchIndexableResources =
                EnterprisePrivacySettings.SEARCH_INDEX_DATA_PROVIDER
                        .getXmlResourcesToIndex(context, true);

        verifyFinancedSearchIndexableResources(searchIndexableResources);
    }

    @Test
    public void onCreate_enterprisePrivacyPreference_updatesTitle() {
        mSettings.onCreate(new Bundle());

        assertThat(mActivity.getTitle())
                .isEqualTo(mContext.getText(R.string.enterprise_privacy_settings));
    }

    @Test
    public void onCreate_financedPrivacyPreference_doesNotUpdateTitle() {
        mSettings.mPrivacySettingsPreference = new PrivacySettingsFinancedPreference(mContext);

        mSettings.onCreate(new Bundle());

        assertThat(mActivity.getTitle())
                .isEqualTo(mContext.getText(R.string.financed_privacy_settings));
    }

    private void setupPrivacyPreference(Context context, int deviceOwnerType) {
        when(context.getSystemService(DevicePolicyManager.class)).thenReturn(mDevicePolicyManager);
        when(mDevicePolicyManager.isDeviceManaged()).thenReturn(true);
        when(mDevicePolicyManager.getDeviceOwnerComponentOnAnyUser())
                .thenReturn(DEVICE_OWNER_COMPONENT);
        when(mDevicePolicyManager.getDeviceOwnerType(DEVICE_OWNER_COMPONENT))
                .thenReturn(deviceOwnerType);
    }

    private static final class TestActivity extends AppCompatActivity {

        private static final int CONTAINER_VIEW_ID = 1234;

        @Override
        protected void onCreate(Bundle bundle) {
            super.onCreate(bundle);

            FrameLayout frameLayout = new FrameLayout(this);
            frameLayout.setId(CONTAINER_VIEW_ID);

            // Need to set the Theme.AppCompat theme (or descendant) with this activity, otherwise
            // a {@link IllegalStateException} is thrown when setting the content view.
            setTheme(androidx.appcompat.R.style.Theme_AppCompat_Light);
            setContentView(frameLayout);
        }
    }
}
