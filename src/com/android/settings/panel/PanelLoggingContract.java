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
package com.android.settings.panel;

/**
 * Simple contract class to track keys in Panel logging.
 *
 * <p>
 *    Constants should only be removed if underlying panel, or use case is removed.
 * </p>
 */
public class PanelLoggingContract {

    /**
     * Keys tracking different ways users exit Panels.
     */
    interface PanelClosedKeys {
        /**
         * The user clicked the See More button linking deeper into Settings.
         */
        String KEY_SEE_MORE = "see_more";

        /**
         * The user clicked the Done button, closing the Panel.
         */
        String KEY_DONE = "done";

        /**
         * The user closed the panel by other ways, for example: clicked outside of dialog, tapping
         * on back button, etc.
         */
        String KEY_OTHERS = "others";
    }
}
