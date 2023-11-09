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

import static com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.RestrictedLockUtils;
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
         * @param isChecked The new checked state of switchView.
         */
        boolean onBeforeCheckedChanged(boolean isChecked);
    }

    private EnforcedAdmin mEnforcedAdmin;
    private boolean mDisabledByAdmin;

    private final MetricsFeatureProvider mMetricsFeatureProvider;
    private OnBeforeCheckedChangeListener mOnBeforeListener;

    private int mMetricsCategory;

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
        mMetricsFeatureProvider = FeatureFactory.getFeatureFactory().getMetricsFeatureProvider();

        addOnSwitchChangeListener((switchView, isChecked) -> logMetrics(isChecked));
    }

    /**
     * If admin is not null, disables the text and switch but keeps the view clickable (unless the
     * switch is disabled for other reasons). Otherwise, calls setEnabled.
     */
    public void setDisabledByAdmin(EnforcedAdmin admin) {
        mEnforcedAdmin = admin;
        if (admin != null) {
            super.setEnabled(true);
            mDisabledByAdmin = true;
            mTextView.setEnabled(false);
            mSwitch.setEnabled(false);
        } else {
            mDisabledByAdmin = false;
            mSwitch.setVisibility(View.VISIBLE);
            setEnabled(isEnabled());
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (enabled && mDisabledByAdmin) {
            setDisabledByAdmin(null);
            return;
        }
        super.setEnabled(enabled);
    }

    /**
     * Called by the restricted icon clicked.
     */

    @Override
    public boolean performClick() {
        if (mDisabledByAdmin) {
            performRestrictedClick();
            return true;
        }

        return mSwitch.performClick();
    }

    @Override
    public void setChecked(boolean checked) {
        if (mOnBeforeListener != null
                && mOnBeforeListener.onBeforeCheckedChanged(checked)) {
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
     * Set the metrics tag.
     */
    public void setMetricsCategory(int category) {
        mMetricsCategory = category;
    }

    private void logMetrics(boolean isChecked) {
        mMetricsFeatureProvider.changed(mMetricsCategory, "switch_bar", isChecked ? 1 : 0);
    }

    private void performRestrictedClick() {
        RestrictedLockUtils.sendShowAdminSupportDetailsIntent(getContext(), mEnforcedAdmin);
        mMetricsFeatureProvider.clicked(mMetricsCategory, "switch_bar|restricted");
    }
}
