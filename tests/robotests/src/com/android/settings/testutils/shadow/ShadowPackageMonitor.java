/*
 * Copyright (C) 2016 The Android Open Source Project
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

import static org.robolectric.util.ReflectionHelpers.ClassParameter.from;

import android.content.Context;
import android.os.Looper;
import android.os.UserHandle;

import com.android.internal.content.PackageMonitor;
import com.android.internal.os.BackgroundThread;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.RealObject;
import org.robolectric.internal.Shadow;
import org.robolectric.internal.ShadowExtractor;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowMessageQueue;

/*
 * Shadow for hidden {@link PackageMonitor}.
 */
@Implements(value = PackageMonitor.class, isInAndroidSdk = false)
public class ShadowPackageMonitor {

    @RealObject
    private PackageMonitor mPackageMonitor;

    @Implementation
    public void register(Context context, Looper thread, UserHandle user, boolean externalStorage) {
        // Call through to @RealObject's method.
        Shadow.directlyOn(mPackageMonitor, PackageMonitor.class, "register",
                from(Context.class, context), from(Looper.class, thread),
                from(UserHandle.class, user), from(Boolean.TYPE, externalStorage));
        // When <code>thread</code> is null, the {@link BackgroundThread} is used. Here we have to
        // setup background Robolectric scheduler for it.
        if (thread == null) {
            setupBackgroundThreadScheduler();
        }
    }

    private static void setupBackgroundThreadScheduler() {
        ShadowMessageQueue shadowMessageQueue = ((ShadowMessageQueue) ShadowExtractor.extract(
                BackgroundThread.getHandler().getLooper().getQueue()));
        shadowMessageQueue.setScheduler(
                ShadowApplication.getInstance().getBackgroundThreadScheduler());
    }
}
