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

package com.android.settings;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.text.format.Formatter;

/**
 * Preference to handle the deletion of photos and videos in the Deletion Helper.
 */
public class PhotosDeletionPreference extends DeletionPreference {
    // TODO(b/28560570): Remove this dummy value.
    private static final int FAKE_DAYS_TO_KEEP = 30;

    public PhotosDeletionPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setIcon(getIcon(context));
        updatePreferenceText(0, 0);
    }

    /**
     * Updates the title and summary of the preference with fresh information.
     */
    public void updatePreferenceText(int items, long bytes) {
        Context context = getContext();
        setTitle(context.getString(R.string.deletion_helper_photos_title, items));
        setSummary(context.getString(R.string.deletion_helper_photos_summary,
                Formatter.formatFileSize(context, bytes), FAKE_DAYS_TO_KEEP));
    }

    @Override
    public void onFreeableChanged(int items, long bytes) {
        super.onFreeableChanged(items, bytes);
        updatePreferenceText(items, bytes);
    }

    private Drawable getIcon(Context context) {
        final Drawable iconDrawable;
        try {
            Resources resources = context.getResources();
            final int resId = resources.getIdentifier("ic_photos_black_24", "drawable",
                    context.getPackageName());
            iconDrawable = context.getDrawable(resId);
        } catch (Resources.NotFoundException e) {
            return null;
        }
        return iconDrawable;
    }
}
