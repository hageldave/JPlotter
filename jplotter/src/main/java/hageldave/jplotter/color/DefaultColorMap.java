package hageldave.jplotter.color;

public enum DefaultColorMap implements ColorMap {
	S_BLACK_WHITE(0xff_000000,0xff_ffffff),
	S_GRAY_WHITE(0xff_888888,0xff_ffffff),
	S_BLACK_GRAY(0xff_000000,0xff_888888),
	S_RAINBOW(
			0xff_dd00dd,
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
	S_BLACKBODYRAD(
			new int[]{0xff_000000, 0xff_aa2222, 0xff_dd6605, 0xff_eedd22, 0xff_ffffff},
			new double[]{       0,        0.39,        0.58,        0.84,           1}
			),
	S_VIRIDIS(
			0xff_440154,
			0xff_433982,
			0xff_30678d,
			0xff_208f8c,
			0xff_35b778,
			0xff_8fd643,
			0xff_fde724
			),
	S_PLASMA(
			0xff_0c0786,
			0xff_5c00a5,
			0xff_9b179e,
			0xff_ca4677,
			0xff_ec7853,
			0xff_fdb22e,
			0xff_eff821
			),
	S_RISING_DEEP_PURPLE(
			0xff_49006a,
			0xff_8a0179,
			0xff_cd238e,
			0xff_f667a0,
			0xff_faabb8,
			0xff_fcd6d2,
			0xff_fff7f3
			),
	S_BEACH(0xff_081d58,
			0xff_24419a,
			0xff_1e80b8,
			0xff_40b5c3,
			0xff_97d6b8,
			0xff_dff2b2,
			0xff_ffffd9
			),
	S_COPPER(
			0xff_000000,
			0xff_330000,
			0xff_660000,
			0xff_993322,
			0xff_cc6644,
			0xff_ff9966,
			0xff_ffcc88
			),
	S_ROMANIAN_PERMUTATION(
			new int[]{0xff_00277c, 0xff_cf0326, 0xff_fdd339},
			new double[]{       0,         0.6,           1}),
	D_BLACK_WHITE_BLACK(0xff000000, 0xff_ffffff, 0xff_000000),
	D_BLACK_GRAY_BLACK(0xff_000000, 0xff_888888, 0xff_000000),
	D_GRAY_WHITE_GRAY(0xff_888888, 0xff_ffffff, 0xff_888888),
	D_COOL_WARM(
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
	D_BLUE_WHITE_RED(0xff_0000ff, 0xff_eeeeee, 0xff_ff0000),
	D_FRANCE(0xff_002395, 0xff_ffffff, 0xff_ed2939),
	D_IRELAND(0xff_169b62, 0xff_ffffff, 0xff_ff883e),
	D_SPECTRAL(
			0xff_9e0142,
			0xff_e95d46,
			0xff_fdbe6e,
			0xff_fefebd,
			0xff_bee5a0,
			0xff_54aeac,
			0xff_5e4fa2
			),
	Q_12_PAIRED(
			0xff_a6cee3,
			0xff_1f78b4,
			0xff_b2df8a,
			0xff_33a02c,
			0xff_fb9a99,
			0xff_e31a1c,
			0xff_fdbf6f,
			0xff_ff7f00,
			0xff_cab2d6,
			0xff_6a3d9a,
			0xff_ffff99,
			0xff_b15928
			),
	Q_12_SET3(
			0xff_8dd3c7,
			0xff_ffffb3,
			0xff_bebada,
			0xff_fb8072,
			0xff_80b1d3,
			0xff_fdb462,
			0xff_b3de69,
			0xff_fccde5,
			0xff_d9d9d9,
			0xff_bc80bd,
			0xff_ccebc5,
			0xff_ffed6f
			),
	Q_9_PASTEL1(
			0xff_fbb4ae,
			0xff_b3cde3,
			0xff_ccebc5,
			0xff_decbe4,
			0xff_fed9a6,
			0xff_ffffcc,
			0xff_e5d8bd,
			0xff_fddaec,
			0xff_f2f2f2
			),
	Q_9_SET1(
			0xff_e41a1c,
			0xff_377eb8,
			0xff_4daf4a,
			0xff_984ea3,
			0xff_ff7f00,
			0xff_ffff33,
			0xff_a65628,
			0xff_f781bf,
			0xff_999999
			),
	Q_8_ACCENT(
			0xff_7fc97f,
			0xff_beaed4,
			0xff_fdc086,
			0xff_ffff99,
			0xff_386cb0,
			0xff_f0027f,
			0xff_bf5b17,
			0xff_666666
			),
	Q_8_DARK2(
			0xff_1b9e77,
			0xff_d95f02,
			0xff_7570b3,
			0xff_e7298a,
			0xff_66a61e,
			0xff_e6ab02,
			0xff_a6761d,
			0xff_666666
			),
	Q_8_PASTEL2(
			0xff_b3e2cd,
			0xff_fdcdac,
			0xff_cbd5e8,
			0xff_f4cae4,
			0xff_e6f5c9,
			0xff_fff2ae,
			0xff_f1e2cc,
			0xff_cccccc
			),
	Q_8_SET2(
			0xff_66c2a5,
			0xff_fc8d62,
			0xff_8da0cb,
			0xff_e78ac3,
			0xff_a6d854,
			0xff_ffd92f,
			0xff_e5c494,
			0xff_b3b3b3
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
