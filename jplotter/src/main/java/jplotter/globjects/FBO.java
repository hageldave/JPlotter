package jplotter.globjects;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL32;

import jplotter.util.GLUtils;

/**
 * FrameBufferObject
 */
public class FBO implements AutoCloseable {

	public final int width;
	public final int height;
	public final boolean isMultisampled;
	public final int numMultisamples;

	int fboid;
	int colortexid;
	int pickingtexid;
	int depthtexid;

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

	public int getFBOid() {
		return fboid;
	}

	public int getMainColorTexId() {
		return colortexid;
	}

	public int getPickingColorTexId() {
		return pickingtexid;
	}

	public int getDepthTexId() {
		return depthtexid;
	}

	@Override
	public void close() {
		GL11.glDeleteTextures(colortexid);
		GL11.glDeleteTextures(pickingtexid);
		GL11.glDeleteTextures(depthtexid);
		GL30.glDeleteFramebuffers(fboid);
		fboid = colortexid = pickingtexid = depthtexid = 0;
	}

}
