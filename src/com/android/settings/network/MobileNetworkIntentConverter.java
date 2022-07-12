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

package com.android.settings.network;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.SystemClock;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.ims.ImsRcsManager;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.android.settings.Settings.MobileNetworkActivity;
import com.android.settings.SettingsActivity;
import com.android.settings.network.telephony.MobileNetworkUtils;

import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * A Java {@link Function} for conversion between {@link Intent} to Settings,
 * and within Settings itself.
 */
public class MobileNetworkIntentConverter implements Function<Intent, Intent> {
    private static final String TAG = "MobileNetworkIntentConverter";

    private static final ComponentName sTargetComponent = ComponentName
            .createRelative("com.android.settings",
                    MobileNetworkActivity.class.getTypeName());

    /**
     * These actions has better aligned with definitions within AndroidManifest.xml
     */
    private static final String [] sPotentialActions = new String [] {
        null,
        Intent.ACTION_MAIN,
        android.provider.Settings.ACTION_NETWORK_OPERATOR_SETTINGS,
        android.provider.Settings.ACTION_DATA_ROAMING_SETTINGS,
        android.provider.Settings.ACTION_MMS_MESSAGE_SETTING,
        ImsRcsManager.ACTION_SHOW_CAPABILITY_DISCOVERY_OPT_IN
    };

    private static final String RE_ROUTE_TAG = ":reroute:" + TAG;
    private static final AtomicReference<String> mCachedClassName =
            new AtomicReference<String>();

    private final Context mAppContext;
    private final ComponentName mComponent;

    /**
     * Constructor
     * @param activity which receiving {@link Intent}
     */
    public MobileNetworkIntentConverter(@NonNull Activity activity) {
        mAppContext = activity.getApplicationContext();
        mComponent = activity.getComponentName();
    }

    /**
     * API defined by {@link Function}.
     * @param fromIntent is the {@link Intent} for convert.
     * @return {@link Intent} for sending internally within Settings.
     *      Return {@code null} when failure.
     */
    public Intent apply(Intent fromIntent) {
        long startTime = SystemClock.elapsedRealtimeNanos();

        Intent potentialReqIntent = null;
        if (isAttachedToExposedComponents()) {
            potentialReqIntent = convertFromDeepLink(fromIntent);
        } else if (mayRequireConvert(fromIntent)) {
            potentialReqIntent = fromIntent;
        } else {
            return null;
        }

        final Intent reqIntent = potentialReqIntent;
        String action = reqIntent.getAction();

        // Find out the subscription ID of request.
        final int subId = extractSubscriptionId(reqIntent);

        // Prepare the arguments Bundle.
        Function<Intent, Intent> ops = Function.identity();

        if (TextUtils.equals(action,
                android.provider.Settings.ACTION_NETWORK_OPERATOR_SETTINGS)
                || TextUtils.equals(action,
                        android.provider.Settings.ACTION_DATA_ROAMING_SETTINGS)) {
            // Accepted.
            ops = ops.andThen(intent -> extractArguments(intent, subId))
                     .andThen(args -> rePackIntent(args, reqIntent))
                     .andThen(intent -> updateFragment(intent, mAppContext, subId));
        } else if (TextUtils.equals(action,
                android.provider.Settings.ACTION_MMS_MESSAGE_SETTING)) {
            ops = ops.andThen(intent -> extractArguments(intent, subId))
                     .andThen(args -> convertMmsArguments(args))
                     .andThen(args -> rePackIntent(args, reqIntent))
                     .andThen(intent -> updateFragment(intent, mAppContext, subId));
        } else if (TextUtils.equals(action,
                ImsRcsManager.ACTION_SHOW_CAPABILITY_DISCOVERY_OPT_IN)) {
            ops = ops.andThen(intent -> extractArguments(intent, subId))
                     .andThen(args -> supportContactDiscoveryDialog(args, mAppContext, subId))
                     .andThen(args -> rePackIntent(args, reqIntent))
                     .andThen(intent -> updateFragment(intent, mAppContext, subId));
        } else if ((sTargetComponent.compareTo(mComponent) == 0)
                && ((action == null) || Intent.ACTION_MAIN.equals(action))) {
            Log.d(TAG, "Support default actions direct to this component");
            ops = ops.andThen(intent -> extractArguments(intent, subId))
                     .andThen(args -> rePackIntent(args, reqIntent))
                     .andThen(intent -> replaceIntentAction(intent))
                     .andThen(intent -> updateFragment(intent, mAppContext, subId));
        } else {
            return null;
        }

        if (!isAttachedToExposedComponents()) {
            ops = ops.andThen(intent -> configForReRoute(intent));
        }

        Intent result = ops.apply(reqIntent);
        if (result != null) {
            long endTime = SystemClock.elapsedRealtimeNanos();
            Log.d(TAG, mComponent.toString() + " intent conversion: "
                    + (endTime - startTime) + " ns");
        }
        return result;
    }

    @VisibleForTesting
    protected boolean isAttachedToExposedComponents() {
        return (sTargetComponent.compareTo(mComponent) == 0);
    }

    protected int extractSubscriptionId(Intent reqIntent) {
        return reqIntent.getIntExtra(android.provider.Settings.EXTRA_SUB_ID,
                SubscriptionManager.INVALID_SUBSCRIPTION_ID);
    }

    protected Bundle extractArguments(Intent reqIntent, int subId) {
        // Duplicate from SettingsActivity#getIntent()
        Bundle args = reqIntent.getBundleExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT_ARGUMENTS);
        Bundle result = (args != null) ? new Bundle(args) : new Bundle();
        result.putParcelable("intent", reqIntent);
        result.putInt(android.provider.Settings.EXTRA_SUB_ID, subId);
        return result;
    }

    protected Bundle convertMmsArguments(Bundle args) {
        // highlight "mms_message" preference.
        args.putString(SettingsActivity.EXTRA_FRAGMENT_ARG_KEY,
                MobileNetworkActivity.EXTRA_MMS_MESSAGE);
        return args;
    }

    @VisibleForTesting
    protected boolean mayShowContactDiscoveryDialog(Context context, int subId) {
        // If this activity was launched using ACTION_SHOW_CAPABILITY_DISCOVERY_OPT_IN, show the
        // associated dialog only if the opt-in has not been granted yet.
        return MobileNetworkUtils.isContactDiscoveryVisible(context, subId)
                // has the user already enabled this configuration?
                && !MobileNetworkUtils.isContactDiscoveryEnabled(context, subId);
    }

    protected Bundle supportContactDiscoveryDialog(Bundle args, Context context, int subId) {
        boolean showDialog = mayShowContactDiscoveryDialog(context, subId);
        Log.d(TAG, "maybeShowContactDiscoveryDialog subId=" + subId + ", show=" + showDialog);
        args.putBoolean(MobileNetworkActivity.EXTRA_SHOW_CAPABILITY_DISCOVERY_OPT_IN,
                showDialog);
        return args;
    }

    protected Intent rePackIntent(Bundle args, Intent reqIntent) {
        Intent intent = new Intent(reqIntent);
        intent.setComponent(sTargetComponent);
        intent.putExtra(android.provider.Settings.EXTRA_SUB_ID,
                args.getInt(android.provider.Settings.EXTRA_SUB_ID));
        intent.putExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT_ARGUMENTS, args);
        return intent;
    }

    protected Intent replaceIntentAction(Intent intent) {
        intent.setAction(android.provider.Settings.ACTION_NETWORK_OPERATOR_SETTINGS);
        return intent;
    }

    @VisibleForTesting
    protected CharSequence getFragmentTitle(Context context, int subId) {
        SubscriptionInfo subInfo = SubscriptionUtil.getSubscriptionOrDefault(context, subId);
        return SubscriptionUtil.getUniqueSubscriptionDisplayName(subInfo, context);
    }

    protected Intent updateFragment(Intent intent, Context context, int subId) {
        if (intent.getStringExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT_TITLE) == null) {
            CharSequence title = getFragmentTitle(context, subId);
            if (title != null) {
                intent.putExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT_TITLE, title.toString());
            }
        }
        intent.putExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT, getFragmentClass(context));
        return intent;
    }

    protected String getFragmentClass(Context context) {
        String className = mCachedClassName.get();
        if (className != null) {
            return className;
        }
        try {
            ActivityInfo ai = context.getPackageManager()
                    .getActivityInfo(sTargetComponent, PackageManager.GET_META_DATA);
            if (ai != null && ai.metaData != null) {
                className = ai.metaData.getString(SettingsActivity.META_DATA_KEY_FRAGMENT_CLASS);
                if (className != null) {
                    mCachedClassName.set(className);
                }
                return className;
            }
        } catch (NameNotFoundException nnfe) {
            // No recovery
            Log.d(TAG, "Cannot get Metadata for: " + sTargetComponent.toString());
        }
        return null;
    }

    protected Intent configForReRoute(Intent intent) {
        if (intent.hasExtra(RE_ROUTE_TAG)) {
            Log.d(TAG, "Skip re-routed intent " + intent);
            return null;
        }
        return intent.putExtra(RE_ROUTE_TAG, intent.getAction())
                .setComponent(null);
    }

    protected static boolean mayRequireConvert(Intent intent) {
        if (intent == null) {
            return false;
        }
        final String action = intent.getAction();
        return Arrays.stream(sPotentialActions).anyMatch(potentialAction ->
                        TextUtils.equals(action, potentialAction)
                );
    }

    protected Intent convertFromDeepLink(Intent intent) {
        if (intent == null) {
            return null;
        }
        if (!TextUtils.equals(intent.getAction(),
                android.provider.Settings.ACTION_SETTINGS_EMBED_DEEP_LINK_ACTIVITY)) {
            return intent;
        }
        try {
            return Intent.parseUri(intent.getStringExtra(
                android.provider.Settings.EXTRA_SETTINGS_EMBEDDED_DEEP_LINK_INTENT_URI),
                Intent.URI_INTENT_SCHEME);
        } catch (URISyntaxException exception) {
            Log.d(TAG, "Intent URI corrupted", exception);
        }
        return null;
    }
}
