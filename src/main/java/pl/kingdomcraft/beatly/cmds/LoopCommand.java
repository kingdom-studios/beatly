package pl.kingdomcraft.beatly.cmds;

import pl.kingdomcraft.beatly.managers.PlayerManager;

public class LoopCommand implements Command {

    @Override public String name() { return "loop"; }

    @Override
    public void execute(CommandContext ctx) {
        var gmm = PlayerManager.getInstance().getGuildMusicManager(ctx.guild());

        if (ctx.args().isEmpty()) {
            ctx.channel().sendMessage("Loop jest teraz: **" + (gmm.scheduler.isLoop() ? "ON" : "OFF")
                    + "**. Użycie: `!loop on` / `!loop off`").queue();
            return;
        }

        String arg = ctx.args().get(0).toLowerCase();
        switch (arg) {
            case "on", "true", "1" -> {
                gmm.scheduler.setLoop(true);
                ctx.channel().sendMessage("🔁 Loop: **ON**").queue();
            }
            case "off", "false", "0" -> {
                gmm.scheduler.setLoop(false);
                ctx.channel().sendMessage("⏹️ Loop: **OFF**").queue();
            }
            default -> ctx.channel().sendMessage("Użycie: `!loop on` / `!loop off`").queue();
        }
    }
}
