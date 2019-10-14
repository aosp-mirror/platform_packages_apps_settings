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
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;

import java.util.Locale;

/**
 * {@link Activity} that displays regulatory information for the "Regulatory information"
 * preference item, and when "*#07#" is dialed on the Phone keypad. To enable this feature,
 * set the "config_show_regulatory_info" boolean to true in a device overlay resource, and in the
 * same overlay, either add a drawable named "regulatory_info.png" containing a graphical version
 * of the required regulatory info (If ro.bootloader.hardware.sku property is set use
 * "regulatory_info_<sku>.png where sku is ro.bootloader.hardware.sku property value in lowercase"),
 * or add a string resource named "regulatory_info_text" with an HTML version of the required
 * information (text will be centered in the dialog).
 */
public class RegulatoryInfoDisplayActivity extends Activity implements
        DialogInterface.OnDismissListener {

    private final String REGULATORY_INFO_RESOURCE = "regulatory_info";
    private static final String DEFAULT_REGULATORY_INFO_FILEPATH =
            "/data/misc/elabel/regulatory_info.png";
    private static final String REGULATORY_INFO_FILEPATH_TEMPLATE =
            "/data/misc/elabel/regulatory_info_%s.png";

    /**
     * Display the regulatory info graphic in a dialog window.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(R.string.regulatory_labels)
                .setOnDismissListener(this);

        boolean regulatoryInfoDrawableExists = false;

        final String regulatoryInfoFile = getRegulatoryInfoImageFileName();
        final Bitmap regulatoryInfoBitmap = BitmapFactory.decodeFile(regulatoryInfoFile);

        if (regulatoryInfoBitmap != null) {
            regulatoryInfoDrawableExists = true;
        }

        int resId = 0;
        if (!regulatoryInfoDrawableExists) {
            resId = getResourceId();
        }
        if (resId != 0) {
            try {
                Drawable d = getDrawable(resId);
                // set to false if the width or height is <= 2
                // (missing PNG can return an empty 2x2 pixel Drawable)
                regulatoryInfoDrawableExists = (d.getIntrinsicWidth() > 2
                        && d.getIntrinsicHeight() > 2);
            } catch (Resources.NotFoundException ignored) {
                regulatoryInfoDrawableExists = false;
            }
        }

        CharSequence regulatoryText = getResources()
                .getText(R.string.regulatory_info_text);

        if (regulatoryInfoDrawableExists) {
            View view = getLayoutInflater().inflate(R.layout.regulatory_info, null);
            ImageView image = view.findViewById(R.id.regulatoryInfo);
            if (regulatoryInfoBitmap != null) {
                image.setImageBitmap(regulatoryInfoBitmap);
            } else {
                image.setImageResource(resId);
            }
            builder.setView(view);
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

    @VisibleForTesting
    int getResourceId() {
        // Use regulatory_info by default.
        int resId = getResources().getIdentifier(
                REGULATORY_INFO_RESOURCE, "drawable", getPackageName());

        // When hardware sku property exists, use regulatory_info_<sku> resource if valid.
        final String sku = getSku();
        if (!TextUtils.isEmpty(sku)) {
            String regulatory_info_res = REGULATORY_INFO_RESOURCE + "_" + sku.toLowerCase();
            int id = getResources().getIdentifier(
                    regulatory_info_res, "drawable", getPackageName());
            if (id != 0) {
                resId = id;
            }
        }

        // When hardware coo property exists, use regulatory_info_<sku>_<coo> resource if valid.
        final String coo = getCoo();
        if (!TextUtils.isEmpty(coo) && !TextUtils.isEmpty(sku)) {
            final String regulatory_info_coo_res =
                    REGULATORY_INFO_RESOURCE + "_" + sku.toLowerCase() + "_" + coo.toLowerCase();
            final int id = getResources().getIdentifier(
                    regulatory_info_coo_res, "drawable", getPackageName());
            if (id != 0) {
                resId = id;
            }
        }
        return resId;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        finish();   // close the activity
    }

    private String getCoo() {
        return SystemProperties.get("ro.boot.hardware.coo", "");
    }

    private String getSku() {
        return SystemProperties.get("ro.boot.hardware.sku", "");
    }

    private String getRegulatoryInfoImageFileName() {
        final String sku = getSku();
        if (TextUtils.isEmpty(sku)) {
            return DEFAULT_REGULATORY_INFO_FILEPATH;
        } else {
            return String.format(Locale.US, REGULATORY_INFO_FILEPATH_TEMPLATE,
                    sku.toLowerCase());
        }
    }
}
