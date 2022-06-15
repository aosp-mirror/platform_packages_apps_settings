/*
 * Copyright (C) 2019 The Android Open Source Project
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
package com.android.settings.dashboard;

import static com.android.settingslib.core.instrumentation.Instrumentable.METRICS_CATEGORY_UNKNOWN;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;

import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class ControllerFutureTaskTest {
    private static final String KEY = "my_key";

    private Context mContext;
    private TestPreferenceController mTestController;
    private PreferenceScreen mScreen;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mTestController = new TestPreferenceController(mContext, KEY);
        final PreferenceManager preferenceManager = new PreferenceManager(mContext);
        mScreen = preferenceManager.createPreferenceScreen(mContext);
    }

    @Test
    public void getController_addTask_checkControllerKey() {
        final ControllerFutureTask futureTask = new ControllerFutureTask(
                new ControllerTask(mTestController, mScreen, null /* metricsFeature */,
                        METRICS_CATEGORY_UNKNOWN), null /* result */);

        assertThat(futureTask.getController().getPreferenceKey()).isEqualTo(KEY);
    }


    static class TestPreferenceController extends BasePreferenceController {
        TestPreferenceController(Context context, String preferenceKey) {
            super(context, preferenceKey);
        }

        @Override
        public int getAvailabilityStatus() {
            return AVAILABLE;
        }
    }
}
