/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.settings.indexer;

import java.util.Locale;

/**
 * Indexable Data.
 *
 * This is the raw data used by the Indexer and should match its data model.
 *
 * See {@link Indexable} and {@link IndexableRef}.
 */
public class IndexableData {

    public Locale locale;

    public String title;
    public String summary;
    public String keywords;

    public String intentAction;
    public String intentTargetPackage;
    public String intentTargetClass;

    public String fragmentTitle;

    public IndexableData() {
        locale = Locale.getDefault();
    }
}
