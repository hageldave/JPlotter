package hageldave.jplotter.util.latex;

import org.scilab.forge.jlatexmath.Box;
import org.scilab.forge.jlatexmath.HorizontalBox;
import org.scilab.forge.jlatexmath.TeXIcon;
import java.awt.*;
import java.awt.geom.AffineTransform;

/**
 * The TexIconWithoutColorInterpolation class is an extension of the {@link TeXIcon} class.
 * It removes the "RenderingHints.KEY_RENDERING" {@link RenderingHints} when calling the {@link #paintIcon(Component, Graphics, int, int)} method.
 * The usage of the rendering hint resulted in SVG rendering issues in some browsers.
 */
public class TexIconWithoutColorInterpolation extends TeXIcon {
    private static final Color defaultColor = new Color(0, 0, 0);
    private Box box;
    private float size;
    private Insets insets;
    private Color fg;

    protected TexIconWithoutColorInterpolation(Box b, float size) {
        this(b, size, false);
    }

    protected TexIconWithoutColorInterpolation(Box b, float size, boolean trueValues) {
        super(b, size, trueValues);
        this.insets = new Insets(0, 0, 0, 0);
        this.fg = null;
        this.isColored = false;
        this.box = b;
        if (defaultSize != -1.0F) {
            size = defaultSize;
        }

        if (magFactor != 0.0F) {
            this.size = size * Math.abs(magFactor);
        } else {
            this.size = size;
        }

        if (!trueValues) {
            Insets var10000 = this.insets;
            var10000.top += (int)(0.18F * size);
            var10000 = this.insets;
            var10000.bottom += (int)(0.18F * size);
            var10000 = this.insets;
            var10000.left += (int)(0.18F * size);
            var10000 = this.insets;
            var10000.right += (int)(0.18F * size);
        }
    }

    public void setForeground(Color fg) {
        this.fg = fg;
    }

    public Insets getInsets() {
        return this.insets;
    }

    public void setInsets(Insets insets, boolean trueValues) {
        this.insets = insets;
        if (!trueValues) {
            Insets var10000 = this.insets;
            var10000.top += (int)(0.18F * this.size);
            var10000 = this.insets;
            var10000.bottom += (int)(0.18F * this.size);
            var10000 = this.insets;
            var10000.left += (int)(0.18F * this.size);
            var10000 = this.insets;
            var10000.right += (int)(0.18F * this.size);
        }

    }

    public void setInsets(Insets insets) {
        this.setInsets(insets, false);
    }

    public void setIconWidth(int width, int alignment) {
        float diff = (float)(width - this.getIconWidth());
        if (diff > 0.0F) {
            this.box = new HorizontalBox(this.box, this.box.getWidth() + diff, alignment);
        }

    }

//    public void setIconHeight(int height, int alignment) {
//        float diff = (float)(height - this.getIconHeight());
//        if (diff > 0.0F) {
//            this.box = new VerticalBox(this.box, diff, alignment);
//        }
//    }

    public int getIconHeight() {
        return (int)((double)(this.box.getHeight() * this.size) + 0.99 + (double)this.insets.top) + (int)((double)(this.box.getDepth() * this.size) + 0.99 + (double)this.insets.bottom);
    }

    public int getIconDepth() {
        return (int)((double)(this.box.getDepth() * this.size) + 0.99 + (double)this.insets.bottom);
    }

    public int getIconWidth() {
        return (int)((double)(this.box.getWidth() * this.size) + 0.99 + (double)this.insets.left + (double)this.insets.right);
    }

    public float getTrueIconHeight() {
        return (this.box.getHeight() + this.box.getDepth()) * this.size;
    }

    public float getTrueIconDepth() {
        return this.box.getDepth() * this.size;
    }

    public float getTrueIconWidth() {
        return this.box.getWidth() * this.size;
    }

    public float getBaseLine() {
        return (float)(((double)(this.box.getHeight() * this.size) + 0.99 + (double)this.insets.top) / ((double)((this.box.getHeight() + this.box.getDepth()) * this.size) + 0.99 + (double)this.insets.top + (double)this.insets.bottom));
    }

    public Box getBox() {
        return this.box;
    }

    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D)g;
        RenderingHints oldHints = g2.getRenderingHints();
        AffineTransform oldAt = g2.getTransform();
        Color oldColor = g2.getColor();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.scale((double) this.size, (double) this.size);
        if (this.fg != null) {
            g2.setColor(this.fg);
        } else if (c != null) {
            g2.setColor(c.getForeground());
        } else {
            g2.setColor(defaultColor);
        }

        this.box.draw(g2, (float)(x + this.insets.left) / this.size, (float)(y + this.insets.top) / this.size + this.box.getHeight());
        g2.setRenderingHints(oldHints);
        g2.setTransform(oldAt);
        g2.setColor(oldColor);
    }
}
