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

package com.android.settings.bluetooth;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import android.content.Context;

import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;

import java.util.List;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class BluetoothSettingsObsoleteTest {
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    private BluetoothSettingsObsolete mFragment;
    private FakeFeatureFactory mFeatureFactory;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        FakeFeatureFactory.setupForTest(mContext);
        mFeatureFactory = (FakeFeatureFactory) FakeFeatureFactory.getFactory(mContext);

        mFragment = spy(new BluetoothSettingsObsolete());
        doReturn(mContext).when(mFragment).getContext();
    }

    @Test
    public void testSearchIndexProvider_pairPageEnabled_keyAdded() {
        doReturn(true).when(mFeatureFactory.bluetoothFeatureProvider).isPairingPageEnabled();

        final List<String> keys = mFragment.SEARCH_INDEX_DATA_PROVIDER.getNonIndexableKeys(
                mContext);

        assertThat(keys).contains(BluetoothSettingsObsolete.DATA_KEY_REFERENCE);
    }

    @Test
    public void testSearchIndexProvider_pairPageDisabled_keyNotAdded() {
        final List<String> keys = mFragment.SEARCH_INDEX_DATA_PROVIDER.getNonIndexableKeys(
                mContext);

        assertThat(keys).doesNotContain(BluetoothSettingsObsolete.DATA_KEY_REFERENCE);
    }

}
