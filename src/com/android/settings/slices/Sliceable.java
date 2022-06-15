/*
 * Copyright (C) 2019 The Android Open Source Project
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
import android.content.IntentFilter;
import android.net.Uri;
import android.widget.Toast;

import androidx.slice.Slice;

import com.android.settings.R;

/**
 * A collection of API making a PreferenceController "sliceable"
 */
public interface Sliceable {
    /**
     * @return an {@link IntentFilter} that includes all broadcasts which can affect the state of
     * this Setting.
     */
    default IntentFilter getIntentFilter() {
        return null;
    }

    /**
     * Determines if the controller should be used as a Slice.
     * <p>
     * Important criteria for a Slice are:
     * - Must be secure
     * - Must not be a privacy leak
     * - Must be understandable as a stand-alone Setting.
     * <p>
     * This does not guarantee the setting is available.
     *
     * @return {@code true} if the controller should be used as a Slice.
     */
    default boolean isSliceable() {
        return false;
    }

    /**
     * Determines if the {@link Slice} should be public to other apps.
     * This does not guarantee the setting is available.
     *
     * @return {@code true} if the controller should be used as a Slice, and is
     * publicly visible to other apps.
     */
    default boolean isPublicSlice() {
        return false;
    }

    /**
     * Returns uri for this slice (if it's a slice).
     */
    default Uri getSliceUri() {
        return null;
    }

    /**
     * @return {@code true} if the setting update asynchronously.
     * <p>
     * For example, a Wifi controller would return true, because it needs to update the radio
     * and wait for it to turn on.
     */
    default boolean hasAsyncUpdate() {
        return false;
    }

    /**
     * Copy the key slice information to the clipboard.
     * It is highly recommended to show the toast to notify users when implemented this function.
     */
    default void copy() {
    }

    /**
     * Whether or not it's a copyable slice.
     */
    default boolean isCopyableSlice() {
        return false;
    }

    /**
     * Whether or not summary comes from something dynamic (ie, not hardcoded in xml)
     */
    default boolean useDynamicSliceSummary() {
        return false;
    }

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

    /**
     * Settings Slices which require background work, such as updating lists should implement a
     * {@link SliceBackgroundWorker} and return it here. An example of background work is updating
     * a list of Wifi networks available in the area.
     *
     * @return a {@link Class<? extends SliceBackgroundWorker>} to perform background work for the
     * slice.
     */
    default Class<? extends SliceBackgroundWorker> getBackgroundWorkerClass() {
        return null;
    }
}
