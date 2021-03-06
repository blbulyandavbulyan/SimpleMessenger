package serverconnection;

import general.loginorregisterrequest.LoginOrRegisterRequest;
import common.interfaces.MessageGetter;
import common.interfaces.MessageSender;
import common.interfaces.StatusMessagePrinter;
import general.message.Message;
import serverconnection.exceptions.ConnectionClosed;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Objects;

import serverconnection.exceptions.RegisterOrLoginInterrupted;
import serverconnection.exceptions.ServerHasDBProblemsException;
import serverconnection.exceptions.WrongAnswerFromServer;
import userdata.*;
public class ServerConnection implements MessageGetter, MessageSender {
    private String userName;
    private final Socket socket;
    private ObjectOutputStream serverObjOut;
    private ObjectInputStream serverObjIn;
    private StatusMessagePrinter statusMessagePrinter;
    private LoginOrRegisterResultGetter loginOrRegisterResultGetter;
    private int timeout;
    public ServerConnection(InetSocketAddress serverAddress, int timeout, LoginOrRegisterResultGetter loginOrRegisterResultGetter, StatusMessagePrinter statusMessagePrinter) throws IOException {
        this.statusMessagePrinter = Objects.requireNonNullElseGet(statusMessagePrinter, () -> System.out::println);
        this.timeout = timeout;
        this.statusMessagePrinter.printStatusMessage("Создание сокета...");
        this.socket = new Socket();
        try{
            this.statusMessagePrinter.printStatusMessage("Подключение к серверу...");
            socket.connect(serverAddress, timeout);
            this.statusMessagePrinter.printStatusMessage("Регистрация пользователя...");
            this.statusMessagePrinter.printStatusMessage("Инициализация потока объектов для проведения регистрации/входа и отправки сообщений...");
            initObjStreams();
            this.loginOrRegisterResultGetter = loginOrRegisterResultGetter;
            loginOrRegister(loginOrRegisterResultGetter);
            //если мы здесь, значит пользователь уже зарегистрирован
            this.statusMessagePrinter.printStatusMessage("Создание завершено.");
        }
        catch(Throwable e) {
            if(!socket.isClosed()) socket.close();
            throw e;
        }

    }
    private  void initObjStreams() throws IOException {
        this.statusMessagePrinter.printStatusMessage("Создание выходного потока объектов...");
        serverObjOut = new ObjectOutputStream(socket.getOutputStream());
        this.statusMessagePrinter.printStatusMessage("Создание выходного потока объектов завершено.");
        this.statusMessagePrinter.printStatusMessage("Создание входного потока объектов...");
        serverObjIn = new ObjectInputStream(socket.getInputStream());
        this.statusMessagePrinter.printStatusMessage("Создание входного потока объектов завершено.");
    }
    private void loginOrRegister() throws IOException {
        loginOrRegister(this.loginOrRegisterResultGetter);
    }
    private void loginOrRegister(LoginOrRegisterResultGetter loginOrRegisterResultGetter) throws IOException {
        LoginOrRegisterRequest lor = loginOrRegisterResultGetter.getLoginOrRegisterResult(LoginOrRegisterResultGetter.ActionCode.GET);
        if(lor == null)
            throw new RegisterOrLoginInterrupted("Регистрация прервана пользователем, подключение не будет создано.");
        final String readyMessage = "WELCOME TO SERVER!";
        final String userAlreadyExists = "USER ALREADY EXISTS";
        final String invalidLoginOrPassword = "INVALID LOGIN OR PASSWORD";
        final String registrationOrLoginFailed = "SERVER HAS DataBase problem";
        while(true){
            if(lor == null || lor.getOperation() == LoginOrRegisterRequest.OperationType.CANCELLED)
                throw new RegisterOrLoginInterrupted("Процедура регистрации/логина прервана пользователем, подключение не будет создано.");
            statusMessagePrinter.printStatusMessage(String.format(
                    "Ожидание ответа от сервера о успешной процедуре %s...",
                    lor.getOperation() == LoginOrRegisterRequest.OperationType.REGISTER ? "регистрации" : "входа"));
            serverObjOut.writeObject(lor);
            String answerFromServer = serverObjIn.readUTF();
            switch (answerFromServer){
                case userAlreadyExists -> lor = loginOrRegisterResultGetter.getLoginOrRegisterResult(LoginOrRegisterResultGetter.ActionCode.GET_NEW_USERNAME_BECAUSE_OLD_IS_ALREADY_REGISTERED);
                case invalidLoginOrPassword -> lor = loginOrRegisterResultGetter.getLoginOrRegisterResult(LoginOrRegisterResultGetter.ActionCode.GET_BECAUSE_INVALID_LOGIN_OR_PASSWORD);

                case readyMessage -> {
                    statusMessagePrinter.printStatusMessage(lor.getOperation() == LoginOrRegisterRequest.OperationType.REGISTER ? "Вы зарегистрированы." : "Вы вошли.");
                    userName = lor.getUserName();
                    return;
                }
                case registrationOrLoginFailed -> throw new ServerHasDBProblemsException(String.format(
                                "У сервера проблемы с базой данных. Процедура %s провалена.",
                                lor.getOperation() == LoginOrRegisterRequest.OperationType.REGISTER ? "регистрации" : "входа"
                        ));
                default -> throw new WrongAnswerFromServer(String.format("Неожиданный ответ от сервера, от сервера, пришло: %s\n Завершение.\n", answerFromServer));
            }
        }
    }
    public void sendMessage(Message msg) throws IOException{
        if(isClosed())throw new ConnectionClosed("Ошибка отправки сообщения, подключение закрыто.");
        serverObjOut.writeObject(msg);
    }
    public Message getMessage() throws IOException, ClassNotFoundException {
        if(isClosed())throw new ConnectionClosed("Ошибка получения сообщения, подключение закрыто.");
        return (Message) serverObjIn.readObject();
    }
    public String getUserName(){
        return userName;
    }
    public boolean isOpen(){
        return !socket.isClosed();
    }
    public boolean isClosed(){
        return socket.isClosed();
    }

    public void close() throws IOException {
        if(!isClosed()){
            socket.close();
        }
    }
    public void reconnect() throws IOException {
        try{
            this.close();
        }
        catch (IOException ignored){

        }
        socket.connect(socket.getRemoteSocketAddress(), timeout);
        initObjStreams();
        loginOrRegister();
        this.statusMessagePrinter.printStatusMessage("Переподключение завершено");
    }
}
