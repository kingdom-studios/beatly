package pl.kingdomcraft.beatly.cmds;

import pl.kingdomcraft.beatly.managers.PlayerManager;

public class StopCommand implements Command {
    @Override public String name() { return "stop"; }

    @Override
    public void execute(CommandContext ctx) {
        if (ctx.guild() == null) {
            ctx.channel().sendMessage("To działa tylko na serwerze.").queue();
            return;
        }

        var gmm = PlayerManager.getInstance().getGuildMusicManager(ctx.guild());
        gmm.scheduler.stopAndClear();

        ctx.channel().sendMessage("⏹️ Zatrzymano odtwarzanie i wyczyszczono kolejkę.").queue();
    }
}
