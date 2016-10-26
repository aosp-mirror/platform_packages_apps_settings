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

import android.app.Activity;
import android.provider.Settings.System;
import com.android.settings.dashboard.SummaryLoader;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

import static android.provider.Settings.System.SCREEN_OFF_TIMEOUT;
import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class DisplaySettingsTest {

  private Activity mActivity;
  @Mock private SummaryLoader mSummaryLoader;
  private SummaryLoader.SummaryProvider mSummaryProvider;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    mActivity = Robolectric.buildActivity(Activity.class).get();
    mSummaryProvider = DisplaySettings.SUMMARY_PROVIDER_FACTORY.createSummaryProvider(
            mActivity, mSummaryLoader);
  }

  @Test
  public void testInvalidTimeouts_summaryShouldBeEmpty() {
    System.putLong(mActivity.getContentResolver(), SCREEN_OFF_TIMEOUT, -1);
    assertEquals(System.getLong(mActivity.getContentResolver(), SCREEN_OFF_TIMEOUT, 0), -1);
    mSummaryProvider.setListening(true);

    System.putLong(mActivity.getContentResolver(), SCREEN_OFF_TIMEOUT, 1234);
    mSummaryProvider.setListening(true);

    verify(mSummaryLoader, times(2)).setSummary(mSummaryProvider, "");
  }

  @Test
  public void testValidTimeouts_summaryShouldNotBeEmpty() {
    final CharSequence[] values =
            mActivity.getResources().getTextArray(R.array.screen_timeout_values);

    for (CharSequence value : values) {
      long timeout = Long.parseLong(value.toString());
      System.putLong(mActivity.getContentResolver(), SCREEN_OFF_TIMEOUT, timeout);
      assertEquals(System.getLong(mActivity.getContentResolver(), SCREEN_OFF_TIMEOUT, 0), timeout);
      mSummaryProvider.setListening(true);
    }

    verify(mSummaryLoader, never()).setSummary(mSummaryProvider, "");
  }
}
