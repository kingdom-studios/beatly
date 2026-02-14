package pl.kingdomcraft.beatly.cmds;

public interface Command {
    String name();
    void execute(CommandContext ctx) throws Exception;
}
