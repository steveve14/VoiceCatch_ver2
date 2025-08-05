package com.example.voicecatch_ver2.engine;

import java.io.IOException;

public interface WhisperEngine {
    boolean isInitialized();
    boolean initialize(String modelPath, String vocabPath, boolean multilingual) throws IOException;
    void deinitialize();
    String transcribeFile(String wavePath);
    String transcribeBuffer(float[] samples);
}
