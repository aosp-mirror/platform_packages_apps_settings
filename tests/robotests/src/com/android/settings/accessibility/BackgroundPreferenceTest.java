/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.accessibility;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;

import androidx.preference.PreferenceViewHolder;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link BackgroundPreference} */
@RunWith(RobolectricTestRunner.class)
public class BackgroundPreferenceTest {

    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    private final Context mContext = ApplicationProvider.getApplicationContext();

    private View mRootView = new View(mContext);
    @Spy
    private PreferenceViewHolder mViewHolder = PreferenceViewHolder.createInstanceForTests(
            mRootView);
    @Spy
    private LinearLayout mLinearLayout = new LinearLayout(mContext);
    private BackgroundPreference mPreference;

    @Before
    public void setUp() {
        mPreference = new BackgroundPreference(mContext);
    }

    @Test
    public void setBackground_success() {
        doReturn(mLinearLayout).when(mViewHolder).findViewById(R.id.background);

        mPreference.setBackground(android.R.drawable.screen_background_dark);
        mPreference.onBindViewHolder(mViewHolder);

        verify(mLinearLayout).setBackground(any());
    }
}
