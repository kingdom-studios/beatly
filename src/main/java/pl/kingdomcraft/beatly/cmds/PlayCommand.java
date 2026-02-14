package pl.kingdomcraft.beatly.cmds;

import pl.kingdomcraft.beatly.managers.PlayerManager;
import pl.kingdomcraft.beatly.music.MusicLibrary;

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

        if (ctx.args().isEmpty()) {
            ctx.channel().sendMessage("Użycie: `!play 1` albo `!play nazwa` (lista: `!songs`)").queue();
            return;
        }

        // Join jeśli nie jest podłączony
        var audioManager = ctx.guild().getAudioManager();
        if (!audioManager.isConnected()) {
            audioManager.openAudioConnection(vs.getChannel());
        }

        // Podpinamy handler audio (LavaPlayer -> JDA)
        var gmm = PlayerManager.getInstance().getGuildMusicManager(ctx.guild());
        if (audioManager.getSendingHandler() == null) {
            audioManager.setSendingHandler(gmm.sendHandler);
        }

        String query = String.join(" ", ctx.args()).trim();

        var songOpt = parseSong(query);
        if (songOpt.isEmpty()) {
            ctx.channel().sendMessage("Nie znalazłem utworu dla: `" + query + "`. Użyj `!songs`.").queue();
            return;
        }

        var song = songOpt.get();
        var abs = song.filePath().toAbsolutePath();

        // debug (na chwilę)
        ctx.channel().sendMessage("DEBUG exists=" + java.nio.file.Files.exists(abs)
                + " readable=" + java.nio.file.Files.isReadable(abs)
                + " path=" + abs).queue();

        // najpewniejsze dla local source:
        String identifier = abs.toString(); // <-- FIX

        PlayerManager.getInstance().loadAndPlay(ctx.guild(), identifier,
                track -> ctx.channel().sendMessage("▶️ Gram: **" + song.index() + ". " + song.displayName() + "**").queue(),
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
