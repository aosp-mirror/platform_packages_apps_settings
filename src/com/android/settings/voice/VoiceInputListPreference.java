package com.android.settings.voice;

import android.content.ComponentName;
import android.content.Context;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;

import com.android.settings.AppListPreferenceWithSettings;
import com.android.settings.R;

import java.util.ArrayList;
import java.util.List;

public class VoiceInputListPreference extends AppListPreferenceWithSettings {

    private VoiceInputHelper mHelper;

    // The assist component name to restrict available voice inputs.
    private ComponentName mAssistRestrict;

    private final List<Integer> mAvailableIndexes = new ArrayList<>();

    public VoiceInputListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setDialogTitle(R.string.choose_voice_input_title);
    }

    @Override
    protected ListAdapter createListAdapter() {
        return new CustomAdapter(getContext(), getEntries());
    }

    @Override
    protected boolean persistString(String value) {
        for (int i = 0; i < mHelper.mAvailableInteractionInfos.size(); ++i) {
            VoiceInputHelper.InteractionInfo info = mHelper.mAvailableInteractionInfos.get(i);
            if (info.key.equals(value)) {
                Settings.Secure.putString(getContext().getContentResolver(),
                        Settings.Secure.VOICE_INTERACTION_SERVICE, value);
                Settings.Secure.putString(getContext().getContentResolver(),
                        Settings.Secure.VOICE_RECOGNITION_SERVICE,
                        new ComponentName(info.service.packageName,
                                info.serviceInfo.getRecognitionService())
                                .flattenToShortString());
                setSummary(getEntry());
                setSettingsComponent(info.settings);
                return true;
            }
        }

        for (int i = 0; i < mHelper.mAvailableRecognizerInfos.size(); ++i) {
            VoiceInputHelper.RecognizerInfo info = mHelper.mAvailableRecognizerInfos.get(i);
            if (info.key.equals(value)) {
                Settings.Secure.putString(getContext().getContentResolver(),
                        Settings.Secure.VOICE_INTERACTION_SERVICE, "");
                Settings.Secure.putString(getContext().getContentResolver(),
                        Settings.Secure.VOICE_RECOGNITION_SERVICE, value);
                setSummary(getEntry());
                setSettingsComponent(info.settings);
               return true;
            }
        }

        setSettingsComponent(null);
        return true;
    }

    @Override
    public void setPackageNames(CharSequence[] packageNames, CharSequence defaultPackageName) {
        // Skip since all entries are created from |mHelper|.
    }

    public void setAssistRestrict(ComponentName assistRestrict) {
        mAssistRestrict = assistRestrict;
    }

    public void refreshVoiceInputs() {
        mHelper = new VoiceInputHelper(getContext());
        mHelper.buildUi();

        final String assistKey =
                mAssistRestrict == null ? "" : mAssistRestrict.flattenToShortString();

        mAvailableIndexes.clear();
        List<CharSequence> entries = new ArrayList<>();
        List<CharSequence> values = new ArrayList<>();
        for (int i = 0; i < mHelper.mAvailableInteractionInfos.size(); ++i) {
            VoiceInputHelper.InteractionInfo info = mHelper.mAvailableInteractionInfos.get(i);
            entries.add(info.appLabel);
            values.add(info.key);

            if (info.key.contentEquals(assistKey)) {
                mAvailableIndexes.add(i);
            }
        }

        final boolean assitIsService = !mAvailableIndexes.isEmpty();
        final int serviceCount = entries.size();

        for (int i = 0; i < mHelper.mAvailableRecognizerInfos.size(); ++i) {
            VoiceInputHelper.RecognizerInfo info = mHelper.mAvailableRecognizerInfos.get(i);
            entries.add(info.label);
            values.add(info.key);
            if (!assitIsService) {
                mAvailableIndexes.add(serviceCount + i);
            }
        }
        setEntries(entries.toArray(new CharSequence[entries.size()]));
        setEntryValues(values.toArray(new CharSequence[values.size()]));

        if (mHelper.mCurrentVoiceInteraction != null) {
            setValue(mHelper.mCurrentVoiceInteraction.flattenToShortString());
        } else if (mHelper.mCurrentRecognizer != null) {
            setValue(mHelper.mCurrentRecognizer.flattenToShortString());
        } else {
            setValue(null);
        }
    }

    public ComponentName getCurrentService() {
        if (mHelper.mCurrentVoiceInteraction != null) {
            return mHelper.mCurrentVoiceInteraction;
        } else if (mHelper.mCurrentRecognizer != null) {
            return mHelper.mCurrentRecognizer;
        } else {
            return null;
        }
    }

    private class CustomAdapter extends ArrayAdapter<CharSequence> {

        public CustomAdapter(Context context, CharSequence[] objects) {
            super(context, com.android.internal.R.layout.select_dialog_singlechoice_material,
                    android.R.id.text1, objects);
        }

        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }

        @Override
        public boolean isEnabled(int position) {
            return mAvailableIndexes.contains(position);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = super.getView(position, convertView, parent);
            view.setEnabled(isEnabled(position));
            return view;
        }
    }
}
