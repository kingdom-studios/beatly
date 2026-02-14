package pl.kingdomcraft.beatly.managers;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import lombok.Getter;
import net.dv8tion.jda.api.entities.Guild;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class PlayerManager {

    @Getter
    private static final PlayerManager Instance = new PlayerManager();

    private final AudioPlayerManager playerManager;
    private final Map<Long, GuildMusicManager> musicManagers = new ConcurrentHashMap<>();

    private PlayerManager() {
        this.playerManager = new DefaultAudioPlayerManager();
        AudioSourceManagers.registerLocalSource(playerManager);   // MP3 z dysku
        AudioSourceManagers.registerRemoteSources(playerManager); // (opcjonalnie) URL
    }

    public GuildMusicManager getGuildMusicManager(Guild guild) {
        return musicManagers.computeIfAbsent(guild.getIdLong(), (id) -> {
            AudioPlayer player = playerManager.createPlayer();
            return new GuildMusicManager(player);
        });
    }

    public void loadAndPlay(Guild guild, String pathOrUrl,
                            Consumer<AudioTrack> onQueued,
                            Consumer<String> onError) {

        GuildMusicManager gmm = getGuildMusicManager(guild);

        playerManager.loadItemOrdered(gmm, pathOrUrl, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                gmm.scheduler.queue(track);
                onQueued.accept(track);
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                // Jeśli podasz folder/playlistę, możesz to rozwinąć
                AudioTrack first = playlist.getTracks().get(0);
                gmm.scheduler.queue(first);
                onQueued.accept(first);
            }

            @Override
            public void noMatches() {
                onError.accept("Nie znaleziono: " + pathOrUrl);
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                onError.accept("Nie udało się załadować: " + exception.getMessage());
            }
        });
    }
}
