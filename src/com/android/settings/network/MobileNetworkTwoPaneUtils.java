package com.android.settings.network;

import android.annotation.Nullable;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.android.settings.activityembedding.ActivityEmbeddingRulesController;

public class MobileNetworkTwoPaneUtils {

    private static final String TAG = "MobileNetworkTwoPaneUtils";

    /**
     * TODO: b/206061070, the problem of multi-instance should be fixed in Android T to apply the
     * Settings' architecture and 2 panes mode instead of registering the rule.
     *
     * The launchMode of MobileNetworkActivity is singleTask, set SplitPairRule to show in 2-pane.
     */
    public static void registerTwoPaneForMobileNetwork(Context context, Intent intent,
            @Nullable String secondaryIntentAction) {
        Log.d(TAG, "registerTwoPaneForMobileNetwork");
        ActivityEmbeddingRulesController.registerTwoPanePairRuleForSettingsHome(
                context,
                intent.getComponent(),
                secondaryIntentAction /* secondaryIntentAction */,
                false /* clearTop */);
    }
}
