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

package com.android.settings.users;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.NewUserRequest;
import android.os.NewUserResponse;
import android.os.UserManager;

import androidx.annotation.Nullable;

import com.android.settings.R;

import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Fallback activity for supervised user creation.
 * Built to test {@link UserManager#createUser(NewUserRequest)} API.
 */
// TODO(b/209659998): [to-be-removed] fallback activity for supervised user creation.
public class AddSupervisedUserActivity extends Activity {

    private UserManager mUserManager;
    private ActivityManager mActivityManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mUserManager = getSystemService(UserManager.class);
        mActivityManager = getSystemService(ActivityManager.class);
        setContentView(R.layout.add_supervised_user);
        findViewById(R.id.createSupervisedUser).setOnClickListener(v -> createUser());
    }

    private void createUserAsync(final NewUserRequest request,
            final Consumer<NewUserResponse> onResponse) {
        Objects.requireNonNull(onResponse);

        final Handler mMainThread = new Handler(Looper.getMainLooper());
        Executors.newSingleThreadExecutor().execute(() -> {
            final NewUserResponse response = mUserManager.createUser(request);
            mMainThread.post(() -> onResponse.accept(response));
        });
    }

    private void createUser() {
        final NewUserRequest request = new NewUserRequest.Builder().build();

        final AlertDialog pleaseWaitDialog = new AlertDialog.Builder(this)
                .setMessage(getString(R.string.creating_new_user_dialog_message))
                .setCancelable(false)
                .create();

        pleaseWaitDialog.show();
        createUserAsync(request, response -> {
            pleaseWaitDialog.dismiss();

            if (response.isSuccessful()) {
                mActivityManager.switchUser(response.getUser());
                finish();
            } else {
                new AlertDialog.Builder(this)
                        .setTitle(getString(R.string.add_user_failed))
                        .setMessage(UserManager.UserOperationResult.class.getName()
                                + " = " + response.getOperationResult())
                        .setNeutralButton(getString(R.string.okay), null)
                        .show();
            }
        });
    }
}
