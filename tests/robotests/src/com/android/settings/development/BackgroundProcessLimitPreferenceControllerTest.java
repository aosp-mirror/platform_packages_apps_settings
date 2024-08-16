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

package com.android.settings.development;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.IActivityManager;
import android.content.Context;
import android.os.RemoteException;

import androidx.preference.ListPreference;
import androidx.preference.PreferenceScreen;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class BackgroundProcessLimitPreferenceControllerTest {

    @Mock
    private IActivityManager mActivityManager;
    @Mock
    private ListPreference mPreference;
    @Mock
    private PreferenceScreen mScreen;

    /**
     * 0: Standard limit
     * 1: No Background processes
     * 2: At most 1 process
     * 3: At most 2 processes
     * 4: At most 3 processes
     * 5: At most 4 processes
     */
    private String[] mListValues;
    private String[] mListSummaries;
    private Context mContext;
    private BackgroundProcessLimitPreferenceController mController;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mListValues = mContext.getResources()
                .getStringArray(com.android.settingslib.R.array.app_process_limit_values);
        mListSummaries = mContext.getResources()
                .getStringArray(com.android.settingslib.R.array.app_process_limit_entries);
        mController = spy(new BackgroundProcessLimitPreferenceController(mContext));
        doReturn(mActivityManager).when(mController).getActivityManagerService();
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
        mController.displayPreference(mScreen);
    }

    @Test
    public void onPreferenceChange_noBackgroundProcessSet_shouldSetToNoBackgroundProcess()
            throws RemoteException {
        mController.onPreferenceChange(mPreference, mListValues[1]);

        verify(mActivityManager).setProcessLimit(Integer.valueOf(mListValues[1]));
    }

    @Test
    public void onPreferenceChange_1ProcessSet_shouldSetTo1BackgroundProcess()
            throws RemoteException {
        mController.onPreferenceChange(mPreference, mListValues[2]);

        verify(mActivityManager).setProcessLimit(Integer.valueOf(mListValues[2]));
    }

    @Test
    public void updateState_noBackgroundProcessSet_shouldSetPreferenceToNoBackgroundProcess()
            throws RemoteException {
        when(mActivityManager.getProcessLimit()).thenReturn(Integer.valueOf(mListValues[1]));

        mController.updateState(mPreference);

        verify(mPreference).setValue(mListValues[1]);
        verify(mPreference).setSummary(mListSummaries[1]);
    }

    @Test
    public void updateState_1ProcessSet_shouldSetPreference1BackgroundProcess()
            throws RemoteException {
        when(mActivityManager.getProcessLimit()).thenReturn(Integer.valueOf(mListValues[2]));

        mController.updateState(mPreference);

        verify(mPreference).setValue(mListValues[2]);
        verify(mPreference).setSummary(mListSummaries[2]);
    }

    @Test
    public void updateState_veryHighLimit_shouldDefaultToStandardLimit() throws RemoteException {
        when(mActivityManager.getProcessLimit()).thenReturn(Integer.MAX_VALUE);

        mController.updateState(mPreference);

        verify(mPreference).setValue(mListValues[0]);
        verify(mPreference).setSummary(mListSummaries[0]);
    }

    @Test
    public void onDeveloperOptionsSwitchDisabled_shouldDisableAndResetPreference()
            throws RemoteException {
        mController.onDeveloperOptionsSwitchDisabled();

        verify(mPreference).setEnabled(false);
        verify(mActivityManager).setProcessLimit(-1);
    }
}
