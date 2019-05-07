package jplotter.globjects;

public interface Renderable extends AutoCloseable {

	public void close();
	
	/** should return early if already initialized */
	public void initGL();
	
	public void updateGL();
	
	/** indicates that {@link #updateGL()} has to be called */
	public boolean isDirty();
	
}
