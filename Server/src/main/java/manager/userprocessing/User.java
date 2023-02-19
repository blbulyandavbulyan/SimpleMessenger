package manager.userprocessing;

import general.message.servercommand.ServerCommand;
import manager.groupprocessing.Group;

import java.util.HashSet;
import java.util.Set;

public class User {
    private final String userName;
    private int rank;
    private Group group;
    private Set<ServerCommand.Command> allowedCommands;
    public User(String userName, Group group, int userRank){
        this.userName = userName;
        this.group = group;
        this.rank = userRank;
        allowedCommands = new HashSet<>();
    }

    public String getUserName() {
        return userName;
    }

    public Group getGroup() {
        return group;
    }
    public int getRank(){
        return rank + (group != null ? group.getRank() : 0);
    }
    public boolean canExecute(ServerCommand.Command command){
        return allowedCommands.contains(command);

    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public void setGroup(Group group) {
        this.group = group;
    }
    public void allowExecute(ServerCommand.Command command){
        allowedCommands.add(command);
    }
}