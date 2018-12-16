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

package com.android.settings.deviceinfo;

import static com.google.common.truth.Truth.assertThat;

import android.os.Bundle;
import android.os.storage.VolumeInfo;
import android.widget.Button;

import com.android.settings.R;
import com.android.settings.testutils.shadow.ShadowStorageManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.androidx.fragment.FragmentController;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowStorageManager.class)
public class PrivateVolumeUnmountTest {

    private PrivateVolumeUnmount mFragment;

    @Before
    public void setUp() {
        Bundle bundle = new Bundle();
        bundle.putString(VolumeInfo.EXTRA_VOLUME_ID, "id");
        mFragment = FragmentController.of(new PrivateVolumeUnmount(), bundle)
                .create()
                .start()
                .resume()
                .visible()
                .get();
    }

    @After
    public void tearDown() {
        ShadowStorageManager.reset();
    }

    @Test
    public void OnClickListener_shouldCallUnmount() {
        assertThat(ShadowStorageManager.isUnmountCalled()).isFalse();

        final Button confirm = mFragment.getView().findViewById(R.id.confirm);

        confirm.performClick();

        assertThat(ShadowStorageManager.isUnmountCalled()).isTrue();
    }
}