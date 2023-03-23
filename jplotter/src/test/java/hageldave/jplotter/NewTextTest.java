package hageldave.jplotter;


import hageldave.imagingkit.core.Img;
import hageldave.imagingkit.core.io.ImageSaver;
import hageldave.jplotter.canvas.BlankCanvas;
import hageldave.jplotter.canvas.BlankCanvasFallback;
import hageldave.jplotter.canvas.JPlotterCanvas;
import hageldave.jplotter.interaction.CoordSysPanning;
import hageldave.jplotter.pdf.PDFUtils;
import hageldave.jplotter.renderables.Lines;
import hageldave.jplotter.renderables.NewPosRect;
import hageldave.jplotter.renderables.NewText;
import hageldave.jplotter.renderables.TextDecoration;
import hageldave.jplotter.renderers.CompleteRenderer;
import hageldave.jplotter.renderers.CoordSysRenderer;
import hageldave.jplotter.svg.SVGUtils;
import hageldave.jplotter.util.PickingRegistry;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.w3c.dom.Document;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;

public class NewTextTest {
    @SuppressWarnings("resource" /* compiler is too dumb to realize there is no leak */)
    public static void main(String[] args) {
        int fontSize = 19;
        int fontStyle = Font.PLAIN;

        Lines textLines = new Lines();
        CoordSysRenderer coordsys = new CoordSysRenderer();
        PickingRegistry<NewText> pr = new PickingRegistry<>();

        // Test text underlining
        NewText textUnderline = new NewText("testing the underlining", fontSize, fontStyle);
        textUnderline.setOrigin(0, 135);
        textUnderline.setColor(Color.RED);
        textUnderline.setTextDecoration(TextDecoration.UNDERLINE);
        textUnderline.setPickColor(pr.getNewID());
        textUnderline.setPositioningRectangle(new NewPosRect(1, 1));

        // Test text strikethrough
        NewText textStrikeThrough = new NewText("testing the strikethrough", fontSize, fontStyle);
        textStrikeThrough.setOrigin(10, 135);
        textStrikeThrough.setColor(Color.BLUE);
        textStrikeThrough.setTextDecoration(TextDecoration.STRIKETHROUGH);
        textStrikeThrough.setPickColor(pr.getNewID());
        textStrikeThrough.setPositioningRectangle(new NewPosRect(0, 1));

        // Test anchor points
        NewText anchorPoints = new NewText("testing the anchorpoint \n (2, 2)", fontSize, fontStyle);
        anchorPoints.setOrigin(0, 100);
        anchorPoints.setBackground(Color.ORANGE);
        anchorPoints.setPickColor(pr.getNewID());
        anchorPoints.setPositioningRectangle(new NewPosRect(1, 0));

        // Test text insets with background
        NewText textInsets = new NewText("testing the insets \n with multiple lines", fontSize, fontStyle);
        textInsets.setOrigin(10, 110);
        textInsets.setBackground(Color.MAGENTA);
        textInsets.setTextDecoration(TextDecoration.UNDERLINE);
        textInsets.setPickColor(pr.getNewID());
        textInsets.setInsets(new Insets(10, 10, 10, 10));
        textInsets.setPositioningRectangle(new NewPosRect(0, 1));

        // Test text insets with background and rotation
        NewText textInsetsWithRotation = new NewText("testing the insets \n with rotation", fontSize, fontStyle);
        textInsetsWithRotation.setOrigin(0, 50);
        textInsetsWithRotation.setBackground(Color.GREEN);
        textInsetsWithRotation.setPickColor(pr.getNewID());
        textInsetsWithRotation.setInsets(new Insets(10, 10, 10, 10));
        textInsetsWithRotation.setAngle(0.2);
        textInsetsWithRotation.setPositioningRectangle(new NewPosRect(1, 1));

        // Testing strikethrough, insets, rotation all in one
        NewText textInsetsWithRotationAndStrikethrough = new NewText("testing the insets \n with rotation and strikethrough", fontSize, fontStyle);
        textInsetsWithRotationAndStrikethrough.setOrigin(0, 50);
        textInsetsWithRotationAndStrikethrough.setBackground(Color.CYAN);
        textInsetsWithRotationAndStrikethrough.setInsets(new Insets(10, 10, 10, 10));
        textInsetsWithRotationAndStrikethrough.setAngle(0.2);
        textInsetsWithRotationAndStrikethrough.setPickColor(pr.getNewID());
        textInsetsWithRotationAndStrikethrough.setTextDecoration(TextDecoration.STRIKETHROUGH);
        textInsetsWithRotationAndStrikethrough.setPositioningRectangle(new NewPosRect(0, 1));

        // Testing latex rendering in text mode
        NewText textLatex = new NewText("##BEGINLATEX## \\text{testing the latex rendering} \\\\ \\text{in text mode}", fontSize, fontStyle);
        textLatex.setOrigin(0, 200);
        textLatex.setBackground(Color.LIGHT_GRAY);
        textLatex.setTextDecoration(TextDecoration.UNDERLINE);
        textLatex.setPickColor(pr.getNewID());
        textLatex.setInsets(new Insets(5, 10, 5, 10));
        textLatex.setPositioningRectangle(new NewPosRect(1, 1));

        // Testing latex rendering in math mode
        NewText textLatexMath = new NewText("##BEGINLATEX## (\\bigwedge_{i=1}^{1} F_i) \\wedge (\\bigwedge_{i=1}^{1} G_i) \\equiv \\bigwedge_{i=1}^{1} (F_i \\wedge G_i)", fontSize, fontStyle);
        textLatexMath.setOrigin(10, 200);
        textLatexMath.setBackground(Color.PINK);
        textLatexMath.setPickColor(pr.getNewID());
        textLatexMath.setInsets(new Insets(5, 10, 5, 10));
        textLatexMath.setPositioningRectangle(new NewPosRect(0, 1));

        // okay we're good to go, lets display the data in a coordinate system
        CompleteRenderer content = new CompleteRenderer();
        coordsys.setContent(content
                .addItemToRender(textLines)
                .addItemToRender(anchorPoints)
                .addItemToRender(textUnderline)
                .addItemToRender(textStrikeThrough)
                .addItemToRender(textInsets)
                .addItemToRender(textInsetsWithRotation)
                .addItemToRender(textInsetsWithRotationAndStrikethrough)
                .addItemToRender(textLatex)
                .addItemToRender(textLatexMath));
        // lets set the coordinate view to cover the whole sampling space
        coordsys.setCoordinateView(-550, -20, 550, 210);

        // display the coordinate system on a blank canvas
        boolean useOpenGL = false;
        JPlotterCanvas canvas = useOpenGL ? new BlankCanvas() : new BlankCanvasFallback();
        canvas.setRenderer(coordsys);

        new CoordSysPanning(canvas, coordsys).register();

        // lets put a JFrame around it all and launch
        JFrame frame = new JFrame("Text features example");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(canvas.asComponent());
        canvas.asComponent().setPreferredSize(new Dimension(800, 500));
        canvas.asComponent().setBackground(Color.WHITE);

        canvas.addCleanupOnWindowClosingListener(frame);

        SwingUtilities.invokeLater(()->{
            frame.pack();
            frame.setVisible(true);
        });

        // add a pop up menu (on right click) for exporting to SVG or PNG
        PopupMenu menu = new PopupMenu();
        canvas.asComponent().add(menu);
        canvas.asComponent().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if(SwingUtilities.isRightMouseButton(e))
                    menu.show(canvas.asComponent(), e.getX(), e.getY());
            }
        });
        MenuItem svgExport = new MenuItem("SVG export");
        svgExport.addActionListener(e->{
            Document svg = SVGUtils.containerToSVG(frame.getContentPane());
            SVGUtils.documentToXMLFile(svg, new File("new_text_test.svg"));
            System.out.println("exported SVG.");
        });
        menu.add(svgExport);

        MenuItem pdfExport = new MenuItem("PDF export");
        pdfExport.addActionListener(e->{
            try {
                PDDocument pdf = PDFUtils.containerToPDF(frame.getContentPane());
                pdf.save("new_text_test.pdf");
                pdf.close();
                System.out.println("exported PDF.");
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }

        });
        menu.add(pdfExport);

        MenuItem pngExport = new MenuItem("PNG export");
        pngExport.addActionListener(e->{
            Img img = new Img(frame.getContentPane().getSize());
            img.paint(g -> frame.getContentPane().paintAll(g));
            ImageSaver.saveImage(img.getRemoteBufferedImage(), "new_text_test.png");
            System.out.println("exported PNG.");
        });
        menu.add(pngExport);

    }
}