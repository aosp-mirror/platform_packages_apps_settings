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

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;

import com.android.settings.SettingsActivity;
import com.android.settings.bluetooth.BluetoothPairingDetail;
import com.android.settings.testutils.shadow.ShadowAlertDialogCompat;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

/** Tests for {@link HearingAidDialogFragment}. */
@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
@Config(shadows = {
        com.android.settings.testutils.shadow.ShadowFragment.class,
        ShadowAlertDialogCompat.class,
})
public class HearingAidDialogFragmentTest {

    @Rule
    public MockitoRule mocks = MockitoJUnit.rule();

    private FragmentActivity mActivity;
    private HearingAidDialogFragment mFragment;

    @Before
    public void setUpTestFragment() {
        mFragment = spy(HearingAidDialogFragment.newInstance());
        mActivity = Robolectric.setupActivity(FragmentActivity.class);
        when(mFragment.getActivity()).thenReturn(mActivity);
    }

    @Test
    public void onCreateDialog_dialogExist() {
        final Dialog dialog = mFragment.onCreateDialog(Bundle.EMPTY);

        assertThat(dialog).isNotNull();
    }

    @Test
    public void dialogPositiveButtonClick_intentToExpectedClass() {
        final AlertDialog dialog = (AlertDialog) mFragment.onCreateDialog(Bundle.EMPTY);
        dialog.show();

        dialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();

        final Intent intent = shadowOf(mActivity).getNextStartedActivity();
        assertThat(intent.getStringExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT))
                .isEqualTo(BluetoothPairingDetail.class.getName());
    }

    @Test
    public void dialogNegativeButtonClick_dismissDialog() {
        final AlertDialog dialog = (AlertDialog) mFragment.onCreateDialog(Bundle.EMPTY);
        dialog.show();

        dialog.getButton(DialogInterface.BUTTON_NEGATIVE).performClick();

        assertThat(dialog.isShowing()).isFalse();
    }

    @Test
    public void getMetricsCategory_returnsCorrectCategory() {
        assertThat(mFragment.getMetricsCategory()).isEqualTo(
                SettingsEnums.DIALOG_ACCESSIBILITY_HEARINGAID);
    }
}
