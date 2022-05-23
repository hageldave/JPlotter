package hageldave.jplotter.debugging.controlHandler.annotations;

import hageldave.jplotter.debugging.controlHandler.panelcreators.control.ControlPanelCreator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface DebugSetter {
    public String key();
    public Class<? extends ControlPanelCreator> creator();
}
