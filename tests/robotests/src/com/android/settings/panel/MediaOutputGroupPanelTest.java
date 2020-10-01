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

package com.android.settings.panel;

import static com.android.settings.media.MediaOutputSlice.MEDIA_PACKAGE_NAME;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.net.Uri;

import com.android.settings.slices.CustomSliceRegistry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class MediaOutputGroupPanelTest {

    private static final String TEST_PACKAGENAME = "com.test.packagename";

    private MediaOutputGroupPanel mPanel;
    private Context mContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = spy(RuntimeEnvironment.application);
        when(mContext.getApplicationContext()).thenReturn(mContext);
        mPanel = MediaOutputGroupPanel.create(mContext, TEST_PACKAGENAME);
    }

    @Test
    public void getSlices_containsNecessarySlices() {
        final List<Uri> uris = mPanel.getSlices();

        assertThat(uris).containsExactly(CustomSliceRegistry.MEDIA_OUTPUT_GROUP_SLICE_URI);
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

    @Test
    public void getViewType_checkType() {
        assertThat(mPanel.getViewType()).isEqualTo(PanelContent.VIEW_TYPE_SLIDER_LARGE_ICON);
    }
}
