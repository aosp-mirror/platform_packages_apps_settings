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

package com.android.settings.language;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;

import androidx.preference.Preference;

import com.android.settings.inputmethod.UserDictionaryList;
import com.android.settings.inputmethod.UserDictionarySettings;
import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.TreeSet;

@RunWith(RobolectricTestRunner.class)
public class UserDictionaryPreferenceControllerTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    private Preference mPreference;
    private TestController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        FakeFeatureFactory.setupForTest();
        mController = new TestController(mContext);
        mPreference = new Preference(RuntimeEnvironment.application);
    }

    @Test
    public void testIsAvailable_shouldReturnTrue() {
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void updateState_noLocale_setUserDictionarySettingsAsFragment() {
        mController.updateState(mPreference);

        assertThat(mPreference.getFragment())
                .isEqualTo(UserDictionarySettings.class.getCanonicalName());
    }

    @Test
    public void updateState_singleLocale_setUserDictionarySettingsAsFragment_setLocaleInExtra() {
        mController.mLocales.add("en");

        mController.updateState(mPreference);

        final String fragmentName = UserDictionarySettings.class.getCanonicalName();
        assertThat(mPreference.getFragment()).isEqualTo(fragmentName);
        assertThat(mPreference.getExtras().getString("locale")).isEqualTo("en");
    }

    @Test
    public void updateState_multiLocale_setUserDictionaryListAsFragment() {
        mController.mLocales.add("en");
        mController.mLocales.add("de");
        mController.updateState(mPreference);

        assertThat(mPreference.getFragment())
                .isEqualTo(UserDictionaryList.class.getCanonicalName());
    }

    /**
     * Fake Controller that overrides getDictionaryLocales to make testing the rest of stuff easier.
     */
    private class TestController extends UserDictionaryPreferenceController {

        private TreeSet<String> mLocales = new TreeSet<>();

        @Override
        protected TreeSet<String> getDictionaryLocales() {
            return mLocales;
        }

        private TestController(Context context) {
            super(context, "test_key");
        }
    }
}
