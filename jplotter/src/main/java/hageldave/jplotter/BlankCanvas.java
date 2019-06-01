package hageldave.jplotter;

import java.awt.Color;
import java.util.Objects;

import hageldave.jplotter.Annotations.GLContextRequired;
import hageldave.jplotter.renderers.Renderer;

public class BlankCanvas extends FBOCanvas {
	private static final long serialVersionUID = 1L;

	protected Renderer renderer;

	public BlankCanvas() {
		super.fboClearColor = Color.white;
	}

	@Override
	@GLContextRequired
	protected void paintToFBO(int width, int height) {
		if(Objects.nonNull(renderer) && width > 0 && height > 0){
			renderer.glInit();
			renderer.render(width, height);
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
	
	public void setRenderer(Renderer renderer) {
		this.renderer = renderer;
	}

}
