package hageldave.jplotter.interaction.kml;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * TODO add documentation
 */
public class KeyMaskListener extends KeyAdapter {
    protected boolean areKeysPressed = false;
    protected boolean noMasking = false;
    final protected HashMap<Integer, Boolean> keyPressedMap = new HashMap<>();
    final protected ArrayList<Integer> keysToPress = new ArrayList<>();

    public KeyMaskListener(final int... extModifierMask) {
        for (int j : extModifierMask) {
            this.keysToPress.add(j);
        }
        Collections.sort(this.keysToPress);
    }

    public KeyMaskListener(final int extModifierMask) {
        if (extModifierMask == 0) {
            this.noMasking = true;
        } else {
            this.keysToPress.add(extModifierMask);
        }
        Collections.sort(this.keysToPress);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (!noMasking) {
            keyPressedMap.put(e.getKeyCode(), true);
            areKeysPressed = this.keysToPress.equals(getPressedKeys());
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (!noMasking) {
            keyPressedMap.put(e.getKeyCode(), false);
            areKeysPressed = this.keysToPress.equals(getPressedKeys());
        }
    }

    public boolean areKeysPressed() {
        if (noMasking) {
            return true;
        }
        return areKeysPressed;
    }

    protected ArrayList<Integer> getPressedKeys() {
        ArrayList<Integer> pressedKeys = new ArrayList<>(keyPressedMap.size());
        for (Map.Entry<Integer, Boolean> v : keyPressedMap.entrySet())
            if (v.getValue())
                pressedKeys.add(v.getKey());

        Collections.sort(pressedKeys);
        return pressedKeys;
    }
}
