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

package com.android.settings.slices;

import static android.content.Context.CLIPBOARD_SERVICE;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.widget.Toast;

import com.android.settings.R;

/**
 * Provide the copy ability for preference controller to copy the data to the clipboard.
 */
public interface Copyable {
    /**
     * Copy the key slice information to the clipboard.
     * It is highly recommended to show the toast to notify users when implemented this function.
     */
    void copy();

    /**
     * Set the copy content to the clipboard and show the toast.
     */
    static void setCopyContent(Context context, CharSequence copyContent,
            CharSequence messageTitle) {
        final ClipboardManager clipboard = (ClipboardManager) context.getSystemService(
                CLIPBOARD_SERVICE);
        final ClipData clip = ClipData.newPlainText("text", copyContent);
        clipboard.setPrimaryClip(clip);

        final String toast = context.getString(R.string.copyable_slice_toast, messageTitle);
        Toast.makeText(context, toast, Toast.LENGTH_SHORT).show();
    }
}
