/*
 * Copyright (C) 2020 The Android Open Source Project
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

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Switch;

import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.widget.MainSwitchBar;

/**
 * A {@link MainSwitchBar} with a customized Switch and provides the metrics feature.
 */
public class SettingsMainSwitchBar extends MainSwitchBar {

    /**
     * Called before the checked state of the Switch has changed.
     */
    public interface OnBeforeCheckedChangeListener {

        /**
         * @param switchView The Switch view whose state has changed.
         * @param isChecked  The new checked state of switchView.
         */
        boolean onBeforeCheckedChanged(Switch switchView, boolean isChecked);
    }

    private final MetricsFeatureProvider mMetricsFeatureProvider;
    private OnBeforeCheckedChangeListener mOnBeforeListener;

    private Switch mSwitch;
    private String mMetricsTag;

    public SettingsMainSwitchBar(Context context) {
        this(context, null);
    }

    public SettingsMainSwitchBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SettingsMainSwitchBar(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public SettingsMainSwitchBar(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        mMetricsFeatureProvider = FeatureFactory.getFactory(context).getMetricsFeatureProvider();

        mSwitch = (Switch) findViewById(android.R.id.switch_widget);

        addOnSwitchChangeListener((switchView, isChecked) -> logMetrics(isChecked));
    }

    @Override
    protected void onRestrictedIconClick() {
        mMetricsFeatureProvider.action(
                SettingsEnums.PAGE_UNKNOWN,
                SettingsEnums.ACTION_SETTINGS_PREFERENCE_CHANGE,
                SettingsEnums.PAGE_UNKNOWN,
                mMetricsTag + "/switch_bar|restricted",
                1);
    }

    @Override
    public void setChecked(boolean checked) {
        if (mOnBeforeListener != null
                && mOnBeforeListener.onBeforeCheckedChanged(mSwitch, checked)) {
            return;
        }
        super.setChecked(checked);
    }

    /**
     * Update the status of switch but doesn't notify the mOnBeforeListener.
     */
    public void setCheckedInternal(boolean checked) {
        super.setChecked(checked);
    }

    /**
     * Set the OnBeforeCheckedChangeListener.
     */
    public void setOnBeforeCheckedChangeListener(OnBeforeCheckedChangeListener listener) {
        mOnBeforeListener = listener;
    }

    /**
     * Returns if this view is visible.
     */
    public boolean isShowing() {
        return (getVisibility() == View.VISIBLE);
    }

    /**
     * Set the metrics tag.
     */
    public void setMetricsTag(String tag) {
        mMetricsTag = tag;
    }

    private void logMetrics(boolean isChecked) {
        mMetricsFeatureProvider.action(
                SettingsEnums.PAGE_UNKNOWN,
                SettingsEnums.ACTION_SETTINGS_PREFERENCE_CHANGE,
                SettingsEnums.PAGE_UNKNOWN,
                mMetricsTag + "/switch_bar",
                isChecked ? 1 : 0);
    }
}
