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

package com.android.settings.privatespace;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.fragment.app.FragmentActivity;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowApplication;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowAlertDialog.class})
public class HidePrivateSpaceControllerTest {
    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    private static final String KEY = "private_space_hidden";
    private static final String DETAIL_PAGE_KEY = "private_space_hidden_details";
    private HidePrivateSpaceController mHidePrivateSpaceController;
    private HidePrivateSpaceSummaryController mHidePrivateSpaceSummaryController;
    private FragmentActivity mActivity;

    @Before
    public void setUp() {
        Context context = RuntimeEnvironment.application;
        mHidePrivateSpaceController = new HidePrivateSpaceController(context, DETAIL_PAGE_KEY);
        mHidePrivateSpaceSummaryController = new HidePrivateSpaceSummaryController(context, KEY);
        mActivity = Robolectric.setupActivity(FragmentActivity.class);
    }

    /** Tests that when flags enabled the controller is available. */
    @Test
    public void getAvailabilityStatus_flagEnabled_returnsAvailable() {
        mSetFlagsRule.enableFlags(android.multiuser.Flags.FLAG_ENABLE_PRIVATE_SPACE_FEATURES);

        assertThat(mHidePrivateSpaceController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    /** Tests that when flags disabled the controller is unsupported. */
    @Test
    public void getAvailabilityStatus_flagDisabled_returnsUnsupported() {
        mSetFlagsRule.disableFlags(android.multiuser.Flags.FLAG_ENABLE_PRIVATE_SPACE_FEATURES);

        assertThat(mHidePrivateSpaceController.getAvailabilityStatus())
                .isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    /** Tests that when hide toggle is enabled dialog is displayed. */
    @Test
    public void setChecked_enabled_showsDialog() {
        mSetFlagsRule.enableFlags(android.multiuser.Flags.FLAG_ENABLE_PRIVATE_SPACE_FEATURES);
        mHidePrivateSpaceController.setChecked(true);

        ShadowAlertDialog shadowAlertDialog = getShadowAlertDialog();
        assertThat(shadowAlertDialog).isNotNull();
        assertThat(shadowAlertDialog.getTitle().toString())
                .isEqualTo(mActivity.getString(R.string.private_space_hide_dialog_title));
        assertThat(shadowAlertDialog.getMessage().toString())
                .isEqualTo(mActivity.getString(R.string.private_space_hide_dialog_message));
    }

    /** Tests that when hide toggle is disabled dialog is not displayed. */
    @Test
    public void setChecked_disabled_NoDialogShown() {
        mSetFlagsRule.enableFlags(android.multiuser.Flags.FLAG_ENABLE_PRIVATE_SPACE_FEATURES);
        mHidePrivateSpaceController.setChecked(false);

        ShadowAlertDialog shadowAlertDialog = getShadowAlertDialog();
        assertThat(shadowAlertDialog).isNull();
    }

    /** Tests that when hide toggle is enabled then isChecked returns true. */
    @Test
    public void setChecked_enabled_isCheckedIsTrue() {
        mSetFlagsRule.enableFlags(android.multiuser.Flags.FLAG_ENABLE_PRIVATE_SPACE_FEATURES);
        mHidePrivateSpaceController.setChecked(true);
        assertThat(mHidePrivateSpaceController.isChecked()).isTrue();
    }

    /** Tests that when hide toggle is disabled then isChecked returns false. */
    @Test
    public void setChecked_disabled_isCheckedIsFalse() {
        mSetFlagsRule.enableFlags(android.multiuser.Flags.FLAG_ENABLE_PRIVATE_SPACE_FEATURES);
        mHidePrivateSpaceController.setChecked(false);
        assertThat(mHidePrivateSpaceController.isChecked()).isFalse();
    }

    /** Tests that hide preference summary displays On when toggle is enabled. */
    @Test
    public void setChecked_enable_summaryShouldDisplayOn() {
        mSetFlagsRule.enableFlags(android.multiuser.Flags.FLAG_ENABLE_PRIVATE_SPACE_FEATURES);
        mHidePrivateSpaceController.setChecked(true);

        assertThat(mHidePrivateSpaceSummaryController.getSummary().toString()).isEqualTo("On");
    }

    /** Tests that hide preference summary displays Off when toggle is disabled. */
    @Test
    public void setChecked_disable_summaryShouldDisplayOff() {
        mSetFlagsRule.enableFlags(android.multiuser.Flags.FLAG_ENABLE_PRIVATE_SPACE_FEATURES);
        mHidePrivateSpaceController.setChecked(false);

        assertThat(mHidePrivateSpaceSummaryController.getSummary().toString()).isEqualTo("Off");
    }

    private ShadowAlertDialog getShadowAlertDialog() {
        ShadowApplication shadowApplication =
                Shadow.extract(ApplicationProvider.getApplicationContext());
        ShadowAlertDialog shadowAlertDialog = shadowApplication.getLatestAlertDialog();
        return shadowAlertDialog;
    }
}
