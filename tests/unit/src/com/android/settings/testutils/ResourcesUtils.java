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
package com.android.settings.testutils;

import android.content.Context;

/**
 * Test util to provide the correct resources.
 */
public final class ResourcesUtils {
    /**
     * Return a resource identifier for the given resource name.
     * @param context Context to use.
     * @param type Optional default resource type to find, if "type/" is not included in the name.
     *             Can be null to require an explicit type.
     * @param name The name of the desired resource.
     * @return The associated resource identifier. Returns 0 if no such resource was found.
     * (0 is not a valid resource ID.)
     */
    public static int getResourcesId(Context context, String type, String name) {
        return context.getResources().getIdentifier(name, type, context.getPackageName());
    }

    /**
     * Returns a localized string from the application's package's default string table.
     * @param context Context to use.
     * @param name The name of the desired resource.
     * @return The string data associated with the resource, stripped of styled text information.
     */
    public static String getResourcesString(Context context, String name) {
        return context.getResources().getString(getResourcesId(context, "string", name));
    }

    /**
     * Return the string value associated with a particular neame of resource,
     * substituting the format arguments as defined in {@link java.util.Formatter}
     * and {@link java.lang.String#format}. It will be stripped of any styled text
     * information.
     * @param context Context to use.
     * @param name The name of the desired resource.
     * @param value The format arguments that will be used for substitution.
     * @return The string data associated with the resource, stripped of styled text information.
     */
    public static String getResourcesString(Context context, String name, Object... value) {
        return context.getResources().getString(getResourcesId(context, "string", name), value);
    }
}
