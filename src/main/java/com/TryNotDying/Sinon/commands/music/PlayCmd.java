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

import com.TryNotDying.Sinon.audio.RequestMetadata;
import com.TryNotDying.Sinon.utils.TimeUtil;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.menu.ButtonMenu;
import com.TryNotDying.Sinon.Bot;
import com.TryNotDying.Sinon.audio.AudioHandler;
import com.TryNotDying.Sinon.audio.QueuedTrack;
import com.TryNotDying.Sinon.commands.DJCommand;
import com.TryNotDying.Sinon.commands.MusicCommand;
import com.TryNotDying.Sinon.playlist.PlaylistLoader.Playlist;
import com.TryNotDying.Sinon.utils.FormatUtil;
import java.util.concurrent.TimeUnit;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.exceptions.PermissionException;
import com.jagrosh.jdautilities.command.SlashCommand;
import net.dv8tion.jda.api.interactions.commands.OptionType;

/**
 * Above import dependencies
 * Below is the play (song) command
 */
public class PlayCmd extends SlashMusicCommand {

    private final static String LOAD = "\uD83D\uDCE5"; // 📥
    private final static String CANCEL = "\uD83D\uDEAB"; // 🚫

    private final String loadingEmoji;

    public PlayCmd(Bot bot) {
        super(bot);
        this.loadingEmoji = bot.getConfig().getLoading();
        this.name = "play";
        this.help = "plays the provided song";
        this.aliases = bot.getConfig().getAliases(this.name);
        this.category = new Category("Music");
        this.userPermissions = new Permission[]{Permission.MESSAGE_EMBED_LINKS};
        this.options = new Option[]{
            new Option("song", "Song name or URL", OptionType.STRING, true)
        };
    }

    @Override
    protected void doCommand(CommandEvent event) {
        String args = event.getOption("song").getAsString();
        event.reply(loadingEmoji + " Loading... `[" + args + "]`", m -> bot.getPlayerManager().loadItemOrdered(event.getGuild(), args, new ResultHandler(m, event, false)));
    }

    private class ResultHandler implements AudioLoadResultHandler {
        private final Message m;
        private final CommandEvent event;
        private final boolean ytsearch;

        private ResultHandler(Message m, CommandEvent event, boolean ytsearch) {
            this.m = m;
            this.event = event;
            this.ytsearch = ytsearch;
        }

        private void loadSingle(AudioTrack track, AudioPlaylist playlist) {
            if (bot.getConfig().isTooLong(track)) {
                m.editMessage(FormatUtil.filter(event.getClient().getWarning() + " This track (**" + track.getInfo().title + "**) is longer than the allowed maximum: `"
                        + TimeUtil.formatTime(track.getDuration()) + "` > `" + TimeUtil.formatTime(bot.getConfig().getMaxSeconds() * 1000) + "`")).queue();
                return;
            }
            AudioHandler handler = (AudioHandler) event.getGuild().getAudioManager().getSendingHandler();
            int pos = handler.addTrack(new QueuedTrack(track, RequestMetadata.fromResultHandler(track, event))) + 1;
            String addMsg = FormatUtil.filter(event.getClient().getSuccess() + " Added **" + track.getInfo().title
                    + "** (`" + TimeUtil.formatTime(track.getDuration()) + "`) " + (pos == 0 ? "to begin playing" : " to the queue at position " + pos));
            if (playlist == null || !event.getSelfMember().hasPermission(event.getTextChannel(), Permission.MESSAGE_ADD_REACTION)) {
                m.editMessage(addMsg).queue();
            } else {
                new ButtonMenu.Builder()
                        .setText(addMsg + "\n" + event.getClient().getWarning() + " This track has a playlist of **" + playlist.getTracks().size() + "** tracks attached. Select " + LOAD + " to load playlist.")
                        .setChoices(LOAD, CANCEL)
                        .setEventWaiter(bot.getWaiter())
                        .setTimeout(30, TimeUnit.SECONDS)
                        .setAction(re -> {
                            if (re.getName().equals(LOAD)) {
                                m.editMessage(addMsg + "\n" + event.getClient().getSuccess() + " Loaded **" + loadPlaylist(playlist, track) + "** additional tracks!").queue();
                            } else {
                                m.editMessage(addMsg).queue();
                            }
                        }).setFinalAction(m -> {
                            try {
                                m.clearReactions().queue();
                            } catch (PermissionException ignore) {}
                        }).build().display(m);
            }
        }

        private int loadPlaylist(AudioPlaylist playlist, AudioTrack exclude) {
            int[] count = {0};
            playlist.getTracks().stream().forEach((track) -> {
                if (!bot.getConfig().isTooLong(track) && !track.equals(exclude)) {
                    AudioHandler handler = (AudioHandler) event.getGuild().getAudioManager().getSendingHandler();
                    handler.addTrack(new QueuedTrack(track, RequestMetadata.fromResultHandler(track, event)));
                    count[0]++;
                }
            });
            return count[0];
        }

        @Override
        public void trackLoaded(AudioTrack track) {
            loadSingle(track, null);
        }

        @Override
        public void playlistLoaded(AudioPlaylist playlist) {
            if (playlist.getTracks().size() == 1 || playlist.isSearchResult()) {
                AudioTrack single = playlist.getSelectedTrack() == null ? playlist.getTracks().get(0) : playlist.getSelectedTrack();
                loadSingle(single, null);
            } else if (playlist.getSelectedTrack() != null) {
                AudioTrack single = playlist.getSelectedTrack();
                loadSingle(single, playlist);
            } else {
                int count = loadPlaylist(playlist, null);
                if (playlist.getTracks().size() == 0) {
                    m.editMessage(FormatUtil.filter(event.getClient().getWarning() + " The playlist " + (playlist.getName() == null ? "" : "(**" + playlist.getName()
                            + "**) ") + " could not be loaded or contained 0 entries")).queue();
                } else if (count == 0) {
                    m.editMessage(FormatUtil.filter(event.getClient().getWarning() + " All entries in this playlist " + (playlist.getName() == null ? "" : "(**" + playlist.getName()
                            + "**) ") + "were longer than the allowed maximum (`" + bot.getConfig().getMaxTime() + "`)")).queue();
                } else {
                    m.editMessage(FormatUtil.filter(event.getClient().getSuccess() + " Found "
                            + (playlist.getName() == null ? "a playlist" : "playlist **" + playlist.getName() + "**") + " with `"
                            + playlist.getTracks().size() + "` entries; added to the queue!"
                            + (count < playlist.getTracks().size() ? "\n" + event.getClient().getWarning() + " Tracks longer than the allowed maximum (`"
                            + bot.getConfig().getMaxTime() + "`) have been omitted." : ""))).queue();
                }
            }
        }

        @Override
        public void noMatches() {
            if (ytsearch) {
                m.editMessage(FormatUtil.filter(event.getClient().getWarning() + " No results found for `" + event.getArgs() + "`.")).queue();
            } else {
                bot.getPlayerManager().loadItemOrdered(event.getGuild(), "ytsearch:" + event.getArgs(), new ResultHandler(m, event, true));
            }
        }

        @Override
        public void loadFailed(FriendlyException throwable) {
            if (throwable.severity == Severity.COMMON) {
                m.editMessage(event.getClient().getError() + " Error loading: " + throwable.getMessage()).queue();
            } else {
                m.editMessage(event.getClient().getError() + " Error loading track.").queue();
            }
        }
    }

    public class PlaylistCmd extends SlashMusicCommand {
        public PlaylistCmd(Bot bot) {
            super(bot);
            this.name = "playlist";
            this.aliases = new String[]{"pl"};
            this.help = "plays the provided playlist";
            this.category = new Category("Music");
            this.userPermissions = new Permission[]{Permission.MESSAGE_EMBED_LINKS};
            this.options = new Option[]{
                new Option("playlist", "Playlist name", OptionType.STRING, true)
            };
        }

        @Override
        protected void doCommand(CommandEvent event) {
            String playlistName = event.getOption("playlist").getAsString();
            if (playlistName.isEmpty()) {
                event.reply(event.getClient().getError() + " Please include a playlist name.");
                return;
            }
            Playlist playlist = bot.getPlaylistLoader().getPlaylist(playlistName);
            if (playlist == null) {
                event.replyError("I could not find `" + playlistName + ".txt` in the Playlists folder.");
                return;
            }
            event.getChannel().sendMessage(loadingEmoji + " Loading playlist **" + playlistName + "**... (" + playlist.getItems().size() + " items)").queue(m -> {
                AudioHandler handler = (AudioHandler) event.getGuild().getAudioManager().getSendingHandler();
                playlist.loadTracks(bot.getPlayerManager(), (at) -> handler.addTrack(new QueuedTrack(at, RequestMetadata.fromResultHandler(at, event))), () -> {
                    StringBuilder builder = new StringBuilder(playlist.getTracks().isEmpty()
                            ? event.getClient().getWarning() + " No tracks were loaded!"
                            : event.getClient().getSuccess() + " Loaded **" + playlist.getTracks().size() + "** tracks!");
                    if (!playlist.getErrors().isEmpty()) {
                        builder.append("\nThe following tracks failed to load:");
                    }
                    playlist.getErrors().forEach(err -> builder.append("\n`[").append(err.getIndex() + 1).append("]` **").append(err.getItem()).append("**: ").append(err.getReason()));
                    String str = builder.toString();
                    if (str.length() > 2000) {
                        str = str.substring(0, 1994) + " (...)";
                    }
                    m.editMessage(FormatUtil.filter(str)).queue();
                });
            });
        }
    }
}