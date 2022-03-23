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
import android.os.Looper;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.view.autofill.AutofillManager;

import androidx.preference.PreferenceCategory;

public final class AutofillPreferenceCategory extends PreferenceCategory {

    private static final String TAG = "AutofillPreferenceCategory";
    private static final long DELAYED_MESSAGE_TIME_MS = 2000;

    private final ContentResolver mContentResolver;
    private final ContentObserver mSettingsObserver;
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    public AutofillPreferenceCategory(Context context, AttributeSet attrs) {
        super(context, attrs);

        mSettingsObserver = new ContentObserver(mHandler) {
            @Override
            public void onChange(boolean selfChange, Uri uri, int userId) {
                // We cannot apply the change yet because AutofillManager.isEnabled() state is
                // updated by a ContentObserver as well and there's no guarantee of which observer
                // is called first - hence, it's possible that the state didn't change here yet.
                mHandler.postDelayed(() -> notifyDependencyChange(shouldDisableDependents()),
                        DELAYED_MESSAGE_TIME_MS);
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
