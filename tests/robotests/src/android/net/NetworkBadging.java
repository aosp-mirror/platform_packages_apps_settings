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
package android.net;

import android.annotation.IntDef;
import android.annotation.Nullable;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Implementation for {@link android.net.NetworkBadging}.
 *
 * <p>Can be removed once Robolectric supports Android O.
 */
public class NetworkBadging {
    @IntDef({BADGING_NONE, BADGING_SD, BADGING_HD, BADGING_4K})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Badging {}

    public static final int BADGING_NONE = 0;
    public static final int BADGING_SD = 10;
    public static final int BADGING_HD = 20;
    public static final int BADGING_4K = 30;

    private static Drawable drawable;

    public static Drawable getWifiIcon(
            int signalLevel, @NetworkBadging.Badging int badging, @Nullable Resources.Theme theme) {
        return new ColorDrawable(Color.GREEN);
    }
}
