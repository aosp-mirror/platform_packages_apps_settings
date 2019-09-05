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

package com.android.settings.wifi.qrcode;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.util.Size;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.WriterException;
import com.google.zxing.common.HybridBinarizer;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class QrCameraTest {

    @Mock
    private SurfaceTexture mSurfaceTexture;

    private QrCamera mCamera;
    private Context mContext;

    private String mQrCode;
    CountDownLatch mCallbackSignal;
    private boolean mCameraCallbacked;

    private class ScannerTestCallback implements QrCamera.ScannerCallback {
        @Override
        public Size getViewSize() {
            return new Size(0, 0);
        }

        @Override
        public Rect getFramePosition(Size previewSize, int cameraOrientation) {
            return new Rect(0,0,0,0);
        }

        @Override
        public void handleSuccessfulResult(String qrCode) {
            mQrCode = qrCode;
        }

        @Override
        public void handleCameraFailure() {
            mCameraCallbacked = true;
            mCallbackSignal.countDown();
        }

        @Override
        public void setTransform(Matrix transform) {
            // Do nothing
        }

        @Override
        public boolean isValid(String qrCode) {
            return true;
        }
    }

    private ScannerTestCallback mScannerCallback;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mScannerCallback = new ScannerTestCallback();
        mCamera = new QrCamera(mContext, mScannerCallback);
        mSurfaceTexture = mock(SurfaceTexture.class);
        mQrCode = "";
        mCameraCallbacked = false;
        mCallbackSignal = null;
    }

    @Test
    public void testCamera_Init_Callback() throws InterruptedException {
        mCallbackSignal = new CountDownLatch(1);
        mCamera.start(mSurfaceTexture);
        mCallbackSignal.await(5000, TimeUnit.MILLISECONDS);
        assertThat(mCameraCallbacked).isTrue();
    }

    @Test
    public void testDecode_PictureCaptured_QrCodeCorrectValue() {
        final String googleUrl = "http://www.google.com";

        try {
            final Bitmap bmp = QrCodeGenerator.encodeQrCode(googleUrl, 320);
            final int[] intArray = new int[bmp.getWidth() * bmp.getHeight()];
            bmp.getPixels(intArray, 0, bmp.getWidth(), 0, 0, bmp.getWidth(), bmp.getHeight());
            LuminanceSource source = new RGBLuminanceSource(bmp.getWidth(), bmp.getHeight(),
                    intArray);
            final BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
            mCamera.decodeImage(bitmap);
            bmp.recycle();
        } catch (WriterException e) {
        }

        assertThat(mQrCode).isEqualTo(googleUrl);
    }

    @Test
    public void testDecode_unicodePictureCaptured_QrCodeCorrectValue() {
        final String unicodeTest = "中文測試";

        try {
            final Bitmap bmp = QrCodeGenerator.encodeQrCode(unicodeTest, 320);
            final int[] intArray = new int[bmp.getWidth() * bmp.getHeight()];
            bmp.getPixels(intArray, 0, bmp.getWidth(), 0, 0, bmp.getWidth(), bmp.getHeight());
            LuminanceSource source = new RGBLuminanceSource(bmp.getWidth(), bmp.getHeight(),
                    intArray);
            final BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
            mCamera.decodeImage(bitmap);
            bmp.recycle();
        } catch (WriterException e) {
        }

        assertThat(mQrCode).isEqualTo(unicodeTest);
    }
}
