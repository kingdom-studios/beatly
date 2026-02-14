package pl.kingdomcraft.beatly.cmds;

import net.dv8tion.jda.api.EmbedBuilder;
import pl.kingdomcraft.beatly.music.MusicLibrary;

import java.util.stream.Collectors;

public class SongsCommand implements Command {

    private final MusicLibrary library;

    public SongsCommand(MusicLibrary library) {
        this.library = library;
    }

    @Override public String name() { return "songs"; }

    @Override
    public void execute(CommandContext ctx) {
        var list = library.list();

        // Dla długich list lepiej paginować — tu prosto: max ~30 pozycji w embedzie
        String body = list.stream()
                .limit(30)
                .map(s -> "`" + s.index() + "` • " + s.displayName())
                .collect(Collectors.joining("\n"));

        if (list.size() > 30) body += "\n\n… i jeszcze " + (list.size() - 30) + " więcej.";

        var eb = new EmbedBuilder()
                .setTitle("🎵 Lista piosenek")
                .setDescription(body)
                .setFooter("Użycie: !play <numer> albo !play <fragment nazwy>", null);

        ctx.channel().sendMessageEmbeds(eb.build()).queue();
    }
}
