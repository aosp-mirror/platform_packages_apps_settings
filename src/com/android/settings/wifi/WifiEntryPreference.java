/*
 * Copyright (C) 2021 The Android Open Source Project
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
package com.android.settings.wifi;

import static com.android.settingslib.wifi.WifiUtils.getHotspotIconResource;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.preference.PreferenceViewHolder;

import com.android.settingslib.R;
import com.android.settingslib.RestrictedPreference;
import com.android.settingslib.Utils;
import com.android.settingslib.wifi.WifiUtils;
import com.android.wifitrackerlib.HotspotNetworkEntry;
import com.android.wifitrackerlib.WifiEntry;

/**
 * Preference to display a WifiEntry in a wifi picker.
 */
public class WifiEntryPreference extends RestrictedPreference implements
        WifiEntry.WifiEntryCallback,
        View.OnClickListener {

    private static final int[] STATE_SECURED = {
            R.attr.state_encrypted
    };

    private static final int[] FRICTION_ATTRS = {
            R.attr.wifi_friction
    };

    // These values must be kept within [WifiEntry.WIFI_LEVEL_MIN, WifiEntry.WIFI_LEVEL_MAX]
    private static final int[] WIFI_CONNECTION_STRENGTH = {
            R.string.accessibility_no_wifi,
            R.string.accessibility_wifi_one_bar,
            R.string.accessibility_wifi_two_bars,
            R.string.accessibility_wifi_three_bars,
            R.string.accessibility_wifi_signal_full
    };

    // StateListDrawable to display secured lock / metered "$" icon
    @Nullable private final StateListDrawable mFrictionSld;
    private final WifiUtils.InternetIconInjector mIconInjector;
    private WifiEntry mWifiEntry;
    private int mLevel = -1;
    private boolean mShowX; // Shows the Wi-Fi signl icon of Pie+x when it's true.
    private CharSequence mContentDescription;
    private OnButtonClickListener mOnButtonClickListener;

    public WifiEntryPreference(@NonNull Context context, @NonNull WifiEntry wifiEntry) {
        this(context, wifiEntry, new WifiUtils.InternetIconInjector(context));
    }

    @VisibleForTesting
    WifiEntryPreference(@NonNull Context context, @NonNull WifiEntry wifiEntry,
            @NonNull WifiUtils.InternetIconInjector iconInjector) {
        super(context);

        setLayoutResource(R.layout.preference_access_point);
        mFrictionSld = getFrictionStateListDrawable();
        mIconInjector = iconInjector;
        setWifiEntry(wifiEntry);
    }

    /**
     * Set updated {@link WifiEntry} to refresh the preference
     *
     * @param wifiEntry An instance of {@link WifiEntry}
     */
    public void setWifiEntry(@NonNull WifiEntry wifiEntry) {
        mWifiEntry = wifiEntry;
        mWifiEntry.setListener(this);
        refresh();
    }

    public WifiEntry getWifiEntry() {
        return mWifiEntry;
    }

    @Override
    public void onBindViewHolder(final PreferenceViewHolder view) {
        super.onBindViewHolder(view);
        if (mWifiEntry.isVerboseSummaryEnabled()) {
            TextView summary = (TextView) view.findViewById(android.R.id.summary);
            if (summary != null) {
                summary.setMaxLines(100);
            }
        }
        final Drawable drawable = getIcon();
        if (drawable != null) {
            drawable.setLevel(mLevel);
        }

        view.itemView.setContentDescription(mContentDescription);

        // Turn off divider
        view.findViewById(com.android.settingslib.widget.preference.twotarget.R.id.two_target_divider)
                .setVisibility(View.INVISIBLE);

        // Enable the icon button when the help string in this WifiEntry is not null.
        final ImageButton imageButton = (ImageButton) view.findViewById(R.id.icon_button);
        final ImageView frictionImageView = (ImageView) view.findViewById(
                R.id.friction_icon);
        if (mWifiEntry.getHelpUriString() != null
                && mWifiEntry.getConnectedState() == WifiEntry.CONNECTED_STATE_DISCONNECTED) {
            final Drawable drawablehelp = getDrawable(R.drawable.ic_help);
            drawablehelp.setTintList(
                    Utils.getColorAttr(getContext(), android.R.attr.colorControlNormal));
            ((ImageView) imageButton).setImageDrawable(drawablehelp);
            imageButton.setVisibility(View.VISIBLE);
            imageButton.setOnClickListener(this);
            imageButton.setContentDescription(
                    getContext().getText(R.string.help_label));

            if (frictionImageView != null) {
                frictionImageView.setVisibility(View.GONE);
            }
        } else {
            imageButton.setVisibility(View.GONE);

            if (frictionImageView != null) {
                frictionImageView.setVisibility(View.VISIBLE);
                bindFrictionImage(frictionImageView);
            }
        }
    }

    /**
     * Updates the title and summary; may indirectly call notifyChanged().
     */
    public void refresh() {
        setTitle(mWifiEntry.getTitle());
        if (mWifiEntry instanceof HotspotNetworkEntry) {
            updateHotspotIcon(((HotspotNetworkEntry) mWifiEntry).getDeviceType());
        } else {
            mLevel = mWifiEntry.getLevel();
            mShowX = mWifiEntry.shouldShowXLevelIcon();
            updateIcon(mShowX, mLevel);
        }

        setSummary(mWifiEntry.getSummary(false /* concise */));
        mContentDescription = buildContentDescription();
    }

    /**
     * Indicates the state of the WifiEntry has changed and clients may retrieve updates through
     * the WifiEntry getter methods.
     */
    public void onUpdated() {
        // TODO(b/70983952): Fill this method in
        refresh();
    }

    /**
     * Result of the connect request indicated by the WifiEntry.CONNECT_STATUS constants.
     */
    public void onConnectResult(int status) {
        // TODO(b/70983952): Fill this method in
    }

    /**
     * Result of the disconnect request indicated by the WifiEntry.DISCONNECT_STATUS constants.
     */
    public void onDisconnectResult(int status) {
        // TODO(b/70983952): Fill this method in
    }

    /**
     * Result of the forget request indicated by the WifiEntry.FORGET_STATUS constants.
     */
    public void onForgetResult(int status) {
        // TODO(b/70983952): Fill this method in
    }

    /**
     * Result of the sign-in request indecated by the WifiEntry.SIGNIN_STATUS constants
     */
    public void onSignInResult(int status) {
        // TODO(b/70983952): Fill this method in
    }

    protected int getIconColorAttr() {
        final boolean accent = (mWifiEntry.hasInternetAccess()
                && mWifiEntry.getConnectedState() == WifiEntry.CONNECTED_STATE_CONNECTED);
        return accent ? android.R.attr.colorAccent : android.R.attr.colorControlNormal;
    }

    private void setIconWithTint(Drawable drawable) {
        if (drawable != null) {
            // Must use Drawable#setTintList() instead of Drawable#setTint() to show the grey
            // icon when the preference is disabled.
            drawable.setTintList(Utils.getColorAttr(getContext(), getIconColorAttr()));
            setIcon(drawable);
        } else {
            setIcon(null);
        }
    }

    @VisibleForTesting
    void updateIcon(boolean showX, int level) {
        if (level == -1) {
            setIcon(null);
            return;
        }
        setIconWithTint(mIconInjector.getIcon(showX, level));
    }

    @VisibleForTesting
    void updateHotspotIcon(int deviceType) {
        setIconWithTint(getContext().getDrawable(getHotspotIconResource(deviceType)));
    }

    @Nullable
    private StateListDrawable getFrictionStateListDrawable() {
        TypedArray frictionSld;
        try {
            frictionSld = getContext().getTheme().obtainStyledAttributes(FRICTION_ATTRS);
        } catch (Resources.NotFoundException e) {
            // Fallback for platforms that do not need friction icon resources.
            frictionSld = null;
        }
        return frictionSld != null ? (StateListDrawable) frictionSld.getDrawable(0) : null;
    }

    /**
     * Binds the friction icon drawable using a StateListDrawable.
     *
     * <p>Friction icons will be rebound when notifyChange() is called, and therefore
     * do not need to be managed in refresh()</p>.
     */
    private void bindFrictionImage(ImageView frictionImageView) {
        if (frictionImageView == null || mFrictionSld == null) {
            return;
        }
        if ((mWifiEntry.getSecurity() != WifiEntry.SECURITY_NONE)
                && (mWifiEntry.getSecurity() != WifiEntry.SECURITY_OWE)) {
            mFrictionSld.setState(STATE_SECURED);
        }
        frictionImageView.setImageDrawable(mFrictionSld.getCurrent());
    }

    /**
     * Helper method to generate content description string.
     */
    @VisibleForTesting
    CharSequence buildContentDescription() {
        final Context context = getContext();

        CharSequence contentDescription = getTitle();
        final CharSequence summary = getSummary();
        if (!TextUtils.isEmpty(summary)) {
            contentDescription = TextUtils.concat(contentDescription, ",", summary);
        }
        int level = mWifiEntry.getLevel();
        if (level >= 0 && level < WIFI_CONNECTION_STRENGTH.length) {
            contentDescription = TextUtils.concat(contentDescription, ",",
                    context.getString(WIFI_CONNECTION_STRENGTH[level]));
        }
        return TextUtils.concat(contentDescription, ",",
                mWifiEntry.getSecurity() == WifiEntry.SECURITY_NONE
                        ? context.getString(R.string.accessibility_wifi_security_type_none)
                        : context.getString(R.string.accessibility_wifi_security_type_secured));
    }

    /**
     * Set listeners, who want to listen the button client event.
     */
    public void setOnButtonClickListener(OnButtonClickListener listener) {
        mOnButtonClickListener = listener;
        notifyChanged();
    }

    @Override
    protected int getSecondTargetResId() {
        return R.layout.access_point_friction_widget;
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.icon_button) {
            if (mOnButtonClickListener != null) {
                mOnButtonClickListener.onButtonClick(this);
            }
        }
    }

    /**
     * Callback to inform the caller that the icon button is clicked.
     */
    public interface OnButtonClickListener {

        /**
         * Register to listen the button click event.
         */
        void onButtonClick(WifiEntryPreference preference);
    }

    private Drawable getDrawable(@DrawableRes int iconResId) {
        Drawable buttonIcon = null;

        try {
            buttonIcon = getContext().getDrawable(iconResId);
        } catch (Resources.NotFoundException exception) {
            // Do nothing
        }
        return buttonIcon;
    }
}
