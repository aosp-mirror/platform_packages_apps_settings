/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.android.settings.applications;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.spy;

import android.content.Context;
import android.provider.SearchIndexableResource;

import com.android.settings.R;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.List;

@RunWith(SettingsRobolectricTestRunner.class)
public class SpecialAccessSettingsTest {

  private Context mContext;
  private SpecialAccessSettings mFragment;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    mContext = spy(RuntimeEnvironment.application);
    mFragment = new SpecialAccessSettings() {
      @Override
      public Context getContext() {
        return mContext;
      }
    };
  }

  @Test
  public void testSearchIndexProvider_shouldIndexResource() {
    final List<SearchIndexableResource> indexRes =
            SpecialAccessSettings.SEARCH_INDEX_DATA_PROVIDER.getXmlResourcesToIndex(mContext,
                    true /* enabled */);
    final List<String> niks =
            SpecialAccessSettings.SEARCH_INDEX_DATA_PROVIDER.getNonIndexableKeys(mContext);

    assertThat(indexRes).isNotNull();
    assertThat(indexRes.get(0).xmlResId).isEqualTo(R.xml.special_access);
    assertThat(niks).isEmpty();
  }

  @Test
  @Config(qualifiers = "mcc999")
  public void testSearchIndexProvider_ifElementsAreNotShown_shouldNotBeIndexed() {
    final List<String> niks =
            SpecialAccessSettings.SEARCH_INDEX_DATA_PROVIDER.getNonIndexableKeys(mContext);

    assertThat(niks).contains(HighPowerAppsController.KEY_HIGH_POWER_APPS);
    assertThat(niks).contains(DeviceAdministratorsController.KEY_DEVICE_ADMIN);
    assertThat(niks).contains(PremiumSmsController.KEY_PREMIUM_SMS);
    assertThat(niks).contains(DataSaverController.KEY_DATA_SAVER);
    assertThat(niks).contains(EnabledVrListenersController.KEY_ENABLED_VR_LISTENERS);
  }
}