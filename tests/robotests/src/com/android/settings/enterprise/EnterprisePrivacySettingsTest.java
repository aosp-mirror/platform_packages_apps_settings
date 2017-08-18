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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.res.XmlResourceParser;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.TestConfig;
import com.android.settings.core.DynamicAvailabilityPreferenceController;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settingslib.core.AbstractPreferenceController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;
import org.xmlpull.v1.XmlPullParser;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Tests for {@link EnterprisePrivacySettings}.
 */
@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public final class EnterprisePrivacySettingsTest {

    private final static String RESOURCES_NAMESPACE = "http://schemas.android.com/apk/res/android";
    private final static String ATTR_KEY = "key";

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    private FakeFeatureFactory mFeatureFactory;
    private EnterprisePrivacySettings mSettings;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        FakeFeatureFactory.setupForTest(mContext);
        mFeatureFactory = (FakeFeatureFactory) FakeFeatureFactory.getFactory(mContext);

        mSettings = new EnterprisePrivacySettings();
    }

    @Test
    public void testGetMetricsCategory() {
        assertThat(mSettings.getMetricsCategory())
                .isEqualTo(MetricsEvent.ENTERPRISE_PRIVACY_SETTINGS);
    }

    @Test
    public void testGetCategoryKey() {
        assertThat(mSettings.getCategoryKey()).isNull();
    }

    @Test
    public void testGetLogTag() {
        assertThat(mSettings.getLogTag()).isEqualTo("EnterprisePrivacySettings");
    }

    @Test
    public void testGetPreferenceScreenResId() {
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
        when(mFeatureFactory.enterprisePrivacyFeatureProvider.hasDeviceOwner())
                .thenReturn(false);

        assertThat(EnterprisePrivacySettings.isPageEnabled(mContext))
                .isFalse();
    }

    @Test
    public void getPreferenceControllers() throws Exception {
        final List<AbstractPreferenceController> controllers = mSettings.getPreferenceControllers(
                ShadowApplication.getInstance().getApplicationContext());
        verifyPreferenceControllers(controllers);
    }

    @Test
    public void getSearchIndexProviderPreferenceControllers() throws Exception {
        final List<AbstractPreferenceController> controllers
                = EnterprisePrivacySettings.SEARCH_INDEX_DATA_PROVIDER.getPreferenceControllers(
                        ShadowApplication.getInstance().getApplicationContext());
        verifyPreferenceControllers(controllers);
    }

    private void verifyPreferenceControllers(List<AbstractPreferenceController> controllers)
            throws Exception {
        assertThat(controllers).isNotNull();
        assertThat(controllers.size()).isEqualTo(17);
        int position = 0;
        assertThat(controllers.get(position++)).isInstanceOf(NetworkLogsPreferenceController.class);
        assertThat(controllers.get(position++)).isInstanceOf(BugReportsPreferenceController.class);
        assertThat(controllers.get(position++)).isInstanceOf(
                SecurityLogsPreferenceController.class);
        assertThat(controllers.get(position++)).isInstanceOf(
                EnterpriseInstalledPackagesPreferenceController.class);
        assertThat(controllers.get(position++)).isInstanceOf(
                AdminGrantedLocationPermissionsPreferenceController.class);
        assertThat(controllers.get(position++)).isInstanceOf(
                AdminGrantedMicrophonePermissionPreferenceController.class);
        assertThat(controllers.get(position++)).isInstanceOf(
                AdminGrantedCameraPermissionPreferenceController.class);
        assertThat(controllers.get(position++)).isInstanceOf(
                EnterpriseSetDefaultAppsPreferenceController.class);
        assertThat(controllers.get(position++)).isInstanceOf(
                AlwaysOnVpnCurrentUserPreferenceController.class);
        assertThat(controllers.get(position++)).isInstanceOf(
                AlwaysOnVpnManagedProfilePreferenceController.class);
        assertThat(controllers.get(position++)).isInstanceOf(ImePreferenceController.class);
        assertThat(controllers.get(position++)).isInstanceOf(
                GlobalHttpProxyPreferenceController.class);
        assertThat(controllers.get(position++)).isInstanceOf(
                CaCertsCurrentUserPreferenceController.class);
        assertThat(controllers.get(position++)).isInstanceOf(
                CaCertsManagedProfilePreferenceController.class);
        final AbstractPreferenceController exposureChangesCategoryController =
                controllers.get(position);
        final int exposureChangesCategoryControllerIndex = position;
        assertThat(controllers.get(position++)).isInstanceOf(
                ExposureChangesCategoryPreferenceController.class);
        assertThat(controllers.get(position++)).isInstanceOf(
                FailedPasswordWipeCurrentUserPreferenceController.class);
        assertThat(controllers.get(position++)).isInstanceOf(
                FailedPasswordWipeManagedProfilePreferenceController.class);

        // The "Changes made by your organization's admin" category is hidden when all Preferences
        // inside it become unavailable. To do this correctly, the category's controller must:
        // a) Observe the availability of all Preferences in the category and
        // b) Be listed after those Preferences' controllers, so that availability is updated in
        //    the correct order

        // Find all Preferences in the category.
        final XmlResourceParser parser = RuntimeEnvironment.application.getResources().getXml(
                R.xml.enterprise_privacy_settings);
        boolean done = false;
        int type;
        final Set<String> expectedObserved = new HashSet<>();
        while (!done && (type = parser.next()) != XmlPullParser.END_DOCUMENT) {
            if (type != XmlPullParser.START_TAG || !"exposure_changes_category".equals(
                    parser.getAttributeValue(RESOURCES_NAMESPACE, ATTR_KEY))) {
                continue;
            }
            int depth = 1;
            while ((type = parser.next()) != XmlPullParser.END_DOCUMENT) {
                if (type == XmlPullParser.START_TAG) {
                    final String key = parser.getAttributeValue(RESOURCES_NAMESPACE, ATTR_KEY);
                    if (key != null) {
                        expectedObserved.add(key);
                    }
                    depth++;
                } else if (type == XmlPullParser.END_TAG) {
                    depth--;
                    if (depth == 0) {
                        done = true;
                        break;
                    }
                }
            }
        }

        // Find all Preferences the category's controller is observing.
        final Set<String> actualObserved = new HashSet<>();
        int maxObservedIndex = -1;
        for (int i = 0; i < controllers.size(); i++) {
            final AbstractPreferenceController controller = controllers.get(i);
            if (controller instanceof DynamicAvailabilityPreferenceController &&
                    ((DynamicAvailabilityPreferenceController) controller).getAvailabilityObserver()
                            == exposureChangesCategoryController) {
                actualObserved.add(controller.getPreferenceKey());
                maxObservedIndex = i;
            }
        }

        // Verify that the category's controller is observing the Preferences inside it.
        assertThat(actualObserved).isEqualTo(expectedObserved);
        // Verify that the category's controller is listed after the Preferences' controllers.
        assertThat(maxObservedIndex).isLessThan(exposureChangesCategoryControllerIndex);
    }
}
