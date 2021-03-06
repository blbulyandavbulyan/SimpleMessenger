package ui.customuicomponents.passwordfieldwithshowpasscheckbox;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;

public class ShowUnshowPasswordPanel extends JPanel {
    private final JPasswordField passwordField;
    private final ShowUnshowCheckBox showPassword;
    private char echoChar;

    public static void main(String[] args) {
//        JFrame jFrame = new JFrame();
//        jFrame.setLayout(new BorderLayout());
//        JPanel passwordField = new ShowUnshowPasswordPanel("Enter your password");
//        jFrame.add(passwordField);
//        jFrame.setPreferredSize(new Dimension(500, 200));
//        jFrame.pack();
//        jFrame.setVisible(true);
    }
    public ShowUnshowPasswordPanel(){
        this(new JPasswordField());
    }
    public ShowUnshowPasswordPanel(JPasswordField jPasswordField){
        passwordField = jPasswordField;
        this.setLayout(new BorderLayout());
        showPassword = new ShowUnshowCheckBox();
        this.add(passwordField, BorderLayout.CENTER);
        this.add(showPassword, BorderLayout.EAST);
        showPassword.setBackground(passwordField.getBackground());
        this.setBorder(passwordField.getBorder());
        passwordField.setBorder(null);
        echoChar = passwordField.getEchoChar();
        showPassword.addItemListener(e -> {
            switch (e.getStateChange()){
                case ItemEvent.SELECTED -> {
                    passwordField.setEchoChar((char) 0);
                    passwordField.requestFocus();
                }
                case ItemEvent.DESELECTED -> {
                    passwordField.setEchoChar(echoChar);
                    passwordField.requestFocus();
                }
            }
        });
    }

    public boolean isPasswordShow(){
        return showPassword != null && showPassword.isSelected();
    }
    public JPasswordField getPasswordField() {
        return passwordField;
    }
    public ShowUnshowCheckBox getShowPassword() {
        return showPassword;
    }
}
