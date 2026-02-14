package pl.kingdomcraft.beatly.managers;

import pl.kingdomcraft.beatly.cmds.Command;

import java.util.*;

public class CommandManager {

    private final Map<String, Command> commands = new HashMap<>();

    /** Rejestracja komendy */
    public void register(Command command) {
        commands.put(command.name().toLowerCase(Locale.ROOT), command);
    }

    /** Pobranie komendy po nazwie */
    public Optional<Command> get(String name) {
        return Optional.ofNullable(commands.get(name.toLowerCase(Locale.ROOT)));
    }

    /** Debug / pomocnicze */
    public Collection<Command> getCommands() {
        return commands.values();
    }
}
