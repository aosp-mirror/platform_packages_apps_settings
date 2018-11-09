package com.android.settings.accounts;

import android.accounts.Account;
import android.content.Context;
import android.content.Intent;

public class AccountFeatureProviderImpl implements AccountFeatureProvider {
    @Override
    public String getAccountType() {
        return null;
    }

    @Override
    public Account[] getAccounts(Context context) {
        return new Account[0];
    }

    @Override
    public Intent getAccountSettingsDeeplinkIntent() {
        return null;
    }
}
