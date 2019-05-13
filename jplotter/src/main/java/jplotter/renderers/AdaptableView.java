package jplotter.renderers;

import org.joml.Matrix3fc;

public interface AdaptableView {

	public void setViewMX(Matrix3fc viewmx, Matrix3fc scalemx, Matrix3fc transmx);
	
}
