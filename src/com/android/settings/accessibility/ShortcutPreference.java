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
import android.widget.LinearLayout;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;

/**
 * Preference that can enable accessibility shortcut and let users choose which shortcut type they
 * prefer to use.
 */
public class ShortcutPreference extends Preference {

    /**
     * Interface definition for a callback to be invoked when the checkbox or settings has been
     * clicked.
     */
    public interface OnClickListener {
        /**
         * Called when the checkbox in ShortcutPreference has been clicked.
         *
         * @param preference The clicked preference
         */
        void onCheckboxClicked(ShortcutPreference preference);
        /**
         * Called when the settings view has been clicked.
         *
         * @param preference The clicked preference
         */
        void onSettingsClicked(ShortcutPreference preference);
    }
    private OnClickListener mListener = null;

    private int mSettingsVisibility = View.VISIBLE;
    private boolean mChecked = false;

    ShortcutPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        final CheckBox checkBox = holder.itemView.findViewById(R.id.checkbox);
        final LinearLayout mainFrame = holder.itemView.findViewById(R.id.main_frame);
        if (mainFrame != null && checkBox != null) {
            mainFrame.setOnClickListener(view -> callOnCheckboxClicked());
            checkBox.setChecked(mChecked);
        }

        final View settings = holder.itemView.findViewById(android.R.id.widget_frame);
        final View divider = holder.itemView.findViewById(R.id.divider);
        if (settings != null && divider != null) {
            settings.setOnClickListener(view -> callOnSettingsClicked());
            settings.setVisibility(mSettingsVisibility);
            divider.setVisibility(mSettingsVisibility);
        }
    }

    /**
     * Set the shortcut checkbox according to checked value.
     *
     * @param checked the state value of shortcut checkbox
     */
    public void setChecked(boolean checked) {
        if (mChecked != checked) {
            mChecked = checked;
            notifyChanged();
        }
    }

    /**
     * Get the checked value of shortcut checkbox.
     *
     * @return the checked value of shortcut checkbox
     */
    public boolean getChecked() {
        return mChecked;
    }



    /**
     * Sets the visibility state of Settings view.
     *
     * @param visibility one of {@link View#VISIBLE}, {@link View#INVISIBLE}, or {@link View#GONE}.
     */
    public void setSettingsVisibility(@View.Visibility int visibility) {
        if (mSettingsVisibility != visibility) {
            mSettingsVisibility = visibility;
            notifyChanged();
        }
    }

    /**
     * Sets the callback to be invoked when this preference is clicked by the user.
     *
     * @param listener the callback to be invoked
     */
    public void setOnClickListener(OnClickListener listener) {
        mListener = listener;
    }

    private void init() {
        setLayoutResource(R.layout.accessibility_shortcut_secondary_action);
        setWidgetLayoutResource(R.layout.preference_widget_settings);
        setIconSpaceReserved(false);
    }

    private void callOnSettingsClicked() {
        if (mListener != null) {
            mListener.onSettingsClicked(this);
        }
    }

    private void callOnCheckboxClicked() {
        setChecked(!mChecked);
        if (mListener != null) {
            mListener.onCheckboxClicked(this);
        }
    }
}
