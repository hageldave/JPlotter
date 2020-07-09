package hageldave.jplotter.color;

/**
 * The {@link DefaultColorMap} enum provides predefined {@link ColorMap}s
 * for different usecases.
 * <br>
 * Each {@link DefaultColorMap} has a name starting with either of the
 * followig letters which indicate the type of color map
 * <ul>
 * <li> S - sequential color map for mapping de-/increasing values, e.g.
 * {@link #S_PLASMA}.
 * <li> D - diverging color map for mapping values around a pivot value, e.g.
 * {@link #D_COOL_WARM}.
 * <li> Q - qualitative color map for mapping categorical values. Qualitative
 * color map names also specify for how many categories they are suited, e.g.
 * {@link #Q_12_PAIRED} has discrete colors for up to 12 categories.
 * </ul><br>
 * For a visual overview see 
 * <a href="https://github.com/hageldave/JPlotter/wiki/Color-Maps">
 * github.com/hageldave/JPlotter/wiki/Color-Maps
 * </a>
 * 
 * @author hageldave
 */
public enum DefaultColorMap implements ColorMap {
	/** <img src="https://raw.githubusercontent.com/wiki/hageldave/JPlotter/images/colormaps/S_BLACK_WHITE.png"> */
	S_BLACK_WHITE(0xff_000000,0xff_ffffff),
	/** <img src="https://raw.githubusercontent.com/wiki/hageldave/JPlotter/images/colormaps/S_GRAY_WHITE.png"> */
	S_GRAY_WHITE(0xff_888888,0xff_ffffff),
	/** <img src="https://raw.githubusercontent.com/wiki/hageldave/JPlotter/images/colormaps/S_BLACK_GRAY.png"> */
	S_BLACK_GRAY(0xff_000000,0xff_888888),
	/** <img src="https://raw.githubusercontent.com/wiki/hageldave/JPlotter/images/colormaps/S_RISING_DEEP_PURPLE.png"> */
	S_RISING_DEEP_PURPLE(
			0xff_49006a,
			0xff_8a0179,
			0xff_cd238e,
			0xff_f667a0,
			0xff_faabb8,
			0xff_fcd6d2,
			0xff_fff7f3
			),
	/** <img src="https://raw.githubusercontent.com/wiki/hageldave/JPlotter/images/colormaps/S_COPPER.png"> */
	S_COPPER(
			0xff_000000,
			0xff_330000,
			0xff_660000,
			0xff_993322,
			0xff_cc6644,
			0xff_ff9966,
			0xff_ffcc88
			),
	/** <img src="https://raw.githubusercontent.com/wiki/hageldave/JPlotter/images/colormaps/S_BLACKBODYRAD.png"> */
	S_BLACKBODYRAD(
			new int[]{0xff_000000, 0xff_aa2222, 0xff_dd6605, 0xff_eedd22, 0xff_ffffff},
			new double[]{       0,        0.39,        0.58,        0.84,           1}
			),
	/** <img src="https://raw.githubusercontent.com/wiki/hageldave/JPlotter/images/colormaps/S_PLASMA.png"> */
	S_PLASMA(
			0xff_0c0786,
			0xff_5c00a5,
			0xff_9b179e,
			0xff_ca4677,
			0xff_ec7853,
			0xff_fdb22e,
			0xff_eff821
			),
	/** <img src="https://raw.githubusercontent.com/wiki/hageldave/JPlotter/images/colormaps/S_VIRIDIS.png"> */
	S_VIRIDIS(
			0xff_440154,
			0xff_433982,
			0xff_30678d,
			0xff_208f8c,
			0xff_35b778,
			0xff_8fd643,
			0xff_fde724
			),
	/** <img src="https://raw.githubusercontent.com/wiki/hageldave/JPlotter/images/colormaps/S_BEACH.png"> */
	S_BEACH(0xff_081d58,
			0xff_24419a,
			0xff_1e80b8,
			0xff_40b5c3,
			0xff_97d6b8,
			0xff_dff2b2,
			0xff_ffffd9
			),
	/** <img src="https://raw.githubusercontent.com/wiki/hageldave/JPlotter/images/colormaps/S_RAINBOW.png"> */
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
	/** <img src="https://raw.githubusercontent.com/wiki/hageldave/JPlotter/images/colormaps/S_TERRAIN.png"> */
	S_TERRAIN(
			0xff_333399,
			0xff_1075db,
			0xff_00b1b4,
			0xff_31d56f,
			0xff_95e983,
			0xff_fdfe98,
			0xff_ccbd7d,
			0xff_9a7d62,
			0xff_977a74,
			0xff_cabbb8,
			0xff_fdfcfc
			),
	/** <img src="https://raw.githubusercontent.com/wiki/hageldave/JPlotter/images/colormaps/D_BLACK_WHITE_BLACK.png"> */
	D_BLACK_WHITE_BLACK(0xff000000, 0xff_ffffff, 0xff_000000),
	/** <img src="https://raw.githubusercontent.com/wiki/hageldave/JPlotter/images/colormaps/D_BLACK_GRAY_BLACK.png"> */
	D_BLACK_GRAY_BLACK(0xff_000000, 0xff_888888, 0xff_000000),
	/** <img src="https://raw.githubusercontent.com/wiki/hageldave/JPlotter/images/colormaps/D_GRAY_WHITE_GRAY.png"> */
	D_GRAY_WHITE_GRAY(0xff_888888, 0xff_ffffff, 0xff_888888),
	/** <img src="https://raw.githubusercontent.com/wiki/hageldave/JPlotter/images/colormaps/D_COOL_WARM.png"> */
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
	/** <img src="https://raw.githubusercontent.com/wiki/hageldave/JPlotter/images/colormaps/D_BLUE_WHITE_RED.png"> */
	D_BLUE_WHITE_RED(0xff_0000ff, 0xff_eeeeee, 0xff_ff0000),
	/** <img src="https://raw.githubusercontent.com/wiki/hageldave/JPlotter/images/colormaps/D_FRANCE.png"> */
	D_FRANCE(0xff_002395, 0xff_ffffff, 0xff_ed2939),
	/** <img src="https://raw.githubusercontent.com/wiki/hageldave/JPlotter/images/colormaps/D_IRELAND.png"> */
	D_IRELAND(0xff_169b62, 0xff_ffffff, 0xff_ff883e),
	/** <img src="https://raw.githubusercontent.com/wiki/hageldave/JPlotter/images/colormaps/D_SPECTRAL.png"> */
	D_SPECTRAL(
			0xff_9e0142,
			0xff_e95d46,
			0xff_fdbe6e,
			0xff_fefebd,
			0xff_bee5a0,
			0xff_54aeac,
			0xff_5e4fa2
			),
	/** <img src="https://raw.githubusercontent.com/wiki/hageldave/JPlotter/images/colormaps/Q_8_DARK2.png"> */
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
	/** <img src="https://raw.githubusercontent.com/wiki/hageldave/JPlotter/images/colormaps/Q_8_SET2.png"> */
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
	/** <img src="https://raw.githubusercontent.com/wiki/hageldave/JPlotter/images/colormaps/Q_8_PASTEL2.png"> */
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
	/** <img src="https://raw.githubusercontent.com/wiki/hageldave/JPlotter/images/colormaps/Q_8_ACCENT.png"> */
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
	/** <img src="https://raw.githubusercontent.com/wiki/hageldave/JPlotter/images/colormaps/Q_9_SET1.png"> */
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
	/** <img src="https://raw.githubusercontent.com/wiki/hageldave/JPlotter/images/colormaps/Q_9_PASTEL1.png"> */
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
	/** <img src="https://raw.githubusercontent.com/wiki/hageldave/JPlotter/images/colormaps/Q_12_PAIRED.png"> */
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
	/** <img src="https://raw.githubusercontent.com/wiki/hageldave/JPlotter/images/colormaps/Q_12_SET3.png"> */
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
	public int[] getColors() {
		return map.getColors();
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
