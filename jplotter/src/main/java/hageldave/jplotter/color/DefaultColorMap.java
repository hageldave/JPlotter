package hageldave.jplotter.color;

public enum DefaultColorMap implements ColorMap {
	BLACK_WHITE(0xff_000000,0xff_ffffff),
	GRAY_WHITE(0xff_888888,0xff_ffffff),
	BLACK_GRAY(0xff_000000,0xff_888888),
	BLACK_WHITE_BLACK(0xff000000, 0xff_ffffff, 0xff_000000),
	BLACK_GRAY_BLACK(0xff_000000, 0xff_888888, 0xff_000000),
	GRAY_WHITE_GRAY(0xff_888888, 0xff_ffffff, 0xff_888888),
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
			0xff_ff0000
			),
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
	VIRIDIS(0xff_440154,
			0xff_433982,
			0xff_30678d,
			0xff_208f8c,
			0xff_35b778,
			0xff_8fd643,
			0xff_fde724
			),
	PLASMA( 0xff_0c0786,
			0xff_5c00a5,
			0xff_9b179e,
			0xff_ca4677,
			0xff_ec7853,
			0xff_fdb22e,
			0xff_eff821
			),
	RISING_DEEP_PURPLE(
			0xff_49006a,
			0xff_8a0179,
			0xff_cd238e,
			0xff_f667a0,
			0xff_faabb8,
			0xff_fcd6d2,
			0xff_fff7f3
			),
	BEACH(	0xff_081d58,
			0xff_24419a,
			0xff_1e80b8,
			0xff_40b5c3,
			0xff_97d6b8,
			0xff_dff2b2,
			0xff_ffffd9
			),
	COPPER(	0xff_000000,
			0xff_330000,
			0xff_660000,
			0xff_993322,
			0xff_cc6644,
			0xff_ff9966,
			0xff_ffcc88
			),
	BLUE_WHITE_RED(0xff_0000ff, 0xff_eeeeee, 0xff_ff0000),
	FRANCE(0xff_002395, 0xff_ffffff, 0xff_ed2939),
	IRELAND(0xff_169b62, 0xff_ffffff, 0xff_ff883e),
	ROMANIAN_PERMUTATION(new int[]{0xff_00277c, 0xff_cf0326, 0xff_fdd339},
			new double[]{       0,         0.6,           1}),
	SPECTRAL(
			0xff_9e0142,
			0xff_e95d46,
			0xff_fdbe6e,
			0xff_fefebd,
			0xff_bee5a0,
			0xff_54aeac,
			0xff_5e4fa2
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
