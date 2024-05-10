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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Bundle;

import androidx.test.annotation.UiThreadTest;
import androidx.test.core.app.ApplicationProvider;

import com.android.internal.app.LocaleStore;
import com.android.settings.testutils.FakeFeatureFactory;
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
    private LocaleListEditor mLocaleListEditor;
    private LocaleDialogFragment mDialogFragment;
    private FakeFeatureFactory mFeatureFactory;

    @Before
    public void setUp() throws Exception {
        mContext = ApplicationProvider.getApplicationContext();
        mDialogFragment = new LocaleDialogFragment();
        mLocaleListEditor = spy(new LocaleListEditor());
        mFeatureFactory = FakeFeatureFactory.setupForTest();
    }

    private void setArgument(int type) {
        LocaleStore.LocaleInfo localeInfo = LocaleStore.getLocaleInfo(Locale.ENGLISH);
        Bundle args = new Bundle();
        args.putInt(ARG_DIALOG_TYPE, type);
        args.putSerializable(ARG_TARGET_LOCALE, localeInfo);
        mDialogFragment.setArguments(args);
    }

    @Test
    public void getDialogContent_confirmSystemDefault_has2ButtonText() {
        setArgument(DIALOG_CONFIRM_SYSTEM_DEFAULT);
        LocaleDialogFragment.LocaleDialogController controller =
                mDialogFragment.getLocaleDialogController(mContext, mDialogFragment,
                        mLocaleListEditor);

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
        setArgument(LocaleDialogFragment.DIALOG_NOT_AVAILABLE_LOCALE);
        LocaleDialogFragment.LocaleDialogController controller =
                mDialogFragment.getLocaleDialogController(mContext, mDialogFragment,
                        mLocaleListEditor);

        LocaleDialogFragment.LocaleDialogController.DialogContent dialogContent =
                controller.getDialogContent();

        assertEquals(ResourcesUtils.getResourcesString(mContext, "okay"),
                dialogContent.mPositiveButton);
        assertTrue(dialogContent.mNegativeButton.isEmpty());
    }

    @Test
    public void getMetricsCategory_systemLocaleChange() {
        setArgument(DIALOG_CONFIRM_SYSTEM_DEFAULT);
        int result = mDialogFragment.getMetricsCategory();

        assertEquals(SettingsEnums.DIALOG_SYSTEM_LOCALE_CHANGE, result);
    }

    @Test
    public void getMetricsCategory_unavailableLocale() {
        setArgument(LocaleDialogFragment.DIALOG_NOT_AVAILABLE_LOCALE);
        int result = mDialogFragment.getMetricsCategory();

        assertEquals(SettingsEnums.DIALOG_SYSTEM_LOCALE_UNAVAILABLE, result);
    }
}
