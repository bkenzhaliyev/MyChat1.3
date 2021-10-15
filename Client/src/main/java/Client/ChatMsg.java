package Client;

import java.io.Serializable;

public class ChatMsg implements Serializable {
    protected String login;
    protected String msg;

    public ChatMsg(String login, String msg) {
        this.login = login;
        this.msg = msg;
    }

    @Override
    public String toString() {
        return login + "|" + msg;
    }
}
