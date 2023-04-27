package hageldave.jplotter.color;

/**
 * The {@link DefaultColorMap} enum provides predefined {@link ColorMap}s
 * for different usecases.
 * <br>
 * Each {@link DefaultColorMap} has a name starting with either of the
 * following letters which indicate the type of color map
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
	/** <img alt="image of colormap" src="https://raw.githubusercontent.com/wiki/hageldave/JPlotter/images/colormaps/S_BLACK_WHITE.png"> */
	S_BLACK_WHITE(0xff_000000,0xff_ffffff),
	/** <img alt="image of colormap" src="https://raw.githubusercontent.com/wiki/hageldave/JPlotter/images/colormaps/S_GRAY_WHITE.png"> */
	S_GRAY_WHITE(0xff_888888,0xff_ffffff),
	/** <img alt="image of colormap" src="https://raw.githubusercontent.com/wiki/hageldave/JPlotter/images/colormaps/S_BLACK_GRAY.png"> */
	S_BLACK_GRAY(0xff_000000,0xff_888888),
	/** <img alt="image of colormap" src="https://raw.githubusercontent.com/wiki/hageldave/JPlotter/images/colormaps/S_RISING_DEEP_PURPLE.png"> */
	S_RISING_DEEP_PURPLE(
			0xff_49006a,
			0xff_8a0179,
			0xff_cd238e,
			0xff_f667a0,
			0xff_faabb8,
			0xff_fcd6d2,
			0xff_fff7f3
			),
	/** <img alt="image of colormap" src="https://raw.githubusercontent.com/wiki/hageldave/JPlotter/images/colormaps/S_COPPER.png"> */
	S_COPPER(
			0xff_000000,
			0xff_330000,
			0xff_660000,
			0xff_993322,
			0xff_cc6644,
			0xff_ff9966,
			0xff_ffcc88
			),
	/** <img alt="image of colormap" src="https://raw.githubusercontent.com/wiki/hageldave/JPlotter/images/colormaps/S_BLACKBODYRAD.png"> */
	S_BLACKBODYRAD(
			new int[]{0xff_000000, 0xff_aa2222, 0xff_dd6605, 0xff_eedd22, 0xff_ffffff},
			new double[]{       0,        0.39,        0.58,        0.84,           1}
			),
	/** <img alt="image of colormap" src="https://raw.githubusercontent.com/wiki/hageldave/JPlotter/images/colormaps/S_PLASMA.png"> */
	S_PLASMA(
			0xff_0c0786,
			0xff_5c00a5,
			0xff_9b179e,
			0xff_ca4677,
			0xff_ec7853,
			0xff_fdb22e,
			0xff_eff821
			),
	/** <img alt="image of colormap" src="https://raw.githubusercontent.com/wiki/hageldave/JPlotter/images/colormaps/S_VIRIDIS.png"> */
	S_VIRIDIS(
			0xff_440154,
			0xff_433982,
			0xff_30678d,
			0xff_208f8c,
			0xff_35b778,
			0xff_8fd643,
			0xff_fde724
			),
	/** <img alt="image of colormap" src="https://raw.githubusercontent.com/wiki/hageldave/JPlotter/images/colormaps/S_BEACH.png"> */
	S_BEACH(0xff_081d58,
			0xff_24419a,
			0xff_1e80b8,
			0xff_40b5c3,
			0xff_97d6b8,
			0xff_dff2b2,
			0xff_ffffd9
			),
	/** <img alt="image of colormap" src="https://raw.githubusercontent.com/wiki/hageldave/JPlotter/images/colormaps/S_RAINBOW.png"> */
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
	/** <img alt="image of colormap" src="https://raw.githubusercontent.com/wiki/hageldave/JPlotter/images/colormaps/S_TERRAIN.png"> */
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

	// TODO: Add license from https://colorcet.com/download/index.html
	S_CET_I2(0xff_70D1FF,
			0xff_74D5E0,
			0xff_7FD7BC,
			0xff_9BD596,
			0xff_C4CD7D,
			0xff_E9C176,
			0xff_FFB380
	),

	// TODO: Add license from https://ai.googleblog.com/2019/08/turbo-improved-rainbow-colormap-for.html
	S_TURBO(
			0xff_30123B,
			0xff_3B2F80,
			0xff_424BB5,
			0xff_4666DD,
			0xff_4680F6,
			0xff_4099FF,
			0xff_2FB2F4,
			0xff_1FC9DD,
			0xff_18DDC2,
			0xff_22EBAA,
			0xff_3FF68A,
			0xff_65FD69,
			0xff_8BFF4B,
			0xff_A9FB39,
			0xff_C3F134,
			0xff_DBE236,
			0xff_EECF3A,
			0xff_FABA39,
			0xff_FEA130,
			0xff_FC8423,
			0xff_F46617,
			0xff_E84B0C,
			0xff_D83706,
			0xff_C32503
	),

	// COLORBLIND
	// TODO: Add license from https://colorcet.com/download/index.html
	S_CET_CBL1(
			0xff_111111,
			0xff_06305E,
			0xff_28518E,
			0xff_777578,
			0xff_AE9B59,
			0xff_D7C386,
			0xff_F0EDE8
	),

	S_CET_CBL2(
			0xff_111111,
			0xff_15294A,
			0xff_0F437D,
			0xff_0C5EB0,
			0xff_017BE5,
			0xff_8398D0,
			0xff_CEB86D,
			0xff_F9D73E
	),

	S_CET_I3(
			0xff_14B9E6,
			0xff_46B6E8,
			0xff_62B3EB,
			0xff_78AFEC,
			0xff_8CABEC,
			0xff_9FA7EA,
			0xff_B1A2E6,
			0xff_C19DDE,
			0xff_CF98D5,
			0xff_DC94CC,
			0xff_E88FC2,
			0xff_F28AB8
	),

	S_CET_C1(
			0xff_F985F8,
			0xff_FC81EE,
			0xff_FD7AE2,
			0xff_FC72D4,
			0xff_FA69C5,
			0xff_F760B5,
			0xff_F356A6,
			0xff_EF4D97,
			0xff_EA4388,
			0xff_E53B79,
			0xff_DE336A,
			0xff_D72C5A,
			0xff_CF244B,
			0xff_C71E3C,
			0xff_C0182E,
			0xff_BA1721,
			0xff_B61B16,
			0xff_B5230D,
			0xff_B62E07,
			0xff_B93905,
			0xff_BD4304,
			0xff_C14D04,
			0xff_C55705,
			0xff_C96005,
			0xff_CC6A05,
			0xff_CF7305,
			0xff_D17D05,
			0xff_D38604,
			0xff_D59004,
			0xff_D69806,
			0xff_D6A00C,
			0xff_D5A718,
			0xff_D0AA27,
			0xff_C7AA37,
			0xff_BCA846,
			0xff_AFA455,
			0xff_A19F63,
			0xff_929A70,
			0xff_81957C,
			0xff_6E9088,
			0xff_598B94,
			0xff_4384A0,
			0xff_2F7DAC,
			0xff_2374B8,
			0xff_2269C5,
			0xff_285DD1,
			0xff_2E51DD,
			0xff_3546E8,
			0xff_3E3EF0,
			0xff_493DF5,
			0xff_5540F8,
			0xff_6147F9,
			0xff_6B4FF9,
			0xff_7557F9,
			0xff_7F5EFA,
			0xff_8A65FA,
			0xff_966BFA,
			0xff_A371FB,
			0xff_B175FC,
			0xff_C079FE,
			0xff_CE7DFF,
			0xff_DB81FF,
			0xff_E884FF,
			0xff_F286FD
	),

	/** <img alt="image of colormap" src="https://raw.githubusercontent.com/wiki/hageldave/JPlotter/images/colormaps/D_BLACK_WHITE_BLACK.png"> */
	D_BLACK_WHITE_BLACK(0xff000000, 0xff_ffffff, 0xff_000000),
	/** <img alt="image of colormap" src="https://raw.githubusercontent.com/wiki/hageldave/JPlotter/images/colormaps/D_BLACK_GRAY_BLACK.png"> */
	D_BLACK_GRAY_BLACK(0xff_000000, 0xff_888888, 0xff_000000),
	/** <img alt="image of colormap" src="https://raw.githubusercontent.com/wiki/hageldave/JPlotter/images/colormaps/D_GRAY_WHITE_GRAY.png"> */
	D_GRAY_WHITE_GRAY(0xff_888888, 0xff_ffffff, 0xff_888888),
	/** <img alt="image of colormap" src="https://raw.githubusercontent.com/wiki/hageldave/JPlotter/images/colormaps/D_COOL_WARM.png"> */
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
	/** <img alt="image of colormap" src="https://raw.githubusercontent.com/wiki/hageldave/JPlotter/images/colormaps/D_BLUE_WHITE_RED.png"> */
	D_BLUE_WHITE_RED(0xff_0000ff, 0xff_eeeeee, 0xff_ff0000),
	/** <img alt="image of colormap" src="https://raw.githubusercontent.com/wiki/hageldave/JPlotter/images/colormaps/D_FRANCE.png"> */
	D_FRANCE(0xff_002395, 0xff_ffffff, 0xff_ed2939),
	/** <img alt="image of colormap" src="https://raw.githubusercontent.com/wiki/hageldave/JPlotter/images/colormaps/D_IRELAND.png"> */
	D_IRELAND(0xff_169b62, 0xff_ffffff, 0xff_ff883e),
	/** <img alt="image of colormap" src="https://raw.githubusercontent.com/wiki/hageldave/JPlotter/images/colormaps/D_SPECTRAL.png"> */
	D_SPECTRAL(
			0xff_9e0142,
			0xff_e95d46,
			0xff_fdbe6e,
			0xff_fefebd,
			0xff_bee5a0,
			0xff_54aeac,
			0xff_5e4fa2
			),

	D_CET_CBD1(
			0xff_3A90FE,
			0xff_7DA7FC,
			0xff_A9BEF9,
			0xff_CED7F5,
			0xff_EDEDEC,
			0xff_E2D6B8,
			0xff_D1BD83,
			0xff_BDA64E
	),

	D_CET_D02(

			0xff_39970E,
			0xff_5EA63D,
			0xff_7DB561,
			0xff_9AC383,
			0xff_B6D2A5,
			0xff_D2E0C9,
			0xff_EAEBE9,
			0xff_EAD9EF,
			0xff_E2C1EE,
			0xff_D9A9EC,
			0xff_CF91EA,
			0xff_C578E7
	),

	D_CET_D04(
			0xff_1981FA,
			0xff_2479E7,
			0xff_2B70D3,
			0xff_2E67C0,
			0xff_315FAE,
			0xff_32579B,
			0xff_314F89,
			0xff_304678,
			0xff_2E3E67,
			0xff_2C3756,
			0xff_292F46,
			0xff_242837,
			0xff_212228,
			0xff_241F20,
			0xff_2F2220,
			0xff_3E2722,
			0xff_4D2B25,
			0xff_5C2F28,
			0xff_6B342B,
			0xff_7B382E,
			0xff_8A3C31,
			0xff_9B3F34,
			0xff_AB4337,
			0xff_BB473A
	),

	D_CET_D07(
			0xff_1431C1,
			0xff_4341BA,
			0xff_5B50B3,
			0xff_6C60AB,
			0xff_7A6FA2,
			0xff_867F9A,
			0xff_908F91,
			0xff_A49D85,
			0xff_B7AA77,
			0xff_C9B767,
			0xff_DAC556,
			0xff_EAD33F
	),

	D_CET_D11(
			0xff_00B6FF,
			0xff_49B5F4,
			0xff_69B3E5,
			0xff_80B1D7,
			0xff_90AFC9,
			0xff_9EADBA,
			0xff_ABABAC,
			0xff_B9A8A1,
			0xff_C7A397,
			0xff_D49F8D,
			0xff_E09B83,
			0xff_EB9679
	),

	D_CET_D13(
			0xff_112D68,
			0xff_254895,
			0xff_3467C0,
			0xff_338BE3,
			0xff_4EB0F8,
			0xff_9CD0FA,
			0xff_DFEAEF,
			0xff_9EDCC9,
			0xff_4DC19C,
			0xff_24A065,
			0xff_1E7E36,
			0xff_0F5D18
	),

	/** <img alt="image of colormap" src="https://raw.githubusercontent.com/wiki/hageldave/JPlotter/images/colormaps/Q_8_DARK2.png"> */
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
	/** <img alt="image of colormap" src="https://raw.githubusercontent.com/wiki/hageldave/JPlotter/images/colormaps/Q_8_SET2.png"> */
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
	/** <img alt="image of colormap" src="https://raw.githubusercontent.com/wiki/hageldave/JPlotter/images/colormaps/Q_8_PASTEL2.png"> */
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
	/** <img alt="image of colormap" src="https://raw.githubusercontent.com/wiki/hageldave/JPlotter/images/colormaps/Q_8_ACCENT.png"> */
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
	/** <img alt="image of colormap" src="https://raw.githubusercontent.com/wiki/hageldave/JPlotter/images/colormaps/Q_9_SET1.png"> */
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
	/** <img alt="image of colormap" src="https://raw.githubusercontent.com/wiki/hageldave/JPlotter/images/colormaps/Q_9_PASTEL1.png"> */
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
	/** <img alt="image of colormap" src="https://raw.githubusercontent.com/wiki/hageldave/JPlotter/images/colormaps/Q_12_PAIRED.png"> */
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
	/** <img alt="image of colormap" src="https://raw.githubusercontent.com/wiki/hageldave/JPlotter/images/colormaps/Q_12_SET3.png"> */
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
