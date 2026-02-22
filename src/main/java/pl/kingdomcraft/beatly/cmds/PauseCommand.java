package pl.kingdomcraft.beatly.cmds;

import pl.kingdomcraft.beatly.managers.PlayerManager;

public class PauseCommand implements Command {
    @Override public String name() { return "pause"; }

    @Override
    public void execute(CommandContext ctx) {
        if (ctx.guild() == null) {
            ctx.channel().sendMessage("To działa tylko na serwerze.").queue();
            return;
        }

        var gmm = PlayerManager.getInstance().getGuildMusicManager(ctx.guild());
        var player = gmm.player; // dostosuj jeśli masz getter

        boolean newState = !player.isPaused();
        player.setPaused(newState);

        ctx.channel().sendMessage(newState ? "⏸️ Pauza." : "▶️ Wznowiono.").queue();
    }
}
