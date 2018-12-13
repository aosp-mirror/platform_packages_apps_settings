/*
 * Copyright (C) 2017 Google Inc.
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

package com.android.settings.password;

import static com.google.common.truth.Truth.assertThat;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;

import com.android.settings.R;
import com.android.settings.testutils.shadow.ShadowAlertDialogCompat;
import com.android.settings.testutils.shadow.ShadowUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowUtils.class, ShadowAlertDialogCompat.class})
public class SetupSkipDialogTest {

    private FragmentActivity mActivity;

    @Before
    public void setUp() {
        mActivity = Robolectric.setupActivity(FragmentActivity.class);
    }

    @Test
    public void frpMessages_areShownCorrectly_whenNotSupported() {
        SetupSkipDialog setupSkipDialog = SetupSkipDialog.newInstance(false);
        setupSkipDialog.show(mActivity.getSupportFragmentManager());

        AlertDialog alertDialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        assertThat(alertDialog).isNotNull();
        ShadowAlertDialogCompat shadowAlertDialog = ShadowAlertDialogCompat.shadowOf(alertDialog);
        assertThat(mActivity.getString(R.string.lock_screen_intro_skip_title)).isEqualTo(
                shadowAlertDialog.getTitle());
        assertThat(mActivity.getString(R.string.lock_screen_intro_skip_dialog_text)).isEqualTo(
                shadowAlertDialog.getMessage());
    }

    @Test
    public void frpMessages_areShownCorrectly_whenSupported() {
        SetupSkipDialog setupSkipDialog = SetupSkipDialog.newInstance(true);
        setupSkipDialog.show(mActivity.getSupportFragmentManager());

        AlertDialog alertDialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        assertThat(alertDialog).isNotNull();
        ShadowAlertDialogCompat shadowAlertDialog = ShadowAlertDialogCompat.shadowOf(alertDialog);
        assertThat(mActivity.getString(R.string.lock_screen_intro_skip_title)).isEqualTo(
                shadowAlertDialog.getTitle());
        assertThat(mActivity.getString(R.string.lock_screen_intro_skip_dialog_text_frp)).isEqualTo(
                shadowAlertDialog.getMessage());
    }
}
