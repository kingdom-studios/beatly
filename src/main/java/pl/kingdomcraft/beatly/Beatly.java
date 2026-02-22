package pl.kingdomcraft.beatly;

import club.minnced.discord.jdave.interop.JDaveSessionFactory;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.audio.AudioModuleConfig;
import net.dv8tion.jda.api.requests.GatewayIntent;
import pl.kingdomcraft.beatly.cmds.*;
import pl.kingdomcraft.beatly.listeners.DashboardListener;
import pl.kingdomcraft.beatly.listeners.MessageCommandListener;
import pl.kingdomcraft.beatly.listeners.SlashCommandListener;
import pl.kingdomcraft.beatly.listeners.StageAutoSpeakerListener;
import pl.kingdomcraft.beatly.managers.CommandManager;
import pl.kingdomcraft.beatly.music.MusicLibrary;

import java.nio.file.Path;
import java.util.EnumSet;

public class Beatly {

    public static void main(String[] args) throws Exception {
        String token = System.getenv("DISCORD_TOKEN");
        if (token == null || token.isBlank()) {
            System.err.println("Brak DISCORD_TOKEN w env.");
            System.exit(1);
        }

        var intents = EnumSet.of(
                GatewayIntent.GUILD_MESSAGES,
                GatewayIntent.MESSAGE_CONTENT,
                GatewayIntent.GUILD_VOICE_STATES // ważne do voice state
        );

        MusicLibrary library = new MusicLibrary(Path.of("music"));
        library.load();

        CommandManager cm = new CommandManager();
        cm.register(new PingCommand());
        cm.register(new JoinCommand());
        cm.register(new LeaveCommand());
        cm.register(new PlayCommand(library));
        cm.register(new SongsCommand(library));
        cm.register(new VolumeCommand());
        cm.register(new LoopCommand());
        cm.register(new StopCommand());
        cm.register(new SkipCommand());
        cm.register(new QueueCommand(library));
        cm.register(new PauseCommand());

        JDABuilder builder = JDABuilder.create(token, intents)
                .disableCache(
                        net.dv8tion.jda.api.utils.cache.CacheFlag.ACTIVITY,
                        net.dv8tion.jda.api.utils.cache.CacheFlag.EMOJI,
                        net.dv8tion.jda.api.utils.cache.CacheFlag.STICKER,
                        net.dv8tion.jda.api.utils.cache.CacheFlag.CLIENT_STATUS,
                        net.dv8tion.jda.api.utils.cache.CacheFlag.ONLINE_STATUS,
                        net.dv8tion.jda.api.utils.cache.CacheFlag.SCHEDULED_EVENTS
                );

        builder.enableIntents(
                net.dv8tion.jda.api.requests.GatewayIntent.GUILD_MESSAGES,
                net.dv8tion.jda.api.requests.GatewayIntent.MESSAGE_CONTENT,
                net.dv8tion.jda.api.requests.GatewayIntent.GUILD_VOICE_STATES
        );

        SlashCommandListener slashListener = new SlashCommandListener(library);

        builder.addEventListeners(
                new MessageCommandListener(cm, "!"),
                new StageAutoSpeakerListener(),
                new DashboardListener(library),
                slashListener
        );

        builder.setAudioModuleConfig(
                new AudioModuleConfig()
                        .withDaveSessionFactory(new JDaveSessionFactory())
        );

        JDA jda = builder.build();

        jda.awaitReady();


        System.out.println("Bot online.");
    }
}
