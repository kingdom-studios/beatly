package pl.kingdomcraft.beatly.cmds;

import net.dv8tion.jda.api.EmbedBuilder;

import java.time.Instant;

public class PingCommand implements Command {
    @Override
    public String name() {
        return "ping";
    }

    @Override
    public void execute(CommandContext ctx) throws Exception {
        var eb = new EmbedBuilder()
                .setTitle("Pong 🏓")
                .setDescription("Bot działa poprawnie.")
                .addField("Guild", ctx.guild().getName(), true)
                .addField("User", ctx.member().getEffectiveName(), true)
                .setTimestamp(Instant.now());

        ctx.channel().sendMessageEmbeds(eb.build()).queue();
    }
}
