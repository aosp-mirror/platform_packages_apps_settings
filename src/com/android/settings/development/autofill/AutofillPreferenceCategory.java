/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.android.settings.development.autofill;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.view.autofill.AutofillManager;

import androidx.preference.PreferenceCategory;

public final class AutofillPreferenceCategory extends PreferenceCategory {

    private static final String TAG = "AutofillPreferenceCategory";

    private final ContentResolver mContentResolver;
    private final ContentObserver mSettingsObserver;

    public AutofillPreferenceCategory(Context context, AttributeSet attrs) {
        super(context, attrs);

        mSettingsObserver = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange, Uri uri, int userId) {
                Log.w(TAG, "Autofill Service changed, but UI cannot be refreshed");
                // TODO(b/111838239): we cannot update the UI because AFM.isEnabled() will return
                // the previous value. Once that's fixed, we'll need to call one of the 2 callbacks
                // below:
                // notifyChanged();
                // notifyDependencyChange(shouldDisableDependents());
            }
        };
        mContentResolver = context.getContentResolver();
    }

    @Override
    public void onAttached() {
        super.onAttached();

        mContentResolver.registerContentObserver(
                Settings.Secure.getUriFor(Settings.Secure.AUTOFILL_SERVICE), false,
                mSettingsObserver);
    }

    @Override
    public void onDetached() {
        mContentResolver.unregisterContentObserver(mSettingsObserver);

        super.onDetached();
    }

    // PreferenceCategory.isEnabled() always return false, so we rather not change that logic
    // decide whether the children should be shown using isAutofillEnabled() instead.
    private boolean isAutofillEnabled() {
        final AutofillManager afm = getContext().getSystemService(AutofillManager.class);
        final boolean enabled = afm != null && afm.isEnabled();
        Log.v(TAG, "isAutofillEnabled(): " + enabled);
        return enabled;
    }

    @Override
    public boolean shouldDisableDependents() {
        final boolean shouldIt = !isAutofillEnabled();
        Log.v(TAG, "shouldDisableDependents(): " + shouldIt);
        return shouldIt;
    }
}
