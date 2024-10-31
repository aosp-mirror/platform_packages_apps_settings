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

package com.android.settings.bluetooth;

import static com.google.common.truth.Truth.assertThat;

import static org.robolectric.shadows.ShadowLooper.shadowMainLooper;

import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.android.settings.R;
import com.android.settings.testutils.shadow.ShadowAlertDialogCompat;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.androidx.fragment.FragmentController;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowAlertDialogCompat.class})
public class ProgressDialogFragmentTest {
    @Rule public final MockitoRule mocks = MockitoJUnit.rule();

    private static final String TEST_MESSAGE1 = "message1";
    private static final String TEST_MESSAGE2 = "message2";

    private Fragment mParent;

    @Before
    public void setUp() {
        ShadowAlertDialogCompat.reset();
        mParent = new Fragment();
        FragmentController.setupFragment(
                mParent, FragmentActivity.class, /* containerViewId= */ 0, /* bundle= */ null);
    }

    @After
    public void tearDown() {
        ShadowAlertDialogCompat.reset();
    }

    @Test
    public void getMetricsCategory_correctValue() {
        ProgressDialogFragment fragment = ProgressDialogFragment.newInstance(mParent);
        // TODO: update real metric
        assertThat(fragment.getMetricsCategory()).isEqualTo(0);
    }

    @Test
    public void onCreateDialog_unattachedFragment_nullDialogFragment() {
        ProgressDialogFragment fragment = ProgressDialogFragment.newInstance(new Fragment());
        assertThat(fragment).isNull();
    }

    @Test
    public void onCreateDialog_showDialog() {
        ProgressDialogFragment fragment = ProgressDialogFragment.newInstance(mParent);
        fragment.show(TEST_MESSAGE1);
        shadowMainLooper().idle();
        AlertDialog dialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        assertThat(dialog).isNotNull();
        assertThat(dialog.isShowing()).isTrue();
        TextView view = dialog.findViewById(R.id.message);
        assertThat(view).isNotNull();
        assertThat(view.getText().toString()).isEqualTo(TEST_MESSAGE1);
    }

    @Test
    public void dismissDialog_succeed() {
        ProgressDialogFragment fragment = ProgressDialogFragment.newInstance(mParent);
        fragment.show(TEST_MESSAGE1);
        shadowMainLooper().idle();
        AlertDialog dialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        assertThat(dialog).isNotNull();
        assertThat(dialog.isShowing()).isTrue();

        fragment.dismissAllowingStateLoss();
        shadowMainLooper().idle();
        assertThat(dialog.isShowing()).isFalse();
    }

    @Test
    public void showDialog_sameMessage_keepExistingDialog() {
        ProgressDialogFragment fragment = ProgressDialogFragment.newInstance(mParent);
        fragment.show(TEST_MESSAGE1);
        shadowMainLooper().idle();
        AlertDialog dialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        assertThat(dialog).isNotNull();
        assertThat(dialog.isShowing()).isTrue();

        fragment.show(TEST_MESSAGE1);
        shadowMainLooper().idle();
        assertThat(dialog.isShowing()).isTrue();
        TextView view = dialog.findViewById(R.id.message);
        assertThat(view).isNotNull();
        assertThat(view.getText().toString()).isEqualTo(TEST_MESSAGE1);
    }

    @Test
    public void showDialog_newMessage_keepAndUpdateDialog() {
        ProgressDialogFragment fragment = ProgressDialogFragment.newInstance(mParent);
        fragment.show(TEST_MESSAGE1);
        shadowMainLooper().idle();
        AlertDialog dialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        assertThat(dialog).isNotNull();
        assertThat(dialog.isShowing()).isTrue();
        TextView view = dialog.findViewById(R.id.message);
        assertThat(view).isNotNull();
        assertThat(view.getText().toString()).isEqualTo(TEST_MESSAGE1);

        fragment.show(TEST_MESSAGE2);
        shadowMainLooper().idle();
        assertThat(dialog.isShowing()).isTrue();
        assertThat(view.getText().toString()).isEqualTo(TEST_MESSAGE2);
    }
}
