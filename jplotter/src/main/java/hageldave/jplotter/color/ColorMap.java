package hageldave.jplotter.color;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import hageldave.imagingkit.core.Img;
import hageldave.imagingkit.core.Pixel;
import hageldave.imagingkit.core.PixelBase;
import hageldave.imagingkit.core.operations.ColorSpaceTransformation;
import hageldave.jplotter.util.Utils;

/**
 * The ColorMap interface defines discrete mapping from a fixed integer 
 * interval [0..N-1] to colors through the {@link #getColor(int)} method.
 * It also defines a continuous mapping from the unit interval [0,1] to 
 * colors through the {@link #interpolate(double)} method.
 * For this each of the discrete colors is mapped to a location within
 * the unit interval [0,1] with the first color mapping to 0 and the last
 * mapping to 1.
 * The colors in between have arbitrary but ascending locations within ]0,1[
 * which can be accessed using {@link #getLocation(int)}.
 * 
 * @author hageldave
 */
public interface ColorMap {
	
	/**
	 * @return the number of discrete colors in this {@link ColorMap}
	 */
	public int numColors();
	
	/**
	 * Returns the ith color
	 * @param i index of the color
	 * @return ith color in integer packed ARGB format (0xff00ff00 is opaque green)
	 */
	public int getColor(int i);
	
	/**
	 * @return all discrete colors in this {@link ColorMap} 
	 * in integer packed ARGB format (0xff00ff00 is opaque green)
	 */
	public int[] getColors();
	
	/**
	 * Returns the location of the ith color within the unit interval.
	 * @param i index of the location/color
	 * @return location within [0,1]
	 */
	public double getLocation(int i);
	
	/**
	 * @return all locations of the discrete colors in this {@link ColorMap}
	 */
	public double[] getLocations();
	
	/**
	 * linearly interpolates colors for the specified location of the unit interval.
	 * @param m location within [0,1]
	 * @return the interpolated color for specified location
	 * in integer packed ARGB format (0xff00ff00 is opaque green)
	 */
	public default int interpolate(double m){
		m = Utils.clamp(0, m, 1);
		int idx = Arrays.binarySearch(getLocations(), (float)m);
		if(idx >= 0){
			return getColor(idx);
		} else {
			idx = -idx -1;
			int c0 = getColor(idx-1);
			int c1 = getColor(idx);
			double m0 = getLocation(idx-1);
			double m1 = getLocation(idx);
			return Utils.interpolateColor(c0, c1, (m-m0)/(m1-m0));
		}
	}
	
	/**
	 * @return copy of this color map
	 */
	public default SimpleColorMap copy(){
		int[] colors = getColors().clone();
		double[] locations = getLocations().clone();
		return new SimpleColorMap(colors, locations);
	}
	
	/**
	 * Returns a reversed version of this color map, where
	 * the order of colors and locations is reversed.
	 * @return reversed color map 
	 */
	public default SimpleColorMap reversed(){
		int[] colors = IntStream.range(0, numColors())
				.map(i->getColor(numColors()-1-i))
				.toArray();
		double[] locations = Arrays.stream(getLocations())
				.map(l -> -l+1)
				.sorted()
				.toArray();
		return new SimpleColorMap(colors, locations);
	}
	
	/**
	 * Returns a copy of this map where the specified color transformation
	 * (color grading) has been applied to each of the discrete colors.
	 * <p>
	 * A few {@link ColorGrader}s have been defined as static methods
	 * in the {@link ColorMap} interface.
	 * 
	 * @param colorGraders the color transformation
	 * @return color graded map
	 */
	public default SimpleColorMap colorGrade(ColorGrader... colorGraders){
		SimpleColorMap copy = copy();
		for(ColorGrader grader:colorGraders)
			grader.applyTo(copy.colors);
		return copy;
	}
	
	/**
	 * Returns a uniformly sampled version of this map with the specified
	 * number of samples within the specified interval.
	 * @param numSamples number of uniform samples
	 * @param start of sampling interval within [0,1[
	 * @param end of sampling interval within ]0,1]
	 * @return re-sampled map
	 */
	public default SimpleColorMap resample(int numSamples, double start, double end){
		start = Utils.clamp(0, start, 1);
		end = Utils.clamp(0, end, 1);
		double range = end-start;
		int[] colors = new int[numSamples];
		for(int i = 0; i < numSamples; i++){
			double m = (i*range)/(numSamples-1) + start;
			colors[i] = interpolate(m);
		}
		return new SimpleColorMap(colors);
	}
	
	/**
	 * Creates an {@link Img} from this {@link ColorMap} of specified dimensions.
	 * @param w width of the img
	 * @param h height of the img
	 * @param horizontal when true, the color map will shown left to right, 
	 * otherwise top to bottom.
	 * @param interpolate when true, the color map's interpolate method will be 
	 * used to obtain the colors, otherwise the discrete colors will be used
	 * @return image of the color map
	 */
	public default Img toImg(int w, int h, boolean horizontal, boolean interpolate){
		Img img = new Img(w, h);
		img.forEach(px->{
			double m = horizontal ? px.getXnormalized() : px.getYnormalized();
			if(interpolate){
				px.setValue(interpolate(m));
			} else {
				int numColors = numColors();
				int idx = Math.min((int)(m*numColors),numColors-1);
				px.setValue(getColor(idx));
			}
		});
		return img;
	}
	
	/**
	 * Creates an {@link BufferedImage} from this {@link ColorMap} of specified dimensions.
	 * @param w width of the image
	 * @param h height of the image
	 * @param horizontal when true, the color map will shown left to right, otherwise top to bottom.
	 * @return image of the color map
	 */
	public default BufferedImage toImage(int w, int h, boolean horizontal, boolean interpolate){
		return toImg(w, h, horizontal, interpolate).getRemoteBufferedImage();
	}
	
	// MAP GRADING UTILITY METHODS
	
	/**
	 * The ColorGrading interface defines a color transformation 
	 * on a sequence of colors.
	 * @author hageldave
	 */
	public static interface ColorGrader {
		/**
		 * applies the grading
		 * @param colors sequence of colors 
		 * in integer packed ARGB format (0xff00ff00 is opaque green)
		 */
		public void applyTo(int[] colors);
	}
	
	/**
	 * {@link ColorGrader} for shifting hue in HSV color space.
	 * @param degree the angle by which hue is shifted within [0,360]
	 * @return hue shifting grader
	 */
	public static ColorGrader hueShift(double degree){
		double shift = degree/360;
		return gradeAsPixels(
				ColorSpaceTransformation.RGB_2_HSV
				.andThen(px->px.setR_fromDouble((px.r_asDouble()+shift)%1.0))
				.andThen(ColorSpaceTransformation.HSV_2_RGB)
			);
	}
	
	/**
	 * {@link ColorGrader} that copies the hue and saturation of the
	 * specified color in HSV color space to the colorized sequence.
	 * The value (as in HSV) is preserved.
	 * @param color in integer packed ARGB format (0xff00ff00 is opaque green)
	 * @return colorizing grader
	 */
	public static ColorGrader colorize(int color){
		Pixel colorPx = new Img(1, 1, new int[]{color}).getPixel();
		ColorSpaceTransformation.RGB_2_HSV.accept(colorPx);
		return gradeAsPixels(ColorSpaceTransformation.RGB_2_HSV
				.andThen(px->px.setR_fromDouble(colorPx.r_asDouble()).setG_fromDouble(colorPx.g_asDouble()))
				.andThen(ColorSpaceTransformation.HSV_2_RGB));
	}
	
	/**
	 * {@link ColorGrader} that multiplies the saturation of a color
	 * by the specified factor. Desaturation will occur when factor is
	 * less than 1.
	 * @param m saturation factor in [0,infty[
	 * @return saturating grader
	 */
	public static ColorGrader saturate(double m){
		return gradeAsPixels(ColorSpaceTransformation.RGB_2_HSV
				.andThen(px->px.setG_fromDouble(px.g_asDouble()*m))
				.andThen(ColorSpaceTransformation.HSV_2_RGB));
	}
	
	/**
	 * {@link ColorGrader} that multiplies the value (as in HSV) of a color
	 * by the specified factor, thus raising or lowering the luminance.
	 * @param m illumination factor in [0,infty[
	 * @return illuminating grader
	 */
	public static ColorGrader illuminate(double m){
		return gradeAsPixels(ColorSpaceTransformation.RGB_2_HSV
				.andThen(px->px.setB_fromDouble(px.b_asDouble()*m))
				.andThen(ColorSpaceTransformation.HSV_2_RGB));
	}
	
	/**
	 * {@link ColorGrader} that adds the specified delta to the
	 * value (as in HSV) of a color, thus raising or lowering the luminance.
	 * @param delta value to add
	 * @return illumination raising grader
	 */
	public static ColorGrader raiseLuminance(double delta){
		return gradeAsPixels(ColorSpaceTransformation.RGB_2_HSV
				.andThen(px->px.setB_fromDouble(px.b_asDouble()+delta))
				.andThen(ColorSpaceTransformation.HSV_2_RGB));
	}
	
	/**
	 * {@link ColorGrader} that will invert colors in RGB color space
	 * @return inverting grader
	 */
	public static ColorGrader invert() {
		return gradeAsPixels(px->{
			px	.setR_fromDouble(1-px.r_asDouble())
				.setG_fromDouble(1-px.g_asDouble())
				.setB_fromDouble(1-px.b_asDouble());
		});
	}
	
	/**
	 * {@link ColorGrader} that spreads RGB values around a
	 * specified mid point by a specified factor which results
	 * in raising or lowering the contrast of a color sequence
	 * @param m spread factor
	 * @param mid point to spread RGB from in [0,1]
	 * @return contrast raising grader
	 */
	public static ColorGrader raiseContrast(double m, double mid){
		return gradeAsPixels(px->{
			double r = px.r_asDouble();
			double g = px.g_asDouble();
			double b = px.b_asDouble();
			r = r+(r-mid)*m;
			g = g+(g-mid)*m;
			b = b+(b-mid)*m;
			px.setRGB_fromDouble_preserveAlpha(r, g, b);
		});
	}
	
	
	public static ColorGrader gradeAsImg(Consumer<Img> consumer){
		return colors -> consumer.accept(new Img(colors.length, 1, colors));
	}
	
	public static ColorGrader gradeAsPixels(Consumer<PixelBase> consumer){
		return gradeAsImg(img->img.forEach(consumer));
	}
	
}
