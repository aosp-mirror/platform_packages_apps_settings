/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.localepicker;

import static com.android.settings.localepicker.LocaleDialogFragment.ARG_DIALOG_TYPE;
import static com.android.settings.localepicker.LocaleDialogFragment.ARG_TARGET_LOCALE;
import static com.android.settings.localepicker.LocaleDialogFragment.DIALOG_CONFIRM_SYSTEM_DEFAULT;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import android.os.Bundle;
import android.window.OnBackInvokedDispatcher;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.android.internal.app.LocaleStore;
import com.android.settings.testutils.shadow.ShadowAlertDialogCompat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

import java.util.Locale;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowAlertDialogCompat.class})
@LooperMode(LooperMode.Mode.LEGACY)
public class LocaleDialogFragmentTest {

    @Mock
    private OnBackInvokedDispatcher mOnBackInvokedDispatcher;

    private FragmentActivity mActivity;
    private LocaleDialogFragment mDialogFragment;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        mActivity = Robolectric.setupActivity(FragmentActivity.class);
        mDialogFragment = LocaleDialogFragment.newInstance();
        LocaleStore.LocaleInfo localeInfo = LocaleStore.getLocaleInfo(Locale.ENGLISH);
        Bundle args = new Bundle();
        args.putInt(ARG_DIALOG_TYPE, DIALOG_CONFIRM_SYSTEM_DEFAULT);
        args.putSerializable(ARG_TARGET_LOCALE, localeInfo);
        mDialogFragment.setArguments(args);
        FragmentManager fragmentManager = mActivity.getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.add(mDialogFragment, null);
        fragmentTransaction.commit();
    }

    @Test
    public void onCreateDialog_onBackInvokedCallbackIsRegistered() {
        mDialogFragment.setBackDispatcher(mOnBackInvokedDispatcher);
        mDialogFragment.onCreateDialog(null);

        verify(mOnBackInvokedDispatcher).registerOnBackInvokedCallback(
                eq(OnBackInvokedDispatcher.PRIORITY_DEFAULT), any());
    }

    @Test
    public void onBackInvoked_dialogIsStillDisplaying() {
        mDialogFragment.setBackDispatcher(mOnBackInvokedDispatcher);
        AlertDialog alertDialog = (AlertDialog) mDialogFragment.onCreateDialog(null);
        alertDialog.show();
        assertThat(alertDialog).isNotNull();
        assertThat(alertDialog.isShowing()).isTrue();

        mOnBackInvokedDispatcher.registerOnBackInvokedCallback(
                eq(OnBackInvokedDispatcher.PRIORITY_DEFAULT), any());

        mDialogFragment.getBackInvokedCallback().onBackInvoked();

        assertThat(alertDialog.isShowing()).isTrue();

    }
}
