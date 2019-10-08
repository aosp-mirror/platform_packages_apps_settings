/*
 * Copyright (C) 2019 The Android Open Source Project
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
 *
 */
package com.android.settings.panel;

import static com.android.settings.media.MediaOutputSlice.MEDIA_PACKAGE_NAME;

import static com.google.common.truth.Truth.assertThat;

import android.net.Uri;

import com.android.settings.slices.CustomSliceRegistry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class MediaOutputPanelTest {

    private static final String TEST_PACKAGENAME = "com.test.packagename";

    private MediaOutputPanel mPanel;

    @Before
    public void setUp() {
        mPanel = MediaOutputPanel.create(RuntimeEnvironment.application, TEST_PACKAGENAME);
    }

    @Test
    public void getSlices_containsNecessarySlices() {
        final List<Uri> uris = mPanel.getSlices();

        assertThat(uris).containsExactly(CustomSliceRegistry.MEDIA_OUTPUT_SLICE_URI);
    }

    @Test
    public void getSlices_verifyPackageName_isEqual() {
        final List<Uri> uris = mPanel.getSlices();

        assertThat(uris.get(0).getQueryParameter(MEDIA_PACKAGE_NAME)).isEqualTo(TEST_PACKAGENAME);
    }

    @Test
    public void getSeeMoreIntent_isNull() {
        assertThat(mPanel.getSeeMoreIntent()).isNull();
    }
}
