package com.vintozver.ipv6tunnel.helpers;

public class UUIDGen {
	public static java.util.UUID uuidForDate(java.util.Date d)
	{
		/*
		Magic number obtained from #cassandra's thobbs, who
		claims to have stolen it from a Python library.
		*/
		final long NUM_100NS_INTERVALS_SINCE_UUID_EPOCH = 0x01b21dd213814000L;

		long origTime = d.getTime();
		long time = origTime * 10000 + NUM_100NS_INTERVALS_SINCE_UUID_EPOCH;
		long timeLow = time &       0xffffffffL;
		long timeMid = time &   0xffff00000000L;
		long timeHi = time & 0xfff000000000000L;
		long upperLong = (timeLow << 32) | (timeMid >> 16) | (1 << 12) | (timeHi >> 48) ;
		return new java.util.UUID(upperLong, 0xC000000000000000L);
	}
}
