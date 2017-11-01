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

import android.app.Activity;
import android.content.Context;
import android.nfc.NfcAdapter;
import android.os.Bundle;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.Resetter;
import org.robolectric.util.ReflectionHelpers;
import org.robolectric.util.ReflectionHelpers.ClassParameter;

/**
 * Shadow of {@link NfcAdapter}.
 */
@Implements(NfcAdapter.class)
public class ShadowNfcAdapter {
    private static boolean sReaderModeEnabled;

    @Implementation
    public void enableReaderMode(Activity activity, NfcAdapter.ReaderCallback callback, int flags,
            Bundle extras) {
        sReaderModeEnabled = true;
    }

    @Implementation
    public static NfcAdapter getDefaultAdapter(Context context) {
        return ReflectionHelpers.callConstructor(
                NfcAdapter.class, ClassParameter.from(Context.class, context));
    }

    public static boolean isReaderModeEnabled() {
        return sReaderModeEnabled;
    }

    @Resetter
    public static void reset() {
        sReaderModeEnabled = false;
    }
}