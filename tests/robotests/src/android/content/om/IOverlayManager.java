/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package android.content.om;

import android.os.IBinder;

import java.util.ArrayList;
import java.util.LinkedList;

public interface IOverlayManager {

    public OverlayInfo getOverlayInfo(String packageName, int userId);

    public java.util.List getOverlayInfosForTarget(java.lang.String targetPackageName, int userId);

    public boolean setEnabled(java.lang.String packageName, boolean enable, int userId);

    public static class Stub {
        public static IOverlayManager asInterface(IBinder b) {
            return null;
        }
    }
}
