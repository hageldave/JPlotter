package hageldave.jplotter.util.latex;

import org.scilab.forge.jlatexmath.Box;
import org.scilab.forge.jlatexmath.HorizontalBox;
import org.scilab.forge.jlatexmath.TeXIcon;

import java.awt.*;
import java.awt.geom.AffineTransform;

public class CustomTexIcon extends TeXIcon {
    private static final Color defaultColor = new Color(0, 0, 0);
    public static float defaultSize = -1.0F;
    public static float magFactor = 0.0F;
    private Box box;
    private final float size;
    private Insets insets;
    private Color fg;
    public boolean isColored;

    protected CustomTexIcon(Box b, float size) {
        this(b, size, false);
    }

    protected CustomTexIcon(Box b, float size, boolean trueValues) {
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

    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D)g;
        RenderingHints oldHints = g2.getRenderingHints();
        AffineTransform oldAt = g2.getTransform();
        Color oldColor = g2.getColor();
        // TODO: this is changed from the original
//        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
//        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
//        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.scale((double)this.size, (double)this.size);
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
