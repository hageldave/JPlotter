package hageldave.jplotter.color;

import java.awt.*;

/**
 * The SchemePresets contains predefined {@link ColorScheme}s (Light, Dark),
 * which can be accessed through {@link #get()}.
 *
 * @author lucareichmann
 */
public enum ColorSchemePreset {
	LIGHT(
		new ColorScheme(
			Color.BLACK,
			Color.GRAY,
			Color.DARK_GRAY,
			new Color(0xdddddd),
			new Color(96, 96, 96)
		)
	),
	DARK(
		new ColorScheme(
			Color.WHITE,
			Color.LIGHT_GRAY,
			Color.LIGHT_GRAY,
			Color.DARK_GRAY,
			new Color(196, 196, 196)
		)
	),
	;

	private final ColorScheme scheme;

	private ColorSchemePreset (ColorScheme scheme) {
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
