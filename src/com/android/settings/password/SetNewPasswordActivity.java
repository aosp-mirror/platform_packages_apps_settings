/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings.password;

import static android.app.admin.DevicePolicyManager.ACTION_SET_NEW_PARENT_PROFILE_PASSWORD;
import static android.app.admin.DevicePolicyManager.ACTION_SET_NEW_PASSWORD;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.android.settings.ChooseLockGeneric;
import com.android.settings.SetupChooseLockGeneric;
import com.android.settings.Utils;

/**
 * Trampolines {@link DevicePolicyManager#ACTION_SET_NEW_PASSWORD} and
 * {@link DevicePolicyManager#ACTION_SET_NEW_PARENT_PROFILE_PASSWORD} intent to the appropriate UI
 * activity for handling set new password.
 */
public class SetNewPasswordActivity extends Activity implements SetNewPasswordController.Ui {
    private static final String TAG = "SetNewPasswordActivity";
    private String mNewPasswordAction;
    private SetNewPasswordController mSetNewPasswordController;

    @Override
    protected void onCreate(Bundle savedState) {
        super.onCreate(savedState);

        mNewPasswordAction = getIntent().getAction();
        if (!ACTION_SET_NEW_PASSWORD.equals(mNewPasswordAction)
                && !ACTION_SET_NEW_PARENT_PROFILE_PASSWORD.equals(mNewPasswordAction)) {
            Log.e(TAG, "Unexpected action to launch this activity");
            finish();
            return;
        }
        mSetNewPasswordController = SetNewPasswordController.create(
                this, this, getIntent(), getActivityToken());
        mSetNewPasswordController.dispatchSetNewPasswordIntent();
    }

    @Override
    public void launchChooseLock(Bundle chooseLockFingerprintExtras) {
        Intent intent = new Intent(this, ChooseLockGeneric.class)
                .setAction(mNewPasswordAction);
        intent.putExtras(chooseLockFingerprintExtras);
        startActivity(intent);
        finish();
    }
}
