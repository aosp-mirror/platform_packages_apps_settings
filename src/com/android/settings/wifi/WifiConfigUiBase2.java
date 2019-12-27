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

package com.android.settings.wifi;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.Button;

/**
 * Foundation interface glues between Activities and UIs like {@link WifiDialog2}.
 */
public interface WifiConfigUiBase2 {

    /**
     * Viewing mode for a Wi-Fi access point. Data is displayed in non-editable mode.
     */
    int MODE_VIEW = 0;
    /**
     * Connect mode. Data is displayed in editable mode, and a connect button will be shown.
     */
    int MODE_CONNECT = 1;
    /**
     * Modify mode. All data is displayed in editable fields, and a "save" button is shown instead
     * of "connect". Clients are expected to only save but not connect to the access point in this
     * mode.
     */
    int MODE_MODIFY = 2;

    /**
     * UI like {@link WifiDialog} overrides to provide {@link Context} to controller.
     */
    Context getContext();

    /**
     * {@link WifiConfigController2} share the logic for controlling buttons, text fields, etc.
     */
    WifiConfigController2 getController();

    /**
     * UI like {@link WifiDialog} overrides to provide {@link LayoutInflater} to controller.
     */
    LayoutInflater getLayoutInflater();

    /**
     * One of MODE_VIEW, MODE_CONNECT and MODE_MODIFY of the UI like {@link WifiDialog}.
     */
    int getMode();

    /**
     * For controller to dispatch submit event to host UI and UI like {@link WifiDialog}.
     */
    void dispatchSubmit();

    /**
     * UI like {@link WifiDialog} overrides to set title.
     */
    void setTitle(int id);

    /**
     * UI like {@link WifiDialog} overrides to set title.
     */
    void setTitle(CharSequence title);

    /**
     * UI like {@link WifiDialog} overrides to set submit button text.
     */
    void setSubmitButton(CharSequence text);

    /**
     * UI like {@link WifiDialog} overrides to set forget button text.
     */
    void setForgetButton(CharSequence text);

    /**
     * UI like {@link WifiDialog} overrides to set cancel button text.
     */
    void setCancelButton(CharSequence text);

    /**
     * UI like {@link WifiDialog} overrides to get submit button.
     */
    Button getSubmitButton();

    /**
     * UI like {@link WifiDialog} overrides to get forget button.
     */
    Button getForgetButton();

    /**
     * UI like {@link WifiDialog} overrides to get cancel button.
     */
    Button getCancelButton();
}
