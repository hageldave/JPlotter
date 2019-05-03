package jplotter.renderers;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.URL;

import jplotter.util.GLUtils;

public class QuadWithFrag implements Renderer {
	
	private static final float[] vertices = new float[]{
			0,0,
			1,0,
			0,1,
			1,1,
	};
	private static final float[] orthoMX = GLUtils.orthoMX(0, 1, 0, 1);
	private static final char NL = '\n';
	public static final String vertexShaderSrc = ""
			+ "" + "#version 330"
			+ NL + "layout(location = 0) in vec2 in_position;"
			+ NL + "uniform mat4 projMX;"
			+ NL + "out vec2 tex_Coords;"
			+ NL + "void main() {"
			+ NL + "   gl_Position = projMX*vec4(in_position,0,1);"
			+ NL + "   tex_Coords = in_position;"
			+ NL + "}"
			+ NL
			;
	public static final String fragmentShaderEnvStubSrc = ""
			+ "" + "#version 330"
			+ NL + "out vec4 frag_Color;"
			+ NL + "in vec2 tex_Coords;"
			+ NL + "uniform float dt_seconds;"
			+ NL + "uniform float time_seconds;"
			+ NL + "uniform float aspect;"
			+ NL
			;
	private static final String fragmentShaderMainSrc = ""
			+ "" + "void main() {"
			+ NL + "   float sn = 0.5*(sin(time_seconds)+1);"
			+ NL + "   float cs = 0.5*(cos(time_seconds)+1);"
			+ NL + "   float x=tex_Coords.x;"
			+ NL + "   float y=tex_Coords.y;"
			+ NL + "   frag_Color = vec4(x*cs*(1-sn), y*sn*(1-x), cs*(1-y)*(1-x), 1);"
			+ NL + "}"
			+ NL
			;
	
	// GL handles
	private int va;
	private int vbo;
	private int vsh;
	private int fsh;
	protected int prg;
	
	// source code of fragement shader
	protected CharSequence fragSrc;
	
	// time stuffW
	private float dtSeconds;
	private float timeSeconds;
	private long createTime;
	// aspect ratio of viewport
	private float aspect;
	
	public QuadWithFrag() {
		this(fragmentShaderEnvStubSrc+fragmentShaderMainSrc);
	}
	
	public QuadWithFrag(CharSequence fragSrc) {
		this.fragSrc = fragSrc;
		this.createTime = System.currentTimeMillis();
	}
	
	public QuadWithFrag(InputStream fragSrc){
		this(stream2chars(fragSrc));
	}
	
	public QuadWithFrag(URL fragSrc){
		try(
				InputStream in = fragSrc.openStream();
		){
			this.fragSrc = stream2chars(in);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public QuadWithFrag(File fragSrc){
		try(
				InputStream in = fragSrc.toURI().toURL().openStream();
		){
			this.fragSrc = stream2chars(in);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	static CharSequence stream2chars(InputStream in){
		StringBuilder sb = new StringBuilder();
		try(
				InputStreamReader isr = new InputStreamReader(in);
				BufferedReader br = new BufferedReader(isr);  
		){
			String line;
			while((line=br.readLine()) != null){
				sb.append(line).append('\n');
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return sb;
	}
	
	@Override
	public void glInit() {
		createVertexArray();
		createShaders();
	}
	
	public void createVertexArray() {
		va = glGenVertexArrays();
		vbo = glGenBuffers();
		glBindVertexArray(va);
		{
			glBindBuffer(GL_ARRAY_BUFFER, vbo);
			{
				// put vertices into vbo
				glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);
				// put vbo into va
				glEnableVertexAttribArray(0);
				glVertexAttribPointer(0, 2, GL_FLOAT, false, 0, 0);
				glDisableVertexAttribArray(0);
			}
			glBindBuffer(GL_ARRAY_BUFFER, 0);
		}
		glBindVertexArray(0);
	}
	
	public void createShaders(){
		vsh = glCreateShader(GL_VERTEX_SHADER);
		{
			System.out.println("compiling vertex shader...");
			glShaderSource(vsh, vertexShaderSrc);
			glCompileShader(vsh);
			int compileStatus = glGetShaderi(vsh, GL_COMPILE_STATUS);
			String shaderInfoLog = glGetShaderInfoLog(vsh);
			printInfoLogAndShader(compileStatus == 0 ? System.err:System.out, shaderInfoLog, vertexShaderSrc);
		}
		fsh = glCreateShader(GL_FRAGMENT_SHADER);
		{
			System.out.println("compiling fragment shader...");
			glShaderSource(fsh, fragSrc);
			glCompileShader(fsh);
			int compileStatus = glGetShaderi(fsh, GL_COMPILE_STATUS);
			String shaderInfoLog = glGetShaderInfoLog(fsh);
			printInfoLogAndShader(compileStatus == 0 ? System.err:System.out, shaderInfoLog, fragSrc);
		}
		prg = glCreateProgram();
		{
			System.out.println("creating shader program");
			glAttachShader(prg, vsh);
			glAttachShader(prg, fsh);
			glLinkProgram(prg);
			int linkStatus = glGetProgrami(prg, GL_LINK_STATUS);
			String programInfoLog = glGetProgramInfoLog(prg);
			printInfoLogAndShader(linkStatus == 0 ? System.err:System.out, programInfoLog, "");
		}
	}
	
	static void printInfoLogAndShader(PrintStream ps, String infolog, CharSequence shader){
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
	
	@Override
	public void render(int w, int h) {
		render(0,(System.currentTimeMillis()-createTime)/1000f, w*1f/h);
	}

	public void render(float dtSeconds, float timeSeconds, float aspect) {
		this.dtSeconds = dtSeconds;
		this.timeSeconds = timeSeconds;
		this.aspect = aspect;
		glUseProgram(prg);
		{
			bindShaderVariables();
			glDrawArrays( GL_TRIANGLE_STRIP, 0, 4 );
			releaseShaderVariables();
		}
		glUseProgram(0);
	}
	
	public void bindShaderVariables(){
		glBindVertexArray(va);
		glEnableVertexAttribArray(0);
		int loc = glGetUniformLocation(prg, "projMX");
		glUniformMatrix4fv(loc, false, orthoMX);
		loc = glGetUniformLocation(prg, "dt_seconds");
		glUniform1f(loc, dtSeconds);
		loc = glGetUniformLocation(prg, "time_seconds");
		glUniform1f(loc, timeSeconds);
		loc = glGetUniformLocation(prg, "aspect");
		glUniform1f(loc, aspect);
	}
	
	public void releaseShaderVariables(){
		glDisableVertexAttribArray(0);
		glBindVertexArray(0);
	}
	
	@Override
	public void close() {
		deleteShaders();
		deleteVertexArray();
	}
	
	protected void deleteShaders(){
		glDeleteShader(vsh); vsh = 0;
		glDeleteShader(fsh); fsh = 0;
		glDeleteProgram(prg); prg = 0;
	}
	
	protected void deleteVertexArray() {
		glDeleteBuffers(vbo); vbo = 0;
		glDeleteVertexArrays(va); va = 0;
	}
	
}