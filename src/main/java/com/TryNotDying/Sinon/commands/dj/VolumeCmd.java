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
import com.TryNotDying.Sinon.settings.Settings;
import com.TryNotDying.Sinon.utils.FormatUtil;
import com.jagrosh.jdautilities.command.SlashCommand;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import java.util.List;

/**
 * Above import dependencies
 * Below is the volume control commands, depricated when you can use discords built in volume. Can come in handy for unusually quiet songs.
 */
public class VolumeCmd extends SlashDJCommand {

    public VolumeCmd(Bot bot) {
        super(bot);
        this.name = "volume";
        this.aliases = bot.getConfig().getAliases(this.name);
        this.help = "sets or shows volume";
        this.category = new Category("DJ");
        this.userPermissions = new Permission[]{Permission.MESSAGE_EMBED_LINKS};
        this.options = new Option[]{
            new Option("volume", "Volume level (5-150)", OptionType.INTEGER, false)
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
                AudioHandler handler = (AudioHandler) event.getGuild().getAudioManager().getSendingHandler();
                Settings settings = event.getClient().getSettingsFor(event.getGuild());
                int volume = handler.getPlayer().getVolume();
                String args = event.getOption("volume") != null ? event.getOption("volume").getAsString() : "";
                if (args.isEmpty()) {
                    event.reply(FormatUtil.volumeIcon(volume) + " Current volume is `" + volume + "`");
                } else {
                    int nvolume;
                    try {
                        nvolume = Integer.parseInt(args);
                    } catch (NumberFormatException e) {
                        nvolume = -1;
                    }
                    if (nvolume < 0 || nvolume > 150) {
                        event.reply(event.getClient().getError() + " Volume must be a valid integer between 5 and 200!");
                    } else {
                        handler.getPlayer().setVolume(nvolume);
                        settings.setVolume(nvolume);
                        event.reply(FormatUtil.volumeIcon(nvolume) + " Volume changed from `" + volume + "` to `" + nvolume + "`");
                    }
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