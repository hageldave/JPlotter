package hageldave.jplotter.renderers;

import java.awt.geom.Rectangle2D;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import hageldave.jplotter.renderables.Lines;
import hageldave.jplotter.renderables.Points;
import hageldave.jplotter.renderables.Renderable;
import hageldave.jplotter.renderables.Text;
import hageldave.jplotter.renderables.Triangles;

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
 * <li>{@link Points}</li>
 * <li>{@link Text}</li>
 * </ol>
 * This implies that Lines will be drawn over Triangles,
 * Points over Lines and Text over Points.
 * Using the {@link #setRenderOrder(int, int, int, int)} method
 * the order of renderers can be changed.
 * <p>
 * To add {@link Renderable}s to this Renderer either use the public attributes
 * {@link #triangles}, {@link #lines}, {@link #points}, {@link #text} to directly
 * access the desired renderer or use the {@link #addItemToRender(Renderable)}
 * method.
 * 
 * @author hageldave
 */
public class CompleteRenderer implements Renderer, AdaptableView {
	
	public final LinesRenderer lines = new LinesRenderer();
	public final PointsRenderer points = new PointsRenderer();
	public final TextRenderer text = new TextRenderer();
	public final TrianglesRenderer triangles = new TrianglesRenderer();
	
	private final Renderer[] rendererLUT = {triangles,lines,points,text};
	public static final int TRI = 0, LIN = 1, PNT = 2, TXT = 3;
	private final int[] renderOrder = {TRI,LIN,PNT,TXT};
	
	/**
	 * Sets the order of the renderers. 
	 * You can use the constants {@link #TRI}, {@link #LIN}, {@link #PNT}, {@link #TXT}
	 * to set the order, e.g. {@code setRenderOrder(TRI, LIN, PNT, TXT);} to set
	 * the order {@link TrianglesRenderer} before {@link LinesRenderer} before {@link PointsRenderer}
	 * before {@link TextRenderer}.
	 * <br>
	 * Of course if one renderer is missing in this order or occurs multiple times,
	 * this renderer is not being processed (or processed multiple times respectively).
	 * 
	 * @param first one of {0,1,2,3} or {TRI,LIN,PNT,TXT}
	 * @param second one of {0,1,2,3} or {TRI,LIN,PNT,TXT}
	 * @param third one of {0,1,2,3} or {TRI,LIN,PNT,TXT}
	 * @param fourth one of {0,1,2,3} or {TRI,LIN,PNT,TXT}
	 * @return this for chaining
	 */
	public CompleteRenderer setRenderOrder(int first, int second, int third, int fourth){
		renderOrder[0] = first;
		renderOrder[1] = second;
		renderOrder[2] = third;
		renderOrder[3] = fourth;
		return this;
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
	}

	/**
	 * Renders according to the set render order.<br>
	 * (See {@link #setRenderOrder(int, int, int, int)})
	 */
	@Override
	public void render(int w, int h) {
		rendererLUT[renderOrder[0]].render(w, h);
		rendererLUT[renderOrder[1]].render(w, h);
		rendererLUT[renderOrder[2]].render(w, h);
		rendererLUT[renderOrder[3]].render(w, h);
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
		rendererLUT[renderOrder[0]].renderSVG(doc, parent, w, h);
		rendererLUT[renderOrder[1]].renderSVG(doc, parent, w, h);
		rendererLUT[renderOrder[2]].renderSVG(doc, parent, w, h);
		rendererLUT[renderOrder[3]].renderSVG(doc, parent, w, h);
	}

}
