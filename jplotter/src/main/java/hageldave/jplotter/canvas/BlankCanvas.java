package hageldave.jplotter.canvas;

import java.util.Objects;

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
public class BlankCanvas extends FBOCanvas implements JPlotterCanvas {
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
	
	@Override
	public BlankCanvas setRenderer(Renderer renderer) {
		this.renderer = renderer;
		return this;
	}
	
	@Override
	public Renderer getRenderer() {
		return renderer;
	}
	
	@Override
	public BlankCanvas asComponent() {
		return this;
	}

}
