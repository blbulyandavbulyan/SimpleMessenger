package threads;

import java.io.*;
import java.net.SocketException;
import java.util.Objects;

import general.message.Message;
import common.Server;
import general.message.servermessages.ServerErrorMessage;

public class ClientProcessingServerThread extends ClientServerThread{
    private final String clientName;

    public ClientProcessingServerThread(ClientServerThread clientServerThread, String clientName){
        super(clientServerThread);
        this.clientName = clientName;
        start();
    }
    synchronized public void sendMessage(Message msg) throws IOException {
        if(!isTerminated()){
            clientObjOut.writeObject(msg);
        }
    }
    public void run(){
        try{
            clientObjOut.writeUTF("WELCOME TO SERVER!");
            clientObjOut.flush();
            Server.serverPrint("Поток для клиента %s запущен\n".formatted(clientName));
            while(!isTerminated()){
                try{
                    Message msg = (Message) clientObjIn.readObject();
                    if(!msg.getSender().equals(clientName)){
                        sendMessage(new ServerErrorMessage(clientName, msg.getSender(), ServerErrorMessage.ServerErrorCode.YOUR_USERNAME_ON_SERVER_AND_IN_YOUR_MESSAGE_ARE_NOT_EQUAL));
                    }
                    else if(msg.getReceiver()!= null){
                        String msgReceiver = msg.getReceiver();
                        if(msgReceiver.equalsIgnoreCase("SERVER")){
                            //todo проверить является ли msg экземпляром ServerCommand, если да, то приступить к её обработке
                            //if()
                        }
                        else if(!Objects.equals(msgReceiver, clientName)){
                            if(Server.IsClientExists(msgReceiver)) Server.getClient(msgReceiver).sendMessage(msg);
                            else sendMessage(new ServerErrorMessage(clientName, msgReceiver, ServerErrorMessage.ServerErrorCode.MESSAGE_DELIVERY_ERROR_NO_USER_WITH_THIS_NAME));
                        }
                    }
                    else{
                        for (ClientProcessingServerThread client : Server.getClients()) {
                            if(client != this)client.sendMessage(msg);
                        }
                    }
                }
                catch(ClassNotFoundException | ClassCastException e){

                    e.printStackTrace();
                }
            }
        }
        catch (SocketException e){
            if(!(e.getMessage().equals("Socket closed") && isTerminated())){
                e.printStackTrace();
            }
        }
        catch(EOFException  e){
            Server.serverPrint("Пользователь %s отключился.\n".formatted(clientName));
        }
        catch (IOException e){
            e.printStackTrace();
        }
        finally {
            Server.removeClient(clientName);
        }
    }
    public String getClientName() {
        return clientName;
    }
}
