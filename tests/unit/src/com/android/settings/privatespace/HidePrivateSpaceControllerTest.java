/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.privatespace;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;

import android.content.ContentResolver;
import android.content.Context;
import android.os.Flags;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;
import android.provider.Settings;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@RequiresFlagsEnabled(Flags.FLAG_ALLOW_PRIVATE_PROFILE)
public class HidePrivateSpaceControllerTest {
    @Rule
    public final CheckFlagsRule mCheckFlagsRule =
            DeviceFlagsValueProvider.createCheckFlagsRule();

    private static final String KEY = "private_space_hidden";
    private static final String DETAIL_PAGE_KEY = "private_space_hidden_details";
    private Context mContext;
    private HidePrivateSpaceSummaryController mHidePrivateSpaceSummaryController;
    private HidePrivateSpaceController mHidePrivateSpaceController;
    private ContentResolver mContentResolver;
    private int mOriginalHiddenValue;

    /** Required setup before a test. */
    @Before
    public void setUp() {
        mContext = spy(ApplicationProvider.getApplicationContext());
        mContentResolver = mContext.getContentResolver();
        mHidePrivateSpaceSummaryController = new HidePrivateSpaceSummaryController(mContext, KEY);
        mHidePrivateSpaceController =
                new HidePrivateSpaceController(mContext, DETAIL_PAGE_KEY);
        mOriginalHiddenValue = Settings.Secure.getInt(mContentResolver,
                Settings.Secure.HIDE_PRIVATESPACE_ENTRY_POINT, 0);
    }

    @After
    public void tearDown() {
        Settings.Secure.putInt(mContentResolver, Settings.Secure.HIDE_PRIVATESPACE_ENTRY_POINT,
                mOriginalHiddenValue);
    }

    /** Tests that the controller is always available. */
    @Test
    public void getAvailabilityStatus_returnsAvailable() {
        assertThat(mHidePrivateSpaceController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    /** Tests that hide preference summary displays On when hide toggle is enabled.*/
    @Test
    public void setChecked_enable_shouldDisplayOn() {
        Settings.Secure.putInt(mContentResolver, Settings.Secure.HIDE_PRIVATESPACE_ENTRY_POINT,
                0);
        assertThat(mHidePrivateSpaceController.isChecked()).isFalse();

        mHidePrivateSpaceController.setChecked(true);

        assertThat(mHidePrivateSpaceSummaryController.getSummary().toString())
                .isEqualTo("On");
        assertThat(mHidePrivateSpaceController.isChecked()).isTrue();
    }

    /** Tests that hide preference summary displays Off when toggle is disabled.*/
    @Test
    public void setChecked_disable_shouldDisplayOff() {
        Settings.Secure.putInt(mContentResolver, Settings.Secure.HIDE_PRIVATESPACE_ENTRY_POINT,
                1);

        assertThat(mHidePrivateSpaceController.isChecked()).isTrue();

        mHidePrivateSpaceController.setChecked(false);

        assertThat(mHidePrivateSpaceSummaryController.getSummary().toString())
                .isEqualTo("Off");
        assertThat(mHidePrivateSpaceController.isChecked()).isFalse();
    }
}
