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
import com.jagrosh.jdautilities.commons.JDAUtilitiesInfo;
import com.TryNotDying.Sinon.Bot;
import com.TryNotDying.Sinon.commands.OwnerCommand;
import com.TryNotDying.Sinon.utils.OtherUtil;
import com.sedmelluq.discord.lavaplayer.tools.PlayerLibrary;
import net.dv8tion.jda.api.JDAInfo;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.ChannelType;
import com.jagrosh.jdautilities.command.SlashCommand;

/**
 * Above import dependencies
 * Below is the Debug command
 */
public class DebugCmd extends OwnerCommand {
    private final static String[] PROPERTIES = {"java.version", "java.vm.name", "java.vm.specification.version",
        "java.runtime.name", "java.runtime.version", "java.specification.version",  "os.arch", "os.name"};

    private final Bot bot;

    public DebugCmd(Bot bot) {
        super(bot);
        this.bot = bot;
        this.name = "debug";
        this.help = "shows debug info";
        this.category = new Category("Owner");
        this.userPermissions = new Permission[]{Permission.MESSAGE_EMBED_LINKS};
    }

    @Override
    protected void doCommand(CommandEvent event) {
        StringBuilder sb = new StringBuilder();
        sb.append("```\nSystem Properties:");
        for (String key : PROPERTIES) {
            sb.append("\n  ").append(key).append(" = ").append(System.getProperty(key));
        }
        sb.append("\n\nSinon Information:")
                .append("\n  Version = ").append(OtherUtil.getCurrentVersion())
                .append("\n  Owner = ").append(bot.getConfig().getOwnerId())
                .append("\n  Prefix = ").append(bot.getConfig().getPrefix())
                .append("\n  AltPrefix = ").append(bot.getConfig().getAltPrefix())
                .append("\n  MaxSeconds = ").append(bot.getConfig().getMaxSeconds())
                .append("\n  NPImages = ").append(bot.getConfig().useNPImages())
                .append("\n  SongInStatus = ").append(bot.getConfig().getSongInStatus())
                .append("\n  StayInChannel = ").append(bot.getConfig().getStay())
                .append("\n  UseEval = ").append(bot.getConfig().useEval())
                .append("\n  UpdateAlerts = ").append(bot.getConfig().useUpdateAlerts());
        sb.append("\n\nDependency Information:")
                .append("\n  JDA Version = ").append(JDAInfo.VERSION)
                .append("\n  JDA-Utilities Version = ").append(JDAUtilitiesInfo.VERSION)
                .append("\n  Lavaplayer Version = ").append(PlayerLibrary.VERSION);
        long total = Runtime.getRuntime().totalMemory() / 1024 / 1024;
        long used = total - (Runtime.getRuntime().freeMemory() / 1024 / 1024);
        sb.append("\n\nRuntime Information:")
                .append("\n  Total Memory = ").append(total)
                .append("\n  Used Memory = ").append(used);
        sb.append("\n\nDiscord Information:")
                .append("\n  ID = ").append(event.getJDA().getSelfUser().getId())
                .append("\n  Guilds = ").append(event.getJDA().getGuildCache().size())
                .append("\n  Users = ").append(event.getJDA().getUserCache().size());
        sb.append("\n```");

        if (event.isFromType(ChannelType.PRIVATE)
                || event.getSelfMember().hasPermission(event.getTextChannel(), Permission.MESSAGE_ATTACH_FILES)) {
            event.getChannel().sendFile(sb.toString().getBytes(), "debug_information.txt").queue();
        } else {
            event.reply("Debug Information: " + sb.toString());
        }
    }
}