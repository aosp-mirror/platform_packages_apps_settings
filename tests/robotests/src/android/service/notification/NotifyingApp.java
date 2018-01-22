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
package android.service.notification;

import android.annotation.NonNull;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

/**
 * Stub implementation of framework's NotifyingApp for Robolectric tests. Otherwise Robolectric
 * throws ClassNotFoundError.
 *
 * TODO: Remove this class when Robolectric supports P
 */
public final class NotifyingApp implements Comparable<NotifyingApp> {

    private int mUid;
    private String mPkg;
    private long mLastNotified;

    public NotifyingApp() {}

    public int getUid() {
        return mUid;
    }

    /**
     * Sets the uid of the package that sent the notification. Returns self.
     */
    public NotifyingApp setUid(int mUid) {
        this.mUid = mUid;
        return this;
    }

    public String getPackage() {
        return mPkg;
    }

    /**
     * Sets the package that sent the notification. Returns self.
     */
    public NotifyingApp setPackage(@NonNull String mPkg) {
        this.mPkg = mPkg;
        return this;
    }

    public long getLastNotified() {
        return mLastNotified;
    }

    /**
     * Sets the time the notification was originally sent. Returns self.
     */
    public NotifyingApp setLastNotified(long mLastNotified) {
        this.mLastNotified = mLastNotified;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NotifyingApp that = (NotifyingApp) o;
        return getUid() == that.getUid()
                && getLastNotified() == that.getLastNotified()
                && Objects.equals(mPkg, that.mPkg);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getUid(), mPkg, getLastNotified());
    }

    /**
     * Sorts notifying apps from newest last notified date to oldest.
     */
    @Override
    public int compareTo(NotifyingApp o) {
        if (getLastNotified() == o.getLastNotified()) {
            if (getUid() == o.getUid()) {
                return getPackage().compareTo(o.getPackage());
            }
            return Integer.compare(getUid(), o.getUid());
        }

        return -Long.compare(getLastNotified(), o.getLastNotified());
    }

    @Override
    public String toString() {
        return "NotifyingApp{"
                + "mUid=" + mUid
                + ", mPkg='" + mPkg + '\''
                + ", mLastNotified=" + mLastNotified
                + '}';
    }
}
