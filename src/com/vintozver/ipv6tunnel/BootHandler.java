package com.vintozver.ipv6tunnel;

import android.content.Intent;
import android.content.Context;
import android.content.BroadcastReceiver;

public class BootHandler extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		Intent msg = new Intent(context, com.vintozver.ipv6tunnel.NetworkService.class);
		msg.setAction(com.vintozver.ipv6tunnel.NetworkService.BOOT);
		context.startService(msg);
	}
}
