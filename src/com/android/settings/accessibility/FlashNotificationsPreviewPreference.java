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

package com.android.settings.accessibility;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;
import com.android.settingslib.Utils;

/**
 * Preference for Flash notifications preview.
 */
public class FlashNotificationsPreviewPreference extends Preference {
    private Drawable mBackgroundEnabled;
    private Drawable mBackgroundDisabled;
    @ColorInt
    private int mTextColorDisabled;

    public FlashNotificationsPreviewPreference(Context context) {
        super(context);
        init();
    }

    public FlashNotificationsPreviewPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public FlashNotificationsPreviewPreference(Context context, AttributeSet attrs,
            int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public FlashNotificationsPreviewPreference(Context context, AttributeSet attrs,
            int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        setLayoutResource(R.layout.flash_notification_preview_preference);
        mBackgroundEnabled = getContext().getDrawable(
                com.android.settingslib.widget.mainswitch.R.drawable.settingslib_switch_bar_bg_on);
        mBackgroundDisabled = getContext().getDrawable(R.drawable.switch_bar_bg_disabled);
        mTextColorDisabled = Utils.getColorAttrDefaultColor(getContext(),
                android.R.attr.textColorPrimary);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        final boolean enabled = isEnabled();
        final View frame = holder.findViewById(R.id.frame);
        if (frame != null) {
            frame.setBackground(enabled ? mBackgroundEnabled : mBackgroundDisabled);
        }
        final TextView title = (TextView) holder.findViewById(android.R.id.title);
        if (title != null) {
            @ColorInt final int textColorEnabled = title.getCurrentTextColor();
            title.setAlpha(enabled ? 1f : 0.38f);
            title.setTextColor(enabled ? textColorEnabled : mTextColorDisabled);
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        notifyChanged();
    }
}
