// Copyright 2015 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.chrome.browser.preferences.privacy;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;

import org.chromium.chrome.R;
import org.chromium.chrome.browser.preferences.ChromeSwitchPreference;
import org.chromium.chrome.browser.preferences.PrefServiceBridge;
import org.chromium.chrome.browser.preferences.PreferenceUtils;

import android.view.View;
import org.chromium.base.ContextUtils;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.widget.ListView;

/**
 * Fragment to manage 'Do Not Track' preference and to explain to the user what it does.
 */
public class DoNotTrackPreference extends PreferenceFragment {

    private static final String PREF_DO_NOT_TRACK_SWITCH = "do_not_track_switch";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PreferenceUtils.addPreferencesFromResource(this, R.xml.do_not_track_preferences);
        getActivity().setTitle(R.string.do_not_track_title);

        ChromeSwitchPreference doNotTrackSwitch =
                (ChromeSwitchPreference) findPreference(PREF_DO_NOT_TRACK_SWITCH);

        boolean isDoNotTrackEnabled = PrefServiceBridge.getInstance().isDoNotTrackEnabled();
        doNotTrackSwitch.setChecked(isDoNotTrackEnabled);

        doNotTrackSwitch.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                PrefServiceBridge.getInstance().setDoNotTrackEnabled((boolean) newValue);
                return true;
            }
        });
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (ContextUtils.getAppSharedPreferences().getBoolean("user_night_mode_enabled", false) || ContextUtils.getAppSharedPreferences().getString("active_theme", "").equals("Diamond Black")) {
            view.setBackgroundColor(Color.BLACK);
            ListView list = (ListView) view.findViewById(android.R.id.list);
            if (list != null)
                list.setDivider(new ColorDrawable(Color.GRAY));
                list.setDividerHeight((int) getResources().getDisplayMetrics().density);
        }
    }
}
