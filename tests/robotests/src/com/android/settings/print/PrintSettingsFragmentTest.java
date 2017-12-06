/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.print;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.content.res.Resources;
import android.print.PrintJob;
import android.print.PrintJobInfo;
import android.print.PrintManager;
import android.printservice.PrintServiceInfo;

import com.android.settings.R;
import com.android.settings.TestConfig;
import com.android.settings.dashboard.SummaryLoader;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;


@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class PrintSettingsFragmentTest {

    @Mock
    private PrintSettingsFragment.PrintSummaryProvider.PrintManagerWrapper mPrintManager;
    @Mock
    private Activity mActivity;
    @Mock
    private Resources mRes;
    @Mock
    private SummaryLoader mSummaryLoader;
    private SummaryLoader.SummaryProvider mSummaryProvider;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mActivity.getResources()).thenReturn(mRes);
        mSummaryProvider = new PrintSettingsFragment.PrintSummaryProvider(mActivity, mSummaryLoader,
                mPrintManager);
    }

    @Test
    public void testSummary_hasActiveJob_shouldSetSummaryToNumberOfJobs() {
        final List<PrintJob> printJobs = new ArrayList<>();
        final PrintJob job = mock(PrintJob.class, Mockito.RETURNS_DEEP_STUBS);
        printJobs.add(job);
        when(job.getInfo().getState()).thenReturn(PrintJobInfo.STATE_STARTED);
        when(mPrintManager.getPrintJobs()).thenReturn(printJobs);

        mSummaryProvider.setListening(true);

        verify(mRes).getQuantityString(R.plurals.print_jobs_summary, 1, 1);
    }

    @Test
    public void testSummary_shouldSetSummaryToNumberOfPrintServices() {
        final List<PrintServiceInfo> printServices = mock(List.class);
        when(printServices.isEmpty()).thenReturn(false);
        when(printServices.size()).thenReturn(2);
        // 2 services
        when(mPrintManager.getPrintServices(PrintManager.ENABLED_SERVICES))
                .thenReturn(printServices);

        mSummaryProvider.setListening(true);

        verify(mRes).getQuantityString(R.plurals.print_settings_summary, 2, 2);

        // No service
        when(mPrintManager.getPrintServices(PrintManager.ENABLED_SERVICES)).thenReturn(null);

        mSummaryProvider.setListening(true);

        verify(mActivity).getString(R.string.print_settings_summary_no_service);
    }
}
