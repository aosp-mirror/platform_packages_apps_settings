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

package com.android.settings.deviceinfo.simstatus;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.UserManager;
import android.telephony.SubscriptionInfo;
import android.text.TextUtils;
import android.util.Log;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.deviceinfo.PhoneNumberUtil;
import com.android.settings.network.SubscriptionUtil;
import com.android.settings.network.telephony.TelephonyPreferenceDialog;
import com.android.settingslib.Utils;
import com.android.settingslib.qrcode.QrCodeGenerator;

/**
 * This is to show a preference regarding EID of SIM card.
 */
public class SimEidPreferenceController extends BasePreferenceController
        implements DialogInterface.OnShowListener {

    private static final String TAG = "SimEidPreferenceController";

    private SlotSimStatus mSlotSimStatus;
    private EidStatus mEidStatus;
    private boolean mShowEidOnSummary;
    private TelephonyPreferenceDialog mPreference;

    /**
     * Constructer.
     * @param context Context
     * @param preferenceKey is the key for Preference
     */
    public SimEidPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    /**
     * Update status.
     *
     * @param slotSimStatus sim status per slot
     * @param eidStatus status of EID
     */
    public void init(SlotSimStatus slotSimStatus, EidStatus eidStatus) {
        mSlotSimStatus = slotSimStatus;
        mEidStatus = eidStatus;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        if ((!SubscriptionUtil.isSimHardwareVisible(mContext)) || (mSlotSimStatus == null)) {
            return;
        }
        TelephonyPreferenceDialog preference = (TelephonyPreferenceDialog)
                screen.findPreference(getPreferenceKey());
        if (preference == null) {
            return;
        }

        preference.setTitle(getTitle());
        mPreference = preference;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (TextUtils.equals(preference.getKey(), getPreferenceKey())) {
            TelephonyPreferenceDialog preferenceDialog = (TelephonyPreferenceDialog)preference;
            preferenceDialog.setDialogTitle(getTitle());
            preferenceDialog.setDialogMessage(mEidStatus.getEid());
            preferenceDialog.setOnShowListener(this);
            return true;
        }
        return super.handlePreferenceTreeClick(preference);
    }

    /**
     * Construct title string.
     * @return title string
     */
    @VisibleForTesting
    protected CharSequence getTitle() {
        int slotSize = (mSlotSimStatus == null) ? 0 : mSlotSimStatus.size();
        if (slotSize <= SimStatusDialogController.MAX_PHONE_COUNT_SINGLE_SIM) {
            return mContext.getString(R.string.status_eid);
        }
        // Only append slot index to title when more than 1 is available
        for (int idxSlot = 0; idxSlot < slotSize; idxSlot++) {
            SubscriptionInfo subInfo = mSlotSimStatus.getSubscriptionInfo(idxSlot);
            if ((subInfo != null) && subInfo.isEmbedded()) {
                return mContext.getString(R.string.eid_multi_sim, idxSlot+1);
            }
        }
        return mContext.getString(R.string.status_eid);
    }

    @Override
    public CharSequence getSummary() {
        if (!mShowEidOnSummary) {
            return mContext.getString(R.string.device_info_protected_single_press);
        }
        String summary = mEidStatus.getEid();
        return (summary == null) ? "" : summary;
    }

    @Override
    public int getAvailabilityStatus() {
        if (!SubscriptionUtil.isSimHardwareVisible(mContext)) {
            return UNSUPPORTED_ON_DEVICE;
        }
        boolean isAvailable = SubscriptionUtil.isSimHardwareVisible(mContext) &&
                mContext.getSystemService(UserManager.class).isAdminUser() &&
                !Utils.isWifiOnly(mContext) &&
                ((mEidStatus != null) && !TextUtils.isEmpty(mEidStatus.getEid()));
        return isAvailable ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }

    /**
     * Callback when dialog end of show().
     */
    public void onShow(DialogInterface dialog) {
        Dialog dialogShwon = mPreference.getDialog();

        dialogShwon.getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);
        dialogShwon.setCanceledOnTouchOutside(false);

        String eid = mEidStatus.getEid();
        if (eid != null) {
            TextView textView = dialogShwon.findViewById(R.id.esim_id_value);
            textView.setText(PhoneNumberUtil.expandByTts(eid));
            textView.setTextIsSelectable(true);

            ImageView qrCodeView = dialogShwon.findViewById(R.id.esim_id_qrcode);
            qrCodeView.setImageBitmap(getEidQRcode(eid, qrCodeView.getWidth()));
        }
        mShowEidOnSummary = true;

        dialogShwon.setOnDismissListener(dlg -> {
            mPreference.setSummary(getSummary());
        });
    }

    /**
     * Get the QR code for EID
     * @param eid is the EID string
     * @param widthInPixel is the width of Bitmap in pixel
     * @return a Bitmap of QR code
     */
    public Bitmap getEidQRcode(String eid, int widthInPixel) {
        Bitmap qrCodeBitmap = null;
        try {
            qrCodeBitmap = QrCodeGenerator.encodeQrCode(eid, widthInPixel);
        } catch (Exception exception) {
            Log.w(TAG, "Error when creating QR code width " + widthInPixel, exception);
        }
        return qrCodeBitmap;
    }
}
