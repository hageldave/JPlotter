package hageldave.jplotter.renderers;

import org.joml.Matrix3fc;

/**
 * The AdaptableView interface defines the {@link #setViewMX(Matrix3fc, Matrix3fc, Matrix3fc)}
 * method.
 * An implementing class of this interface is meant to be able to adjust its
 * view based on the view matrix passed to the method.
 * This is implemented for example by {@link GenericRenderer}.
 * 
 * @author hageldave
 */
public interface AdaptableView {

	/**
	 * Sets the view matrix, that is the matrix that translates and scales
	 * the 2D space in order to bring a specific 2D region into view.
	 * The view matrix is the product of scaling and translation
	 * {@code scaleMX * transMX} (translation before scaling).
	 * These two matrices are as well passed in this method in order for
	 * translation or scale invariant views to pick.
	 * @param viewmx the view matrix to be set
	 * @param scalemx the scaling matrix (viewMX=scaleMX*transMX)
	 * @param transmx the translation matrix (viewMX=scaleMX*transMX)
	 */
	public void setViewMX(Matrix3fc viewmx, Matrix3fc scalemx, Matrix3fc transmx);
	
}
