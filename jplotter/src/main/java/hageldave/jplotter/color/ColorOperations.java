package hageldave.jplotter.color;

import static hageldave.imagingkit.core.Pixel.*;

/**
 * The ColorOperations class contains methods for manipulating
 * 32bit ARGB color values.
 * 
 * @author hageldave
 */
public class ColorOperations {
	
	/**
	 * Changes the saturation of a color.
	 * @param argb int packed argb color value
	 * @param s saturation multilpier (1 is no change, 0 is greyscale)
	 * @return saturation changed color
	 */
	public static int changeSaturation(int argb, double s) {
		double tounit = 1/255.0;
		double r=r(argb)*tounit, g=g(argb)*tounit, b=b(argb)*tounit;
		double l = r*0.2126 + g*0.7152 + b*0.0722; // luminance
		double dr=r-l, dg=g-l, db=b-l;
		if(s > 1.0) {
			// find maximal saturation that will keep channel values in range [0,1]
			//s*dr+l=1 -> s*dr=1-l -> (1-l)/dr=s
			//s*dr+l=0 -> s*dr=-l  -> (0-l)/dr=s
			s = Math.min(s, dr<0 ? -l/dr:(1-l)/dr);
			s = Math.min(s, dg<0 ? -l/dg:(1-l)/dg);
			s = Math.min(s, db<0 ? -l/db:(1-l)/db);
		}
		r=l+s*dr; g=l+s*dg; b=l+s*db;
		return (0xff000000 & argb) | (0x00ffffff & rgb_fromNormalized(r, g, b)); // bit operations are for alpha preservation
	}

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
		double r1 = r_normalized(c1);
		double g1 = g_normalized(c1);
		double b1 = b_normalized(c1);
		double a1 = a_normalized(c1);
		
		double r2 = r_normalized(c2);
		double g2 = g_normalized(c2);
		double b2 = b_normalized(c2);
		double a2 = a_normalized(c2);
		
		return argb_fromNormalized(
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
		double af = a_normalized(color)*m;
		int a = argb_fromNormalized(af, 0,0,0);
		return (color&0x00ffffff)|a;
	}
	
}
