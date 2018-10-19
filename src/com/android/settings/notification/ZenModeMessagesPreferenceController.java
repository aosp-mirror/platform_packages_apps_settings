package com.android.settings.notification;

import android.app.NotificationManager;
import android.content.Context;
import android.provider.Settings;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import android.text.TextUtils;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settingslib.core.lifecycle.Lifecycle;

public class ZenModeMessagesPreferenceController extends AbstractZenModePreferenceController
        implements Preference.OnPreferenceChangeListener {

    protected static final String KEY = "zen_mode_messages";

    private final ZenModeBackend mBackend;
    private ListPreference mPreference;
    private final String[] mListValues;

    public ZenModeMessagesPreferenceController(Context context, Lifecycle lifecycle) {
        super(context, KEY, lifecycle);
        mBackend = ZenModeBackend.getInstance(context);
        mListValues = context.getResources().getStringArray(R.array.zen_mode_contacts_values);
    }

    @Override
    public String getPreferenceKey() {
        return KEY;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = (ListPreference) screen.findPreference(KEY);
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        updateFromContactsValue(preference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object selectedContactsFrom) {
        mBackend.saveSenders(NotificationManager.Policy.PRIORITY_CATEGORY_MESSAGES,
                ZenModeBackend.getSettingFromPrefKey(selectedContactsFrom.toString()));
        updateFromContactsValue(preference);
        return true;
    }

    private void updateFromContactsValue(Preference preference) {
        mPreference = (ListPreference) preference;
        switch (getZenMode()) {
            case Settings.Global.ZEN_MODE_NO_INTERRUPTIONS:
            case Settings.Global.ZEN_MODE_ALARMS:
                mPreference.setEnabled(false);
                mPreference.setValue(ZenModeBackend.ZEN_MODE_FROM_NONE);
                mPreference.setSummary(mBackend.getContactsSummary(ZenModeBackend.SOURCE_NONE));
                break;
            default:
                preference.setEnabled(true);
                preference.setSummary(mBackend.getContactsSummary(
                        NotificationManager.Policy.PRIORITY_CATEGORY_MESSAGES));

                final String currentVal = ZenModeBackend.getKeyFromSetting(
                        mBackend.getPriorityMessageSenders());
                mPreference.setValue(mListValues[getIndexOfSendersValue(currentVal)]);
        }
    }

    @VisibleForTesting
    protected int getIndexOfSendersValue(String currentVal) {
        int index = 3; // defaults to "none" based on R.array.zen_mode_contacts_values
        for (int i = 0; i < mListValues.length; i++) {
            if (TextUtils.equals(currentVal, mListValues[i])) {
                return i;
            }
        }

        return index;
    }
}
