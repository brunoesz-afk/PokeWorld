package com.example.pokeworld.core;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.widget.VideoView;
import com.example.pokeworld.R;

public class VideoActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        VideoView videoView = new VideoView(this);
        setContentView(videoView);

        String videoPath = "android.resource://" + getPackageName() + "/" + R.raw.daycare_intro;
        videoView.setVideoURI(Uri.parse(videoPath));
        
        videoView.setOnCompletionListener(mp -> finish());
        videoView.start();
    }
}
