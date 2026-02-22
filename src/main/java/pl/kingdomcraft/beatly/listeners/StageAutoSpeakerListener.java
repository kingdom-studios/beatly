package pl.kingdomcraft.beatly.listeners;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.StageChannel;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class StageAutoSpeakerListener extends ListenerAdapter {

    @Override
    public void onGuildVoiceUpdate(GuildVoiceUpdateEvent event) {
        Member self = event.getGuild().getSelfMember();

        // interesuje nas tylko zmiana voice samego bota
        if (!event.getEntity().equals(self)) return;

        var joined = event.getChannelJoined();
        if (joined instanceof StageChannel stage) {
            // bot wszedł na stage -> wymuś speaker
            stage.requestToSpeak().queue(
                    ok -> {},
                    err -> System.out.println("requestToSpeak failed: " + err.getMessage())
            );
        }
    }
}