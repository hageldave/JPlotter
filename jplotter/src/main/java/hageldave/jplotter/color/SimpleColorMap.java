package hageldave.jplotter.color;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.IntStream;

import hageldave.jplotter.util.Utils;

public class SimpleColorMap implements ColorMap {
	int[] colors;
	double[] locations;
	
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
	
	public SimpleColorMap(int... colors) {
		if(Objects.requireNonNull(colors).length < 2)
			throw new IllegalArgumentException("color map needs at least 2 colors");
		this.colors = colors;
		this.locations = IntStream.range(0, colors.length)
				.mapToDouble( i -> i*1.0/(colors.length-1) )
				.toArray();
	}
	
	public int numColors(){
		return colors.length;
	}
	
	public int getColor(int index){
		return colors[index];
	}
	
	public int[] getColors(){
		return colors;
	}
	
	public double getLocation(int index){
		return locations[index];
	}
	
	@Override
	public double[] getLocations() {
		return locations;
	}
}