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

import android.app.Fragment;
import android.content.Intent;
import android.preference.PreferenceActivity;

import com.android.settings.ChooseLockPassword.ChooseLockPasswordFragment;

public class InputMethodAndSubtypeEnablerActivity extends PreferenceActivity {
    @Override
    public Intent getIntent() {
        final Intent modIntent = new Intent(super.getIntent());
        if (!modIntent.hasExtra(EXTRA_SHOW_FRAGMENT)) {
            modIntent.putExtra(EXTRA_SHOW_FRAGMENT, InputMethodAndSubtypeEnabler.class.getName());
            modIntent.putExtra(EXTRA_NO_HEADERS, true);
        }
        return modIntent;
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        if (InputMethodAndSubtypeEnabler.class.getName().equals(fragmentName)) return true;
        return false;
    }

}
