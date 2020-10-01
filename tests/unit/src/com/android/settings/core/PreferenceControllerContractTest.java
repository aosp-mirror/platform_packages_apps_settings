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

package com.android.settings.core;

import static junit.framework.Assert.fail;

import android.content.Context;
import android.os.Looper;
import android.platform.test.annotations.Presubmit;
import android.util.ArraySet;

import androidx.test.InstrumentationRegistry;
import androidx.test.filters.MediumTest;
import androidx.test.runner.AndroidJUnit4;

import com.android.settings.overlay.FeatureFactory;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.search.SearchIndexableData;
import com.android.settingslib.search.SearchIndexableResources;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.Set;

@RunWith(AndroidJUnit4.class)
@MediumTest
public class PreferenceControllerContractTest {

    private Context mContext;

    @Before
    public void setUp() {
        mContext = InstrumentationRegistry.getTargetContext();
    }

    @Test
    @Presubmit
    public void controllersInSearchShouldImplementPreferenceControllerMixin() {
        Looper.prepare(); // Required by AutofillLoggingLevelPreferenceController
        final Set<String> errorClasses = new ArraySet<>();

        final SearchIndexableResources resources =
                FeatureFactory.getFactory(mContext).getSearchFeatureProvider()
                        .getSearchIndexableResources();
        for (SearchIndexableData bundle : resources.getProviderValues()) {

            final BaseSearchIndexProvider provider =
                    (BaseSearchIndexProvider) bundle.getSearchIndexProvider();
            if (provider == null) {
                continue;
            }

            final List<AbstractPreferenceController> controllers =
                    provider.getPreferenceControllers(mContext);
            if (controllers == null) {
                continue;
            }
            for (AbstractPreferenceController controller : controllers) {
                if (!(controller instanceof PreferenceControllerMixin)
                        && !(controller instanceof BasePreferenceController)) {
                    errorClasses.add(controller.getClass().getName());
                }
            }
        }

        if (!errorClasses.isEmpty()) {
            final StringBuilder errorMessage = new StringBuilder()
                    .append("Each preference must implement PreferenceControllerMixin ")
                    .append("or extend BasePreferenceController, ")
                    .append("the following classes don't:\n");
            for (String c : errorClasses) {
                errorMessage.append(c).append("\n");
            }
            fail(errorMessage.toString());
        }
    }
}
