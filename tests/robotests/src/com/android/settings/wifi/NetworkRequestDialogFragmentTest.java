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

package com.android.settings.wifi;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.DialogInterface;
import android.widget.Button;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.shadow.SettingsShadowResourcesImpl;
import com.android.settings.testutils.shadow.ShadowAlertDialogCompat;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;
import com.android.settings.testutils.shadow.ShadowAlertDialogCompat;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(shadows = {SettingsShadowResourcesImpl.class, ShadowAlertDialogCompat.class})
public class NetworkRequestDialogFragmentTest {

  private FragmentActivity mActivity;
  private NetworkRequestDialogFragment networkRequestDialogFragment;

  @Before
  public void setUp() {
    mActivity = Robolectric.setupActivity(FragmentActivity.class);
    networkRequestDialogFragment = spy(NetworkRequestDialogFragment.newInstance(-1, null));
  }

  @Test
  public void display_shouldShowTheDialog() {
    networkRequestDialogFragment.show(mActivity.getSupportFragmentManager(), null);
    AlertDialog alertDialog = ShadowAlertDialogCompat.getLatestAlertDialog();
    assertThat(alertDialog).isNotNull();
    assertThat(alertDialog.isShowing()).isTrue();
  }

  @Test
  public void clickPositiveButton_shouldCloseTheDialog() {
    networkRequestDialogFragment.show(mActivity.getSupportFragmentManager(), null);
    AlertDialog alertDialog = ShadowAlertDialogCompat.getLatestAlertDialog();
    assertThat(alertDialog.isShowing()).isTrue();

    Button positiveButton = alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
    assertThat(positiveButton).isNotNull();

    positiveButton.performClick();
    assertThat(alertDialog.isShowing()).isFalse();
  }
}
