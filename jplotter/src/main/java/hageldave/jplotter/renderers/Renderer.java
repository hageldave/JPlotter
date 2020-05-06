package hageldave.jplotter.renderers;

import hageldave.jplotter.gl.FBO;
import hageldave.jplotter.gl.Shader;
import hageldave.jplotter.renderables.Renderable;
import hageldave.jplotter.svg.SVGRenderer;
import hageldave.jplotter.util.Annotations.GLContextRequired;

/**
 * The Renderer interface defines methods to 
 * initialize the renderer,
 * execute a rendering pass,
 * close the renderer.
 * <p>
 * <b>Implementation Notice:</b><br>
 * If this renderer directly uses a shader,
 * its fragment shader is obliged to output color for two
 * buffers, which are the two color attachments of an {@link FBO}.
 * These have to be written to <br>
 * {@code layout(location=0) out vec4 c1;} and <br>
 * {@code layout(location=1) out vec4 c2;}.<br>
 * When the renderer has no use for the picking attachment, {@code vec4(0,0,0,0)}
 * can be written as default.
 * 
 * @author hageldave
 */
public interface Renderer extends AutoCloseable, SVGRenderer {

	/**
	 * Initializes this renderer, i.e. allocates GL resources such as a 
	 * {@link Shader} or the resources of {@link Renderable}s it's using.
	 * This method should return early when already been called before.
	 */
	@GLContextRequired
	public void glInit();
	
	/**
	 * renders this {@link Renderer}'s 'scene'.
	 * @param vpx x coordinate of the viewport's origin
	 * @param vpy y coordinate of the viewport's origin
	 * @param w width of the current viewport in pixels
	 * @param h height of the current viewport in pixels
	 */
	@GLContextRequired
	public void render(int vpx, int vpy, int w, int h);
	
	/**
	 * Disposes of any GL resources belonging to this object.
	 */
	@GLContextRequired
	public void close();
	
	/**
	 * En-/Disables this renderer. By default a renderer is enabled and will
	 * render upon {@link #render(int, int, int, int)} or {@link #renderSVG(org.w3c.dom.Document, org.w3c.dom.Element, int, int)}.
	 * When disabled those methods return right away and will not render anything.
	 * @param enable true when activating, false when deactivating.
	 */
	public void setEnabled(boolean enable);
	
	/**
	 * Whether this renderer is enabled or not. By default a renderer is enabled and will
	 * render upon {@link #render(int, int, int, int)} or {@link #renderSVG(org.w3c.dom.Document, org.w3c.dom.Element, int, int)}.
	 * When disabled those methods return right away and will not render anything.
	 * @return true when active
	 */
	public boolean isEnabled();
	
	/**
	 * Creates a {@link ChainedRenderer} with this as first and the
	 * specified as second renderer in sequence.
	 * @param r the renderer subsequent to this in the chain
	 * @return new ChainedRenderer with the specified renderer following this.
	 */
	public default ChainedRenderer withAppended(Renderer r){
		return new ChainedRenderer(this, r);
	}
	
	/**
	 * Creates a {@link ChainedRenderer} with the specified as first and this
	 * as second renderer in sequence.
	 * @param r the renderer preceding this in the chain
	 * @return new ChainedRenderer with the specified renderer preceding this.
	 */
	public default ChainedRenderer withPrepended(Renderer r){
		return new ChainedRenderer(r, this);
	}
	
}
