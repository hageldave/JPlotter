package hageldave.jplotter.interaction.kml;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.HashMap;

/**
 * TODO add documentation
 */
public class KeyMaskListener extends KeyAdapter {
    protected boolean keysPressed = false;
    protected boolean noMasking = false;
    final protected HashMap<Integer, Boolean> keyPressedInMask = new HashMap<>();

    public KeyMaskListener(final int... extModifierMask) {
        for (int j : extModifierMask) {
            this.keyPressedInMask.put(j, false);
        }
    }

    public KeyMaskListener(final int extModifierMask) {
        if (extModifierMask == 0) {
            this.noMasking = true;
        } else {
            this.keyPressedInMask.put(extModifierMask, false);
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (!noMasking) {
            if (keyPressedInMask.containsKey(e.getKeyCode())) {
                keyPressedInMask.put(e.getKeyCode(), true);
            }
            boolean areKeysPressed = true;
            for (Boolean values : keyPressedInMask.values()) {
                if (!values) {
                    areKeysPressed = false;
                    break;
                }
            }
            if (areKeysPressed)
                keysPressed = true;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (!noMasking) {
            if (keyPressedInMask.containsKey(e.getKeyCode())) {
                keysPressed = false;
                keyPressedInMask.put(e.getKeyCode(), false);
            }
        }
    }

    public boolean isKeysPressed() {
        if (noMasking) {
            return true;
        }
        return keysPressed;
    }
}
