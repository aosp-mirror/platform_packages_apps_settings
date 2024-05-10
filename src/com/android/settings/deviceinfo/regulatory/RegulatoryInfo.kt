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

package com.android.settings.deviceinfo.regulatory

import android.content.Context
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.os.SystemProperties
import androidx.annotation.DrawableRes
import androidx.annotation.VisibleForTesting
import com.android.settings.R



/** To load Regulatory Info from device. */
object RegulatoryInfo {
    private const val REGULATORY_INFO_RESOURCE = "regulatory_info"

    @VisibleForTesting
    const val KEY_COO = "ro.boot.hardware.coo"

    @VisibleForTesting
    const val KEY_SKU = "ro.boot.hardware.sku"

    /** Gets the regulatory drawable. */
    fun Context.getRegulatoryInfo(): Drawable? {
        val sku = getSku()
        if (sku.isNotBlank()) {
            // When hardware coo property exists, use regulatory_info_<sku>_<coo> resource if valid.
            val coo = getCoo()
            if (coo.isNotBlank()) {
                getRegulatoryInfo("${REGULATORY_INFO_RESOURCE}_${sku}_$coo")?.let { return it }
            }
            // Use regulatory_info_<sku> resource if valid.
            getRegulatoryInfo("${REGULATORY_INFO_RESOURCE}_$sku")?.let { return it }
        }
        return getRegulatoryInfo(REGULATORY_INFO_RESOURCE)
    }

    private fun getCoo(): String = SystemProperties.get(KEY_COO).lowercase()

    private fun getSku(): String = SystemProperties.get(KEY_SKU).lowercase()

    private fun Context.getRegulatoryInfo(fileName: String): Drawable? {
        val overlayPackageName =
            resources.getString(R.string.config_regulatory_info_overlay_package_name)
                .ifBlank { packageName }
        val resources = packageManager.getResourcesForApplication(overlayPackageName)
        val id = resources.getIdentifier(fileName, "drawable", overlayPackageName)
        return if (id > 0) resources.getRegulatoryInfo(id) else null
    }

    private fun Resources.getRegulatoryInfo(@DrawableRes resId: Int): Drawable? = try {
        getDrawable(resId, null).takeIf {
            // Ignore the placeholder image
            it.intrinsicWidth > 10 && it.intrinsicHeight > 10
        }
    } catch (_: Resources.NotFoundException) {
        null
    }
}
