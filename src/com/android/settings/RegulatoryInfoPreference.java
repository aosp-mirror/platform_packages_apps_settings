/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.settings;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;

/**
 * {@link DialogPreference} that displays regulatory information. "About phone"
 * will show a "Regulatory information" preference if
 * R.bool.config_show_regulatory_info is true.
 */
public class RegulatoryInfoPreference extends DialogPreference {

    public RegulatoryInfoPreference(Context context, AttributeSet attrs) {
        super(context, attrs, com.android.internal.R.attr.preferenceStyle);
        setDialogLayoutResource(R.layout.regulatory_info);
    }
}
