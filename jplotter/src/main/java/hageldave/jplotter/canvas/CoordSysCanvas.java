//package hageldave.jplotter.canvas;
//
//import java.awt.AWTEventMulticaster;
//import java.awt.Color;
//import java.awt.Dimension;
//import java.awt.Font;
//import java.awt.Rectangle;
//import java.awt.event.ActionEvent;
//import java.awt.event.ActionListener;
//import java.awt.geom.Point2D;
//import java.awt.geom.Rectangle2D;
//import java.util.LinkedList;
//import java.util.Objects;
//
//import org.lwjgl.opengl.GL11;
//import org.w3c.dom.Document;
//import org.w3c.dom.Element;
//import org.w3c.dom.Node;
//
//import hageldave.jplotter.coordsys.ExtendedWilkinson;
//import hageldave.jplotter.coordsys.TickMarkGenerator;
//import hageldave.jplotter.font.CharacterAtlas;
//import hageldave.jplotter.interaction.CoordSysPanning;
//import hageldave.jplotter.interaction.CoordSysScrollZoom;
//import hageldave.jplotter.interaction.CoordinateViewListener;
//import hageldave.jplotter.renderables.Legend;
//import hageldave.jplotter.renderables.Lines;
//import hageldave.jplotter.renderables.Text;
//import hageldave.jplotter.renderers.AdaptableView;
//import hageldave.jplotter.renderers.CoordSysRenderer;
//import hageldave.jplotter.renderers.LinesRenderer;
//import hageldave.jplotter.renderers.Renderer;
//import hageldave.jplotter.renderers.TextRenderer;
//import hageldave.jplotter.svg.SVGUtils;
//import hageldave.jplotter.util.Annotations.GLContextRequired;
//import hageldave.jplotter.util.Annotations.GLCoordinates;
//import hageldave.jplotter.util.Pair;
//import hageldave.jplotter.util.PointeredPoint2D;
//import hageldave.jplotter.util.TranslatedPoint2D;
//import hageldave.jplotter.util.Utils;
//
///**
// * The CoordSysCanvas is an {@link FBOCanvas} that displays a coordinate system.
// * This coordinate system is enclosed by 4 axes that form a rectangle around the
// * area that displays the contents of the coordinate system.
// * <p>
// * The upper x-axis and right y-axis feature the labels (names) of the axes.
// * The lower x-axis and the left y-axis feature tick marks and labels that
// * help to orientate and read off coordinates.
// * The positioning and labeling of the tick marks is done by a {@link TickMarkGenerator}
// * which is per default an instance of {@link ExtendedWilkinson}.
// * For each tick a vertical or horizontal guide line is drawn across the area of the
// * coordinate system.
// * <p>
// * What coordinate range the coordinate system area corresponds to is controlled by
// * the coordinate view (see {@link #setCoordinateView(double, double, double, double)})
// * and defaults to [-1,1] for both axes.
// * The contents that are drawn inside the coordinate area are rendered by the content renderer
// * (see {@link #setContent(Renderer)}).
// * If that renderer implements the {@link AdaptableView} interface it will be passed the
// * view matrix corresponding to the coordinate view.
// * The content renderer will be able to draw within the viewport defined by the coordinate
// * system area of this Canvas.
// * <p>
// * Optionally a {@link Renderer} for drawing a legend (such as the {@link Legend} class)
// * can be set to either the bottom or right hand side of the coordinate system (can also
// * use both areas at once).
// * Use {@link #setLegendBottom(Renderer)} or {@link #setLegendRight(Renderer)} to do so.
// * The legend area size can be partially controlled by {@link #setLegendBottomHeight(int)}
// * and {@link #setLegendRightWidth(int)} if this is needed.
// * <p>
// * The overlay renderer ({@link #setOverlay(Renderer)}) can be used to finally draw over all
// * of the canvas viewport.
// * <p>
// * For interacting with this CoordSysCanvas there already exist implementations of MouseListeners
// * for panning and zooming (see {@link CoordSysPanning} and {@link CoordSysScrollZoom}).
// * 
// * @author hageldave
// */
//public class CoordSysCanvas extends FBOCanvas {
//	private static final long serialVersionUID = 1L;
//
//	protected CoordSysRenderer renderer;
//
//
//	/**
//	 * Creates a new {@link CoordSysCanvas}.
//	 */
//	public CoordSysCanvas() {
//		this(null);
//	}
//	
//	/**
//	 * Creates a new {@link CoordSysCanvas} with the specified {@link FBOCanvas}
//	 * as it's GL context sharing parent.
//	 * When sharing GL context both canvases can use the same GL textures and buffers
//	 * which saves memory and may also improve performance.
//	 * @param parent to share GL context with
//	 */
//	public CoordSysCanvas(FBOCanvas parent) {
//		super(parent);
//		
//	}
//
//	/**
//	 * Allocates GL resources, i.e. initializes its renderers.
//	 */
//	@Override
//	public void initGL() {
//		super.initGL();
//		if(Objects.nonNull(renderer))
//			renderer.glInit();
//	}
//
//	/**
//	 * Draws the coordinate system with its guides and labels
//	 * and draws the contents into the coordinate system area.
//	 */
//	@Override
//	public void paintToFBO(int w, int h) {
//		if(Objects.nonNull(renderer))
//			renderer.glInit();
//		renderer.render(w, h);
//	}
//	
//	@Override
//	protected void paintToSVG(Document doc, Element parent, int w, int h) {
//		this.renderer.renderSVG(doc, parent, w, h);
//	}
//
//	/**
//	 * Disposes of GL resources, i.e. closes its renderers and all resources 
//	 * of the FBOCanvas that it is (see {@link FBOCanvas#close()}).
//	 */
//	@Override
//	public void close() {
//		if(Objects.nonNull(renderer))
//			this.renderer.close();
//		super.close();
//	}
//
//}
