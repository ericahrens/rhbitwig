package com.akai.fire.display;

import static com.akai.fire.AkaiFireDrumSeqExtension.DEVICE_ID;
import static com.akai.fire.AkaiFireDrumSeqExtension.MAN_ID_AKAI;
import static com.akai.fire.AkaiFireDrumSeqExtension.PRODUCT_ID;
import static com.akai.fire.AkaiFireDrumSeqExtension.SE_EN;
import static com.akai.fire.AkaiFireDrumSeqExtension.SE_OLED_RGB;
import static com.akai.fire.AkaiFireDrumSeqExtension.SE_ST;

import java.util.List;

import com.akai.fire.SysExUtil;
import com.bitwig.extension.controller.api.MidiOut;

public class OledDisplay {
	private static final int GENERAL_BAR_WIDTH = 126;
	private final byte[] oledBar = new byte[] { SE_ST, MAN_ID_AKAI, DEVICE_ID, PRODUCT_ID, 0x09, 0x00, 0x08, //
			0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, SE_EN };
	private final byte[] oledCmd = new byte[] { SE_ST, MAN_ID_AKAI, DEVICE_ID, PRODUCT_ID, SE_OLED_RGB, 00 };
	private final byte[] oledPack = new byte[] { SE_ST, MAN_ID_AKAI, DEVICE_ID, PRODUCT_ID, 0x0E };

	private final MidiOut midiOut;
	private boolean inGraphicsMode = false;
	private long clearTask = -1;

	public enum Fill {
		Empty, Solid, Fifty, Hatch;
	}

	public enum TextJustification {
		LEFT((byte) 0), CENTER((byte) 1), RIGHT((byte) 2);

		private byte code;

		private TextJustification(final byte code) {
			this.code = code;
		}

		public byte getCode() {
			return code;
		}
	}

	public OledDisplay(final MidiOut midiOut) {
		super();
		this.midiOut = midiOut;
	}

	public void showLogo() {
		sendImage(SysExUtil.PLASTIKMAN_LOGO);
	}

	public void clearScreenDelayed() {
		clearTask = System.currentTimeMillis();
	}

	public void clearScreen() {
		sendImage(SysExUtil.EMPTY_SCREEN);
		inGraphicsMode = false;
	}

	public void sendImage(final int[] imageData) {
		sendImageData(imageData);
		inGraphicsMode = true;
	}

	public void sendImageData(final int[] imageData) {
		final byte[] bytelist = SysExUtil.toBytePack(imageData);
		final int datalen = bytelist.length + 4;
		final byte[] sysex = new byte[oledPack.length + bytelist.length + 7];

		System.arraycopy(oledPack, 0, sysex, 0, oledPack.length);
		System.arraycopy(bytelist, 0, sysex, 11, bytelist.length);
		sysex[5] = (byte) (datalen >> 7 & 0x7F);
		sysex[6] = (byte) (datalen & 0x7F);
		sysex[7] = (byte) 0;
		sysex[8] = (byte) 7;
		sysex[9] = (byte) 0;
		sysex[10] = (byte) 127;
		sysex[sysex.length - 1] = SE_EN;
		midiOut.sendSysex(sysex);
	}

	public void showBar(final boolean outline, final int width, final int height, final Fill foreground,
			final Fill background, final int offset, final int start, final int end) {
		oledBar[7] = (byte) width;
		oledBar[8] = (byte) height;
		oledBar[9] = (byte) (outline ? 1 : 0);
		oledBar[10] = (byte) foreground.ordinal();
		oledBar[11] = (byte) background.ordinal();
		oledBar[12] = (byte) offset;
		oledBar[13] = (byte) start;
		oledBar[14] = (byte) end;
		midiOut.sendSysex(oledBar);
	}

	public void detailInfo(final String title, final String lines) {
		final String[] line = lines.split("\\n");
		sendString(1, TextJustification.CENTER, 0, title);
		for (int i = 0; i < 7; i++) {
			final String l = i < line.length ? line[i] : "";
			sendString(0, TextJustification.LEFT, 2 + i * 1, l);
		}
	}

	public void lineInfo(final String title, final String lines) {
		final String[] line = lines.split("\\n");
		sendString(2, TextJustification.CENTER, 0, title);
		for (int i = 0; i < 3; i++) {
			final String l = i < line.length ? line[i] : "";
			sendString(1, TextJustification.LEFT, 2 + i * 2, l);
		}
	}

	public void functionInfo(final String details, final String functionName, final String lines) {
		sendString(0, TextJustification.CENTER, 0, details);
		sendString(2, TextJustification.CENTER, 1, functionName);
		sendString(0, TextJustification.LEFT, 3, "");
		final String[] line = lines.split("\\n");
		for (int i = 0; i < 4; i++) {
			final String l = i < line.length ? line[i] : "";
			sendString(0, TextJustification.LEFT, 4 + i * 1, l);
		}
	}

	public void showInfo(final DisplayInfo info, final Object... values) {
		final List<Line> lines = info.getLines();
		for (final Line line : lines) {
			showLine(line);
		}
	}

	public void showLine(final Line line) {
		sendString(line.getSize(), line.getJustification(), line.getOffset(), line.getViewText());
	}

	public void paramInfo(final String paramName, final String details) {
		sendString(0, TextJustification.CENTER, 0, details);
		sendString(2, TextJustification.CENTER, 1, paramName);
		sendString(2, TextJustification.CENTER, 3, "");
		sendString(3, TextJustification.CENTER, 5, "");
	}

	public void paramInfo(final String paramName, final String value, final String details) {
		sendString(0, TextJustification.CENTER, 0, details);
		sendString(2, TextJustification.CENTER, 1, paramName);
		sendString(0, TextJustification.CENTER, 3, "");
		sendString(2, TextJustification.CENTER, 4, value);
		sendString(0, TextJustification.CENTER, 6, "");
	}

	public void paramInfo(final String paramName, final int value, final String details, final int min, final int max) {
		paramInfo(paramName, value, details, min, max, null);
	}

	public void paramInfo(final String paramName, final int value, final String details, final int min, final int max,
			final Integer offValue) {
		sendString(0, TextJustification.CENTER, 0, details);
		sendString(2, TextJustification.CENTER, 1, paramName);
		if (offValue != null && value == offValue.intValue()) {
			sendString(2, TextJustification.CENTER, 3, "Off");
		} else {
			sendString(2, TextJustification.CENTER, 3, Integer.toString(value));
		}
		sendString(0, TextJustification.CENTER, 5, "");

		final int range = max - min;
		final double unit = (double) GENERAL_BAR_WIDTH / (double) range;
		final int bar = (int) Math.round(min + unit * (value - min));
		showBar(true, GENERAL_BAR_WIDTH, 1, Fill.Fifty, Fill.Empty, 6, 0, bar);
	}

	public void valueInfo(final String title, final String value) {
		sendString(2, TextJustification.CENTER, 0, title);
		sendString(3, TextJustification.CENTER, 2, value);
		sendString(5, TextJustification.CENTER, 5, "");
	}

	public void paramInfoDouble(final String paramName, final double value, final String details, final double min,
			final double max) {
		sendString(0, TextJustification.CENTER, 0, details);
		sendString(2, TextJustification.CENTER, 1, paramName);
		sendString(2, TextJustification.CENTER, 3, Integer.toString((int) value));
		sendString(0, TextJustification.CENTER, 5, "");
		barValue(value, min, max);
	}

	public void paramInfoPercent(final String paramName, final double value, final String details, final double min,
			final double max) {
		sendString(0, TextJustification.CENTER, 0, details);
		sendString(2, TextJustification.CENTER, 1, paramName);
		sendString(2, TextJustification.CENTER, 3, toPercent(value));
		sendString(0, TextJustification.CENTER, 5, "");
		barValue(value, min, max);
	}

	public void paramInfoDuration(final String paramName, final double duration, final String details,
			final double gridRes) {
		final double stepLen = duration / gridRes;
		sendString(0, TextJustification.CENTER, 0, details);
		sendString(2, TextJustification.CENTER, 1, paramName);
		sendString(2, TextJustification.CENTER, 3, String.format("%.2f", stepLen));
		sendString(0, TextJustification.CENTER, 5, "");
		sendString(3, TextJustification.CENTER, 6, "");
	}

	private void barValue(final double value, final double min, final double max) {
		final double range = max - min;
		final double unit = GENERAL_BAR_WIDTH / range;
		int start = 0;
		int end = 0;
		if (min < 0) {
			if (value < 0) {
				end = GENERAL_BAR_WIDTH / 2;
				start = end + (int) Math.round(unit * value);
			} else {
				start = GENERAL_BAR_WIDTH / 2;
				end = start + (int) Math.round(unit * value);
			}
		} else {
			end = (int) Math.round(unit * value);
		}
		showBar(true, GENERAL_BAR_WIDTH, 1, Fill.Fifty, Fill.Empty, 6, start, end);
	}

	private String toPercent(final double chance) {
		final int val = (int) Math.round(chance * 100);
		return Integer.toString(val) + "%";
	}

	public void sendString(final int fontSize, final TextJustification justification, final int placement,
			final String text) {
		if (inGraphicsMode) {
			clearScreen();
		}
		if (placement > 7 || fontSize > 3) {
			return;
		}
		final String fitText = text.length() > 20 ? text.substring(0, 20) : text;
		final byte[] sysex = new byte[fitText.length() + oledCmd.length + 5];
		System.arraycopy(oledCmd, 0, sysex, 0, oledCmd.length);
		sysex[sysex.length - 1] = SE_EN;
		sysex[6] = (byte) (fitText.length() + 3);
		sysex[7] = (byte) fontSize;
		sysex[8] = justification.getCode();
		sysex[9] = (byte) placement;
		for (int i = 0; i < fitText.length(); i++) {
			sysex[10 + i] = (byte) fitText.charAt(i);
		}
		midiOut.sendSysex(sysex);
		clearTask = -1;
	}

	public void notifyBlink(final int blinkTicks) {
		if (clearTask > 0 && System.currentTimeMillis() - clearTask > 1500) {
			clearScreen();
			clearTask = -1;
		}
	}

}
