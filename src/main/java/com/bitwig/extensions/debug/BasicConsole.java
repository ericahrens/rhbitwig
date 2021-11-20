package com.bitwig.extensions.debug;

public abstract class BasicConsole implements RemoteConsole {

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

	protected abstract void print(final String msg);

	protected abstract void println(final String msg);

}
