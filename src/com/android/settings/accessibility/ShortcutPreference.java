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

package com.android.settings.accessibility;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.CheckBox;

import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;

/**
 * Preference that can enable accessibility shortcut and let users choose which shortcut type they
 * prefer to use.
 */
public class ShortcutPreference extends Preference {

    private View.OnClickListener mCheckBoxListener;
    private View.OnClickListener mSettingButtonListener;

    private boolean mChecked = false;

    ShortcutPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        setLayoutResource(R.layout.accessibility_shortcut_secondary_action);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        holder.itemView.setClickable(false);

        final CheckBox checkBox = holder.itemView.findViewById(R.id.checkbox);
        checkBox.setOnClickListener(mCheckBoxListener);
        checkBox.setChecked(mChecked);
        final View settingButton = holder.itemView.findViewById(R.id.settings_button);
        settingButton.setOnClickListener(mSettingButtonListener);
    }

    /**
     * Set the shortcut checkbox according to checked value.
     *
     * @param checked the state value of shortcut checkbox.
     */
    public void setChecked(boolean checked) {
        if (checked != mChecked) {
            mChecked = checked;
            notifyChanged();
        }
    }

    /**
     * Set the given onClickListener to the SettingButtonListener.
     *
     * @param listener the given onClickListener.
     */
    public void setSettingButtonListener(@Nullable View.OnClickListener listener) {
        mSettingButtonListener = listener;
        notifyChanged();
    }

    /**
     * Returns the callback to be invoked when the setting button is clicked.
     *
     * @return The callback to be invoked
     */
    public View.OnClickListener getSettingButtonListener() {
        return mSettingButtonListener;
    }

    /**
     * Set the given onClickListener to the CheckBoxListener.
     *
     * @param listener the given onClickListener.
     */
    public void setCheckBoxListener(@Nullable View.OnClickListener listener) {
        mCheckBoxListener = listener;
        notifyChanged();
    }

    /**
     * Returns the callback to be invoked when the checkbox is clicked.
     *
     * @return The callback to be invoked
     */
    public View.OnClickListener getCheckBoxListener() {
        return mCheckBoxListener;
    }
}
