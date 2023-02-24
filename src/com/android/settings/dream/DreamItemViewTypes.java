/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.dream;

import android.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Class representing a dream item view types.
 */
public final class DreamItemViewTypes {

    /**
     * The default dream item layout
     */
    public static final int DREAM_ITEM = 0;

    /**
     * The dream item layout indicating no dream item selected.
     */
    public static final int NO_DREAM_ITEM = 1;


    @Retention(RetentionPolicy.SOURCE)
    @IntDef({DreamItemViewTypes.DREAM_ITEM, DreamItemViewTypes.NO_DREAM_ITEM})
    public @interface ViewType {}
}
