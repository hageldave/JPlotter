package hageldave.jplotter.gl;

import java.io.PrintStream;
import java.util.Objects;

import hageldave.jplotter.util.GLUtils;
import hageldave.jplotter.util.Annotations.GLContextRequired;
import hageldave.jplotter.util.GLUtils.GLRuntimeException;

import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL32.*;

/**
 * The Shader class encapsulates GL shader objects and the corresponding GL program object.
 * In this implementation a shader program may consist of a vertex shader, optional geometry shader
 * and fragment shader. Tesselation shaders are not supported.
 * 
 * @author hageldave
 */
public class Shader implements AutoCloseable {
	public static boolean alwaysPrintInfoLogsAndShaders = false;
	
	int vertexShaderID;
	int geometryShaderID;
	int fragmentShaderID;
	int shaderProgID;
	
	/**
	 * Creates a Shader program that consists of a vertex, an optional geometry and 
	 * a fragment shader which are specified in passed {@link CharSequence}s.
	 * This will compile the shaders from their sources and link the shader 
	 * program afterwards.
	 * 
	 * @param vertsh_src source code of the vertex shader
	 * @param geomsh_src (optional) source code of the geometry shader, or null
	 * @param fragsh_src source code of the fragment shader
	 * 
	 * @throws GLRuntimeException if shader compilation or linking fails.
	 */
	@GLContextRequired
	public Shader(CharSequence vertsh_src, CharSequence geomsh_src, CharSequence fragsh_src)
	{
		vertexShaderID(vertsh_src);
		geometryShaderID(geomsh_src);
		fragmentShaderID(fragsh_src);
		shaderProgID(vertsh_src, geomsh_src, fragsh_src);
	}

	private void shaderProgID(CharSequence vertsh_src, CharSequence geomsh_src, CharSequence fragsh_src) {
		shaderProgID = glCreateProgram();
		{
			glAttachShader(shaderProgID, vertexShaderID);
			if(geometryShaderID != 0){
				glAttachShader(shaderProgID, geometryShaderID);
			}
			glAttachShader(shaderProgID, fragmentShaderID);
			glLinkProgram(shaderProgID);
			int linkStatus = glGetProgrami(shaderProgID, GL_LINK_STATUS);
			String programInfoLog = glGetProgramInfoLog(shaderProgID);
			if(linkStatus == 0){
				throw new GLRuntimeException(
						"GL Error: GL_LINK_STATUS = 0.\n"
						+ programInfoLog + '\n'
						+ vertsh_src + '\n'
						+ (Objects.nonNull(geomsh_src) ? geomsh_src :"") + '\n'
						+ fragsh_src
				);
			}
			if(alwaysPrintInfoLogsAndShaders)
				printInfoLogAndShader(System.out, programInfoLog, "");
		}
	}

	private void fragmentShaderID(CharSequence fragsh_src) {
		fragmentShaderID = glCreateShader(GL_FRAGMENT_SHADER);
		{
			glShaderSource(fragmentShaderID, fragsh_src);
			glCompileShader(fragmentShaderID);
			int compileStatus = glGetShaderi(fragmentShaderID, GL_COMPILE_STATUS);
			String shaderInfoLog = glGetShaderInfoLog(fragmentShaderID);
			if(compileStatus == 0){
				throw new GLRuntimeException(
						"GL Error: GL_COMPILE_STATUS = 0 for fragment shader.\n"
						+ shaderInfoLog + '\n' + fragsh_src
				);
			}
			if(alwaysPrintInfoLogsAndShaders)
				printInfoLogAndShader(System.out, shaderInfoLog, fragsh_src);
		}
	}

	private void geometryShaderID(CharSequence geomsh_src) {
		geometryShaderID = Objects.isNull(geomsh_src) ? 0:glCreateShader(GL_GEOMETRY_SHADER);
		if(geometryShaderID != 0){
			glShaderSource(geometryShaderID, geomsh_src);
			glCompileShader(geometryShaderID);
			int compileStatus = glGetShaderi(geometryShaderID, GL_COMPILE_STATUS);
			String shaderInfoLog = glGetShaderInfoLog(geometryShaderID);
			if(compileStatus == 0){
				throw new GLRuntimeException(
						"GL Error: GL_COMPILE_STATUS = 0 for geometry shader.\n"
						+ shaderInfoLog + '\n' + geomsh_src
				);
			}
			if(alwaysPrintInfoLogsAndShaders)
				printInfoLogAndShader(System.out, shaderInfoLog, geomsh_src);
		}
	}

	private void vertexShaderID(CharSequence vertsh_src) {
		vertexShaderID = glCreateShader(GL_VERTEX_SHADER);
		{
			glShaderSource(vertexShaderID, vertsh_src);
			glCompileShader(vertexShaderID);
			int compileStatus = glGetShaderi(vertexShaderID, GL_COMPILE_STATUS);
			String shaderInfoLog = glGetShaderInfoLog(vertexShaderID);
			if(compileStatus == 0){
				throw new GLRuntimeException(
						"GL Error: GL_COMPILE_STATUS = 0 for vertex shader.\n"
						+ shaderInfoLog + '\n' + vertsh_src
				);
			}
			if(alwaysPrintInfoLogsAndShaders)
				printInfoLogAndShader(System.out, shaderInfoLog, vertsh_src);
		}
	}

	/**
	 * Calls {@link Shader#Shader(CharSequence, CharSequence, CharSequence)} with null geometry shader source.
	 * @param vertsh_src source code of the vertex shader
	 * @param fragsh_src source code of the fragment shader
	 */
	@GLContextRequired
	public Shader(CharSequence vertsh_src, CharSequence fragsh_src){
		this(vertsh_src, null, fragsh_src);
	}
	
	/**
	 * Calls glUseProgram(this.shaderProgID) to enable this shader
	 */
	@GLContextRequired
	public void bind() {
		glUseProgram(shaderProgID);
	}
	
	/**
	 * Calls glUseProgram(0) to disable the shader
	 */
	@GLContextRequired
	public void release() {
		glUseProgram(0);
	}
	
	/**
	 * @return the GL object name of the shader program
	 */
	public int getShaderProgID() {
		return shaderProgID;
	}
	
	/**
	 * @return the GL object name of the fragment shader
	 */
	public int getFragmentShaderID() {
		return fragmentShaderID;
	}
	
	/**
	 * @return the GL object name of the geometry shader
	 */
	public int getGeometryShaderID() {
		return geometryShaderID;
	}
	
	/**
	 * @return the GL object name of the vertex shader
	 */
	public int getVertexShaderID() {
		return vertexShaderID;
	}
	
	/**
	 * Disposes of this shader's GL resources, i.e. deletes shaders and program.
	 */
	@Override
	@GLContextRequired
	public void close() {
		glDeleteShader(vertexShaderID);
		glDeleteShader(geometryShaderID);
		glDeleteShader(fragmentShaderID);
		glDeleteProgram(shaderProgID);
		shaderProgID = vertexShaderID = geometryShaderID = fragmentShaderID = 0;
	}
	
	private static void printInfoLogAndShader(PrintStream ps, String infolog, CharSequence shader){
		if(!infolog.isEmpty())
			ps.println(infolog);
		String shaderStr = shader.toString();
		if(!shaderStr.isEmpty()){
			int lineNum = 1;
			String[] lines = shaderStr.split("\n", -1);
			int lineNumPadding = (int)Math.log10(lines.length)+1;
			final String lineNumFormat = "%0"+lineNumPadding+"d: ";
			for(String l: lines){
				ps.format(lineNumFormat, lineNum++);
				ps.println(l);
			}
		}
	}

}
