/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.gestures;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.os.SystemProperties;
import android.provider.SearchIndexableResource;
import android.provider.Settings;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.util.ReflectionHelpers;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class SwipeBottomToNotificationSettingsTest {
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;

    private SwipeBottomToNotificationSettings mSettings = new SwipeBottomToNotificationSettings();

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void getPreferenceScreenResId_shouldReturnsXml() {
        assertThat(mSettings.getPreferenceScreenResId())
                .isEqualTo(R.xml.swipe_bottom_to_notification_settings);
    }

    @Test
    public void searchIndexProvider_shouldIndexResource() {
        final List<SearchIndexableResource> indexRes =
                SwipeBottomToNotificationSettings.SEARCH_INDEX_DATA_PROVIDER.getXmlResourcesToIndex(
                        mContext, true /* enabled */);

        assertThat(indexRes.get(0).xmlResId).isEqualTo(mSettings.getPreferenceScreenResId());
    }

    @Test
    public void isPageSearchEnabled_oneHandedUnsupported_shouldReturnTrue() {
        SystemProperties.set(OneHandedEnablePreferenceController.SUPPORT_ONE_HANDED_MODE, "false");

        final Object obj = ReflectionHelpers.callInstanceMethod(
                SwipeBottomToNotificationSettings.SEARCH_INDEX_DATA_PROVIDER, "isPageSearchEnabled",
                ReflectionHelpers.ClassParameter.from(Context.class, mContext));

        final boolean isEnabled = (Boolean) obj;
        assertThat(isEnabled).isTrue();
    }

    @Test
    public void isPageSearchEnabled_oneHandedDisabled_shouldReturnTrue() {
        SystemProperties.set(OneHandedEnablePreferenceController.SUPPORT_ONE_HANDED_MODE, "true");
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ONE_HANDED_MODE_ENABLED, 0);

        final Object obj = ReflectionHelpers.callInstanceMethod(
                SwipeBottomToNotificationSettings.SEARCH_INDEX_DATA_PROVIDER, "isPageSearchEnabled",
                ReflectionHelpers.ClassParameter.from(Context.class, mContext));

        final boolean isEnabled = (Boolean) obj;
        assertThat(isEnabled).isTrue();
    }

    @Test
    public void isPageSearchEnabled_oneHandedEnabled_shouldReturnFalse() {
        SystemProperties.set(OneHandedEnablePreferenceController.SUPPORT_ONE_HANDED_MODE, "true");
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ONE_HANDED_MODE_ENABLED, 1);

        final Object obj = ReflectionHelpers.callInstanceMethod(
                SwipeBottomToNotificationSettings.SEARCH_INDEX_DATA_PROVIDER, "isPageSearchEnabled",
                ReflectionHelpers.ClassParameter.from(Context.class, mContext));

        final boolean isEnabled = (Boolean) obj;
        assertThat(isEnabled).isFalse();
    }
}
