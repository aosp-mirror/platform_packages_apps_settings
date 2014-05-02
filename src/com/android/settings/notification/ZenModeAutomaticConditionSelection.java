/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.settings.notification;

import android.animation.LayoutTransition;
import android.app.INotificationManager;
import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.service.notification.Condition;
import android.service.notification.IConditionListener;
import android.util.ArraySet;
import android.util.Log;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;

import com.android.settings.R;

public class ZenModeAutomaticConditionSelection extends LinearLayout {
    private static final String TAG = "ZenModeAutomaticConditionSelection";
    private static final boolean DEBUG = true;

    private final INotificationManager mNoMan;
    private final H mHandler = new H();
    private final Context mContext;
    private final ArraySet<Uri> mSelectedConditions = new ArraySet<Uri>();

    public ZenModeAutomaticConditionSelection(Context context) {
        super(context);
        mContext = context;
        setOrientation(VERTICAL);
        setLayoutTransition(new LayoutTransition());
        final int p = mContext.getResources().getDimensionPixelSize(R.dimen.content_margin_left);
        setPadding(p, p, p, 0);
        mNoMan = INotificationManager.Stub.asInterface(
                ServiceManager.getService(Context.NOTIFICATION_SERVICE));
        refreshSelectedConditions();
    }

    private void refreshSelectedConditions() {
        try {
            final Condition[] automatic = mNoMan.getAutomaticZenModeConditions();
            mSelectedConditions.clear();
            if (automatic != null) {
                for (Condition c : automatic) {
                    mSelectedConditions.add(c.id);
                }
            }
        } catch (RemoteException e) {
            Log.w(TAG, "Error calling getAutomaticZenModeConditions", e);
        }
    }

    private CheckBox newCheckBox(Object tag) {
        final CheckBox button = new CheckBox(mContext);
        button.setTag(tag);
        button.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                 setSelectedCondition((Uri)button.getTag(), isChecked);
            }
        });
        addView(button);
        return button;
    }

    private void setSelectedCondition(Uri conditionId, boolean selected) {
        if (DEBUG) Log.d(TAG, "setSelectedCondition conditionId=" + conditionId
                + " selected=" + selected);
        if (selected) {
            mSelectedConditions.add(conditionId);
        } else {
            mSelectedConditions.remove(conditionId);
        }
        final Uri[] automatic = new Uri[mSelectedConditions.size()];
        for (int i = 0; i < automatic.length; i++) {
            automatic[i] = mSelectedConditions.valueAt(i);
        }
        try {
            mNoMan.setAutomaticZenModeConditions(automatic);
        } catch (RemoteException e) {
            Log.w(TAG, "Error calling setAutomaticZenModeConditions", e);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        requestZenModeConditions(Condition.FLAG_RELEVANT_ALWAYS);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        requestZenModeConditions(0 /*none*/);
    }

    protected void requestZenModeConditions(int relevance) {
        if (DEBUG) Log.d(TAG, "requestZenModeConditions " + Condition.relevanceToString(relevance));
        try {
            mNoMan.requestZenModeConditions(mListener, relevance);
        } catch (RemoteException e) {
            Log.w(TAG, "Error calling requestZenModeConditions", e);
        }
    }

    protected void handleConditions(Condition[] conditions) {
        for (final Condition c : conditions) {
            CheckBox v = (CheckBox) findViewWithTag(c.id);
            if (c.state != Condition.STATE_ERROR) {
                if (v == null) {
                    v = newCheckBox(c.id);
                }
            }
            if (v != null) {
                v.setText(c.summary);
                v.setEnabled(c.state != Condition.STATE_ERROR);
                v.setChecked(mSelectedConditions.contains(c.id));
            }
        }
    }

    private final IConditionListener mListener = new IConditionListener.Stub() {
        @Override
        public void onConditionsReceived(Condition[] conditions) {
            if (conditions == null || conditions.length == 0) return;
            mHandler.obtainMessage(H.CONDITIONS, conditions).sendToTarget();
        }
    };

    private final class H extends Handler {
        private static final int CONDITIONS = 1;

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == CONDITIONS) handleConditions((Condition[])msg.obj);
        }
    }
}
