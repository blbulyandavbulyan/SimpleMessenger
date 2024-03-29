package ui.components.displayers.generators;

import ui.components.displayers.filedisplayers.FileDisplayer;
import ui.components.displayers.filedisplayers.exceptions.InvalidMIMETypeException;
import ui.components.displayers.filedisplayers.ImageFileDisplayer;
import ui.components.displayers.filedisplayers.TextFileDisplayer;

import javax.swing.*;
import java.awt.datatransfer.DataFlavor;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class FileDisplayerGenerator {
//    private final ResourceBundle rb;
    private static final Map<String, Class<? extends FileDisplayer>> mimeToClassMap = new HashMap<>();
    static {
        mimeToClassMap.put("image/png", ImageFileDisplayer.class);
        mimeToClassMap.put("image/jpeg", ImageFileDisplayer.class);
        mimeToClassMap.put("text/plain", TextFileDisplayer.class);
    }

//    public FileDisplayerGenerator(ResourceBundle rb) {
//        this.rb = rb;
//        if(rb == null)throw new NullPointerException();
//        //fixme
//        if(!rb.containsKey("menu.delete"))
//            throw new MissingResourceException("Error creation FileDisplayerGenerator", rb.getBaseBundleName() , "menu.delete");
//    }

    public static boolean isThisFormatValid(DataFlavor dataFlavor){
        return mimeToClassMap.containsKey(dataFlavor.getMimeType());
    }
    public static boolean isThisFileHasValidFormat(File file) throws IOException {
        String fileTypeMIME = Files.probeContentType(file.toPath());
        return mimeToClassMap.containsKey(fileTypeMIME);
    }
    public static FileDisplayer getFileDsiplayer(File file) throws IOException, InvalidMIMETypeException {

        String fileTypeMIME = Files.probeContentType(file.toPath());
        if(mimeToClassMap.containsKey(fileTypeMIME)){
            Class<? extends FileDisplayer> fileDisplayerClass = mimeToClassMap.get(fileTypeMIME);
            try {
                Constructor<? extends FileDisplayer> fileDisplayerConstructor = fileDisplayerClass.getConstructor(File.class);
                return fileDisplayerConstructor.newInstance(file);
            } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        else throw new InvalidMIMETypeException();
    }

    public static void main(String[] args) throws IOException {
//        JFrame jFrame = new JFrame();
//        jFrame.getContentPane().add(getFileDsiplayer(new File("/home/david/solution.txt")));
//        jFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
//        jFrame.pack();
//        jFrame.setVisible(true);
    }
}
