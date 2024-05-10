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

package com.android.settings.localepicker;

import static com.android.settings.localepicker.AppLocalePickerActivity.EXTRA_APP_LOCALE;
import static com.android.settings.localepicker.AppLocalePickerActivity.EXTRA_NOTIFICATION_ID;
import static com.android.settings.localepicker.LocaleListEditor.EXTRA_SYSTEM_LOCALE_DIALOG_TYPE;
import static com.android.settings.localepicker.LocaleListEditor.LOCALE_SUGGESTION;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AppCompatActivity;

/**
 * An Activity that launches the system locale settings page.
 */
public class NotificationActionActivity extends AppCompatActivity {
    private static final String TAG = "NotificationActionActivity";
    private static final int INVALID_NOTIFICATION_ID = -1;
    private final ActivityResultLauncher<Intent> mStartForResult =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                    });

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        int notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, INVALID_NOTIFICATION_ID);
        String appLocale = intent.getStringExtra(EXTRA_APP_LOCALE);
        if (TextUtils.isEmpty(appLocale) || notificationId == INVALID_NOTIFICATION_ID) {
            finish();
            return;
        }
        int savedNotificationID = getNotificationController(this).getNotificationId(appLocale);
        if (savedNotificationID == notificationId) {
            Intent actionIntent = new Intent(Settings.ACTION_LOCALE_SETTINGS);
            actionIntent.putExtra(EXTRA_SYSTEM_LOCALE_DIALOG_TYPE, LOCALE_SUGGESTION);
            actionIntent.putExtra(EXTRA_APP_LOCALE, appLocale);
            actionIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            getLauncher().launch(actionIntent);
            finish();
            return;
        }
    }

    @VisibleForTesting
    protected NotificationController getNotificationController(Context context) {
        return NotificationController.getInstance(context);
    }

    @VisibleForTesting
    protected ActivityResultLauncher<Intent> getLauncher() {
        return mStartForResult;
    }
}
