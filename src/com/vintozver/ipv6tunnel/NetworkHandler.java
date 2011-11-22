package com.vintozver.ipv6tunnel;

import android.content.Intent;
import android.content.Context;
import android.content.BroadcastReceiver;

public class NetworkHandler extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		System.out.println("Event handling: connectivity changed");
		final boolean connectivity = !intent.getBooleanExtra(android.net.ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
		if (connectivity) {
			System.out.println("No connectivity right now");
		}
		
        Intent msg = new Intent(context, com.vintozver.ipv6tunnel.NetworkService.class);
		msg.setAction(com.vintozver.ipv6tunnel.NetworkService.NETWORK_STATUS);
		msg.putExtra("flag", connectivity);
		context.startService(msg);
	}
}
