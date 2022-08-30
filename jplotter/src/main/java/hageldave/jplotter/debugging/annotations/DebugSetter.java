package hageldave.jplotter.debugging.annotations;

import hageldave.jplotter.debugging.panelcreators.control.ControlPanelCreator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Any setter method annotated with the DebugSetter annotation will create the
 * ControlPanelCreator passed to the annotation.
 * The key of the DebugSetter is used to couple it with the corresponding DebugGetter (has to have the same key).
 * With the DebugGetter the current information of the object can be retrieved.
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface DebugSetter {
    public String key();
    public Class<? extends ControlPanelCreator> creator();
}
