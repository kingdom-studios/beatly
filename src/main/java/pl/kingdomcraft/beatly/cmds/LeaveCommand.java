package pl.kingdomcraft.beatly.cmds;

public class LeaveCommand implements Command {

    @Override
    public String name() {
        return "leave";
    }

    @Override
    public void execute(CommandContext ctx) throws Exception {
        ctx.guild().getAudioManager().closeAudioConnection();
        ctx.channel().sendMessage("Rozłączono z voice.").queue();
    }
}