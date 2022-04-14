package hageldave.jplotter.debugging.controlHandler;

import hageldave.jplotter.canvas.JPlotterCanvas;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Objects;
import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;

public class FieldHandler {
    // TODO: Merge these if possible / or if merging causes problems (if we want to control stroke pattern of renderable, but not of renderer for example) the split it completely, by using polymorphy
    final static String[] toControlRenderable = new String[]{"globalThicknessMultiplier", "globalSaturationMultiplier", "globalAlphaMultiplier", "hidden", "globalScaling", "glyph", "strokeLength", "strokePattern", "txtStr", "color", "background", "origin", "angle"};
    final static String[] toControlRenderer = new String[]{"isEnabled", "paddingLeft", "paddingRight", "paddingTop", "paddingBot", "legendRightWidth", "legendBottomHeight", "guideColor", "tickColor", "textColor"};

    final static String[] toDisplayRenderable = new String[]{"isDirty", "useVertexRounding", "isGLDoublePrecision", "useAAinFallback", "useCrispEdgesForSVG", "numEffectiveSegments", "pickColor", "textSize", "fontsize", "style", "segments", "points", "triangles", "curves"};
    final static String[] toDisplayRenderer = new String[]{"strokePattern", "view", "itemToRender", "isGLDoublePrecisionEnabled", "orthoMX", "coordsysAreaRT", "coordsysAreaRB", "coordsysAreaLT", "coordsysAreaLB", "currentViewPort", "tickMarkLabels", "tickMarkGenerator", "colorScheme", "renderOrder"};

    HashMap<String, Method> field2guiElementMethod = new HashMap<>();

    // TODO: all fields should land here (without prefiltering before), if the field is in field2guiEL then draw the gui elements
    // TODO: if it isn't look if it should be drawn at all
    // TODO: only do that later when all GUI elements are created (so we have a demo how it will look later)
    public JPanel handleField(JPlotterCanvas canvas, Object obj, Field field) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        JPanel labelContainer = new JPanel();
        labelContainer.setLayout(new FlowLayout(FlowLayout.LEFT));
        labelContainer.setAlignmentX(Component.LEFT_ALIGNMENT);
        Object fieldValue = field.get(obj);

        labelContainer.add(new JLabel(("(" + field.getType()) + ") "));
        JLabel fieldName = new JLabel((field.getName()) + ": ");
        fieldName.setFont(new Font(fieldName.getFont().getName(), Font.BOLD, fieldName.getFont().getSize()));
        labelContainer.add(fieldName);

        if (field2guiElementMethod.containsKey(field.getName())) {
            Method m = field2guiElementMethod.get(field.getName());
            m.invoke(null, canvas, obj, labelContainer);
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
                    if (arrLen < 6) {
                        StringBuilder allArrElements = new StringBuilder();
                        for (int i = 0; i < arrLen; i++) {
                            allArrElements.append(Array.get(fieldValue, i)).append(", ");
                        }
                        labelContainer.add(new JLabel(allArrElements.toString()));
                    } else {
                        labelContainer.add(new JLabel(String.valueOf((Array.getLength(fieldValue)))));
                    }
                } else {
                    labelContainer.add(new JLabel(String.valueOf(fieldValue)));
                }
            }
        }

        labelContainer.setPreferredSize(new Dimension((int) labelContainer.getPreferredSize().getWidth(), 1));
        return labelContainer;
    }


    public void registerGUIElement(String field, Method m) {
        this.field2guiElementMethod.put(field, m);
    }

    public static boolean displayInControlArea(String fieldName) {
        return Arrays.asList(toControlRenderable).contains(fieldName) || Arrays.asList(toControlRenderer).contains(fieldName);
    }

    public static boolean displayInInformationArea(String fieldName) {
        return Arrays.asList(toDisplayRenderable).contains(fieldName) || Arrays.asList(toDisplayRenderer).contains(fieldName);
    }
}
