package hageldave.jplotter.debugging.customPrinter;

public class StandardPrinter implements CustomPrinterInterface {
    @Override
    public String print(Object object, String variableName) {
        return object.toString();
    }
}
