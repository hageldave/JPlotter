package jplotter.renderers;

import org.joml.Matrix3fc;

public class CompleteRenderer implements Renderer, AdaptableView {
	
	public final LinesRenderer lines = new LinesRenderer();
	public final PointsRenderer points = new PointsRenderer();
	public final TextRenderer text = new TextRenderer();
	

	@Override
	public void setViewMX(Matrix3fc viewmx, Matrix3fc scalemx, Matrix3fc transmx) {
		lines.setViewMX(viewmx, scalemx, transmx);
		points.setViewMX(viewmx, scalemx, transmx);
		text.setViewMX(viewmx, scalemx, transmx);
	}

	@Override
	public void glInit() {
		lines.glInit();
		points.glInit();
		text.glInit();
	}

	@Override
	public void render(int w, int h) {
		lines.render(w, h);
		points.render(w, h);
		text.render(w, h);
	}

	@Override
	public void close() {
		lines.close();
		points.close();
		text.close();
	}

}
