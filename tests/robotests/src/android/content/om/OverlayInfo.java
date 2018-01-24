/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package android.content.om;

import android.annotation.NonNull;
import android.annotation.Nullable;

public class OverlayInfo {

    public final String packageName;

    public final String category;

    public OverlayInfo(@NonNull String packageName, @NonNull String targetPackageName,
            @Nullable String category, @NonNull String baseCodePath, int state, int userId) {
        this.packageName = packageName;
        this.category = category;
    }

    public boolean isEnabled() {
        return false;
    }

}