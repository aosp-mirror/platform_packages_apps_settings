/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.spa.core.instrumentation

import android.app.settings.SettingsEnums
import android.os.Bundle
import android.util.Log
import com.android.settings.overlay.FeatureFactory.Companion.featureFactory
import com.android.settingslib.spa.framework.common.LOG_DATA_METRICS_CATEGORY
import com.android.settingslib.spa.framework.common.LogCategory
import com.android.settingslib.spa.framework.common.LogEvent
import com.android.settingslib.spa.framework.common.SpaLogger

/**
 * To receive the events from spa framework and logging the these events.
 */
object SpaLogMetricsProvider : SpaLogger {
    override fun event(id: String, event: LogEvent, category: LogCategory, extraData: Bundle) {
        val metricsFeatureProvider = featureFactory.metricsFeatureProvider
        val metricsCategoryOfPage = extraData.getInt(LOG_DATA_METRICS_CATEGORY)
        Log.d("SpaLogMetricsProvider", "${event} page ${metricsCategoryOfPage}")

        if (metricsCategoryOfPage == SettingsEnums.PAGE_UNKNOWN) {
            return
        }

        when (event) {
            LogEvent.PAGE_ENTER -> {
                metricsFeatureProvider.visible(
                    null,
                    SettingsEnums.PAGE_UNKNOWN,
                    metricsCategoryOfPage,
                    0
                )
            }

            LogEvent.PAGE_LEAVE -> {
                metricsFeatureProvider.hidden(
                    null,
                    metricsCategoryOfPage,
                    0
                )
            }

            else -> return
        }
    }
}
