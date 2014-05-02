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
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.android.settings.R;

public class ZenModeConditionSelection extends RadioGroup {
    private static final String TAG = "ZenModeConditionSelection";
    private static final boolean DEBUG = true;

    private final INotificationManager mNoMan;
    private final H mHandler = new H();
    private final Context mContext;

    public ZenModeConditionSelection(Context context) {
        super(context);
        mContext = context;
        setLayoutTransition(new LayoutTransition());
        final int p = mContext.getResources().getDimensionPixelSize(R.dimen.content_margin_left);
        setPadding(p, p, p, 0);
        mNoMan = INotificationManager.Stub.asInterface(
                ServiceManager.getService(Context.NOTIFICATION_SERVICE));
        final RadioButton b = newRadioButton(null);
        b.setText(R.string.zen_mode_default_option);
        b.setChecked(true);
    }

    private RadioButton newRadioButton(Object tag) {
        final RadioButton button = new RadioButton(mContext);
        button.setTag(tag);
        button.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    handleSubscribe((Uri)button.getTag());
                }
            }
        });
        addView(button);
        return button;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        requestZenModeConditions(Condition.FLAG_RELEVANT_NOW);
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
            // noop
        }
    }

    protected void handleConditions(Condition[] conditions) {
        for (final Condition c : conditions) {
            RadioButton v = (RadioButton) findViewWithTag(c.id);
            if (c.state == Condition.STATE_TRUE || c.state == Condition.STATE_UNKNOWN) {
                if (v == null) {
                    v = newRadioButton(c.id);
                }
            }
            if (v != null) {
                v.setText(c.summary);
                v.setEnabled(c.state == Condition.STATE_TRUE);
            }
        }
    }

    protected void handleSubscribe(Uri id) {
        if (DEBUG) Log.d(TAG, "handleSubscribe " + id);
        try {
            mNoMan.setZenModeCondition(id);
        } catch (RemoteException e) {
            // noop
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
