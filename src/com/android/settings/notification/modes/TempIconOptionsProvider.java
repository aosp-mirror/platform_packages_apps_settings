/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.notification.modes;

import com.google.common.collect.ImmutableList;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;

/** Temporary implementation of {@link IconOptionsProvider} until we have the real list. */
class TempIconOptionsProvider implements IconOptionsProvider {
    @Override
    public ImmutableList<IconInfo> getIcons() {
        return ImmutableList.copyOf(
                Arrays.stream(com.android.internal.R.drawable.class.getFields())
                        .filter(
                                f -> Modifier.isStatic(f.getModifiers())
                                        && Modifier.isFinal(f.getModifiers())
                                        && f.getName().startsWith("ic_"))
                        .limit(20)
                        .map(f -> {
                            try {
                                return new IconInfo(f.getInt(null), f.getName());
                            } catch (IllegalAccessException e) {
                                return null;
                            }
                        })
                        .filter(Objects::nonNull)
                        .sorted(Comparator.comparing(IconInfo::resId).reversed())
                        .toList());
    }
}
