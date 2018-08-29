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

package com.android.settings.applications.assist;

import android.content.ContentResolver;
import android.database.ContentObserver;
import android.net.Uri;
import android.provider.Settings;

import androidx.annotation.MainThread;

import com.android.settingslib.utils.ThreadUtils;

import java.util.List;

public abstract class AssistSettingObserver extends ContentObserver {

    private final Uri ASSIST_URI =
            Settings.Secure.getUriFor(Settings.Secure.ASSISTANT);

    public AssistSettingObserver() {
        super(null /* handler */);
    }

    public void register(ContentResolver cr, boolean register) {
        if (register) {
            cr.registerContentObserver(ASSIST_URI, false, this);
            final List<Uri> settingUri = getSettingUris();
            if (settingUri != null) {
                for (Uri uri : settingUri) {
                    cr.registerContentObserver(uri, false, this);
                }
            }
        } else {
            cr.unregisterContentObserver(this);
        }
    }

    @Override
    public void onChange(boolean selfChange, Uri uri) {
        super.onChange(selfChange, uri);
        boolean shouldUpdatePreference = false;
        final List<Uri> settingUri = getSettingUris();
        if (ASSIST_URI.equals(uri) || (settingUri != null && settingUri.contains(uri))) {
            shouldUpdatePreference = true;
        }
        if (shouldUpdatePreference) {
            ThreadUtils.postOnMainThread(() -> {
                onSettingChange();
            });

        }
    }

    protected abstract List<Uri> getSettingUris();

    @MainThread
    public abstract void onSettingChange();
}
