package pl.kingdomcraft.beatly.listeners;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import pl.kingdomcraft.beatly.managers.GuildMusicManager;
import pl.kingdomcraft.beatly.managers.PlayerManager;
import pl.kingdomcraft.beatly.music.MusicLibrary;
import pl.kingdomcraft.beatly.utils.DashboardUtils;
import pl.kingdomcraft.beatly.utils.StageVoice;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SlashCommandListener extends ListenerAdapter {

    private final MusicLibrary library;

    /** Informacje o aktywnej sesji radia na serwerze. */
    private record Session(long messageId, long channelId) {}

    /**
     * Mapa guildId -> Session (messageId dashboardu + channelId kanału tekstowego).
     * Pozwala śledzić, czy sesja jest aktywna i na jakim kanale.
     */
    private final Map<Long, Session> activeSessions = new ConcurrentHashMap<>();

    public SlashCommandListener(MusicLibrary library) {
        this.library = library;
    }

    @Override
    public void onGuildReady(GuildReadyEvent event) {
        event.getGuild().updateCommands().addCommands(
                Commands.slash("start", "Uruchom radio — bot dołącza na voice i otwiera panel"),
                Commands.slash("end", "Zakończ radio — bot wychodzi z voice i usuwa panel"),
                Commands.slash("remove", "Usuń utwór z kolejki po numerze")
                        .addOption(OptionType.INTEGER, "numer", "Numer pozycji w kolejce do usunięcia", true)
        ).queue();
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (event.getGuild() == null) {
            event.reply("❌ Ta komenda działa tylko na serwerze.").setEphemeral(true).queue();
            return;
        }

        switch (event.getName()) {
            case "start"  -> handleStart(event);
            case "end"    -> handleEnd(event);
            case "remove" -> handleRemove(event);
        }
    }

    private void handleStart(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        long guildId = guild.getIdLong();

        if (activeSessions.containsKey(guildId)) {
            Session session = activeSessions.get(guildId);
            event.reply("⚠️ Radio jest już uruchomione na kanale <#" + session.channelId + ">! Użyj `/end`, aby je zakończyć.")
                    .setEphemeral(true).queue();
            return;
        }

        Member member = event.getMember();
        if (member == null) return;

        GuildVoiceState vs = member.getVoiceState();
        if (vs == null || !vs.inAudioChannel()) {
            event.reply("❌ Wejdź najpierw na kanał głosowy!").setEphemeral(true).queue();
            return;
        }

        AudioChannel voiceChannel = vs.getChannel();

        var audioManager = guild.getAudioManager();
        GuildMusicManager gmm = PlayerManager.getInstance().getGuildMusicManager(guild);

        if (audioManager.getSendingHandler() == null) {
            audioManager.setSendingHandler(gmm.sendHandler);
        }

        event.deferReply().queue(hook -> {
            StageVoice.connect(guild, voiceChannel);

            MessageCreateData dashboard = DashboardUtils.createDashboard(gmm, library);
            hook.editOriginal(MessageEditBuilder.fromCreateData(dashboard).build()).queue(message -> {
                activeSessions.put(guildId, new Session(message.getIdLong(), event.getChannel().getIdLong()));
            });
        });
    }

    private void handleEnd(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        long guildId = guild.getIdLong();

        if (!activeSessions.containsKey(guildId)) {
            event.reply("⚠️ Radio nie jest uruchomione! Użyj `/start`, aby je włączyć.")
                    .setEphemeral(true).queue();
            return;
        }

        GuildMusicManager gmm = PlayerManager.getInstance().getGuildMusicManager(guild);
        gmm.scheduler.stopAndClear();
        gmm.player.setPaused(false);

        guild.getAudioManager().closeAudioConnection();

        Session session = activeSessions.remove(guildId);
        if (session != null) {
            event.getGuild().getTextChannelById(session.channelId)
                    .deleteMessageById(session.messageId).queue(
                            _ -> {},
                            _ -> {}
                    );
        }

        event.reply("👋 Radio zostało wyłączone. Do usłyszenia!")
                .setEphemeral(true).queue();
    }

    private void handleRemove(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        long guildId = guild.getIdLong();

        // Sprawdź czy sesja istnieje
        if (!activeSessions.containsKey(guildId)) {
            event.reply("⚠️ Radio nie jest uruchomione! Użyj `/start`, aby je włączyć.")
                    .setEphemeral(true).queue();
            return;
        }

        // Sprawdź czy komenda jest na tym samym kanale co dashboard
        Session session = activeSessions.get(guildId);
        if (event.getChannel().getIdLong() != session.channelId) {
            event.reply("⚠️ Kolejka radia jest aktywna na kanale <#" + session.channelId + ">! Użyj komendy tam.")
                    .setEphemeral(true).queue();
            return;
        }

        int position = event.getOption("numer").getAsInt();

        GuildMusicManager gmm = PlayerManager.getInstance().getGuildMusicManager(guild);
        var snapshot = gmm.scheduler.snapshot();

        if (snapshot.isEmpty()) {
            event.reply("❌ Kolejka jest pusta — nie ma czego usuwać.").setEphemeral(true).queue();
            return;
        }

        if (position < 1 || position > snapshot.size()) {
            event.reply("❌ Nieprawidłowy numer! Podaj liczbę od **1** do **" + snapshot.size() + "**.")
                    .setEphemeral(true).queue();
            return;
        }

        // Pobierz nazwę tracku PRZED usunięciem
        var track = snapshot.get(position - 1);
        String trackName = track.getUserData() != null
                ? track.getUserData().toString()
                : track.getInfo().title;

        int removed = gmm.scheduler.removeRange(position, 1);

        if (removed > 0) {
            // Zaktualizuj dashboard
            MessageCreateData data = DashboardUtils.createDashboard(gmm, library);
            event.getGuild().getTextChannelById(session.channelId)
                    .editMessageById(session.messageId, MessageEditBuilder.fromCreateData(data).build())
                    .queue(_ -> {}, _ -> {});

            event.reply("🗑️ Usunięto z kolejki: **" + trackName + "** (poz. " + position + ")")
                    .setEphemeral(true).queue();
        } else {
            event.reply("❌ Nie udało się usunąć — sprawdź numer pozycji.")
                    .setEphemeral(true).queue();
        }
    }
}

