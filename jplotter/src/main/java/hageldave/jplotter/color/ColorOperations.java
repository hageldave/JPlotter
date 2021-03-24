package hageldave.jplotter.color;

import static hageldave.imagingkit.core.Pixel.*;

import hageldave.imagingkit.core.Pixel;

/**
 * The ColorOperations class contains methods for manipulating
 * 32bit ARGB color values.
 * 
 * @author hageldave
 */
public class ColorOperations {

	/**
	 * Mixes the specified colors equally using mean channel values.
	 * @param argb1 integer packed ARGB color value 
	 * @param argb2 integer packed ARGB color value, e.g. 0xff00ff00 for opaque green
	 * @return the mixed color
	 */
	public static int mixARGB(int argb1, int argb2){
		return argb(
				(a(argb1)+a(argb2))/2, 
				(r(argb1)+r(argb2))/2, 
				(g(argb1)+g(argb2))/2, 
				(b(argb1)+b(argb2))/2);
	}
	
	/**
	 * Desaturates the specified color, i.e. calcualtes
	 * the color's luminance and returns a grey color for it.
	 * @param argb integer packed ARGB color value, e.g. 0xff00ff00 for opaque green
	 * @return desaturated color
	 */
	public static int desaturate(int argb){
		int luminance = getLuminance(argb);
		return argb(a(argb), luminance, luminance, luminance);
	}
	
	/**
	 * Linearly interpolates between the two specified colors.<br>
	 * c = c1*(1-m) + c2*m
	 * @param c1 integer packed ARGB color value 
	 * @param c2 integer packed ARGB color value, e.g. 0xff00ff00 for opaque green
	 * @param m in [0,1]
	 * @return interpolated color
	 */
	public static int interpolateColor(int c1, int c2, double m){
		double r1 = Pixel.r_normalized(c1);
		double g1 = Pixel.g_normalized(c1);
		double b1 = Pixel.b_normalized(c1);
		double a1 = Pixel.a_normalized(c1);
		
		double r2 = Pixel.r_normalized(c2);
		double g2 = Pixel.g_normalized(c2);
		double b2 = Pixel.b_normalized(c2);
		double a2 = Pixel.a_normalized(c2);
		
		return Pixel.argb_fromNormalized(
				a1*(1-m)+a2*m,
				r1*(1-m)+r2*m,
				g1*(1-m)+g2*m,
				b1*(1-m)+b2*m
		);
	}

	/**
	 * Scales the alpha value of a specified integer packed ARGB color by a
	 * specified scaling factor {@code m}. 
	 * New color will be {@code (a*m, r, g, b)}.
	 * @param color of which alpha will be scaled
	 * @param m scaling factor
	 * @return integer packed ARGB color with scaled alpha
	 */
	public static int scaleColorAlpha(int color, double m) {
		double af = Pixel.a_normalized(color)*m;
		int a = Pixel.argb_fromNormalized(af, 0,0,0);
		return (color&0x00ffffff)|a;
	}
	
}
