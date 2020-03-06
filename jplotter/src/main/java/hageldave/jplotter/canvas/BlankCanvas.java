package hageldave.jplotter.canvas;

import java.util.Objects;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import hageldave.jplotter.renderers.CoordSysRenderer;
import hageldave.jplotter.renderers.Renderer;
import hageldave.jplotter.util.Annotations.GLContextRequired;

/**
 * The BlankCanvas is an {@link FBOCanvas} that uses a single {@link Renderer} 
 * to draw its contents.
 * <p>
 * Usually a {@link CoordSysRenderer} will be used in conjunction with this canvas to display plots.
 * 
 * @author hageldave
 */
public class BlankCanvas extends FBOCanvas {
	private static final long serialVersionUID = 1L;

	protected Renderer renderer;
	
	/**
	 * Creates a new {@link BlankCanvas} with the specified {@link FBOCanvas}
	 * as it's GL context sharing parent.
	 * When sharing GL context both canvases can use the same GL textures and buffers
	 * which saves memory and may also improve performance.
	 * @param parent to share GL context with
	 */
	public BlankCanvas(FBOCanvas parent) {
		super(parent);
	}
	
	/**
	 * Creates a new {@link BlankCanvas}.
	 */
	public BlankCanvas() {
		this(null);
	}

	@Override
	@GLContextRequired
	protected void paintToFBO(int width, int height) {
		if(Objects.nonNull(renderer) && width > 0 && height > 0){
			renderer.glInit();
			renderer.render(0, 0, width, height);
		}
	}
	
	@Override
	protected void paintToSVG(Document doc, Element parent, int w, int h) {
		renderer.renderSVG(doc, parent, w, h);
	}

	@Override
	@GLContextRequired
	public void initGL() {
		super.initGL();
		if(Objects.nonNull(renderer))
			renderer.glInit();
	}

	@Override
	@GLContextRequired
	public void close() {
		super.close();
		if(Objects.nonNull(renderer))
			renderer.close();
		renderer = null;
	}
	
	/**
	 * Sets the renderer of this canvas.
	 * @param renderer to draw contents.
	 * @return this for chaining
	 */
	public BlankCanvas setRenderer(Renderer renderer) {
		this.renderer = renderer;
		return this;
	}

}
