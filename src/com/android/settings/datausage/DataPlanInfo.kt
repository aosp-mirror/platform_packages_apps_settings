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

package com.android.settings.datausage

data class DataPlanInfo(

    /** The number of registered plans, [0, N] */
    val dataPlanCount: Int,

    /**
     * The size of the first registered plan if one exists or the size of the warning if it is set.
     *
     * Set to -1 if no plan information is available.
     */
    val dataPlanSize: Long,

    /**
     * The "size" of the data usage bar, i.e. the amount of data its rhs end represents.
     *
     * Set to -1 if not display a data usage bar.
     */
    val dataBarSize: Long,

    /** The number of bytes used since the start of the cycle. */
    val dataPlanUse: Long,

    /**
     * The ending time of the billing cycle in ms since the epoch.
     *
     * Set to `null` if no cycle information is available.
     */
    val cycleEnd: Long?,

    /** The time of the last update in milliseconds since the epoch, or -1 if unknown. */
    val snapshotTime: Long,
)
