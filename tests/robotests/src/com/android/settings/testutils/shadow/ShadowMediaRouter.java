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

package com.android.settings.testutils.shadow;

import static org.robolectric.RuntimeEnvironment.application;

import android.media.MediaRouter;

import org.robolectric.annotation.Implements;
import org.robolectric.shadow.api.Shadow;

import java.util.concurrent.CopyOnWriteArrayList;

@Implements(value = MediaRouter.class)
public class ShadowMediaRouter extends org.robolectric.shadows.ShadowMediaRouter {
    MediaRouter.RouteInfo mSelectedRoute;

    final CopyOnWriteArrayList<MediaRouter.Callback> mCallbacks =
            new CopyOnWriteArrayList<>();

    public MediaRouter.RouteInfo getSelectedRoute(int type) {
        return mSelectedRoute;
    }

    public void addCallback(int types, MediaRouter.Callback cb) {
        mCallbacks.add(cb);
    }

    public void removeCallback(MediaRouter.Callback cb) {
        if (mCallbacks.contains(cb)) {
            mCallbacks.remove(cb);
        }
    }

    public static ShadowMediaRouter getShadow() {
        return Shadow.extract(application.getSystemService(MediaRouter.class));
    }
}
