/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.core;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;

import androidx.preference.PreferenceManager;

import com.android.settings.R;
import com.android.settings.slices.FakePreferenceController;
import com.android.settingslib.core.AbstractPreferenceController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class PreferenceControllerListHelperTest {

    private Context mContext;
    private PreferenceManager mPreferenceManager;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mPreferenceManager = new PreferenceManager(mContext);
    }

    @Test
    @Config(qualifiers = "mcc999")
    public void getControllers_shouldReturnAList() {
        final List<BasePreferenceController> controllers =
                PreferenceControllerListHelper.getPreferenceControllersFromXml(mContext,
                        R.xml.location_settings);

        assertThat(controllers).isNotEmpty();
        for (BasePreferenceController controller : controllers) {
            assertThat(controller).isInstanceOf(FakePreferenceController.class);
        }
    }

    @Test
    @Config(qualifiers = "mcc998")
    public void getControllers_partialFailure_shouldReturnTheRest() {
        final List<BasePreferenceController> controllers =
                PreferenceControllerListHelper.getPreferenceControllersFromXml(mContext,
                        R.xml.location_settings);

        assertThat(controllers).hasSize(1);
        assertThat(controllers.get(0)).isInstanceOf(FakePreferenceController.class);
    }

    @Test
    @Config(qualifiers = "mcc999")
    public void areAllPreferencesUnavailable_allAvailable() {
        // All preferences have controllers indicating they are available.
        assertThat(PreferenceControllerListHelper.areAllPreferencesUnavailable(mContext,
                mPreferenceManager, R.xml.location_settings)).isFalse();
    }

    @Test
    @Config(qualifiers = "mcc997")
    public void areAllPreferencesUnavailable_allUnavailable() {
        // All preferences have controllers indicating they are unavailable. (note the qualifier)
        assertThat(PreferenceControllerListHelper.areAllPreferencesUnavailable(mContext,
                mPreferenceManager, R.xml.location_settings)).isTrue();
    }

    @Test
    @Config(qualifiers = "mcc999")
    public void areAllPreferencesUnavailable_noControllersShouldAssumeAvailable() {
        // None of the preferences have controllers, so they are assumed available.
        assertThat(PreferenceControllerListHelper.areAllPreferencesUnavailable(mContext,
                mPreferenceManager, R.xml.display_settings)).isFalse();
    }

    @Test
    public void filterControllers_noFilter_shouldReturnSameList() {
        final List<BasePreferenceController> controllers = new ArrayList<>();
        controllers.add(new BasePreferenceController(mContext, "key") {
            @Override
            public int getAvailabilityStatus() {
                return AVAILABLE;
            }
        });
        final List<BasePreferenceController> result = PreferenceControllerListHelper
                .filterControllers(controllers, null /* filter */);
        assertThat(result).containsExactlyElementsIn(controllers);
    }

    @Test
    public void filterControllers_noDuplicationFromFilter_shouldReturnSameList() {
        final List<BasePreferenceController> controllers = new ArrayList<>();
        controllers.add(new BasePreferenceController(mContext, "key") {
            @Override
            public int getAvailabilityStatus() {
                return AVAILABLE;
            }
        });
        final List<AbstractPreferenceController> filter = new ArrayList<>();
        filter.add(new BasePreferenceController(mContext, "key2") {
            @Override
            public int getAvailabilityStatus() {
                return AVAILABLE;
            }
        });
        final List<BasePreferenceController> result = PreferenceControllerListHelper
                .filterControllers(controllers, filter);
        assertThat(result).containsExactlyElementsIn(controllers);
    }

    @Test
    public void filterControllers_hasDuplicationFromFilter_shouldReturnSameList() {
        final List<BasePreferenceController> controllers = new ArrayList<>();
        controllers.add(new BasePreferenceController(mContext, "key") {
            @Override
            public int getAvailabilityStatus() {
                return AVAILABLE;
            }
        });
        final List<AbstractPreferenceController> filter = new ArrayList<>();
        filter.add(new BasePreferenceController(mContext, "key") {
            @Override
            public int getAvailabilityStatus() {
                return AVAILABLE;
            }
        });
        final List<BasePreferenceController> result = PreferenceControllerListHelper
                .filterControllers(controllers, filter);
        assertThat(result).isEmpty();
    }
}
