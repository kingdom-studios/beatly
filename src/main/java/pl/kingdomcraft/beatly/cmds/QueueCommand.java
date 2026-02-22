package pl.kingdomcraft.beatly.cmds;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import pl.kingdomcraft.beatly.managers.PlayerManager;
import pl.kingdomcraft.beatly.music.MusicLibrary;

import java.nio.file.Path;
import java.util.List;

public class QueueCommand implements Command {

    private final MusicLibrary library;

    public QueueCommand(MusicLibrary library) {
        this.library = library;
    }

    @Override public String name() { return "queue"; }

    @Override
    public void execute(CommandContext ctx) {
        if (ctx.guild() == null) {
            ctx.channel().sendMessage("To działa tylko na serwerze.").queue();
            return;
        }

        var args = ctx.args();
        var gmm = PlayerManager.getInstance().getGuildMusicManager(ctx.guild());

        // !queue -> print
        if (args.isEmpty()) {
            printQueue(ctx, gmm);
            return;
        }

        String sub = args.get(0).toLowerCase();

        // !queue clear
        if (sub.equals("clear")) {
            gmm.scheduler.clearQueue();
            ctx.channel().sendMessage("🧹 Wyczyściłem kolejkę.").queue();
            return;
        }

        // !queue add <number>
        if (sub.equals("add")) {
            if (args.size() < 2) {
                ctx.channel().sendMessage("Użycie: `!queue add <numer>`").queue();
                return;
            }

            String q = String.join(" ", args.subList(1, args.size())).trim();
            var songOpt = parseSong(q);

            if (songOpt.isEmpty()) {
                ctx.channel().sendMessage("Nie znalazłem utworu: `" + q + "`. Użyj `!songs`.").queue();
                return;
            }

            var song = songOpt.get();
            Path abs = song.filePath().toAbsolutePath();
            String identifier = abs.toString();

            // potrzebujemy metody loadTrack (bez auto-queue). Jeśli jej nie masz, dopisz jak wcześniej.
            PlayerManager.getInstance().loadTrack(ctx.guild(), identifier,
                    track -> {
                        track.setUserData(song.displayName());
                        gmm.scheduler.queue(track);
                        ctx.channel().sendMessage("➕ Dodano do kolejki: **" + song.index() + ". " + song.displayName() + "**").queue();
                    },
                    err -> ctx.channel().sendMessage("❌ " + err).queue()
            );
            return;
        }

        // !queue remove <position> <number>
        if (sub.equals("remove")) {
            if (args.size() < 3) {
                ctx.channel().sendMessage("Użycie: `!queue remove <pozycja> <ile>`").queue();
                return;
            }
            try {
                int pos = Integer.parseInt(args.get(1));
                int count = Integer.parseInt(args.get(2));

                int removed = gmm.scheduler.removeRange(pos, count);
                ctx.channel().sendMessage(
                        removed == 0 ? "Nic nie usunięto (zła pozycja?)." : "🗑️ Usunięto **" + removed + "** pozycji."
                ).queue();
            } catch (NumberFormatException e) {
                ctx.channel().sendMessage("Pozycja i ilość muszą być liczbami.").queue();
            }
            return;
        }

        ctx.channel().sendMessage("Użycie: `!queue`, `!queue add <numer>`, `!queue remove <pozycja> <ile>`, `!queue clear`").queue();
    }

    private void printQueue(CommandContext ctx, pl.kingdomcraft.beatly.managers.GuildMusicManager gmm) {
        var now = gmm.player.getPlayingTrack();
        List<AudioTrack> list = gmm.scheduler.snapshot();

        StringBuilder sb = new StringBuilder("🎶 **Kolejka**\n");
        if (now != null) sb.append("Teraz: **").append(titleOf(now)).append("**\n\n");
        else sb.append("Teraz: *(nic)*\n\n");

        if (list.isEmpty()) {
            sb.append("Następne: *(pusto)*\n");
        } else {
            sb.append("Następne:\n");
            for (int i = 0; i < list.size(); i++) {
                sb.append(i + 1).append(" • ").append(titleOf(list.get(i))).append("\n");
                if (i >= 20) { sb.append("..."); break; }
            }
        }

        ctx.channel().sendMessage(sb.toString()).queue();
    }

    private String titleOf(com.sedmelluq.discord.lavaplayer.track.AudioTrack t) {
        Object ud = t.getUserData();
        if (ud instanceof String s && !s.isBlank()) return s;
        return t.getInfo().title != null ? t.getInfo().title : "Unknown";
    }

    private java.util.Optional<MusicLibrary.Song> parseSong(String query) {
        try {
            int idx = Integer.parseInt(query);
            return library.byIndex(idx);
        } catch (NumberFormatException ignored) {}
        return library.searchByName(query);
    }
}
