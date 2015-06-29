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
package com.android.settings.applications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

public class PermissionsSummaryHelper {

    private static final String ACTION_PERM_COUNT_RESPONSE
            = "com.android.settings.PERM_COUNT_RESPONSE";
    private static final String ACTION_APP_COUNT_RESPONSE
            = "com.android.settings.APP_COUNT_RESPONSE";

    public static BroadcastReceiver getPermissionSummary(Context context, String pkg,
            PermissionsResultCallback callback) {
        Intent request = new Intent(Intent.ACTION_GET_PERMISSIONS_COUNT);
        request.putExtra(Intent.EXTRA_PACKAGE_NAME, pkg);
        return sendPermissionRequest(context, ACTION_PERM_COUNT_RESPONSE, request, callback);
    }

    public static BroadcastReceiver getAppWithPermissionsCounts(Context context,
            PermissionsResultCallback callback) {
        Intent request = new Intent(Intent.ACTION_GET_PERMISSIONS_COUNT);
        return sendPermissionRequest(context, ACTION_APP_COUNT_RESPONSE, request, callback);
    }

    private static BroadcastReceiver sendPermissionRequest(Context context, String action,
            Intent request, final PermissionsResultCallback callback) {
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int[] counts = intent.getIntArrayExtra(Intent.EXTRA_GET_PERMISSIONS_COUNT_RESULT);

                CharSequence[] groups = intent.getCharSequenceArrayExtra(
                        Intent.EXTRA_GET_PERMISSIONS_GROUP_LIST_RESULT);

                callback.onPermissionSummaryResult(counts, groups);

                context.unregisterReceiver(this);
            }
        };
        context.registerReceiver(receiver, new IntentFilter(action));
        request.putExtra(Intent.EXTRA_GET_PERMISSIONS_RESPONSE_INTENT, action);
        request.setFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        context.sendBroadcast(request);
        return receiver;
    }

    public interface PermissionsResultCallback {
        void onPermissionSummaryResult(int[] counts, CharSequence[] groupLabels);
    }
}
