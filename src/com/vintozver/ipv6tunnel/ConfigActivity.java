package com.vintozver.ipv6tunnel;

import java.util.IllegalFormatException;

import android.content.Intent;
import android.content.ActivityNotFoundException;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceActivity;
import com.vintozver.ipv6tunnel.PreferenceChangeListenerBase;
import com.vintozver.ipv6tunnel.PreferenceClickListenerBase;
import com.vintozver.ipv6tunnel.ConfigTunnelsActivity;
import com.vintozver.ipv6tunnel.R;

public class ConfigActivity extends PreferenceActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        addPreferencesFromResource(R.xml.config);

        Preference preference_active = findPreference("active");
        preference_active.setOnPreferenceChangeListener(new ActivePreferenceListener(this));
        Preference preference_tunnels = findPreference("tunnels");
        preference_tunnels.setOnPreferenceClickListener(new TunnelsPreferenceListener(this));
    }

    @Override
	protected void onResume() {
		SharedPreferences config = getSharedPreferences(getPackageName(), MODE_WORLD_READABLE);
		boolean config_active = config.getBoolean("global_active", false);
        ((CheckBoxPreference)findPreference("active")).setChecked(config_active);

        super.onResume();
    }

    @Override
    protected void onPause() {
		super.onPause();
    }

    private class ActivePreferenceListener extends PreferenceChangeListenerBase<ConfigActivity> {
		public ActivePreferenceListener(ConfigActivity self) {
			super(self);
		}

		@Override
		public boolean onPreferenceChange(Preference preference, Object newValue) {
			boolean config_active = ((Boolean)newValue).booleanValue();
			try {
				System.err.printf("Global active status is set to %b\n", config_active);
			} catch (IllegalFormatException e) {
			} catch (NullPointerException e) {
			}
			SharedPreferences config = getSharedPreferences(getPackageName(), MODE_PRIVATE);
			SharedPreferences.Editor config_editor = config.edit();
			config_editor.putBoolean("global_active", config_active);
			config_editor.commit();
			
			Intent service_msg = new Intent(getOwner(), NetworkService.class);
			service_msg.setAction(com.vintozver.ipv6tunnel.NetworkService.GLOBAL_ACTIVATION);
			service_msg.putExtra("flag", config_active);
			startService(service_msg);
			return true;
		}
    }

    private class TunnelsPreferenceListener extends PreferenceClickListenerBase<ConfigActivity> {
		public TunnelsPreferenceListener(ConfigActivity self) {
			super(self);
		}

		@Override
		public boolean onPreferenceClick(Preference preference) {
			try {
				Intent tunnels_list_message = new Intent(getOwner(), ConfigTunnelsActivity.class);
				startActivity(tunnels_list_message);
			} catch (ActivityNotFoundException e) {
				System.err.println("Cannot start ConfigTunnels activity");
			}
			return true;
		}
    }
}
