package hageldave.jplotter.color;

import java.awt.*;

/**
 * The ColorScheme is responsible for storing and providing color information,
 * which other components can use.
 * This enables an easy way to distribute color information to multiple components.
 * There is the option to use one of the predefined {@link DefaultColorScheme} or to define custom colors.
 * The color scheme contains five different color attributes,
 * which can be accessed by the respective components.
 *
 * @author lucareichmann
 */
public class ColorScheme {
	protected final int color1;
	protected final int color2;
	protected final int color3;
	protected final int color4;
	protected final int colorText;
	protected final int colorBackground;

	/**
	 * Constructor for defining a new color scheme.
	 *
	 * @param primaryColor primary color of the color scheme
	 * @param secondaryColor secondary color of the color scheme
	 * @param tertiaryColor tertiary color of the color scheme
	 * @param quaternaryColor quarternary color of the color scheme
	 * @param textColor text color of the color scheme
	 * @param backgroundColor the background color
	 */
	public ColorScheme (Color primaryColor, Color secondaryColor, Color tertiaryColor, Color quaternaryColor, Color textColor, Color backgroundColor) {
		this(primaryColor.getRGB(),secondaryColor.getRGB(),tertiaryColor.getRGB(),quaternaryColor.getRGB(),textColor.getRGB(), backgroundColor.getRGB());
	}
	
	/**
	 * Constructor for defining a new color scheme.
	 *
	 * @param primaryColor primary color of the color scheme (integer packed ARGB)
	 * @param secondaryColor secondary color of the color scheme (integer packed ARGB)
	 * @param tertiaryColor tertiary color of the color scheme (integer packed ARGB)
	 * @param quaternaryColor quarternary color of the color scheme (integer packed ARGB)
	 * @param textColor text color of the color scheme (integer packed ARGB)
	 * @param backgroundColor the background color (integer packed ARGB)
	 */
	public ColorScheme (int primaryColor, int secondaryColor, int tertiaryColor, int quaternaryColor, int textColor, int backgroundColor) {
		this.color1 = primaryColor;
		this.color2 = secondaryColor;
		this.color3 = tertiaryColor;
		this.color4 = quaternaryColor;
		this.colorText = textColor;
		this.colorBackground = backgroundColor;
	}

	/**
	 * @return primary color of the color scheme (integer packed ARGB)
	 */
	public int getColor1() {
		return this.color1;
	}

	/**
	 * @return secondary color of the color scheme (integer packed ARGB)
	 */
	public int getColor2() {
		return this.color2;
	}

	/**
	 * @return tertiary color of the color scheme (integer packed ARGB)
	 */
	public int getColor3() {
		return this.color3;
	}

	/**
	 * @return fourth color of the color scheme (integer packed ARGB)
	 */
	public int getColor4 () {
		return this.color4;
	}

	/**
	 * @return text color of the color scheme (integer packed ARGB)
	 */
	public int getColorText() {
		return this.colorText;
	}
	
	/**
	 * @return background color of the scheme (integer packed ARGB)
	 */
	public int getColorBackground() {
		return colorBackground;
	}
}
