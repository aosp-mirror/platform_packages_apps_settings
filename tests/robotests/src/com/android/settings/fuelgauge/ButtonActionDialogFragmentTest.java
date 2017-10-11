/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.android.settings.fuelgauge;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.robolectric.Shadows.shadowOf;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;

import com.android.settings.R;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.testutils.shadow.ShadowEventLogWriter;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowDialog;
import org.robolectric.util.FragmentTestUtil;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION, shadows = {
        ShadowEventLogWriter.class
})
public class ButtonActionDialogFragmentTest {
    private static final int FORCE_STOP_ID = ButtonActionDialogFragment.DialogType.FORCE_STOP;
    private static final int DISABLE_ID = ButtonActionDialogFragment.DialogType.DISABLE;
    private static final int SPECIAL_DISABLE_ID =
            ButtonActionDialogFragment.DialogType.SPECIAL_DISABLE;
    @Mock
    private TestFragment mTargetFragment;
    private ButtonActionDialogFragment mFragment;
    private Context mShadowContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mShadowContext = RuntimeEnvironment.application;

        mFragment = spy(ButtonActionDialogFragment.newInstance(FORCE_STOP_ID));
        doReturn(mShadowContext).when(mFragment).getContext();
        mFragment.setTargetFragment(mTargetFragment, 0);
    }

    @Test
    public void testOnClick_handleToTargetFragment() {
        mFragment.onClick(null, 0);

        verify(mTargetFragment).handleDialogClick(anyInt());
    }

    @Test
    public void testOnCreateDialog_forceStopDialog() {
        ButtonActionDialogFragment fragment = ButtonActionDialogFragment.newInstance(FORCE_STOP_ID);

        FragmentTestUtil.startFragment(fragment);

        final AlertDialog dialog = (AlertDialog) ShadowDialog.getLatestDialog();
        ShadowAlertDialog shadowDialog = shadowOf(dialog);

        assertThat(shadowDialog.getMessage()).isEqualTo(
                mShadowContext.getString(R.string.force_stop_dlg_text));
        assertThat(shadowDialog.getTitle()).isEqualTo(
                mShadowContext.getString(R.string.force_stop_dlg_title));
        assertThat(dialog.getButton(DialogInterface.BUTTON_POSITIVE).getText()).isEqualTo(
                mShadowContext.getString(R.string.dlg_ok));
        assertThat(dialog.getButton(DialogInterface.BUTTON_NEGATIVE).getText()).isEqualTo(
                mShadowContext.getString(R.string.dlg_cancel));
    }

    @Test
    public void testOnCreateDialog_disableDialog() {
        ButtonActionDialogFragment fragment = ButtonActionDialogFragment.newInstance(DISABLE_ID);

        FragmentTestUtil.startFragment(fragment);

        final AlertDialog dialog = (AlertDialog) ShadowDialog.getLatestDialog();
        ShadowAlertDialog shadowDialog = shadowOf(dialog);

        assertThat(shadowDialog.getMessage()).isEqualTo(
                mShadowContext.getString(R.string.app_disable_dlg_text));
        assertThat(dialog.getButton(DialogInterface.BUTTON_POSITIVE).getText()).isEqualTo(
                mShadowContext.getString(R.string.app_disable_dlg_positive));
        assertThat(dialog.getButton(DialogInterface.BUTTON_NEGATIVE).getText()).isEqualTo(
                mShadowContext.getString(R.string.dlg_cancel));
    }

    @Test
    public void testOnCreateDialog_specialDisableDialog() {
        ButtonActionDialogFragment fragment = ButtonActionDialogFragment.newInstance(
                SPECIAL_DISABLE_ID);

        FragmentTestUtil.startFragment(fragment);

        final AlertDialog dialog = (AlertDialog) ShadowDialog.getLatestDialog();
        ShadowAlertDialog shadowDialog = shadowOf(dialog);

        assertThat(shadowDialog.getMessage()).isEqualTo(
                mShadowContext.getString(R.string.app_disable_dlg_text));
        assertThat(dialog.getButton(DialogInterface.BUTTON_POSITIVE).getText()).isEqualTo(
                mShadowContext.getString(R.string.app_disable_dlg_positive));
        assertThat(dialog.getButton(DialogInterface.BUTTON_NEGATIVE).getText()).isEqualTo(
                mShadowContext.getString(R.string.dlg_cancel));
    }

    /**
     * Test fragment that used as the target fragment, it must implement the
     * {@link com.android.settings.fuelgauge.ButtonActionDialogFragment.AppButtonsDialogListener}
     */
    public static class TestFragment extends Fragment implements
            ButtonActionDialogFragment.AppButtonsDialogListener {

        @Override
        public void handleDialogClick(int type) {
            // do nothing
        }
    }
}
