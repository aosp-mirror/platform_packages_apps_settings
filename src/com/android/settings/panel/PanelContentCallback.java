/*
 * Copyright (C) 2020 The Android Open Source Project
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
 * PanelContentCallback provides a callback interface for {@link PanelFragment} to receive
 * events from {@link PanelContent}.
 *
 * @deprecated this is no longer used after V and will be removed.
 */
@Deprecated(forRemoval = true)
public interface PanelContentCallback {

    /**
     * It will be called when customized button state is changed. For example, custom button
     * would be hidden for specific behavior.
     */
    void onCustomizedButtonStateChanged();

    /**
     * It will be called when header content is changed. For example, to add/remove a device into
     * a group
     */
    void onHeaderChanged();

    /**
     * It will be called when panel requests to close itself.
     */
    void forceClose();

    /**
     * It will be called when panel requests to change the title.
     */
    void onTitleChanged();

    /**
     * It will be called when panel requests to change the progress bar visibility.
     */
    void onProgressBarVisibleChanged();
}
