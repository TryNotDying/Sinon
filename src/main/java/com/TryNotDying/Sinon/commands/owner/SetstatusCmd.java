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
import com.TryNotDying.Sinon.Bot;
import com.TryNotDying.Sinon.commands.OwnerCommand;
import net.dv8tion.jda.api.OnlineStatus;
import com.jagrosh.jdautilities.command.SlashCommand;
import net.dv8tion.jda.api.interactions.commands.OptionType;

/**
 * Above import dependencies
 * Below is the set status command
 */
public class SetstatusCmd extends OwnerCommand {

    public SetstatusCmd(Bot bot) {
        super(bot);
        this.name = "setstatus";
        this.help = "sets the status Sinon displays";
        this.category = new Category("Owner");
        this.userPermissions = new Permission[]{Permission.MESSAGE_EMBED_LINKS};
        this.options = new Option[]{
            new Option("status", "New status for the bot", OptionType.STRING, true)
        };
    }

    @Override
    protected void doCommand(CommandEvent event) {
        try {
            String status = event.getOption("status").getAsString();
            OnlineStatus onlineStatus = OnlineStatus.fromKey(status);
            if (onlineStatus == OnlineStatus.UNKNOWN) {
                event.replyError("Please include one of the following statuses: `ONLINE`, `IDLE`, `DND`, `INVISIBLE`, `STREAMING`, `PLAYING`");
            } else {
                event.getJDA().getPresence().setStatus(onlineStatus);
                event.replySuccess("Set the status to `" + onlineStatus.getKey().toUpperCase() + "`");
            }
        } catch (Exception e) {
            event.reply(event.getClient().getError() + " The status could not be set!");
        }
    }
}