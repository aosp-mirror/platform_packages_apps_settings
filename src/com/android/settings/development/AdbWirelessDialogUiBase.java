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

package com.android.settings.development;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.Button;

/**
 * Foundation interface glues between Activities and UIs like {@link AdbWirelessDialog}.
 */
public interface AdbWirelessDialogUiBase {
    /**
     * Dialog shown when pairing a device via six-digit code.
     */
    int MODE_PAIRING = 0;
    /**
     *  Dialog shown when connecting to a paired device failed.
     */
    int MODE_CONNECTION_FAILED = 1;
    /**
     * Dialog shown when pairing failed.
     */
    int MODE_PAIRING_FAILED = 2;

    /**
     * Dialog shown when QR code pairing failed.
     */
    int MODE_QRCODE_FAILED = 3;

    /**
     * Gets the context for the dialog.
     *
     * @return the context for the dialog
     */
    Context getContext();

    /**
     * Gets the controller for the dialog.
     *
     * @return the controller for the dialog.
     */
    AdbWirelessDialogController getController();

    /**
     * Gets the layout for the dialog.
     *
     * @return the {@link LayoutInflater} for the dialog
     */
    LayoutInflater getLayoutInflater();

    /**
     * Gets the dialog mode/ID.
     *
     * @return the mode of the dialog
     */
    int getMode();

    /**
     * Sends a submit command to the dialog.
     */
    void dispatchSubmit();

    /**
     * Enables if user can cancel a dialog by clicking outside of the dialog.
     *
     * @param cancel The flag indicating if can cancel by clicking outside
     */
    void setCanceledOnTouchOutside(boolean cancel);

    /**
     * Sets the title of the dialog.
     *
     * @param id the string id
     */
    void setTitle(int id);

    /**
     * Sets the title of the dialog.
     *
     * @param title the title string
     */
    void setTitle(CharSequence title);

    /**
     * Sets the text for the submit button.
     *
     * @param text the submit text
     */
    void setSubmitButton(CharSequence text);

    /**
     * Sets the text for the cancel button.
     *
     * @param text the cancel text
     */
    void setCancelButton(CharSequence text);

    /**
     * Gets the button widget for the submit button.
     *
     * @return the submit {@link Button} widget
     */
    Button getSubmitButton();

    /**
     * Gets the button widget for the cancel button.
     *
     * @return the cancel {@link Button} widget
     */
    Button getCancelButton();
}
