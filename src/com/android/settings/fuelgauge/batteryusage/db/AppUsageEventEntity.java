/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.fuelgauge.batteryusage.db;

import android.content.ContentValues;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.android.settings.fuelgauge.batteryusage.ConvertUtils;

import com.google.errorprone.annotations.CanIgnoreReturnValue;

import java.util.Locale;

/** A {@link Entity} class to save app usage events into database. */
@Entity
public class AppUsageEventEntity {
    /** Keys for accessing {@link ContentValues}. */
    public static final String KEY_UID = "uid";

    public static final String KEY_USER_ID = "userId";
    public static final String KEY_TIMESTAMP = "timestamp";
    public static final String KEY_APP_USAGE_EVENT_TYPE = "appUsageEventType";
    public static final String KEY_PACKAGE_NAME = "packageName";
    public static final String KEY_INSTANCE_ID = "instanceId";
    public static final String KEY_TASK_ROOT_PACKAGE_NAME = "taskRootPackageName";

    @PrimaryKey(autoGenerate = true)
    private long mId;

    // Records the app relative information.
    public final long uid;
    public final long userId;
    public final long timestamp;
    public final int appUsageEventType;
    public final String packageName;
    public final int instanceId;
    public final String taskRootPackageName;

    public AppUsageEventEntity(
            final long uid,
            final long userId,
            final long timestamp,
            final int appUsageEventType,
            final String packageName,
            final int instanceId,
            final String taskRootPackageName) {
        this.uid = uid;
        this.userId = userId;
        this.timestamp = timestamp;
        this.appUsageEventType = appUsageEventType;
        this.packageName = packageName;
        this.instanceId = instanceId;
        this.taskRootPackageName = taskRootPackageName;
    }

    /** Sets the auto-generated content ID. */
    public void setId(long id) {
        this.mId = id;
    }

    /** Gets the auto-generated content ID. */
    public long getId() {
        return mId;
    }

    @Override
    public String toString() {
        final String recordAtDateTime = ConvertUtils.utcToLocalTimeForLogging(timestamp);
        final StringBuilder builder =
                new StringBuilder()
                        .append("\nAppUsageEvent{")
                        .append(
                                String.format(
                                        Locale.US,
                                        "\n\tpackage=%s|uid=%d|userId=%d",
                                        packageName,
                                        uid,
                                        userId))
                        .append(
                                String.format(
                                        Locale.US,
                                        "\n\ttimestamp=%s|eventType=%d|instanceId=%d",
                                        recordAtDateTime,
                                        appUsageEventType,
                                        instanceId))
                        .append(
                                String.format(
                                        Locale.US,
                                        "\n\ttaskRootPackageName=%s",
                                        taskRootPackageName));
        return builder.toString();
    }

    /** Creates new {@link AppUsageEventEntity} from {@link ContentValues}. */
    public static AppUsageEventEntity create(ContentValues contentValues) {
        Builder builder = AppUsageEventEntity.newBuilder();
        if (contentValues.containsKey(KEY_UID)) {
            builder.setUid(contentValues.getAsLong(KEY_UID));
        }
        if (contentValues.containsKey(KEY_USER_ID)) {
            builder.setUserId(contentValues.getAsLong(KEY_USER_ID));
        }
        if (contentValues.containsKey(KEY_TIMESTAMP)) {
            builder.setTimestamp(contentValues.getAsLong(KEY_TIMESTAMP));
        }
        if (contentValues.containsKey(KEY_APP_USAGE_EVENT_TYPE)) {
            builder.setAppUsageEventType(contentValues.getAsInteger(KEY_APP_USAGE_EVENT_TYPE));
        }
        if (contentValues.containsKey(KEY_PACKAGE_NAME)) {
            builder.setPackageName(contentValues.getAsString(KEY_PACKAGE_NAME));
        }
        if (contentValues.containsKey(KEY_INSTANCE_ID)) {
            builder.setInstanceId(contentValues.getAsInteger(KEY_INSTANCE_ID));
        }
        if (contentValues.containsKey(KEY_TASK_ROOT_PACKAGE_NAME)) {
            builder.setTaskRootPackageName(contentValues.getAsString(KEY_TASK_ROOT_PACKAGE_NAME));
        }
        return builder.build();
    }

    /** Creates a new {@link Builder} instance. */
    public static Builder newBuilder() {
        return new Builder();
    }

    /** A convenience builder class to improve readability. */
    public static class Builder {
        private long mUid;
        private long mUserId;
        private long mTimestamp;
        private int mAppUsageEventType;
        private String mPackageName;
        private int mInstanceId;
        private String mTaskRootPackageName;

        /** Sets the uid. */
        @CanIgnoreReturnValue
        public Builder setUid(final long uid) {
            this.mUid = uid;
            return this;
        }

        /** Sets the user ID. */
        @CanIgnoreReturnValue
        public Builder setUserId(final long userId) {
            this.mUserId = userId;
            return this;
        }

        /** Sets the timestamp. */
        @CanIgnoreReturnValue
        public Builder setTimestamp(final long timestamp) {
            this.mTimestamp = timestamp;
            return this;
        }

        /** Sets the app usage event type. */
        @CanIgnoreReturnValue
        public Builder setAppUsageEventType(final int appUsageEventType) {
            this.mAppUsageEventType = appUsageEventType;
            return this;
        }

        /** Sets the package name. */
        @CanIgnoreReturnValue
        public Builder setPackageName(final String packageName) {
            this.mPackageName = packageName;
            return this;
        }

        /** Sets the instance ID. */
        @CanIgnoreReturnValue
        public Builder setInstanceId(final int instanceId) {
            this.mInstanceId = instanceId;
            return this;
        }

        /** Sets the task root package name. */
        @CanIgnoreReturnValue
        public Builder setTaskRootPackageName(final String taskRootPackageName) {
            this.mTaskRootPackageName = taskRootPackageName;
            return this;
        }

        /** Builds the AppUsageEvent. */
        public AppUsageEventEntity build() {
            return new AppUsageEventEntity(
                    mUid,
                    mUserId,
                    mTimestamp,
                    mAppUsageEventType,
                    mPackageName,
                    mInstanceId,
                    mTaskRootPackageName);
        }

        private Builder() {}
    }
}
