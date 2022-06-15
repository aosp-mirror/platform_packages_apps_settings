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

package com.android.settings.testutils.shadow;

import android.content.Context;
import android.content.IContentProvider;
import android.net.Uri;
import android.os.Bundle;
import android.util.Pair;

import com.android.settings.R;
import com.android.settingslib.drawer.TileUtils;

import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import java.util.Map;

@Implements(TileUtils.class)
public class ShadowTileUtils {

    public static final String MOCK_SUMMARY = "summary";

    private static boolean sChecked;
    private static Bundle sResult;

    @Implementation
    protected static String getTextFromUri(Context context, Uri uri,
            Map<String, IContentProvider> providerMap, String key) {
        return MOCK_SUMMARY;
    }

    @Implementation
    protected static Pair<String, Integer> getIconFromUri(Context context, String packageName,
            Uri uri, Map<String, IContentProvider> providerMap) {
        return Pair.create(RuntimeEnvironment.application.getPackageName(), R.drawable.ic_settings_accent);
    }

    @Implementation
    public static boolean getBooleanFromUri(Context context, Uri uri,
            Map<String, IContentProvider> providerMap, String key) {
        return sChecked;
    }

    @Implementation
    public static Bundle putBooleanToUriAndGetResult(Context context, Uri uri,
            Map<String, IContentProvider> providerMap, String key, boolean value) {
        sChecked = value;
        return sResult;
    }

    public static boolean getProviderChecked() {
        return sChecked;
    }

    public static void setProviderChecked(boolean value) {
        sChecked = value;
    }

    public static void setResultBundle(Bundle result) {
        sResult = result;
    }
}
