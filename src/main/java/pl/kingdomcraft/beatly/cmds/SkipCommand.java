package pl.kingdomcraft.beatly.cmds;

import pl.kingdomcraft.beatly.managers.PlayerManager;

public class SkipCommand implements Command {
    @Override public String name() { return "skip"; }

    @Override
    public void execute(CommandContext ctx) {
        if (ctx.guild() == null) {
            ctx.channel().sendMessage("To działa tylko na serwerze.").queue();
            return;
        }

        var gmm = PlayerManager.getInstance().getGuildMusicManager(ctx.guild());
        boolean ok = gmm.scheduler.skip();

        ctx.channel().sendMessage(ok ? "⏭️ Skip." : "Kolejka jest pusta.").queue();
    }
}
