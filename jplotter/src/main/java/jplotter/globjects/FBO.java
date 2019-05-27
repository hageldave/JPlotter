package jplotter.globjects;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL32;

import jplotter.Annotations.GLContextRequired;
import jplotter.util.GLUtils;
import jplotter.util.GLUtils.GLRuntimeException;

/**
 * The FBO class encapsulates a GL 
 * <a href="https://www.khronos.org/opengl/wiki/Framebuffer_Object">
 * framebuffer object
 * </a>.
 * This implementation is a framebuffer which consists of a depth attachment and two color 
 * attachments where the second is intended to contain the renderings 
 * in picking colors.
 * The FBO can be created multisampled for multisampling anti aliasing (MSAA).
 * The backing attachment textures are created with a specified dimension
 * and the color textures are of internal format {@link GL11#GL_RGBA8}.
 */
public class FBO implements AutoCloseable {

	/** width of the framebuffer textures */
	public final int width;
	/** height of the framebuffer textures */
	public final int height;
	/** whether the FBO is multisampled */
	public final boolean isMultisampled;
	/** how many samples are used for multisampling, 0 if not multisampled */
	public final int numMultisamples;

	int fboid;
	int colortexid;
	int pickingtexid;
	int depthtexid;

	/**
	 * Creates a new framebuffer object with textures of specified size.
	 * If multisampling is true, a multisampled FBO is created which
	 * will use 4 samples or 2 if 4 are not supported.
	 * The created FBO has two color attachments and one depth attachment.
	 * @param width of the FBO textures
	 * @param height of the FBO textures
	 * @param multisampling if true FBO will be multisampled
	 * @throws GLRuntimeException when {@link GLUtils#checkFBOstatus()}
	 * is not successful.
	 */
	@GLContextRequired
	public FBO(int width, int height, boolean multisampling) {
		this.width = width;
		this.height = height;
		this.isMultisampled = multisampling;
		this.fboid = GL30.glGenFramebuffers();
		if(multisampling){
			this.numMultisamples = GLUtils.canMultisample4X() ? 4:2;
			this.colortexid = GLUtils.create2DTextureMultisample(width, height, GL11.GL_RGBA8, numMultisamples);
			this.pickingtexid = GLUtils.create2DTextureMultisample(width, height, GL11.GL_RGBA8, numMultisamples);
			this.depthtexid = GLUtils.create2DTextureMultisample(width, height, GL11.GL_DEPTH_COMPONENT, numMultisamples);
		} else {
			this.numMultisamples = 0;
			this.colortexid = GLUtils.create2DTexture(width, height, GL11.GL_RGBA8, GL11.GL_RGBA, GL11.GL_LINEAR, GL12.GL_CLAMP_TO_EDGE);
			this.pickingtexid = GLUtils.create2DTexture(width, height, GL11.GL_RGBA8, GL11.GL_RGBA, GL11.GL_LINEAR, GL12.GL_CLAMP_TO_EDGE);
			this.depthtexid = GLUtils.create2DTexture(width, height, GL11.GL_DEPTH_COMPONENT, GL11.GL_DEPTH_COMPONENT, GL11.GL_NEAREST, GL12.GL_CLAMP_TO_EDGE);
		}
		GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fboid);
		{
			int texTarget = multisampling ? GL32.GL_TEXTURE_2D_MULTISAMPLE : GL11.GL_TEXTURE_2D;
			GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, texTarget, colortexid, 0);
			GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT1, texTarget, pickingtexid, 0);
			GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, texTarget, depthtexid, 0);
			String status = GLUtils.checkFBOstatus();
			if(!status.isEmpty()){
				close();
				throw new GLUtils.GLRuntimeException("Bad framebuffer status: " + status);
			}
		}
		GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
	}

	/**
	 * @return GL name of this framebuffer object.
	 */
	public int getFBOid() {
		return fboid;
	}

	/**
	 * @return GL name of the GL texture object corresponding to the first color attachment
	 * {@link GL30#GL_COLOR_ATTACHMENT0}.
	 */
	public int getMainColorTexId() {
		return colortexid;
	}

	/**
	 * @return GL name of the GL texture object corresponding to the second color attachment
	 * {@link GL30#GL_COLOR_ATTACHMENT1}.
	 */
	public int getPickingColorTexId() {
		return pickingtexid;
	}

	/**
	 * @return GL name of the GL texture object corresponding to the depth attachment
	 * {@link GL30#GL_DEPTH_ATTACHMENT}.
	 */
	public int getDepthTexId() {
		return depthtexid;
	}

	/**
	 * Disposes of all GL resources this FBO owns, i.e.
	 * deletes the GL texture objects and GL framebuffer object.
	 */
	@Override
	@GLContextRequired
	public void close() {
		GL11.glDeleteTextures(colortexid);
		GL11.glDeleteTextures(pickingtexid);
		GL11.glDeleteTextures(depthtexid);
		GL30.glDeleteFramebuffers(fboid);
		fboid = colortexid = pickingtexid = depthtexid = 0;
	}

}
