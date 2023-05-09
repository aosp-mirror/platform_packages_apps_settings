/*
 * Copyright (C) 2022 The Android Open Source Project
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
package com.android.settings.sim.receivers

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.util.Log
import com.android.settings.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** A JobService work on SIM slot change.  */
class SimSlotChangeService : JobService() {
    private var job: Job? = null

    override fun onStartJob(params: JobParameters): Boolean {
        job = CoroutineScope(Dispatchers.Default + SupervisorJob()).launch {
            try {
                SimSlotChangeReceiver.runOnBackgroundThread(this@SimSlotChangeService)
            } catch (exception: Throwable) {
                Log.e(TAG, "Exception running job", exception)
            }
            jobFinished(params, false)
        }
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        job?.cancel()
        return false
    }

    companion object {
        private const val TAG = "SimSlotChangeService"

        /**
         * Schedules a service to work on SIM slot change.
         *
         * @param context is the caller context.
         */
        @JvmStatic
        fun scheduleSimSlotChange(context: Context) {
            val component = ComponentName(context, SimSlotChangeService::class.java)
            val jobScheduler = context.getSystemService(JobScheduler::class.java)!!
            jobScheduler.schedule(JobInfo.Builder(R.integer.sim_slot_changed, component).build())
        }
    }
}