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
package com.TryNotDying.Sinon.commands.music;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.TryNotDying.Sinon.Bot;
import com.TryNotDying.Sinon.audio.AudioHandler;
import com.TryNotDying.Sinon.audio.QueuedTrack;
import com.TryNotDying.Sinon.commands.MusicCommand;
import com.TryNotDying.Sinon.settings.Settings;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.User;
import com.jagrosh.jdautilities.command.SlashCommand;
import net.dv8tion.jda.api.interactions.commands.OptionType;

/**
 * Above import dependencies
 * Below is the remove song command
 */
public class RemoveCmd extends SlashMusicCommand {

    public RemoveCmd(Bot bot) {
        super(bot);
        this.name = "remove";
        this.help = "removes a song from the queue";
        this.category = new Category("Music");
        this.userPermissions = new Permission[]{Permission.MESSAGE_EMBED_LINKS};
        this.options = new Option[]{
            new Option("position", "Position of the song to remove", OptionType.INTEGER, true)
        };
    }

    @Override
    protected void doCommand(CommandEvent event) {
        AudioHandler handler = (AudioHandler) event.getGuild().getAudioManager().getSendingHandler();
        if (handler.getQueue().isEmpty()) {
            event.replyError("There is nothing in the queue!");
            return;
        }
        String args = event.getOption("position").getAsString();
        if (args.equalsIgnoreCase("all")) {
            int count = handler.getQueue().removeAll(event.getAuthor().getIdLong());
            if (count == 0) {
                event.replyWarning("You don't have any songs in the queue!");
            } else {
                event.replySuccess("Successfully removed your " + count + " entries.");
            }
            return;
        }
        int pos;
        try {
            pos = Integer.parseInt(args);
        } catch (NumberFormatException e) {
            pos = 0;
        }
        if (pos < 1 || pos > handler.getQueue().size()) {
            event.replyError("Position must be a valid integer between 1 and " + handler.getQueue().size() + "!");
            return;
        }
        Settings settings = event.getClient().getSettingsFor(event.getGuild());
        boolean isDJ = event.getMember().hasPermission(Permission.MANAGE_SERVER);
        if (!isDJ) {
            isDJ = event.getMember().getRoles().contains(settings.getRole(event.getGuild()));
        }
        QueuedTrack qt = handler.getQueue().get(pos - 1);
        if (qt.getIdentifier() == event.getAuthor().getIdLong()) {
            handler.getQueue().remove(pos - 1);
            event.replySuccess("Removed **" + qt.getTrack().getInfo().title + "** from the queue");
        } else if (isDJ) {
            handler.getQueue().remove(pos - 1);
            User u;
            try {
                u = event.getJDA().getUserById(qt.getIdentifier());
            } catch (Exception e) {
                u = null;
            }
            event.replySuccess("Removed **" + qt.getTrack().getInfo().title
                    + "** from the queue (requested by " + (u == null ? "someone" : "**" + u.getName() + "**") + ")");
        } else {
            event.replyError("You cannot remove **" + qt.getTrack().getInfo().title + "** because you didn't add it!");
        }
    }
}