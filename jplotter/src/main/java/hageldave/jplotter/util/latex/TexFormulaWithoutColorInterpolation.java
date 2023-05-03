package hageldave.jplotter.util.latex;

import org.scilab.forge.jlatexmath.*;

import javax.imageio.ImageIO;
import javax.imageio.stream.FileImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;

/**
 * The TexFormulaWithoutColorInterpolation extends the {@link TeXFormula} class.
 * The changes behavior consists of modified methods that return {@link TexIconWithoutColorInterpolation} objects
 * instead of {@link TeXIcon} objects.
 */
public class TexFormulaWithoutColorInterpolation extends TeXFormula {
    public TexFormulaWithoutColorInterpolation(String s) throws ParseException {
        super(s);
    }

    public TexIconWithoutColorInterpolation createTeXIcon(int style, float size) {
        return (new TeXIconBuilderWithoutColorInterpolation()).setStyle(style).setSize(size).build();
    }

    public TexIconWithoutColorInterpolation createTeXIcon(int style, float size, int type) {
        return (new TeXIconBuilderWithoutColorInterpolation()).setStyle(style).setSize(size).setType(type).build();
    }

    public TexIconWithoutColorInterpolation createTeXIcon(int style, float size, int type, Color fgcolor) {
        return (new TeXIconBuilderWithoutColorInterpolation()).setStyle(style).setSize(size).setType(type).setFGColor(fgcolor).build();
    }

    public TexIconWithoutColorInterpolation createTeXIcon(int style, float size, boolean trueValues) {
        return (new TeXIconBuilderWithoutColorInterpolation()).setStyle(style).setSize(size).setTrueValues(trueValues).build();
    }

    public TexIconWithoutColorInterpolation createTeXIcon(int style, float size, int widthUnit, float textwidth, int align) {
        return this.createTeXIcon(style, size, 0, widthUnit, textwidth, align);
    }

    public TexIconWithoutColorInterpolation createTeXIcon(int style, float size, int type, int widthUnit, float textwidth, int align) {
        return (new TeXIconBuilderWithoutColorInterpolation()).setStyle(style).setSize(size).setType(type).setWidth(widthUnit, textwidth, align).build();
    }

    public TexIconWithoutColorInterpolation createTeXIcon(int style, float size, int type, int widthUnit, float textwidth, int align, int interlineUnit, float interline) {
        return (new TeXIconBuilderWithoutColorInterpolation()).setStyle(style).setSize(size).setType(type).setWidth(widthUnit, textwidth, align).setInterLineSpacing(interlineUnit, interline).build();
    }

    public void createImage(String format, int style, float size, String out, Color bg, Color fg, boolean transparency) {
        TeXIcon icon = this.createTeXIcon(style, size);
        icon.setInsets(new Insets(1, 1, 1, 1));
        int w = icon.getIconWidth();
        int h = icon.getIconHeight();
        BufferedImage image = new BufferedImage(w, h, transparency ? 2 : 1);
        Graphics2D g2 = image.createGraphics();
        if (bg != null && !transparency) {
            g2.setColor(bg);
            g2.fillRect(0, 0, w, h);
        }

        icon.setForeground(fg);
        icon.paintIcon((Component)null, g2, 0, 0);

        try {
            FileImageOutputStream imout = new FileImageOutputStream(new File(out));
            ImageIO.write(image, format, imout);
            imout.flush();
            imout.close();
        } catch (IOException var14) {
            System.err.println("I/O error : Cannot generate " + out);
        }

        g2.dispose();
    }

    private Box createBox(TeXEnvironment style) {
        return (Box)(this.root == null ? new StrutBox(0.0F, 0.0F, 0.0F, 0.0F) : this.root.createBox(style));
    }

    private DefaultTeXFont createFont(float size, int type) {
        DefaultTeXFont dtf = new DefaultTeXFont(size);
        if (type == 0) {
            dtf.setSs(false);
        }

        if ((type & 8) != 0) {
            dtf.setRoman(true);
        }

        if ((type & 16) != 0) {
            dtf.setTt(true);
        }

        if ((type & 1) != 0) {
            dtf.setSs(true);
        }

        if ((type & 4) != 0) {
            dtf.setIt(true);
        }

        if ((type & 2) != 0) {
            dtf.setBold(true);
        }

        return dtf;
    }

    public class TeXIconBuilderWithoutColorInterpolation {
        private Integer style;
        private Float size;
        private Integer type;
        private Color fgcolor;
        private boolean trueValues = false;
        private Integer widthUnit;
        private Float textWidth;
        private Integer align;
        private boolean isMaxWidth = false;
        private Integer interLineUnit;
        private Float interLineSpacing;

        public TeXIconBuilderWithoutColorInterpolation() {
        }

        public TeXIconBuilderWithoutColorInterpolation setStyle(int style) {
            this.style = style;
            return this;
        }

        public TeXIconBuilderWithoutColorInterpolation setSize(float size) {
            this.size = size;
            return this;
        }

        public TeXIconBuilderWithoutColorInterpolation setType(int type) {
            this.type = type;
            return this;
        }

        public TeXIconBuilderWithoutColorInterpolation setFGColor(Color fgcolor) {
            this.fgcolor = fgcolor;
            return this;
        }

        public TeXIconBuilderWithoutColorInterpolation setTrueValues(boolean trueValues) {
            this.trueValues = trueValues;
            return this;
        }

        public TeXIconBuilderWithoutColorInterpolation setWidth(int widthUnit, float textWidth, int align) {
            this.widthUnit = widthUnit;
            this.textWidth = textWidth;
            this.align = align;
            this.trueValues = true;
            return this;
        }

        public TeXIconBuilderWithoutColorInterpolation setInterLineSpacing(int interLineUnit, float interLineSpacing) {
            if (this.widthUnit == null) {
                throw new IllegalStateException("Cannot set inter line spacing without having specified a width!");
            } else {
                this.interLineUnit = interLineUnit;
                this.interLineSpacing = interLineSpacing;
                return this;
            }
        }

        public TexIconWithoutColorInterpolation build() {
            if (this.style == null) {
                throw new IllegalStateException("A style is required. Use setStyle()");
            } else if (this.size == null) {
                throw new IllegalStateException("A size is required. Use setStyle()");
            } else {
                DefaultTeXFont font = this.type == null ? new DefaultTeXFont(this.size) : TexFormulaWithoutColorInterpolation.this.createFont(this.size, this.type);
                TeXEnvironment te;
                if (this.widthUnit != null) {
                    te = new TeXEnvironment(this.style, font, this.widthUnit, this.textWidth);
                } else {
                    te = new TeXEnvironment(this.style, font);
                }

                if (this.interLineUnit != null) {
                    te.setInterline(this.interLineUnit, this.interLineSpacing);
                }

                Box box = TexFormulaWithoutColorInterpolation.this.createBox(te);
                TexIconWithoutColorInterpolation ti;
                if (this.widthUnit != null) {
                    HorizontalBox hb;
                    if (this.interLineUnit != null) {
                        float il = this.interLineSpacing * SpaceAtom.getFactor(this.interLineUnit, te);
                        Box b = BreakFormula.split(box, te.getTextwidth(), il);
                        hb = new HorizontalBox(b, this.isMaxWidth ? b.getWidth() : te.getTextwidth(), this.align);
                    } else {
                        hb = new HorizontalBox(box, this.isMaxWidth ? box.getWidth() : te.getTextwidth(), this.align);
                    }

                    ti = new TexIconWithoutColorInterpolation(hb, this.size, this.trueValues);
                } else {
                    ti = new TexIconWithoutColorInterpolation(box, this.size, this.trueValues);
                }

                if (this.fgcolor != null) {
                    ti.setForeground(this.fgcolor);
                }

                ti.isColored = te.isColored;
                return ti;
            }
        }
    }

}
