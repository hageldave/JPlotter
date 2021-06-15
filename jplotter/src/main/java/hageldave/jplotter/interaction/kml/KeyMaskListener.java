package hageldave.jplotter.interaction.kml;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.HashMap;

/**
 * TODO add documentation
 */
public class KeyMaskListener extends KeyAdapter {
    protected boolean keyTyped = false;
    protected boolean noMasking = false;
    final protected HashMap<Integer, Boolean> keyTypedInMask = new HashMap<>();

    public KeyMaskListener(final int... extModifierMask) {
        for (int j : extModifierMask) {
            this.keyTypedInMask.put(j, false);
        }
    }

    public KeyMaskListener(final int extModifierMask) {
        if (extModifierMask == 0) {
            this.noMasking = true;
        } else {
            this.keyTypedInMask.put(extModifierMask, false);
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (!noMasking) {
            if (keyTypedInMask.containsKey(e.getKeyCode())) {
                keyTypedInMask.put(e.getKeyCode(), true);
            }
            boolean areKeysPressed = true;
            for (Boolean values : keyTypedInMask.values()) {
                if (!values) {
                    areKeysPressed = false;
                    break;
                }
            }
            if (areKeysPressed)
                keyTyped = true;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (!noMasking) {
            if (keyTypedInMask.containsKey(e.getKeyCode())) {
                keyTyped = false;
                keyTypedInMask.put(e.getKeyCode(), false);
            }
        }
    }

    public boolean isKeyTyped() {
        if (noMasking) {
            return true;
        }
        return keyTyped;
    }
}
