/*
 * Copyright (C) 2022 The Android Open Source Project
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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.view.View;

import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;

/**
 * Tests for {@link TextReadingResetController}.
 */
@RunWith(RobolectricTestRunner.class)
public class TextReadingResetControllerTest {
    private static final String RESET_KEY = "reset";
    private final Context mContext = ApplicationProvider.getApplicationContext();
    private TextReadingResetController mResetController;
    private TextReadingResetPreference mResetPreference;

    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Mock
    private View.OnClickListener mOnResetButtonClickListener;

    @Mock
    private PreferenceScreen mPreferenceScreen;

    @Before
    public void setUp() {
        mResetController = new TextReadingResetController(mContext, RESET_KEY,
                mOnResetButtonClickListener);
        mResetPreference = spy(new TextReadingResetPreference(mContext, /* attrs= */ null));

        when(mPreferenceScreen.findPreference(RESET_KEY)).thenReturn(mResetPreference);
    }

    @Test
    public void displayResetPreference_verifyResetClickListener() {
        mResetController.displayPreference(mPreferenceScreen);

        verify(mResetPreference).setOnResetClickListener(any(View.OnClickListener.class));
    }

    @Test
    public void setVisibleAsFalse_preferenceInvisible() {
        mResetController.setVisible(false);
        mResetController.displayPreference(mPreferenceScreen);

        assertThat(mResetPreference.isVisible()).isFalse();
    }

    @Test
    public void setVisibleAsTrue_preferenceVisible() {
        mResetController.setVisible(true);
        mResetController.displayPreference(mPreferenceScreen);

        assertThat(mResetPreference.isVisible()).isTrue();
    }
}
