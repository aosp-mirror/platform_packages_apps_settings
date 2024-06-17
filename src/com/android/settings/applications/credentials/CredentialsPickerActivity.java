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

package com.android.settings.applications.credentials;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Slog;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.SettingsActivity;

/**
 * Standalone activity used to launch a {@link DefaultCombinedPicker} fragment if the user is a
 * normal user, a {@link DefaultCombinedPickerWork} fragment if the user is a work profile or {@link
 * DefaultCombinedPickerPrivate} fragment if the user is a private profile.
 */
public class CredentialsPickerActivity extends SettingsActivity {
    private static final String TAG = "CredentialsPickerActivity";

    /** Injects the fragment name into the intent so the correct fragment is opened. */
    @VisibleForTesting
    public static void injectFragmentIntoIntent(Context context, Intent intent) {
        final int userId = UserHandle.myUserId();
        final UserManager userManager = UserManager.get(context);

        if (DefaultCombinedPickerWork.isUserHandledByFragment(userManager, userId)) {
            Slog.d(TAG, "Creating picker fragment using work profile");
            intent.putExtra(EXTRA_SHOW_FRAGMENT, DefaultCombinedPickerWork.class.getName());
        } else if (DefaultCombinedPickerPrivate.isUserHandledByFragment(userManager)) {
            Slog.d(TAG, "Creating picker fragment using private profile");
            intent.putExtra(EXTRA_SHOW_FRAGMENT, DefaultCombinedPickerPrivate.class.getName());
        } else {
            Slog.d(TAG, "Creating picker fragment using normal profile");
            intent.putExtra(EXTRA_SHOW_FRAGMENT, DefaultCombinedPicker.class.getName());
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        injectFragmentIntoIntent(this, getIntent());
        super.onCreate(savedInstanceState);
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return super.isValidFragment(fragmentName)
                || DefaultCombinedPicker.class.getName().equals(fragmentName)
                || DefaultCombinedPickerWork.class.getName().equals(fragmentName)
                || DefaultCombinedPickerPrivate.class.getName().equals(fragmentName);
    }
}
