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
package com.android.settings.core.instrumentation;

import android.content.Context;

import com.android.settings.TestConfig;
import com.android.settings.overlay.FeatureFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class SharedPreferenceLoggerTest {

    private static final String TEST_TAG = "tag";
    private static final String TEST_KEY = "key";

    @Mock
    private LogWriter mLogWriter;

    private MetricsFeatureProvider mMetricsFeature;
    private ShadowApplication mApplication;
    private SharedPreferencesLogger mSharedPrefLogger;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        mApplication = ShadowApplication.getInstance();
        Context context = mApplication.getApplicationContext();
        mMetricsFeature = FeatureFactory.getFactory(context).getMetricsFeatureProvider();
        ((MetricsFeatureProviderImpl) mMetricsFeature).addLogWriter(mLogWriter);

        mSharedPrefLogger = new SharedPreferencesLogger(
                mApplication.getApplicationContext(), TEST_TAG);
    }

    @Test
    public void putInt_shouldLogCount() {
        mSharedPrefLogger.edit().putInt(TEST_KEY, 1);
        verify(mLogWriter).count(any(Context.class), anyString(), anyInt());
    }

    @Test
    public void putBoolean_shouldLogCount() {
        mSharedPrefLogger.edit().putBoolean(TEST_KEY, true);
        verify(mLogWriter).count(any(Context.class), anyString(), anyInt());
    }

    @Test
    public void putLong_shouldLogCount() {
        mSharedPrefLogger.edit().putLong(TEST_KEY, 1);
        verify(mLogWriter).count(any(Context.class), anyString(), anyInt());
    }

    @Test
    public void putFloat_shouldLogCount() {
        mSharedPrefLogger.edit().putInt(TEST_KEY, 1);
        verify(mLogWriter).count(any(Context.class), anyString(), anyInt());
    }

}
