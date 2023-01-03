/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.regionalpreferences;

import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.os.LocaleList;

import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Locale;

public class NumberingSystemControllerTest {
    private Context mApplicationContext;
    private NumberingSystemController mController;
    private LocaleList mCacheLocales;

    @Before
    public void setUp() throws Exception {
        mApplicationContext = ApplicationProvider.getApplicationContext();
        mController = new NumberingSystemController(mApplicationContext, "key");
        mCacheLocales = LocaleList.getDefault();
    }


    @After
    public void tearDown() throws Exception {
        LocaleList.setDefault(mCacheLocales);
    }

    @Test
    public void getSummary_has1Locale_showEnUs() {
        LocaleList.setDefault(LocaleList.forLanguageTags("en-US"));

        String summary = mController.getSummary().toString();

        String expectedResult =
                Locale.forLanguageTag("en-us").getDisplayName();
        assertEquals(expectedResult, summary);
    }

    @Test
    public void getSummary_has2Locales_showEnUsAndZhTw() {
        LocaleList.setDefault(LocaleList.forLanguageTags("en-US,zh-TW"));

        String summary = mController.getSummary().toString();

        Locale locale1 = Locale.forLanguageTag("en-US");
        Locale locale2 = Locale.forLanguageTag("zh-TW");
        String expectedResult =
                locale1.getDisplayName(locale1) + ", " + locale2.getDisplayName(locale2);
        assertEquals(expectedResult, summary);
    }

    @Test
    public void getSummary_localeHasExtensionTag_showEnUsWithoutTag() {
        LocaleList.setDefault(LocaleList.forLanguageTags("en-US-u-ca-chinese"));

        String summary = mController.getSummary().toString();

        String expectedResult = Locale.forLanguageTag("en-US").getDisplayName();
        assertEquals(expectedResult, summary);
    }
}
