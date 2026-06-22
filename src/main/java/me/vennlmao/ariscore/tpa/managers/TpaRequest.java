package me.vennlmao.ariscore.tpa.managers;

import org.bukkit.entity.Player;

public class TpaRequest {

    public enum Type { TPA, TPAHERE }

    private final Player sender;
    private final Player receiver;
    private final Type type;

    public TpaRequest(Player sender, Player receiver, Type type) {
        this.sender = sender;
        this.receiver = receiver;
        this.type = type;
    }

    public Player getSender() {
        return sender;
    }

    public Player getReceiver() {
        return receiver;
    }

    public Type getType() {
        return type;
    }
}
