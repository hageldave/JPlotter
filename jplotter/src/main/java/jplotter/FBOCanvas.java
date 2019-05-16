package jplotter;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.lang.reflect.Field;
import java.util.Objects;

import javax.swing.SwingUtilities;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL32;
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
	private static final String blitVertexShaderSrc = ""
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
	private static final String blitFragmentShaderSrc = ""
			+ "" + "#version 330"
			+ NL + "layout(location = 0) out vec4 frag_color;"
			+ NL + "layout(location = 1) out vec4 frag_pick;"
			+ NL + "uniform sampler2DMS colorTex;"
			+ NL + "uniform sampler2DMS pickTex;"
			+ NL + "uniform int numSamples;"
			+ NL + "uniform vec2 screensize;"
			+ NL + "in vec2 tex_Coords;"
			+ NL + "void main() {"
			+ NL + "   vec2 scaled = tex_Coords*screensize;"
			+ NL + "   ivec2 screen_Coords = ivec2(int(scaled.x),int(scaled.y));"
			+ NL + "   float blitFactor = 1.0f/numSamples;"
			+ NL + "   vec4 color = vec4(0,0,0,0);"
			+ NL + "   for(int s=0; s<numSamples; s++){"
			+ NL + "      color = color + texelFetch(colorTex, screen_Coords, s)*blitFactor;"
			+ NL + "   }"
			+ NL + "   vec4 pick = texelFetch(pickTex, screen_Coords, 0);"
			+ NL + "   frag_color = color;"
			+ NL + "   frag_pick  = pick;"
			+ NL + "}"
			;

	private static final String fillVertexShaderSrc = ""
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
	private static final String fillFragmentShaderSrc = ""
			+ "" + "#version 330"
			+ NL + "layout(location = 0) out vec4 frag_color;"
			+ NL + "layout(location = 1) out vec4 frag_pick;"
			+ NL + "uniform vec4 colorFill;"
			+ NL + "uniform vec4 pickFill;"
			+ NL + "in vec2 tex_Coords;"
			+ NL + "void main() {"
			+ NL + "   frag_color = colorFill;"
			+ NL + "   frag_pick  = pickFill;"
			+ NL + "}"
			;



	protected FBO fbo=null;
	protected FBO fboMS=null;
	protected Shader blitShader=null;
	protected Shader fillShader=null;
	protected VertexArray vertexArray=null;
	protected float[] orthoMX = GLUtils.orthoMX(null,0, 1, 0, 1);
	protected PlatformGLCanvas platformcanvas;
	protected boolean useBitBlit = false;
	protected Color fboClearColor = Color.darkGray;
	protected Color screenClearColor = Color.BLACK;

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
		this.blitShader = new Shader(blitVertexShaderSrc, blitFragmentShaderSrc);
		this.fillShader = new Shader(fillVertexShaderSrc, fillFragmentShaderSrc);
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
				setFBO(new FBO(w, h, false));
				if(GLUtils.canMultisample2X()){
					setFBO_MS(new FBO(w, h, true));
				}
			}
			// offscreen
			GL11.glClearColor(0, 0, 0, 0);
			setRenderTargetsColorAndPicking(w, h);
			GL11.glClear( GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT );
			/* we need to first draw a viewport filling quad that fills the buffer with the clear color
			 * in order to resolve issue #4
			 */
			{
				fillShader.bind();
				int loc;
				loc = GL20.glGetUniformLocation(fillShader.getShaderProgID(), "colorFill");
				GL20.glUniform4f(loc, fboClearColor.getRed()/255f, fboClearColor.getGreen()/255f, fboClearColor.getBlue()/255f, fboClearColor.getAlpha()/255f);
				loc = GL20.glGetUniformLocation(fillShader.getShaderProgID(), "pickFill");
				GL20.glUniform4f(loc, 0,0,0,0);
				loc = GL20.glGetUniformLocation(fillShader.getShaderProgID(), "projMX");
				GL20.glUniformMatrix4fv(loc, false, orthoMX);
				vertexArray.bindAndEnableAttributes(0);
				GL11.glDrawArrays(GL11.GL_TRIANGLE_STRIP, 0, 4);
				vertexArray.unbindAndDisableAttributes(0);
				fillShader.unbind();
			}
			// now draw the other stuff to the fbo
			paintToFBO(w, h);

			/* if we're using a multisampled FBO manually (=with a shader) blit to normal FBO
			 * we need to do this manually because the picking colors are not to be anti aliased
			 */
			if(Objects.nonNull(fboMS)){
				setRenderTargets(fbo.getFBOid(), w,h, GL30.GL_COLOR_ATTACHMENT0, GL30.GL_COLOR_ATTACHMENT1);
				GL11.glClear( GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT );
				{
					blitShader.bind();
					vertexArray.bindAndEnableAttributes(0);
					int loc;
					// set texture in shader
					GL13.glActiveTexture(GL13.GL_TEXTURE0);
					GL13.glBindTexture(GL32.GL_TEXTURE_2D_MULTISAMPLE, fboMS.getMainColorTexId());
					loc = GL20.glGetUniformLocation(blitShader.getShaderProgID(), "colorTex");
					GL20.glUniform1i(loc, 0);

					GL13.glActiveTexture(GL13.GL_TEXTURE1);
					GL13.glBindTexture(GL32.GL_TEXTURE_2D_MULTISAMPLE, fboMS.getPickingColorTexId());
					loc = GL20.glGetUniformLocation(blitShader.getShaderProgID(), "pickTex");
					GL20.glUniform1i(loc, 1);

					loc = GL20.glGetUniformLocation(blitShader.getShaderProgID(), "screensize");
					GL20.glUniform2f(loc, w, h);

					loc = GL20.glGetUniformLocation(blitShader.getShaderProgID(), "numSamples");
					GL20.glUniform1i(loc, fboMS.numMultisamples);

					loc = GL20.glGetUniformLocation(blitShader.getShaderProgID(), "projMX");
					GL20.glUniformMatrix4fv(loc, false, orthoMX);

					GL11.glDrawArrays(GL11.GL_TRIANGLE_STRIP, 0, 4);
					// done
					vertexArray.unbindAndDisableAttributes(0);
					blitShader.unbind();
				}
			}

			// onscreen
			setRenderTargets(0, w, h, GL11.GL_BACK);
			GL11.glClearColor( screenClearColor.getRed()/255f, screenClearColor.getGreen()/255f, screenClearColor.getBlue()/255f, screenClearColor.getAlpha()/255f );
			GL11.glClear( GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT );
			{
				GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, fbo.getFBOid());
				GL30.glReadBuffer(GL30.GL_COLOR_ATTACHMENT0);
				GL30.glBlitFramebuffer(0, 0, w, h, 0, 0, w, h, GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
				GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, 0);
			}
		}
		this.swapBuffers();
	}

	public abstract void paintToFBO(int width, int height);



	@Override
	public void close() {
		setFBO(null);
		setFBO_MS(null);
		if(Objects.nonNull(blitShader)){
			blitShader.close();
			blitShader = null;
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

	private void setFBO_MS(FBO fboMS){
		if(Objects.nonNull(this.fboMS)){
			this.fboMS.close();
		}
		this.fboMS = fboMS;
	}

	protected static void setRenderTargets(int fboID, int width, int height, int... drawBuffers){
		GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fboID);
		if(drawBuffers.length > 1){
			GL30.glDrawBuffers(drawBuffers);
		} else {
			GL30.glDrawBuffer(drawBuffers[0]);
		}
		GL11.glViewport(0, 0, width, height);
	}

	protected void setRenderTargetsColorAndPicking(int width, int height) {
		int fboIDToUse = Objects.nonNull(this.fboMS) ? this.fboMS.getFBOid() : this.fbo.getFBOid();
		setRenderTargets(fboIDToUse, width, height, GL30.GL_COLOR_ATTACHMENT0, GL30.GL_COLOR_ATTACHMENT1);
	}

	protected void setRenderTargetsColorOnly(int width, int height) {
		int fboIDToUse = Objects.nonNull(this.fboMS) ? this.fboMS.getFBOid() : this.fbo.getFBOid();
		setRenderTargets(fboIDToUse, width, height, GL30.GL_COLOR_ATTACHMENT0);
	}

	protected void setRenderTargetsPickingOnly(int width, int height) {
		int fboIDToUse = Objects.nonNull(this.fboMS) ? this.fboMS.getFBOid() : this.fbo.getFBOid();
		setRenderTargets(fboIDToUse, width, height, GL30.GL_COLOR_ATTACHMENT1);
	}
	
	@Override
	public void repaint() {
		SwingUtilities.invokeLater(()->render());
	}
	
	@Override
	public void paint(Graphics g) {
		// don't modify the graphics object
	}
	@Override
	public void paintAll(Graphics g) {
		// don't modify the graphics object
	}
	
	@Override
	public void update(Graphics g) {
		// don't modify the graphics object
	}
	
}
