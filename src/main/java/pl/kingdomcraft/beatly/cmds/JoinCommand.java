package pl.kingdomcraft.beatly.cmds;

import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;

public class JoinCommand implements Command {
    @Override public String name() { return "join"; }

    @Override
    public void execute(CommandContext ctx) {
        var member = ctx.member();
        if (member == null) {
            ctx.channel().sendMessage("To działa tylko na serwerze.").queue();
            return;
        }

        var vs = member.getVoiceState();
        if (vs == null || !vs.inAudioChannel()) {
            ctx.channel().sendMessage("Wejdź najpierw na voice.").queue();
            return;
        }

        var channel = (VoiceChannel) vs.getChannel();
        ctx.guild().getAudioManager().openAudioConnection(channel);
        ctx.channel().sendMessage("Dołączono na: **" + channel.getName() + "**").queue();
    }
}
