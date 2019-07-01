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

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.os.storage.VolumeInfo;
import android.widget.Button;

import com.android.settings.R;
import com.android.settings.testutils.shadow.ShadowStorageManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.androidx.fragment.FragmentController;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowStorageManager.class)
public class PrivateVolumeFormatTest {

    private PrivateVolumeFormat mFragment;
    private ShadowActivity mShadowActivity;

    @Before
    public void setUp() {
        final Bundle bundle = new Bundle();
        bundle.putString(VolumeInfo.EXTRA_VOLUME_ID, "id");
        mFragment = FragmentController.of(new PrivateVolumeFormat(), bundle)
                .create()
                .start()
                .resume()
                .visible()
                .get();
        mShadowActivity = Shadows.shadowOf(mFragment.getActivity());
    }

    @Test
    public void OnClickListener_shouldStartStorageWizardFormatProgress() {
        final Button confirm = mFragment.getView().findViewById(R.id.confirm);

        confirm.performClick();
        final Intent intent = mShadowActivity.getNextStartedActivity();

        assertThat(intent.getComponent()).isEqualTo(
                new ComponentName(RuntimeEnvironment.application.getPackageName(),
                        StorageWizardFormatProgress.class.getName()));
    }
}