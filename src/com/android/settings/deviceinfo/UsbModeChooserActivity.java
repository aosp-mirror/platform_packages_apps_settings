/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.settings.deviceinfo;

import android.annotation.Nullable;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.UserManager;

import com.android.settings.R;

/**
 * UI for the USB chooser dialog.
 *
 */
public class UsbModeChooserActivity extends Activity {

    private UsbManager mUsbManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        CharSequence[] items;
        UserManager userManager =
                (UserManager) getSystemService(Context.USER_SERVICE);
        if (userManager.hasUserRestriction(UserManager.DISALLOW_USB_FILE_TRANSFER)) {
            items = new CharSequence[] { getText(R.string.usb_use_charging_only) };
        } else {
            items = getResources().getTextArray(R.array.usb_available_functions);
        }

        final AlertDialog levelDialog;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.usb_use);
        builder.setSingleChoiceItems(items, getCurrentFunction(),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (!ActivityManager.isUserAMonkey()) {
                            setCurrentFunction(which);
                        }
                        dialog.dismiss();
                        UsbModeChooserActivity.this.finish();
                    }
                });
        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                UsbModeChooserActivity.this.finish();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                UsbModeChooserActivity.this.finish();
            }
        });
        levelDialog = builder.create();
        levelDialog.show();
    }

    /*
     * If you change the numbers here, you also need to change R.array.usb_available_functions
     * so that everything matches.
     */
    private int getCurrentFunction() {
        if (!mUsbManager.isUsbDataUnlocked()) {
            return 0;
        } else if (mUsbManager.isFunctionEnabled(UsbManager.USB_FUNCTION_MTP)) {
            return 1;
        } else if (mUsbManager.isFunctionEnabled(UsbManager.USB_FUNCTION_PTP)) {
            return 2;
        } else if (mUsbManager.isFunctionEnabled(UsbManager.USB_FUNCTION_MIDI)) {
            return 3;
        }
        return 0;
    }

    /*
     * If you change the numbers here, you also need to change R.array.usb_available_functions
     * so that everything matches.
     */
    private void setCurrentFunction(int which) {
        switch (which) {
            case 0:
                mUsbManager.setCurrentFunction(null);
                mUsbManager.setUsbDataUnlocked(false);
                break;
            case 1:
                mUsbManager.setCurrentFunction(UsbManager.USB_FUNCTION_MTP);
                mUsbManager.setUsbDataUnlocked(true);
                break;
            case 2:
                mUsbManager.setCurrentFunction(UsbManager.USB_FUNCTION_PTP);
                mUsbManager.setUsbDataUnlocked(true);
                break;
            case 3:
                mUsbManager.setCurrentFunction(UsbManager.USB_FUNCTION_MIDI);
                mUsbManager.setUsbDataUnlocked(true);
                break;
        }
    }
}
