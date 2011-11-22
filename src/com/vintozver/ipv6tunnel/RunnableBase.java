package com.vintozver.ipv6tunnel;

public abstract class RunnableBase<A> extends java.lang.Object implements java.lang.Runnable {
	private A self;
	private java.lang.Object[] params;

	public A getOwner() {
		return self;
	}
	
	public java.lang.Object[] getParams() {
		return params;
	}
	
	public RunnableBase(A self, java.lang.Object ... params) {
		this.self = self;
		this.params = params;
	}

}
