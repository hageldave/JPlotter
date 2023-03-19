package hageldave.jplotter;


import hageldave.imagingkit.core.Img;
import hageldave.imagingkit.core.io.ImageSaver;
import hageldave.jplotter.canvas.BlankCanvas;
import hageldave.jplotter.canvas.BlankCanvasFallback;
import hageldave.jplotter.canvas.JPlotterCanvas;
import hageldave.jplotter.font.CharacterAtlas;
import hageldave.jplotter.interaction.CoordSysPanning;
import hageldave.jplotter.pdf.PDFUtils;
import hageldave.jplotter.renderables.*;
import hageldave.jplotter.renderers.CompleteRenderer;
import hageldave.jplotter.renderers.CoordSysRenderer;
import hageldave.jplotter.svg.SVGUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.w3c.dom.Document;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;

public class TestTextHeights {

    @SuppressWarnings("resource" /* compiler is too dumb to realize there is no leak */)
    public static void main(String[] args) {
        int fontsize = 19;
//        int xPos = 10;
        int fontstyle = Font.PLAIN;

//        int coordHeight = 150;
//        double correctionFactor = 1.0 / (345) * coordHeight + 28;

        Lines textLines = new Lines();
        FontMetrics fm = CharacterAtlas.getFontMetrics(fontsize, fontstyle);
        CoordSysRenderer coordsys = new CoordSysRenderer();



        // Testing baseline height calculation
//        NewText textBaseline = new NewText("Testing text baseline", fontsize, fontstyle);
//        textBaseline.setOrigin(xPos, 10);
//
//        double textBaselineHeight = textBaseline.getTextSize().getHeight() * correctionFactor;
//        double baselineHeight = textBaseline.getBaselineHeight(fm);
//        textLines.addSegment(
//                new Point2D.Double(textBaseline.getOrigin().getX(), textBaseline.getOrigin().getY() + baselineHeight / textBaselineHeight),
//                new Point2D.Double(textBaseline.getOrigin().getX()+textBaseline.getBounds().getWidth(), textBaseline.getOrigin().getY() + baselineHeight / textBaselineHeight)
//                ).setColor(Color.RED);
//
//        // Testing median height calculation
//        NewText textMedian = new NewText("Testing text median", fontsize, fontstyle);
//        textMedian.setOrigin(xPos, 20);
//
//        double textMedianHeight = textMedian.getTextSize().getHeight() * correctionFactor;
//        double medianHeight = textMedian.getMedianHeight(fm);
//        textLines.addSegment(
//                new Point2D.Double(textMedian.getOrigin().getX(), textMedian.getOrigin().getY() + medianHeight/textMedianHeight),
//                new Point2D.Double(textMedian.getOrigin().getX()+textMedian.getBounds().getWidth(), textMedian.getOrigin().getY() + medianHeight/textMedianHeight)
//        ).setColor(Color.BLUE);
//
//        // Testing descent height calculation
//        NewText textDescent = new NewText("Testing text descent", fontsize, fontstyle);
//        textDescent.setOrigin(xPos, 30);
//
//        double textDescentHeight = textDescent.getTextSize().getHeight() * correctionFactor;
//        double descentHeight = textDescent.getDescentHeight();
//        textLines.addSegment(
//                new Point2D.Double(textDescent.getOrigin().getX(), textDescent.getOrigin().getY() + descentHeight / textDescentHeight),
//                new Point2D.Double(textDescent.getOrigin().getX()+textDescent.getBounds().getWidth(), textDescent.getOrigin().getY() + descentHeight / textDescentHeight)
//        ).setColor(Color.GREEN);
//
//        // Testing text height calculation
//        NewText textHeight = new NewText("Testing text height", fontsize, fontstyle);
//        textHeight.setOrigin(xPos, 40);
//
//        double textHeightHeight = textHeight.getTextSize().getHeight() * correctionFactor;
//        textLines.addSegment(
//                new Point2D.Double(textHeight.getOrigin().getX(), textHeight.getOrigin().getY() + textHeightHeight),
//                new Point2D.Double(textHeight.getOrigin().getX()+textHeight.getTextSize().getWidth(), textHeight.getOrigin().getY() + textHeightHeight)
//        ).setColor(Color.MAGENTA);
//
//
//        // Testing old text height calculation
//        NewText oldTextHeight = new NewText("Testing strikethrough text height", fontsize, fontstyle);
//        oldTextHeight.setOrigin(xPos, 50);
//
//        double strikethroughTextHeight = oldTextHeight.getBounds().getHeight() * correctionFactor;
//        double strikethroughHeight = oldTextHeight.getStrikethroughHeight(fm);
//        textLines.addSegment(
//                new Point2D.Double(oldTextHeight.getOrigin().getX(), oldTextHeight.getOrigin().getY() + strikethroughHeight / strikethroughTextHeight),
//                new Point2D.Double(oldTextHeight.getOrigin().getX()+oldTextHeight.getTextSize().getWidth(), oldTextHeight.getOrigin().getY() + strikethroughHeight / strikethroughTextHeight)
//        ).setColor(Color.PINK);


        // Test text features

        // Test anchor points
        NewText anchorpoints = new NewText("testing the anchorpoint \n (2, 2)", fontsize, fontstyle);
        anchorpoints.setOrigin(0, 100);
        anchorpoints.setBackground(Color.ORANGE);
        anchorpoints.setPositioningRectangle(new PositioningRectangle(2, 2));

        // Test text decoration
        NewText textUnderline = new NewText("testing the underlining", fontsize, fontstyle);
        textUnderline.setOrigin(0, 120);
        textUnderline.setColor(Color.RED);
        textUnderline.setTextDecoration(TextDecoration.UNDERLINE);
        textUnderline.setPositioningRectangle(new PositioningRectangle(2, 2));

        NewText textStrikeThrough = new NewText("testing the underlining", fontsize, fontstyle);
        textStrikeThrough.setOrigin(0, 140);
        textStrikeThrough.setColor(Color.BLUE);
        textStrikeThrough.setTextDecoration(TextDecoration.STRIKETHROUGH);
        textStrikeThrough.setPositioningRectangle(new PositioningRectangle(2, 2));

        // Test text insets with background
        NewText textInsets = new NewText("testing the insets \n with multiple lines", fontsize, fontstyle);
        textInsets.setOrigin(0, 60);
        textInsets.setBackground(Color.MAGENTA);
        textInsets.setTextDecoration(TextDecoration.UNDERLINE);
        textInsets.setInsets(new Insets(10, 10, 10, 10));
        textInsets.setPositioningRectangle(new PositioningRectangle(2, 2));

        // Test text insets with background
        NewText textInsetsWithRotation = new NewText("testing the insets \n with rotation", fontsize, fontstyle);
        textInsetsWithRotation.setOrigin(0, 30);
        textInsetsWithRotation.setBackground(Color.GREEN);
        textInsetsWithRotation.setInsets(new Insets(10, 10, 10, 10));
        textInsetsWithRotation.setAngle(0.2);
        textInsetsWithRotation.setPositioningRectangle(new PositioningRectangle(2, 2));

        // Testing latex rendering in text mode
        NewText textLatex = new NewText("##BEGINLATEX## \\text{testing the latex rendering} \\\\ \\text{in text mode}", fontsize, fontstyle);
        textLatex.setOrigin(0, 200);
        textLatex.setBackground(Color.ORANGE);
        textLatex.setTextDecoration(TextDecoration.UNDERLINE);
        textLatex.setInsets(new Insets(5, 10, 5, 10));
        textLatex.setPositioningRectangle(new PositioningRectangle(2, 2));

        // Testing latex rendering in math mode
        NewText textLatexMath = new NewText("##BEGINLATEX## ", fontsize, fontstyle);
        textLatexMath.setOrigin(0, 200);
        textLatexMath.setBackground(Color.ORANGE);
        textLatexMath.setTextDecoration(TextDecoration.UNDERLINE);
        textLatexMath.setInsets(new Insets(5, 10, 5, 10));
        textLatexMath.setPositioningRectangle(new PositioningRectangle(2, 2));

        // okay we're good to go, lets display the data in a coordinate system
        CompleteRenderer content = new CompleteRenderer();
        coordsys.setContent(content
                .addItemToRender(textLines)
                .addItemToRender(anchorpoints)
                .addItemToRender(textUnderline)
                .addItemToRender(textStrikeThrough)
                .addItemToRender(textInsets)
                .addItemToRender(textInsetsWithRotation)
                .addItemToRender(textLatex)
                .addItemToRender(textLatexMath));
        // lets set the coordinate view to cover the whole sampling space
        coordsys.setCoordinateView(-600, 0, 100, 220);

        // lets add a legend so the viewer can make sense of the data
        Legend legend = new Legend();
        coordsys.setLegendRightWidth(80);

        // display the coordinate system on a blank canvas
        boolean useOpenGL = false;
        JPlotterCanvas canvas = useOpenGL ? new BlankCanvas() : new BlankCanvasFallback();
        canvas.setRenderer(coordsys);
        // lets add some controls for exploring the data
//        new CoordSysScrollZoom(canvas,coordsys).setZoomFactor(1.7).register();
        new CoordSysPanning(canvas, coordsys).register();

        // lets put a JFrame around it all and launch
        JFrame frame = new JFrame("Example Viz");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(canvas.asComponent());
        canvas.asComponent().setPreferredSize(new Dimension(480, 400));
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
            SVGUtils.documentToXMLFile(svg, new File("text_heights_export.svg"));
            System.out.println("exported SVG.");
        });
        menu.add(svgExport);

        MenuItem pdfExport = new MenuItem("PDF export");
        pdfExport.addActionListener(e->{
            try {
                PDDocument pdf = PDFUtils.containerToPDF(frame.getContentPane());
                pdf.save("text_heights_export.pdf");
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
            ImageSaver.saveImage(img.getRemoteBufferedImage(), "text_heights_export.png");
            System.out.println("exported PNG.");
        });
        menu.add(pngExport);

    }
}

