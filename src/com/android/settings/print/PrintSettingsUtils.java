/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.settings.print;

import android.content.ComponentName;
import android.content.Context;
import android.provider.Settings;
import android.text.TextUtils;
import android.text.TextUtils.SimpleStringSplitter;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper methods for reading and writing to print settings.
 */
public class PrintSettingsUtils {

    private static final char DISABLED_PRINT_SERVICES_SEPARATOR = ':';

    private PrintSettingsUtils() {
        /* do nothing */
    }

    public static List<ComponentName> readDisabledPrintServices(Context context) {
        List<ComponentName> disabledServices = new ArrayList<ComponentName>();

        String disabledServicesSetting = Settings.Secure.getString(context
                .getContentResolver(), Settings.Secure.DISABLED_PRINT_SERVICES);
        if (TextUtils.isEmpty(disabledServicesSetting)) {
            return disabledServices;
        }

        SimpleStringSplitter colonSplitter = new SimpleStringSplitter(
                DISABLED_PRINT_SERVICES_SEPARATOR);
        colonSplitter.setString(disabledServicesSetting);

        while (colonSplitter.hasNext()) {
            String componentNameString = colonSplitter.next();
            ComponentName disabledService = ComponentName.unflattenFromString(
                    componentNameString);
            if (disabledService != null) {
                disabledServices.add(disabledService);
            }
        }

        return disabledServices;
    }

    public static void writeDisabledPrintServices(Context context,
            List<ComponentName> services) {
        StringBuilder builder = new StringBuilder();
        final int serviceCount = services.size();
        for (int i = 0; i < serviceCount; i++) {
            ComponentName service = services.get(i);
            if (builder.length() > 0) {
                builder.append(DISABLED_PRINT_SERVICES_SEPARATOR);
            }
            builder.append(service.flattenToString());
        }
        Settings.Secure.putString(context.getContentResolver(),
                Settings.Secure.DISABLED_PRINT_SERVICES,
                builder.toString());
    }
}
