package hageldave.jplotter.color;

public enum DefaultColorMap implements ColorMap {
	BLACK_WHITE(0xff_000000,0xff_ffffff),
	GRAY_WHITE(0xff_888888,0xff_ffffff),
	BLACK_GRAY(0xff_000000,0xff_888888),
	BLACK_WHITE_BLACK(0xff000000,0xff_ffffff, 0xff_000000),
	BLACK_GRAY_BLACK(0xff_000000,0xff_888888, 0xff_000000),
	GRAY_WHITE_GRAY(0xff_888888,0xff_ffffff, 0xff_888888),
	RAINBOW(0xff_dd00dd,
			0xff_8800ee,
			0xff_0000ff, 
			0xff_0088ee,
			0xff_00dddd,
			0xff_00ee88, 
			0xff_00ff00, 
			0xff_88ee00,
			0xff_dddd00,
			0xff_ee8800, 
			0xff_ff0000),
	BLACKBODYRAD(
			new int[]{0xff_000000, 0xff_aa2222, 0xff_dd6605, 0xff_eedd22, 0xff_ffffff},
			new double[]{       0,        0.39,        0.58,        0.84,           1}
			),
	COOL_WARM(
			0xff_3b4cc0,
			0xff_6688ee,
			0xff_88bbff,
			0xff_b8d0f9,
			0xff_dddddd,
			0xff_f5c4ad,
			0xff_ff9977,
			0xff_dd6644,
			0xff_b40426
			),
	;
	
	final ColorMap map;
	
	private DefaultColorMap(int[] colors, double[] locations) {
		this.map = new SimpleColorMap(colors, locations);
	}
	
	private DefaultColorMap(int... colors) {
		this.map = new SimpleColorMap(colors);
	}
	
	private DefaultColorMap(ColorMap map){
		this.map = map;
	}

	@Override
	public int numColors() {
		return map.numColors();
	}

	@Override
	public int getColor(int index) {
		return map.getColor(index);
	}

	@Override
	public double getLocation(int index) {
		return map.getLocation(index);
	}

	@Override
	public double[] getLocations() {
		return map.getLocations();
	}

}
