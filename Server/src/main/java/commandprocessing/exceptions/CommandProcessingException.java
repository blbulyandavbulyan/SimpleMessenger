package commandprocessing.exceptions;

import common.exceptions.ServerException;
import general.message.servercommand.ServerCommand;

public class CommandProcessingException extends ServerException {
    private ServerCommand serverCommand;
    public CommandProcessingException(ServerCommand serverCommand){
        this.serverCommand = serverCommand;
    }
    public CommandProcessingException(String msg) {
        super(msg);
    }
    public CommandProcessingException(){

    }
}