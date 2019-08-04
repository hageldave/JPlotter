package hageldave.jplotter.color;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.IntStream;

import hageldave.jplotter.util.Utils;

/**
 * Implementation of the {@link ColorMap} interface.
 * 
 * @author hageldave
 */
public class SimpleColorMap implements ColorMap {
	
	protected int[] colors;
	protected double[] locations;
	
	/**
	 * Creates a {@link SimpleColorMap} with the specified colors
	 * and corresponding locations in the unit interval.
	 * As required by the {@link ColorMap} interface the first location has
	 * to be 0, the last location has to 1 and the locations in between have
	 * to monotonically increase within ]0,1[.
	 * 
	 * @param colors integer packed ARGB values (e.g. 0xff00ff00 is opaque green)
	 * @param locations corresponding to colors. Within [0,1].
	 * 
	 * @throws IllegalArgumentException if colors and locations are of different
	 * lengths, if less than 2 colors are specified, if locations does not start
	 * with 0 or does not end with 1, if locations is not sorted in ascending order.
	 */
	public SimpleColorMap(int[] colors, double[] locations) {
		if(Objects.requireNonNull(colors).length != Objects.requireNonNull(locations).length)
			throw new IllegalArgumentException("provided arrays of different lengths");
		if(colors.length < 2)
			throw new IllegalArgumentException("color map needs at least 2 colors");
		if(locations[0] != 0 || locations[locations.length-1] != 1)
			throw new IllegalArgumentException("locations array needs to start with 0 and end with 1");
		if(!Utils.isSorted(Arrays.stream(locations).iterator()))
			throw new IllegalArgumentException("locations have to be sorted in ascending order");
		
		this.colors = colors;
		this.locations = locations;
	}
	
	/**
	 * Creates a new {@link SimpleColorMap} with the specified colors
	 * and uniform spacing.
	 * 
	 * @param colors integer packed ARGB values (e.g. 0xff00ff00 is opaque green)
	 * 
	 * @throws IllegalArgumentException when less than 2 colors are specified.
	 */
	public SimpleColorMap(int... colors) {
		if(Objects.requireNonNull(colors).length < 2)
			throw new IllegalArgumentException("color map needs at least 2 colors");
		this.colors = colors;
		this.locations = IntStream.range(0, colors.length)
				.mapToDouble( i -> i*1.0/(colors.length-1) )
				.toArray();
	}
	
	@Override
	public int numColors(){
		return colors.length;
	}
	
	@Override
	public int getColor(int index){
		return colors[index];
	}
	
	@Override
	public int[] getColors(){
		return colors;
	}
	
	@Override
	public double getLocation(int index){
		return locations[index];
	}
	
	@Override
	public double[] getLocations() {
		return locations;
	}
}