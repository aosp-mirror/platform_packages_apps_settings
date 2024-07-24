/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings;

import android.app.Activity;
import android.app.AppOpsManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

public class ActionDisabledByAppOpsDialog extends Activity
        implements DialogInterface.OnDismissListener {

    private static final String TAG = "ActionDisabledByAppOpsDialog";

    private ActionDisabledByAppOpsHelper mDialogHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDialogHelper = new ActionDisabledByAppOpsHelper(this);
        mDialogHelper.prepareDialogBuilder()
                .setOnDismissListener(this)
                .show();
        updateAppOps();
    }

    private void updateAppOps() {
        final Intent intent = getIntent();
        final String packageName = intent.getStringExtra(Intent.EXTRA_PACKAGE_NAME);
        final int uid = intent.getIntExtra(Intent.EXTRA_UID, android.os.Process.INVALID_UID);
        getSystemService(AppOpsManager.class)
                .setMode(AppOpsManager.OP_ACCESS_RESTRICTED_SETTINGS,
                        uid,
                        packageName,
                        AppOpsManager.MODE_IGNORED);
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        mDialogHelper.updateDialog();
        updateAppOps();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        finish();
    }
}
