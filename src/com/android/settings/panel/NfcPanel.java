package com.android.settings.panel;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.android.settings.R;
import com.android.settings.SubSettings;
import com.android.settings.connecteddevice.AdvancedConnectedDeviceDashboardFragment;
import com.android.settings.slices.CustomSliceRegistry;
import com.android.settings.slices.SliceBuilderUtils;

import java.util.ArrayList;
import java.util.List;

public class NfcPanel implements PanelContent {

    private final Context mContext;

    public static NfcPanel create(Context context) {
        return new NfcPanel(context);
    }

    private NfcPanel(Context context) {
        mContext = context.getApplicationContext();
    }

    @Override
    public CharSequence getTitle() {
        return mContext.getText(R.string.nfc_quick_toggle_title);
    }

    @Override
    public List<Uri> getSlices() {
        final List<Uri> uris = new ArrayList<>();
        uris.add(CustomSliceRegistry.NFC_SLICE_URI);
        return uris;
    }

    @Override
    public Intent getSeeMoreIntent() {
        final String screenTitle =
                mContext.getText(R.string.connected_device_connections_title).toString();
        Intent intent = SliceBuilderUtils.buildSearchResultPageIntent(mContext,
                AdvancedConnectedDeviceDashboardFragment.class.getName(),
                null /* key */,
                screenTitle,
                SettingsEnums.SETTINGS_CONNECTED_DEVICE_CATEGORY);
        intent.setClassName(mContext.getPackageName(), SubSettings.class.getName());
        return intent;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.PANEL_NFC;
    }
}
