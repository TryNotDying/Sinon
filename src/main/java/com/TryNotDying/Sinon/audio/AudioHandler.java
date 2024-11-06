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
package com.TryNotDying.Sinon.audio;

import com.TryNotDying.Sinon.playlist.PlaylistLoader.Playlist;
import com.TryNotDying.Sinon.queue.AbstractQueue;
import com.TryNotDying.Sinon.settings.QueueType;
import com.TryNotDying.Sinon.utils.TimeUtil;
import com.TryNotDying.Sinon.settings.RepeatMode;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import com.TryNotDying.Sinon.settings.Settings;
import com.TryNotDying.Sinon.utils.FormatUtil;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioTrack;
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioTrack;
import java.nio.ByteBuffer;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.audio.AudioSendHandler;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONException;
import org.json.JSONObject;


/**
 * Above dependencies are imported
 * Below we have an exceptional audio handler class
 */
public class AudioHandler extends AudioEventAdapter implements AudioSendHandler {
    public final static String PLAY_EMOJI  = "\u25B6"; // ▶
    public final static String PAUSE_EMOJI = "\u23F8"; // ⏸
    public final static String STOP_EMOJI  = "\u23F9"; // ⏹

    private final static Logger LOGGER = LoggerFactory.getLogger(AudioHandler.class);

    private final List<AudioTrack> defaultQueue = new LinkedList<>();
    private final Set<String> votes = new HashSet<>();

    private final PlayerManager manager;
    private final AudioPlayer audioPlayer;
    private final long guildId;

    private AudioFrame lastFrame;
    private AbstractQueue<QueuedTrack> queue;

    protected AudioHandler(PlayerManager manager, Guild guild, AudioPlayer player) {
        this.manager = manager;
        this.audioPlayer = player;
        this.guildId = guild.getIdLong();

        this.setQueueType(manager.getBot().getSettingsManager().getSettings(guildId).getQueueType());
    }

    public void setQueueType(QueueType type) {
        queue = type.createInstance(queue); // Potential issue:  createInstance needs to handle null 'queue'
    }

    public int addTrackToFront(QueuedTrack qtrack) {
        if (audioPlayer.getPlayingTrack() == null) {
            audioPlayer.playTrack(qtrack.getTrack());
            return -1;
        } else {
            queue.addAt(0, qtrack);
            return 0;
        }
    }

    public int addTrack(QueuedTrack qtrack) {
        if (audioPlayer.getPlayingTrack() == null) {
            audioPlayer.playTrack(qtrack.getTrack());
            return -1;
        } else
            return queue.add(qtrack);
    }

    public AbstractQueue<QueuedTrack> getQueue() {
        return queue;
    }

    public void stopAndClear() {
        queue.clear();
        defaultQueue.clear();
        audioPlayer.stopTrack();
    }

    public boolean isMusicPlaying(JDA jda) {
        Guild guild = guild(jda); // Added this line to avoid potential NullPointerException
        return guild != null && guild.getSelfMember().getVoiceState().inVoiceChannel() && audioPlayer.getPlayingTrack() != null;
    }

    public Set<String> getVotes() {
        return votes;
    }

    public AudioPlayer getPlayer() {
        return audioPlayer;
    }

    public RequestMetadata getRequestMetadata() {
        AudioTrack playingTrack = audioPlayer.getPlayingTrack(); // Assign to variable to avoid repeated calls
        return playingTrack == null ? RequestMetadata.EMPTY : playingTrack.getUserData(RequestMetadata.class) != null ? playingTrack.getUserData(RequestMetadata.class) : RequestMetadata.EMPTY;
    }

    public boolean playFromDefault() {
        if (!defaultQueue.isEmpty()) {
            audioPlayer.playTrack(defaultQueue.remove(0));
            return true;
        }
        Settings settings = manager.getBot().getSettingsManager().getSettings(guildId);
        if (settings == null || settings.getDefaultPlaylist() == null)
            return false;

        Playlist pl = manager.getBot().getPlaylistLoader().getPlaylist(settings.getDefaultPlaylist());
        if (pl == null || pl.getItems().isEmpty())
            return false;
        pl.loadTracks(manager, (at) -> {
            if (audioPlayer.getPlayingTrack() == null)
                audioPlayer.playTrack(at);
            else
                defaultQueue.add(at);
        }, () -> {
            if (pl.getTracks().isEmpty() && !manager.getBot().getConfig().getStay())
                manager.getBot().closeAudioConnection(guildId);
        });
        return true;
    }

    // Audio Events
    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        RepeatMode repeatMode = manager.getBot().getSettingsManager().getSettings(guildId).getRepeatMode();
        if (endReason == AudioTrackEndReason.FINISHED && repeatMode != RepeatMode.OFF) {
            QueuedTrack clone = new QueuedTrack(track.makeClone(), track.getUserData(RequestMetadata.class));
            if (repeatMode == RepeatMode.ALL)
                queue.add(clone);
            else
                queue.addAt(0, clone);
        }

        if (queue.isEmpty()) {
            if (!playFromDefault()) {
                manager.getBot().getNowplayingHandler().onTrackUpdate(null);
                if (!manager.getBot().getConfig().getStay())
                    manager.getBot().closeAudioConnection(guildId);
                player.setPaused(false);
            }
        } else {
            QueuedTrack qt = queue.pull();
            if(qt != null) { //Null check added
                player.playTrack(qt.getTrack());
            } else {
                LOGGER.error("Pulled track from queue is null!");
            }
        }
    }

    @Override
    public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception) {
        if (exception.getMessage().toLowerCase().contains("sign in")) {
            LOGGER.error(
                    "Track {} has failed to play: {}. "
                            + "You will need to sign in to Google to play YouTube tracks. "
                            + "More info: https://jmusicbot.com/youtube-oauth2",
                    track.getIdentifier(),
                    exception.getMessage()
            );
        } else {
            LOGGER.error("Track {} has failed to play: {}", track.getIdentifier(), exception.getMessage(), exception);
        }
    }

    @Override
    public void onTrackStart(AudioPlayer player, AudioTrack track) {
        votes.clear();
        manager.getBot().getNowplayingHandler().onTrackUpdate(track);
    }


    // Formatting
    public Message getNowPlaying(JDA jda) {
        if (isMusicPlaying(jda)) {
            Guild guild = guild(jda);
            if (guild == null) return null; //Handle null guild
            AudioTrack track = audioPlayer.getPlayingTrack();
            MessageBuilder mb = new MessageBuilder();
            mb.append(FormatUtil.filter(manager.getBot().getConfig().getSuccess() + "Requested In: " + guild.getSelfMember().getVoiceState().getChannel().getAsMention()));
            EmbedBuilder eb = new EmbedBuilder();
            eb.setColor(guild.getSelfMember().getColor());
            eb.setImage("https://i.pinimg.com/originals/3e/fe/1c/3efe1cb845954233246f60d5d8395dd0.gif");
            RequestMetadata rm = getRequestMetadata();

            String djName = "Sinon"; // Default if no DJ info is found
            User u = guild.getJDA().getUserById(rm.user.id);; // Declare u outside the if block
            if (rm != null && rm.user != null && rm.user.id != 0L) {
                u = guild.getJDA().getUserById(rm.user.id);
            }
            eb.setAuthor(FormatUtil.formatUsername(u) + "'s currently the DJ in " + guild.getSelfMember().getVoiceState().getChannel(), null, (u != null) ? u.getEffectiveAvatarUrl() : null);
            eb.setTitle(track.getInfo().title, track.getInfo().uri);
            if (track instanceof YoutubeAudioTrack && manager.getBot().getConfig().useNPImages()) {
                eb.setThumbnail("https://i.ytimg.com/vi/" + track.getIdentifier() + "/hqdefault.jpg");
            } else if (track instanceof SoundCloudAudioTrack && manager.getBot().getConfig().useNPImages()) {
                String trackId = extractSoundCloudTrackId(track);
                if (trackId != null) {
                    try {
                        String artworkUrl = getSoundCloudArtworkUrl(trackId);
                        eb.setThumbnail(artworkUrl != null ? artworkUrl : "https://w.soundcloud.com/icon/assets/images/black_white_64-94fc761.png");
                    } catch (IOException | InterruptedException | JSONException e) {
                        LOGGER.error("Error fetching SoundCloud artwork: {}", e.getMessage(), e);
                    }
                } else {
                    eb.setThumbnail("https://w.soundcloud.com/icon/assets/images/black_white_64-94fc761.png");
                }
            }

            if (track.getInfo().author != null && !track.getInfo().author.isEmpty()) {
                eb.setFooter("Source: " + track.getInfo().author, null);
            }

            double progress = (double) audioPlayer.getPlayingTrack().getPosition() / track.getDuration();
            eb.setDescription(getStatusEmoji()
                    + " " + FormatUtil.progressBar(progress)
                    + " `[" + TimeUtil.formatTime(track.getPosition()) + "/" + TimeUtil.formatTime(track.getDuration()) + "]` "
                    + FormatUtil.volumeIcon(audioPlayer.getVolume()));

            return mb.setEmbeds(eb.build()).build();
        } else {
            return null;
        }
    }

    private String extractSoundCloudTrackId(AudioTrack track) {
        String uri = track.getInfo().uri;
        Pattern pattern = Pattern.compile("tracks\\/([0-9]+)");
        Matcher matcher = pattern.matcher(uri);
        return matcher.find() ? matcher.group(1) : null;
    }

    //Improved SoundCloud artwork retrieval using a more efficient method.  Consider using a proper SoundCloud API client library.
    private String getSoundCloudArtworkUrl(String trackId) throws IOException, InterruptedException, JSONException {
        //  Replace with your actual SoundCloud Client ID.  GET a client ID from SoundCloud's developer portal.
        String clientId = "YOUR_SOUNDCLOUD_CLIENT_ID"; // Obtain a client ID from SoundCloud's developer portal.
        String apiUrl = "https://api.soundcloud.com/tracks/" + trackId + "?client_id=" + clientId;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .build();

        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            JSONObject json = new JSONObject(response.body());
            return json.getString("artwork_url");
        } else {
            LOGGER.error("SoundCloud API request failed with status code: {}", response.statusCode());
            return null;
        }
    }

    public Message getNoMusicPlaying(JDA jda) {
        Guild guild = guild(jda);
        return new MessageBuilder()
                .setContent(FormatUtil.filter(manager.getBot().getConfig().getSuccess() + " **Now Playing...**"))
                .setEmbeds(new EmbedBuilder()
                        .setTitle("No music playing")
                        .setDescription(STOP_EMOJI + " " + FormatUtil.progressBar(-1) + " " + FormatUtil.volumeIcon(audioPlayer.getVolume()))
                        .setColor(guild == null ? java.awt.Color.GRAY : guild.getSelfMember().getColor()) // Handle potential null guild
                        .build()).build();
    }

    public String getStatusEmoji() {
        return audioPlayer.isPaused() ? PAUSE_EMOJI : PLAY_EMOJI;
    }

    // Audio Send Handler methods

    @Override
    public boolean canProvide() {
        lastFrame = audioPlayer.provide();
        return lastFrame != null;
    }

    @Override
    public ByteBuffer provide20MsAudio() {
        return ByteBuffer.wrap(lastFrame != null ? lastFrame.getData() : new byte[0]); //Handle potential null lastFrame
    }

    @Override
    public boolean isOpus() {
        return true;
    }


    // Private methods
    private Guild guild(JDA jda) {
        return jda.getGuildById(guildId);
    }
}