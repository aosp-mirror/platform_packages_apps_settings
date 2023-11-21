/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.deviceinfo.firmwareversion;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;

import com.android.settings.core.BasePreferenceController;
import com.android.settings.flags.Flags;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.TimeZone;

public class MainlineModuleVersionPreferenceController extends BasePreferenceController {

    private static final String TAG = "MainlineModuleControl";
    private static final List<String> VERSION_NAME_DATE_PATTERNS = Arrays.asList("yyyy-MM-dd",
            "yyyy-MM");
    @VisibleForTesting
    static final String MODULE_UPDATE_INTENT_ACTION =
        "android.settings.MODULE_UPDATE_SETTINGS";
    @VisibleForTesting
    static final String MODULE_UPDATE_V2_INTENT_ACTION =
        "android.settings.MODULE_UPDATE_VERSIONS";

    private final Intent mModuleUpdateIntent;
    private final Intent mModuleUpdateV2Intent;
    private final PackageManager mPackageManager;

    private String mModuleVersion;

    public MainlineModuleVersionPreferenceController(Context context, String key) {
        super(context, key);
        mPackageManager = mContext.getPackageManager();
        mModuleUpdateIntent = new Intent(MODULE_UPDATE_INTENT_ACTION);
        mModuleUpdateV2Intent = new Intent(MODULE_UPDATE_V2_INTENT_ACTION);
        if (Flags.mainlineModuleExplicitIntent()) {
          String packageName = mContext
              .getString(com.android.settings.R.string.config_mainline_module_update_package);
          mModuleUpdateIntent.setPackage(packageName);
          mModuleUpdateV2Intent.setPackage(packageName);
        }
        initModules();
    }

    @Override
    public int getAvailabilityStatus() {
        return !TextUtils.isEmpty(mModuleVersion) ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    private void initModules() {
        final String moduleProvider = mContext.getString(
                com.android.internal.R.string.config_defaultModuleMetadataProvider);
        if (!TextUtils.isEmpty(moduleProvider)) {
            try {
                mModuleVersion =
                        mPackageManager.getPackageInfo(moduleProvider, 0 /* flags */).versionName;
                return;
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, "Failed to get mainline version.", e);
                mModuleVersion = null;
            }
        }
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);

        final ResolveInfo resolvedV2 =
                mPackageManager.resolveActivity(mModuleUpdateV2Intent, 0 /* flags */);
        if (resolvedV2 != null) {
            preference.setIntent(mModuleUpdateV2Intent);
            preference.setSelectable(true);
            return;
        }

        final ResolveInfo resolved =
                mPackageManager.resolveActivity(mModuleUpdateIntent, 0 /* flags */);
        if (resolved != null) {
            preference.setIntent(mModuleUpdateIntent);
            preference.setSelectable(true);
        } else {
            Log.d(TAG, "The ResolveInfo of the update intent is null.");
            preference.setIntent(null);
            preference.setSelectable(false);
        }
    }

    @Override
    public CharSequence getSummary() {
        if (TextUtils.isEmpty(mModuleVersion)) {
            return mModuleVersion;
        }

        final Optional<Date> parsedDate = parseDateFromVersionName(mModuleVersion);
        if (!parsedDate.isPresent()) {
            Log.w("Could not parse mainline versionName (%s) as date.", mModuleVersion);
            return mModuleVersion;
        }

        return DateFormat.getLongDateFormat(mContext).format(parsedDate.get());
    }

    private Optional<Date> parseDateFromVersionName(String text) {
        for (String pattern : VERSION_NAME_DATE_PATTERNS) {
            try {
                final SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern,
                        Locale.getDefault());
                simpleDateFormat.setTimeZone(TimeZone.getDefault());
                return Optional.of(simpleDateFormat.parse(text));
            } catch (ParseException e) {
                // ignore and try next pattern
            }
        }
        return Optional.empty();
    }
}
