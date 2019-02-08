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

package com.android.settings.panel;

import android.content.Intent;
import android.net.Uri;

import com.android.settingslib.core.instrumentation.Instrumentable;

import java.util.List;

/**
 * Represents the data class needed to create a Settings Panel. See {@link PanelFragment}.
 */
public interface PanelContent extends Instrumentable {

    /**
     * @return a string for the title of the Panel.
     */
    CharSequence getTitle();

    /**
     * @return an ordered list of the Slices to be displayed in the Panel. The first item in the
     * list is shown on top of the Panel.
     */
    List<Uri> getSlices();


    /**
     * @return an {@link Intent} to the full content in Settings that is summarized by the Panel.
     *
     * <p>
     *     For example, for the connectivity panel you would intent to the Network & Internet page.
     * </p>
     */
    Intent getSeeMoreIntent();
}
