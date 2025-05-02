package Server;

import java.io.Serial;
import java.io.Serializable;

public class Message implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    public enum MessageType {
        CHAT,
        PLAYER_JOINED,
        PLAYER_LEFT,
        GAME_REQUEST,
        GAME_ACCEPT,
        GAME_DECLINE,
        GAME_MOVE,
        PLAYER_LIST,
        GAME_OVER,
        GAME_STARTED,
        AUTHENTICATION,
        REMATCH_REQUEST,
        REMATCH_ACCEPT,
        REMATCH_DECLINE
    }

    private final MessageType type;
    private final String sender;
    private final String content;
    private Object data;

    public Message(MessageType type, String sender, String content) {
        this.type = type;
        this.sender = sender;
        this.content = content;
    }

    public Message(MessageType type, String sender, String content, Object data) {
        this.type = type;
        this.sender = sender;
        this.content = content;
        this.data = data;
    }

    public MessageType getType() {
        return type;
    }

    public String getSender() {
        return sender;
    }

    public String getContent() {
        return content;
    }

    public Object getData() {
        return data;
    }

    @Override
    public String toString() {
        return sender + ": " + content;
    }
}