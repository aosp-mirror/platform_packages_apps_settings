/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.connecteddevice.audiosharing.audiostreams;

import static com.android.settings.connecteddevice.audiosharing.audiostreams.AudioStreamsQrCodeScanFragment.SHOW_ERROR_MESSAGE_INTERVAL;
import static com.android.settings.connecteddevice.audiosharing.audiostreams.AudioStreamsQrCodeScanFragment.SHOW_SUCCESS_SQUARE_INTERVAL;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.view.TextureView;
import android.view.View;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.connecteddevice.audiosharing.audiostreams.testshadows.ShadowAudioStreamsHelper;
import com.android.settings.connecteddevice.audiosharing.audiostreams.testshadows.ShadowQrCamera;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.qrcode.QrCamera;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;
import org.robolectric.shadows.androidx.fragment.FragmentController;

import java.util.concurrent.TimeUnit;

@RunWith(RobolectricTestRunner.class)
@Config(
        shadows = {
            ShadowAudioStreamsHelper.class,
            ShadowQrCamera.class,
        })
public class AudioStreamsQrCodeScanFragmentTest {
    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    private static final String VALID_METADATA =
            "BLUETOOTH:UUID:184F;BN:VGVzdA==;AT:1;AD:00A1A1A1A1A1;BI:1E240;BC:VGVzdENvZGU=;"
                    + "MD:BgNwVGVzdA==;AS:1;PI:A0;NS:1;BS:3;NB:2;SM:BQNUZXN0BARlbmc=;;";
    private static final String DEVICE_NAME = "device_name";
    @Mock private CachedBluetoothDevice mDevice;
    @Mock private QrCamera mQrCamera;
    @Mock private SurfaceTexture mSurfaceTexture;
    private Context mContext;
    private AudioStreamsQrCodeScanFragment mFragment;

    @Before
    public void setUp() {
        ShadowAudioStreamsHelper.setCachedBluetoothDeviceInSharingOrLeConnected(mDevice);
        ShadowQrCamera.setUseMock(mQrCamera);
        when(mDevice.getName()).thenReturn(DEVICE_NAME);
        mContext = ApplicationProvider.getApplicationContext();
        mFragment = new AudioStreamsQrCodeScanFragment();
    }

    @After
    public void tearDown() {
        ShadowAudioStreamsHelper.reset();
        ShadowQrCamera.reset();
    }

    @Test
    public void getMetricsCategory_returnEnum() {
        assertThat(mFragment.getMetricsCategory())
                .isEqualTo(SettingsEnums.AUDIO_STREAM_QR_CODE_SCAN);
    }

    @Test
    public void onCreateView_createLayout() {
        FragmentController.setupFragment(
                mFragment, FragmentActivity.class, /* containerViewId= */ 0, /* bundle= */ null);
        ShadowLooper.idleMainLooper();
        View view = mFragment.getView();

        assertThat(view).isNotNull();
        TextureView textureView = view.findViewById(R.id.preview_view);
        assertThat(textureView).isNotNull();
        assertThat(textureView.getSurfaceTextureListener()).isNotNull();
        assertThat(textureView.getOutlineProvider()).isNotNull();
        assertThat(textureView.getClipToOutline()).isTrue();

        TextView errorMessage = view.findViewById(R.id.error_message);
        assertThat(errorMessage).isNotNull();
        assertThat(errorMessage.getText().toString()).isEqualTo("");

        TextView summary = view.findViewById(android.R.id.summary);
        assertThat(summary).isNotNull();
        assertThat(summary.getText().toString())
                .isEqualTo(
                        mContext.getString(
                                R.string.audio_streams_main_page_qr_code_scanner_summary,
                                DEVICE_NAME));
    }

    @Test
    public void surfaceTextureListener_startAndStopQrCamera() {
        FragmentController.setupFragment(
                mFragment, FragmentActivity.class, /* containerViewId= */ 0, /* bundle= */ null);
        ShadowLooper.idleMainLooper();
        View view = mFragment.getView();

        assertThat(view).isNotNull();
        TextureView textureView = view.findViewById(R.id.preview_view);
        assertThat(textureView).isNotNull();
        TextureView.SurfaceTextureListener listener = textureView.getSurfaceTextureListener();

        assertThat(listener).isNotNull();
        listener.onSurfaceTextureAvailable(mSurfaceTexture, 50, 50);
        verify(mQrCamera).start(any());

        listener.onSurfaceTextureSizeChanged(mSurfaceTexture, 150, 150);
        listener.onSurfaceTextureUpdated(mSurfaceTexture);
        listener.onSurfaceTextureDestroyed(mSurfaceTexture);
        verify(mQrCamera).stop();

        mFragment.handleCameraFailure();
        verify(mQrCamera).stop();
    }

    @Test
    public void scannerCallback_sendSuccessMessage() {
        FragmentController.setupFragment(
                mFragment, FragmentActivity.class, /* containerViewId= */ 0, /* bundle= */ null);
        View view = mFragment.getView();
        ShadowLooper.idleMainLooper();

        assertThat(view).isNotNull();
        TextureView textureView = view.findViewById(R.id.preview_view);
        TextView errorMessage = view.findViewById(R.id.error_message);

        mFragment.handleSuccessfulResult("qrcode");
        ShadowLooper.idleMainLooper(SHOW_SUCCESS_SQUARE_INTERVAL, TimeUnit.MILLISECONDS);

        assertThat(textureView).isNotNull();
        assertThat(textureView.getVisibility()).isEqualTo(View.INVISIBLE);
        assertThat(errorMessage).isNotNull();
        assertThat(errorMessage.getVisibility()).isEqualTo(View.INVISIBLE);
    }

    @Test
    public void scannerCallback_isValid() {
        Boolean result = mFragment.isValid(VALID_METADATA);
        assertThat(result).isTrue();
    }

    @Test
    public void scannerCallback_isInvalid_showErrorThenHide() {
        FragmentController.setupFragment(
                mFragment, FragmentActivity.class, /* containerViewId= */ 0, /* bundle= */ null);
        Boolean result = mFragment.isValid("invalid");
        assertThat(result).isFalse();

        ShadowLooper.idleMainLooper();
        View view = mFragment.getView();
        assertThat(view).isNotNull();
        TextView errorMessage = view.findViewById(R.id.error_message);
        assertThat(errorMessage).isNotNull();
        assertThat(errorMessage.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(errorMessage.getText().toString())
                .isEqualTo(mContext.getString(R.string.audio_streams_qr_code_is_not_valid_format));

        ShadowLooper.idleMainLooper(SHOW_ERROR_MESSAGE_INTERVAL, TimeUnit.MILLISECONDS);
        assertThat(errorMessage.getVisibility()).isEqualTo(View.INVISIBLE);
    }

    @Test
    public void getViewSize_getSize() {
        FragmentController.setupFragment(
                mFragment, FragmentActivity.class, /* containerViewId= */ 0, /* bundle= */ null);
        ShadowLooper.idleMainLooper();
        View view = mFragment.getView();
        assertThat(view).isNotNull();
        TextureView textureView = view.findViewById(R.id.preview_view);
        assertThat(textureView).isNotNull();

        var result = mFragment.getViewSize();
        assertThat(result.getWidth()).isEqualTo(textureView.getWidth());
        assertThat(result.getHeight()).isEqualTo(textureView.getHeight());
    }
}
