/*
 * Copyright (C) 2017 The Android Open Source Project
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

import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;

/*
 * A controller class for general switch widget handling. We have different containers that provide
 * different forms of switch layout. Provide a centralized control for updating the switch widget.
 */
public abstract class SwitchWidgetController {

    protected OnSwitchChangeListener mListener;

    /**
     * Interface definition for a callback to be invoked when the switch has been toggled.
     */
    public interface OnSwitchChangeListener {
        /**
         * Called when the checked state of the Switch has changed.
         *
         * @param isChecked The new checked state of switchView.
         *
         * @return true to update the state of the switch with the new value.
         */
        boolean onSwitchToggled(boolean isChecked);
    }

    /**
     * Perform any view setup.
     */
    public void setupView() {
    }

    /**
     * Perform any view teardown.
     */
    public void teardownView() {
    }

    /**
     * Set the callback to be invoked when the switch is toggled by the user (but before the
     * internal state has been updated).
     *
     * @param listener the callback to be invoked
     */
    public void setListener(OnSwitchChangeListener listener) {
        mListener = listener;
    }

    /**
     * Update the preference title associated with the switch.
     *
     * @param isChecked whether the switch is currently checked
     */
    public abstract void updateTitle(boolean isChecked);

    /**
     * Start listening to switch toggling.
     */
    public abstract void startListening();

    /**
     * Stop listening to switch toggling.
     */
    public abstract void stopListening();

    /**
     * Set the checked state for the switch.
     *
     * @param checked whether the switch should be checked or not.
     */
    public abstract void setChecked(boolean checked);

    /**
     * Get the checked state for the switch.
     *
     * @return true if the switch is currently checked, false otherwise.
     */
    public abstract boolean isChecked();

    /**
     * Set the enabled state for the switch.
     *
     * @param enabled whether the switch should be enabled or not.
     */
    public abstract void setEnabled(boolean enabled);

    /**
     * Disable the switch based on the enforce admin.
     *
     * @param admin Details of the admin who enforced the restriction. If it
     * is {@code null}, then this preference will be enabled. Otherwise, it will be disabled.
     */
    public abstract void setDisabledByAdmin(EnforcedAdmin admin);
}