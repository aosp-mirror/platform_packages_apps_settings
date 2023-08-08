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
package com.android.settings.development;


import static com.android.settings.development.GrammaticalGenderPreferenceController.GRAMMATICAL_GENDER_PROPERTY;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.IActivityManager;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.RemoteException;
import android.os.SystemProperties;

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
public class GrammaticalGenderPreferenceControllerTest {
    @Mock
    private ListPreference mPreference;
    @Mock
    private PreferenceScreen mPreferenceScreen;
    @Mock
    private IActivityManager mActivityManager;
    private Configuration mConfiguration;
    private Context mContext;
    private GrammaticalGenderPreferenceController mController;
    private String[] mListValues;
    private String[] mListSummaries;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        final Resources resources = mContext.getResources();
        mListValues = resources.getStringArray(
                com.android.settingslib.R.array.grammatical_gender_values);
        mListSummaries = resources.getStringArray(
                com.android.settingslib.R.array.grammatical_gender_entries);
        mConfiguration = new Configuration();
        mController = new GrammaticalGenderPreferenceController(mContext, mActivityManager);
        when(mPreferenceScreen.findPreference(mController.getPreferenceKey()))
                .thenReturn(mPreference);
        mController.displayPreference(mPreferenceScreen);
        doReturn(mConfiguration).when(mActivityManager).getConfiguration();
    }

    @Test
    public void onPreferenceChange_setNeuter_shouldEnableNeuter() throws RemoteException {
        mController.onPreferenceChange(mPreference, mListValues[1]);
        final String currentValue = SystemProperties.get(GRAMMATICAL_GENDER_PROPERTY);
        assertThat(currentValue).isEqualTo(mListValues[1]);
        verify(mActivityManager).updatePersistentConfiguration(mConfiguration);
        assertThat(mConfiguration.getGrammaticalGender())
                .isEqualTo(Integer.parseInt(mListValues[1]));
    }

    @Test
    public void updateState_setNeuter_shouldSetPreferenceToNeuter() {
        SystemProperties.set(GRAMMATICAL_GENDER_PROPERTY, mListValues[1]);

        mController.updateState(mPreference);

        verify(mPreference).setValue(mListValues[1]);
        verify(mPreference).setSummary(mListSummaries[1]);
    }

    @Test
    public void onPreferenceChange_setFeminine_shouldEnableFeminine() throws RemoteException {
        mController.onPreferenceChange(mPreference, mListValues[2]);
        final String currentValue = SystemProperties.get(GRAMMATICAL_GENDER_PROPERTY);
        assertThat(currentValue).isEqualTo(mListValues[2]);
        verify(mActivityManager).updatePersistentConfiguration(mConfiguration);
        assertThat(mConfiguration.getGrammaticalGender())
                .isEqualTo(Integer.parseInt(mListValues[2]));
    }

    @Test
    public void updateState_setFeminine_shouldSetPreferenceToFeminine() {
        SystemProperties.set(GRAMMATICAL_GENDER_PROPERTY, mListValues[2]);

        mController.updateState(mPreference);

        verify(mPreference).setValue(mListValues[2]);
        verify(mPreference).setSummary(mListSummaries[2]);
    }

    @Test
    public void onPreferenceChange_setMasculine_shouldEnableMasculine() throws RemoteException {
        mController.onPreferenceChange(mPreference, mListValues[3]);
        final String currentValue = SystemProperties.get(GRAMMATICAL_GENDER_PROPERTY);
        assertThat(currentValue).isEqualTo(mListValues[3]);
        verify(mActivityManager).updatePersistentConfiguration(mConfiguration);
        assertThat(mConfiguration.getGrammaticalGender())
                .isEqualTo(Integer.parseInt(mListValues[3]));
    }

    @Test
    public void updateState_setMasculine_shouldSetPreferenceToMasculine() {
        SystemProperties.set(GRAMMATICAL_GENDER_PROPERTY, mListValues[3]);

        mController.updateState(mPreference);

        verify(mPreference).setValue(mListValues[3]);
        verify(mPreference).setSummary(mListSummaries[3]);
    }

    @Test
    public void updateState_noValueSet_shouldSetDefaultToNotSpecified() {
        mController.updateState(mPreference);

        verify(mPreference).setValue(mListValues[0]);
        verify(mPreference).setSummary(mListSummaries[0]);
    }
}
