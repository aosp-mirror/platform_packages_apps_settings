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

package com.android.settings.deletionhelper;

import android.content.Context;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.text.format.Formatter;
import com.android.settings.DeletionPreference;
import com.android.settings.R;

/**
 * Preference to handle the deletion of photos and videos in the Deletion Helper.
 */
public class DownloadsDeletionPreference extends DeletionPreference {
    public DownloadsDeletionPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        updatePreferenceText(0, 0, Long.MAX_VALUE);
    }

    @Override
    public void onFreeableChanged(int numItems, long freeableBytes) {
        super.onFreeableChanged(numItems, freeableBytes);
        DownloadsDeletionType deletionService = (DownloadsDeletionType) getDeletionService();
        updatePreferenceText(numItems, freeableBytes, deletionService.getMostRecentLastModified());
    }

    private void updatePreferenceText(int items, long bytes, long mostRecent) {
        Context context = getContext();
        setTitle(context.getString(R.string.deletion_helper_downloads_title,
                items));
        // If there are no files to clear, show the empty text instead.
        if (mostRecent < Long.MAX_VALUE) {
            setSummary(context.getString(R.string.deletion_helper_downloads_summary,
                    Formatter.formatFileSize(context, bytes),
                    DateUtils.getRelativeTimeSpanString(mostRecent,
                            System.currentTimeMillis(),
                            DateUtils.DAY_IN_MILLIS,
                            DateUtils.FORMAT_ABBREV_RELATIVE)));
        } else {
            setSummary(context.getString(R.string.deletion_helper_downloads_summary_empty,
                    Formatter.formatFileSize(context, bytes)));
        }
    }

}
