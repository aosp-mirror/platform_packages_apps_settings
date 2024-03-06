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

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Looper;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;

import java.util.Locale;

@RunWith(AndroidJUnit4.class)
public class TermsOfAddressCategoryControllerTest {

    private static final String KEY_CATEGORY_TERMS_OF_ADDRESS = "key_category_terms_of_address";

    private Context mContext;
    private TermsOfAddressCategoryController mTermsOfAddressCategoryController;
    private Locale mCacheLocale;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(ApplicationProvider.getApplicationContext());

        if (Looper.myLooper() == null) {
            Looper.prepare();
        }

        mTermsOfAddressCategoryController = new TermsOfAddressCategoryController(mContext,
                KEY_CATEGORY_TERMS_OF_ADDRESS);
        mCacheLocale = Locale.getDefault(Locale.Category.FORMAT);
    }

    @After
    public void tearDown() throws Exception {
        Locale.setDefault(mCacheLocale);
    }

    @Test
    public void getAvailabilityStatus_returnUnavailable() {
        Locale.setDefault(Locale.forLanguageTag("fr-CA"));

        assertThat(mTermsOfAddressCategoryController.getAvailabilityStatus()).isEqualTo(
                CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_returnAvailable() {
        Locale.setDefault(Locale.forLanguageTag("fr-FR"));

        assertThat(mTermsOfAddressCategoryController.getAvailabilityStatus()).isEqualTo(
                AVAILABLE);
    }
}
