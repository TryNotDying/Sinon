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
package com.TryNotDying.Sinon.commands.owner;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.TryNotDying.Sinon.Bot;
import com.TryNotDying.Sinon.commands.OwnerCommand;
import com.TryNotDying.Sinon.settings.Settings;
import com.jagrosh.jdautilities.command.SlashCommand;
import net.dv8tion.jda.api.interactions.commands.OptionType;

/**
 * Above import dependencies
 * Below is the default playlist command
 */
public class AutoplaylistCmd extends OwnerCommand {
    private final Bot bot;

    public AutoplaylistCmd(Bot bot) {
        super(bot);
        this.bot = bot;
        this.name = "autoplaylist";
        this.help = "sets the default playlist for the server";
        this.category = new Category("Owner");
        this.userPermissions = new Permission[]{Permission.MESSAGE_EMBED_LINKS};
        this.options = new Option[]{
            new Option("playlist", "Playlist name (or NONE to clear)", OptionType.STRING, true)
        };
    }

    @Override
    protected void doCommand(CommandEvent event) {
        String pname = event.getOption("playlist").getAsString();
        if (pname.equalsIgnoreCase("none")) {
            Settings settings = event.getClient().getSettingsFor(event.getGuild());
            settings.setDefaultPlaylist(null);
            event.reply(event.getClient().getSuccess() + " Cleared the default playlist for **" + event.getGuild().getName() + "**");
            return;
        }
        pname = pname.replaceAll("\\s+", "_");
        if (bot.getPlaylistLoader().getPlaylist(pname) == null) {
            event.reply(event.getClient().getError() + " Could not find `" + pname + ".txt`!");
        } else {
            Settings settings = event.getClient().getSettingsFor(event.getGuild());
            settings.setDefaultPlaylist(pname);
            event.reply(event.getClient().getSuccess() + " The default playlist for **" + event.getGuild().getName() + "** is now `" + pname + "`");
        }
    }
}