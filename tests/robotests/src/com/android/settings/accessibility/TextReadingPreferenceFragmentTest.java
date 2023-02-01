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

import static com.android.settings.accessibility.FontWeightAdjustmentPreferenceController.BOLD_TEXT_ADJUSTMENT;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.DialogInterface;
import android.provider.Settings;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.PreferenceManager;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.accessibility.AccessibilityDialogUtils.DialogEnums;
import com.android.settings.accessibility.TextReadingResetController.ResetStateListener;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowToast;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Tests for {@link TextReadingPreferenceFragment}.
 */
@RunWith(RobolectricTestRunner.class)
public class TextReadingPreferenceFragmentTest {
    private TextReadingPreferenceFragment mFragment;
    private Context mContext = ApplicationProvider.getApplicationContext();
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private PreferenceManager mPreferenceManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext.setTheme(R.style.Theme_AppCompat);

        mFragment = spy(new TextReadingPreferenceFragment());
        when(mFragment.getPreferenceManager()).thenReturn(mPreferenceManager);
        when(mFragment.getPreferenceManager().getContext()).thenReturn(mContext);
        when(mFragment.getContext()).thenReturn(mContext);
        when(mFragment.getActivity()).thenReturn(Robolectric.setupActivity(FragmentActivity.class));

        // Avoid a NPE is happened in ShadowWindowManagerGlobal
        doReturn(mock(DisplaySizeData.class)).when(mFragment).createDisplaySizeData(mContext);
        mFragment.createPreferenceControllers(mContext);
    }

    @Test
    public void onDialogPositiveButtonClicked_boldTextEnabled_needResetSettings() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.FONT_WEIGHT_ADJUSTMENT, BOLD_TEXT_ADJUSTMENT);
        final AlertDialog dialog = (AlertDialog) mFragment.onCreateDialog(
                DialogEnums.DIALOG_RESET_SETTINGS);
        dialog.show();

        dialog.getButton(DialogInterface.BUTTON_POSITIVE).callOnClick();

        assertThat(mFragment.mNeedResetSettings).isTrue();
    }

    @Test
    public void onDialogPositiveButtonClicked_boldTextDisabled_resetAllListeners() {
        final ResetStateListener listener1 = mock(ResetStateListener.class);
        final ResetStateListener listener2 = mock(ResetStateListener.class);
        mFragment.mResetStateListeners = new ArrayList<>(Arrays.asList(listener1, listener2));
        final AlertDialog dialog = (AlertDialog) mFragment.onCreateDialog(
                DialogEnums.DIALOG_RESET_SETTINGS);
        dialog.show();

        dialog.getButton(DialogInterface.BUTTON_POSITIVE).callOnClick();

        verify(listener1).resetState();
        verify(listener2).resetState();
    }

    @Test
    public void onDialogPositiveButtonClicked_boldTextEnabled_showToast() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.FONT_WEIGHT_ADJUSTMENT, BOLD_TEXT_ADJUSTMENT);
        final AlertDialog dialog = (AlertDialog) mFragment.onCreateDialog(
                DialogEnums.DIALOG_RESET_SETTINGS);
        dialog.show();

        dialog.getButton(DialogInterface.BUTTON_POSITIVE).callOnClick();

        assertThat(ShadowToast.getTextOfLatestToast())
                .isEqualTo(mContext.getString(R.string.accessibility_text_reading_reset_message));
    }
}
