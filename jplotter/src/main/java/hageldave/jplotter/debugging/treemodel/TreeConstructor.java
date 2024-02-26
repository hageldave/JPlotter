package hageldave.jplotter.debugging.treemodel;

import hageldave.jplotter.canvas.JPlotterCanvas;
import hageldave.jplotter.renderables.Renderable;
import hageldave.jplotter.renderers.Renderer;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;

/**
 * The Debugger class contains the static internal methods for creating
 * the tree model of the renderers &amp; renderables shown on the canvas.
 */
public class TreeConstructor {

    /**
     * The getAllRenderersOnCanvas method is responsible for constructing
     * the tree model of the rendering structure.
     *
     * @param canvas where renderables are rendered in: root of the tree structure
     * @return tree node of canvas with renderer &amp; renderable nodes appended
     */
    public static TreeNode getAllRenderersOnCanvas(JPlotterCanvas canvas) {
        Renderer rootRenderer = canvas.getRenderer();
        Class<?> canvasClass = canvas.getClass();
        Field[] fields = canvasClass.getDeclaredFields();
        CoupledMutableTreeNode root = new CoupledMutableTreeNode(canvas.getClass().getSimpleName(), canvas);

        for (Field field : fields) {
            Class<?> fieldType = field.getType();
            field.setAccessible(true);
            boolean rendererAssignable = Renderer.class.isAssignableFrom(fieldType);

            if (rendererAssignable) {
                CoupledMutableTreeNode firstRendererNode = new CoupledMutableTreeNode("(" + rootRenderer.getClass().getSimpleName() + ") " + field.getName(), rootRenderer);
                root.add(firstRendererNode);
                constructTree(rootRenderer, firstRendererNode);
            }
        }
        return root;
    }

    protected static TreeNode constructTree(Renderer rootRenderer, DefaultMutableTreeNode node) {
        Class<? extends Renderer> rootRenClass = rootRenderer.getClass();
        Class<?> rootRenSuperclass = rootRenClass.getSuperclass();

        Field[] fields = rootRenClass.getDeclaredFields();
        Field[] superclassFields = rootRenSuperclass.getDeclaredFields();

        ArrayList<Field> allFields = new ArrayList<>();
        Collections.addAll(allFields, fields);
        Collections.addAll(allFields, superclassFields);

        for (Field field : allFields) {
            Class<?> fieldType = field.getType();
            field.setAccessible(true);

            boolean isRendererAssignable = Renderer.class.isAssignableFrom(fieldType);
            boolean isRenderableAssignable = Renderable.class.isAssignableFrom(fieldType);
            boolean isIterable = java.util.List.class.isAssignableFrom(fieldType);

            if (isRendererAssignable) {
                try {
                    Renderer nestedRenderer = (Renderer) field.get(rootRenderer);
                    // null check, as renderers might be null in coordsysrenderer
                    if (nestedRenderer != null) {
                        CoupledMutableTreeNode newRendererNode = new CoupledMutableTreeNode("(" + nestedRenderer.getClass().getSimpleName() + ") " + field.getName(), nestedRenderer);
                        node.add(newRendererNode);
                        constructTree(nestedRenderer, newRendererNode);
                    }
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            } else if (isRenderableAssignable) {
                try {
                    Renderable nestedRenderable = (Renderable) field.get(rootRenderer);
                    node.add(
                            new CoupledMutableTreeNode("(" + nestedRenderable.getClass().getSimpleName() + ") " + field.getName(), nestedRenderable));
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            } else if (isIterable) {
                try {
                    java.util.List<?> iterableField = (java.util.List<?>) field.get(rootRenderer);

                    if (!iterableField.isEmpty()) {
                        DefaultMutableTreeNode listNode = new DefaultMutableTreeNode(field.getName());

                        for (Object iteratedObject : iterableField) {
                            Class<?> iteratedClass = iteratedObject.getClass();
                            if (Renderable.class.isAssignableFrom(iteratedClass)) {
                                listNode.add(
                                        new CoupledMutableTreeNode("(" + iteratedClass.getSimpleName() + ") Hashcode: @" + iteratedObject.hashCode(), iteratedObject));
                            }
                        }
                        node.add(listNode);
                    }
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return node;
    }
}