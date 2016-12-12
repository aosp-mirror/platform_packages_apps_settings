package com.android.settings.tts;

import android.speech.tts.TextToSpeech;
import com.android.settings.R;
import android.os.Bundle;
import com.android.settings.SettingsPreferenceFragment;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import android.util.Log;
import com.android.settings.SeekBarPreference;
import android.support.v7.preference.Preference;
import android.content.ContentResolver;
import com.android.settings.search.Indexable;
import com.android.settings.search.BaseSearchIndexProvider;
import android.content.Context;
import android.provider.SearchIndexableResource;

import java.util.List;
import java.util.Arrays;

import static android.provider.Settings.Secure.TTS_DEFAULT_PITCH;
import static android.provider.Settings.Secure.TTS_DEFAULT_RATE;

public class TtsSlidersFragment extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener,
                Preference.OnPreferenceClickListener,
                Indexable {
    private static final String TAG = TtsSlidersFragment.class.getSimpleName();
    private static final boolean DBG = false;

    /** Preference key for the TTS pitch selection slider. */
    private static final String KEY_DEFAULT_PITCH = "tts_default_pitch";

    /** Preference key for the TTS rate selection slider. */
    private static final String KEY_DEFAULT_RATE = "tts_default_rate";

    /** Preference key for the TTS reset speech rate preference. */
    private static final String KEY_RESET_SPEECH_RATE = "reset_speech_rate";

    /** Preference key for the TTS reset speech pitch preference. */
    private static final String KEY_RESET_SPEECH_PITCH = "reset_speech_pitch";

    /**
     * Speech rate value. This value should be kept in sync with the max value set in tts_settings
     * xml.
     */
    private static final int MAX_SPEECH_RATE = 600;

    private static final int MIN_SPEECH_RATE = 10;

    /**
     * Speech pitch value. TTS pitch value varies from 25 to 400, where 100 is the value for normal
     * pitch. The max pitch value is set to 400, based on feedback from users and the GoogleTTS
     * pitch variation range. The range for pitch is not set in stone and should be readjusted based
     * on user need. This value should be kept in sync with the max value set in tts_settings xml.
     */
    private static final int MAX_SPEECH_PITCH = 400;

    private static final int MIN_SPEECH_PITCH = 25;

    private int mDefaultPitch = TextToSpeech.Engine.DEFAULT_PITCH;
    private int mDefaultRate = TextToSpeech.Engine.DEFAULT_RATE;

    private SeekBarPreference mDefaultPitchPref;
    private SeekBarPreference mDefaultRatePref;
    private Preference mResetSpeechRate;
    private Preference mResetSpeechPitch;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.tts_sliders);

        mResetSpeechRate = findPreference(KEY_RESET_SPEECH_RATE);
        mResetSpeechRate.setOnPreferenceClickListener(this);
        mResetSpeechPitch = findPreference(KEY_RESET_SPEECH_PITCH);
        mResetSpeechPitch.setOnPreferenceClickListener(this);

        mDefaultPitchPref = (SeekBarPreference) findPreference(KEY_DEFAULT_PITCH);
        mDefaultRatePref = (SeekBarPreference) findPreference(KEY_DEFAULT_RATE);

        initSettings();
    }

    private void initSettings() {
        final ContentResolver resolver = getContentResolver();
        // Set up the default rate and pitch.
        mDefaultRate =
                android.provider.Settings.Secure.getInt(
                        resolver, TTS_DEFAULT_RATE, TextToSpeech.Engine.DEFAULT_RATE);
        mDefaultPitch =
                android.provider.Settings.Secure.getInt(
                        resolver, TTS_DEFAULT_PITCH, TextToSpeech.Engine.DEFAULT_PITCH);

        mDefaultRatePref.setProgress(getSeekBarProgressFromValue(KEY_DEFAULT_RATE, mDefaultRate));
        mDefaultRatePref.setOnPreferenceChangeListener(this);
        mDefaultRatePref.setMax(getSeekBarProgressFromValue(KEY_DEFAULT_RATE, MAX_SPEECH_RATE));

        mDefaultPitchPref.setProgress(
                getSeekBarProgressFromValue(KEY_DEFAULT_PITCH, mDefaultPitch));
        mDefaultPitchPref.setOnPreferenceChangeListener(this);
        mDefaultPitchPref.setMax(getSeekBarProgressFromValue(KEY_DEFAULT_PITCH, MAX_SPEECH_PITCH));
    }

    /**
     * The minimum speech pitch/rate value should be > 0 but the minimum value of a seekbar in
     * android is fixed at 0. Therefore, we increment the seekbar progress with MIN_SPEECH_VALUE so
     * that the minimum seekbar progress value is MIN_SPEECH_PITCH/RATE. SPEECH_VALUE =
     * MIN_SPEECH_VALUE + SEEKBAR_PROGRESS
     */
    private int getValueFromSeekBarProgress(String preferenceKey, int progress) {
        if (preferenceKey.equals(KEY_DEFAULT_RATE)) {
            return MIN_SPEECH_RATE + progress;
        } else if (preferenceKey.equals(KEY_DEFAULT_PITCH)) {
            return MIN_SPEECH_PITCH + progress;
        }
        return progress;
    }

    /**
     * Since we are appending the MIN_SPEECH value to the speech seekbar progress, the speech
     * seekbar progress should be set to (speechValue - MIN_SPEECH value).
     */
    private int getSeekBarProgressFromValue(String preferenceKey, int value) {
        if (preferenceKey.equals(KEY_DEFAULT_RATE)) {
            return value - MIN_SPEECH_RATE;
        } else if (preferenceKey.equals(KEY_DEFAULT_PITCH)) {
            return value - MIN_SPEECH_PITCH;
        }
        return value;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        if (KEY_DEFAULT_RATE.equals(preference.getKey())) {
            updateSpeechRate((Integer) objValue);
        } else if (KEY_DEFAULT_PITCH.equals(preference.getKey())) {
            updateSpeechPitchValue((Integer) objValue);
        }
        return true;
    }

    /** Called when mPlayExample, mResetSpeechRate or mResetSpeechPitch is clicked. */
    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference == mResetSpeechRate) {
            int speechRateSeekbarProgress =
                    getSeekBarProgressFromValue(KEY_DEFAULT_RATE, TextToSpeech.Engine.DEFAULT_RATE);
            mDefaultRatePref.setProgress(speechRateSeekbarProgress);
            updateSpeechRate(speechRateSeekbarProgress);
            return true;
        } else if (preference == mResetSpeechPitch) {
            int pitchSeekbarProgress =
                    getSeekBarProgressFromValue(
                            KEY_DEFAULT_PITCH, TextToSpeech.Engine.DEFAULT_PITCH);
            mDefaultPitchPref.setProgress(pitchSeekbarProgress);
            updateSpeechPitchValue(pitchSeekbarProgress);
            return true;
        }
        return false;
    }

    private void updateSpeechRate(int speechRateSeekBarProgress) {
        mDefaultRate = getValueFromSeekBarProgress(KEY_DEFAULT_RATE, speechRateSeekBarProgress);
        try {
            android.provider.Settings.Secure.putInt(
                    getContentResolver(), TTS_DEFAULT_RATE, mDefaultRate);
            if (DBG) Log.d(TAG, "TTS default rate changed, now " + mDefaultRate);
        } catch (NumberFormatException e) {
            Log.e(TAG, "could not persist default TTS rate setting", e);
        }
        return;
    }

    private void updateSpeechPitchValue(int speechPitchSeekBarProgress) {
        mDefaultPitch = getValueFromSeekBarProgress(KEY_DEFAULT_PITCH, speechPitchSeekBarProgress);
        try {
            android.provider.Settings.Secure.putInt(
                    getContentResolver(), TTS_DEFAULT_PITCH, mDefaultPitch);
            if (DBG) Log.d(TAG, "TTS default pitch changed, now" + mDefaultPitch);
        } catch (NumberFormatException e) {
            Log.e(TAG, "could not persist default TTS pitch setting", e);
        }
        return;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.TTS_SLIDERS;
    }

    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(
                        Context context, boolean enabled) {
                    Log.i(TAG, "Indexing");
                    final SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.tts_sliders;
                    return Arrays.asList(sir);
                }
            };
}
