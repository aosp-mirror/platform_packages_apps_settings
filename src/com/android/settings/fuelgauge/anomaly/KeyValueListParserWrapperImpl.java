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

package com.android.settings.fuelgauge.anomaly;

import android.util.KeyValueListParser;

/**
 * Impl of {@link KeyValueListParser}
 */
public class KeyValueListParserWrapperImpl implements KeyValueListParserWrapper {
    private KeyValueListParser mParser;

    public KeyValueListParserWrapperImpl(KeyValueListParser parser) {
        mParser = parser;
    }

    @Override
    public KeyValueListParser getKeyValueListParser() {
        return mParser;
    }

    @Override
    public void setString(String str) throws IllegalArgumentException {
        mParser.setString(str);
    }

    @Override
    public String getString(String key, String defaultValue) {
        return mParser.getString(key, defaultValue);
    }

    @Override
    public boolean getBoolean(String key, boolean defaultValue) {
        return mParser.getBoolean(key, defaultValue);
    }

    @Override
    public long getLong(String key, long defaultValue) {
        return mParser.getLong(key, defaultValue);
    }
}
