package com.example.studymate;

import java.io.*;

public class WaveHeaderWriter {
    public static void writeWavHeader(OutputStream out, int sampleRate, int channels, int bitsPerSample) throws IOException {
        byte[] header = new byte[44];

        long totalDataLen = 0; // 이후에 덮어씌움
        long byteRate = sampleRate * channels * bitsPerSample / 8;

        // ChunkID "RIFF"
        header[0] = 'R'; header[1] = 'I'; header[2] = 'F'; header[3] = 'F';

        // ChunkSize (임시 0, 나중에 update)
        header[4] = 0; header[5] = 0; header[6] = 0; header[7] = 0;

        // Format "WAVE"
        header[8] = 'W'; header[9] = 'A'; header[10] = 'V'; header[11] = 'E';

        // Subchunk1ID "fmt "
        header[12] = 'f'; header[13] = 'm'; header[14] = 't'; header[15] = ' ';

        // Subchunk1Size (16 for PCM)
        header[16] = 16; header[17] = 0; header[18] = 0; header[19] = 0;

        // AudioFormat (1 for PCM)
        header[20] = 1; header[21] = 0;

        // NumChannels
        header[22] = (byte) channels; header[23] = 0;

        // SampleRate
        header[24] = (byte) (sampleRate & 0xff);
        header[25] = (byte) ((sampleRate >> 8) & 0xff);
        header[26] = (byte) ((sampleRate >> 16) & 0xff);
        header[27] = (byte) ((sampleRate >> 24) & 0xff);

        // ByteRate
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);

        // BlockAlign
        header[32] = (byte) (channels * bitsPerSample / 8);
        header[33] = 0;

        // BitsPerSample
        header[34] = (byte) bitsPerSample;
        header[35] = 0;

        // Subchunk2ID "data"
        header[36] = 'd'; header[37] = 'a'; header[38] = 't'; header[39] = 'a';

        // Subchunk2Size (임시 0, 나중에 덮어씀)
        header[40] = 0; header[41] = 0; header[42] = 0; header[43] = 0;

        out.write(header, 0, 44);
    }

    public static void updateWavHeader(File wavFile) throws IOException {
        RandomAccessFile raf = new RandomAccessFile(wavFile, "rw");
        long totalAudioLen = raf.length() - 44;
        long totalDataLen = totalAudioLen + 36;
        raf.seek(4);
        raf.write((byte) (totalDataLen & 0xff));
        raf.write((byte) ((totalDataLen >> 8) & 0xff));
        raf.write((byte) ((totalDataLen >> 16) & 0xff));
        raf.write((byte) ((totalDataLen >> 24) & 0xff));
        raf.seek(40);
        raf.write((byte) (totalAudioLen & 0xff));
        raf.write((byte) ((totalAudioLen >> 8) & 0xff));
        raf.write((byte) ((totalAudioLen >> 16) & 0xff));
        raf.write((byte) ((totalAudioLen >> 24) & 0xff));
        raf.close();
    }
}
