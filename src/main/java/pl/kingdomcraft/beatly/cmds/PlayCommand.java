package pl.kingdomcraft.beatly.cmds;

import pl.kingdomcraft.beatly.managers.PlayerManager;
import pl.kingdomcraft.beatly.music.MusicLibrary;
import pl.kingdomcraft.beatly.utils.DashboardUtils;
import pl.kingdomcraft.beatly.utils.StageVoice;

public class PlayCommand implements Command {

    private final MusicLibrary library;

    public PlayCommand(MusicLibrary library) {
        this.library = library;
    }

    @Override public String name() { return "play"; }

    @Override
    public void execute(CommandContext ctx) {
        var member = ctx.member();
        if (member == null) {
            ctx.channel().sendMessage("To działa tylko na serwerze.").queue();
            return;
        }

        var vs = member.getVoiceState();
        if (vs == null || !vs.inAudioChannel()) {
            ctx.channel().sendMessage("Wejdź na voice, żebym wiedział gdzie dołączyć.").queue();
            return;
        }

        // Join jeśli nie jest podłączony
        var audioManager = ctx.guild().getAudioManager();
        var gmm = PlayerManager.getInstance().getGuildMusicManager(ctx.guild());

        if (audioManager.getSendingHandler() == null) {
            audioManager.setSendingHandler(gmm.sendHandler);
        }

        if (!audioManager.isConnected()) {
            StageVoice.connect(ctx.guild(), vs.getChannel());
        }

        // !play (bez args) -> Pokaż Dashboard
        if (ctx.args().isEmpty()) {
            ctx.channel().sendMessage(DashboardUtils.createDashboard(gmm, library)).queue();
            return;
        }

        // jeśli coś już gra -> nie pozwalamy !play <...> jako “add”
        if (gmm.scheduler.isPlaying()) {
            ctx.channel().sendMessage("Już gram 🎶 Jeśli chcesz dodać do kolejki, użyj: `!queue add <numer>`").queue();
            return;
        }

        // idle: !play <numer/nazwa> -> zagra od razu
        String query = String.join(" ", ctx.args()).trim();
        var songOpt = parseSong(query);
        if (songOpt.isEmpty()) {
            ctx.channel().sendMessage("Nie znalazłem utworu dla: `" + query + "`. Użyj `!songs`.").queue();
            return;
        }

        var song = songOpt.get();
        var abs = song.filePath().toAbsolutePath();
        String identifier = abs.toString();

        PlayerManager.getInstance().loadTrack(ctx.guild(), identifier,
                track -> {
                    track.setUserData(song.displayName());
                    gmm.scheduler.queue(track);
                    ctx.channel().sendMessage("▶️ Gram: **" + song.index() + ". " + song.displayName() + "**").queue();
                },
                err -> ctx.channel().sendMessage("❌ " + err).queue()
        );
    }

    private java.util.Optional<MusicLibrary.Song> parseSong(String query) {
        // 1) jeśli to liczba -> po indeksie
        try {
            int idx = Integer.parseInt(query);
            return library.byIndex(idx);
        } catch (NumberFormatException ignored) {}

        // 2) inaczej szukamy po nazwie
        return library.searchByName(query);
    }
}
