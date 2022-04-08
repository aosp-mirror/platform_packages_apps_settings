/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.accounts;

import android.content.Context;
import android.icu.text.ListFormatter;
import android.os.UserHandle;
import android.text.BidiFormatter;
import android.text.TextUtils;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.accounts.AuthenticatorHelper;

import java.util.ArrayList;
import java.util.List;

public class TopLevelAccountEntryPreferenceController extends BasePreferenceController {
    public TopLevelAccountEntryPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public CharSequence getSummary() {
        final AuthenticatorHelper authHelper = new AuthenticatorHelper(mContext,
                UserHandle.of(UserHandle.myUserId()), null /* OnAccountsUpdateListener */);
        final String[] types = authHelper.getEnabledAccountTypes();
        final BidiFormatter bidiFormatter = BidiFormatter.getInstance();
        final List<CharSequence> summaries = new ArrayList<>();

        if (types == null || types.length == 0) {
            summaries.add(mContext.getString(R.string.account_dashboard_default_summary));
        } else {
            // Show up to 3 account types, ignore any null value
            int accountToAdd = Math.min(3, types.length);

            for (int i = 0; i < types.length && accountToAdd > 0; i++) {
                final CharSequence label = authHelper.getLabelForType(mContext, types[i]);
                if (TextUtils.isEmpty(label)) {
                    continue;
                }

                summaries.add(bidiFormatter.unicodeWrap(label));
                accountToAdd--;
            }
        }
        return ListFormatter.getInstance().format(summaries);
    }
}
