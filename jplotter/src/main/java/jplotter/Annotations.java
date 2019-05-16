package jplotter;

import static java.lang.annotation.ElementType.METHOD;

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
	@Target(METHOD)
	public @interface GLContextRequired {}
}
