package ui.windows;


import audioprocessing.RecordStopButtonActionListener;
import common.interfaces.MessageSender;
import general.message.Message;
import general.message.textmessage.TextMessage;
import general.message.voicemessage.VoiceMessage;
import serverconnection.MessagesReaderThread;
import serverconnection.ServerConnection;
import common.interfaces.MessagePrinter;
import ui.customuicomponents.closedjtabbedpane.JTabbedPaneWithCloseableTabs;
import ui.exceptions.PersonalMessageIsEmpty;
import ui.customuicomponents.textfieldswithghosttext.JTextFiledWithGhostText;
import ui.messagedisplaying.messagepanels.MessagePanel;
import ui.messagedisplaying.MessagePanelGenerator;

import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.HashMap;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static ui.common.DisplayErrors.showErrorMessage;

public class MainWindow extends JFrame implements MessagePrinter {
    private JTextFiledWithGhostText messageField;
    private JButton sendBtn;
    private JTabbedPaneWithCloseableTabs dialogsTappedPane;
    private final HashMap<String, JPanel> privateDialogs = new HashMap<>();
    private MessageSender messageSender;
    private JPanel generalDialog;
    private String myUserName;
    private MessagesReaderThread readerThread;
    private final ResourceBundle rb;
    public static void main(String[] args) {
        MainWindow mw = new MainWindow(ResourceBundle.getBundle("resources/locales/guitext"));
        mw.pack();
        mw.setSize(new Dimension(500, 500));
        mw.setVisible(true);
        TextMessage msg1 = new TextMessage("Hello, i am johan", "johan", "you"),
                msg2 = new TextMessage("Hello, i am georgy", "georgy", "you");
        for (int i = 0; i < 1000; i++) {
            mw.printMessage(msg1);
            mw.printMessage(msg2);
        }
    }
    private String[] getReceiversFromMessage(String msgStr){
        final String regex = "@([\\w\\d]+)\b";
        final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
        final Matcher matcher = pattern.matcher(msgStr);
        return matcher.results().map((matchResult -> matchResult.group(1))).toArray(String[]::new);
    }
    private TextMessage getTextMessageWithReceiver(String msgStr, String senderName) throws PersonalMessageIsEmpty{
        String receiverName = null;
        String newMsgStr = msgStr;
        char[] msgChars = msgStr.toCharArray();
        if (msgChars[0] == '@') {
            StringBuilder receiverNameBuilder = new StringBuilder();
            int i = 1;
            for (; i < msgChars.length && (msgChars[i] != ',' && msgChars[i] != ' '); i++) {
                receiverNameBuilder.append(msgChars[i]);
            }
            receiverName = receiverNameBuilder.toString();
            i++;
            if(i >= msgChars.length ||
                    (newMsgStr = String.valueOf(msgChars, i, msgChars.length - i)).isBlank())
                throw new PersonalMessageIsEmpty();
        }
        else{
            int selectedDialogIndex = dialogsTappedPane.getSelectedIndex();
            if(selectedDialogIndex > 0)receiverName = dialogsTappedPane.getTitleAt(selectedDialogIndex);
        }
        return new TextMessage(newMsgStr, senderName, receiverName);
    }

    private void init() {
        messageField = new JTextFiledWithGhostText(rb.getString("mainWindow.messageFieldGhostText"));
        sendBtn = new JButton(rb.getString("mainWindow.startRecordVoiceMessage"));
        ActionListener sendMessage = (e) -> {
            String message = messageField.getText().trim();
            if (!(message.isBlank() && message.isEmpty())) {
                try {
                    TextMessage msg = getTextMessageWithReceiver(message, myUserName);
                    messageSender.sendMessage(msg);
                    printMessage(msg);
                    messageField.setText("");
                }
                catch (PersonalMessageIsEmpty ex){
                    showErrorMessage(((JComponent) e.getSource()).getParent(), "mainWindow.errorMessages.emptyPersonalMessage",
                            "mainWindow.errorCaptions.messageSendingError", rb);
                    messageField.requestFocus();
                }
                catch (IOException ex) {
                    ex.printStackTrace();
                    showErrorMessage(((JComponent) e.getSource()).getParent(),
                            (messageSender.isClosed() ? "mainWindow.errorMessages.connectionClosed" : "mainWindow.errorMessages.messageSendingUnknownError"),
                            "mainWindow.errorCaptions.messageSendingError", rb);

                    if (messageSender.isClosed()) {

                    }
                    System.exit(-1);
                }
            }
        };

        try {
            AudioFormat audioFormat =  new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100, 16, 1, 2, 44100, true);
            RecordStopButtonActionListener recordStopActionListener = new RecordStopButtonActionListener(audioFormat, 3,
                (record_state)->{
                    switch (record_state){
                        case RECORD_STARTED -> {
                            sendBtn.setText(rb.getString("mainWindow.stopRecordVoiceMessageAndSend"));
                            messageField.setEnabled(false);
                        }
                        case RECORD_STOPPED -> {
                            if(messageField.isEmpty())sendBtn.setText(rb.getString("mainWindow.startRecordVoiceMessage"));
                            else sendBtn.setText(rb.getString("mainWindow.sendButton"));
                            messageField.setEnabled(true);
                        }
                    }
                },
                messageField::isEmpty,
                (audioData)->{
                    int selectedIndex = dialogsTappedPane.getSelectedIndex();
                    VoiceMessage voiceMessage = new VoiceMessage(
                            myUserName, selectedIndex > 0 ? dialogsTappedPane.getTitleAt(selectedIndex) : null,
                            audioData, audioFormat);
                    try {
                        messageSender.sendMessage(voiceMessage);
                        printMessage(voiceMessage);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            );
            messageField.getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent e) {
                    if(!messageField.isEmpty())sendBtn.setText(rb.getString("mainWindow.sendButton"));
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    if(messageField.isEmpty())sendBtn.setText(rb.getString("mainWindow.startRecordVoiceMessage"));
                }

                @Override
                public void changedUpdate(DocumentEvent e) {

                }
            });
            sendBtn.addActionListener(recordStopActionListener);
        } catch (LineUnavailableException | IllegalArgumentException e) {
            e.printStackTrace();
            sendBtn.setText(rb.getString("mainWindow.sendButton"));
            showErrorMessage(null, "mainWindow.errorMessage.VoiceMessageIsNotSupported", "errorCaptions.generalErrorCaption", rb);
        }

        messageField.addActionListener(sendMessage);
        sendBtn.addActionListener(sendMessage);
        dialogsTappedPane = new JTabbedPaneWithCloseableTabs(privateDialogs::remove, rb.getString("mainWindow.closeDialogTooltipText"));
        generalDialog = new JPanel();
        generalDialog.setLayout(new BoxLayout(generalDialog, BoxLayout.Y_AXIS));
        dialogsTappedPane.addTab(rb.getString("mainWindow.generalChatTabname"), new JScrollPane(generalDialog));
        this.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.weighty = 1;
        gbc.weightx = 1;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(3, 3, 3, 3);
        this.add(dialogsTappedPane, gbc);
        gbc.insets = new Insets(0, 3, 3,3);
        gbc.weighty = 0.05;
        gbc.weightx = 0.99999;
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        this.add(messageField, gbc);
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.weightx = 0.00001;
        this.add(sendBtn, gbc);
    }

    public MainWindow(ServerConnection connection, ResourceBundle rb) {
        this.rb = rb;
        if (connection == null) throw new NullPointerException("connection is null");
        if (connection.isClosed()) throw new RuntimeException("connection is closed");
        init();
        this.messageSender = connection;
        this.myUserName = connection.getUserName();
        this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        this.setTitle(myUserName);
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (messageSender.isOpen()) {
                    try {
                        readerThread.terminate();
                        messageSender.close();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                        System.exit(-1);
                    }
                }
                System.exit(0);
            }
        });
        this.setMinimumSize(new Dimension(662, 378));
        this.setSize(this.getMinimumSize());
        readerThread = new MessagesReaderThread(connection, this);
    }

    private MainWindow(ResourceBundle rb) {
        this.rb = rb;
        init();
        this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    }

    synchronized public void printMessage(Message msg) {
        try {
            MessagePanel messagePanel = MessagePanelGenerator.getMessagePanel(msg);
            if (msg.getReceiver() != null) {
                String dialogName = msg.getReceiver().equals(myUserName) ?  msg.getSender() : msg.getReceiver();
                JPanel privateDialogPanel = initPrivateDialogWithUser(dialogName);
                privateDialogPanel.add(messagePanel);
                privateDialogPanel.revalidate();

            } else {
                generalDialog.add(messagePanel);
                messagePanel.addDoUserNameMuseClick(()->{
                    if(messageField.getText().isBlank() ||  messageField.getText().isEmpty()){
                        String messageFieldStr = "@" + msg.getSender() + ", ";
                        messageField.setText(messageFieldStr);
                        messageField.setCaretPosition(messageFieldStr.length());
                    }
                    else{
                        String messageFieldStr = '@' + msg.getSender() + ", " + messageField.getText().trim();
                        messageField.setText(messageFieldStr);
                        messageField.setCaretPosition(messageFieldStr.length());
                    }
                });

                generalDialog.revalidate();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    private JPanel initPrivateDialogWithUser(String username) {
        synchronized (privateDialogs){
            if (!privateDialogs.containsKey(username)) {
                JPanel privateDialogPanel = new JPanel();
                privateDialogPanel.setLayout(new BoxLayout(privateDialogPanel, BoxLayout.Y_AXIS));
                privateDialogs.put(username, privateDialogPanel);
                dialogsTappedPane.addCloseableTab(username, new JScrollPane(privateDialogPanel));
                return privateDialogPanel;
            }
            else return privateDialogs.get(username);
        }

    }
}

