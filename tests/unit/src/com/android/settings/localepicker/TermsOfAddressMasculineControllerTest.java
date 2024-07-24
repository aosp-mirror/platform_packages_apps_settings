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

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.spy;

import android.content.Context;
import android.os.Looper;

import com.android.settings.widget.TickButtonPreference;

import androidx.preference.PreferenceManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class TermsOfAddressMasculineControllerTest {

    private static final String KEY_CATEGORY_TERMS_OF_ADDRESS = "key_category_terms_of_address";
    private static final String KEY_FEMININE = "key_terms_of_address_feminine";
    private static final String KEY_MASCULINE = "key_terms_of_address_masculine";
    private static final String KEY_NEUTRAL = "key_terms_of_address_neutral";
    private static final String KEY_NOT_SPECIFIED = "key_terms_of_address_not_specified";

    private Context mContext;
    private PreferenceManager mPreferenceManager;
    private PreferenceCategory mPreferenceCategory;
    private PreferenceScreen mPreferenceScreen;
    private TermsOfAddressMasculineController mController;
    private TickButtonPreference mFemininePreference;
    private TickButtonPreference mMasculinePreference;
    private TickButtonPreference mNotSpecifiedPreference;
    private TickButtonPreference mNeutralPreference;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mContext = spy(ApplicationProvider.getApplicationContext());

        if (Looper.myLooper() == null) {
            Looper.prepare();
        }

        mPreferenceManager = new PreferenceManager(mContext);
        mPreferenceScreen = mPreferenceManager.createPreferenceScreen(mContext);
        mPreferenceCategory = new PreferenceCategory(mContext);
        mPreferenceCategory.setKey(KEY_CATEGORY_TERMS_OF_ADDRESS);
        mNotSpecifiedPreference = new TickButtonPreference(mContext);
        mNotSpecifiedPreference.setKey(KEY_NOT_SPECIFIED);
        mFemininePreference = new TickButtonPreference(mContext);
        mFemininePreference.setKey(KEY_FEMININE);
        mMasculinePreference = new TickButtonPreference(mContext);
        mMasculinePreference.setKey(KEY_MASCULINE);
        mNeutralPreference = new TickButtonPreference(mContext);
        mNeutralPreference.setKey(KEY_NEUTRAL);
        mPreferenceScreen.addPreference(mPreferenceCategory);
        mPreferenceScreen.addPreference(mNotSpecifiedPreference);
        mPreferenceScreen.addPreference(mFemininePreference);
        mPreferenceScreen.addPreference(mMasculinePreference);
        mPreferenceScreen.addPreference(mNeutralPreference);
        mController = new TermsOfAddressMasculineController(mContext, KEY_MASCULINE);
        mController.displayPreference(mPreferenceScreen);
    }

    @Test
    public void displayPreference_setGrammaticalGenderIsMasculine_MasculineIsSelected() {
        TickButtonPreference selectedPreference =
                (TickButtonPreference) mPreferenceScreen.getPreference(3);
        TickButtonPreference pref = (TickButtonPreference) mPreferenceScreen.getPreference(1);

        selectedPreference.performClick();

        assertThat(selectedPreference.getKey()).isEqualTo(KEY_MASCULINE);
        assertThat(selectedPreference.isSelected()).isTrue();
        assertThat(pref.isSelected()).isFalse();
    }
}
