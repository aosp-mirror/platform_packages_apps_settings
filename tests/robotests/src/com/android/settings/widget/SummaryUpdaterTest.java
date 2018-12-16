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

package com.android.settings.widget;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class SummaryUpdaterTest {

    private Context mContext;

    private SummaryUpdaterTestable mSummaryUpdater;
    @Mock
    private SummaryListener mListener;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application.getApplicationContext();
        mSummaryUpdater = new SummaryUpdaterTestable(mContext, mListener);
    }

    @Test
    public void notifyChangeIfNeeded_fistTimeInit_shouldNotifyChange() {
        final String summary = "initialized";
        mSummaryUpdater.setTestSummary(summary);
        mSummaryUpdater.notifyChangeIfNeeded();

        verify(mListener).onSummaryChanged(summary);
    }

    @Test
    public void notifyChangeIfNeeded_summaryUpdated_shouldNotifyChange() {
        final String summaryInit = "initialized";
        mSummaryUpdater.setTestSummary(summaryInit);
        mSummaryUpdater.notifyChangeIfNeeded();
        final String summaryUpdate = "updated";

        mSummaryUpdater.setTestSummary(summaryUpdate);
        mSummaryUpdater.notifyChangeIfNeeded();

        verify(mListener).onSummaryChanged(summaryUpdate);
    }

    @Test
    public void notifyChangeIfNeeded_summaryNotUpdated_shouldOnlyNotifyChangeOnce() {
        final String summaryInit = "initialized";
        mSummaryUpdater.setTestSummary(summaryInit);
        mSummaryUpdater.notifyChangeIfNeeded();
        final String summaryUpdate = "updated";
        mSummaryUpdater.setTestSummary(summaryUpdate);

        mSummaryUpdater.notifyChangeIfNeeded();
        mSummaryUpdater.notifyChangeIfNeeded();

        verify(mListener, times(1)).onSummaryChanged(summaryUpdate);
    }

    private class SummaryListener implements SummaryUpdater.OnSummaryChangeListener {
        String summary;

        @Override
        public void onSummaryChanged(String summary) {
            this.summary = summary;
        }
    }

    public final class SummaryUpdaterTestable extends SummaryUpdater {
        private String mTestSummary;

        SummaryUpdaterTestable(Context context, SummaryUpdater.OnSummaryChangeListener listener) {
            super(context, listener);
        }

        @Override
        public void register(boolean register) {
        }

        @Override
        public void notifyChangeIfNeeded() {
            super.notifyChangeIfNeeded();
        }

        @Override
        public String getSummary() {
            return mTestSummary;
        }

        private void setTestSummary(String summary) {
            mTestSummary = summary;
        }
    }
}
