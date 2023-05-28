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

	/**
	 * <img alt="image of colormap" src="https://raw.githubusercontent.com/wiki/hageldave/JPlotter/images/colormaps/S_CET_I2.png">
	 * <br>
	 * Copyright holder of the colormap is Peter Kovesi, https://arxiv.org/abs/1509.03700
	 * It has been released under the CC BY 4.0 licence: https://creativecommons.org/licenses/by/4.0/legalcode
	 * It has been downsampled and is therefore slightly modified, but is perceptually indistinguishable.
	 * <img alt="image of colormap" src="https://raw.githubusercontent.com/wiki/hageldave/JPlotter/images/colormaps/S_CET_I2.png"> */
	S_CET_I2(
			0xff_70D1FF,
			0xff_71D3F6,
			0xff_73D4E9,
			0xff_75D5DC,
			0xff_79D6CE,
			0xff_7ED7C0,
			0xff_85D7B1,
			0xff_8FD6A2,
			0xff_9CD594,
			0xff_ACD289,
			0xff_BCCF81,
			0xff_CBCB7B,
			0xff_DAC677,
			0xff_E7C276,
			0xff_F4BC77,
			0xff_FFB77C,
			0xff_FFB282
	),


	/**
	 * <img alt="image of colormap" src="https://raw.githubusercontent.com/wiki/hageldave/JPlotter/images/colormaps/S_TURBO.png">
	 * <br>
	 * Copyright 2019 Google LLC.
	 * SPDX-License-Identifier: Apache-2.0
	 * Author: Anton Mikhailov
	 * The colormap (https://ai.googleblog.com/2019/08/turbo-improved-rainbow-colormap-for.html) has been downsampled and is therefore slightly modified, but is perceptually indistinguishable.
	 */
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

	/**
	 * <img alt="image of colormap" src="https://raw.githubusercontent.com/wiki/hageldave/JPlotter/images/colormaps/S_CET_CBL1.png">
	 * <br>
	 * Copyright holder of the colormap is Peter Kovesi, https://arxiv.org/abs/1509.03700
	 * It has been released under the CC BY 4.0 licence: https://creativecommons.org/licenses/by/4.0/legalcode
	 * It has been downsampled and is therefore slightly modified, but is perceptually indistinguishable.
	 */
	S_CET_CBL1(
			0xff_111111,
			0xff_141D2F,
			0xff_0E284D,
			0xff_013568,
			0xff_01417E,
			0xff_204E8C,
			0xff_435B8D,
			0xff_616982,
			0xff_7A7776,
			0xff_90856A,
			0xff_A5935C,
			0xff_B7A25A,
			0xff_C7B169,
			0xff_D5C182,
			0xff_E1D1A2,
			0xff_EBE1C8,
			0xff_F0F1F0
	),

	/**
	 * <img alt="image of colormap" src="https://raw.githubusercontent.com/wiki/hageldave/JPlotter/images/colormaps/S_CET_CBL2.png">
	 * <br>
	 * Copyright holder of the colormap is Peter Kovesi, https://arxiv.org/abs/1509.03700
	 * It has been released under the CC BY 4.0 licence: https://creativecommons.org/licenses/by/4.0/legalcode
	 * It has been downsampled and is therefore slightly modified, but is perceptually indistinguishable.
	 */
	S_CET_CBL2(
			0xff_111111,
			0xff_161D2D,
			0xff_15294A,
			0xff_103665,
			0xff_0F437D,
			0xff_0F5096,
			0xff_0C5EB0,
			0xff_066DCA,
			0xff_017BE5,
			0xff_3B8AF1,
			0xff_8398D0,
			0xff_AFA89E,
			0xff_CEB86D,
			0xff_E7C735,
			0xff_F9D73E,
			0xff_FFE8A5,
			0xff_FCF9F3
	),

	/**
	 * <img alt="image of colormap" src="https://raw.githubusercontent.com/wiki/hageldave/JPlotter/images/colormaps/S_CET_I3.png">
	 * <br>
	 * Copyright holder of the colormap is Peter Kovesi, https://arxiv.org/abs/1509.03700
	 * It has been released under the CC BY 4.0 licence: https://creativecommons.org/licenses/by/4.0/legalcode
	 * It has been downsampled and is therefore slightly modified, but is perceptually indistinguishable.
	 */
	S_CET_I3(
			0xff_14B9E6,
			0xff_3EB7E8,
			0xff_56B5EA,
			0xff_69B2EB,
			0xff_79AFEC,
			0xff_89ACEC,
			0xff_97A8EB,
			0xff_A5A5E9,
			0xff_B3A1E5,
			0xff_BF9EDF,
			0xff_CA9AD8,
			0xff_D497D2,
			0xff_DE93CA,
			0xff_E790C3,
			0xff_EF8CBC,
			0xff_F788B4,
			0xff_FD85AD
	),

	/**
	 * <img alt="image of colormap" src="https://raw.githubusercontent.com/wiki/hageldave/JPlotter/images/colormaps/S_CET_C1.png">
	 * <br>
	 * Copyright holder of the colormap is Peter Kovesi, https://arxiv.org/abs/1509.03700
	 * It has been released under the CC BY 4.0 licence: https://creativecommons.org/licenses/by/4.0/legalcode
	 * It has been downsampled and is therefore slightly modified, but is perceptually indistinguishable.
	 */
	S_CET_C1(
			0xff_F985F8,
			0xff_FA69C5,
			0xff_EA4388,
			0xff_CF244B,
			0xff_B61B16,
			0xff_BD4304,
			0xff_CC6A05,
			0xff_D59004,
			0xff_D0AA27,
			0xff_A19F63,
			0xff_598B94,
			0xff_2269C5,
			0xff_3E3EF0,
			0xff_6B4FF9,
			0xff_966BFA,
			0xff_CE7DFF,
			0xff_F785F9
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

	/**
	 * <img alt="image of colormap" src="https://raw.githubusercontent.com/wiki/hageldave/JPlotter/images/colormaps/D_CET_CBD1.png">
	 * <br>
	 * Copyright holder of the colormap is Peter Kovesi, https://arxiv.org/abs/1509.03700
	 * It has been released under the CC BY 4.0 licence: https://creativecommons.org/licenses/by/4.0/legalcode
	 * It has been downsampled and is therefore slightly modified, but is perceptually indistinguishable.
	 */
	D_CET_CBD1(
			0xff_3A90FE,
			0xff_619CFD,
			0xff_7DA7FC,
			0xff_94B3FA,
			0xff_A9BEF9,
			0xff_BCCBF7,
			0xff_CED7F5,
			0xff_DFE3F3,
			0xff_EDEDEC,
			0xff_E9E3D3,
			0xff_E2D6B8,
			0xff_DACA9D,
			0xff_D1BD83,
			0xff_C7B169,
			0xff_BDA64E,
			0xff_B39A31,
			0xff_A89008
	),

	/**
	 * <img alt="image of colormap" src="https://raw.githubusercontent.com/wiki/hageldave/JPlotter/images/colormaps/D_CET_D02.png">
	 * <br>
	 * Copyright holder of the colormap is Peter Kovesi, https://arxiv.org/abs/1509.03700
	 * It has been released under the CC BY 4.0 licence: https://creativecommons.org/licenses/by/4.0/legalcode
	 * It has been downsampled and is therefore slightly modified, but is perceptually indistinguishable.
	 */
	D_CET_D02(0xff_39970E,
			0xff_56A234,
			0xff_6EAE50,
			0xff_85B96A,
			0xff_9CC485,
			0xff_B1CF9F,
			0xff_C6DABA,
			0xff_DBE5D4,
			0xff_ECEBEB,
			0xff_EBDDEF,
			0xff_E5CAEE,
			0xff_DEB8ED,
			0xff_D7A6EB,
			0xff_D093EA,
			0xff_C880E8,
			0xff_C06DE6,
			0xff_B859E4
	),

	/**
	 * <img alt="image of colormap" src="https://raw.githubusercontent.com/wiki/hageldave/JPlotter/images/colormaps/D_CET_D04.png">
	 * <br>
	 * Copyright holder of the colormap is Peter Kovesi, https://arxiv.org/abs/1509.03700
	 * It has been released under the CC BY 4.0 licence: https://creativecommons.org/licenses/by/4.0/legalcode
	 * It has been downsampled and is therefore slightly modified, but is perceptually indistinguishable.
	 */
	D_CET_D04(
			0xff_1981FA,
			0xff_2873DB,
			0xff_2F65BD,
			0xff_31589F,
			0xff_314B82,
			0xff_2E3E67,
			0xff_2A324D,
			0xff_242734,
			0xff_221F21,
			0xff_352421,
			0xff_4D2B25,
			0xff_65322A,
			0xff_7E392F,
			0xff_973F34,
			0xff_B14538,
			0xff_CC4A3D,
			0xff_E65042
	),

	/**
	 * <img alt="image of colormap" src="https://raw.githubusercontent.com/wiki/hageldave/JPlotter/images/colormaps/D_CET_D07.png">
	 * <br>
	 * Copyright holder of the colormap is Peter Kovesi, https://arxiv.org/abs/1509.03700
	 * It has been released under the CC BY 4.0 licence: https://creativecommons.org/licenses/by/4.0/legalcode
	 * It has been downsampled and is therefore slightly modified, but is perceptually indistinguishable.
	 */
	D_CET_D07(
			0xff_1431C1,
			0xff_3B3DBC,
			0xff_5149B6,
			0xff_6054B0,
			0xff_6D60AA,
			0xff_786CA4,
			0xff_81789E,
			0xff_898597,
			0xff_919190,
			0xff_A19B86,
			0xff_B0A57C,
			0xff_BEAF71,
			0xff_CBB965,
			0xff_D8C457,
			0xff_E5CF47,
			0xff_F1DA32,
			0xff_FDE409
	),

	/**
	 * <img alt="image of colormap" src="https://raw.githubusercontent.com/wiki/hageldave/JPlotter/images/colormaps/D_CET_D11.png">
	 * <br>
	 * Copyright holder of the colormap is Peter Kovesi, https://arxiv.org/abs/1509.03700
	 * It has been released under the CC BY 4.0 licence: https://creativecommons.org/licenses/by/4.0/legalcode
	 * It has been downsampled and is therefore slightly modified, but is perceptually indistinguishable.
	 */
	D_CET_D11(
			0xff_00B6FF,
			0xff_3EB5F7,
			0xff_5CB4EC,
			0xff_70B2E1,
			0xff_80B0D6,
			0xff_8EAFCB,
			0xff_99AEC1,
			0xff_A3ACB6,
			0xff_ACABAB,
			0xff_B7A8A3,
			0xff_C2A59B,
			0xff_CCA293,
			0xff_D59F8B,
			0xff_DE9B84,
			0xff_E7977C,
			0xff_EF9474,
			0xff_F6906D
	),

	/**
	 * <img alt="image of colormap" src="https://raw.githubusercontent.com/wiki/hageldave/JPlotter/images/colormaps/D_CET_D13.png">
	 * <br>
	 * Copyright holder of the colormap is Peter Kovesi, https://arxiv.org/abs/1509.03700
	 * It has been released under the CC BY 4.0 licence: https://creativecommons.org/licenses/by/4.0/legalcode
	 * It has been downsampled and is therefore slightly modified, but is perceptually indistinguishable.
	 */
	D_CET_D13(
			0xff_112D68,
			0xff_21418A,
			0xff_2E58AC,
			0xff_3671CB,
			0xff_338DE5,
			0xff_42AAF5,
			0xff_78C3FB,
			0xff_B7DAF8,
			0xff_DFEBEC,
			0xff_ABDFCF,
			0xff_69CCAE,
			0xff_36B588,
			0xff_249B5D,
			0xff_1F8139,
			0xff_156820,
			0xff_08500F,
			0xff_003A02
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
