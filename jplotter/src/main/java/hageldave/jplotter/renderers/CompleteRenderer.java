package hageldave.jplotter.renderers;

import hageldave.jplotter.debugging.controlHandler.annotations.DebugGetter;
import hageldave.jplotter.debugging.controlHandler.annotations.DebugSetter;
import hageldave.jplotter.debugging.controlHandler.panelcreators.control.RenderOrderCreator;
import hageldave.jplotter.renderables.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.awt.*;
import java.awt.geom.Rectangle2D;

/**
 * The {@link CompleteRenderer} comprises a {@link LinesRenderer},
 * a {@link PointsRenderer}, a {@link TextRenderer} and 
 * a {@link TrianglesRenderer}.
 * It thus can render the most important graphical elements for
 * a scientific 2D visualization (hence its name).
 * <p>
 * The order in which these Renderers are processed by default is:
 * <ol>
 * <li>{@link Triangles}</li>
 * <li>{@link Lines}</li>
 * <li>{@link Curves}</li>
 * <li>{@link Points}</li>
 * <li>{@link Text}</li>
 * </ol>
 * This implies that Lines will be drawn over Triangles,
 * Curves over Lines, Points over Curves, and Text over Points.
 * Using the {@link #setRenderOrder(int, int, int, int, int)} method
 * the order of renderers can be changed.
 * <p>
 * To add {@link Renderable}s to this Renderer either use the public attributes
 * {@link #triangles}, {@link #lines}, {@link #curves} , {@link #points}, {@link #text} 
 * to directly access the desired renderer or use the {@link #addItemToRender(Renderable)}
 * method.
 * 
 * @author hageldave
 */
public class CompleteRenderer implements Renderer, AdaptableView, GLDoublePrecisionSupport {
	
	public final LinesRenderer lines = new LinesRenderer();
	public final PointsRenderer points = new PointsRenderer();
	public final TextRenderer text = new TextRenderer();
	public final TrianglesRenderer triangles = new TrianglesRenderer();
	public final CurvesRenderer curves = new CurvesRenderer();

	private final Renderer[] rendererLUT = {triangles,lines,curves,points,text};
	public static final int TRI = 0, LIN = 1, PNT = 2, TXT = 3, CRV = 4;
	private final int[] renderOrder = {TRI,LIN,CRV,PNT,TXT};
	boolean isEnabled = true;
	
	/**
	 * Sets the order of the renderers. 
	 * You can use the constants {@link #TRI}, {@link #LIN}, {@link #PNT}, {@link #TXT}, {@link #CRV}
	 * to set the order, e.g. {@code setRenderOrder(TRI, LIN, CRV, PNT, TXT);} to set
	 * the order {@link TrianglesRenderer} before {@link LinesRenderer} before {@link CurvesRenderer} 
	 * before {@link PointsRenderer} before {@link TextRenderer}.
	 * <br>
	 * Of course if one renderer is missing in this order or occurs multiple times,
	 * this renderer is not being processed (or processed multiple times respectively).
	 * 
	 * @param first one of {0,1,2,3,4} or {TRI,LIN,PNT,TXT,CRV}
	 * @param second one of {0,1,2,3,4} or {TRI,LIN,PNT,TXT,CRV}
	 * @param third one of {0,1,2,3,4} or {TRI,LIN,PNT,TXT,CRV}
	 * @param fourth one of {0,1,2,3,4} or {TRI,LIN,PNT,TXT,CRV}
	 * @param fifth one of {0,1,2,3,4} or {TRI,LIN,PNT,TXT,CRV}
	 * @return this for chaining
	 */
	@DebugSetter(key = "renderOrder", creator = RenderOrderCreator.class)
	public CompleteRenderer setRenderOrder(int first, int second, int third, int fourth, int fifth){
		renderOrder[0] = first;
		renderOrder[1] = second;
		renderOrder[2] = third;
		renderOrder[3] = fourth;
		renderOrder[4] = fifth;
		return this;
	}

	@DebugGetter(key = "renderOrder")
	public int[] getRenderOrder() {
		return renderOrder;
	}

	/**
	 * Sets the view matrix for each of the renderers
	 */
	@Override
	public void setView(Rectangle2D rect) {
		triangles.setView(rect);
		lines.setView(rect);
		points.setView(rect);
		text.setView(rect);
		curves.setView(rect);
	}

	/**
	 * GL initializes each of the renderers.
	 */
	@Override
	public void glInit() {
		triangles.glInit();
		lines.glInit();
		points.glInit();
		text.glInit();
		curves.glInit();
	}

	/**
	 * Renders according to the set render order.<br>
	 * (See {@link #setRenderOrder(int, int, int, int, int)})
	 */
	@Override
	public void render(int vpx, int vpy, int w, int h) {
		if(!isEnabled()){
			return;
		}
		rendererLUT[renderOrder[0]].render(vpx,vpy,w, h);
		rendererLUT[renderOrder[1]].render(vpx,vpy,w, h);
		rendererLUT[renderOrder[2]].render(vpx,vpy,w, h);
		rendererLUT[renderOrder[3]].render(vpx,vpy,w, h);
		rendererLUT[renderOrder[4]].render(vpx,vpy,w, h);
	}
	
	@Override
	public void renderFallback(Graphics2D g, Graphics2D p, int w, int h) {
		if(!isEnabled()){
			return;
		}
		rendererLUT[renderOrder[0]].renderFallback(g, p, w, h);
		rendererLUT[renderOrder[1]].renderFallback(g, p, w, h);
		rendererLUT[renderOrder[2]].renderFallback(g, p, w, h);
		rendererLUT[renderOrder[3]].renderFallback(g, p, w, h);
		rendererLUT[renderOrder[4]].renderFallback(g, p, w, h);
	}

	/**
	 * Closes each of the renderers.
	 */
	@Override
	public void close() {
		triangles.close();
		lines.close();
		points.close();
		text.close();
		curves.close();
	}
	
	@Override
	public void setEnabled(boolean enable) {
		this.isEnabled = enable;
	}
	
	@Override
	public boolean isEnabled() {
		return isEnabled;
	}
	
	/**
	 * Convenience method for enabling/disabling GL double precision rendering.
	 * This calls {@link GenericRenderer#setGLDoublePrecisionEnabled(boolean)}
	 * for each base {@link Renderer} of this CompleteRenderer.
	 */
	@Override
	public void setGLDoublePrecisionEnabled(boolean enable) {
		for(Renderer r : rendererLUT) {
			if(r instanceof GLDoublePrecisionSupport)
				((GLDoublePrecisionSupport)r).setGLDoublePrecisionEnabled(enable);
		}
	}
	
	/**
	 * Adds the specified item to the corresponding renderer.
	 * Only instances of {@link Triangles}, {@link Lines}, {@link Points} and {@link Text}
	 * are accepted, other item types result in an {@link IllegalArgumentException}. 
	 * @param item to add
	 * @return this for chaining
	 * @throws IllegalArgumentException when unsupported type of item is specified.
	 */
	public CompleteRenderer addItemToRender(Renderable item){
		if(item instanceof Triangles){
			triangles.addItemToRender((Triangles) item);
		} else 
		if(item instanceof Lines){
			lines.addItemToRender((Lines) item);
		} else 
		if(item instanceof Points){
			points.addItemToRender((Points) item);
		} else
		if(item instanceof Text){
			text.addItemToRender((Text) item);
		} else
		if(item instanceof Curves){
			curves.addItemToRender((Curves) item);
		} else {
			throw new IllegalArgumentException(
					"Cannot add Renderable of type " 
					+ item.getClass().getSimpleName()
					+ ". This type is not supported by this renderer."
			);
		}
		return this;
	}
	
	@Override
	public void renderSVG(Document doc, Element parent, int w, int h) {
		if(!isEnabled()){
			return;
		}
		rendererLUT[renderOrder[0]].renderSVG(doc, parent, w, h);
		rendererLUT[renderOrder[1]].renderSVG(doc, parent, w, h);
		rendererLUT[renderOrder[2]].renderSVG(doc, parent, w, h);
		rendererLUT[renderOrder[3]].renderSVG(doc, parent, w, h);
		rendererLUT[renderOrder[4]].renderSVG(doc, parent, w, h);
	}

	@Override
	public void renderPDF(PDDocument doc, PDPage page, int x, int y, int w, int h) {
		if(!isEnabled()){
			return;
		}
		rendererLUT[renderOrder[0]].renderPDF(doc, page, x, y, w, h);
		rendererLUT[renderOrder[1]].renderPDF(doc, page, x, y, w, h);
		rendererLUT[renderOrder[2]].renderPDF(doc, page, x, y, w, h);
		rendererLUT[renderOrder[3]].renderPDF(doc, page, x, y, w, h);
		rendererLUT[renderOrder[4]].renderPDF(doc, page, x, y, w, h);
	}
}
