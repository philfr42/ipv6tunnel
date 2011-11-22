package com.vintozver.ipv6tunnel;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.List;
import java.util.Arrays;
import java.util.Vector;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HttpContext;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.widget.Toast;
import android.app.IntentService;

public class NetworkService extends IntentService {
	public final static String BOOT = String.format("%s.BOOT", NetworkService.class.getName()); 
	public final static String NETWORK_STATUS = String.format("%s.GLOBAL_ACTIVATION", NetworkService.class.getName()); 
	public final static String GLOBAL_ACTIVATION = String.format("%s.GLOBAL_ACTIVATION", NetworkService.class.getName()); 
	public final static String TUNNEL_CHANGE = String.format("%s.TUNNEL_CHANGE", NetworkService.class.getName()); 

	Handler thread_handler = null;

	public NetworkService() {
		super(NetworkService.class.getName());

		thread_handler = new Handler();
	}

	@Override
	protected void onHandleIntent(Intent arg) {
		System.out.printf("Network service intent handling: %s\n", arg.toString());
		try {
			updateBusyboxBinary();
		} catch (IOException e) {
			thread_handler.post(new RunnableBase<NetworkService>(this, e) {
				@Override
				public void run() {
					Toast.makeText(getOwner(), getOwner().getString(R.string.service_error_busybox, ((IOException)getParams()[0]).toString()), Toast.LENGTH_LONG).show();
				}
			});
			return;
		}
		final String action = arg.getAction();
		if (action.equals(NetworkService.BOOT) || action.equals(NetworkService.NETWORK_STATUS) || action.equals(NetworkService.GLOBAL_ACTIVATION)) {
			tunnelsUpdate();
			thread_handler.post(new RunnableBase<NetworkService>(this) {
				@Override
				public void run() {
					Toast.makeText(getOwner(), getOwner().getString(R.string.service_success), Toast.LENGTH_LONG).show();
				}
			});
			return;
		}
		if (action.equals(NetworkService.GLOBAL_ACTIVATION)) {
			tunnelsUpdate();
			return;
		}
		if (action == NetworkService.TUNNEL_CHANGE) {
			final String subaction = arg.getStringExtra("action");
			final String tunnel_id = arg.getStringExtra("tunnel_id");
			if (subaction.equals("add")) {
				tunnelAdd(tunnel_id);
				return;
			}
			if (subaction.equals("update")) {
				tunnelUpdate(tunnel_id);
				return;
			}
			if (subaction.equals("remove")) {
				tunnelRemove(tunnel_id);
				return;
			}
		}
	}

	public void updateBusyboxBinary() throws IOException {
		Context context = this;

		String busybox_filename = context.getString(R.string.file_busybox);
		File root_dir = context.getFilesDir();
		File busybox_file = new File(root_dir, busybox_filename);
		if (!busybox_file.canRead()) {
			InputStream busybox_file_istream = context.getAssets().open(busybox_filename);
			FileOutputStream busybox_file_ostream = new FileOutputStream(busybox_file);

			final int BUFFER_SIZE = 1024;
			byte[] buffer = new byte[BUFFER_SIZE];
			BufferedInputStream in = new BufferedInputStream(busybox_file_istream, BUFFER_SIZE);
			BufferedOutputStream out = new BufferedOutputStream(busybox_file_ostream, BUFFER_SIZE);
			try {
				int n = 0;
				while ((n = in.read(buffer, 0, BUFFER_SIZE)) != -1) {
					out.write(buffer, 0, n);
				}
				out.flush();
			} catch (IOException e) {
				throw new IOException("Error updating busybox internal binary");
			} finally {
				boolean throw_flag = false;
				try {
					out.close();
				} catch (IOException e) {
					throw_flag = true;
				}
				try {
					in.close();
				} catch (IOException e) {
					throw_flag = true;
				}
				if (throw_flag) {
					throw new IOException("Error closing descriptors");
				}
			}

			try {
				String[] p_params = new String[] { "chmod", "755", busybox_file.getAbsolutePath() };
				Process p = Runtime.getRuntime().exec(p_params);   
				try {   
					p.waitFor();
					System.out.printf("Updating internal busybox binary, chmod value: %d\n", p.exitValue());
				} catch (InterruptedException e) {
					throw new IOException();
				}   
			} catch (IOException e) {   
				throw new IOException("Error changing internal busybox binary flags");
			}  		

			System.out.println("Updated internal busybox binary");
		}
	}

	public String getExtIp() throws IOException {
		Context context = this;

		try {
			DefaultHttpClient http_client = new DefaultHttpClient();
			HttpRequestBase http_req = new HttpGet("http://checkip.dyndns.org/");
			http_req.setHeader("User-Agent", context.getString(R.string.config_http_useragent));
			HttpResponse http_data = http_client.execute(http_req, (HttpContext)null);
			HttpEntity http_entity = http_data.getEntity();
			if (http_entity != null) {
				InputStream http_stream = http_entity.getContent();
				byte[] http_content = new byte[1024]; 
				http_stream.read(http_content);
				String http_string = new String(http_content);
				java.util.regex.Pattern re_pattern = java.util.regex.Pattern.compile("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}");
				java.util.regex.Matcher re_matcher = re_pattern.matcher(http_string);
				if (re_matcher.find()) {
					String self_ip = re_matcher.group(0);
					System.out.printf("Self global IPv4 address: %s\n", self_ip);
					return self_ip;
				}
				throw new IOException(String.format("Invalid response for IP address reqest, %s", http_string));
			} else {
				throw new IOException("IP address request response is empty");
			}
		} catch (IOException e) {
			System.err.println("An error occured while retrieving self IPv4 address");
			throw e;
		} catch (IllegalStateException e) {
			System.err.println("An error occured while retrieving self IPv4 address");
			throw new IOException(String.format("Invalid state, %s", e.toString()));
		}
	}

	public String getSelfIp() {
		List<NetworkInterface> networkInterfaces = new Vector<NetworkInterface>();
		try {
			Enumeration<NetworkInterface> networkInterfacesEnum = NetworkInterface.getNetworkInterfaces();
			while (networkInterfacesEnum.hasMoreElements()) {
				NetworkInterface networkInterface = networkInterfacesEnum.nextElement();
				if (networkInterface.getDisplayName().equals("lo")) { /* This is API version 8 hack. :( See Android documentation for further questions. */
					continue;
				}
				networkInterfaces.add(networkInterface);
				Enumeration<InetAddress> networkAddressesEnum = networkInterface.getInetAddresses();
				while (networkAddressesEnum.hasMoreElements()) {
					return networkAddressesEnum.nextElement().getHostAddress();
				}
			}
		} catch (SocketException e) {
			System.err.println("Cannot retrieve self IP address");
			return null;
		}
		return null;
	}

	public boolean isOwnIp(String ip) {
		List<NetworkInterface> networkInterfaces = new Vector<NetworkInterface>();
		List<InetAddress> networkAddresses = new Vector<InetAddress>();
		try {
			Enumeration<NetworkInterface> networkInterfacesEnum = NetworkInterface.getNetworkInterfaces();
			while (networkInterfacesEnum.hasMoreElements()) {
				NetworkInterface networkInterface = networkInterfacesEnum.nextElement();
				networkInterfaces.add(networkInterface);
				Enumeration<InetAddress> networkAddressesEnum = networkInterface.getInetAddresses();
				while (networkAddressesEnum.hasMoreElements()) {
					networkAddresses.add(networkAddressesEnum.nextElement());
				}
			}
		} catch (SocketException e) {
			System.err.println("Cannot retrieve network interface list");
			return false;
		}
		for (InetAddress networkAddress : networkAddresses) {
			if (new String(networkAddress.getAddress()).equalsIgnoreCase(ip)) {
				return true;
			}
		}
		return false;
	}

	private void exec(String command, String ... params) throws IOException {
		try {
			List<String> args = new java.util.Vector<String>();
			args.add(command);
			args.addAll(Arrays.asList(params));
			String[] args_array = new String[args.size()];
			Process p = Runtime.getRuntime().exec(args.toArray(args_array));
			try {   
				p.waitFor();   
				if (p.exitValue() == 0) {   
					System.out.printf("Command completed: %s\n", command);
				} else {   
					System.out.printf("Command: %s, error code: %d\n", command, p.exitValue());
				}   
			} catch (InterruptedException e) {   
				System.err.printf("Command execution interrupted: %s\n", command);
				throw new IOException();
			}   
		} catch (IOException e) {   
			throw new IOException(String.format("Command execution failure: %s", command));
		}  		
	}

	private void execAsRoot(String command, String ... params) throws IOException {
		try {
			List<String> args = new java.util.Vector<String>();
			args.add("su");
			args.add("-c");
			args.add(command);
			args.addAll(Arrays.asList(params));
			String[] args_array = new String[args.size()];
			Process p = Runtime.getRuntime().exec((String[])(args.toArray(args_array)));   
			try {   
				p.waitFor();   
				if (p.exitValue() == 0) {   
					System.out.printf("Command completed: %s\n", command);
				} else {   
					System.out.printf("Command: %s, error code: %d\n", command, p.exitValue());
				}   
			} catch (InterruptedException e) {   
				System.err.printf("Command execution interrupted: %s\n", command);
				throw new IOException();
			}   
		} catch (IOException e) {   
			throw new IOException(String.format("Command execution failure: %s", command));
		}  		
	}

	private String tunnelIdentifierToName(String tunnel_id) {
		return String.format("sit%s", Long.toString(java.util.UUID.fromString(tunnel_id).timestamp() / 10000000, 16));
	}

	private void tunnelsUpdate() {
		Context context = this;

		SharedPreferences config = context.getSharedPreferences(getPackageName(), MODE_WORLD_READABLE);
		boolean config_active = config.getBoolean("global_active", false);
		String tunnels = config.getString("tunnels", "");
		List<String> tunnels_list = Arrays.asList(tunnels.split("\n"));
		for (String tunnel_id : tunnels_list) {
			if (tunnel_id.length() == 0) continue;
			tunnelRemove(tunnel_id);
			if (config_active) {
				tunnelAdd(tunnel_id);
			}
		}
	}

	private void tunnelAdd(String tunnel_id) {
		Context context = this;

		File root_dir = context.getFilesDir();
		String busybox_filename = context.getString(R.string.file_busybox);
		File busybox_file = new File(root_dir, busybox_filename);

		SharedPreferences config = context.getSharedPreferences(getPackageName(), MODE_WORLD_READABLE);
		final boolean config_active = config.getBoolean(String.format("tunnels.%s.active", tunnel_id), false);
		if (!config_active) return;
		String interface_name = tunnelIdentifierToName(tunnel_id);
		String remote_server = config.getString(String.format("tunnels.%s.remote_server", tunnel_id), "0.0.0.0");
		String local_client = config.getString(String.format("tunnels.%s.local_client", tunnel_id), "0.0.0.0");
		final boolean local_autodetect = config.getBoolean(String.format("tunnels.%s.local_autodetect", tunnel_id), false);
		if (local_autodetect) {
			String self_ip = getSelfIp();
			if (self_ip != null) {
				local_client = self_ip;
			}
		}
		String tunnel_address = config.getString(String.format("tunnels.%s.tunnel_address", tunnel_id), "::0/0");
		try {
			File tmpfile = new File(root_dir, "tunnel-cmd");
			tmpfile.delete();
			tmpfile.createNewFile();
			FileOutputStream tmpfile_ostream = new FileOutputStream(tmpfile);
			tmpfile_ostream.write(("#!/system/bin/sh\n"
					+ String.format("%s ip tunnel add %s mode sit remote %s local %s\n", busybox_file.getAbsolutePath(), interface_name, remote_server, local_client)
					+ String.format("%s ip link set %s up\n", busybox_file.getAbsolutePath(), interface_name)
					+ String.format("%s ip addr add %s dev %s\n", busybox_file.getAbsolutePath(), tunnel_address, interface_name)
					+ String.format("%s ip route add ::/0 dev %s\n", busybox_file.getAbsolutePath(), interface_name)
					).getBytes());
			tmpfile_ostream.flush();
			tmpfile_ostream.close();
			exec("chmod", "0755", tmpfile.getAbsolutePath());
			execAsRoot(tmpfile.getAbsolutePath());
		} catch (IOException e) {
		}

		/* Update the tunnel */
		final boolean config_updating = config.getBoolean(String.format("tunnels.%s.updating_active", tunnel_id), false);
		if (config_updating) {
			final String config_updating_username = config.getString(String.format("tunnels.%s.updating_username", tunnel_id), "");
			final String config_updating_password = config.getString(String.format("tunnels.%s.updating_password", tunnel_id), "");
			final String config_updating_endpoint = config.getString(String.format("tunnels.%s.updating_endpoint", tunnel_id), "");
			try {
				SchemeRegistry schemeRegistry = new SchemeRegistry();
				schemeRegistry.register(new Scheme("http", org.apache.http.conn.scheme.PlainSocketFactory.getSocketFactory(), 80));
				schemeRegistry.register(new Scheme("https", new com.vintozver.ipv6tunnel.helpers.EasySSLSocketFactory(), 443));

				HttpParams params = new BasicHttpParams();
				params.setParameter(org.apache.http.conn.params.ConnManagerPNames.MAX_TOTAL_CONNECTIONS, 30);
				params.setParameter(org.apache.http.conn.params.ConnManagerPNames.MAX_CONNECTIONS_PER_ROUTE, new org.apache.http.conn.params.ConnPerRouteBean(30));
				params.setParameter(HttpProtocolParams.USE_EXPECT_CONTINUE, false);
				HttpProtocolParams.setVersion(params, org.apache.http.HttpVersion.HTTP_1_1);

				ClientConnectionManager cm = new org.apache.http.impl.conn.SingleClientConnManager(params, schemeRegistry);
				DefaultHttpClient http_client = new DefaultHttpClient(cm, params);
				HttpRequestBase http_req = new HttpGet(config_updating_endpoint);
				http_req.setHeader("Host", "ipv4.tunnelbroker.net");
				http_req.setHeader("User-Agent", context.getString(R.string.config_http_useragent));
				http_req.setHeader("Authorization", "Basic " + android.util.Base64.encodeToString((config_updating_username + ":" + config_updating_password).getBytes(), android.util.Base64.DEFAULT));
				HttpResponse http_resp = http_client.execute(http_req, (HttpContext)null);
				HttpEntity http_entity = http_resp.getEntity();
				if (http_entity != null) {
					InputStream http_stream = http_entity.getContent();
					byte[] http_content = new byte[1024]; 
					http_stream.read(http_content);
					System.out.printf("Tunnel update data: %s\n", new String(http_content));
				}
				System.out.printf("Tunnel %s updated\n", tunnel_id);
			} catch (IOException e) {
				System.err.println("An error occured while updating tunnel");
			} catch (IllegalStateException e) {
				System.err.println("An error occured while updating tunnel");
			}
		}
	}

	private void tunnelUpdate(String tunnel_id) {
		tunnelRemove(tunnel_id);
		tunnelAdd(tunnel_id);
	}

	private void tunnelRemove(String tunnel_id) {
		Context context = this;

		File root_dir = context.getFilesDir();
		String busybox_filename = context.getString(R.string.file_busybox);
		File busybox_file = new File(root_dir, busybox_filename);

		String interface_name = tunnelIdentifierToName(tunnel_id);

		try {
			File tmpfile = new File(root_dir, "tunnel-cmd");
			tmpfile.delete();
			tmpfile.createNewFile();
			FileOutputStream tmpfile_ostream = new FileOutputStream(tmpfile);
			tmpfile_ostream.write(("#!/system/bin/sh\n"
					+ String.format("%s ip tunnel del %s\n", busybox_file.getAbsolutePath(), interface_name)
					).getBytes());
			tmpfile_ostream.flush();
			tmpfile_ostream.close();
			exec("chmod", "0755", tmpfile.getAbsolutePath());
			execAsRoot(tmpfile.getAbsolutePath());
		} catch (IOException e) {
		}
	}
}
