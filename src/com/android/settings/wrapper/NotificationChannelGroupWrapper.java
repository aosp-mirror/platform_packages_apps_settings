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

package com.android.settings.wrapper;

import android.app.NotificationChannelGroup;

/**
 * Wrapper for {@link NotificationChannelGroup} until roboletric supports O MR1.
 */
public class NotificationChannelGroupWrapper {

    private final NotificationChannelGroup mGroup;

    public NotificationChannelGroupWrapper(NotificationChannelGroup group) {
        mGroup = group;
    }

    /**
     * Get the real group object so we can call APIs directly on it.
     */
    public NotificationChannelGroup getGroup() {
        return mGroup;
    }

    public String getDescription() {
        if (mGroup != null) {
            return mGroup.getDescription();
        }
        return null;
    }

    public void setDescription(String desc) {
        if (mGroup != null) {
            mGroup.setDescription(desc);
        }
    }

    public boolean isBlocked() {
        if (mGroup != null) {
            return mGroup.isBlocked();
        }
        return true;
    }

    public void setBlocked(boolean blocked) {
        if (mGroup != null) {
            mGroup.setBlocked(blocked);
        }
    }
}