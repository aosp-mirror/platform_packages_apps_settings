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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.Context;

import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import com.android.settingslib.core.AbstractPreferenceController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class ControllerTaskTest {
    private static final String KEY = "my_key";

    private Context mContext;
    private PreferenceScreen mScreen;
    private TestPreferenceController mTestController;
    private ControllerTask mControllerTask;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        final PreferenceManager preferenceManager = new PreferenceManager(mContext);
        mScreen = preferenceManager.createPreferenceScreen(mContext);
        mTestController = spy(new TestPreferenceController(mContext));
        mControllerTask = new ControllerTask(mTestController, mScreen, null /* metricsFeature */,
                METRICS_CATEGORY_UNKNOWN);
    }

    @Test
    public void doRun_controlNotAvailable_noRunUpdateState() {
        mTestController.setAvailable(false);

        mControllerTask.run();

        verify(mTestController, never()).updateState(any(Preference.class));
    }

    @Test
    public void doRun_emptyKey_noRunUpdateState() {
        mTestController.setKey("");

        mControllerTask.run();

        verify(mTestController, never()).updateState(any(Preference.class));
    }

    @Test
    public void doRun_preferenceNotExist_noRunUpdateState() {
        mTestController.setKey(KEY);

        mControllerTask.run();

        verify(mTestController, never()).updateState(any(Preference.class));
    }

    @Test
    public void doRun_executeUpdateState() {
        mTestController.setKey(KEY);
        final Preference preference = new Preference(mContext);
        preference.setKey(KEY);
        mScreen.addPreference(preference);

        mControllerTask.run();

        verify(mTestController).updateState(any(Preference.class));
    }

    static class TestPreferenceController extends AbstractPreferenceController {
        private boolean mAvailable;
        private String mKey;

        TestPreferenceController(Context context) {
            super(context);
            mAvailable = true;
        }

        @Override
        public boolean isAvailable() {
            return mAvailable;
        }

        @Override
        public String getPreferenceKey() {
            return mKey;
        }

        void setAvailable(boolean available) {
            mAvailable = available;
        }

        void setKey(String key) {
            mKey = key;
        }
    }
}
