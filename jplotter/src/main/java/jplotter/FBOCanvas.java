package jplotter;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.SwingUtilities;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL32;
import org.lwjgl.opengl.awt.AWTGLCanvas;
import org.lwjgl.opengl.awt.GLData;

import jplotter.Annotations.GLContextRequired;
import jplotter.globjects.FBO;
import jplotter.globjects.Shader;
import jplotter.globjects.VertexArray;
import jplotter.renderables.CharacterAtlas;
import jplotter.util.CapabilitiesCreator;
import jplotter.util.GLUtils;
import jplotter.util.Utils;

/**
 * The FBOCanvas is an {@link AWTGLCanvas} which uses a FrameBufferObject ({@link FBO}) 
 * for off screen rendering which enables picking by utilizing a second color attachment 
 * in the FBO.
 * The FBO's first color attachment is drawn (blit) to the onscreen framebuffer for display.
 * <p>
 * Picking is a technique for figuring out what primitive or object was rendered to which
 * position of the framebuffer. Every drawn object can be assigned a unique integer id (24 bits)
 * which is then translated into an RGB color value and used to render the object into 
 * the second color attachment of the FBO.
 * The second attachment is never shown on screen but can be read with a {@code glReadPixels()}
 * call.
 * This way the object id can be queried for a specific mouse location, allowing for easy
 * interaction.
 * <p>
 * If the system supports multisampled FBOs (and {@link #useMSAA} is true) this Canvas will use
 * a multisampled FBO on top of a regular FBO for anti aliasing (MSAA).
 * The reason for the regular FBO in between is that {@code glReadPixels()} is not supported by
 * multisampled FBOs, so in order for picking to work the contents have to be transferred to a 
 * regular FBO first.
 * This transfer can unfortunately not be done using a {@code glBlitFramebuffer} operation as the
 * anti aliasing would feather the edges of the picking color attachment as well and thus break
 * its functionality by altering the colors.
 * Instead a shader is used to draw the multisampled FBO's textures to the other FBO.
 * <p>
 * The actual rendering of contents is done in the {@link #paintToFBO(int, int)} method which
 * is abstract and has to be implemented by a subclass.
 * <p>
 * Since every AWTGLCanvas has its own GL context, all GL resources, such as textures or vertex arrays,
 * are only valid for the context they were created in.
 * Unfortunately there is no way of telling which resource belongs to which context, which is why
 * All instanced of {@link FBOCanvas} have a unique nonzero integer {@link #canvasID}.
 * This id will be set as the static class attribute {@link #CURRENTLY_ACTIVE_CANVAS} whenever
 * an {@link FBOCanvas} activates its GL context, when the canvas' GL context becomes inactive
 * the CURRENTLY_ACTIVE_CANVAS attribute is set back to zero.
 * By checking the value of this attribute we are able to know which {@link FBOCanvas}'
 * GL context is currently active.
 * <br><b> For this to work properly it is essential that all GL calls are happening on the AWT event dispatch
 * thread </b>(see https://docs.oracle.com/javase/tutorial/uiswing/concurrency/dispatch.html).
 * 
 * @author hageldave
 */
public abstract class FBOCanvas extends AWTGLCanvas implements AutoCloseable {
	private static final long serialVersionUID = 1L;
	
	public static final AtomicInteger ATOMIC_COUNTER = new AtomicInteger(0);
	public static int CURRENTLY_ACTIVE_CANVAS = 0;

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
	protected boolean useBitBlit = false;
	protected Color fboClearColor = Color.darkGray;
	protected Color screenClearColor = Color.BLACK;
	protected boolean useMSAA = true;
	public final int canvasID;

	public FBOCanvas(GLData data){
		super(data);
		this.canvasID = ATOMIC_COUNTER.incrementAndGet();
		this.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				repaint();
			}
		});
	}

	public FBOCanvas() {
		super();
		this.canvasID = ATOMIC_COUNTER.incrementAndGet();
		this.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				repaint();
			}
		});
	}
	
	@Override
	protected void beforeRender() {
		CURRENTLY_ACTIVE_CANVAS = this.canvasID;
		super.beforeRender();
	}
	
	@Override
	protected void afterRender() {
		super.afterRender();
		CURRENTLY_ACTIVE_CANVAS = 0;
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

	public int getPixel(int x, int y, boolean picking, int areaSize){
		int attachment = picking ? GL30.GL_COLOR_ATTACHMENT1:GL30.GL_COLOR_ATTACHMENT0;
		int[] colors = new int[areaSize*areaSize];
		
		Utils.execOnAWTEventDispatch(()->{
			runInContext(()->{
				GLUtils.fetchPixels(
						fbo.getFBOid(), 
						attachment, 
						x-areaSize/2, 
						fbo.height-1-y+areaSize/2, 
						areaSize, 
						areaSize, 
						colors
				);
			});
		});
		if(areaSize == 1){
			return colors[0];
		}
		int center = areaSize*(areaSize/2)+(areaSize/2);
		int centerValue = colors[center];
		int centerBonus = centerValue == 0 ? 0:1;
		// calculate most prominent color (mode)
		Arrays.sort(colors);
		int currentValue = colors[0]; 
		int mostValue = currentValue; 
		int count = currentValue == centerValue ? 1+centerBonus:1; // center color gets bonus
		int maxCount=count;
		for(int i = 1; i < colors.length; i++){
			if(colors[i]==currentValue && currentValue != 0){
				count++;
			} else {
				if(count > maxCount){
					maxCount = count;
					mostValue = currentValue;
				}
				currentValue = colors[i];
				count = currentValue == centerValue ? 1+centerBonus:1; // center color gets bonus
			}
		}
		return mostValue;
	}


	@Override
	public void paintGL(){
		int w,h;
		if((w=getWidth()) >0 && (h=getHeight()) >0){
			if(fbo == null || w!=fbo.width || h!=fbo.height){
				setFBO(new FBO(w, h, false));
				if(useMSAA && GLUtils.canMultisample2X()){
					setFBO_MS(new FBO(w, h, true));
				}
			}
			// offscreen
			GL11.glClearColor(0, 0, 0, 0);
			setRenderTargetsColorAndPicking(w, h);
			GL11.glClear( GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT );
			/* we need to first draw a viewport filling quad that fills the buffer with the clear color
			 * in order to resolve issue #4 (https://github.com/hageldave/JPlotter/issues/4)
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
				vertexArray.releaseAndDisableAttributes(0);
				fillShader.release();
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
					vertexArray.releaseAndDisableAttributes(0);
					blitShader.release();
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
	@GLContextRequired
	public void close() {
		CharacterAtlas.clearAndCloseAtlasCollection();
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
		if(SwingUtilities.isEventDispatchThread()){
			render();
		} else {
			SwingUtilities.invokeLater(()->render());
		}
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
