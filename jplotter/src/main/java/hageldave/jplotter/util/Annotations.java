package hageldave.jplotter.util;

import static java.lang.annotation.ElementType.*;

import java.lang.annotation.Documented;
import java.lang.annotation.Target;

import org.lwjgl.opengl.awt.AWTGLCanvas;

public final class Annotations {
	private Annotations(){}
	
	/**
	 * This annotation serves as a label to signalize developers that the
	 * annotated method is to be called in a thread that currently owns an
	 * OpenGL context.
	 * Such context is present for example during 
	 * {@link AWTGLCanvas#render()},
	 * {@link AWTGLCanvas#runInContext(Runnable)} or 
	 * {@link AWTGLCanvas#executeInContext(java.util.concurrent.Callable)}.
	 * Of course if the calling thread does not own a GL context the latter
	 * two methods may be used if possible. It is recommended though
	 * to avoid this due to the overhead required to obtain a GL context.
	 * 
	 * @author hageldave
	 */
	@Documented
	@Target({METHOD,CONSTRUCTOR})
	public @interface GLContextRequired {}
	
	/**
	 * This annotation serves as a label to signalize developers that the
	 * annotated field, type or parameter as well as return value of a method 
	 * is in OpenGL coordinates and NOT in AWT coordinates.<br>
	 * This means that coordinates are relative to a coordinate system with origin
	 * in the bottom left corner of a view port and upwards pointing y axis as 
	 * opposed to AWT's top left corner origin and downwards pointing y axis.
	 * <p>
	 * The utility methods {@link Utils#swapYAxis(java.awt.geom.Point2D, int)}
	 * and {@link Utils#swapYAxis(java.awt.geom.Rectangle2D, int)} can be used
	 * to transform between the reference coordinate systems.
	 * 
	 * @author hageldave
	 */
	@Documented
	@Target({METHOD,FIELD,PARAMETER,TYPE})
	public @interface GLCoordinates {}
}
