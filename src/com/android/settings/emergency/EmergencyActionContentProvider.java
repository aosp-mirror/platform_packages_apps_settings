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

package com.android.settings.emergency;

import static android.telecom.TelecomManager.EXTRA_CALL_SOURCE;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.telecom.PhoneAccount;
import android.telecom.TelecomManager;
import android.text.TextUtils;
import android.util.Log;

import com.android.settings.R;
import com.android.settingslib.emergencynumber.EmergencyNumberUtils;

/**
 * ContentProvider to delegate emergency action work
 */
public class EmergencyActionContentProvider extends ContentProvider {
    private static final String TAG = "EmergencyActionContentP";

    private static final String ACTION_START_EMERGENCY_CALL =
            "com.android.settings.emergency.MAKE_EMERGENCY_CALL";

    @Override
    public Bundle call(String authority, String method, String arg, Bundle extras) {
        int uid = Binder.getCallingUid();
        Log.d(TAG, "calling pid/uid" + Binder.getCallingPid() + "/" + uid);
        if (!isEmergencyInfo(getContext())) {
            throw new SecurityException("Uid is not allowed: " + uid);
        }
        if (!TextUtils.equals(method, ACTION_START_EMERGENCY_CALL)) {
            throw new IllegalArgumentException("Unsupported operation");
        }
        placeEmergencyCall(getContext());
        return new Bundle();
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        return null;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }

    private static boolean isEmergencyInfo(Context context) {
        final int callingUid = Binder.getCallingUid();
        final String callingPackage = context.getPackageManager().getPackagesForUid(callingUid)[0];
        return TextUtils.equals(callingPackage,
                context.getString(R.string.config_aosp_emergency_package_name));
    }

    private static void placeEmergencyCall(Context context) {
        if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
            Log.i(TAG, "Telephony is not supported, skipping.");
            return;
        }
        Bundle extras = new Bundle();
        extras.putBoolean(TelecomManager.EXTRA_IS_USER_INTENT_EMERGENCY_CALL, true);
        extras.putInt(EXTRA_CALL_SOURCE, TelecomManager.CALL_SOURCE_EMERGENCY_SHORTCUT);
        TelecomManager telecomManager = context.getSystemService(TelecomManager.class);
        EmergencyNumberUtils emergencyNumberUtils = new EmergencyNumberUtils(context);
        telecomManager.placeCall(
                Uri.fromParts(PhoneAccount.SCHEME_TEL, emergencyNumberUtils.getPoliceNumber(),
                        /* fragment= */ null), extras);
    }
}
