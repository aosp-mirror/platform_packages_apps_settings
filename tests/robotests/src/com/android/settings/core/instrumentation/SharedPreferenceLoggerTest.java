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
import android.content.SharedPreferences;
import android.util.Pair;

import com.android.settings.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.testutils.FakeFeatureFactory;

import com.google.common.truth.Platform;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;

import static com.android.internal.logging.nano.MetricsProto.MetricsEvent
        .FIELD_SETTINGS_PREFERENCE_CHANGE_FLOAT_VALUE;
import static com.android.internal.logging.nano.MetricsProto.MetricsEvent
        .FIELD_SETTINGS_PREFERENCE_CHANGE_LONG_VALUE;
import static com.android.internal.logging.nano.MetricsProto.MetricsEvent
        .FIELD_SETTINGS_PREFERENCE_CHANGE_NAME;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class SharedPreferenceLoggerTest {

    private static final String TEST_TAG = "tag";
    private static final String TEST_KEY = "key";

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;

    private PairMatcher mNamePairMatcher;
    private FakeFeatureFactory mFactory;
    private MetricsFeatureProvider mMetricsFeature;
    private SharedPreferencesLogger mSharedPrefLogger;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        FakeFeatureFactory.setupForTest(mContext);
        mFactory = (FakeFeatureFactory) FakeFeatureFactory.getFactory(mContext);
        mMetricsFeature = mFactory.metricsFeatureProvider;

        mSharedPrefLogger = new SharedPreferencesLogger(mContext, TEST_TAG);
        mNamePairMatcher = new PairMatcher(FIELD_SETTINGS_PREFERENCE_CHANGE_NAME, String.class);
    }

    @Test
    public void putInt_shouldNotLogInitialPut() {
        final SharedPreferences.Editor editor = mSharedPrefLogger.edit();
        editor.putInt(TEST_KEY, 1);
        editor.putInt(TEST_KEY, 1);
        editor.putInt(TEST_KEY, 1);
        editor.putInt(TEST_KEY, 2);
        editor.putInt(TEST_KEY, 2);
        editor.putInt(TEST_KEY, 2);
        editor.putInt(TEST_KEY, 2);

        final PairMatcher longMatcher =
                new PairMatcher(FIELD_SETTINGS_PREFERENCE_CHANGE_LONG_VALUE, Long.class);

        verify(mMetricsFeature, times(6)).action(any(Context.class), anyInt(),
                argThat(mNamePairMatcher), argThat(longMatcher));
    }

    @Test
    public void putBoolean_shouldNotLogInitialPut() {
        final SharedPreferences.Editor editor = mSharedPrefLogger.edit();
        editor.putBoolean(TEST_KEY, true);
        editor.putBoolean(TEST_KEY, true);
        editor.putBoolean(TEST_KEY, false);
        editor.putBoolean(TEST_KEY, false);
        editor.putBoolean(TEST_KEY, false);


        final PairMatcher trueMatcher =
                new PairMatcher(FIELD_SETTINGS_PREFERENCE_CHANGE_LONG_VALUE, true);
        final PairMatcher falseMatcher =
                new PairMatcher(FIELD_SETTINGS_PREFERENCE_CHANGE_LONG_VALUE, false);

        verify(mMetricsFeature).action(any(Context.class), anyInt(),
                argThat(mNamePairMatcher), argThat(trueMatcher));
        verify(mMetricsFeature, times(3)).action(any(Context.class), anyInt(),
                argThat(mNamePairMatcher), argThat(falseMatcher));
    }

    @Test
    public void putLong_shouldNotLogInitialPut() {
        final SharedPreferences.Editor editor = mSharedPrefLogger.edit();
        editor.putLong(TEST_KEY, 1);
        editor.putLong(TEST_KEY, 1);
        editor.putLong(TEST_KEY, 1);
        editor.putLong(TEST_KEY, 1);
        editor.putLong(TEST_KEY, 2);

        final PairMatcher longMatcher =
                new PairMatcher(FIELD_SETTINGS_PREFERENCE_CHANGE_LONG_VALUE, Long.class);

        verify(mMetricsFeature, times(4)).action(any(Context.class), anyInt(),
                argThat(mNamePairMatcher), argThat(longMatcher));
    }

    @Test
    public void putFloat_shouldNotLogInitialPut() {
        final SharedPreferences.Editor editor = mSharedPrefLogger.edit();
        editor.putFloat(TEST_KEY, 1);
        editor.putFloat(TEST_KEY, 1);
        editor.putFloat(TEST_KEY, 1);
        editor.putFloat(TEST_KEY, 1);
        editor.putFloat(TEST_KEY, 2);

        final PairMatcher floatMatcher =
                new PairMatcher(FIELD_SETTINGS_PREFERENCE_CHANGE_FLOAT_VALUE, Float.class);

        verify(mMetricsFeature, times(4)).action(any(Context.class), anyInt(),
                argThat(mNamePairMatcher), argThat(floatMatcher));
    }

    private static class PairMatcher extends ArgumentMatcher<Pair<Integer, Object>> {

        private final int mExpectedTag;
        private final Class mExpectedClass;
        private final Long mExpectedBoolean;


        public PairMatcher(int tag, Class clazz) {
            mExpectedTag = tag;
            mExpectedClass = clazz;
            mExpectedBoolean = null;
        }

        public PairMatcher(int tag, boolean bool) {
            mExpectedTag = tag;
            mExpectedClass = Long.class;
            mExpectedBoolean = bool ? 1L : 0L;
        }

        @Override
        public boolean matches(Object arg) {
            final Pair<Integer, Object> pair = (Pair) arg;
            boolean booleanMatch = mExpectedBoolean == null
                    || mExpectedBoolean == pair.second;
            return pair.first == mExpectedTag
                    && Platform.isInstanceOfType(pair.second, mExpectedClass)
                    && booleanMatch;
        }
    }
}
