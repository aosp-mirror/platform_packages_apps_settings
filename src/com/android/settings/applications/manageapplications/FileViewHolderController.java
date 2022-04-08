/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.settings.applications.manageapplications;

import androidx.fragment.app.Fragment;

/**
 * FileViewHolderController handles adapting the AppViewHolder to work as a general purpose
 * storage categorization preference in the ManageApplications view.
 */
public interface FileViewHolderController {
    /**
     * Begins a synchronous query for statistics for the files.
     */
    void queryStats();

    /**
     * Returns if the preference should be shown.
     */
    boolean shouldShow();

    /**
     * Initializes the view within an AppViewHolder.
     *
     * @param holder The holder to use to initialize.
     */
    void setupView(ApplicationViewHolder holder);

    /**
     * Handles the behavior when the view is clicked.
     *
     * @param fragment Fragment where the click originated.
     */
    void onClick(Fragment fragment);
}
