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
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;

import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.Checkable;
import android.widget.TextView;

import java.util.LinkedHashMap;

/**
 * CollapsibleCheckboxPreferenceGroup is a preference group that can be expanded or collapsed and
 * also has a checkbox.
 */
public class CollapsibleCheckboxPreferenceGroup extends PreferenceGroup implements
        View.OnClickListener {
    private boolean mCollapsed;
    private boolean mChecked;

    public CollapsibleCheckboxPreferenceGroup(Context context) {
        this(context, null);
    }

    public CollapsibleCheckboxPreferenceGroup(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWidgetLayoutResource(com.android.settings.R.layout.preference_widget_checkbox);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        View checkbox = holder.findViewById(com.android.internal.R.id.checkbox);
        if (checkbox != null && checkbox instanceof Checkable) {
            ((Checkable) checkbox).setChecked(mChecked);
            checkbox.setClickable(true);
            checkbox.setFocusable(true);
            checkbox.setOnClickListener(this);
        }

        final TextView titleView = (TextView) holder.findViewById(android.R.id.title);
        if (titleView != null) {
            Context context = getContext();
            TypedValue value = new TypedValue();
            context.getTheme().resolveAttribute(android.R.attr.colorAccent, value, true);
            titleView.setTextColor(context.getColor(value.resourceId));
        }
    }

    @Override
    public boolean addPreference(Preference p) {
        super.addPreference(p);
        p.setVisible(!isCollapsed());
        return true;
    }

    // The preference click handler.
    @Override
    protected void onClick() {
        super.onClick();
        setCollapse(!isCollapsed());
    }

    // The checkbox view click handler.
    @Override
    public void onClick(View v) {
        setChecked(!isChecked());
    }

    /**
     * Return if the view is collapsed.
     */
    public boolean isCollapsed() {
        return mCollapsed;
    }

    /**
     * Returns the checked state of the preference.
     */
    public boolean isChecked() {
        return mChecked;
    }

    /**
     * Sets the checked state and notifies listeners of the state change.
     */
    public void setChecked(boolean checked) {
        if (mChecked != checked) {
            mChecked = checked;

            callChangeListener(checked);
            notifyDependencyChange(shouldDisableDependents());
            notifyChanged();
        }
    }

    private void setCollapse(boolean isCollapsed) {
        if (mCollapsed == isCollapsed) {
            return;
        }

        mCollapsed = isCollapsed;
        if (isCollapsed) {
            hideDropdownPreferences();
        } else {
            showDropdownPreferences();
        }
    }

    private void showDropdownPreferences() {
        setAllPreferencesVisibility(true);
        setIcon(R.drawable.ic_keyboard_arrow_down_black_32);
    }

    private void hideDropdownPreferences() {
        setAllPreferencesVisibility(false);
        setIcon(R.drawable.ic_keyboard_arrow_up_black_32);
    }

    private void setAllPreferencesVisibility(boolean visible) {
        for (int i = 0; i < getPreferenceCount(); i++) {
            Preference p = getPreference(i);
            p.setVisible(visible);
        }
    }
}
