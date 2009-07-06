/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.settings.vpn;

import com.android.settings.R;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

class Util {

    static void showShortToastMessage(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    static void showShortToastMessage(Context context, int messageId) {
        Toast.makeText(context, messageId, Toast.LENGTH_SHORT).show();
    }

    static void showLongToastMessage(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }

    static void showLongToastMessage(Context context, int messageId) {
        Toast.makeText(context, messageId, Toast.LENGTH_LONG).show();
    }

    static void showErrorMessage(Context c, String message) {
        createErrorDialog(c, message, null).show();
    }

    static void showErrorMessage(Context c, String message,
            DialogInterface.OnClickListener listener) {
        createErrorDialog(c, message, listener).show();
    }

    static void deleteFile(String path) {
        deleteFile(new File(path));
    }

    static void deleteFile(String path, boolean toDeleteSelf) {
        deleteFile(new File(path), toDeleteSelf);
    }

    static void deleteFile(File f) {
        deleteFile(f, true);
    }

    static void deleteFile(File f, boolean toDeleteSelf) {
        if (f.isDirectory()) {
            for (File child : f.listFiles()) deleteFile(child, true);
        }
        if (toDeleteSelf) f.delete();
    }

    static boolean isFileOrEmptyDirectory(String path) {
        File f = new File(path);
        if (!f.isDirectory()) return true;

        String[] list = f.list();
        return ((list == null) || (list.length == 0));
    }

    static boolean copyFiles(String sourcePath , String targetPath)
            throws IOException {
        return copyFiles(new File(sourcePath), new File(targetPath));
    }

    // returns false if sourceLocation is the same as the targetLocation
    static boolean copyFiles(File sourceLocation , File targetLocation)
            throws IOException {
        if (sourceLocation.equals(targetLocation)) return false;

        if (sourceLocation.isDirectory()) {
            if (!targetLocation.exists()) {
                targetLocation.mkdir();
            }
            String[] children = sourceLocation.list();
            for (int i=0; i<children.length; i++) {
                copyFiles(new File(sourceLocation, children[i]),
                        new File(targetLocation, children[i]));
            }
        } else if (sourceLocation.exists()) {
            InputStream in = new FileInputStream(sourceLocation);
            OutputStream out = new FileOutputStream(targetLocation);

            // Copy the bits from instream to outstream
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
        }
        return true;
    }

    private static AlertDialog createErrorDialog(Context c, String message,
            DialogInterface.OnClickListener okListener) {
        AlertDialog.Builder b = new AlertDialog.Builder(c)
                .setTitle(android.R.string.dialog_alert_title)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setMessage(message);
        if (okListener != null) {
            b.setPositiveButton(R.string.vpn_back_button, okListener);
        } else {
            b.setPositiveButton(android.R.string.ok, null);
        }
        return b.create();
    }

    private Util() {
    }
}
