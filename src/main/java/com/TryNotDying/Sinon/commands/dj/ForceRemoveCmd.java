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
import com.jagrosh.jdautilities.commons.utils.FinderUtil;
import com.jagrosh.jdautilities.menu.OrderedMenu;
import com.TryNotDying.Sinon.Bot;
import com.TryNotDying.Sinon.audio.AudioHandler;
import com.TryNotDying.Sinon.commands.DJCommand;
import com.TryNotDying.Sinon.utils.FormatUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import java.util.List;
import java.util.concurrent.TimeUnit;
import com.jagrosh.jdautilities.command.SlashCommand;
import net.dv8tion.jda.api.interactions.commands.OptionType;

/**
 * Above import dependencies
 * Below is the command to remove someones queue
 */
public class ForceRemoveCmd extends SlashDJCommand {

    public ForceRemoveCmd(Bot bot) {
        super(bot);
        this.name = "forceremove";
        this.help = "removes all entries by a user from the queue";
        this.category = new Category("DJ");
        this.userPermissions = new Permission[]{Permission.MESSAGE_EMBED_LINKS};
        this.options = new Option[]{
            new Option("user", "User to remove from the queue", OptionType.USER, true)
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
                User target = event.getOption("user").getAsUser();
                AudioHandler handler = (AudioHandler) event.getGuild().getAudioManager().getSendingHandler();
                if (handler.getQueue().isEmpty()) {
                    event.replyError("There is nothing in the queue!");
                    return;
                }
                int count = handler.getQueue().removeAll(target.getIdLong());
                if (count == 0) {
                    event.replyWarning("**" + target.getName() + "** doesn't have any songs in the queue!");
                } else {
                    event.replySuccess("Successfully removed `" + count + "` entries from " + FormatUtil.formatUsername(target) + ".");
                }
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