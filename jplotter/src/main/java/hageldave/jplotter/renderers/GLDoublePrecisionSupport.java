package hageldave.jplotter.renderers;

/**
 * Interface for {@link Renderer}s that may support double precision GL rendering.
 * @author hageldave
 */
public interface GLDoublePrecisionSupport {

	/**
	 * Enables/Disables GL double precision rendering.
	 * @param enable true when enabling
	 */
	public void setGLDoublePrecisionEnabled(boolean enable);
	
}
