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
package com.TryNotDying.Sinon.commands.general;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.TryNotDying.Sinon.Bot;
import com.TryNotDying.Sinon.settings.QueueType;
import com.TryNotDying.Sinon.settings.RepeatMode;
import com.TryNotDying.Sinon.settings.Settings;
import com.TryNotDying.Sinon.utils.FormatUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.VoiceChannel;
import com.jagrosh.jdautilities.command.SlashCommand;
import net.dv8tion.jda.api.entities.Member;
import java.util.List;

/**
 * Above import dependencies
 * Below in the settings command for Sinon
 */
public class SettingsCmd extends SlashAdminCommand {

    private final static String EMOJI = "\uD83C\uDFA7"; // 🎧

    public SettingsCmd(Bot bot) {
        super(bot);
        this.name = "settings";
        this.help = "shows the bots settings";
        this.category = new Category("Admin");
        this.userPermissions = new Permission[]{Permission.MESSAGE_EMBED_LINKS};
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
                Settings s = event.getClient().getSettingsFor(event.getGuild());
                MessageBuilder builder = new MessageBuilder()
                        .append(EMOJI + " **")
                        .append(FormatUtil.filter(event.getSelfUser().getName()))
                        .append("** settings:");
                TextChannel tchan = s.getTextChannel(event.getGuild());
                VoiceChannel vchan = s.getVoiceChannel(event.getGuild());
                Role role1 = s.getRole(event.getGuild());
                EmbedBuilder ebuilder = new EmbedBuilder()
                        .setColor(event.getSelfMember().getColor())
                        .setDescription("Text Channel: " + (tchan == null ? "Any" : "**#" + tchan.getName() + "**")
                                + "\nVoice Channel: " + (vchan == null ? "Any" : vchan.getAsMention())
                                + "\nDJ Role: " + (role1 == null ? "None" : "**" + role1.getName() + "**")
                                + "\nCustom Prefix: " + (s.getPrefix() == null ? "None" : "`" + s.getPrefix() + "`")
                                + "\nRepeat Mode: " + (s.getRepeatMode() == RepeatMode.OFF
                                                        ? s.getRepeatMode().getUserFriendlyName()
                                                        : "**"+s.getRepeatMode().getUserFriendlyName()+"**")
                                + "\nQueue Type: " + (s.getQueueType() == QueueType.FAIR
                                                        ? s.getQueueType().getUserFriendlyName()
                                                        : "**"+s.getQueueType().getUserFriendlyName()+"**")
                                + "\nDefault Playlist: " + (s.getDefaultPlaylist() == null ? "None" : "**" + s.getDefaultPlaylist() + "**")
                                )
                        .setFooter(event.getJDA().getGuilds().size() + " servers | "
                                + event.getJDA().getGuilds().stream().filter(g -> g.getSelfMember().getVoiceState().inVoiceChannel()).count()
                                + " audio connections", null);
                event.getChannel().sendMessage(builder.setEmbeds(ebuilder.build()).build()).queue();
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