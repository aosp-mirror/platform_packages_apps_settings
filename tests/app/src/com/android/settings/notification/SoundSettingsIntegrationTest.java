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
package com.android.settings.notification;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.hasDescendant;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;

import android.content.Context;
import android.media.AudioManager;
import android.support.test.espresso.contrib.RecyclerViewActions;
import android.support.test.filters.SmallTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import com.android.settings.R;
import com.android.settings.Settings;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class SoundSettingsIntegrationTest {

    private AudioManager mAudioManager;
    private final String TRUNCATED_SUMMARY = "Ring volume at";

    @Rule
    public ActivityTestRule<Settings> mActivityRule =
            new ActivityTestRule<>(Settings.class, true);

    @Test
    public void soundPreferenceShowsCorrectSummaryOnSilentMode() {
        mAudioManager = (AudioManager) mActivityRule.getActivity().getApplicationContext()
                .getSystemService(Context.AUDIO_SERVICE);
        mAudioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
        onView(withId(R.id.dashboard_container))
                .perform(RecyclerViewActions.scrollTo(
                        hasDescendant(withText(R.string.sound_settings))));
        onView(withText(R.string.sound_settings_summary_silent)).check(matches(isDisplayed()));
    }

    @Test
    public void soundPreferenceShowsCorrectSummaryOnVibrateMode() {
        mAudioManager = (AudioManager) mActivityRule.getActivity().getApplicationContext()
                .getSystemService(Context.AUDIO_SERVICE);
        mAudioManager.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
        onView(withId(R.id.dashboard_container)).perform(RecyclerViewActions
                .scrollTo(hasDescendant(withText(R.string.sound_settings))));
        onView(withText(R.string.sound_settings_summary_vibrate)).check(matches(isDisplayed()));
    }

    @Test
    public void soundPreferenceShowsCorrectSummaryOnMaxVolume() {
        mAudioManager = (AudioManager) mActivityRule.getActivity().getApplicationContext()
                .getSystemService(Context.AUDIO_SERVICE);
        mAudioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
        mAudioManager.setStreamVolume(AudioManager.STREAM_RING,
                mAudioManager.getStreamMaxVolume(AudioManager.STREAM_RING), 0);
        onView(withId(R.id.dashboard_container))
                .perform(RecyclerViewActions.scrollTo(
                        hasDescendant(withText(R.string.sound_settings))));
        onView(withText(containsString(TRUNCATED_SUMMARY))).check(matches(isDisplayed()));
    }
}