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
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.text.format.Formatter;
import android.widget.TextView;
import com.android.settings.deletionhelper.DeletionType;

/**
 * Preference to handle the deletion of photos and videos in the Deletion Helper.
 */
public class PhotosDeletionPreference extends CheckBoxPreference implements
        DeletionType.FreeableChangedListener, OnPreferenceChangeListener {
    // TODO(b/28560570): Remove this dummy value.
    private static final int FAKE_DAYS_TO_KEEP = 30;
    private DeletionType.FreeableChangedListener mListener;
    private boolean mChecked;
    private long mFreeableBytes;
    private int mFreeableItems;
    private DeletionType mDeletionService;

    public PhotosDeletionPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setIcon(getIcon(context));
        updatePreferenceText();
        setOnPreferenceChangeListener(this);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        final TextView titleView = (TextView) holder.findViewById(android.R.id.title);
        if (titleView != null) {
            titleView.setTextColor(getTintColor(getContext()));
        }
    }

    /**
     * Get the tint color for the preference's icon and text.
     * @param context UI context to get the theme.
     * @return The tint color.
     */
    public int getTintColor(Context context) {
        TypedValue value = new TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.colorAccent, value, true);
        return context.getColor(value.resourceId);
    }

    /**
     * Updates the title and summary of the preference with fresh information.
     */
    public void updatePreferenceText() {
        Context context = getContext();
        setTitle(context.getString(R.string.deletion_helper_photos_title,
                mFreeableItems));
        setSummary(context.getString(R.string.deletion_helper_photos_summary,
                Formatter.formatFileSize(context, mFreeableBytes), FAKE_DAYS_TO_KEEP));
    }

    /**
     * Returns the number of bytes which can be cleared by the deletion service.
     * @return The number of bytes.
     */
    public long getFreeableBytes() {
        return mChecked ? mFreeableBytes : 0;
    }

    /**
     * Register a listener to be called back on when the freeable bytes have changed.
     * @param listener The callback listener.
     */
    public void registerFreeableChangedListener(DeletionType.FreeableChangedListener listener) {
        mListener = listener;
    }

    /**
     * Registers a deletion service to update the preference's information.
     * @param deletionService A photo/video deletion service.
     */
    public void registerDeletionService(DeletionType deletionService) {
        mDeletionService = deletionService;
        if (mDeletionService != null) {
            mDeletionService.registerFreeableChangedListener(this);
        }
    }

    @Override
    public void onFreeableChanged(int numItems, long freeableBytes) {
        mFreeableItems = numItems;
        mFreeableBytes = freeableBytes;
        updatePreferenceText();
        maybeUpdateListener();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        mChecked = (boolean) newValue;
        maybeUpdateListener();
        return true;
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

    private void maybeUpdateListener() {
        if (mListener != null) {
            mListener.onFreeableChanged(mFreeableItems, getFreeableBytes());
        }
    }
}
