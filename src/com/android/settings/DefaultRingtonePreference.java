/*
 * Copyright (C) 2007 The Android Open Source Project
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


import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.Log;

public class DefaultRingtonePreference extends RingtonePreference {
    private static final String TAG = "DefaultRingtonePreference";

    private int mUserId = UserHandle.USER_CURRENT;
    protected Context mUserContext;

    public DefaultRingtonePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mUserContext = getContext();
    }

    public void setUserId(int userId) {
        mUserId = userId;
        mUserContext = Utils.createPackageContextAsUser(getContext(), mUserId);
    }

    @Override
    public void performClick() {
        if (mUserId != UserHandle.USER_CURRENT) {
            if (Utils.confirmWorkProfileCredentialsIfNecessary(getContext(), mUserId) ||
                    Utils.startQuietModeDialogIfNecessary(getContext(),
                            UserManager.get(getContext()), mUserId)) {
                return;
            }
        }
        super.performClick();
    }

    @Override
    public void onPrepareRingtonePickerIntent(Intent ringtonePickerIntent) {
        super.onPrepareRingtonePickerIntent(ringtonePickerIntent);

        /*
         * Since this preference is for choosing the default ringtone, it
         * doesn't make sense to show a 'Default' item.
         */
        ringtonePickerIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, false);
        if (mUserId != UserHandle.USER_CURRENT) {
            ringtonePickerIntent.putExtra(Intent.EXTRA_USER_ID, mUserId);
        }
    }

    @Override
    protected void onSaveRingtone(Uri ringtoneUri) {
        RingtoneManager.setActualDefaultRingtoneUri(mUserContext, getRingtoneType(), ringtoneUri);
    }

    @Override
    protected Uri onRestoreRingtone() {
        return RingtoneManager.getActualDefaultRingtoneUri(mUserContext, getRingtoneType());
    }

}
