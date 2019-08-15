package hageldave.jplotter.color;

import static hageldave.imagingkit.core.Pixel.*;

public class ColorOperations {

	public static int mixARGB(int argb1, int argb2){
		return argb(
				(a(argb1)+a(argb2))/2, 
				(r(argb1)+r(argb2))/2, 
				(g(argb1)+g(argb2))/2, 
				(b(argb1)+b(argb2))/2);
	}
	
	public static int desaturate(int argb){
		int luminance = getLuminance(argb);
		return argb(a(argb), luminance, luminance, luminance);
	}
	
}
