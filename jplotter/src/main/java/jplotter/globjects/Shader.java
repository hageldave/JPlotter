package jplotter.globjects;

import java.io.PrintStream;
import java.util.Objects;

import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL32.*;

public class Shader implements AutoCloseable {
	
	int vertexShaderID;
	int geometryShaderID;
	int fragmentShaderID;
	int shaderProgID;
	
	public Shader(CharSequence vertsh_src, CharSequence geomsh_ssrc, CharSequence fragsh_src){
		vertexShaderID = glCreateShader(GL_VERTEX_SHADER);
		{
			glShaderSource(vertexShaderID, vertsh_src);
			glCompileShader(vertexShaderID);
			int compileStatus = glGetShaderi(vertexShaderID, GL_COMPILE_STATUS);
			String shaderInfoLog = glGetShaderInfoLog(vertexShaderID);
			printInfoLogAndShader(compileStatus == 0 ? System.err:System.out, shaderInfoLog, vertsh_src);
		}
		fragmentShaderID = glCreateShader(GL_FRAGMENT_SHADER);
		{
			glShaderSource(fragmentShaderID, fragsh_src);
			glCompileShader(fragmentShaderID);
			int compileStatus = glGetShaderi(fragmentShaderID, GL_COMPILE_STATUS);
			String shaderInfoLog = glGetShaderInfoLog(fragmentShaderID);
			printInfoLogAndShader(compileStatus == 0 ? System.err:System.out, shaderInfoLog, fragsh_src);
		}
		geometryShaderID = Objects.isNull(geomsh_ssrc) ? 0:glCreateShader(GL_GEOMETRY_SHADER);
		if(geometryShaderID != 0){
			glShaderSource(geometryShaderID, geomsh_ssrc);
			glCompileShader(geometryShaderID);
			int compileStatus = glGetShaderi(geometryShaderID, GL_COMPILE_STATUS);
			String shaderInfoLog = glGetShaderInfoLog(geometryShaderID);
			printInfoLogAndShader(compileStatus == 0 ? System.err:System.out, shaderInfoLog, geomsh_ssrc);
		}
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
			printInfoLogAndShader(linkStatus == 0 ? System.err:System.out, programInfoLog, "");
		}
	}
	
	public Shader(CharSequence vertsh_src, CharSequence fragsh_src){
		this(vertsh_src, null, fragsh_src);
	}
	
	public void bind() {
		glUseProgram(shaderProgID);
	}
	
	public void unbind() {
		glUseProgram(0);
	}
	
	public int getShaderProgID() {
		return shaderProgID;
	}
	
	public int getFragmentShaderID() {
		return fragmentShaderID;
	}
	
	public int getGeometryShaderID() {
		return geometryShaderID;
	}
	
	public int getVertexShaderID() {
		return vertexShaderID;
	}
	
	@Override
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
