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
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import java.util.List;

/**
 * Above import dependencies
 * Below is the skip to song command
 */
public class SkiptoCmd extends SlashDJCommand {

    public SkiptoCmd(Bot bot) {
        super(bot);
        this.name = "skipto";
        this.help = "skips to the specified song";
        this.category = new Category("DJ");
        this.userPermissions = new Permission[]{Permission.MESSAGE_EMBED_LINKS};
        this.options = new Option[]{
            new Option("position", "Position of the song to skip to", OptionType.INTEGER, true)
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
                int index = event.getOption("position").getAsInt();

                AudioHandler handler = (AudioHandler) event.getGuild().getAudioManager().getSendingHandler();
                if (index < 1 || index > handler.getQueue().size()) {
                    event.reply(event.getClient().getError() + " Position must be a valid integer between 1 and " + handler.getQueue().size() + "!");
                    return;
                }
                handler.getQueue().skip(index - 1);
                event.reply(event.getClient().getSuccess() + " Skipped to **" + handler.getQueue().get(0).getTrack().getInfo().title + "**");
                handler.getPlayer().stopTrack();
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