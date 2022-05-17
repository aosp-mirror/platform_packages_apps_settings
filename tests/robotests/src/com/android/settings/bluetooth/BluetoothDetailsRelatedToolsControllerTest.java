/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.bluetooth;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ComponentName;
import android.content.pm.ApplicationInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.view.accessibility.AccessibilityManager;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;

import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowAccessibilityManager;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.List;

/** Tests for {@link BluetoothDetailsRelatedToolsController}. */
@RunWith(RobolectricTestRunner.class)
public class BluetoothDetailsRelatedToolsControllerTest extends BluetoothDetailsControllerTestBase {
    @Rule
    public final MockitoRule mockito = MockitoJUnit.rule();

    private static final String PACKAGE_NAME = "com.android.test";
    private static final String PACKAGE_NAME2 = "com.android.test2";
    private static final String CLASS_NAME = PACKAGE_NAME + ".test_a11y_service";
    private static final String KEY_RELATED_TOOLS_GROUP = "bluetooth_related_tools";
    private static final String KEY_LIVE_CAPTION = "live_caption";


    private BluetoothDetailsRelatedToolsController mController;
    private BluetoothFeatureProvider mFeatureProvider;
    private ShadowAccessibilityManager mShadowAccessibilityManager;
    private PreferenceCategory mPreferenceCategory;

    @Override
    public void setUp() {
        super.setUp();
        final FakeFeatureFactory fakeFeatureFactory = FakeFeatureFactory.setupForTest();
        mFeatureProvider = fakeFeatureFactory.getBluetoothFeatureProvider();
        mShadowAccessibilityManager = Shadow.extract(AccessibilityManager.getInstance(mContext));
        final Preference preference = new Preference(mContext);
        preference.setKey(KEY_LIVE_CAPTION);
        mPreferenceCategory = new PreferenceCategory(mContext);
        mPreferenceCategory.setKey(KEY_RELATED_TOOLS_GROUP);
        mScreen.addPreference(mPreferenceCategory);
        mScreen.addPreference(preference);

        mController = new BluetoothDetailsRelatedToolsController(mContext, mFragment, mCachedDevice,
                mLifecycle);
        mController.init(mScreen);
    }

    @Test
    public void isAvailable_isHearingAidDevice_available() {
        when(mCachedDevice.isHearingAidDevice()).thenReturn(true);

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_notHearingAidDevice_notAvailable() {
        when(mCachedDevice.isHearingAidDevice()).thenReturn(false);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void displayPreference_oneRelatedToolsMatchA11yService_showOnePreference() {
        when(mCachedDevice.isHearingAidDevice()).thenReturn(true);
        mShadowAccessibilityManager.setInstalledAccessibilityServiceList(
                List.of(getMockAccessibilityServiceInfo(PACKAGE_NAME, CLASS_NAME)));
        when(mFeatureProvider.getRelatedTools()).thenReturn(
                List.of(new ComponentName(PACKAGE_NAME, CLASS_NAME)));

        mController.displayPreference(mScreen);

        assertThat(mPreferenceCategory.getPreferenceCount()).isEqualTo(1);
    }

    @Test
    public void displayPreference_oneRelatedToolsNotMatchA11yService_showNoPreference() {
        when(mCachedDevice.isHearingAidDevice()).thenReturn(true);
        mShadowAccessibilityManager.setInstalledAccessibilityServiceList(
                List.of(getMockAccessibilityServiceInfo(PACKAGE_NAME, CLASS_NAME)));
        when(mFeatureProvider.getRelatedTools()).thenReturn(
                List.of(new ComponentName(PACKAGE_NAME2, CLASS_NAME)));

        mController.displayPreference(mScreen);

        assertThat(mPreferenceCategory.getPreferenceCount()).isEqualTo(0);
    }

    @Test
    public void displayPreference_noRelatedTools_showNoPreference() {
        when(mCachedDevice.isHearingAidDevice()).thenReturn(true);
        mShadowAccessibilityManager.setInstalledAccessibilityServiceList(
                List.of(getMockAccessibilityServiceInfo(PACKAGE_NAME, CLASS_NAME)));
        when(mFeatureProvider.getRelatedTools()).thenReturn(null);

        mController.displayPreference(mScreen);

        assertThat(mPreferenceCategory.getPreferenceCount()).isEqualTo(0);
    }

    private AccessibilityServiceInfo getMockAccessibilityServiceInfo(String packageName,
            String className) {
        final ApplicationInfo applicationInfo = new ApplicationInfo();
        final ServiceInfo serviceInfo = new ServiceInfo();
        applicationInfo.packageName = packageName;
        serviceInfo.packageName = packageName;
        serviceInfo.name = className;
        serviceInfo.applicationInfo = applicationInfo;

        final ResolveInfo resolveInfo = new ResolveInfo();
        resolveInfo.serviceInfo = serviceInfo;

        try {
            final AccessibilityServiceInfo info = new AccessibilityServiceInfo(resolveInfo,
                    mContext);
            ComponentName componentName = ComponentName.unflattenFromString(
                    packageName + "/" + className);
            info.setComponentName(componentName);
            return info;
        } catch (XmlPullParserException | IOException e) {
            // Do nothing
        }

        return null;
    }
}
