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

package com.android.settings.accessibility;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.util.AttributeSet;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class FlashNotificationsPreviewPreferenceTest {

    @Rule
    public MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Spy
    private final Context mContext = ApplicationProvider.getApplicationContext();
    private final AttributeSet mAttributeSet = Robolectric.buildAttributeSet().build();

    @Test
    public void constructor_assertLayoutResource_P00() {
        FlashNotificationsPreviewPreference preference = new FlashNotificationsPreviewPreference(
                mContext);
        assertThat(preference.getLayoutResource())
                .isEqualTo(R.layout.flash_notification_preview_preference);
    }

    @Test
    public void constructor_assertLayoutResource_P01() {
        FlashNotificationsPreviewPreference preference = new FlashNotificationsPreviewPreference(
                mContext, mAttributeSet);
        assertThat(preference.getLayoutResource())
                .isEqualTo(R.layout.flash_notification_preview_preference);
    }

    @Test
    public void constructor_assertLayoutResource_P02() {
        FlashNotificationsPreviewPreference preference = new FlashNotificationsPreviewPreference(
                mContext, mAttributeSet, 0);
        assertThat(preference.getLayoutResource())
                .isEqualTo(R.layout.flash_notification_preview_preference);
    }

    @Test
    public void constructor_assertLayoutResource_P03() {
        FlashNotificationsPreviewPreference preference = new FlashNotificationsPreviewPreference(
                mContext, mAttributeSet, 0, 0);
        assertThat(preference.getLayoutResource())
                .isEqualTo(R.layout.flash_notification_preview_preference);
    }
}
