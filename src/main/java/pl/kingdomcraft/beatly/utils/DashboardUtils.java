package pl.kingdomcraft.beatly.utils;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import pl.kingdomcraft.beatly.managers.GuildMusicManager;
import pl.kingdomcraft.beatly.music.MusicLibrary;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import java.awt.Color;

public class DashboardUtils {

    public static MessageCreateData createDashboard(GuildMusicManager gmm, MusicLibrary library) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("📻 Beatly Radio Panel");
        eb.setColor(Color.GREEN);

        AudioTrack playing = gmm.player.getPlayingTrack();
        boolean isPaused = gmm.player.isPaused();

        StringBuilder desc = new StringBuilder();

        // sekcja aktualnie odtwarzana
        if (playing != null) {
            String status = isPaused ? "⏸️ Zatrzymano" : "▶️ Odtwarzanie";
            String title = playing.getUserData() != null
                    ? playing.getUserData().toString()
                    : playing.getInfo().title;
            desc.append("**Aktualnie:** ")
                    .append(status)
                    .append(" **")
                    .append(title)
                    .append("**\n\n");
        } else {
            desc.append("**Aktualnie:** ⏹️ Brak odtwarzania. Wybierz utwór poniżej.\n\n");
        }

        // sekcja pełnej kolejki
        var snapshot = gmm.scheduler.snapshot();
        if (!snapshot.isEmpty()) {
            desc.append("**Kolejka:**");
            for (int i = 0; i < snapshot.size(); i++) {
                AudioTrack t = snapshot.get(i);
                String title = t.getUserData() != null
                        ? t.getUserData().toString()
                        : t.getInfo().title;
                desc.append("\n").append(i + 1).append(". ").append(title);
            }
        } else {
            desc.append("**Kolejka:** (pusta)");
        }

        eb.setDescription(desc.toString());

        StringBuilder status = new StringBuilder();
        status.append("**Głośność:** ").append(gmm.player.getVolume()).append("%\n");
        status.append("**Pętla:** ").append(gmm.scheduler.isLoop() ? "✅" : "❌").append("\n");
        status.append("**W kolejce:** ").append(gmm.scheduler.snapshot().size()).append(" utworów");

        eb.addField("Statystyki", status.toString(), false);

        StringSelectMenu.Builder menu = StringSelectMenu.create("beatly:select_song")
                .setPlaceholder("Wybierz utwór (" + library.list().size() + ")");

        var list = library.list();
        if (list.isEmpty()) {
             menu.addOption("Brak utworów", "none");
             menu.setDisabled(true);
        } else {
            list.stream().limit(25).forEach(song -> {
                 String label = song.index() + ". " + song.displayName();
                 if (label.length() > 100) label = label.substring(0, 97) + "...";
                 menu.addOption(label, String.valueOf(song.index()));
            });
        }

        Button btnPlayPause;
        boolean hasQueue = !gmm.scheduler.snapshot().isEmpty();

        if (playing == null) {
             // Jeśli nic nie gra, ale jest coś w kolejce -> przycisk aktywny
             btnPlayPause = Button.secondary("beatly:resume", "▶️ Start").withDisabled(!hasQueue);
        } else {
             btnPlayPause = isPaused ? Button.success("beatly:resume", "▶️ Wznów") : Button.secondary("beatly:pause", "⏸️ Pauza");
        }

        Button btnSkip = Button.primary("beatly:skip", "⏭️ Skip");
        Button btnStop = Button.danger("beatly:stop", "🗑️ Clear");
        Button btnLoop = Button.secondary("beatly:loop", gmm.scheduler.isLoop() ? "🔂 Loop ON" : "🔁 Loop OFF");

        int vol = gmm.player.getVolume();
        Button btnVolDown = Button.secondary("beatly:vol_down", "🔉 -10").withDisabled(vol <= 0);
        Button btnVolUp = Button.secondary("beatly:vol_up", "🔊 +10").withDisabled(vol >= 100);

        return new MessageCreateBuilder()
                .setEmbeds(eb.build())
                .setComponents(
                        ActionRow.of(menu.build()),
                        ActionRow.of(btnPlayPause, btnSkip, btnStop, btnLoop),
                        ActionRow.of(btnVolDown, btnVolUp)
                )
                .build();
    }
}

