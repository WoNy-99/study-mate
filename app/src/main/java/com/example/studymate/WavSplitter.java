package com.example.studymate;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class WavSplitter {

    public static List<File> splitWavFile(File sourceFile, int chunkDurationSec, File outputDir) throws IOException {
        List<File> chunks = new ArrayList<>();

        try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(sourceFile))) {
            byte[] header = new byte[44];
            int read = in.read(header);
            if (read != 44 || !(header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F')) {
                throw new IOException("WAV 파일 아님 또는 헤더가 손상됨");
            }

            // WAV 헤더에서 파라미터 추출
            int channels = header[22] & 0xff;
            int sampleRate = (header[24] & 0xff) |
                    ((header[25] & 0xff) << 8) |
                    ((header[26] & 0xff) << 16) |
                    ((header[27] & 0xff) << 24);
            int bitsPerSample = header[34] & 0xff;

            int byteRate = sampleRate * channels * bitsPerSample / 8;
            int chunkSizeBytes = chunkDurationSec * byteRate;

            byte[] buffer = new byte[chunkSizeBytes];
            int index = 0;
            int bytesRead;

            while ((bytesRead = in.read(buffer)) > 0) {
                File chunkFile = new File(outputDir, "chunk_" + index + ".wav");
                try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(chunkFile))) {
                    out.write(buildWavHeader(bytesRead, sampleRate, channels, bitsPerSample));
                    out.write(buffer, 0, bytesRead);
                }
                chunks.add(chunkFile);
                index++;
            }
        }

        return chunks;
    }

    private static byte[] buildWavHeader(int audioLength, int sampleRate, int channels, int bitsPerSample) {
        int totalDataLen = audioLength + 36;
        int byteRate = sampleRate * channels * bitsPerSample / 8;
        byte[] header = new byte[44];

        // RIFF/WAVE header
        header[0] = 'R'; header[1] = 'I'; header[2] = 'F'; header[3] = 'F';
        header[4] = (byte)(totalDataLen & 0xff);
        header[5] = (byte)((totalDataLen >> 8) & 0xff);
        header[6] = (byte)((totalDataLen >> 16) & 0xff);
        header[7] = (byte)((totalDataLen >> 24) & 0xff);
        header[8] = 'W'; header[9] = 'A'; header[10] = 'V'; header[11] = 'E';
        header[12] = 'f'; header[13] = 'm'; header[14] = 't'; header[15] = ' ';
        header[16] = 16; header[17] = 0; header[18] = 0; header[19] = 0;
        header[20] = 1; header[21] = 0;
        header[22] = (byte) channels; header[23] = 0;
        header[24] = (byte)(sampleRate & 0xff);
        header[25] = (byte)((sampleRate >> 8) & 0xff);
        header[26] = (byte)((sampleRate >> 16) & 0xff);
        header[27] = (byte)((sampleRate >> 24) & 0xff);
        header[28] = (byte)(byteRate & 0xff);
        header[29] = (byte)((byteRate >> 8) & 0xff);
        header[30] = (byte)((byteRate >> 16) & 0xff);
        header[31] = (byte)((byteRate >> 24) & 0xff);
        header[32] = (byte)((channels * bitsPerSample) / 8); header[33] = 0;
        header[34] = (byte) bitsPerSample; header[35] = 0;
        header[36] = 'd'; header[37] = 'a'; header[38] = 't'; header[39] = 'a';
        header[40] = (byte)(audioLength & 0xff);
        header[41] = (byte)((audioLength >> 8) & 0xff);
        header[42] = (byte)((audioLength >> 16) & 0xff);
        header[43] = (byte)((audioLength >> 24) & 0xff);

        return header;
    }
}
