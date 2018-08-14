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
 * limitations under the License.
 */
package com.android.settings;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.spy;

import android.content.Context;

import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.XmlTestUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.List;

@RunWith(SettingsRobolectricTestRunner.class)
public class LegalSettingsTest {

    private Context mContext;
    private LegalSettings mFragment;
    private boolean mWallpaperRemoved;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        mFragment = new LegalSettings() {
            @Override
            public boolean removePreference(String key) {
                if (LegalSettings.KEY_WALLPAPER_ATTRIBUTIONS.equals(key)) {
                    mWallpaperRemoved = true;

                    return true;
                }
                return false;
            }
        };
    }

    @Test
    public void testNonIndexableKeys_existInXmlLayout() {
        final Context context = RuntimeEnvironment.application;
        final List<String> niks =
            LegalSettings.SEARCH_INDEX_DATA_PROVIDER.getNonIndexableKeys(context);

        final List<String> keys = XmlTestUtils.getKeysFromPreferenceXml(context, R.xml.about_legal);

        assertThat(keys).containsAllIn(niks);
    }

    @Test
    public void testWallpaperAttributions_byDefault_shouldBeShown() {
        mFragment.checkWallpaperAttributionAvailability(mContext);

        assertThat(mWallpaperRemoved).isEqualTo(false);
    }

    @Test
    @Config(qualifiers = "mcc999")
    public void testWallpaperAttributions_ifDisabled_shouldNotBeShown() {
        mFragment.checkWallpaperAttributionAvailability(mContext);

        assertThat(mWallpaperRemoved).isEqualTo(true);
    }
}
