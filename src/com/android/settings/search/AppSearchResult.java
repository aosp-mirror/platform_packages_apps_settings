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
 *
 */

package com.android.settings.search;

import android.content.pm.ApplicationInfo;
import android.os.UserHandle;

public class AppSearchResult extends SearchResult {
    /**
     * Installed app's ApplicationInfo for delayed loading of icons
     */
    public final ApplicationInfo info;

    public AppSearchResult(Builder builder) {
        super(builder);
        info = builder.mInfo;
    }

    public UserHandle getAppUserHandle() {
        return new UserHandle(UserHandle.getUserId(info.uid));
    }

    public static class Builder extends SearchResult.Builder {
        protected ApplicationInfo mInfo;

        public SearchResult.Builder setAppInfo(ApplicationInfo info) {
            mInfo = info;
            return this;
        }

        public AppSearchResult build() {
            return new AppSearchResult(this);
        }
    }
}
