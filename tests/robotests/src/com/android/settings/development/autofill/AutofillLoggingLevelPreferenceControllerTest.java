/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.development.autofill;

import static androidx.lifecycle.Lifecycle.Event.ON_CREATE;
import static androidx.lifecycle.Lifecycle.Event.ON_DESTROY;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.res.Resources;
import android.view.autofill.AutofillManager;

import androidx.lifecycle.LifecycleOwner;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.Resetter;

@RunWith(RobolectricTestRunner.class)
public class AutofillLoggingLevelPreferenceControllerTest {

    private static final int IDX_OFF = 0;
    private static final int IDX_DEBUG = 1;
    private static final int IDX_VERBOSE = 2;

    @Mock
    private ListPreference mPreference;
    @Mock
    private PreferenceScreen mPreferenceScreen;

    private Context mContext;
    private AutofillLoggingLevelPreferenceController mController;
    private AutofillTestingHelper mHelper;

    private String[] mListValues;
    private String[] mListSummaries;

    private LifecycleOwner mLifecycleOwner;
    private Lifecycle mLifecycle;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this); // TODO: use @Rule
        mLifecycleOwner = () -> mLifecycle;
        mLifecycle = new Lifecycle(mLifecycleOwner);
        mContext = RuntimeEnvironment.application;
        mHelper = new AutofillTestingHelper(mContext);
        final Resources resources = mContext.getResources();
        mListValues = resources.getStringArray(R.array.autofill_logging_level_values);
        mListSummaries = resources.getStringArray(R.array.autofill_logging_level_entries);
        mController = new AutofillLoggingLevelPreferenceController(mContext, mLifecycle);
        when(mPreferenceScreen.findPreference(mController.getPreferenceKey()))
                .thenReturn(mPreference);
        mController.displayPreference(mPreferenceScreen);
    }

    @After
    public void tearDown() {
        ShadowAutofillLoggingLevelPreferenceController.reset();
    }

    @Test
    public void handlePreferenceTreeClick_differentPreferenceKey_shouldNotTrigger()
            throws Exception {
        when(mPreference.getKey()).thenReturn("SomeRandomKey");

        mHelper.setLoggingLevel(108);

        assertThat(mController.handlePreferenceTreeClick(mPreference)).isFalse();

        assertThat(mHelper.getLoggingLevel()).isEqualTo(108);
    }

    @Test
    public void onPreferenceChange_off() throws Exception {
        mHelper.setLoggingLevel(108);

        mController.onPreferenceChange(mPreference, mListValues[IDX_OFF]);

        assertThat(mHelper.getLoggingLevel()).isEqualTo(AutofillManager.NO_LOGGING);
    }

    @Test
    public void onPreferenceChange_debug() throws Exception {
        mHelper.setLoggingLevel(108);

        mController.onPreferenceChange(mPreference, mListValues[IDX_DEBUG]);

        assertThat(mHelper.getLoggingLevel())
                .isEqualTo(AutofillManager.FLAG_ADD_CLIENT_DEBUG);
    }

    @Test
    public void onPreferenceChange_verbose() throws Exception {
        mHelper.setLoggingLevel(108);

        mController.onPreferenceChange(mPreference, mListValues[IDX_VERBOSE]);

        assertThat(mHelper.getLoggingLevel())
                .isEqualTo(AutofillManager.FLAG_ADD_CLIENT_VERBOSE);
    }

    @Test
    public void onSettingsChange_off() {
        mHelper.setLoggingLevel(AutofillManager.NO_LOGGING);

        mController.updateState(mPreference);

        verify(mPreference).setValue(mListValues[IDX_OFF]);
        verify(mPreference).setSummary(mListSummaries[IDX_OFF]);
    }

    @Test
    public void onSettingsChange_debug() {
        mHelper.setLoggingLevel(AutofillManager.FLAG_ADD_CLIENT_DEBUG);

        mController.updateState(mPreference);

        verify(mPreference).setValue(mListValues[IDX_DEBUG]);
        verify(mPreference).setSummary(mListSummaries[IDX_DEBUG]);
    }

    @Test
    public void onSettingsChange_verbose() {
        mHelper.setLoggingLevel(AutofillManager.FLAG_ADD_CLIENT_VERBOSE);

        mController.updateState(mPreference);

        verify(mPreference).setValue(mListValues[IDX_VERBOSE]);
        verify(mPreference).setSummary(mListSummaries[IDX_VERBOSE]);
    }

    @Test
    @Config(shadows = ShadowAutofillLoggingLevelPreferenceController.class)
    public void onDestory_shouldUnregisterObserver() {
        mLifecycle.handleLifecycleEvent(ON_CREATE);
        mLifecycle.handleLifecycleEvent(ON_DESTROY);

        assertThat(ShadowAutofillLoggingLevelPreferenceController.isUnregisterObserver).isTrue();
    }

    @Implements(AutofillLoggingLevelPreferenceController.class)
    public static class ShadowAutofillLoggingLevelPreferenceController {
        private static boolean isUnregisterObserver = false;

        @Implementation
        public void onDestroy() {
            isUnregisterObserver = true;
        }

        @Resetter
        public static void reset() {
            isUnregisterObserver = false;
        }
    }
}
