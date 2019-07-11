package hageldave.jplotter.color;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import hageldave.imagingkit.core.Img;
import hageldave.imagingkit.core.PixelBase;
import hageldave.imagingkit.core.operations.ColorSpaceTransformation;
import hageldave.imagingkit.core.util.ImageFrame;
import hageldave.jplotter.util.Utils;

public interface ColorMap {
	
	public int numColors();
	
	public int getColor(int index);
	
	public double getLocation(int index);
	
	public double[] getLocations();
	
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
	
	public default SimpleColorMap copy(){
		int[] colors = IntStream.range(0, numColors())
				.map(this::getColor)
				.toArray();
		double[] locations = getLocations().clone();
		return new SimpleColorMap(colors, locations);
	}
	
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
	
	public default SimpleColorMap colorGrade(ColorGrader... colorGraders){
		SimpleColorMap copy = copy();
		for(ColorGrader grader:colorGraders)
			grader.applyTo(copy.colors);
		return copy;
	}
	
	// MAP GRADING UTILITY METHODS
	
	public static interface ColorGrader {
		public void applyTo(int[] colors);
	}
	
	public static ColorGrader hueShift(double degree){
		double shift = degree/360;
		return gradeAsPixels(
				ColorSpaceTransformation.RGB_2_HSV
				.andThen(px->px.setR_fromDouble((px.r_asDouble()+shift)%1.0))
				.andThen(ColorSpaceTransformation.HSV_2_RGB)
			);
	}
	
	public static ColorGrader colorize(int color){
		return gradeAsImg(img->img.forEach(px->px.setValue(px.getValue() & color)));
	}
	
	public static ColorGrader saturate(double m){
		return gradeAsPixels(ColorSpaceTransformation.RGB_2_HSV
				.andThen(px->px.setG_fromDouble(px.g_asDouble()*m))
				.andThen(ColorSpaceTransformation.HSV_2_RGB));
	}
	
	public static ColorGrader illuminate(double m){
		return gradeAsPixels(ColorSpaceTransformation.RGB_2_HSV
				.andThen(px->px.setB_fromDouble(px.b_asDouble()*m))
				.andThen(ColorSpaceTransformation.HSV_2_RGB));
	}
	
	public static ColorGrader gradeAsImg(Consumer<Img> consumer){
		return colors -> consumer.accept(new Img(colors.length, 1, colors));
	}
	
	public static ColorGrader gradeAsPixels(Consumer<PixelBase> consumer){
		return gradeAsImg(img->img.forEach(consumer));
	}
	
	public static void main(String[] args) {
		ColorMap map1 = DefaultColorMap.COOL_WARM;
		ColorMap map2 = DefaultColorMap.COOL_WARM.colorGrade(saturate(0),colorize(0xff_987895));
		Img img = new Img(200, 100);
		img.forEach(px->px.setValue((px.getYnormalized()<0.5?map1:map2).interpolate(px.getXnormalized())));
		ImageFrame.display(img);
	}
	
}
