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
package com.TryNotDying.Sinon.commands.admin;

import java.util.List;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.utils.FinderUtil;
import com.TryNotDying.Sinon.Bot;
import com.TryNotDying.Sinon.commands.AdminCommand;
import com.TryNotDying.Sinon.settings.Settings;
import com.TryNotDying.Sinon.utils.FormatUtil;
import net.dv8tion.jda.api.entities.TextChannel;
import com.jagrosh.jdautilities.command.SlashCommand;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import java.util.List;

/**
 * Above import dependencies
 * Below is the Set Channel Command
 */
public class SettcCmd extends SlashAdminCommand {

    public SettcCmd(Bot bot) {
        super(bot);
        this.name = "settc";
        this.help = "sets the text channel for music commands";
        this.category = new Category("Admin");
        this.userPermissions = new Permission[]{Permission.MESSAGE_EMBED_LINKS};
        this.options = new Option[]{
            new Option("channel", "Text channel name (or NONE to clear)", OptionType.CHANNEL, true)
        };
    }

    @Override
    protected void doCommand(CommandEvent event) {
        Member member = event.getMember();
        if (member == null) {
            event.replyError("You do not have permission to use this command.");
            return;
        }

        // Check if the user has the admin role
        List<Role> roles = member.getRoles();
        for (Role role : roles) {
            if (role.getIdLong() == bot.getConfig().getAdminRoleId()) {
                TextChannel channel = event.getOption("channel").getAsTextChannel();
                Settings s = event.getClient().getSettingsFor(event.getGuild());
                if (channel == null) {
                    s.setTextChannel(null);
                    event.reply(event.getClient().getSuccess() + " Music commands can now be used in any channel");
                } else {
                    s.setTextChannel(channel);
                    event.reply(event.getClient().getSuccess() + " Music commands can now only be used in <#" + channel.getId() + ">");
                }
                return; // Allow the user
            }
        }

        // If the user doesn't have the admin role, send an error embed.
        event.reply(new EmbedBuilder()
                .setColor(Color.RED)
                .setTitle("Error")
                .setDescription("You do not have permission to use this command.")
                .build());
    }
}