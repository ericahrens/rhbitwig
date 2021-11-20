package com.bitwig.extensions.debug;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import com.bitwig.extension.controller.api.ControllerHost;

public class RemoteConsoleActive extends BasicConsole {
	private DatagramSocket socket;
	private InetAddress address;
	private byte[] buf;
	private ControllerHost host;

	public RemoteConsoleActive() {
		try {
			socket = new DatagramSocket();
			address = InetAddress.getByName("localhost");
		} catch (final SocketException | UnknownHostException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void registerHost(final ControllerHost host) {
		this.host = host;
	}

	@Override
	public String getStackTrace(final int max) {
		final StringBuilder sb = new StringBuilder();
		final StackTraceElement[] st = Thread.currentThread().getStackTrace();
		int count = 0;
		sb.append("\n Stack Trace \n");
		for (final StackTraceElement stackTraceElement : st) {
			if (count > 1 && (max == -1 || count < max + 2)) {
				sb.append("   ").append(stackTraceElement.toString()).append("\n");
			}
			count++;
		}
		sb.append("\n");
		return sb.toString();
	}

	@Override
	public void printSysEx(final String prefix, final byte[] data) {
		final StringBuilder b = new StringBuilder(prefix + " ");
		for (int i = 0; i < data.length; i++) {
			b.append(pad(Integer.toHexString(data[i])));
			if (i < data.length - 1) {
				b.append(" ");
			}
		}
		println(b.toString());
	}

	@Override
	public void println(final String format, final Object... params) {
		final String[] split = format.split("\\{\\}");
		if (split.length > 0) {
			final StringBuilder sb = new StringBuilder(split[0]);
			int pc = 0;
			for (int i = 1; i < split.length; i++) {
				if (pc < params.length) {
					final Object v = params[pc++];
					if (v == null) {
						sb.append("<NULL>");
					} else {
						sb.append(v.toString());
					}
				} else {
					sb.append(" -- ");
				}
				sb.append(split[i]);
			}
			if (pc < params.length) {
				final Object v = params[pc++];
				if (v == null) {
					sb.append("<NULL>");
				} else {
					sb.append(v.toString());
				}
			}
			println(sb.toString());
		}
	}

	private static String pad(final String v) {
		if (v.length() == 2) {
			return v;
		}
		if (v.length() == 1) {
			return "0" + v;
		}
		if (v.length() == 0) {
			return "--";
		}
		return v.substring(0, 2);
	}

	@Override
	protected void print(final String msg) {
		if (host != null) {
			host.println(msg);
		} else {
			sendToRemote(msg);
		}
	}

	private void sendToRemote(final String msg) {
		final ByteBuffer bb = ByteBuffer.allocate(msg.length() + 10);
		bb.putInt(msg.length());
		bb.put(msg.getBytes());
		buf = bb.array();
		final DatagramPacket packet = new DatagramPacket(buf, buf.length, address, 6001);
		try {
			socket.send(packet);
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void println(final String msg) {
		if (host != null) {
			host.println(msg);
		} else {
			print(msg + "\n");
		}
	}

}
