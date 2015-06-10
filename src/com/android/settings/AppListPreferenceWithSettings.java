package com.android.settings;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

/**
 * An AppListPreference with optional settings button.
 */
public class AppListPreferenceWithSettings extends AppListPreference {

    private View mSettingsIcon;
    private ComponentName mSettingsComponent;

    public AppListPreferenceWithSettings(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWidgetLayoutResource(R.layout.preference_widget_settings);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);

        mSettingsIcon = view.findViewById(R.id.settings_button);
        mSettingsIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.setComponent(mSettingsComponent);
                getContext().startActivity(new Intent(intent));
            }
        });

        ViewGroup container = (ViewGroup) mSettingsIcon.getParent();
        container.setPaddingRelative(0, 0, 0, 0);

        updateSettingsVisibility();
    }

    private void updateSettingsVisibility() {
        if (mSettingsIcon == null) {
            return;
        }

        if (mSettingsComponent == null) {
            mSettingsIcon.setVisibility(View.GONE);
        } else {
            mSettingsIcon.setVisibility(View.VISIBLE);
        }
    }

    protected void setSettingsComponent(ComponentName settings) {
        mSettingsComponent = settings;
        updateSettingsVisibility();
    }
}
