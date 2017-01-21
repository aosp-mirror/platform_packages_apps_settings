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

/*
 * A controller class for general switch widget handling. We have different containers that provide
 * different forms of switch layout. Provide a centralized control for updating the switch widget.
 */
public abstract class SwitchWidgetController {

    protected OnSwitchChangeListener mListener;

    public interface OnSwitchChangeListener {
        /**
         * Called when the checked state of the Switch has changed.
         *
         * @param isChecked  The new checked state of switchView.
         */
        boolean onSwitchToggled(boolean isChecked);
    }

    public void setupView() {
    }

    public void teardownView() {
    }

    public void setListener(OnSwitchChangeListener listener) {
        mListener = listener;
    }

    public abstract void updateTitle(boolean isChecked);

    public abstract void startListening();

    public abstract void stopListening();

    public abstract void setChecked(boolean checked);

    public abstract boolean isChecked();

    public abstract void setEnabled(boolean enabled);

}
