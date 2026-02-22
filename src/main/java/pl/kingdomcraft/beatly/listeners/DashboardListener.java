package pl.kingdomcraft.beatly.listeners;

import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import pl.kingdomcraft.beatly.managers.GuildMusicManager;
import pl.kingdomcraft.beatly.managers.PlayerManager;
import pl.kingdomcraft.beatly.music.MusicLibrary;
import pl.kingdomcraft.beatly.utils.DashboardUtils;
import pl.kingdomcraft.beatly.utils.StageVoice;
import net.dv8tion.jda.api.entities.Guild;
import java.util.Optional;

public class DashboardListener extends ListenerAdapter {
    private final MusicLibrary library;

    public DashboardListener(MusicLibrary library) {
        this.library = library;
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        if (!event.getComponentId().startsWith("beatly:")) return;

        var guild = event.getGuild();
        if (guild == null) return;

        var gmm = ensureVoiceConnection(guild, event.getMember());
        String id = event.getComponentId();

        switch (id) {
            case "beatly:pause":
                gmm.player.setPaused(true);
                updateDashboard(event);
                break;
            case "beatly:resume":
                // If a track is loaded but paused, just unpause it
                if (gmm.player.getPlayingTrack() != null) {
                    gmm.player.setPaused(false);
                } else {
                    // No track playing — try to start next from queue
                    gmm.scheduler.startQueueIfIdle();
                    gmm.player.setPaused(false);
                }
                updateDashboard(event);
                break;
            case "beatly:skip":
                gmm.scheduler.skip();
                updateDashboard(event);
                break;
            case "beatly:stop":
                // Clear only clears the queue, current track keeps playing
                gmm.scheduler.clearQueue();
                updateDashboard(event);
                break;
            case "beatly:loop":
                gmm.scheduler.setLoop(!gmm.scheduler.isLoop());
                updateDashboard(event);
                break;
            case "beatly:vol_down":
                gmm.player.setVolume(Math.max(0, gmm.player.getVolume() - 10));
                updateDashboard(event);
                break;
            case "beatly:vol_up":
                gmm.player.setVolume(Math.min(100, gmm.player.getVolume() + 10));
                updateDashboard(event);
                break;
        }
    }

    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        if (!event.getComponentId().equals("beatly:select_song")) return;

        var guild = event.getGuild();
        if (guild == null) return;

        String value = event.getValues().get(0);
        try {
            int idx = Integer.parseInt(value);
            Optional<MusicLibrary.Song> songOpt = library.byIndex(idx);

            if (songOpt.isPresent()) {
                var song = songOpt.get();
                var gmm = ensureVoiceConnection(guild, event.getMember());

                event.deferEdit().queue(); // Inform Discord we are processing

                PlayerManager.getInstance().loadTrack(guild, song.filePath().toAbsolutePath().toString(),
                    track -> {
                        track.setUserData(song.displayName());
                        gmm.scheduler.queue(track);

                        // Update dashboard AFTER track is queued
                        MessageCreateData data = DashboardUtils.createDashboard(gmm, library);
                        event.getHook().editOriginal(MessageEditBuilder.fromCreateData(data).build()).queue();
                    },
                    err -> {}
                );
            } else {
                event.reply("Błąd: Nie znaleziono utworu.").setEphemeral(true).queue();
            }
        } catch (NumberFormatException e) {
            event.reply("Błąd formatu ID.").setEphemeral(true).queue();
        }
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        if (!event.getModalId().equals("beatly:modal_add")) return;

        String value = event.getValue("song_id").getAsString();
        var guild = event.getGuild();
        if (guild == null) return;

        try {
            int idx = Integer.parseInt(value);
            Optional<MusicLibrary.Song> songOpt = library.byIndex(idx);

            if (songOpt.isPresent()) {
                var song = songOpt.get();
                var gmm = ensureVoiceConnection(guild, event.getMember());

                PlayerManager.getInstance().loadTrack(guild, song.filePath().toAbsolutePath().toString(),
                        track -> {
                            track.setUserData(song.displayName());
                            gmm.scheduler.queue(track);
                        },
                        _ -> {}
                );

                MessageCreateData data = DashboardUtils.createDashboard(gmm, library);
                event.editMessage(MessageEditBuilder.fromCreateData(data).build()).queue();

            } else {
                event.reply("Nie znaleziono utworu o indeksie: " + idx).setEphemeral(true).queue();
            }
        } catch (NumberFormatException e) {
            event.reply("To nie jest liczba!").setEphemeral(true).queue();
        }
    }

    /**
     * Ensures the bot is connected to the member's voice channel and has
     * the audio sendingHandler registered. Returns the GuildMusicManager,
     * or null if the member is not in a voice channel.
     */
    private GuildMusicManager ensureVoiceConnection(Guild guild, Member member) {
        var gmm = PlayerManager.getInstance().getGuildMusicManager(guild);
        var audioManager = guild.getAudioManager();

        // Always ensure the sending handler is set
        if (audioManager.getSendingHandler() == null) {
            audioManager.setSendingHandler(gmm.sendHandler);
        }

        // Auto-join if not connected
        if (!audioManager.isConnected() && member != null) {
            GuildVoiceState vs = member.getVoiceState();
            if (vs != null && vs.inAudioChannel()) {
                StageVoice.connect(guild, vs.getChannel());
            }
        }

        return gmm;
    }

    private void updateDashboard(net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback event) {
        var gmm = PlayerManager.getInstance().getGuildMusicManager(event.getGuild());
        MessageCreateData data = DashboardUtils.createDashboard(gmm, library);
        event.editMessage(MessageEditBuilder.fromCreateData(data).build()).queue();
    }
}

