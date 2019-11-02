/*
 * Copyright (C) 2019 The Android Open Source Project
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

import static org.robolectric.shadows.ShadowMediaPlayer.State.INITIALIZED;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowMediaPlayer;
import org.robolectric.shadows.util.DataSource;

@Implements(MediaPlayer.class)
public class ShadowSettingsMediaPlayer extends ShadowMediaPlayer {

    @Implementation
    public static MediaPlayer create(Context context, Uri uri) {
        final DataSource ds = DataSource.toDataSource(context, uri);
        addMediaInfo(ds, new ShadowMediaPlayer.MediaInfo());

        final MediaPlayer mp = new MediaPlayer();
        final ShadowMediaPlayer shadow = Shadow.extract(mp);
        try {
            shadow.setDataSource(ds);
            shadow.setState(INITIALIZED);
            mp.prepare();
        } catch (Exception e) {
            return null;
        }

        return mp;
    }
}
