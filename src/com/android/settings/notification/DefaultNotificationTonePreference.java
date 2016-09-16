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

package com.android.settings.notification;


import com.android.settings.DefaultRingtonePreference;
import com.android.settings.Utils;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.os.AsyncTask;
import android.os.UserHandle;
import android.os.UserManager;
import android.net.Uri;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.AttributeSet;

import static android.content.ContentProvider.getUriWithoutUserId;

public class DefaultNotificationTonePreference extends DefaultRingtonePreference {
    private Uri mRingtone;

    public DefaultNotificationTonePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected Uri onRestoreRingtone() {
        return mRingtone;
    }

    @Override
    public void onPrepareRingtonePickerIntent(Intent ringtonePickerIntent) {
        super.onPrepareRingtonePickerIntent(ringtonePickerIntent);
        ringtonePickerIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                mRingtone);
    }

    public void setRingtone(Uri ringtone) {
        mRingtone = ringtone;
        updateRingtoneName(mRingtone);
    }

    private void updateRingtoneName(final Uri uri) {
        AsyncTask ringtoneNameTask = new AsyncTask<Object, Void, CharSequence>() {
            @Override
            protected CharSequence doInBackground(Object... params) {
                return Ringtone.getTitle(mUserContext, uri, false /* followSettingsUri */,
                        true /* allowRemote */);
            }

            @Override
            protected void onPostExecute(CharSequence name) {
                setSummary(name);
            }
        };
        ringtoneNameTask.execute();
    }
}