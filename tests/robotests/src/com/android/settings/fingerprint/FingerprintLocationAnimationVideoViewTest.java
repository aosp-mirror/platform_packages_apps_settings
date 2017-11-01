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
 *
 */

package com.android.settings.fingerprint;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.net.Uri;
import android.view.TextureView.SurfaceTextureListener;

import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class FingerprintLocationAnimationVideoViewTest {

    private Context mContext;
    private FingerprintLocationAnimationVideoView mView;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = ShadowApplication.getInstance().getApplicationContext();
        mView = spy(new FingerprintLocationAnimationVideoView(mContext, null));
    }

    @Test
    public void onSurfaceTextureAvailable_nullMediaPlayer_shouldNotCrash() {
        mView.onFinishInflate();
        final SurfaceTextureListener listener = mView.getSurfaceTextureListener();
        when(mView.createMediaPlayer(any(Context.class), any(Uri.class))).thenReturn(null);

        listener.onSurfaceTextureAvailable(mock(SurfaceTexture.class), 48, 48);
        // should not crash
    }
}