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
import static com.android.settings.localepicker.LocaleDialogFragment.ARG_RESULT_RECEIVER;
import static com.android.settings.localepicker.LocaleDialogFragment.ARG_TARGET_LOCALE;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.app.Activity;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.ResultReceiver;

import androidx.test.annotation.UiThreadTest;
import androidx.test.core.app.ApplicationProvider;

import com.android.internal.app.LocaleStore;
import com.android.settings.testutils.ResourcesUtils;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Locale;

@UiThreadTest
public class LocaleDialogFragmentTest {
    @Rule
    public final MockitoRule mockito = MockitoJUnit.rule();

    private Context mContext;
    private LocaleDialogFragment mDialogFragment;

    @Before
    public void setUp() throws Exception {
        mContext = ApplicationProvider.getApplicationContext();
        mDialogFragment = new LocaleDialogFragment();
    }

    private void setArgument(
            int type, ResultReceiver receiver) {
        LocaleStore.LocaleInfo localeInfo = LocaleStore.getLocaleInfo(Locale.ENGLISH);
        Bundle args = new Bundle();
        args.putInt(ARG_DIALOG_TYPE, type);
        args.putSerializable(ARG_TARGET_LOCALE, localeInfo);
        args.putParcelable(ARG_RESULT_RECEIVER, receiver);
        mDialogFragment.setArguments(args);
    }

    @Test
    public void getDialogContent_confirmSystemDefault_has2ButtonText() {
        setArgument(LocaleDialogFragment.DIALOG_CONFIRM_SYSTEM_DEFAULT, null);
        LocaleDialogFragment.LocaleDialogController controller =
                new LocaleDialogFragment.LocaleDialogController(mContext, mDialogFragment);

        LocaleDialogFragment.LocaleDialogController.DialogContent dialogContent =
                controller.getDialogContent();

        assertEquals(ResourcesUtils.getResourcesString(
                mContext, "button_label_confirmation_of_system_locale_change"),
                dialogContent.mPositiveButton);
        assertEquals(ResourcesUtils.getResourcesString(mContext, "cancel"),
                dialogContent.mNegativeButton);
    }

    @Test
    public void getDialogContent_unavailableLocale_has1ButtonText() {
        setArgument(LocaleDialogFragment.DIALOG_NOT_AVAILABLE_LOCALE, null);
        LocaleDialogFragment.LocaleDialogController controller =
                new LocaleDialogFragment.LocaleDialogController(mContext, mDialogFragment);

        LocaleDialogFragment.LocaleDialogController.DialogContent dialogContent =
                controller.getDialogContent();

        assertEquals(ResourcesUtils.getResourcesString(mContext, "okay"),
                dialogContent.mPositiveButton);
        assertTrue(dialogContent.mNegativeButton.isEmpty());
    }

    @Test
    public void onClick_clickPositiveButton_sendOK() {
        ResultReceiver resultReceiver = spy(new ResultReceiver(null));
        setArgument(LocaleDialogFragment.DIALOG_CONFIRM_SYSTEM_DEFAULT, resultReceiver);
        LocaleDialogFragment.LocaleDialogController controller =
                new LocaleDialogFragment.LocaleDialogController(mContext, mDialogFragment);

        controller.onClick(null, DialogInterface.BUTTON_POSITIVE);

        verify(resultReceiver).send(eq(Activity.RESULT_OK), any());
    }

    @Test
    public void onClick_clickNegativeButton_sendCancel() {
        ResultReceiver resultReceiver = spy(new ResultReceiver(null));
        setArgument(LocaleDialogFragment.DIALOG_CONFIRM_SYSTEM_DEFAULT, resultReceiver);
        LocaleDialogFragment.LocaleDialogController controller =
                new LocaleDialogFragment.LocaleDialogController(mContext, mDialogFragment);

        controller.onClick(null, DialogInterface.BUTTON_NEGATIVE);

        verify(resultReceiver).send(eq(Activity.RESULT_CANCELED), any());
    }

    @Test
    public void getMetricsCategory_systemLocaleChange() {
        setArgument(LocaleDialogFragment.DIALOG_CONFIRM_SYSTEM_DEFAULT, null);

        int result = mDialogFragment.getMetricsCategory();

        assertEquals(SettingsEnums.DIALOG_SYSTEM_LOCALE_CHANGE, result);
    }

    @Test
    public void getMetricsCategory_unavailableLocale() {
        setArgument(LocaleDialogFragment.DIALOG_NOT_AVAILABLE_LOCALE, null);

        int result = mDialogFragment.getMetricsCategory();

        assertEquals(SettingsEnums.DIALOG_SYSTEM_LOCALE_UNAVAILABLE, result);
    }
}
