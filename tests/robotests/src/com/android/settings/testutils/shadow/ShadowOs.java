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

package com.android.settings.testutils.shadow;

import static android.system.OsConstants.AF_INET;
import static android.system.OsConstants.AF_INET6;

import android.system.Os;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Pattern;

@Implements(Os.class)
public class ShadowOs {
    // These are not actually correct, but good enough for the test
    private static final Pattern IPV4_PATTERN =
        Pattern.compile("^\\d{1,3}(\\.\\d{1,3}){3}$");
    private static final Pattern IPV6_PATTERN =
        Pattern.compile("^[0-9a-f]{1,4}(:[0-9a-f]{0,4}){0,6}$");

    private static final byte[] IPV4_BYTES = new byte[4];
    private static final byte[] IPV6_BYTES = new byte[16];

    @Implementation
    protected static InetAddress inet_pton(int family, String address) {
        if ((AF_INET  == family && IPV4_PATTERN.matcher(address).find()) ||
            (AF_INET6 == family && IPV6_PATTERN.matcher(address).find())) {
            try {
                return InetAddress.getByAddress((AF_INET == family) ? IPV4_BYTES : IPV6_BYTES);
            } catch (UnknownHostException uhe) {
                // Shouldn't be reached.
            }
        }
        return null;
    }
}
