/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.applications;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE;

import static com.google.common.truth.Truth.assertThat;


import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.Bundle;

import com.android.settings.applications.appcompat.UserAspectRatioAppsPreferenceController;
import com.android.settings.testutils.XmlTestUtils;
import com.android.settings.testutils.shadow.ShadowUserManager;
import com.android.settings.widget.PreferenceCategoryController;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.drawer.CategoryKey;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.androidx.fragment.FragmentController;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class AppDashboardFragmentTest {

    private Context mContext;
    private AppDashboardFragment mFragment;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mFragment = new AppDashboardFragment();
    }

    @Test
    public void testCategory_isApps() {
        assertThat(mFragment.getCategoryKey()).isEqualTo(CategoryKey.CATEGORY_APPS);
    }

    @Test
    @Config(shadows = ShadowUserManager.class)
    public void testPreferenceControllers_existInPreferenceScreen() {
        final List<String> preferenceScreenKeys = XmlTestUtils.getKeysFromPreferenceXml(mContext,
                mFragment.getPreferenceScreenResId());
        final List<String> preferenceKeys = new ArrayList<>();

        for (AbstractPreferenceController controller : mFragment.createPreferenceControllers(
                mContext)) {
            preferenceKeys.add(controller.getPreferenceKey());
        }

        assertThat(preferenceScreenKeys).containsAtLeastElementsIn(preferenceKeys);
    }

    @Ignore("b/313578776")
    @Test
    @Config(shadows = ShadowUserManager.class)
    public void testAdvancedAppsCategory() {
        AppDashboardFragment fragment = FragmentController.of(new AppDashboardFragment(),
                new Bundle()).create().get();
        UserAspectRatioAppsPreferenceController controller =
                mock(UserAspectRatioAppsPreferenceController.class);
        final PreferenceCategoryController advancedController =
                fragment.getAdvancedAppsPreferenceCategoryController();
        advancedController.setChildren(List.of(controller));

        when(controller.getAvailabilityStatus()).thenReturn(AVAILABLE);
        assertTrue(advancedController.isAvailable());

        when(controller.getAvailabilityStatus()).thenReturn(CONDITIONALLY_UNAVAILABLE);
        assertFalse(advancedController.isAvailable());
    }
}
