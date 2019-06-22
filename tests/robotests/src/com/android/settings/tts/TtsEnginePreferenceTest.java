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

package com.android.settings.tts;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Checkable;

import androidx.preference.PreferenceViewHolder;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class TtsEnginePreferenceTest {

    private static final String KEY = "test_key";

    private TtsEnginePreference mPreference;
    private Context mContext;
    private PreferenceViewHolder mViewHolder;
    private View mRootView;
    private FakeRadioButtonGroupState mState;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;

        final TextToSpeech.EngineInfo info = new TextToSpeech.EngineInfo();
        info.system = true;
        mState = new FakeRadioButtonGroupState();
        mPreference = new TtsEnginePreference(mContext, info, mState);
        mPreference.setKey(KEY);

        // Create preference view holder
        final LayoutInflater inflater = LayoutInflater.from(mContext);
        mRootView = View.inflate(mContext, mPreference.getLayoutResource(), null /* parent */);
        final ViewGroup widgetFrame = mRootView.findViewById(android.R.id.widget_frame);
        inflater.inflate(mPreference.getWidgetLayoutResource(), widgetFrame);
        mViewHolder = PreferenceViewHolder.createInstanceForTests(mRootView);
    }

    @Test
    public void onClick_shouldInvokeOnCheckedChangeListener() {
        mPreference.onBindViewHolder(mViewHolder);

        mPreference.onClick();

        assertThat(mState.getCurrentKey()).isEqualTo(mPreference.getKey());
    }

    public static class FakeRadioButtonGroupState implements
            TtsEnginePreference.RadioButtonGroupState {

        private String mKey;

        @Override
        public Checkable getCurrentChecked() {
            return null;
        }

        @Override
        public String getCurrentKey() {
            return mKey;
        }

        @Override
        public void setCurrentChecked(Checkable current) {
        }

        @Override
        public void setCurrentKey(String key) {
            mKey = key;
        }
    }
}
