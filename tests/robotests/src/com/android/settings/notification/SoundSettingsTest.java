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

package com.android.settings.notification;

import android.content.Context;

import android.media.AudioManager;
import android.os.UserManager;
import com.android.settings.R;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.testutils.XmlTestUtils;
import com.android.settings.testutils.shadow.ShadowAudioHelper;
import com.android.settings.testutils.shadow.ShadowUserManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class SoundSettingsTest {

    @Test
    @Config( shadows = {
            ShadowUserManager.class,
            ShadowAudioHelper.class,
    })
    public void testNonIndexableKeys_existInXmlLayout() {
        final Context context = spy(RuntimeEnvironment.application);
        AudioManager audioManager = mock(AudioManager.class);
        doReturn(audioManager).when(context).getSystemService(Context.AUDIO_SERVICE);

        UserManager userManager = mock(UserManager.class);
        when(userManager.isAdminUser()).thenReturn(false);
        doReturn(userManager).when(context).getSystemService(Context.USER_SERVICE);

        final List<String> niks = SoundSettings.SEARCH_INDEX_DATA_PROVIDER
                .getNonIndexableKeys(context);
        final int xmlId = (new SoundSettings()).getPreferenceScreenResId();
        final List<String> keys = XmlTestUtils.getKeysFromPreferenceXml(context, xmlId);
        keys.addAll(XmlTestUtils.getKeysFromPreferenceXml(context,
                R.xml.zen_mode_settings));
        // Add keys with hidden resources
        keys.add("alarm_volume");
        keys.add("ring_volume");
        keys.add("notification_volume");

        assertThat(keys).containsAllIn(niks);
    }
}