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

package com.android.settings.localepicker;

import android.app.GrammaticalInflectionManager;
import android.content.Context;

import androidx.annotation.NonNull;

/**
 * A helper used to get, set, and cache system grammatical gender.
 */
public class TermsOfAddressHelper {

    private int mSystemGrammaticalGender;
    private GrammaticalInflectionManager mGrammaticalInflectionManager;

    public TermsOfAddressHelper(@NonNull Context context) {
        mGrammaticalInflectionManager = context.getSystemService(
                GrammaticalInflectionManager.class);
        mSystemGrammaticalGender = mGrammaticalInflectionManager.getSystemGrammaticalGender();
    }

    /**
     * set system grammatical gender
     *
     * @param gender system grammatical gender
     */
    public void setSystemGrammaticalGender(int gender) {
        mGrammaticalInflectionManager.setSystemWideGrammaticalGender(gender);
        mSystemGrammaticalGender = gender;
    }

    /**
     * get system grammatical gender
     */
    public int getSystemGrammaticalGender() {
        return mSystemGrammaticalGender;
    }
}
