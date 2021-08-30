package hageldave.jplotter.color;

import java.awt.*;

/**
 * Enum containing predefined {@link ColorScheme}s,
 * which can be accessed through {@link #get()}.
 *
 * @author lucareichmann
 */
public enum DefaultColorScheme {
	LIGHT(
		new ColorScheme(
			Color.BLACK,
			Color.GRAY,
			Color.DARK_GRAY,
			new Color(0xdddddd),
			new Color(96, 96, 96),
			Color.WHITE
		)
	),
	DARK(
		new ColorScheme(
			0xffdddddd,
			0xffaaaaaa,
			0xff666666,
			0xff444444,
			0xffbbbbbb,
			0xff21232b
		)
	),
	;

	private final ColorScheme scheme;

	private DefaultColorScheme (ColorScheme scheme) {
		this.scheme = scheme;
	}

	/**
	 * Returns the preset's {@link ColorScheme} object
	 * @return color scheme
	 */
	public ColorScheme get() {
		return this.scheme;
	}
}
