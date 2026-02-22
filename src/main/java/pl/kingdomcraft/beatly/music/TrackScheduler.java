package pl.kingdomcraft.beatly.music;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class TrackScheduler extends AudioEventAdapter {

    private final AudioPlayer player;
    private final BlockingQueue<AudioTrack> queue = new LinkedBlockingQueue<>();

    @Getter @Setter
    private volatile boolean loop = false;

    public TrackScheduler(AudioPlayer player) {
        this.player = player;
    }

    /** Dodaje do kolejki; jeśli nic nie gra -> start od razu */
    public void queue(AudioTrack track) {
        if (!player.startTrack(track, true)) {
            queue.offer(track);
        }
    }

    /** Czy coś aktualnie gra */
    public boolean isPlaying() {
        return player.getPlayingTrack() != null;
    }

    /** Start kolejki jeśli bot jest idle */
    public boolean startQueueIfIdle() {
        if (isPlaying()) return false;
        AudioTrack next = queue.poll();
        if (next == null) return false;
        player.startTrack(next, false);
        return true;
    }

    /** Skip: zatrzymaj obecny track i wymuś następny */
    public boolean skip() {
        player.stopTrack(); // Zatrzymuje obecny (reason STOPPED -> onTrackEnd ignored)
        return startQueueIfIdle(); // Ręcznie odpalamy następny
    }

    /** Stop + clear kolejka */
    public void stopAndClear() {
        queue.clear();
        player.stopTrack();
    }

    public void clearQueue() {
        queue.clear();
    }

    /** Snapshot kolejki do printowania */
    public List<AudioTrack> snapshot() {
        return new ArrayList<>(queue);
    }

    /**
     * Remove <position> <count>
     * position 1-based
     */
    public int removeRange(int position1Based, int count) {
        if (position1Based < 1 || count < 1) return 0;

        // BlockingQueue nie ma remove(index), więc robimy snapshot -> modyfikacja -> rebuild
        List<AudioTrack> list = new ArrayList<>(queue);
        int start = position1Based - 1;
        if (start >= list.size()) return 0;

        int end = Math.min(list.size(), start + count);
        int removed = end - start;
        list.subList(start, end).clear();

        queue.clear();
        queue.addAll(list);
        return removed;
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        if (!endReason.mayStartNext) return;

        if (loop) {
            player.startTrack(track.makeClone(), false);
            return;
        }

        AudioTrack next = queue.poll();
        if (next != null) {
            player.startTrack(next, false);
        }
    }
}
