package com.android.settings.tts;

import static android.provider.Settings.Secure.TTS_DEFAULT_SYNTH;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Bundle;
import android.provider.SearchIndexableResource;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.EngineInfo;
import android.speech.tts.TtsEngines;
import android.util.Log;
import android.widget.Checkable;

import androidx.preference.PreferenceCategory;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.tts.TtsEnginePreference.RadioButtonGroupState;
import com.android.settingslib.search.SearchIndexable;

import java.util.Arrays;
import java.util.List;

@SearchIndexable
public class TtsEnginePreferenceFragment extends SettingsPreferenceFragment
        implements RadioButtonGroupState {
    private static final String TAG = "TtsEnginePrefFragment";

    private static final int VOICE_DATA_INTEGRITY_CHECK = 1977;

    /** The currently selected engine. */
    private String mCurrentEngine;

    /**
     * The engine checkbox that is currently checked. Saves us a bit of effort in deducing the right
     * one from the currently selected engine.
     */
    private Checkable mCurrentChecked;

    /**
     * The previously selected TTS engine. Useful for rollbacks if the users choice is not loaded or
     * fails a voice integrity check.
     */
    private String mPreviousEngine;

    private PreferenceCategory mEnginePreferenceCategory;

    private TextToSpeech mTts = null;
    private TtsEngines mEnginesHelper = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.tts_engine_picker);

        mEnginePreferenceCategory =
                (PreferenceCategory) findPreference("tts_engine_preference_category");
        mEnginesHelper = new TtsEngines(getActivity().getApplicationContext());

        mTts = new TextToSpeech(getActivity().getApplicationContext(), null);

        initSettings();
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.TTS_ENGINE_SETTINGS;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mTts != null) {
            mTts.shutdown();
            mTts = null;
        }
    }

    private void initSettings() {
        if (mTts != null) {
            mCurrentEngine = mTts.getCurrentEngine();
        }

        mEnginePreferenceCategory.removeAll();

        List<EngineInfo> engines = mEnginesHelper.getEngines();
        for (EngineInfo engine : engines) {
            TtsEnginePreference enginePref =
                    new TtsEnginePreference(getPrefContext(), engine, this);
            mEnginePreferenceCategory.addPreference(enginePref);
        }
    }

    @Override
    public Checkable getCurrentChecked() {
        return mCurrentChecked;
    }

    @Override
    public String getCurrentKey() {
        return mCurrentEngine;
    }

    @Override
    public void setCurrentChecked(Checkable current) {
        mCurrentChecked = current;
    }

    /**
     * The initialization listener used when the user changes his choice of engine (as opposed to
     * when then screen is being initialized for the first time).
     */
    private final TextToSpeech.OnInitListener mUpdateListener =
            new TextToSpeech.OnInitListener() {
                @Override
                public void onInit(int status) {
                    onUpdateEngine(status);
                }
            };

    private void updateDefaultEngine(String engine) {
        Log.d(TAG, "Updating default synth to : " + engine);

        // Keep track of the previous engine that was being used. So that
        // we can reuse the previous engine.
        //
        // Note that if TextToSpeech#getCurrentEngine is not null, it means at
        // the very least that we successfully bound to the engine service.
        mPreviousEngine = mTts.getCurrentEngine();

        // Step 1: Shut down the existing TTS engine.
        Log.i(TAG, "Shutting down current tts engine");
        if (mTts != null) {
            try {
                mTts.shutdown();
                mTts = null;
            } catch (Exception e) {
                Log.e(TAG, "Error shutting down TTS engine" + e);
            }
        }

        // Step 2: Connect to the new TTS engine.
        // Step 3 is continued on #onUpdateEngine (below) which is called when
        // the app binds successfully to the engine.
        Log.i(TAG, "Updating engine : Attempting to connect to engine: " + engine);
        mTts = new TextToSpeech(getActivity().getApplicationContext(), mUpdateListener, engine);
        Log.i(TAG, "Success");
    }

    /**
     * Step 3: We have now bound to the TTS engine the user requested. We will attempt to check
     * voice data for the engine if we successfully bound to it, or revert to the previous engine if
     * we didn't.
     */
    public void onUpdateEngine(int status) {
        if (status == TextToSpeech.SUCCESS) {
            Log.d(

                    TAG,
                    "Updating engine: Successfully bound to the engine: "
                            + mTts.getCurrentEngine());
            android.provider.Settings.Secure.putString(
                    getContentResolver(), TTS_DEFAULT_SYNTH, mTts.getCurrentEngine());
        } else {
            Log.d(TAG, "Updating engine: Failed to bind to engine, reverting.");
            if (mPreviousEngine != null) {
                // This is guaranteed to at least bind, since mPreviousEngine would be
                // null if the previous bind to this engine failed.
                mTts =
                        new TextToSpeech(
                                getActivity().getApplicationContext(), null, mPreviousEngine);
            }
            mPreviousEngine = null;
        }
    }

    @Override
    public void setCurrentKey(String key) {
        mCurrentEngine = key;
        updateDefaultEngine(mCurrentEngine);
    }

    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(
                        Context context, boolean enabled) {
                    final SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.tts_engine_picker;
                    return Arrays.asList(sir);
                }
            };
}
