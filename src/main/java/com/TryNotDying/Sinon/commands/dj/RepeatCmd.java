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
package com.TryNotDying.Sinon.commands.dj;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.TryNotDying.Sinon.Bot;
import com.TryNotDying.Sinon.commands.DJCommand;
import com.TryNotDying.Sinon.settings.RepeatMode;
import com.TryNotDying.Sinon.settings.Settings;
import com.jagrosh.jdautilities.command.SlashCommand;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import java.util.List;

/**
 * Above inport dependencies
 * Below is the command to repeat current song, all songs, or none.
 */
public class RepeatCmd extends SlashDJCommand {

    public RepeatCmd(Bot bot) {
        super(bot);
        this.name = "repeat";
        this.help = "re-adds music to the queue when finished";
        this.category = new Category("DJ");
        this.userPermissions = new Permission[]{Permission.MESSAGE_EMBED_LINKS};
        this.options = new Option[]{
            new Option("mode", "Repeat mode (off, all, single)", OptionType.STRING, false)
        };
    }

    @Override
    protected void doCommand(CommandEvent event) {
        Member member = event.getMember();
        if (member == null) {
            event.replyError("You do not have permission to use this command.");
            return;
        }

        // Check if the user has the DJ role
        List<Role> roles = member.getRoles();
        for (Role role : roles) {
            if (role.getIdLong() == bot.getConfig().getDjRoleId()) {
                String args = event.getOption("mode") != null ? event.getOption("mode").getAsString() : "";
                RepeatMode value;
                Settings settings = event.getClient().getSettingsFor(event.getGuild());
                if (args.isEmpty()) {
                    if (settings.getRepeatMode() == RepeatMode.OFF) {
                        value = RepeatMode.ALL;
                    } else {
                        value = RepeatMode.OFF;
                    }
                } else if (args.equalsIgnoreCase("false") || args.equalsIgnoreCase("off")) {
                    value = RepeatMode.OFF;
                } else if (args.equalsIgnoreCase("true") || args.equalsIgnoreCase("on") || args.equalsIgnoreCase("all")) {
                    value = RepeatMode.ALL;
                } else if (args.equalsIgnoreCase("one") || args.equalsIgnoreCase("single")) {
                    value = RepeatMode.SINGLE;
                } else {
                    event.replyError("Valid options are `off`, `all` or `single` (or leave empty to toggle between `off` and `all`)");
                    return;
                }
                settings.setRepeatMode(value);
                event.replySuccess("Repeat mode is now `" + value.getUserFriendlyName() + "`");
                return; // Allow the user
            }
        }

        // If the user doesn't have the DJ role, send an error embed.
        event.reply(new EmbedBuilder()
                .setColor(Color.RED)
                .setTitle("Error")
                .setDescription("You do not have permission to use this command.")
                .build());
    }
}