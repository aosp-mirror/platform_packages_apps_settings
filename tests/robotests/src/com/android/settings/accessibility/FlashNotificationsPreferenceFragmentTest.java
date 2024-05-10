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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.app.settings.SettingsEnums;
import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settingslib.core.AbstractPreferenceController;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class FlashNotificationsPreferenceFragmentTest {

    @Rule
    public MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Spy
    private final Context mContext = ApplicationProvider.getApplicationContext();
    private FlashNotificationsPreferenceFragment mFragment;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mFragment = new FlashNotificationsPreferenceFragment();
    }

    @Test
    public void getPreferenceScreenResId_isFlashNotificationsSettings() {
        assertThat(mFragment.getPreferenceScreenResId())
                .isEqualTo(R.xml.flash_notifications_settings);
    }

    @Test
    public void getLogTag_isSpecifyTag() {
        assertThat(mFragment.getLogTag()).isEqualTo("FlashNotificationsPreferenceFragment");
    }

    @Test
    public void getMetricsCategory_returnsCorrectCategory() {
        assertThat(mFragment.getMetricsCategory()).isEqualTo(
                SettingsEnums.FLASH_NOTIFICATION_SETTINGS);
    }

    @Test
    public void onAttach_attachedTheTestFragment() {
        ScreenFlashNotificationPreferenceController controller = mock(
                ScreenFlashNotificationPreferenceController.class);
        TestFlashNotificationsPreferenceFragment testFragment =
                new TestFlashNotificationsPreferenceFragment(controller);
        testFragment.onAttach(mContext);
        verify(controller).setParentFragment(eq(testFragment));
    }

    @Test
    public void getHelpResource_isExpectedResource() {
        assertThat(mFragment.getHelpResource())
                .isEqualTo(R.string.help_url_flash_notifications);
    }

    static class TestFlashNotificationsPreferenceFragment extends
            FlashNotificationsPreferenceFragment {
        final AbstractPreferenceController mController;

        TestFlashNotificationsPreferenceFragment(AbstractPreferenceController controller) {
            mController = controller;
        }

        @Override
        protected <T extends AbstractPreferenceController> T use(Class<T> clazz) {
            return (T) mController;
        }
    }
}
