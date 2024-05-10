package com.android.settings.tts;

import static android.provider.Settings.Secure.TTS_DEFAULT_SYNTH;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.EngineInfo;
import android.speech.tts.TtsEngines;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;

import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.widget.RadioButtonPickerFragment;
import com.android.settingslib.search.SearchIndexable;
import com.android.settingslib.widget.CandidateInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SearchIndexable
public class TtsEnginePreferenceFragment extends RadioButtonPickerFragment {
    private static final String TAG = "TtsEnginePrefFragment";

    /**
     * The previously selected TTS engine. Useful for rollbacks if the users choice is not loaded or
     * fails a voice integrity check.
     */
    private String mPreviousEngine;

    private TextToSpeech mTts = null;
    private TtsEngines mEnginesHelper = null;
    private Context mContext;
    private Map<String, EngineCandidateInfo> mEngineMap;
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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        mContext = getContext().getApplicationContext();
        mEnginesHelper = new TtsEngines(mContext);
        mEngineMap = new HashMap<>();
        mTts = new TextToSpeech(mContext, null);

        super.onCreate(savedInstanceState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mTts != null) {
            mTts.shutdown();
            mTts = null;
        }
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.TTS_ENGINE_SETTINGS;
    }

    /**
     * Step 3: We have now bound to the TTS engine the user requested. We will attempt to check
     * voice data for the engine if we successfully bound to it, or revert to the previous engine if
     * we didn't.
     */
    public void onUpdateEngine(int status) {
        if (status == TextToSpeech.SUCCESS) {
            Log.d(TAG, "Updating engine: Successfully bound to the engine: "
                    + mTts.getCurrentEngine());
            android.provider.Settings.Secure.putString(
                    mContext.getContentResolver(), TTS_DEFAULT_SYNTH, mTts.getCurrentEngine());
        } else {
            Log.d(TAG, "Updating engine: Failed to bind to engine, reverting.");
            if (mPreviousEngine != null) {
                // This is guaranteed to at least bind, since mPreviousEngine would be
                // null if the previous bind to this engine failed.
                mTts = new TextToSpeech(mContext, null, mPreviousEngine);
                updateCheckedState(mPreviousEngine);
            }
            mPreviousEngine = null;
        }
    }

    @Override
    protected void onRadioButtonConfirmed(String selectedKey) {
        final EngineCandidateInfo info = mEngineMap.get(selectedKey);
        // Should we alert user? if that's true, delay making engine current one.
        if (shouldDisplayDataAlert(info)) {
            displayDataAlert(info, (dialog, which) -> {
                setDefaultKey(selectedKey);
            });
        } else {
            // Privileged engine, set it current
            setDefaultKey(selectedKey);
        }
    }

    @Override
    protected List<? extends CandidateInfo> getCandidates() {
        final List<EngineCandidateInfo> infos = new ArrayList<>();
        final List<EngineInfo> engines = mEnginesHelper.getEngines();
        for (EngineInfo engine : engines) {
            final EngineCandidateInfo info = new EngineCandidateInfo(engine);
            infos.add(info);
            mEngineMap.put(engine.name, info);
        }
        return infos;
    }

    @Override
    protected String getDefaultKey() {
        return mEnginesHelper.getDefaultEngine();
    }

    @Override
    protected boolean setDefaultKey(String key) {
        updateDefaultEngine(key);
        updateCheckedState(key);
        return true;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.tts_engine_picker;
    }

    private boolean shouldDisplayDataAlert(EngineCandidateInfo info) {
        return !info.isSystem();
    }

    private void displayDataAlert(EngineCandidateInfo info,
            DialogInterface.OnClickListener positiveOnClickListener) {
        Log.i(TAG, "Displaying data alert for :" + info.getKey());

        final AlertDialog dialog = new AlertDialog.Builder(getPrefContext())
                .setTitle(android.R.string.dialog_alert_title)
                .setMessage(mContext.getString(
                        com.android.settingslib.R.string.tts_engine_security_warning,
                        info.loadLabel()))
                .setCancelable(true)
                .setPositiveButton(android.R.string.ok, positiveOnClickListener)
                .setNegativeButton(android.R.string.cancel, null)
                .create();

        dialog.show();
    }

    private void updateDefaultEngine(String engine) {
        Log.d(TAG, "Updating default synth to : " + engine);

        // Step 1: Shut down the existing TTS engine.
        Log.i(TAG, "Shutting down current tts engine");
        if (mTts != null) {
            // Keep track of the previous engine that was being used. So that
            // we can reuse the previous engine.
            //
            // Note that if TextToSpeech#getCurrentEngine is not null, it means at
            // the very least that we successfully bound to the engine service.
            mPreviousEngine = mTts.getCurrentEngine();

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
        mTts = new TextToSpeech(mContext, mUpdateListener, engine);
        Log.i(TAG, "Success");
    }

    public static class EngineCandidateInfo extends CandidateInfo {
        private final EngineInfo mEngineInfo;

        EngineCandidateInfo(EngineInfo engineInfo) {
            super(true /* enabled */);
            mEngineInfo = engineInfo;
        }

        @Override
        public CharSequence loadLabel() {
            return mEngineInfo.label;
        }

        @Override
        public Drawable loadIcon() {
            return null;
        }

        @Override
        public String getKey() {
            return mEngineInfo.name;
        }

        public boolean isSystem() {
            return mEngineInfo.system;
        }
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.tts_engine_picker);
}
