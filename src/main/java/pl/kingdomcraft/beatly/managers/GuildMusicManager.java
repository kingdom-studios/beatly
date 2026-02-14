package pl.kingdomcraft.beatly.managers;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import pl.kingdomcraft.beatly.music.AudioPlayerSendHandler;
import pl.kingdomcraft.beatly.music.TrackScheduler;

public class GuildMusicManager {
    public final AudioPlayer player;
    public final TrackScheduler scheduler;
    public final AudioPlayerSendHandler sendHandler;

    public GuildMusicManager(AudioPlayer player) {
        this.player = player;
        this.scheduler = new TrackScheduler(player);
        this.player.addListener(this.scheduler);
        this.sendHandler = new AudioPlayerSendHandler(player);
    }
}
