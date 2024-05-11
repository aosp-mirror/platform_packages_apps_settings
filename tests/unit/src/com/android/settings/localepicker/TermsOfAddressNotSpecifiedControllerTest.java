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

import static org.mockito.Mockito.spy;

import android.app.GrammaticalInflectionManager;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Looper;

import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.widget.TickButtonPreference;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class TermsOfAddressNotSpecifiedControllerTest {

    private static final String KEY_CATEGORY_TERMS_OF_ADDRESS = "key_category_terms_of_address";
    private static final String KEY_FEMININE = "key_terms_of_address_feminine";
    private static final String KEY_MASCULINE = "key_terms_of_address_masculine";
    private static final String KEY_NEUTRAL = "key_terms_of_address_neutral";
    private static final String KEY_NOT_SPECIFIED = "key_terms_of_address_not_specified";

    private Context mContext;
    private PreferenceManager mPreferenceManager;
    private PreferenceCategory mPreferenceCategory;
    private PreferenceScreen mPreferenceScreen;
    private TermsOfAddressNotSpecifiedController mController;
    private TickButtonPreference mFemininePreference;
    private TickButtonPreference mMasculinePreference;
    private TickButtonPreference mNotSpecifiedPreference;
    private TickButtonPreference mNeutralPreference;
    private GrammaticalInflectionManager mGrammaticalInflectionManager;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mContext = spy(ApplicationProvider.getApplicationContext());

        if (Looper.myLooper() == null) {
            Looper.prepare();
        }

        mGrammaticalInflectionManager = mContext.getSystemService(
                GrammaticalInflectionManager.class);
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
        mController = new TermsOfAddressNotSpecifiedController(mContext, KEY_NOT_SPECIFIED);
        mController.setTermsOfAddressHelper(new TermsOfAddressHelper(mContext));
        mController.displayPreference(mPreferenceScreen);
    }

    @Test
    @Ignore("b/339543490")
    public void displayPreference_setGrammaticalGenderIsNotSpecified_NotSpecifiedIsSelected() {
        TickButtonPreference selectedPreference =
                (TickButtonPreference) mPreferenceScreen.getPreference(1);
        selectedPreference.performClick();

        assertThat(selectedPreference.getKey()).isEqualTo(KEY_NOT_SPECIFIED);
        assertThat(mGrammaticalInflectionManager.getSystemGrammaticalGender()).isEqualTo(
                Configuration.GRAMMATICAL_GENDER_NOT_SPECIFIED);
    }
}
