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

package com.android.settings.datetime.timezone;

import static com.google.common.truth.Truth.assertThat;

import android.icu.text.TimeZoneFormat;
import android.text.Spannable;
import android.text.SpannableStringBuilder;

import com.android.settings.R;
import com.android.settingslib.datetime.ZoneGetter;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

@RunWith(RobolectricTestRunner.class)
public class SpannableUtilTest {

    @Test
    public void testGetResourceText() {
        CharSequence gmtString = getGmtString("GMT+00:00");

        Spannable spannable = SpannableUtil.getResourcesText(
                RuntimeEnvironment.application.getResources(), R.string.zone_info_offset_and_name,
                gmtString, "UTC");
        assertThat(spannable.toString()).isEqualTo("UTC (GMT+00:00)");

        // Verify that the spans are kept.
        Object[] spans = ((Spannable) gmtString).getSpans(0, gmtString.length(), Object.class);
        Object[] newSpans = spannable.getSpans(0, spannable.length(), Object.class);
        assertThat(newSpans.length).isEqualTo(spans.length);
        assertThat(newSpans).isEqualTo(spans);
    }

    private static CharSequence getGmtString(String tzId) {
        Locale locale = Locale.US;
        TimeZoneFormat timeZoneFormat = TimeZoneFormat.getInstance(locale);
        TimeZone gmtZone = TimeZone.getTimeZone(tzId);
        Date date = new Date(0);
        return ZoneGetter.getGmtOffsetText(timeZoneFormat, locale, gmtZone, date);
    }
    /**
     * Verify the assumption on the GMT string used in {@link #testGetResourceText()}
     */
    @Test
    public void testGetGmtString() {
        // Create a GMT string and verify the assumptions
        CharSequence gmtString = getGmtString("GMT+00:00");
        assertThat(gmtString.toString()).isEqualTo("GMT+00:00");
        assertThat(gmtString).isInstanceOf(Spannable.class);
        Object[] spans = ((Spannable) gmtString).getSpans(0, gmtString.length(), Object.class);
        assertThat(spans).isNotEmpty();

        assertThat(getGmtString("GMT-08:00").toString()).isEqualTo("GMT-08:00");
    }

    @Test
    public void testTitleCaseSentences_enUS() {
        Locale locale = Locale.US;
        CharSequence titleCasedFirstSentence = SpannableUtil.titleCaseSentences(locale,
                "pacific Daylight Time starts on Mar 11 2018.");
        assertThat(titleCasedFirstSentence.toString())
            .isEqualTo("Pacific Daylight Time starts on Mar 11 2018.");

        SpannableStringBuilder sb = new SpannableStringBuilder()
                .append("uses ")
                .append("Pacific Time (")
                .append(getGmtString("GMT-08:00"))
                .append("). ")
                .append(titleCasedFirstSentence);

        assertThat(sb.toString()).isEqualTo(
                "uses Pacific Time (GMT-08:00). Pacific Daylight Time starts on Mar 11 2018.");

        Object[] spans = sb.getSpans(0, sb.length(), Object.class);
        assertThat(spans).isNotEmpty();

        CharSequence titledOutput = SpannableUtil.titleCaseSentences(Locale.US, sb);
        assertThat(titledOutput.toString()).isEqualTo(
                "Uses Pacific Time (GMT-08:00). Pacific Daylight Time starts on Mar 11 2018.");
        assertThat(titledOutput).isInstanceOf(Spannable.class);
        Object[] newSpans = ((Spannable) titledOutput).getSpans(0, titledOutput.length(),
                Object.class);
        assertThat(newSpans.length).isEqualTo(spans.length);
        assertThat(newSpans).isEqualTo(spans);
    }
}
