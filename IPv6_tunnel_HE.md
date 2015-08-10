# Introduction #

First of all, you will need to register at http://tunnelbroker.net
After successful registration you will be able to add your own tunnel.

Server IPv4 Address: _216.66.84.46_

Server IPv6 Address: _2001:470:1f14:1ecb::1/64_

Client IPv4 Address: _86.57.159.72_

Client IPv6 Address: _2001:470:1f14:1ecb::2/64_


This is the example config. Substitute the config with yours and enter into configs. Activate.

# Details #

You will probably need to change your IP address.
Probably, your provider even assigns you dynamic address.

In this case, use HTTP endpoint
https://ipv4.tunnelbroker.net/ipv4_end.php?tid=139967

_139967_ is your tunnel ID, change for yours.

Also, provide your he.net credentials. The program will kick this URL after successfull tunnel establishing in order to update IPv4 tunnel endpoint on the HE.NET server.

Awesome!

# Notes #

Please ensure that ICMP is traversed to you from HE.NET servers. It's requeired by HE.NET to update tunnel endpoint successfully. In the other case, the error will be raised and tunnel will not work.

You may check stdout/stderr logs via logcat if unsure what's happening.