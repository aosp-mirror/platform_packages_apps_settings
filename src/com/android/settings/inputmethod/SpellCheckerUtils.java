/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.settings.inputmethod;

import android.util.Log;
import android.view.textservice.SpellCheckerInfo;
import android.view.textservice.TextServicesManager;

public class SpellCheckerUtils {
    private static final String TAG = SpellCheckerUtils.class.getSimpleName();
    private static final boolean DBG = false;
    public static void setSpellCheckersEnabled(TextServicesManager tsm, boolean enable) {
    }
    public static boolean getSpellCheckersEnabled(TextServicesManager tsm) {
        return true;
    }
    public static void setCurrentSpellChecker(TextServicesManager tsm, SpellCheckerInfo info) {
    }
    public static SpellCheckerInfo getCurrentSpellChecker(TextServicesManager tsm) {
        final SpellCheckerInfo retval = tsm.getCurrentSpellChecker();
        if (DBG) {
            Log.d(TAG, "getCurrentSpellChecker: " + retval);
        }
        return retval;
    }
    public static SpellCheckerInfo[] getEnabledSpellCheckers(TextServicesManager tsm) {
        final SpellCheckerInfo[] retval = tsm.getEnabledSpellCheckers();
        if (DBG) {
            Log.d(TAG, "get spell checkers: " + retval.length);
        }
        return retval;
    }
}
