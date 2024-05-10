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
package com.android.settings.applications.appinfo;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;

import com.android.settings.R;
import com.android.settings.testutils.shadow.ShadowAlertDialogCompat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
@Config(shadows = ShadowAlertDialogCompat.class)
public class InstantAppButtonDialogFragmentTest {

    private static final String TEST_PACKAGE = "testPackage";

    private InstantAppButtonDialogFragment mFragment;
    private Context mContext;

    @Before
    public void setUp() {
        final FragmentActivity activity = Robolectric.setupActivity(FragmentActivity.class);
        mContext = spy(RuntimeEnvironment.application);
        mFragment = InstantAppButtonDialogFragment.newInstance(TEST_PACKAGE);
        mFragment.show(activity.getSupportFragmentManager(), "InstantAppButtonDialogFragment");
    }

    @Test
    public void onClick_shouldDeleteApp() {
        final InstantAppButtonDialogFragment spyFragment = spy(mFragment);
        doReturn(mContext).when(spyFragment).getContext();
        final PackageManager packageManager = mock(PackageManager.class);
        when(mContext.getPackageManager()).thenReturn(packageManager);

        spyFragment.onClick(null /* dialog */, 0  /* which */);

        verify(packageManager)
            .deletePackageAsUser(eq(TEST_PACKAGE), any(), anyInt(), anyInt());
    }

    @Test
    public void onCreateDialog_clearAppDialog_shouldShowClearAppDataConfirmation() {
        final AlertDialog dialog = ShadowAlertDialogCompat.getLatestAlertDialog();

        assertThat(dialog).isNotNull();

        final ShadowAlertDialogCompat shadowDialog = ShadowAlertDialogCompat.shadowOf(dialog);

        assertThat(shadowDialog.getMessage()).isEqualTo(
                mContext.getString(R.string.clear_instant_app_confirmation));
        assertThat(shadowDialog.getTitle()).isEqualTo(
                mContext.getString(R.string.clear_instant_app_data));
        assertThat(dialog.getButton(DialogInterface.BUTTON_POSITIVE).getText()).isEqualTo(
                mContext.getString(R.string.clear_instant_app_data));
        assertThat(dialog.getButton(DialogInterface.BUTTON_NEGATIVE).getText()).isEqualTo(
                mContext.getString(R.string.cancel));
    }
}
