package hageldave.jplotter.debugging.controlHandler.annotations;

import hageldave.jplotter.debugging.controlHandler.PanelCreator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// TODO: Maybe different annotation for Getter & Setter
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD})
public @interface CreateElement {
    public String key();
    public Class<? extends PanelCreator> creator();
}
