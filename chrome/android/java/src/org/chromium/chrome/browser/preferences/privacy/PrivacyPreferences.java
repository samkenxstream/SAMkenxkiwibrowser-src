// Copyright 2015 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.chrome.browser.preferences.privacy;

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import org.chromium.base.metrics.RecordHistogram;
import org.chromium.chrome.R;
import org.chromium.chrome.browser.ChromeFeatureList;
import org.chromium.chrome.browser.contextualsearch.ContextualSearchFieldTrial;
import org.chromium.chrome.browser.help.HelpAndFeedback;
import org.chromium.chrome.browser.preferences.ChromeBaseCheckBoxPreference;
import org.chromium.chrome.browser.preferences.ManagedPreferenceDelegate;
import org.chromium.chrome.browser.preferences.Pref;
import org.chromium.chrome.browser.preferences.PrefServiceBridge;
import org.chromium.chrome.browser.preferences.PreferenceUtils;
import org.chromium.chrome.browser.profiles.Profile;

import org.chromium.base.ContextUtils;
import android.content.SharedPreferences;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;

import android.view.View;
import android.widget.ListView;

import org.chromium.chrome.browser.RestartWorker;
import org.chromium.chrome.R;
import org.chromium.chrome.browser.ChromeFeatureList;
import org.chromium.chrome.browser.accessibility.FontSizePrefs;
import org.chromium.chrome.browser.accessibility.NightModePrefs;
import org.chromium.chrome.browser.accessibility.FontSizePrefs.FontSizePrefsObserver;
import org.chromium.chrome.browser.accessibility.NightModePrefs.NightModePrefsObserver;
import org.chromium.base.ContextUtils;
import org.chromium.ui.widget.Toast;
import org.chromium.chrome.browser.util.AccessibilityUtil;

import android.view.View;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import java.text.NumberFormat;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;

import android.widget.ListView;
import org.chromium.base.ContextUtils;

/**
 * Fragment to keep track of the all the privacy related preferences.
 */
public class PrivacyPreferences extends PreferenceFragment
        implements OnPreferenceChangeListener {
    private static final String PREF_NAVIGATION_ERROR = "navigation_error";
    private static final String PREF_SEARCH_SUGGESTIONS = "search_suggestions";
    private static final String PREF_SAFE_BROWSING_EXTENDED_REPORTING =
            "safe_browsing_extended_reporting";
    private static final String PREF_SAFE_BROWSING_SCOUT_REPORTING =
            "safe_browsing_scout_reporting";
    private static final String PREF_SAFE_BROWSING = "safe_browsing";
    private static final String PREF_CAN_MAKE_PAYMENT = "can_make_payment";
    private static final String PREF_CONTEXTUAL_SEARCH = "contextual_search";
    private static final String PREF_CLOSE_TABS_ON_EXIT = "close_tabs_on_exit";
    private static final String PREF_CLOSE_BROWSER_AFTER_LAST_TAB = "close_browser_after_last_tab";
    private static final String PREF_NETWORK_PREDICTIONS = "network_predictions";
    private static final String PREF_DO_NOT_TRACK = "do_not_track";
    private static final String PREF_USAGE_AND_CRASH_REPORTING = "usage_and_crash_reports";
    private static final String PREF_CLEAR_BROWSING_DATA = "clear_browsing_data";
    private static final String PREF_AVOID_AMP_WEBSITES = "avoid_amp_websites";

    private ManagedPreferenceDelegate mManagedPreferenceDelegate;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PrivacyPreferencesManager privacyPrefManager = PrivacyPreferencesManager.getInstance();
        privacyPrefManager.migrateNetworkPredictionPreferences();
        PreferenceUtils.addPreferencesFromResource(this, R.xml.privacy_preferences);
        getActivity().setTitle(R.string.prefs_privacy);
        setHasOptionsMenu(true);
        PrefServiceBridge prefServiceBridge = PrefServiceBridge.getInstance();

        mManagedPreferenceDelegate = createManagedPreferenceDelegate();

        ChromeBaseCheckBoxPreference networkPredictionPref =
                (ChromeBaseCheckBoxPreference) findPreference(PREF_NETWORK_PREDICTIONS);
        networkPredictionPref.setChecked(prefServiceBridge.getNetworkPredictionEnabled());
        networkPredictionPref.setOnPreferenceChangeListener(this);
        networkPredictionPref.setManagedPreferenceDelegate(mManagedPreferenceDelegate);

        ChromeBaseCheckBoxPreference navigationErrorPref =
                (ChromeBaseCheckBoxPreference) findPreference(PREF_NAVIGATION_ERROR);
        navigationErrorPref.setOnPreferenceChangeListener(this);
        navigationErrorPref.setManagedPreferenceDelegate(mManagedPreferenceDelegate);

        ChromeBaseCheckBoxPreference searchSuggestionsPref =
                (ChromeBaseCheckBoxPreference) findPreference(PREF_SEARCH_SUGGESTIONS);
        searchSuggestionsPref.setOnPreferenceChangeListener(this);
        searchSuggestionsPref.setManagedPreferenceDelegate(mManagedPreferenceDelegate);
        if (ChromeFeatureList.isEnabled(ChromeFeatureList.CONTENT_SUGGESTIONS_SETTINGS)) {
            searchSuggestionsPref.setTitle(R.string.search_site_suggestions_title);
            searchSuggestionsPref.setSummary(R.string.search_site_suggestions_summary);
        }

        PreferenceScreen preferenceScreen = getPreferenceScreen();
        if (!ContextualSearchFieldTrial.isEnabled()) {
            preferenceScreen.removePreference(findPreference(PREF_CONTEXTUAL_SEARCH));
        }

        // Listen to changes to both Extended Reporting prefs.
        ChromeBaseCheckBoxPreference legacyExtendedReportingPref =
                (ChromeBaseCheckBoxPreference) findPreference(
                    PREF_SAFE_BROWSING_EXTENDED_REPORTING);
        legacyExtendedReportingPref.setOnPreferenceChangeListener(this);
        legacyExtendedReportingPref.setManagedPreferenceDelegate(mManagedPreferenceDelegate);
        ChromeBaseCheckBoxPreference scoutReportingPref =
                (ChromeBaseCheckBoxPreference) findPreference(PREF_SAFE_BROWSING_SCOUT_REPORTING);
        scoutReportingPref.setOnPreferenceChangeListener(this);
        scoutReportingPref.setManagedPreferenceDelegate(mManagedPreferenceDelegate);
        // Remove the extended reporting preference that is NOT active.
        String extended_reporting_pref_to_remove =
                prefServiceBridge.isSafeBrowsingScoutReportingActive()
                    ? PREF_SAFE_BROWSING_EXTENDED_REPORTING : PREF_SAFE_BROWSING_SCOUT_REPORTING;
        preferenceScreen.removePreference(findPreference(PREF_SAFE_BROWSING_EXTENDED_REPORTING));
        preferenceScreen.removePreference(findPreference(PREF_SAFE_BROWSING_SCOUT_REPORTING));
        preferenceScreen.removePreference(findPreference(PREF_USAGE_AND_CRASH_REPORTING));

        ChromeBaseCheckBoxPreference safeBrowsingPref =
                (ChromeBaseCheckBoxPreference) findPreference(PREF_SAFE_BROWSING);
        safeBrowsingPref.setOnPreferenceChangeListener(this);
        safeBrowsingPref.setManagedPreferenceDelegate(mManagedPreferenceDelegate);

        ChromeBaseCheckBoxPreference canMakePaymentPref =
                (ChromeBaseCheckBoxPreference) findPreference(PREF_CAN_MAKE_PAYMENT);
        canMakePaymentPref.setOnPreferenceChangeListener(this);

        ((ChromeBaseCheckBoxPreference) findPreference("hide_incognito_window_content")).setOnPreferenceChangeListener(this);

        updateSummaries();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String key = preference.getKey();
        if (PREF_SEARCH_SUGGESTIONS.equals(key)) {
            PrefServiceBridge.getInstance().setSearchSuggestEnabled((boolean) newValue);
        } else if (PREF_SAFE_BROWSING.equals(key)) {
            PrefServiceBridge.getInstance().setSafeBrowsingEnabled((boolean) newValue);
        } else if (PREF_SAFE_BROWSING_EXTENDED_REPORTING.equals(key)
                   || PREF_SAFE_BROWSING_SCOUT_REPORTING.equals(key)) {
            PrefServiceBridge.getInstance().setSafeBrowsingExtendedReportingEnabled(
                    (boolean) newValue);
        } else if (PREF_NETWORK_PREDICTIONS.equals(key)) {
            PrefServiceBridge.getInstance().setNetworkPredictionEnabled((boolean) newValue);
            recordNetworkPredictionEnablingUMA((boolean) newValue);
        } else if (PREF_NAVIGATION_ERROR.equals(key)) {
            PrefServiceBridge.getInstance().setResolveNavigationErrorEnabled((boolean) newValue);
        } else if (PREF_CAN_MAKE_PAYMENT.equals(key)) {
            PrefServiceBridge.getInstance().setBoolean(
                    Pref.CAN_MAKE_PAYMENT_ENABLED, (boolean) newValue);
        } else if (PREF_CLOSE_TABS_ON_EXIT.equals(key)) {
            SharedPreferences.Editor sharedPreferencesEditor = ContextUtils.getAppSharedPreferences().edit();
            sharedPreferencesEditor.putBoolean(PREF_CLOSE_TABS_ON_EXIT, (boolean)newValue);
            sharedPreferencesEditor.apply();
        } else if (PREF_CLOSE_BROWSER_AFTER_LAST_TAB.equals(key)) {
            SharedPreferences.Editor sharedPreferencesEditor = ContextUtils.getAppSharedPreferences().edit();
            sharedPreferencesEditor.putBoolean(PREF_CLOSE_BROWSER_AFTER_LAST_TAB, (boolean)newValue);
            sharedPreferencesEditor.apply();
        } else if (PREF_AVOID_AMP_WEBSITES.equals(key)) {
            SharedPreferences.Editor sharedPreferencesEditor = ContextUtils.getAppSharedPreferences().edit();
            sharedPreferencesEditor.putBoolean(PREF_AVOID_AMP_WEBSITES, (boolean)newValue);
            sharedPreferencesEditor.apply();
        } else if ("hide_incognito_window_content".equals(key)) {
            SharedPreferences.Editor sharedPreferencesEditor = ContextUtils.getAppSharedPreferences().edit();
            sharedPreferencesEditor.putBoolean("hide_incognito_window_content", (boolean)newValue);
            sharedPreferencesEditor.apply();
            AskForRelaunch();
        }

        return true;
    }

    private void recordNetworkPredictionEnablingUMA(boolean enabled) {
        // Report user turning on and off NetworkPrediction.
        RecordHistogram.recordBooleanHistogram("PrefService.NetworkPredictionEnabled", enabled);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateSummaries();
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

    /**
     * Updates the summaries for several preferences.
     */
    public void updateSummaries() {
        PrefServiceBridge prefServiceBridge = PrefServiceBridge.getInstance();

        PrivacyPreferencesManager privacyPrefManager = PrivacyPreferencesManager.getInstance();

        CharSequence textOn = getActivity().getResources().getText(R.string.text_on);
        CharSequence textOff = getActivity().getResources().getText(R.string.text_off);

        CheckBoxPreference navigationErrorPref = (CheckBoxPreference) findPreference(
                PREF_NAVIGATION_ERROR);
        if (navigationErrorPref != null) {
            navigationErrorPref.setChecked(
                    prefServiceBridge.isResolveNavigationErrorEnabled());
        }

        CheckBoxPreference searchSuggestionsPref = (CheckBoxPreference) findPreference(
                PREF_SEARCH_SUGGESTIONS);
        if (searchSuggestionsPref != null) {
            searchSuggestionsPref.setChecked(prefServiceBridge.isSearchSuggestEnabled());
        }

        String extended_reporting_pref = prefServiceBridge.isSafeBrowsingScoutReportingActive()
                ? PREF_SAFE_BROWSING_SCOUT_REPORTING : PREF_SAFE_BROWSING_EXTENDED_REPORTING;
        CheckBoxPreference extendedReportingPref =
                (CheckBoxPreference) findPreference(extended_reporting_pref);
        if (extendedReportingPref != null) {
            extendedReportingPref.setChecked(
                    prefServiceBridge.isSafeBrowsingExtendedReportingEnabled());
        }

        CheckBoxPreference safeBrowsingPref =
                (CheckBoxPreference) findPreference(PREF_SAFE_BROWSING);
        if (safeBrowsingPref != null) {
            safeBrowsingPref.setChecked(prefServiceBridge.isSafeBrowsingEnabled());
        }

        CheckBoxPreference canMakePaymentPref =
                (CheckBoxPreference) findPreference(PREF_CAN_MAKE_PAYMENT);
        if (canMakePaymentPref != null) {
            canMakePaymentPref.setChecked(
                    prefServiceBridge.getBoolean(Pref.CAN_MAKE_PAYMENT_ENABLED));
        }

        Preference doNotTrackPref = findPreference(PREF_DO_NOT_TRACK);
        if (doNotTrackPref != null) {
            doNotTrackPref.setSummary(prefServiceBridge.isDoNotTrackEnabled() ? textOn : textOff);
        }

        Preference contextualPref = findPreference(PREF_CONTEXTUAL_SEARCH);
        if (contextualPref != null) {
            boolean isContextualSearchEnabled = !prefServiceBridge.isContextualSearchDisabled();
            contextualPref.setSummary(isContextualSearchEnabled ? textOn : textOff);
        }

        Preference usageAndCrashPref = findPreference(PREF_USAGE_AND_CRASH_REPORTING);
        if (usageAndCrashPref != null) {
            usageAndCrashPref.setSummary(
                    privacyPrefManager.isUsageAndCrashReportingPermittedByUser() ? textOn
                                                                                 : textOff);
        }
    }

    private ManagedPreferenceDelegate createManagedPreferenceDelegate() {
        return preference -> {
            String key = preference.getKey();
            PrefServiceBridge prefs = PrefServiceBridge.getInstance();
            if (PREF_NAVIGATION_ERROR.equals(key)) {
                return prefs.isResolveNavigationErrorManaged();
            }
            if (PREF_SEARCH_SUGGESTIONS.equals(key)) {
                return prefs.isSearchSuggestManaged();
            }
            if (PREF_SAFE_BROWSING_EXTENDED_REPORTING.equals(key)
                    || PREF_SAFE_BROWSING_SCOUT_REPORTING.equals(key)) {
                return prefs.isSafeBrowsingExtendedReportingManaged();
            }
            if (PREF_SAFE_BROWSING.equals(key)) {
                return prefs.isSafeBrowsingManaged();
            }
            if (PREF_NETWORK_PREDICTIONS.equals(key)) {
                return prefs.isNetworkPredictionManaged();
            }
            return false;
        };
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_id_targeted_help) {
            HelpAndFeedback.getInstance(getActivity())
                    .show(getActivity(), getString(R.string.help_context_privacy),
                            Profile.getLastUsedProfile(), null);
            return true;
        }
        return false;
    }

    private void AskForRelaunch() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this.getActivity());
         alertDialogBuilder
            .setMessage(R.string.preferences_restart_is_needed)
            .setCancelable(true)
            .setPositiveButton(R.string.preferences_restart_now, new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialog,int id) {
                  RestartWorker restartWorker = new RestartWorker();
                  restartWorker.Restart();
                  dialog.cancel();
              }
            })
            .setNegativeButton(R.string.preferences_restart_later,new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialog,int id) {
                  dialog.cancel();
              }
            });
            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();
    }
}
