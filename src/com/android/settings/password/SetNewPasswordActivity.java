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

import android.annotation.Nullable;
import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.Intent;
import android.os.Bundle;

import com.android.settings.ChooseLockGeneric;

/**
 * Trampolines {@link DevicePolicyManager#ACTION_SET_NEW_PASSWORD} and
 * {@link DevicePolicyManager#ACTION_SET_NEW_PARENT_PROFILE_PASSWORD} intent to the appropriate UI
 * activity for handling set new password.
 */
public class SetNewPasswordActivity extends Activity implements SetNewPasswordController.Ui {
    private String mNewPasswordAction;
    private SetNewPasswordController mSetNewPasswordController;

    @Override
    protected void onCreate(Bundle savedState) {
        super.onCreate(savedState);

        mNewPasswordAction = getIntent().getAction();
        mSetNewPasswordController = new SetNewPasswordController(this, this);
        mSetNewPasswordController.dispatchSetNewPasswordIntent();
    }

    @Override
    public void launchChooseLock(@Nullable Bundle chooseLockFingerprintExtras) {
        Intent intent = new Intent(this, ChooseLockGeneric.class)
                .setAction(mNewPasswordAction);
        if (chooseLockFingerprintExtras != null) {
            intent.putExtras(chooseLockFingerprintExtras);
        }
        startActivity(intent);
        finish();
    }
}
