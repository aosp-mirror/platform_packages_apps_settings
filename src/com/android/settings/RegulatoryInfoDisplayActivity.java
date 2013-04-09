/*
 * Copyright (C) 2013 The Android Open Source Project
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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.TextView;

/**
 * {@link Activity} that displays regulatory information for the "Regulatory information"
 * preference item, and when "*#07#" is dialed on the Phone keypad. To enable this feature,
 * set the "config_show_regulatory_info" boolean to true in a device overlay resource, and in the
 * same overlay, either add a drawable named "regulatory_info.png" containing a graphical version
 * of the required regulatory info, or add a string resource named "regulatory_info_text" with
 * an HTML version of the required information (text will be centered in the dialog).
 */
public class RegulatoryInfoDisplayActivity extends Activity implements
        DialogInterface.OnDismissListener {

    /**
     * Display the regulatory info graphic in a dialog window.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Resources resources = getResources();

        if (!resources.getBoolean(R.bool.config_show_regulatory_info)) {
            finish();   // no regulatory info to display for this device
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(R.string.regulatory_information)
                .setOnDismissListener(this);

        boolean regulatoryInfoDrawableExists;
        try {
            Drawable d = resources.getDrawable(R.drawable.regulatory_info);
            // set to false if the width or height is <= 2
            // (missing PNG can return an empty 2x2 pixel Drawable)
            regulatoryInfoDrawableExists = (d.getIntrinsicWidth() > 2
                    && d.getIntrinsicHeight() > 2);
        } catch (Resources.NotFoundException ignored) {
            regulatoryInfoDrawableExists = false;
        }

        CharSequence regulatoryText = resources.getText(R.string.regulatory_info_text);

        if (regulatoryInfoDrawableExists) {
            builder.setView(getLayoutInflater().inflate(R.layout.regulatory_info, null));
            builder.show();
        } else if (regulatoryText.length() > 0) {
            builder.setMessage(regulatoryText);
            AlertDialog dialog = builder.show();
            // we have to show the dialog first, or the setGravity() call will throw a NPE
            TextView messageText = (TextView) dialog.findViewById(android.R.id.message);
            messageText.setGravity(Gravity.CENTER);
        } else {
            // neither drawable nor text resource exists, finish activity
            finish();
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        finish();   // close the activity
    }
}
