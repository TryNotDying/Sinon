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

import com.jagrosh.jdautilities.command.SlashCommand;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import com.TryNotDying.Sinon.Bot;
import java.util.List;
import java.util.concurrent.TimeUnit;
import com.jagrosh.jdautilities.menu.Paginator;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.TryNotDying.Sinon.audio.AudioHandler;
import com.TryNotDying.Sinon.audio.QueuedTrack;
import com.TryNotDying.Sinon.commands.MusicCommand;
import com.TryNotDying.Sinon.playlist.PlaylistLoader.Playlist;
import com.TryNotDying.Sinon.utils.FormatUtil;
import com.TryNotDying.Sinon.utils.TimeUtil;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.exceptions.PermissionException;

/**
 * Above import dependencies
 * Below is the soundcloud search command
 */
public class SCSearchCmd extends SlashMusicCommand {
    private final String loadingEmoji;
    private final Paginator.Builder builder;

    public SCSearchCmd(Bot bot) {
        super(bot);
        this.loadingEmoji = bot.getConfig().getLoading();
        this.name = "scsearch";
        this.help = "searches Soundcloud for a provided query";
        this.aliases = bot.getConfig().getAliases(this.name);
        this.category = new Category("Music");
        this.userPermissions = new Permission[]{Permission.MESSAGE_EMBED_LINKS};
        builder = new Paginator.Builder()
                .setColumns(1)
                .setFinalAction(m -> {try{m.clearReactions().queue();}catch(PermissionException ignore){}})
                .setItemsPerPage(10)
                .waitOnSinglePage(false)
                .useNumberedItems(true)
                .showPageNumbers(true)
                .wrapPageEnds(true)
                .setEventWaiter(bot.getWaiter())
                .setTimeout(1, TimeUnit.MINUTES);
    }

    @Override
    protected void doCommand(CommandEvent event) {
        String args = event.getOption("song").getAsString();
        event.reply(loadingEmoji + " Loading... `[" + args + "]`", m -> bot.getPlayerManager().loadItemOrdered(event.getGuild(), args, new ResultHandler(m, event)));
    }

    private class ResultHandler implements AudioLoadResultHandler {
        private final Message m;
        private final CommandEvent event;

        private ResultHandler(Message m, CommandEvent event) {
            this.m = m;
            this.event = event;
        }

        // ... Rest of your existing ResultHandler code ...
    }
}