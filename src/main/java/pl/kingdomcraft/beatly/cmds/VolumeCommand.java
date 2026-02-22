package pl.kingdomcraft.beatly.cmds;

import pl.kingdomcraft.beatly.managers.PlayerManager;

public class VolumeCommand implements Command {

    @Override public String name() { return "volume"; }

    @Override
    public void execute(CommandContext ctx) {
        var gmm = PlayerManager.getInstance().getGuildMusicManager(ctx.guild());

        if (ctx.args().isEmpty()) {
            ctx.channel().sendMessage("🔊 Aktualna głośność: **" + gmm.player.getVolume()
                    + "%**. Użycie: `!volume 0-200`").queue();
            return;
        }

        int vol;
        try {
            vol = Integer.parseInt(ctx.args().get(0));
        } catch (NumberFormatException e) {
            ctx.channel().sendMessage("Użycie: `!volume 0-200`").queue();
            return;
        }

        if (vol < 0) vol = 0;
        if (vol > 200) vol = 200;

        gmm.player.setVolume(vol);
        ctx.channel().sendMessage("🔊 Ustawiono głośność na **" + vol + "%**").queue();
    }
}
