package com.example.voicecatch_ver2.asr;

import android.content.Context;
import android.media.MediaPlayer;
import android.util.Log;


import java.io.File;
import java.io.IOException;

public class Player {

    private static final String TAG = "Player"; // 로그를 위한 태그 추가

    public interface PlaybackListener {
        void onPlaybackStarted();
        void onPlaybackStopped();
    }

    private MediaPlayer mediaPlayer;
    private PlaybackListener playbackListener;
    private final Context context;

    public Player(Context context) {
        // context가 null일 경우를 대비한 방어 코드
        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null.");
        }
        this.context = context.getApplicationContext(); // ApplicationContext 사용으로 메모리 누수 방지
    }

    public void setListener(PlaybackListener listener) {
        this.playbackListener = listener;
    }

    public void initializePlayer(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            Log.e(TAG, "File path is null or empty. Cannot initialize MediaPlayer.");
            return;
        }

        // 1. 파일 존재 여부 확인 (가장 중요한 수정)
        File audioFile = new File(filePath);
        if (!audioFile.exists()) {
            Log.e(TAG, "Audio file not found at: " + filePath);
            // 파일이 없으면 리스너를 통해 상태를 알리고 종료
            if (playbackListener != null) {
                playbackListener.onPlaybackStopped();
            }
            return;
        }

        releaseMediaPlayer();

        try {
            mediaPlayer = new MediaPlayer();

            mediaPlayer.setDataSource(filePath);

            mediaPlayer.setOnPreparedListener(mp -> {
                Log.d(TAG, "MediaPlayer prepared. Starting playback.");
                if (playbackListener != null) {
                    playbackListener.onPlaybackStarted();
                }
                mediaPlayer.start();
            });

            mediaPlayer.setOnCompletionListener(mp -> {
                Log.d(TAG, "Playback completed.");
                if (playbackListener != null) {
                    playbackListener.onPlaybackStopped();
                }
                releaseMediaPlayer();
            });

            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Log.e(TAG, "MediaPlayer error occurred. What: " + what + ", Extra: " + extra);
                if (playbackListener != null) {
                    playbackListener.onPlaybackStopped();
                }
                releaseMediaPlayer();
                return true; // 에러를 처리했음을 알림
            });

            Log.d(TAG, "Preparing MediaPlayer...");
            mediaPlayer.prepareAsync(); // 비동기적으로 준비하여 UI 스레드 차단 방지

        } catch (IOException | IllegalArgumentException | SecurityException e) {
            Log.e(TAG, "Error setting data source or preparing MediaPlayer", e);
            if (playbackListener != null) {
                playbackListener.onPlaybackStopped();
            }
            releaseMediaPlayer();
        }
    }

    public void startPlayback() {
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            // 이미 준비가 완료된 상태에서 다시 재생을 시작하는 경우
            try {
                mediaPlayer.start();
                if (playbackListener != null) {
                    playbackListener.onPlaybackStarted();
                }
            } catch (IllegalStateException e) {
                Log.e(TAG, "startPlayback called in an invalid state.", e);
            }
        }
    }

    public void stopPlayback() {
        if (mediaPlayer != null) { // isPlaying() 체크를 제거하여 어떤 상태에서든 중지 및 해제 가능하게 함
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
            } catch (IllegalStateException e) {
                Log.e(TAG, "stopPlayback called in an invalid state.", e);
            } finally {
                // stop() 호출 여부와 관계없이 항상 release를 호출하도록 수정
                if (playbackListener != null) {
                    playbackListener.onPlaybackStopped();
                }
                releaseMediaPlayer();
            }
        }
    }

    public boolean isPlaying() {
        try {
            return mediaPlayer != null && mediaPlayer.isPlaying();
        } catch (IllegalStateException e) {
            return false;
        }
    }

    private void releaseMediaPlayer() {
        if (mediaPlayer != null) {
            // 항상 리소스를 해제하기 위해 try-catch로 감쌈
            try {
                mediaPlayer.release();
            } catch (Exception e) {
                Log.e(TAG, "Exception during mediaPlayer.release()", e);
            }
            mediaPlayer = null;
            Log.d(TAG, "MediaPlayer released.");
        }
    }
}