/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.development.autofill;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.autofill.AutofillManager;

import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

/**
 * Controller class for observing the state of AutofillManager.
 */
public class AutofillCategoryController extends DeveloperOptionsPreferenceController implements
        LifecycleObserver, OnStart, OnStop {

    private static final String TAG = "AutofillCategoryController";

    private static final String CATEGORY_KEY = "debug_autofill_category";
    private static final long DELAYED_MESSAGE_TIME_MS = 2000;

    private ContentResolver mContentResolver;
    private ContentObserver mSettingsObserver;
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    public AutofillCategoryController(Context context, Lifecycle lifecycle) {
        super(context);

        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }

        mSettingsObserver = new ContentObserver(mHandler) {
            @Override
            public void onChange(boolean selfChange, Uri uri, int userId) {
                // We cannot apply the change yet because AutofillManager.isEnabled() state is
                // updated by a ContentObserver as well and there's no guarantee of which observer
                // is called first - hence, it's possible that the state didn't change here yet.
                mHandler.postDelayed(
                        () -> mPreference.notifyDependencyChange(shouldDisableDependents()),
                        DELAYED_MESSAGE_TIME_MS);
            }
        };
        mContentResolver = context.getContentResolver();
    }

    @Override
    public String getPreferenceKey() {
        return CATEGORY_KEY;
    }

    @Override
    public void onStart() {
        mContentResolver.registerContentObserver(
                Settings.Secure.getUriFor(Settings.Secure.AUTOFILL_SERVICE), false,
                mSettingsObserver);

    }

    @Override
    public void onStop() {
        mContentResolver.unregisterContentObserver(mSettingsObserver);
    }

    // PreferenceCategory.isEnabled() always return false, so we rather not change that logic
    // decide whether the children should be shown using isAutofillEnabled() instead.
    private boolean isAutofillEnabled() {
        final AutofillManager afm = mContext.getSystemService(AutofillManager.class);
        final boolean enabled = afm != null && afm.isEnabled();
        Log.v(TAG, "isAutofillEnabled(): " + enabled);
        return enabled;
    }

    private boolean shouldDisableDependents() {
        final boolean shouldIt = !isAutofillEnabled();
        Log.v(TAG, "shouldDisableDependents(): " + shouldIt);
        return shouldIt;
    }
}
