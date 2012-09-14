/*
 * Copyright (C) 2012 The Android Open Source Project
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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.UserInfo;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Profile;
import android.util.Log;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Watches for changes to Me Profile in Contacts and writes the photo to the User Manager.
 */
public class ProfileUpdateReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, Intent intent) {
        // Profile changed, lets get the photo and write to user manager
        new Thread() {
            public void run() {
                copyProfilePhoto(context, null);
            }
        }.start();
    }

    /* Used by UserSettings as well. Call this on a non-ui thread. */
    static boolean copyProfilePhoto(Context context, UserInfo user) {
        Uri contactUri = Profile.CONTENT_URI;

        InputStream avatarDataStream = Contacts.openContactPhotoInputStream(
                    context.getContentResolver(),
                    contactUri, true);
        // If there's no profile photo, assign a default avatar
        if (avatarDataStream == null) {
            return false;
        }
        int userId = user != null ? user.id : UserHandle.myUserId();
        UserManager um = (UserManager) context.getSystemService(Context.USER_SERVICE);
        ParcelFileDescriptor fd = um.setUserIcon(userId);
        FileOutputStream os = new ParcelFileDescriptor.AutoCloseOutputStream(fd);
        byte[] buffer = new byte[4096];
        int readSize;
        try {
            while ((readSize = avatarDataStream.read(buffer)) > 0) {
                os.write(buffer, 0, readSize);
            }
            return true;
        } catch (IOException ioe) {
            Log.e("copyProfilePhoto", "Error copying profile photo " + ioe);
        } finally {
            try {
                os.close();
                avatarDataStream.close();
            } catch (IOException ioe) { }
        }
        return false;
    }
}
