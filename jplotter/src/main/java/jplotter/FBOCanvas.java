package jplotter;

import java.awt.AWTException;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.lang.reflect.Field;
import java.util.Objects;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.awt.AWTGLCanvas;
import org.lwjgl.opengl.awt.GLData;
import org.lwjgl.opengl.awt.PlatformGLCanvas;

import jplotter.globjects.FBO;
import jplotter.globjects.Shader;
import jplotter.globjects.VertexArray;
import jplotter.util.CapabilitiesCreator;
import jplotter.util.GLUtils;
import jplotter.util.Utils;

/**
 * The FBOCanvas is an {@link AWTGLCanvas} which uses a FrameBufferObject ({@link FBO}) for off screen rendering
 * which enables picking by utilizing a second color attachment in the FBO.
 * @author hageldave
 */
public abstract class FBOCanvas extends AWTGLCanvas implements AutoCloseable {
	private static final long serialVersionUID = 1L;
	
	private static final char NL = '\n';
	private static final String vertexShaderSrc = ""
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
	
	private static final String fragmentShaderSrc = ""
			+ "" + "#version 330"
			+ NL + "layout(location = 0) out vec4 frag_color;"
			+ NL + "uniform sampler2D tex;"
			+ NL + "in vec2 tex_Coords;"
			+ NL + "void main() {"
			+ NL + "   vec4 texColor = texture(tex, tex_Coords);"
			+ NL + "   frag_color = texColor;"
			+ NL + "}"
			;
	
	FBO fbo=null;
	Shader shader=null;
	VertexArray vertexArray=null;
	float[] orthoMX = GLUtils.orthoMX(0, 1, 0, 1);
	
	PlatformGLCanvas platformcanvas;
	
	public FBOCanvas(GLData data){
		super(data);
		this.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				render();
			}
		});
	}
	
	public FBOCanvas() {
		super();
		this.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				render();
			}
		});
	}
	
	
	@Override
	public void initGL() {
		CapabilitiesCreator.create();
		this.shader = new Shader(vertexShaderSrc, fragmentShaderSrc);
		this.vertexArray = new VertexArray(1);
		this.vertexArray.setBuffer(0, 2, new float[]{
				0,0,
				1,0,
				0,1,
				1,1
		});
	}
	
	public int getPixel(int x, int y, boolean picking){
		int attachment = picking ? GL30.GL_COLOR_ATTACHMENT1:GL30.GL_COLOR_ATTACHMENT0;
		int[] color = new int[1];
		Runnable fetchPixel = new Runnable() {
			@Override
			public void run() {
				PlatformGLCanvas platformcanvas=FBOCanvas.this.getPlatformCanvas();
				if(Objects.isNull(platformcanvas)){
					return;
				}
				try {
					platformcanvas.lock();
					platformcanvas.makeCurrent(FBOCanvas.this.context);
					color[0] = GLUtils.fetchPixel(fbo.getFBOid(), attachment, x, fbo.height-1-y);
				} catch (IllegalArgumentException | SecurityException | AWTException e) {
					e.printStackTrace();
				} finally {
					if(Objects.nonNull(platformcanvas)){
						try {
							platformcanvas.makeCurrent(0L);
							platformcanvas.unlock();
						} catch (AWTException e) {
						}
					}
				}
			}
		};
		Utils.execOnAWTEventDispatch(fetchPixel);
		return color[0];
	}
	

	protected PlatformGLCanvas getPlatformCanvas() {
		if(Objects.isNull(platformcanvas)){
			try {
				Field declaredField = AWTGLCanvas.class.getDeclaredField("platformCanvas");
				declaredField.setAccessible(true);
				platformcanvas = (PlatformGLCanvas) declaredField.get(FBOCanvas.this);
			} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
				e.printStackTrace();
			}
		}
		return platformcanvas;
	}

	@Override
	public void paintGL(){
		int w,h;
		if((w=getWidth()) >0 && (h=getHeight()) >0){
			if(fbo == null || w!=fbo.width || h!=fbo.height){
				setFBO(new FBO(w, h));
			}
			// offscreen
			setRenderTargetsColorOnly(w,h);
			GL11.glClearColor( 1.0f, 0.0f, 0.0f, 1.0f );
			GL11.glClear( GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT );
			paintToFBO(w, h);
			// onscreen
			setRenderTargets(0, w, h, GL11.GL_BACK);
			GL11.glClearColor( 0.0f, 0.0f, 1.0f, 1.0f );
			GL11.glClear( GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT );
//			shader.bind();
//			{
//				vertexArray.bindAndEnableAttributes(0);
//				int loc;
//				// set texture in shader
//				GL13.glActiveTexture(GL13.GL_TEXTURE0);
//				GL13.glBindTexture(GL11.GL_TEXTURE_2D, fbo.getMainColorTexId());
//				loc = GL20.glGetUniformLocation(shader.getShaderProgID(), "tex");
//				GL20.glUniform1i(loc, 0);
//				// set projection matrix in shader
//				loc = GL20.glGetUniformLocation(shader.getShaderProgID(), "projMX");
//				GL20.glUniformMatrix4fv(loc, false, orthoMX);
//				// draw things
//				GL11.glDrawArrays(GL11.GL_TRIANGLE_STRIP, 0, 4);
//				// done
//				vertexArray.unbindAndDisableAttributes(0);
//			}
//			shader.unbind();
			GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, fbo.getFBOid());
			GL30.glBlitFramebuffer(0, 0, w, h, 0, 0, w, h, GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
			GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, 0);
		}
		this.swapBuffers();
	}
	
	public abstract void paintToFBO(int width, int height);
	
	
	
	@Override
	public void close() {
		setFBO(null);
		if(Objects.nonNull(shader)){
			shader.close();
			shader = null;
		}
		if(Objects.nonNull(vertexArray)){
			vertexArray.close();
			vertexArray = null;
		}
	}
	
	private void setFBO(FBO fbo){
		if(Objects.nonNull(this.fbo)){
			this.fbo.close();
		}
		this.fbo = fbo;
	}
	
	protected void setRenderTargets(int fboID, int width, int height, int... drawBuffers){
		GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fboID);
		if(drawBuffers.length > 1){
			GL30.glDrawBuffers(drawBuffers);
		} else {
			GL30.glDrawBuffer(drawBuffers[0]);
		}
		GL11.glViewport(0, 0, width, height);
	}
	
	protected void setRenderTargetsColorAndPicking(int width, int height) {
		setRenderTargets(this.fbo.getFBOid(), width, height, GL30.GL_COLOR_ATTACHMENT0, GL30.GL_COLOR_ATTACHMENT1);
	}
	
	protected void setRenderTargetsColorOnly(int width, int height) {
		setRenderTargets(this.fbo.getFBOid(), width, height, GL30.GL_COLOR_ATTACHMENT0);
	}
	
	protected void setRenderTargetsPickingOnly(int width, int height) {
		setRenderTargets(this.fbo.getFBOid(), width, height, GL30.GL_COLOR_ATTACHMENT1);
	}

}
