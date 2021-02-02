package com.android.settings.network.telephony;

import android.content.Context;
import android.content.Intent;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.network.SubscriptionUtil;
import com.android.settings.utils.AnnotationSpan;
import com.android.settingslib.HelpUtils;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;

import java.util.List;

public class NetworkProviderWfcFooterPreferenceController extends BasePreferenceController
        implements LifecycleObserver {

    /**
     * Constructor.
     */
    public NetworkProviderWfcFooterPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    /**
     * Initialize the binding with Lifecycle
     *
     * @param lifecycle Lifecycle of UI which owns this Preference
     */
    public void init(Lifecycle lifecycle) {
        lifecycle.addObserver(this);
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);

        if (preference != null) {
            // This is necessary to ensure that setting the title to the spannable string returned
            // by getFooterText will be accepted.  Internally, setTitle does an equality check on
            // the spannable string being set to the text already set on the preference.  That
            // equality check apparently only takes into account the raw text and not and spannables
            // that are part of the text.  So we clear the title before applying the spannable
            // footer to ensure it is accepted.
            preference.setTitle("");
            preference.setTitle(getFooterText());
        }
    }

    private CharSequence getFooterText() {
        final Intent helpIntent = HelpUtils.getHelpIntent(mContext,
                mContext.getString(R.string.help_uri_wifi_calling),
                mContext.getClass().getName());
        final AnnotationSpan.LinkInfo linkInfo = new AnnotationSpan.LinkInfo(mContext,
                "url", helpIntent);

        return AnnotationSpan.linkify(mContext.getText(R.string.calls_sms_footnote), linkInfo);
    }

    @Override
    public int getAvailabilityStatus() {
        final SubscriptionManager subscriptionManager =
                mContext.getSystemService(SubscriptionManager.class);
        final List<SubscriptionInfo> subscriptions = SubscriptionUtil.getActiveSubscriptions(
                subscriptionManager);
        if (subscriptions.size() >= 1) {
            return AVAILABLE;
        } else {
            return CONDITIONALLY_UNAVAILABLE;
        }
    }
}
