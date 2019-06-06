package hageldave.jplotter.renderers;

import org.joml.Matrix3fc;

import hageldave.jplotter.Annotations.GLContextRequired;

public class ChainedRenderer implements Renderer, AdaptableView {

	protected Renderer r1,r2;
	
	public ChainedRenderer(Renderer r1, Renderer r2) {
		this.r1 = r1;
		this.r2 = r2;
	}
	
	@Override
	public void setViewMX(Matrix3fc viewmx, Matrix3fc scalemx, Matrix3fc transmx) {
		if(r1 instanceof AdaptableView)
			((AdaptableView) r1).setViewMX(viewmx, scalemx, transmx);
		if(r2 instanceof AdaptableView)
			((AdaptableView) r2).setViewMX(viewmx, scalemx, transmx);
	}

	@Override
	@GLContextRequired
	public void glInit() {
		r1.glInit();
		r2.glInit();
	}

	@Override
	@GLContextRequired
	public void render(int w, int h) {
		r1.render(w, h);
		r2.render(w, h);
	}

	@Override
	@GLContextRequired
	public void close() {
		if(r1 != null)
			r1.close();
		if(r2 != null)
			r2.close();
		r1=r2=null;
	}
	
	
}
