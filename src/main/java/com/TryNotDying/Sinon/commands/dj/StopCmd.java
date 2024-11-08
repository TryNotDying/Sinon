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
import com.TryNotDying.Sinon.audio.AudioHandler;
import com.TryNotDying.Sinon.commands.DJCommand;
import com.jagrosh.jdautilities.command.SlashCommand;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import java.util.List;

/**
 * Above import dependencies
 * Below is the stop song command
 */
public class StopCmd extends SlashDJCommand {

    public StopCmd(Bot bot) {
        super(bot);
        this.name = "stop";
        this.help = "stops the current song and clears the queue";
        this.aliases = bot.getConfig().getAliases(this.name);
        this.category = new Category("DJ");
        this.userPermissions = new Permission[]{Permission.MESSAGE_EMBED_LINKS};
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
                AudioHandler handler = (AudioHandler) event.getGuild().getAudioManager().getSendingHandler();
                handler.stopAndClear();
                event.getGuild().getAudioManager().closeAudioConnection();
                event.reply(event.getClient().getSuccess() + " The player has stopped and the queue has been cleared.");
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