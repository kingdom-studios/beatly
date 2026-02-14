package pl.kingdomcraft.beatly.listeners;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import pl.kingdomcraft.beatly.cmds.CommandContext;
import pl.kingdomcraft.beatly.managers.CommandManager;

import java.util.Arrays;
import java.util.List;

public class MessageCommandListener extends ListenerAdapter {

    private final CommandManager commandManager;
    private final String prefix;

    public MessageCommandListener(CommandManager commandManager, String prefix) {
        this.commandManager = commandManager;
        this.prefix = prefix;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;
        if (!event.isFromGuild()) return;

        String raw = event.getMessage().getContentRaw();
        if (!raw.startsWith(prefix)) return;

        String withoutPrefix = raw.substring(prefix.length()).trim();
        if (withoutPrefix.isEmpty()) return;

        String[] parts = withoutPrefix.split("\\s+");
        String cmdName = parts[0];
        List<String> args = Arrays.asList(parts).subList(1, parts.length);

        commandManager.get(cmdName).ifPresentOrElse(cmd -> {
            try {
                cmd.execute(new CommandContext(event, args));
            } catch (Exception e) {
                event.getChannel().sendMessage("Błąd: " + e.getMessage()).queue();
                e.printStackTrace();
            }
        }, () -> event.getChannel().sendMessage("Nie znam komendy `" + cmdName + "`").queue());
    }
}
