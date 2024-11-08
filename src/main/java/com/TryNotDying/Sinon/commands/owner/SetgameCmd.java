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
import net.dv8tion.jda.api.entities.Activity;
import com.jagrosh.jdautilities.command.SlashCommand;
import net.dv8tion.jda.api.interactions.commands.OptionType;

/**
 * Above import dependencies
 * Below is the set activity command
 */
public class SetgameCmd extends OwnerCommand {

    public SetgameCmd(Bot bot) {
        super(bot);
        this.name = "setgame";
        this.help = "sets the game Sinon is playing";
        this.category = new Category("Owner");
        this.userPermissions = new Permission[]{Permission.MESSAGE_EMBED_LINKS};
        this.options = new Option[]{
            new Option("action", "Action to perform (playing, streaming, listening, watching)", OptionType.STRING, true),
            new Option("text", "Game name or Stream information", OptionType.STRING, true)
        };
        this.children = new OwnerCommand[]{
            new SetlistenCmd(),
            new SetstreamCmd(),
            new SetwatchCmd()
        };
    }

    @Override
    protected void doCommand(CommandEvent event) {
        String action = event.getOption("action").getAsString();
        String text = event.getOption("text").getAsString();

        switch (action.toLowerCase()) {
            case "playing":
                try {
                    event.getJDA().getPresence().setActivity(text.isEmpty() ? null : Activity.playing(text));
                    event.reply(event.getClient().getSuccess() + " **" + event.getSelfUser().getName()
                            + "** is " + (text.isEmpty() ? "no longer playing anything." : "now playing `" + text + "`"));
                } catch (Exception e) {
                    event.reply(event.getClient().getError() + " The activity could not be set!");
                }
                break;
            case "stream":
            case "twitch":
            case "streaming":
                new SetstreamCmd().doCommand(event);
                break;
            case "listen":
            case "listening":
                new SetlistenCmd().doCommand(event);
                break;
            case "watch":
            case "watching":
                new SetwatchCmd().doCommand(event);
                break;
            default:
                event.reply(event.getClient().getWarning() + " Invalid action. Please specify the action to perform (playing, streaming, listening, watching) followed by the text");
                break;
        }
    }

    private class SetstreamCmd extends OwnerCommand {
        private SetstreamCmd() {
            this.name = "stream";
            this.aliases = new String[]{"twitch", "streaming"};
            this.help = "sets the game Sinon is playing to a stream";
            this.category = new Category("Owner");
            this.userPermissions = new Permission[]{Permission.MESSAGE_EMBED_LINKS};
            this.options = new Option[]{
                new Option("username", "Twitch username", OptionType.STRING, true),
                new Option("game", "Game name", OptionType.STRING, true)
            };
        }

        @Override
        protected void doCommand(CommandEvent event) {
            String username = event.getOption("username").getAsString();
            String game = event.getOption("game").getAsString();
            try {
                event.getJDA().getPresence().setActivity(Activity.streaming(game, "https://twitch.tv/" + username));
                event.replySuccess("**" + event.getSelfUser().getName()
                        + "** is now streaming `" + game + "`");
            } catch (Exception e) {
                event.reply(event.getClient().getError() + " The twitch channel or game could not be set! Please try again!");
            }
        }
    }

    private class SetlistenCmd extends OwnerCommand {
        private SetlistenCmd() {
            this.name = "listen";
            this.aliases = new String[]{"listening"};
            this.help = "sets the activity Sinon is listening to";
            this.category = new Category("Owner");
            this.userPermissions = new Permission[]{Permission.MESSAGE_EMBED_LINKS};
            this.options = new Option[]{
                new Option("title", "Title to listen to", OptionType.STRING, true)
            };
        }

        @Override
        protected void doCommand(CommandEvent event) {
            String title = event.getOption("title").getAsString();
            try {
                event.getJDA().getPresence().setActivity(Activity.listening(title));
                event.replySuccess("**" + event.getSelfUser().getName() + "** is now listening to `" + title + "`");
            } catch (Exception e) {
                event.reply(event.getClient().getError() + " The activity could not be set!");
            }
        }
    }

    private class SetwatchCmd extends OwnerCommand {
        private SetwatchCmd() {
            this.name = "watch";
            this.aliases = new String[]{"watching"};
            this.help = "sets the activity the bot is watching";
            this.category = new Category("Owner");
            this.userPermissions = new Permission[]{Permission.MESSAGE_EMBED_LINKS};
            this.options = new Option[]{
                new Option("title", "Title to watch", OptionType.STRING, true)
            };
        }

        @Override
        protected void doCommand(CommandEvent event) {
            String title = event.getOption("title").getAsString();
            try {
                event.getJDA().getPresence().setActivity(Activity.watching(title));
                event.replySuccess("**" + event.getSelfUser().getName() + "** is now watching `" + title + "`");
            } catch (Exception e) {
                event.reply(event.getClient().getError() + " The activity could not be set!");
            }
        }
    }
}