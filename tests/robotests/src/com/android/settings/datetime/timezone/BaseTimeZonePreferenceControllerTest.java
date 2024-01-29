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

package com.android.settings.datetime.timezone;

import static com.google.common.truth.Truth.assertThat;

import android.app.Activity;
import android.content.Context;

import androidx.preference.Preference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class BaseTimeZonePreferenceControllerTest {

    private Activity mActivity;

    @Before
    public void setUp() {
        mActivity = Robolectric.setupActivity(Activity.class);
    }

    @Test
    public void handlePreferenceTreeClick_correctKey_triggerOnClickListener() {
        String prefKey = "key1";
        TestClickListener clickListener = new TestClickListener();
        TestPreference preference = new TestPreference(mActivity, prefKey);
        TestPreferenceController controller = new TestPreferenceController(mActivity, prefKey);
        controller.setOnClickListener(clickListener);

        controller.handlePreferenceTreeClick(preference);
        assertThat(clickListener.isClicked()).isTrue();
    }

    @Test
    public void handlePreferenceTreeClick_wrongKey_triggerOnClickListener() {
        String prefKey = "key1";
        TestClickListener clickListener = new TestClickListener();
        TestPreference preference = new TestPreference(mActivity, "wrong_key");
        TestPreferenceController controller = new TestPreferenceController(mActivity, prefKey);
        controller.setOnClickListener(clickListener);

        controller.handlePreferenceTreeClick(preference);
        assertThat(clickListener.isClicked()).isFalse();
    }

    private static class TestPreferenceController extends BaseTimeZonePreferenceController {

        private final Preference mTestPreference;

        private TestPreferenceController(Context context, String preferenceKey) {
            super(context, preferenceKey);
            mTestPreference = new Preference(context);
            mTestPreference.setKey(preferenceKey);
        }
    }

    private static class TestPreference extends Preference {
        private TestPreference(Context context, String preferenceKey) {
            super(context);
            setKey(preferenceKey);
        }
    }

    private static class TestClickListener implements OnPreferenceClickListener {

        private boolean isClicked = false;

        @Override
        public void onClick() {
            isClicked = true;
        }

        private boolean isClicked() {
            return isClicked;
        }
    }
}
