/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.settings.utils;

import android.app.VoiceInteractor.PickOptionRequest.Option;
import android.os.Bundle;
import android.text.TextUtils;

/**
 * Model for a single item that can be selected by a {@link VoiceSelectionFragment}.
 * Each item consists of a visual label and several alternative synonyms for the item
 * that can be used to identify the item by voice.
 */
public class VoiceSelection {
    final CharSequence mLabel;
    final CharSequence[] mSynonyms;

    /**
     * Created a new selectable item with a visual label and a set of synonyms.
     */
    public VoiceSelection(CharSequence label, CharSequence synonyms) {
        mLabel = label;
        mSynonyms = TextUtils.split(synonyms.toString(), ",");
    }

    /**
     * Created a new selectable item with a visual label and no synonyms.
     */
    public VoiceSelection(CharSequence label) {
        mLabel = label;
        mSynonyms = null;
    }

    public CharSequence getLabel() {
        return mLabel;
    }

    public CharSequence[] getSynonyms() {
        return mSynonyms;
    }

    Option toOption(int index) {
        Option result = new Option(mLabel);
        Bundle extras = new Bundle();
        extras.putInt("index", index);
        result.setExtras(extras);

        for (CharSequence synonym : mSynonyms) {
            result.addSynonym(synonym);
        }
        return result;
    }

    /**
     * Listener interface for when an item is selected.
     */
    public interface OnItemSelectedListener {
        abstract void onItemSelected(int position, VoiceSelection selection);
    };
}
