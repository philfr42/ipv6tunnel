package com.vintozver.ipv6tunnel;

import android.preference.Preference;

public abstract class PreferenceChangeListenerBase<A> extends java.lang.Object implements Preference.OnPreferenceChangeListener {
	private A self;

	public A getOwner() {
		return self;
	}
	
	public PreferenceChangeListenerBase(A self) {
		this.self = self;
	}

}
