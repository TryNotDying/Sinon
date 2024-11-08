/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.TryNotDying.Sinon.commands.music;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.TryNotDying.Sinon.Bot;
import com.TryNotDying.Sinon.audio.AudioHandler;
import com.TryNotDying.Sinon.audio.RequestMetadata;
import com.TryNotDying.Sinon.commands.DJCommand;
import com.TryNotDying.Sinon.commands.MusicCommand;
import com.TryNotDying.Sinon.utils.TimeUtil;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import java.util.List;
import com.jagrosh.jdautilities.command.SlashCommand;
import net.dv8tion.jda.api.interactions.commands.OptionType;

/**
 * Above import dependencies
 * Below is the seek too (time in song) command
 */
public class SeekCmd extends SlashMusicCommand {

    private final static Logger LOG = LoggerFactory.getLogger("Seeking");

    public SeekCmd(Bot bot) {
        super(bot);
        this.name = "seek";
        this.help = "seeks the current song";
        this.category = new Category("Music");
        this.userPermissions = new Permission[]{Permission.MESSAGE_EMBED_LINKS};
        this.options = new Option[]{
            new Option("position", "Position to seek (e.g. +1:10, -90, 1h10m, +90s)", OptionType.STRING, true)
        };
    }

    @Override
    protected void doCommand(CommandEvent event) {
        AudioHandler handler = (AudioHandler) event.getGuild().getAudioManager().getSendingHandler();
        AudioTrack playingTrack = handler.getPlayer().getPlayingTrack();
        if (!playingTrack.isSeekable()) {
            event.replyError("This track is not seekable.");
            return;
        }

        if (!DJCommand.checkDJPermission(event) && playingTrack.getUserData(RequestMetadata.class).getOwner() != event.getAuthor().getIdLong()) {
            event.replyError("You cannot seek **" + playingTrack.getInfo().title + "** because you didn't add it!");
            return;
        }

        String args = event.getOption("position").getAsString();
        TimeUtil.SeekTime seekTime = TimeUtil.parseTime(args);
        if (seekTime == null) {
            event.replyError("Invalid seek! Expected format: " + arguments + "\nExamples: `1:02:23` `+1:10` `-90`, `1h10m`, `+90s`");
            return;
        }

        long currentPosition = playingTrack.getPosition();
        long trackDuration = playingTrack.getDuration();

        long seekMilliseconds = seekTime.relative ? currentPosition + seekTime.milliseconds : seekTime.milliseconds;
        if (seekMilliseconds > trackDuration) {
            event.replyError("Cannot seek to `" + TimeUtil.formatTime(seekMilliseconds) + "` because the current track is `" + TimeUtil.formatTime(trackDuration) + "` long!");
            return;
        }

        try {
            playingTrack.setPosition(seekMilliseconds);
        } catch (Exception e) {
            event.replyError("An error occurred while trying to seek: " + e.getMessage());
            LOG.warn("Failed to seek track " + playingTrack.getIdentifier(), e);
            return;
        }
        event.replySuccess("Successfully seeked to `" + TimeUtil.formatTime(playingTrack.getPosition()) + "/" + TimeUtil.formatTime(playingTrack.getDuration()) + "`!");
    }
}