package com.streamlite.player;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.rtmp.RtmpDataSource;
import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector;
import androidx.media3.exoplayer.mediacodec.MediaCodecUtil;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.ui.PlayerView;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@UnstableApi
public final class MainActivity extends Activity {
    private static final String DEFAULT_URL = "rtmp://192.168.1.10/live/stream";

    private PlayerView playerView;
    private EditText urlInput;
    private CheckBox preferSoftware;
    private TextView status;
    private ExoPlayer player;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        playerView = findViewById(R.id.player_view);
        urlInput = findViewById(R.id.url_input);
        preferSoftware = findViewById(R.id.prefer_software);
        status = findViewById(R.id.status);
        Button play = findViewById(R.id.play_button);
        Button stop = findViewById(R.id.stop_button);

        String savedUrl = getPreferences(MODE_PRIVATE).getString("url", DEFAULT_URL);
        boolean savedSoftware = getPreferences(MODE_PRIVATE).getBoolean("software", true);
        urlInput.setText(savedUrl);
        preferSoftware.setChecked(savedSoftware);

        play.setOnClickListener(v -> startPlayback());
        stop.setOnClickListener(v -> stopPlayback());
        preferSoftware.setOnCheckedChangeListener((buttonView, isChecked) ->
                getPreferences(MODE_PRIVATE).edit().putBoolean("software", isChecked).apply());
        urlInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                getPreferences(MODE_PRIVATE).edit().putString("url", s.toString().trim()).apply();
            }
        });
    }

    private void startPlayback() {
        String url = urlInput.getText().toString().trim();
        if (!url.startsWith("rtmp://")) {
            setStatus("Enter an RTMP URL");
            return;
        }

        releasePlayer();
        DefaultMediaSourceFactory mediaSourceFactory =
                new DefaultMediaSourceFactory(new RtmpDataSource.Factory());
        DefaultRenderersFactory renderersFactory = new DefaultRenderersFactory(this);
        if (preferSoftware.isChecked()) {
            renderersFactory.setMediaCodecSelector(softwareFirstCodecSelector());
        }

        ExoPlayer.Builder builder = new ExoPlayer.Builder(this, renderersFactory)
                .setMediaSourceFactory(mediaSourceFactory);

        player = builder.build();
        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_BUFFERING) {
                    setStatus("Buffering");
                } else if (playbackState == Player.STATE_READY) {
                    setStatus(preferSoftware.isChecked() ? "Playing, software preferred" : "Playing");
                } else if (playbackState == Player.STATE_ENDED) {
                    setStatus("Stream ended");
                }
            }

            @Override
            public void onPlayerError(PlaybackException error) {
                setStatus(error.getErrorCodeName());
            }
        });
        playerView.setPlayer(player);
        player.setMediaItem(MediaItem.fromUri(Uri.parse(url)));
        player.prepare();
        player.play();
        setStatus("Connecting");
    }

    private MediaCodecSelector softwareFirstCodecSelector() {
        return (mimeType, requiresSecureDecoder, requiresTunnelingDecoder) -> {
            List<androidx.media3.exoplayer.mediacodec.MediaCodecInfo> codecs =
                    MediaCodecUtil.getDecoderInfos(mimeType, requiresSecureDecoder, requiresTunnelingDecoder);
            List<androidx.media3.exoplayer.mediacodec.MediaCodecInfo> sorted = new ArrayList<>(codecs);
            sorted.sort(Comparator.comparingInt(codec -> isSoftwareCodec(codec.name) ? 0 : 1));
            return sorted;
        };
    }

    private boolean isSoftwareCodec(String name) {
        String n = name.toLowerCase();
        return n.startsWith("omx.google.")
                || n.startsWith("c2.android.")
                || n.contains(".sw.")
                || n.contains("software");
    }

    private void stopPlayback() {
        releasePlayer();
        setStatus("Stopped");
    }

    private void setStatus(String text) {
        status.setText(text);
    }

    @Override
    protected void onStop() {
        super.onStop();
        releasePlayer();
    }

    @Override
    protected void onDestroy() {
        releasePlayer();
        super.onDestroy();
    }

    private void releasePlayer() {
        if (player != null) {
            playerView.setPlayer(null);
            player.release();
            player = null;
        }
    }
}
