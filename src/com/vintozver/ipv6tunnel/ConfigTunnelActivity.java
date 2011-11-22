package com.vintozver.ipv6tunnel;

import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;
import java.util.Date;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.PreferenceActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;
import com.vintozver.ipv6tunnel.UUIDGen;
import com.vintozver.ipv6tunnel.R;

public class ConfigTunnelActivity extends PreferenceActivity {
	private boolean isModified = false;
	
	protected boolean getModified() {
		return isModified;
	}
	
	protected void setModified() {
		isModified = true;
	}
	
	protected void loadData() {
        final Intent self_msg = getIntent(); 
		String tunnel_id = self_msg.getStringExtra("tunnel_id");

		SharedPreferences config = getSharedPreferences(getPackageName(), MODE_WORLD_READABLE);
		final boolean config_active = config.getBoolean(String.format("tunnels.%s.active", tunnel_id), false);
		final String config_remote_server = config.getString(String.format("tunnels.%s.remote_server", tunnel_id), "");
		final String config_local_client = config.getString(String.format("tunnels.%s.local_client", tunnel_id), "");
		final boolean config_local_autodetect = config.getBoolean(String.format("tunnels.%s.local_autodetect", tunnel_id), false);
		final String config_tunnel_address = config.getString(String.format("tunnels.%s.tunnel_address", tunnel_id), "");
		final boolean config_updating_active = config.getBoolean(String.format("tunnels.%s.updating_active", tunnel_id), false);
		final String config_updating_username = config.getString(String.format("tunnels.%s.updating_username", tunnel_id), "");
		final String config_updating_password = config.getString(String.format("tunnels.%s.updating_password", tunnel_id), "");
		final String config_updating_endpoint = config.getString(String.format("tunnels.%s.updating_endpoint", tunnel_id), "");
		
		findPreference("local_client").setEnabled(!config_local_autodetect);
		findPreference("updating_username").setEnabled(config_updating_active);
		findPreference("updating_password").setEnabled(config_updating_active);
		findPreference("updating_endpoint").setEnabled(config_updating_active);

		((CheckBoxPreference)findPreference("active")).setChecked(config_active);
		((EditTextPreference)findPreference("remote_server")).setText(config_remote_server);
		((EditTextPreference)findPreference("local_client")).setText(config_local_client);
		((CheckBoxPreference)findPreference("local_autodetect")).setChecked(config_local_autodetect);
		((EditTextPreference)findPreference("tunnel_address")).setText(config_tunnel_address);
		((CheckBoxPreference)findPreference("updating_active")).setChecked(config_updating_active);
		((EditTextPreference)findPreference("updating_username")).setText(config_updating_username);
		((EditTextPreference)findPreference("updating_password")).setText(config_updating_password);
		((EditTextPreference)findPreference("updating_endpoint")).setText(config_updating_endpoint);
	}
	
	protected void saveData() {
		final boolean config_active = ((CheckBoxPreference)findPreference("active")).isChecked();
		final String config_remote_server = ((EditTextPreference)findPreference("remote_server")).getText();
		final String config_local_client = ((EditTextPreference)findPreference("local_client")).getText();
		final boolean config_local_autodetect = ((CheckBoxPreference)findPreference("local_autodetect")).isChecked();
		final String config_tunnel_address = ((EditTextPreference)findPreference("tunnel_address")).getText();
		final boolean config_updating_active = ((CheckBoxPreference)findPreference("updating_active")).isChecked();
		final String config_updating_username = ((EditTextPreference)findPreference("updating_username")).getText();
		final String config_updating_password = ((EditTextPreference)findPreference("updating_password")).getText();
		final String config_updating_endpoint = ((EditTextPreference)findPreference("updating_endpoint")).getText();

        final Intent self_msg = getIntent(); 
		String tunnel_id = self_msg.getStringExtra("tunnel_id");
		
        SharedPreferences config = getSharedPreferences(getPackageName(), MODE_PRIVATE);
        SharedPreferences.Editor config_editor = config.edit();
        config_editor.putBoolean(String.format("tunnels.%s.active", tunnel_id), config_active);
        config_editor.putString(String.format("tunnels.%s.remote_server", tunnel_id), config_remote_server);
        config_editor.putString(String.format("tunnels.%s.local_client", tunnel_id), config_local_client);
        config_editor.putBoolean(String.format("tunnels.%s.local_autodetect", tunnel_id), config_local_autodetect);
        config_editor.putString(String.format("tunnels.%s.tunnel_address", tunnel_id), config_tunnel_address);
        config_editor.putBoolean(String.format("tunnels.%s.updating_active", tunnel_id), config_updating_active);
        config_editor.putString(String.format("tunnels.%s.updating_username", tunnel_id), config_updating_username);
        config_editor.putString(String.format("tunnels.%s.updating_password", tunnel_id), config_updating_password);
        config_editor.putString(String.format("tunnels.%s.updating_endpoint", tunnel_id), config_updating_endpoint);
		Set<String> tunnels_set = new HashSet<String>(Arrays.asList(config.getString("tunnels", "").split("\n")));
		tunnels_set.add(tunnel_id);
		config_editor.putString("tunnels", android.text.TextUtils.join("\n", tunnels_set));
        config_editor.commit();

		isModified = false;

		Intent msg = new Intent(this, com.vintozver.ipv6tunnel.NetworkService.class);
		msg.setAction(com.vintozver.ipv6tunnel.NetworkService.TUNNEL_CHANGE);
		if (getIntent().getBooleanExtra("create", false)) {
			msg.putExtra("action", "add");
		} else {
			msg.putExtra("action", "update");
		}
		msg.putExtra("tunnel_id", tunnel_id);
		this.startService(msg);
	}
	
	protected void removeData() {
        final Intent self_msg = getIntent(); 
		String tunnel_id = self_msg.getStringExtra("tunnel_id");

		SharedPreferences config = getSharedPreferences(getPackageName(), MODE_PRIVATE);
        SharedPreferences.Editor config_editor = config.edit();
        config_editor.remove(String.format("tunnels.%s.active", tunnel_id));
        config_editor.remove(String.format("tunnels.%s.remote_server", tunnel_id));
        config_editor.remove(String.format("tunnels.%s.local_client", tunnel_id));
        config_editor.remove(String.format("tunnels.%s.local_autodetect", tunnel_id));
        config_editor.remove(String.format("tunnels.%s.tunnel_address", tunnel_id));
        config_editor.remove(String.format("tunnels.%s.updating_active", tunnel_id));
        config_editor.remove(String.format("tunnels.%s.updating_username", tunnel_id));
        config_editor.remove(String.format("tunnels.%s.updating_password", tunnel_id));
        config_editor.remove(String.format("tunnels.%s.updating_endpoint", tunnel_id));
		Set<String> tunnels_set = new HashSet<String>(Arrays.asList(config.getString("tunnels", "").split("\n")));
		tunnels_set.remove(tunnel_id);
		config_editor.putString("tunnels", android.text.TextUtils.join("\n", tunnels_set));
        config_editor.commit();

        isModified = false;

        Intent msg = new Intent(this, com.vintozver.ipv6tunnel.NetworkService.class);
		msg.setAction(com.vintozver.ipv6tunnel.NetworkService.TUNNEL_CHANGE);
		msg.putExtra("action", "remove");
		msg.putExtra("tunnel_id", tunnel_id);
		this.startService(msg);
	}

	protected void discardData() {
		isModified = false;
	}
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent self_msg = getIntent(); 
        if (self_msg.getStringExtra("tunnel_id") == null) {
        	self_msg.putExtra("tunnel_id", UUIDGen.uuidForDate(new Date()).toString());
        	self_msg.putExtra("create", true);
        }
        
        addPreferencesFromResource(R.xml.config_tunnel);
        
        PreferenceListener preference_listener = new PreferenceListener(this);
        findPreference("active").setOnPreferenceChangeListener(preference_listener);
        findPreference("remote_server").setOnPreferenceChangeListener(preference_listener);
        findPreference("local_client").setOnPreferenceChangeListener(preference_listener);
        findPreference("local_autodetect").setOnPreferenceChangeListener(preference_listener);
        findPreference("tunnel_address").setOnPreferenceChangeListener(preference_listener);
        findPreference("updating_active").setOnPreferenceChangeListener(preference_listener);
        findPreference("updating_username").setOnPreferenceChangeListener(preference_listener);
        findPreference("updating_password").setOnPreferenceChangeListener(preference_listener);
        findPreference("updating_endpoint").setOnPreferenceChangeListener(preference_listener);
    }

    @Override
	protected void onResume() {
    	loadData();

        super.onResume();

    }

    @Override
    protected void onPause() {
		super.onPause();
    }

    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.xml.config_tunnel_menu, menu);
        final Intent self_msg = getIntent();
        if (self_msg.getBooleanExtra("create", false)) {
        	menu.findItem(R.id.config_tunnel_remove).setEnabled(false);
        }
		return true;
	}

    private class PreferenceListener extends PreferenceChangeListenerBase<ConfigTunnelActivity> {
		public PreferenceListener(ConfigTunnelActivity self) {
			super(self);
		}

		@Override
		public boolean onPreferenceChange(Preference preference, Object newValue) {
			getOwner().setModified();
			if (preference.getKey().equals("local_autodetect")) {
				final boolean config_local_autodetect = ((Boolean)newValue).booleanValue();
				findPreference("local_client").setEnabled(!config_local_autodetect);
				return true;
			}
			if (preference.getKey().equals("updating_active")) {
				final boolean config_updating_active = ((Boolean)newValue).booleanValue();
				findPreference("updating_username").setEnabled(config_updating_active);
				findPreference("updating_password").setEnabled(config_updating_active);
				findPreference("updating_endpoint").setEnabled(config_updating_active);
				return true;
			}
			return true;
		}
    }

    @Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	    case R.id.config_tunnel_save: {
	    	saveData();
	    	finish();

	    	return true;
	    }

	    case R.id.config_tunnel_discard: {
	    	discardData();
	    	finish();
	    	
	    	return true;
	    }

	    case R.id.config_tunnel_remove: {
	    	removeData();
	    	finish();

	    	return true;
	    }

	    default:
	        return super.onOptionsItemSelected(item);
	    }
	}
}
