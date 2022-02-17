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

import static com.android.settings.accessibility.TextReadingResetController.ResetStateListener;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.content.Context;
import android.view.View;

import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.widget.LayoutPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests for {@link TextReadingResetController}.
 */
@RunWith(RobolectricTestRunner.class)
public class TextReadingResetControllerTest {
    private static final String TEST_KEY = "test";
    private static final String RESET_KEY = "reset";
    private final Context mContext = ApplicationProvider.getApplicationContext();
    private final View mResetView = new View(mContext);
    private final List<ResetStateListener> mListeners = new ArrayList<>();
    private TextReadingResetController mResetController;
    private TestPreferenceController mPreferenceController;

    @Mock
    private PreferenceScreen mPreferenceScreen;

    @Mock
    private LayoutPreference mLayoutPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mPreferenceController = new TestPreferenceController(mContext, TEST_KEY);
        mListeners.add(mPreferenceController);
        mResetController = new TextReadingResetController(mContext, RESET_KEY, mListeners);
    }

    @Test
    public void setClickListener_success() {
        setupResetButton();

        mResetController.displayPreference(mPreferenceScreen);

        assertThat(mResetView.hasOnClickListeners()).isTrue();
    }

    @Test
    public void triggerResetState_success() {
        setupResetButton();

        mResetController.displayPreference(mPreferenceScreen);
        mResetView.callOnClick();

        assertThat(mPreferenceController.isReset()).isTrue();
    }

    private void setupResetButton() {
        when(mPreferenceScreen.findPreference(RESET_KEY)).thenReturn(mLayoutPreference);
        when(mLayoutPreference.findViewById(R.id.reset_button)).thenReturn(mResetView);
    }

    private static class TestPreferenceController extends BasePreferenceController implements
            ResetStateListener {
        private boolean mIsReset = false;

        TestPreferenceController(Context context, String preferenceKey) {
            super(context, preferenceKey);
        }

        @Override
        public void resetState() {
            mIsReset = true;
        }

        @Override
        public int getAvailabilityStatus() {
            return AVAILABLE;
        }

        boolean isReset() {
            return mIsReset;
        }
    }
}
