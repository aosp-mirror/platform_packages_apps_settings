/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.widget;

import static com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

import androidx.preference.PreferenceViewHolder;
import androidx.preference.TwoStatePreference;

import com.android.settings.R;
import com.android.settings.widget.SettingsMainSwitchBar.OnBeforeCheckedChangeListener;
import com.android.settingslib.RestrictedPreferenceHelper;
import com.android.settingslib.core.instrumentation.SettingsJankMonitor;

import java.util.ArrayList;
import java.util.List;

/**
 * SettingsMainSwitchPreference is a Preference with a customized Switch.
 * This component is used as the main switch of the page
 * to enable or disable the preferences on the page.
 */
public class SettingsMainSwitchPreference extends TwoStatePreference implements
        OnCheckedChangeListener {

    private final List<OnBeforeCheckedChangeListener> mBeforeCheckedChangeListeners =
            new ArrayList<>();
    private final List<OnCheckedChangeListener> mSwitchChangeListeners = new ArrayList<>();

    private SettingsMainSwitchBar mMainSwitchBar;
    private EnforcedAdmin mEnforcedAdmin;
    private RestrictedPreferenceHelper mRestrictedHelper;

    public SettingsMainSwitchPreference(Context context) {
        super(context);
        init(context, null);
    }

    public SettingsMainSwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public SettingsMainSwitchPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    public SettingsMainSwitchPreference(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        holder.setDividerAllowedAbove(false);
        holder.setDividerAllowedBelow(false);

        if (mEnforcedAdmin == null && mRestrictedHelper != null) {
            mEnforcedAdmin = mRestrictedHelper.checkRestrictionEnforced();
        }
        mMainSwitchBar = (SettingsMainSwitchBar) holder.findViewById(R.id.main_switch_bar);
        initMainSwitchBar();
        if (isVisible()) {
            mMainSwitchBar.show();
            if (mMainSwitchBar.isChecked() != isChecked()) {
                setChecked(isChecked());
            }
            registerListenerToSwitchBar();
        } else {
            mMainSwitchBar.hide();
        }
    }

    private void init(Context context, AttributeSet attrs) {
        setLayoutResource(R.layout.preference_widget_main_switch);
        mSwitchChangeListeners.add(this);

        if (attrs != null) {
            mRestrictedHelper = new RestrictedPreferenceHelper(context, this, attrs);
        }
    }

    @Override
    public void setChecked(boolean checked) {
        super.setChecked(checked);
        if (mMainSwitchBar != null) {
            mMainSwitchBar.setChecked(checked);
        }
    }

    /**
     * Return the SettingsMainSwitchBar
     */
    public final SettingsMainSwitchBar getSwitchBar() {
        return mMainSwitchBar;
    }

    @Override
    public void setTitle(CharSequence title) {
        super.setTitle(title);
        if (mMainSwitchBar != null) {
            mMainSwitchBar.setTitle(title);
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        super.setChecked(isChecked);
        SettingsJankMonitor.detectToggleJank(getKey(), buttonView);
    }

    /**
     * Show the MainSwitchBar
     */
    public void show() {
        setVisible(true);
        if (mMainSwitchBar != null) {
            mMainSwitchBar.show();
        }
    }

    /**
     * Hide the MainSwitchBar
     */
    public void hide() {
        setVisible(false);
        if (mMainSwitchBar != null) {
            mMainSwitchBar.hide();
        }
    }

    /**
     * Returns if the MainSwitchBar is visible.
     */
    public boolean isShowing() {
        if (mMainSwitchBar != null) {
            return mMainSwitchBar.isShowing();
        }
        return false;
    }

    /**
     * Update the status of switch but doesn't notify the mOnBeforeListener.
     */
    public void setCheckedInternal(boolean checked) {
        super.setChecked(checked);
        if (mMainSwitchBar != null) {
            mMainSwitchBar.setCheckedInternal(checked);
        }
    }

    /**
     * Enable or disable the text and switch.
     */
    public void setSwitchBarEnabled(boolean enabled) {
        setEnabled(enabled);
        if (mMainSwitchBar != null) {
            mMainSwitchBar.setEnabled(enabled);
        }
    }

    /**
     * Set the OnBeforeCheckedChangeListener.
     */
    public void setOnBeforeCheckedChangeListener(OnBeforeCheckedChangeListener listener) {
        if (!mBeforeCheckedChangeListeners.contains(listener)) {
            mBeforeCheckedChangeListeners.add(listener);
        }
        if (mMainSwitchBar != null) {
            mMainSwitchBar.setOnBeforeCheckedChangeListener(listener);
        }
    }

    /**
     * Adds a listener for switch changes
     */
    public void addOnSwitchChangeListener(OnCheckedChangeListener listener) {
        if (!mSwitchChangeListeners.contains(listener)) {
            mSwitchChangeListeners.add(listener);
        }
        if (mMainSwitchBar != null) {
            mMainSwitchBar.addOnSwitchChangeListener(listener);
        }
    }

    /**
     * Remove a listener for switch changes
     */
    public void removeOnSwitchChangeListener(OnCheckedChangeListener listener) {
        mSwitchChangeListeners.remove(listener);
        if (mMainSwitchBar != null) {
            mMainSwitchBar.removeOnSwitchChangeListener(listener);
        }
    }

    /**
     * If admin is not null, disables the text and switch but keeps the view clickable.
     * Otherwise, calls setEnabled which will enables the entire view including
     * the text and switch.
     */
    public void setDisabledByAdmin(EnforcedAdmin admin) {
        mEnforcedAdmin = admin;
        if (mMainSwitchBar != null) {
            mMainSwitchBar.setDisabledByAdmin(mEnforcedAdmin);
        }
    }

    private void initMainSwitchBar() {
        if (mMainSwitchBar != null) {
            mMainSwitchBar.setTitle(getTitle());
            mMainSwitchBar.setDisabledByAdmin(mEnforcedAdmin);
        }
    }

    private void registerListenerToSwitchBar() {
        for (OnBeforeCheckedChangeListener listener : mBeforeCheckedChangeListeners) {
            mMainSwitchBar.setOnBeforeCheckedChangeListener(listener);
        }
        for (OnCheckedChangeListener listener : mSwitchChangeListeners) {
            mMainSwitchBar.addOnSwitchChangeListener(listener);
        }
    }
}
