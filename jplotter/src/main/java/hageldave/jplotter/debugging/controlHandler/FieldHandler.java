package hageldave.jplotter.debugging.controlHandler;

import hageldave.jplotter.canvas.JPlotterCanvas;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Objects;
import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;

public class FieldHandler {
    final static String[] toControlRenderable = new String[]{"globalThicknessMultiplier", "globalSaturationMultiplier", "globalAlphaMultiplier", "hidden", "globalScaling", "glyph", "strokeLength", "strokePattern", "txtStr", "color", "background", "origin", "angle"};
    final static String[] toControlRenderer = new String[]{"isEnabled", "paddingLeft", "paddingRight", "paddingTop", "paddingBot", "legendRightWidth", "legendBottomHeight", "guideColor", "tickColor", "textColor"};

    //final static String[] toDisplayRenderable = new String[]{"useVertexRounding", "isGLDoublePrecision", "useAAinFallback", "useCrispEdgesForSVG",
    // "numEffectiveSegments", "pickColor", "textSize", "fontsize", "style", "segments", "points", "triangles", "curves"};
    //final static String[] toDisplayRenderer = new String[]{"strokePattern", "view", "itemToRender", "isGLDoublePrecisionEnabled", "orthoMX",
    // "coordsysAreaRT", "coordsysAreaRB", "coordsysAreaLT", "coordsysAreaLB", "currentViewPort", "tickMarkLabels", "tickMarkGenerator", "colorScheme", "renderOrder"};

    HashMap<String, PanelCreator> field2panelcreator = new HashMap<>();

    // TODO: all fields should land here (without prefiltering before), if the field is in field2guiEL then draw the gui elements
    // TODO: if it isn't look if it should be drawn at all (toDisplay)
    // TODO: only do that later when all GUI elements are created (so we have a demo how it will look later)
    public JPanel handleField(JPlotterCanvas canvas, Object obj, Field field) throws IllegalAccessException {
        JPanel labelContainer = new JPanel();
        labelContainer.setLayout(new BoxLayout(labelContainer, BoxLayout.X_AXIS));
        labelContainer.setAlignmentX(Component.LEFT_ALIGNMENT);
        labelContainer.setBorder(new EmptyBorder(9, 0, 9, 0));
        labelContainer.setBackground(new Color(225, 225, 225));
        Object fieldValue = field.get(obj);

        labelContainer.add(new JLabel(("(" + field.getType().getSimpleName()) + ") "));
        JLabel fieldName = new JLabel((field.getName()) + ": ");
        fieldName.setFont(new Font(fieldName.getFont().getName(), Font.BOLD, fieldName.getFont().getSize()));
        labelContainer.add(fieldName);

        if (field2panelcreator.containsKey(field.getName())) {
            field2panelcreator.get(field.getName()).createUnchecked(canvas, obj, labelContainer);

            // TODO: get the most specific class
            /*for (Map.Entry<Class<?>, PanelCreator> e : field2panelcreator.get(field.getName()).entrySet()) {
                if (e.getKey().isAssignableFrom(obj.getClass())) {
                    field2panelcreator.get(field.getName()).get(e.getKey()).createUnchecked(canvas, obj, labelContainer);
                    break;
                }
            }*/

        } else {
            if (Objects.nonNull(fieldValue)) {
                if (DoubleSupplier.class.isAssignableFrom(fieldValue.getClass())) {
                    labelContainer.add(new JLabel(String.valueOf(((DoubleSupplier) fieldValue).getAsDouble())));
                } else if (IntSupplier.class.isAssignableFrom(field.getType())) {
                    labelContainer.add(new JLabel(String.valueOf(((IntSupplier) fieldValue).getAsInt())));
                } else if (Collection.class.isAssignableFrom(fieldValue.getClass())) {
                    labelContainer.add(new JLabel(((Collection<?>) fieldValue).size() + " element(s)"));
                } else if (fieldValue.getClass().isArray()) {
                    int arrLen = Array.getLength(fieldValue);
                    labelContainer.add(new JLabel(String.valueOf(arrLen)));
                } else {
                    labelContainer.add(new JLabel(String.valueOf(fieldValue)));
                }
            }
        }
        return labelContainer;
    }

    public static interface PanelCreator {
        JPanel create(JPlotterCanvas canvas, Object obj, JPanel labelContainer) throws Exception;

        default JPanel createUnchecked(JPlotterCanvas canvas, Object obj, JPanel labelContainer) {
            try {
                return create(canvas, obj, labelContainer);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void addPanelCreator(String fieldName, PanelCreator p) {
        this.field2panelcreator.put(fieldName, p);
    }

    public void removePanelCreator(String fieldName) {
        if (!this.field2panelcreator.containsKey(fieldName)) {
            throw new RuntimeException("There is no registered component for " + fieldName + ".");
        }
        this.field2panelcreator.remove(fieldName);
    }

    public static boolean displayInControlArea(String fieldName) {
        return Arrays.asList(toControlRenderable).contains(fieldName) || Arrays.asList(toControlRenderer).contains(fieldName);
    }

    /*public static boolean displayInInformationArea(String fieldName) {
        return Arrays.asList(toDisplayRenderable).contains(fieldName) || Arrays.asList(toDisplayRenderer).contains(fieldName);
    }*/
}