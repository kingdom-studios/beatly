package pl.kingdomcraft.beatly;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import pl.kingdomcraft.beatly.cmds.*;
import pl.kingdomcraft.beatly.listeners.MessageCommandListener;
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

        JDA jda = JDABuilder.create(token, intents)
                .addEventListeners(new MessageCommandListener(cm, "!"))
                .build();

        jda.awaitReady();
        System.out.println("Bot online.");
    }
}
