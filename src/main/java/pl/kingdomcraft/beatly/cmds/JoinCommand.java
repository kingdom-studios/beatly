package pl.kingdomcraft.beatly.cmds;

import pl.kingdomcraft.beatly.managers.PlayerManager;
import pl.kingdomcraft.beatly.utils.StageVoice;

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
            ctx.channel().sendMessage("Wejdź najpierw na voice/stage.").queue();
            return;
        }

        var audioManager = ctx.guild().getAudioManager();
        var gmm = PlayerManager.getInstance().getGuildMusicManager(ctx.guild());

        // Zapewniamy, że po wejściu jest cicho (nie startuje kolejka z automatu/nie słychać starej)
        if (gmm.scheduler.isPlaying()) {
            gmm.player.setPaused(true);
        }

        if (audioManager.getSendingHandler() == null) {
            audioManager.setSendingHandler(gmm.sendHandler);
        }

        var channel = vs.getChannel(); // AudioChannel (Voice albo Stage)
        StageVoice.connect(ctx.guild(), channel);

        ctx.channel().sendMessage("Dołączono na: **" + channel.getName() + "**").queue();
    }
}
