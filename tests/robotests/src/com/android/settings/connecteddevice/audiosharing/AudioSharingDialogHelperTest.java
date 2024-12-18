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

package com.android.settings.connecteddevice.audiosharing;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class AudioSharingDialogHelperTest {
    private static final String TAG = "test";

    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Mock FragmentManager mFragmentManager;
    @Mock DialogFragment mFragment;
    @Mock AlertDialog mDialog;
    @Mock TextView mTextView;

    @Test
    public void updateMessageStyle_updateStyle() {
        when(mDialog.findViewById(android.R.id.message)).thenReturn(mTextView);
        AudioSharingDialogHelper.updateMessageStyle(mDialog);
        Typeface typeface = Typeface.create(Typeface.DEFAULT_FAMILY, Typeface.NORMAL);
        verify(mTextView).setTypeface(typeface);
        verify(mTextView).setTextDirection(View.TEXT_DIRECTION_LOCALE);
        verify(mTextView).setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        verify(mTextView).setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
    }

    @Test
    public void getDialogIfShowing_notShowing_returnNull() {
        when(mFragmentManager.findFragmentByTag(TAG)).thenReturn(mFragment);
        when(mFragment.getDialog()).thenReturn(mDialog);
        when(mDialog.isShowing()).thenReturn(false);
        assertThat(AudioSharingDialogHelper.getDialogIfShowing(mFragmentManager, TAG)).isNull();
    }

    @Test
    public void getDialogIfShowing_showing_returnDialog() {
        when(mFragmentManager.findFragmentByTag(TAG)).thenReturn(mFragment);
        when(mFragment.getDialog()).thenReturn(mDialog);
        when(mDialog.isShowing()).thenReturn(true);
        assertThat(AudioSharingDialogHelper.getDialogIfShowing(mFragmentManager, TAG))
                .isEqualTo(mDialog);
    }
}
