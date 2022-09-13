package hageldave.jplotter.debugging.treemodel;

import javax.swing.tree.DefaultMutableTreeNode;

/**
 * The DebuggerMutableTreeNode class is an extension of the DefaultMutableTreeNode class.
 * It is modified to store two entries, instead of just one. The hiddenObject won't be shown in a JTree.
 */
public class DebuggerMutableTreeNode extends DefaultMutableTreeNode {
    protected Object hiddenObject;

    /**
     * Creates a DebuggerMutableTreeNode instance.
     *
     * @param userObject the first entry, which will be shown in a regular JTree
     * @param hiddenObject the second entry, which will be hidden
     */
    public DebuggerMutableTreeNode(Object userObject, Object hiddenObject) {
        super(userObject, true);
        this.hiddenObject = hiddenObject;
    }

    /**
     * @return the hiddenObject, which won't be shown in a JTree
     */
    public Object getHiddenObject() {
        return hiddenObject;
    }

    /**
     * Sets the hiddenObject
     *
     * @param hiddenObject the hiddenObject, which won't be shown in a JTree
     */
    public void setHiddenObject(Object hiddenObject) {
        this.hiddenObject = hiddenObject;
    }
}
