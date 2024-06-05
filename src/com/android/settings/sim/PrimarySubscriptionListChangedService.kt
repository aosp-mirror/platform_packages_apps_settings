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

package com.android.settings.sim

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import com.android.settings.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** A JobService work on primary subscription list changed. */
class PrimarySubscriptionListChangedService : JobService() {
    private var job: Job? = null

    override fun onStartJob(params: JobParameters): Boolean {
        job = CoroutineScope(Dispatchers.Default + SupervisorJob()).launch {
            try {
                val intent = Intent()
                intent.putExtras(params.transientExtras)
                SimSelectNotification.onPrimarySubscriptionListChanged(
                    this@PrimarySubscriptionListChangedService,
                    intent
                )
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
        private const val TAG = "PrimarySubscriptionListChangedService"

        /**
         * Schedules a service to work on primary subscription changed.
         *
         * @param context is the caller context.
         */
        @JvmStatic
        fun scheduleJob(context: Context, intent: Intent) {
            val component =
                ComponentName(context, PrimarySubscriptionListChangedService::class.java)
            val jobScheduler = context.getSystemService(JobScheduler::class.java)!!

            val jobInfoBuilder =
                JobInfo.Builder(R.integer.primary_subscription_list_changed, component)
            intent.extras?.let {
                jobInfoBuilder.setTransientExtras(it)
            }
            jobScheduler.schedule(jobInfoBuilder.build())
        }
    }
}