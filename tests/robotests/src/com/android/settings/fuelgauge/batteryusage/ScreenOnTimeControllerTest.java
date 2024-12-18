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

package com.android.settings.fuelgauge.batteryusage;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.content.res.Resources;
import android.os.LocaleList;
import android.text.SpannableString;

import androidx.preference.PreferenceCategory;

import com.android.settings.R;
import com.android.settings.Utils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.Locale;
import java.util.TimeZone;

@RunWith(RobolectricTestRunner.class)
public final class ScreenOnTimeControllerTest {

    private Context mContext;
    private ScreenOnTimeController mScreenOnTimeController;
    private ArgumentCaptor<SpannableString> mStringCaptor;

    @Mock private PreferenceCategory mRootPreference;
    @Mock private TextViewPreference mScreenOnTimeTextPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        Locale.setDefault(new Locale("en_US"));
        org.robolectric.shadows.ShadowSettings.set24HourTimeFormat(false);
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        mContext = spy(RuntimeEnvironment.application);
        mStringCaptor = ArgumentCaptor.forClass(SpannableString.class);
        final Resources resources = spy(mContext.getResources());
        resources.getConfiguration().setLocales(new LocaleList(new Locale("en_US")));
        doReturn(resources).when(mContext).getResources();
        mScreenOnTimeController = new ScreenOnTimeController(mContext);
        mScreenOnTimeController.mPrefContext = mContext;
        mScreenOnTimeController.mRootPreference = mRootPreference;
        mScreenOnTimeController.mScreenOnTimeTextPreference = mScreenOnTimeTextPreference;
        mScreenOnTimeController.mScreenTimeCategoryLastFullChargeText =
                resources.getString(R.string.screen_time_category_last_full_charge);
    }

    @Test
    public void handleScreenOnTimeUpdated_nullScreenOnTime_hideAllPreference() {
        mScreenOnTimeController.handleScreenOnTimeUpdated(
                /* screenOnTime= */ null, "Friday 12:00 - now", "Friday 12:00 to now");

        verify(mRootPreference).setVisible(false);
        verify(mScreenOnTimeTextPreference).setVisible(false);
    }

    @Test
    public void showCategoryTitle_null_sinceLastFullCharge() {
        mScreenOnTimeController.showCategoryTitle(null, null);

        verify(mRootPreference).setTitle(mStringCaptor.capture());
        verify(mRootPreference).setVisible(true);
        assertThat(mStringCaptor.getValue().toString())
                .isEqualTo(
                        Utils.createAccessibleSequence(
                                        mScreenOnTimeController
                                                .mScreenTimeCategoryLastFullChargeText,
                                        mScreenOnTimeController
                                                .mScreenTimeCategoryLastFullChargeText)
                                .toString());
    }

    @Test
    public void showCategoryTitle_notNull_slotTimestamp() {
        mScreenOnTimeController.showCategoryTitle("Friday 12:00 - now", "Friday 12:00 to now");

        verify(mRootPreference).setTitle(mStringCaptor.capture());
        verify(mRootPreference).setVisible(true);
        assertThat(mStringCaptor.getValue().toString())
                .isEqualTo(
                        Utils.createAccessibleSequence(
                                        "Screen time for Friday 12:00 - now",
                                        "Screen time for Friday 12:00 to now")
                                .toString());
    }

    @Test
    public void showScreenOnTimeText_returnExpectedResult() {
        mScreenOnTimeController.showScreenOnTimeText(1600000000L);

        ArgumentCaptor<CharSequence> argumentCaptor = ArgumentCaptor.forClass(CharSequence.class);
        verify(mScreenOnTimeTextPreference).setText(argumentCaptor.capture());
        assertThat(argumentCaptor.getValue().toString()).isEqualTo("18 days 12 hr 27 min");
        verify(mScreenOnTimeTextPreference).setVisible(true);
    }
}
