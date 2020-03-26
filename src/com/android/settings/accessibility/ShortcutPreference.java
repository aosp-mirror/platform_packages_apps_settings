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
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Switch;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;

/**
 * Preference that can enable accessibility shortcut and let users choose which shortcut type they
 * prefer to use.
 */
public class ShortcutPreference extends Preference {

    /**
     * Interface definition for a callback to be invoked when the toggle or settings has been
     * clicked.
     */
    public interface OnClickCallback {
        /**
         * Called when the settings view has been clicked.
         *
         * @param preference The clicked preference
         */
        void onSettingsClicked(ShortcutPreference preference);

        /**
         * Called when the toggle in ShortcutPreference has been clicked.
         *
         * @param preference The clicked preference
         */
        void onToggleClicked(ShortcutPreference preference);
    }

    private OnClickCallback mClickCallback = null;
    private boolean mChecked = false;
    private boolean mSettingsEditable = true;

    ShortcutPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.accessibility_shortcut_secondary_action);
        setWidgetLayoutResource(R.layout.preference_widget_master_switch);
        setIconSpaceReserved(true);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        final TypedValue outValue = new TypedValue();
        getContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackground,
                outValue, true);

        final LinearLayout mainFrame = holder.itemView.findViewById(R.id.main_frame);
        if (mainFrame != null) {
            mainFrame.setOnClickListener(view -> callOnSettingsClicked());
            mainFrame.setClickable(mSettingsEditable);
            mainFrame.setFocusable(mSettingsEditable);
            mainFrame.setBackgroundResource(
                    mSettingsEditable ? outValue.resourceId : /* Remove background */ 0);
        }

        Switch switchWidget = holder.itemView.findViewById(R.id.switchWidget);
        if (switchWidget != null) {
            // Consumes move events to ignore drag actions.
            switchWidget.setOnTouchListener((v, event) -> {
                return event.getActionMasked() == MotionEvent.ACTION_MOVE;
            });
            switchWidget.setContentDescription(
                    getContext().getText(R.string.accessibility_shortcut_settings));
            switchWidget.setChecked(mChecked);
            switchWidget.setOnClickListener(view -> callOnToggleClicked());
            switchWidget.setClickable(mSettingsEditable);
            switchWidget.setFocusable(mSettingsEditable);
            switchWidget.setBackgroundResource(
                    mSettingsEditable ? outValue.resourceId : /* Remove background */ 0);
        }

        final View divider = holder.itemView.findViewById(R.id.divider);
        if (divider != null) {
            divider.setVisibility(mSettingsEditable ? View.VISIBLE : View.GONE);
        }

        holder.itemView.setOnClickListener(view -> callOnToggleClicked());
        holder.itemView.setClickable(!mSettingsEditable);
        holder.itemView.setFocusable(!mSettingsEditable);
    }

    /**
     * Sets the shortcut toggle according to checked value.
     *
     * @param checked the state value of shortcut toggle
     */
    public void setChecked(boolean checked) {
        if (mChecked != checked) {
            mChecked = checked;
            notifyChanged();
        }
    }

    /**
     * Gets the checked value of shortcut toggle.
     *
     * @return the checked value of shortcut toggle
     */
    public boolean isChecked() {
        return mChecked;
    }

    /**
     * Sets the editable state of Settings view. If the view cannot edited, it makes the settings
     * and toggle be not touchable. The main ui handles touch event directly by {@link #onClick}.
     */
    public void setSettingsEditable(boolean enabled) {
        if (mSettingsEditable != enabled) {
            mSettingsEditable = enabled;
            notifyChanged();
        }
    }

    public boolean isSettingsEditable() {
        return mSettingsEditable;
    }

    /**
     * Sets the callback to be invoked when this preference is clicked by the user.
     *
     * @param callback the callback to be invoked
     */
    public void setOnClickCallback(OnClickCallback callback) {
        mClickCallback = callback;
    }

    private void callOnSettingsClicked() {
        if (mClickCallback != null) {
            mClickCallback.onSettingsClicked(this);
        }
    }

    private void callOnToggleClicked() {
        setChecked(!mChecked);
        if (mClickCallback != null) {
            mClickCallback.onToggleClicked(this);
        }
    }
}
