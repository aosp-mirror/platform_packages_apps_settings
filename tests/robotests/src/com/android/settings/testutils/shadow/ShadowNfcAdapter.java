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
 * limitations under the License
 */

package com.android.settings.testutils.shadow;

import static org.robolectric.shadow.api.Shadow.newInstanceOf;

import android.app.Activity;
import android.content.Context;
import android.nfc.NfcAdapter;
import android.os.Bundle;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.Resetter;

/**
 * Shadow of {@link NfcAdapter}.
 */
@Implements(value = NfcAdapter.class)
public class ShadowNfcAdapter extends org.robolectric.shadows.ShadowNfcAdapter {
    private static boolean sReaderModeEnabled;
    private static Object sNfcAdapter = newInstanceOf("android.nfc.NfcAdapter");

    private boolean mIsNfcEnabled = false;
    private int mState = NfcAdapter.STATE_ON;
    private boolean mIsSecureNfcSupported = false;

    @Implementation
    protected void enableReaderMode(Activity activity, NfcAdapter.ReaderCallback callback,
            int flags, Bundle extras) {
        sReaderModeEnabled = true;
    }

    @Implementation
    protected static NfcAdapter getDefaultAdapter(Context context) {
        return (NfcAdapter) sNfcAdapter;
    }

    @Implementation
    protected boolean isEnabled() {
        return mIsNfcEnabled;
    }

    public void setEnabled(boolean enable) {
        mIsNfcEnabled = enable;
    }

    @Implementation
    protected int getAdapterState() {
        return mState;
    }

    public void setAdapterState(int state) {
        this.mState = state;
    }

    @Implementation
    protected boolean isSecureNfcSupported() {
        return mIsSecureNfcSupported;
    }

    public void setSecureNfcSupported(boolean supported) {
        this.mIsSecureNfcSupported = supported;
    }

    @Implementation
    protected boolean enable() {
        mIsNfcEnabled = true;
        return true;
    }

    @Implementation
    protected boolean disable() {
        mIsNfcEnabled = false;
        return true;
    }

    public static boolean isReaderModeEnabled() {
        return sReaderModeEnabled;
    }

    @Resetter
    public static void reset() {
        sReaderModeEnabled = false;
    }
}