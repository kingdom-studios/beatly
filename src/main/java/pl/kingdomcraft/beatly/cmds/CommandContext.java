package pl.kingdomcraft.beatly.cmds;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.List;

public record CommandContext(MessageReceivedEvent event, List<String> args) {

    public MessageChannel channel() {
        return event.getChannel();
    }

    public Guild guild() {
        return event.getGuild();
    }

    public Member member() {
        return event.getMember();
    }
}
