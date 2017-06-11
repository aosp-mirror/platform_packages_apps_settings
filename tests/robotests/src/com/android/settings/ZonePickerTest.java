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
 * limitations under the License
 */

package com.android.settings;

import static com.google.common.truth.Truth.assertThat;

import android.text.Spanned;
import android.text.style.TtsSpan;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.android.settings.datetime.ZonePicker;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.shadow.ShadowLibcoreTimeZoneNames;
import com.android.settings.testutils.shadow.ShadowTimeZoneNames;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(
        manifest = TestConfig.MANIFEST_PATH,
        sdk = TestConfig.SDK_VERSION,
        shadows = {
                ShadowLibcoreTimeZoneNames.class,
                ShadowLibcoreTimeZoneNames.ShadowZoneStringsCache.class,
                ShadowTimeZoneNames.class
        }
)
public class ZonePickerTest {

    @Test
    public void testConstructTimeZoneAdapter() {
        final SimpleAdapter adapter =
                ZonePicker.constructTimezoneAdapter(RuntimeEnvironment.application, true);
        assertThat(adapter).isNotNull();

        ViewGroup parent = new FrameLayout(RuntimeEnvironment.application);
        ViewGroup convertView = new FrameLayout(RuntimeEnvironment.application);
        TextView text1 = new TextView(RuntimeEnvironment.application);
        text1.setId(android.R.id.text1);
        convertView.addView(text1);
        TextView text2 = new TextView(RuntimeEnvironment.application);
        text2.setId(android.R.id.text2);
        convertView.addView(text2);

        adapter.getView(0, convertView, parent);
        final CharSequence text = text2.getText();
        assertThat(text).isInstanceOf(Spanned.class);
        final TtsSpan[] spans = ((Spanned) text).getSpans(0, text.length(), TtsSpan.class);
        // GMT offset label should have TTS spans
        assertThat(spans.length).isGreaterThan(0);
    }
}
