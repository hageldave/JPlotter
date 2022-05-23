package hageldave.jplotter.debugging.controlHandler.annotations;

import hageldave.jplotter.debugging.controlHandler.panelcreators.display.DisplayPanelCreator;
import hageldave.jplotter.debugging.controlHandler.panelcreators.display.StandardPanelCreator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface DebugGetter {
    public String key();
    public Class<? extends DisplayPanelCreator> creator() default StandardPanelCreator.class;
}
