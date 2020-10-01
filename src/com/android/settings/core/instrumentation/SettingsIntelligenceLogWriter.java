/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.core.instrumentation;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;
import android.os.UserHandle;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.VisibleForTesting;

import com.android.settings.R;
import com.android.settings.intelligence.LogProto.SettingsLog;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.instrumentation.LogWriter;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.LinkedList;
import java.util.List;

public class SettingsIntelligenceLogWriter implements LogWriter {
    private static final String TAG = "IntelligenceLogWriter";

    private static final String LOG = "logs";
    private static final long MESSAGE_DELAY = DateUtils.MINUTE_IN_MILLIS; // 1 minute

    private List<SettingsLog> mSettingsLogList;
    private SendLogHandler mLogHandler;

    public SettingsIntelligenceLogWriter() {
        mSettingsLogList = new LinkedList<>();
        final HandlerThread workerThread = new HandlerThread("SettingsIntelligenceLogWriter",
                Process.THREAD_PRIORITY_BACKGROUND);
        workerThread.start();
        mLogHandler = new SendLogHandler(workerThread.getLooper());
    }

    @Override
    public void visible(Context context, int attribution, int pageId, int latency) {
        action(attribution /* from pageId */,
                SettingsEnums.PAGE_VISIBLE /* action */,
                pageId /* target pageId */,
                "" /* changedPreferenceKey */,
                latency /* changedPreferenceIntValue */);
    }

    @Override
    public void hidden(Context context, int pageId, int visibleTime) {
        action(SettingsEnums.PAGE_UNKNOWN /* attribution */,
                SettingsEnums.PAGE_HIDE /* action */,
                pageId /* pageId */,
                "" /* changedPreferenceKey */,
                visibleTime /* changedPreferenceIntValue */);
    }

    @Override
    public void action(Context context, int action, Pair<Integer, Object>... taggedData) {
        action(SettingsEnums.PAGE_UNKNOWN /* attribution */,
                action,
                SettingsEnums.PAGE_UNKNOWN /* pageId */,
                "" /* changedPreferenceKey */,
                0 /* changedPreferenceIntValue */);
    }

    @Override
    public void action(Context context, int action, int value) {
        action(SettingsEnums.PAGE_UNKNOWN /* attribution */,
                action,
                SettingsEnums.PAGE_UNKNOWN /* pageId */,
                "" /* changedPreferenceKey */,
                value /* changedPreferenceIntValue */);
    }

    @Override
    public void action(Context context, int action, boolean value) {
        action(SettingsEnums.PAGE_UNKNOWN /* attribution */,
                action,
                SettingsEnums.PAGE_UNKNOWN /* pageId */,
                "" /* changedPreferenceKey */,
                value ? 1 : 0 /* changedPreferenceIntValue */);
    }

    @Override
    public void action(Context context, int action, String pkg) {
        action(SettingsEnums.PAGE_UNKNOWN /* attribution */,
                action,
                SettingsEnums.PAGE_UNKNOWN /* pageId */,
                pkg /* changedPreferenceKey */,
                1 /* changedPreferenceIntValue */);
    }

    @Override
    public void action(int attribution, int action, int pageId, String key, int value) {
        final ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
        final SettingsLog settingsLog = SettingsLog.newBuilder()
                .setAttribution(attribution)
                .setAction(action)
                .setPageId(pageId)
                .setChangedPreferenceKey(key != null ? key : "")
                .setChangedPreferenceIntValue(value)
                .setTimestamp(now.toString())
                .build();
        mLogHandler.post(() -> {
            mSettingsLogList.add(settingsLog);
        });
        if (action == SettingsEnums.ACTION_CONTEXTUAL_CARD_DISMISS) {
            // Directly send this event to notify SI instantly that the card is dismissed
            mLogHandler.sendLog();
        } else {
            mLogHandler.scheduleSendLog();
        }
    }

    @VisibleForTesting
    static byte[] serialize(List<SettingsLog> settingsLogs) {
        final int size = settingsLogs.size();
        final ByteArrayOutputStream bout = new ByteArrayOutputStream();
        final DataOutputStream output = new DataOutputStream(bout);
        // The data format is "size, length, byte array, length, byte array ..."
        try {
            output.writeInt(size);
            for (SettingsLog settingsLog : settingsLogs) {
                final byte[] data = settingsLog.toByteArray();
                output.writeInt(data.length);
                output.write(data);
            }
            return bout.toByteArray();
        } catch (Exception e) {
            Log.e(TAG, "serialize error", e);
            return null;
        } finally {
            try {
                output.close();
            } catch (Exception e) {
                Log.e(TAG, "close error", e);
            }
        }
    }

    private class SendLogHandler extends Handler {

        SendLogHandler(Looper looper) {
            super(looper);
        }

        void scheduleSendLog() {
            removeCallbacks(mSendLogsRunnable);
            postDelayed(mSendLogsRunnable, MESSAGE_DELAY);
        }

        void sendLog() {
            removeCallbacks(mSendLogsRunnable);
            post(mSendLogsRunnable);
        }
    }

    private final Runnable mSendLogsRunnable = () -> {
        final Context context = FeatureFactory.getAppContext();
        if (context == null) {
            Log.e(TAG, "context is null");
            return;
        }
        final String action = context.getString(R.string
                .config_settingsintelligence_log_action);
        if (!TextUtils.isEmpty(action) && !mSettingsLogList.isEmpty()) {
            final Intent intent = new Intent();
            intent.setPackage(context.getString(R.string
                    .config_settingsintelligence_package_name));
            intent.setAction(action);
            intent.putExtra(LOG, serialize(mSettingsLogList));
            context.sendBroadcastAsUser(intent, UserHandle.CURRENT);
            mSettingsLogList.clear();
        }
    };
}
