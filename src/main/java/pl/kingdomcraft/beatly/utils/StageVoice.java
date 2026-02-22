package pl.kingdomcraft.beatly.utils;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;


public final class StageVoice {
    private StageVoice() {}

    public static void connect(Guild guild, AudioChannel channel) {
        var am = guild.getAudioManager();
        if (!am.isConnected()) {
            am.openAudioConnection(channel);
        }
    }
}
