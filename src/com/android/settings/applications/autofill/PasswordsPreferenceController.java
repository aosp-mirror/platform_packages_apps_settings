/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.applications.autofill;

import android.annotation.UserIdInt;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;
import android.service.autofill.AutofillServiceInfo;
import android.text.TextUtils;
import android.util.IconDrawableFactory;

import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.Utils;
import com.android.settings.core.BasePreferenceController;

import java.util.List;

/**
 * Queries available autofill services and adds preferences for those that declare passwords
 * settings.
 */
public class PasswordsPreferenceController extends BasePreferenceController {

    private final PackageManager mPm;
    private final IconDrawableFactory mIconFactory;
    private final List<AutofillServiceInfo> mServices;

    public PasswordsPreferenceController(Context context, String preferenceKey) {
        this(context, preferenceKey,
                AutofillServiceInfo.getAvailableServices(context, UserHandle.myUserId()));
    }

    @VisibleForTesting
    public PasswordsPreferenceController(
            Context context, String preferenceKey, List<AutofillServiceInfo> availableServices) {
        super(context, preferenceKey);
        mPm = context.getPackageManager();
        mIconFactory = IconDrawableFactory.newInstance(mContext);
        for (int i = availableServices.size() - 1; i >= 0; i--) {
            final String passwordsActivity = availableServices.get(i).getPasswordsActivity();
            if (TextUtils.isEmpty(passwordsActivity)) {
                availableServices.remove(i);
            }
        }
        mServices = availableServices;
    }

    @Override
    public int getAvailabilityStatus() {
        return mServices.isEmpty() ? CONDITIONALLY_UNAVAILABLE : AVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        final PreferenceGroup group = screen.findPreference(getPreferenceKey());
        // TODO(b/169455298): Show work profile passwords too.
        addPasswordPreferences(screen.getContext(), UserHandle.myUserId(), group);
    }

    private void addPasswordPreferences(
            Context prefContext, @UserIdInt int user, PreferenceGroup group) {
        for (int i = 0; i < mServices.size(); i++) {
            final AutofillServiceInfo service = mServices.get(i);
            final Preference pref = new Preference(prefContext);
            final ServiceInfo serviceInfo = service.getServiceInfo();
            pref.setTitle(serviceInfo.loadLabel(mPm));
            final Drawable icon =
                    mIconFactory.getBadgedIcon(
                            serviceInfo,
                            serviceInfo.applicationInfo,
                            user);
            Utils.setSafeIcon(pref, icon);
            pref.setIntent(
                    new Intent(Intent.ACTION_MAIN)
                            .setClassName(serviceInfo.packageName, service.getPasswordsActivity()));
            group.addPreference(pref);
        }
    }
}
