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

import java.io.IOException;
import java.io.InputStream;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.TryNotDying.Sinon.Bot;
import com.TryNotDying.Sinon.commands.OwnerCommand;
import com.TryNotDying.Sinon.utils.OtherUtil;
import net.dv8tion.jda.api.entities.Icon;
import com.jagrosh.jdautilities.command.SlashCommand;
import net.dv8tion.jda.api.interactions.commands.OptionType;

/**
 * Above import dependencies
 * Below is the set avatar command
 */
public class SetavatarCmd extends OwnerCommand {

    public SetavatarCmd(Bot bot) {
        super(bot);
        this.name = "setavatar";
        this.help = "sets the avatar of the Sinon";
        this.category = new Category("Owner");
        this.userPermissions = new Permission[]{Permission.MESSAGE_EMBED_LINKS};
        this.options = new Option[]{
            new Option("url", "URL of the image to set as avatar", OptionType.STRING, true)
        };
    }

    @Override
    protected void doCommand(CommandEvent event) {
        String url = event.getOption("url").getAsString();
        InputStream s = OtherUtil.imageFromUrl(url);
        if (s == null) {
            event.reply(event.getClient().getError() + " Invalid or missing URL");
        } else {
            try {
                event.getSelfUser().getManager().setAvatar(Icon.from(s)).queue(
                        v -> event.reply(event.getClient().getSuccess() + " Successfully changed avatar."),
                        t -> event.reply(event.getClient().getError() + " Failed to set avatar.")
                );
            } catch (IOException e) {
                event.reply(event.getClient().getError() + " Could not load from provided URL.");
            }
        }
    }
}