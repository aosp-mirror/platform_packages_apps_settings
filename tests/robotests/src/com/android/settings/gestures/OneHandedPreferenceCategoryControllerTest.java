/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.gestures;

import static junit.framework.TestCase.assertTrue;

import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.UserHandle;
import android.text.TextUtils;

import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class OneHandedPreferenceCategoryControllerTest {

    private static final String KEY = "gesture_one_handed_mode_swipe_down";

    private Context mContext;
    private OneHandedSettingsUtils mUtils;
    private OneHandedPreferenceCategoryController mController;
    @Mock
    private PreferenceCategory mPreference;
    @Mock
    private PreferenceScreen mScreen;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mUtils = new OneHandedSettingsUtils(mContext);
        mController = new OneHandedPreferenceCategoryController(mContext, KEY);
        OneHandedSettingsUtils.setUserId(UserHandle.myUserId());
        mPreference = new PreferenceCategory(mContext);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
    }

    @Ignore("b/313541907")
    @Test
    public void getTitle_set3ButtonMode_shouldReturnSetShortcutTo() {
        mUtils.setNavigationBarMode(mContext, "0" /* 3 button */);
        mController.displayPreference(mScreen);

        assertTrue(TextUtils.equals(mPreference.getTitle(), mContext.getText(
                R.string.one_handed_mode_use_shortcut_category)));
    }

    @Test
    public void getTitle_setGestureMode_shouldReturnSwipeDownTo() {
        mUtils.setNavigationBarMode(mContext, "2" /* fully gestural */);
        mController.displayPreference(mScreen);

        assertTrue(TextUtils.equals(mPreference.getTitle(), mContext.getText(
                R.string.one_handed_mode_swipe_down_category)));
    }
}
