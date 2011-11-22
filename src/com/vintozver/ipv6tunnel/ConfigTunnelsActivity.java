package com.vintozver.ipv6tunnel;

import java.util.List;
import java.util.Arrays;
import android.os.Bundle;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.PreferenceActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;
import com.vintozver.ipv6tunnel.R;

public class ConfigTunnelsActivity extends PreferenceActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	protected void onPause() {
		setPreferenceScreen(null);

		super.onPause();
	}

	@Override
	protected void onResume() {
		PreferenceScreen pref_root = getPreferenceManager().createPreferenceScreen(this);

		SharedPreferences config = getSharedPreferences(getPackageName(), MODE_WORLD_READABLE);
		String tunnels = config.getString("tunnels", "");
		List<String> tunnels_list = Arrays.asList(tunnels.split("\n"));
		for (String tunnel : tunnels_list) {
			if (tunnel.length() == 0) continue;
			String remote_server = config.getString(String.format("tunnels.%s.remote_server", tunnel), "0.0.0.0");
			String local_client = config.getString(String.format("tunnels.%s.local_client", tunnel), "0.0.0.0");
			boolean config_updating = config.getBoolean(String.format("tunnels.%s.updating_active", tunnel), false);
			if (config_updating) {
				local_client = "<dynamic>";
			}
			String tunnel_address = config.getString(String.format("tunnels.%s.tunnel_address", tunnel), "::0/0");

	        Preference pref = new Preference(this);
	        pref.setKey(tunnel);
	        pref.setTitle(String.format("%s - %s", local_client, remote_server));
	        pref.setSummary(tunnel_address);
	        pref.setOnPreferenceClickListener(new TunnelPreferenceListener(this));
	        pref_root.addPreference(pref);
		}

		setPreferenceScreen(pref_root);

		super.onResume();
	}

	private class TunnelPreferenceListener extends PreferenceClickListenerBase<ConfigTunnelsActivity> {
		public TunnelPreferenceListener(ConfigTunnelsActivity self) {
			super(self);
		}

		@Override
		public boolean onPreferenceClick(Preference preference) {
			try {
				Intent msg_new_tunnel = new Intent(getOwner(), ConfigTunnelActivity.class);
				msg_new_tunnel.putExtra("tunnel_id", preference.getKey());
				startActivity(msg_new_tunnel);
			} catch (ActivityNotFoundException e) {
				System.err.println("Cannot start ConfigTunnel activity");
			}
			return true;
		}
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.xml.config_tunnels_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	    case R.id.config_tunnel_add: {
			Intent msg_new_tunnel = new Intent(this, ConfigTunnelActivity.class);
			startActivity(msg_new_tunnel);
	    	return true;
	    }
	    
	    default:
	        return super.onOptionsItemSelected(item);
	    }
	}
}
