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
import net.dv8tion.jda.api.entities.Role;
import com.jagrosh.jdautilities.command.SlashCommand;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.entities.Member;
import java.util.List;

/**
 * Above import dependencies
 * Below is Set DJ Role Command
 */
public class SetdjCmd extends SlashAdminCommand {

    public SetdjCmd(Bot bot) {
        super(bot);
        this.name = "setdj";
        this.help = "sets the DJ role for certain music commands";
        this.category = new Category("Admin");
        this.userPermissions = new Permission[]{Permission.MESSAGE_EMBED_LINKS};
        this.options = new Option[]{
            new Option("role", "Role name (or NONE to clear)", OptionType.ROLE, true)
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
                String args = event.getOption("role") != null ? event.getOption("role").getAsString() : "";
                Settings s = event.getClient().getSettingsFor(event.getGuild());
                if (args.equalsIgnoreCase("none")) {
                    s.setDJRole(null);
                    event.reply(event.getClient().getSuccess() + " DJ role cleared; Only Admins can use the DJ commands.");
                } else {
                    List<Role> list = FinderUtil.findRoles(event.getArgs(), event.getGuild());
                    if (list.isEmpty()) {
                        event.reply(event.getClient().getWarning() + " No Roles found matching \"" + event.getArgs() + "\"");
                    } else if (list.size() > 1) {
                        event.reply(event.getClient().getWarning() + FormatUtil.listOfRoles(list, event.getArgs()));
                    } else {
                        s.setDJRole(list.get(0));
                        event.reply(event.getClient().getSuccess() + " DJ commands can now be used by users with the **" + list.get(0).getName() + "** role.");
                    }
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