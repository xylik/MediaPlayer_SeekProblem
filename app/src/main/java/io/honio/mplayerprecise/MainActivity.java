package io.honio.mplayerprecise;

import android.net.Uri;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.TextureView;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import net.protyposis.android.mediaplayer.MediaPlayer;

import java.io.File;

public class MainActivity extends AppCompatActivity {
    public static long lastSeekedPosition = 0;
    public static int lastVideoIndx = 0;
    private TextureView videoView;
    private IPlayer player;
    private Button seekBtn;
    private EditText seekTime;
    private Spinner fileChooser;
    private Button playPauseBtn;
//    private String[] fileNames = new String[] {"369", "371", "372", "373", "374", "375", "376", "378", "379", "zelena_maska"};
    private String[] fileNames = new String[] {"371_ffmpeg_cut"};
    private String vequenceVideoDir;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        videoView = (TextureView) findViewById(R.id.videoView);
        player = MediaPlayerImp.instance(this, videoView);
        seekBtn = (Button) findViewById(R.id.seekBtn);
        seekTime = (EditText) findViewById(R.id.seekTime);
        fileChooser = (Spinner) findViewById(R.id.fileChooser);
        playPauseBtn = (Button) findViewById(R.id.playBtn);

        vequenceVideoDir = Environment.getExternalStorageDirectory() + "/FFmpegVideos/";
        ArrayAdapter<String> filesAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, fileNames);
        fileChooser.setAdapter(filesAdapter);
        setListeners();
    }

    private void setListeners() {
        seekBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String seekTimeStr = seekTime.getText().toString().trim();
                if(TextUtils.isEmpty(seekTimeStr)) Toast.makeText(MainActivity.this, "Please set seek time", Toast.LENGTH_SHORT).show();
                else {
                    setUiActions(false);
                    lastSeekedPosition = Long.valueOf(seekTimeStr);
                    player.seekTo(lastSeekedPosition);
                }
            }
        });

        fileChooser.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                File video = new File(vequenceVideoDir + fileNames[i] + ".mp4");
                if(!video.exists()) throw new RuntimeException(fileNames[i] + ".mp4 file not present on path!");

                player.release();
                player.setDisplay(videoView);
                player.prepare(Uri.parse("file://" + video.getAbsolutePath()));

                if(lastVideoIndx != i) lastSeekedPosition = 0;
                lastVideoIndx = i;
                player.seekTo(lastSeekedPosition);
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                //empty
            }
        });

        player.setSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {
            @Override
            public void onSeekComplete(MediaPlayer mp) {
                setUiActions(true);
            }
        });

        playPauseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(player.isPlaying()) player.pause();
                else player.play();
            }
        });
    }

    private void setUiActions(boolean isEnable) {
        seekBtn.setEnabled(isEnable);
        seekTime.setEnabled(isEnable);
    }

    @Override
    protected void onResume() {
        super.onResume();
        fileChooser.setSelection(lastVideoIndx);
    }

    @Override
    protected void onPause() {
        super.onPause();
        player.release();
    }
}
