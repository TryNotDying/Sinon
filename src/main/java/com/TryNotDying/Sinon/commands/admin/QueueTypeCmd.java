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

import com.jagrosh.jdautilities.command.CommandEvent;
import com.TryNotDying.Sinon.Bot;
import com.TryNotDying.Sinon.audio.AudioHandler;
import com.TryNotDying.Sinon.commands.AdminCommand;
import com.TryNotDying.Sinon.settings.QueueType;
import com.TryNotDying.Sinon.settings.Settings;
import com.jagrosh.jdautilities.command.SlashCommand;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import java.util.List;

/**
 * Above import dependencies
 * Below Queue Information System
 */
public class QueueTypeCmd extends SlashAdminCommand {

    public QueueTypeCmd(Bot bot) {
        super(bot);
        this.name = "queuetype";
        this.help = "changes the queue type";
        this.category = new Category("Admin");
        this.userPermissions = new Permission[]{Permission.MESSAGE_EMBED_LINKS};
        this.options = new Option[]{
            new Option("type", "The new queue type (" + String.join("|", QueueType.getNames()) + ")", OptionType.STRING, false)
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
                String args = event.getOption("type") != null ? event.getOption("type").getAsString() : "";
                QueueType value;
                Settings settings = event.getClient().getSettingsFor(event.getGuild());

                if (args.isEmpty()) {
                    QueueType currentType = settings.getQueueType();
                    event.reply(currentType.getEmoji() + " Current queue type is: `" + currentType.getUserFriendlyName() + "`.");
                    return;
                }

                try {
                    value = QueueType.valueOf(args.toUpperCase());
                } catch (IllegalArgumentException e) {
                    event.replyError("Invalid queue type. Valid types are: [" + String.join("|", QueueType.getNames()) + "]");
                    return;
                }

                if (settings.getQueueType() != value) {
                    settings.setQueueType(value);

                    AudioHandler handler = (AudioHandler) event.getGuild().getAudioManager().getSendingHandler();
                    if (handler != null) {
                        handler.setQueueType(value);
                    }
                }

                event.reply(value.getEmoji() + " Queue type was set to `" + value.getUserFriendlyName() + "`.");
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