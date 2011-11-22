package com.vintozver.ipv6tunnel;

import android.preference.Preference;

public abstract class PreferenceClickListenerBase<A> extends java.lang.Object implements Preference.OnPreferenceClickListener {
	private A self;

	public A getOwner() {
		return self;
	}
	
	public PreferenceClickListenerBase(A self) {
		this.self = self;
	}

}
