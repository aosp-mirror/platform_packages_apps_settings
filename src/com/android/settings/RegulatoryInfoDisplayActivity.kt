/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.android.settings.deviceinfo.regulatory.RegulatoryInfo.getRegulatoryInfo

/**
 * [Activity] that displays regulatory information for the "Regulatory information"
 * preference item, and when "*#07#" is dialed on the Phone keypad. To enable this feature,
 * set the "config_show_regulatory_info" boolean to true in a device overlay resource, and in the
 * same overlay, either add a drawable named "regulatory_info.png" containing a graphical version
 * of the required regulatory info (If ro.bootloader.hardware.sku property is set use
 * "regulatory_info_<sku>.png where sku is ro.bootloader.hardware.sku property value in lowercase"),
 * or add a string resource named "regulatory_info_text" with an HTML version of the required
 * information (text will be centered in the dialog).
 */
class RegulatoryInfoDisplayActivity : Activity() {

    /** Display the regulatory info graphic in a dialog window. */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val builder = AlertDialog.Builder(this)
            .setTitle(R.string.regulatory_labels)
            .setOnDismissListener { finish() }  // close the activity
            .setPositiveButton(android.R.string.ok, null)

        getRegulatoryInfo()?.let {
            val view = layoutInflater.inflate(R.layout.regulatory_info, null)
            val image = view.requireViewById<ImageView>(R.id.regulatoryInfo)
            image.setImageDrawable(it)
            builder.setView(view)
            builder.show()
            return
        }

        val regulatoryText = resources.getText(R.string.regulatory_info_text)
        if (regulatoryText.isNotEmpty()) {
            builder.setMessage(regulatoryText)
            val dialog = builder.show()
            // we have to show the dialog first, or the setGravity() call will throw a NPE
            dialog.findViewById<TextView>(android.R.id.message)?.gravity = Gravity.CENTER
        } else {
            // neither drawable nor text resource exists, finish activity
            finish()
        }
    }
}
