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
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;

import com.android.settings.R;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowDialog;
import org.robolectric.util.FragmentTestUtil;

@RunWith(SettingsRobolectricTestRunner.class)
public class InstantAppButtonDialogFragmentTest {

    private static final String TEST_PACKAGE = "testPackage";

    private InstantAppButtonDialogFragment mFragment;
    private Context mContext;

    @Before
    public void setUp() {
        mContext = spy(RuntimeEnvironment.application);
        mFragment = spy(InstantAppButtonDialogFragment.newInstance(TEST_PACKAGE));
        doReturn(mContext).when(mFragment).getContext();
    }

    @Test
    public void onClick_shouldDeleteApp() {
        final PackageManager packageManager = mock(PackageManager.class);
        when(mContext.getPackageManager()).thenReturn(packageManager);
        FragmentTestUtil.startFragment(mFragment);

        mFragment.onClick(null /* dialog */, 0  /* which */);

        verify(packageManager)
            .deletePackageAsUser(eq(TEST_PACKAGE), any(), anyInt(), anyInt());
    }

    @Test
    public void onCreateDialog_clearAppDialog_shouldShowClearAppDataConfirmation() {
        FragmentTestUtil.startFragment(mFragment);

        final AlertDialog dialog = (AlertDialog) ShadowDialog.getLatestDialog();
        assertThat(dialog).isNotNull();
        final ShadowAlertDialog shadowDialog = shadowOf(dialog);

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
