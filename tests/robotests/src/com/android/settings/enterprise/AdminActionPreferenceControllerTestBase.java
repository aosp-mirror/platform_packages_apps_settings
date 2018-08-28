/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings.enterprise;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.content.Context;
import android.provider.Settings;
import android.text.format.DateUtils;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Common base for testing subclasses of {@link AdminActionPreferenceControllerBase}.
 */
public abstract class AdminActionPreferenceControllerTestBase {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    protected Context mContext;
    protected FakeFeatureFactory mFeatureFactory;

    protected AdminActionPreferenceControllerBase mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mFeatureFactory = FakeFeatureFactory.setupForTest();
    }

    public abstract void setDate(Date date);

    @Test
    public void testUpdateState() {
        final Preference preference = new Preference(mContext, null, 0, 0);
        when(mContext.getString(R.string.enterprise_privacy_none)).thenReturn("None");
        Settings.System.putString(mContext.getContentResolver(), Settings.System.TIME_12_24, "24");

        setDate(null);
        mController.updateState(preference);
        assertThat(preference.getSummary()).isEqualTo("None");

        final Date date = new GregorianCalendar(2011 /* year */, 10 /* month */, 9 /* dayOfMonth */,
                8 /* hourOfDay */, 7 /* minute */, 6 /* second */).getTime();
        setDate(date);
        mController.updateState(preference);
        assertThat(preference.getSummary()).isEqualTo(DateUtils.formatDateTime(
                mContext, date.getTime(), DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_DATE));
    }

    @Test
    public void testIsAvailable() {
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void testHandlePreferenceTreeClick() {
        assertThat(mController.handlePreferenceTreeClick(new Preference(mContext, null, 0, 0)))
                .isFalse();
    }

    public abstract String getPreferenceKey();

    @Test
    public void testGetPreferenceKey() {
        assertThat(mController.getPreferenceKey()).isEqualTo(getPreferenceKey());
    }
}
