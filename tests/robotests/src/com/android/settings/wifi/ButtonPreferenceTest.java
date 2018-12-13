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

package com.android.settings.wifi;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.view.View;
import android.widget.ImageButton;

import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class ButtonPreferenceTest {

    private Context mContext;
    private View mRootView;
    private ButtonPreference mPref;
    private PreferenceViewHolder mHolder;
    private boolean mClicked;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = RuntimeEnvironment.application;
        mPref = new ButtonPreference(mContext);
        mRootView = View.inflate(mContext, R.layout.wifi_button_preference_widget, /* parent */
                null);
        mHolder = PreferenceViewHolder.createInstanceForTests(mRootView);
    }

    @Test
    public void initButton_noIcon_shouldInvisible() {
        mPref.initButton(mHolder);
        assertThat(mRootView.findViewById(R.id.button_icon).getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void initButton_withIcon_shouldVisible() {
        mPref.setButtonIcon(R.drawable.ic_qrcode_24dp);
        mPref.initButton(mHolder);
        assertThat(mRootView.findViewById(R.id.button_icon).getVisibility()).isEqualTo(
                View.VISIBLE);
    }

    @Test
    public void initButton_whenClick_shouldCallback() {
        mClicked = false;
        mPref.setButtonIcon(R.drawable.ic_qrcode_24dp);
        mPref.setButtonOnClickListener((View v) -> {
            mClicked = true;
        });
        mPref.initButton(mHolder);
        ImageButton button = (ImageButton) mRootView.findViewById(R.id.button_icon);
        button.performClick();
        assertThat(mClicked).isTrue();
    }
}
