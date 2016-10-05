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
 * limitations under the License
 */
package com.android.settings.core;

import android.content.Context;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.TestConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class PreferenceControllerTest {
    private static final String KEY_PREF = "test_pref";

    @Mock
    private Context mContext;
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private Preference mPreference;

    private TestPrefController mTestPrefController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mTestPrefController = new TestPrefController(mContext);
    }

    @Test
    public void removeExistingPref_shouldBeRemoved() {
        when(mScreen.findPreference(KEY_PREF)).thenReturn(mPreference);

        mTestPrefController.removePreference(mScreen, KEY_PREF);

        verify(mScreen).removePreference(mPreference);
    }

    @Test
    public void removeNonExistingPref_shouldNotRemoveAnything() {
        mTestPrefController.removePreference(mScreen, KEY_PREF);

        verify(mScreen, never()).removePreference(any(Preference.class));
    }

    private class TestPrefController extends PreferenceController {

        public TestPrefController(Context context) {
            super(context);
        }

        @Override
        public void displayPreference(PreferenceScreen screen) {

        }

        @Override
        public boolean handlePreferenceTreeClick(Preference preference) {
            return false;
        }

        @Override
        public void updateNonIndexableKeys(List<String> keys) {

        }
    }

}
