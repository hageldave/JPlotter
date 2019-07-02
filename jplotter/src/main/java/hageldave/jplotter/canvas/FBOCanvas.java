package hageldave.jplotter.canvas;

import java.awt.Color;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
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
import org.lwjgl.opengl.awt.PlatformGLCanvas;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import hageldave.jplotter.gl.FBO;
import hageldave.jplotter.gl.Shader;
import hageldave.jplotter.gl.VertexArray;
import hageldave.jplotter.misc.CharacterAtlas;
import hageldave.jplotter.svg.SVGUtils;
import hageldave.jplotter.util.Annotations.GLContextRequired;
import hageldave.jplotter.util.CapabilitiesCreator;
import hageldave.jplotter.util.GLUtils;
import hageldave.jplotter.util.Utils;

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
 * <p>
 * The FBOCanvas also provides the ability of scalable vector graphics (SVG) export with the
 * {@link #paintSVG()} method.
 * An implementing class therefore has to implement the {@link #paintToSVG(Document, Element, int, int)}
 * method when it is able to express its contents in terms of SVG elements.
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
	protected Color screenClearColor = Color.BLACK;
	protected boolean useMSAA = true;
	public final int canvasID;

	/**
	 * Creates a new {@link FBOCanvas}.
	 * Increments the {@link #ATOMIC_COUNTER} and sets
	 * its value as this canvas' {@link #canvasID}.
	 * It also registers a {@link ComponentListener} that
	 * calls the {@link #repaint()} method when resizing.
	 * 
	 * @param data GLData object for creating the GL context.
	 */
	protected FBOCanvas(GLData data){
		super(data);
		this.canvasID = ATOMIC_COUNTER.incrementAndGet();
		this.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				repaint();
			}
		});
	}

	/** Calls {@link #FBOCanvas(GLData)} with default {@link GLData}. */
	protected FBOCanvas() {
		this(new GLData());
	}
	
	/**
	 * Activates this {@link AWTGLCanvas}' GL context,
	 * locks the {@link PlatformGLCanvas} and sets the
	 * {@link #CURRENTLY_ACTIVE_CANVAS} canvas to this
	 * {@link #canvasID}.
	 */
	@Override
	protected void beforeRender() {
		CURRENTLY_ACTIVE_CANVAS = this.canvasID;
		super.beforeRender();
	}
	
	/**
	 * Deactivates this {@link AWTGLCanvas}' GL context,
	 * unlocks the {@link PlatformGLCanvas} and sets the
	 * {@link #CURRENTLY_ACTIVE_CANVAS} canvas to 0.
	 */
	@Override
	protected void afterRender() {
		super.afterRender();
		CURRENTLY_ACTIVE_CANVAS = 0;
	}


	/**
	 * Calls {@link CapabilitiesCreator#create()} and allocates GL resources, 
	 * i.e. creates the {@link Shader}s of this canvas as well as its vertex array.
	 */
	@Override
	@GLContextRequired
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

	/**
	 * Reads the color value of the pixel at the specified location if areaSize == 1.
	 * This can be used to get the color or picking color under the mouse cursor.
	 * <p>
	 * Since the cursor placement may be inexact and thus miss the location the user
	 * was actually interested in, the areaSize parameter can be increased to create
	 * a window of pixels around the specified location.
	 * This window area will be examined and the most prominent non zero color value will
	 * be returned.
	 * @param x coordinate of the pixels location
	 * @param y coordinate of the pixels location
	 * @param picking whether the picking color or the visible color should be retrieved.
	 * @param areaSize width and height of the area around the specified location.
	 * @return the most prominent color in the area as integer packed ARGB value.
	 * If the returned value is to be used as an object id from picking color, then the
	 * alpha value probably has to be discarded first using {@code 0x00ffffff & returnValue}.
	 */
	public int getPixel(int x, int y, boolean picking, int areaSize){
		if(fbo == null){
			return 0;
		}
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

	/**
	 * Clears the FBO and sets it as render target before calling {@link #paintToFBO(int, int)}.
	 * Afterwards the contents of the FBO are transferred to the screen framebuffer.
	 */
	@Override
	@GLContextRequired
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
				GL20.glUniform4f(loc, getBackground().getRed()/255f, getBackground().getGreen()/255f, getBackground().getBlue()/255f, getBackground().getAlpha()/255f);
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

	/**
	 * Paints this Canvas' contents to the currently active draw buffer that is this {@link FBOCanvas}'
	 * framebuffer object.
	 * Will be called during {@link #paintGL()} which is called during {@link #render()}.
	 * @param width of the current viewport
	 * @param height of the current viewport
	 */
	@GLContextRequired
	protected abstract void paintToFBO(int width, int height);
	
	/**
	 * Creates a new SVG {@link Document} and renders this canvas as SVG elements.
	 * Will call {@link #paintToSVG(Document, Element, int, int)} after setting up
	 * the document and creating the initial elements.
	 * @return the created document
	 */
	public Document paintSVG(){
		Document document = SVGUtils.createSVGDocument(getWidth(), getHeight());
		paintSVG(document, document.getDocumentElement());
		return document;
	}
	
	/**
	 * Renders this canvas as SVG elements under the specified parent element.
	 * Will call {@link #paintToSVG(Document, Element, int, int)} after creating 
	 * the initial elements.
	 * @param document document to create SVG elements with
	 * @param parent the parent node to which this canvas is supposed to be rendered
	 * to.
	 */
	public void paintSVG(Document document, Element parent){
		int w,h;
		if((w=getWidth()) >0 && (h=getHeight()) >0){
			if(SVGUtils.getDefs(document) == null){
				Element defs = SVGUtils.createSVGElement(document, "defs");
				defs.setAttributeNS(null, "id", "JPlotterDefs");
				document.getDocumentElement().appendChild(defs);
			}
			
			Element rootGroup = SVGUtils.createSVGElement(document, "g");
			parent.appendChild(rootGroup);
			rootGroup.setAttributeNS(null, "transform", "scale(1,-1) translate(0,-"+h+")");
			
			Element background = SVGUtils.createSVGElement(document, "rect");
			rootGroup.appendChild(background);
			background.setAttributeNS(null, "id", "background"+"@"+hashCode());
			background.setAttributeNS(null, "width", ""+w);
			background.setAttributeNS(null, "height", ""+h);
			background.setAttributeNS(null, "fill", SVGUtils.svgRGBhex(getBackground().getRGB()));
			
			paintToSVG(document, rootGroup, w,h);
		}
	}
	
	/**
	 * Renders this {@link FBOCanvas} in terms of SVG elements
	 * to the specified parent element of the specified SVG document.
	 * <p>
	 * This method has to be overridden when the implementing
	 * class can express its contents in terms of SVG.
	 * 
	 * @param doc document to create svg elements with
	 * @param parent to append svg elements to
	 * @param w width of the viewport (the width of this Canvas)
	 * @param h height of the viewport (the height of this Canvas)
	 */
	protected void paintToSVG(Document doc, Element parent, int w, int h){}

	/**
	 * Disposes of this {@link FBOCanvas} GL resources, i.e. closes the shaders, 
	 * vertex array and FBOs.
	 * Since this Canvas is the provider of a GL context, it also closes the 
	 * {@link CharacterAtlas}es that are associated with its context.
	 */
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

	/**
	 * Sets the render targets and view port size.
	 * @param fboID to bind or 0 to unbind the framebuffer
	 * @param width of the viewport
	 * @param height of the viewport
	 * @param drawBuffers the drawbuffers to activate 
	 * e.g. {@link GL30#GL_COLOR_ATTACHMENT0} and {@link GL30#GL_COLOR_ATTACHMENT1} 
	 * or {@link GL11#GL_BACK}.
	 */
	@GLContextRequired
	protected static void setRenderTargets(int fboID, int width, int height, int... drawBuffers){
		GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fboID);
		if(drawBuffers.length > 1){
			GL30.glDrawBuffers(drawBuffers);
		} else {
			GL30.glDrawBuffer(drawBuffers[0]);
		}
		GL11.glViewport(0, 0, width, height);
	}

	/**
	 * Binds this {@link FBOCanvas} {@link FBO} 
	 * (if {@link #fboMS} is non null it will be bound instead of {@link #fbo})
	 * and sets the drawbuffers to the first and second color attachment 
	 * (GL_COLOR_ATTACHMENT0 and GL_COLOR_ATTACHMENT1).
	 * The viewport size will be set to the specified dimensions.
	 * @param width of the viewport
	 * @param height of the viewport
	 */
	protected void setRenderTargetsColorAndPicking(int width, int height) {
		int fboIDToUse = Objects.nonNull(this.fboMS) ? this.fboMS.getFBOid() : this.fbo.getFBOid();
		setRenderTargets(fboIDToUse, width, height, GL30.GL_COLOR_ATTACHMENT0, GL30.GL_COLOR_ATTACHMENT1);
	}

	/**
	 * Binds this {@link FBOCanvas} {@link FBO} 
	 * (if {@link #fboMS} is non null it will be bound instead of {@link #fbo})
	 * and sets the drawbuffer to the first color attachment 
	 * (GL_COLOR_ATTACHMENT0).
	 * The viewport size will be set to the specified dimensions.
	 * @param width of the viewport
	 * @param height of the viewport
	 */
	protected void setRenderTargetsColorOnly(int width, int height) {
		int fboIDToUse = Objects.nonNull(this.fboMS) ? this.fboMS.getFBOid() : this.fbo.getFBOid();
		setRenderTargets(fboIDToUse, width, height, GL30.GL_COLOR_ATTACHMENT0);
	}

	/**
	 * Binds this {@link FBOCanvas} {@link FBO} 
	 * (if {@link #fboMS} is non null it will be bound instead of {@link #fbo})
	 * and sets the drawbuffer to the second color attachment 
	 * (GL_COLOR_ATTACHMENT1).
	 * The viewport size will be set to the specified dimensions.
	 * @param width of the viewport
	 * @param height of the viewport
	 */
	protected void setRenderTargetsPickingOnly(int width, int height) {
		int fboIDToUse = Objects.nonNull(this.fboMS) ? this.fboMS.getFBOid() : this.fbo.getFBOid();
		setRenderTargets(fboIDToUse, width, height, GL30.GL_COLOR_ATTACHMENT1);
	}
	
	/**
	 * Calls {@link #render()} and super repaint on AWT event dispatch thread or
	 * schedules a render and super repaint call on it.
	 */
	@Override
	public void repaint() {
		if(SwingUtilities.isEventDispatchThread()){
			render();
			super.repaint();
		} else {
			SwingUtilities.invokeLater(()->{
				render();
				super.repaint();
			});
		}
	}	
}
