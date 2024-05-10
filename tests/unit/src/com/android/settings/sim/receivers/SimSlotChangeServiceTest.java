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

package com.android.settings.sim.receivers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.app.job.JobScheduler;
import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class SimSlotChangeServiceTest {

    @Mock
    private JobScheduler mJobScheduler;

    private Context mContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = spy(ApplicationProvider.getApplicationContext());
        doReturn(mContext).when(mContext).getApplicationContext();
        mockService(Context.JOB_SCHEDULER_SERVICE, JobScheduler.class, mJobScheduler);
    }

    @Test
    public void scheduleSimSlotChange_addSchedule_whenInvoked() {
        SimSlotChangeService.scheduleSimSlotChange(mContext);
        verify(mJobScheduler).schedule(any());
    }

    private <T> void mockService(String serviceName, Class<T> serviceClass, T service) {
        doReturn(serviceName).when(mContext).getSystemServiceName(serviceClass);
        doReturn(service).when(mContext).getSystemService(serviceName);
    }
}
