package com.android.settings.network;

import static com.android.internal.telephony.TelephonyIntents.ACTION_DEFAULT_VOICE_SUBSCRIPTION_CHANGED;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

public class DefaultSubscriptionReceiver extends BroadcastReceiver {

    private Context mContext;
    private DefaultSubscriptionListener mListener;

    public DefaultSubscriptionReceiver(Context context, DefaultSubscriptionListener listener) {
        mContext = context;
        mListener = listener;
    }

    public void registerReceiver() {
        final IntentFilter filter = new IntentFilter();
        filter.addAction(TelephonyManager.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED);
        filter.addAction(SubscriptionManager.ACTION_DEFAULT_SUBSCRIPTION_CHANGED);
        filter.addAction(ACTION_DEFAULT_VOICE_SUBSCRIPTION_CHANGED);
        filter.addAction(SubscriptionManager.ACTION_DEFAULT_SMS_SUBSCRIPTION_CHANGED);
        mContext.registerReceiver(this, filter);
    }

    public void unRegisterReceiver() {
        mContext.unregisterReceiver(this);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (TelephonyManager.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED.equals(action)) {
            mListener.onDefaultDataChanged(SubscriptionManager.getDefaultDataSubscriptionId());
        } else if (SubscriptionManager.ACTION_DEFAULT_SUBSCRIPTION_CHANGED.equals(action)) {
            mListener.onDefaultSubInfoChanged(SubscriptionManager.getDefaultSubscriptionId());
        } else if (TelephonyManager.ACTION_DEFAULT_VOICE_SUBSCRIPTION_CHANGED.equals(action)) {
            mListener.onDefaultVoiceChanged(SubscriptionManager.getDefaultVoiceSubscriptionId());
        } else if (SubscriptionManager.ACTION_DEFAULT_SMS_SUBSCRIPTION_CHANGED.equals(action)) {
            mListener.onDefaultSmsChanged(SubscriptionManager.getDefaultSmsSubscriptionId());
        }
    }

    public interface DefaultSubscriptionListener {
        default void onDefaultSubInfoChanged(int defaultSubId) {
        }
        default void onDefaultDataChanged(int defaultDataSubId) {
        }
        default void onDefaultVoiceChanged(int defaultVoiceSubId) {
        }
        default void onDefaultSmsChanged(int defaultSmsSubId) {
        }
    }
}
