/* WaveReader.java

   Copyright (c) 2010 Ethan Chen

   This program is free software; you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation; either version 2 of the License, or
   (at your option) any later version.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License along
   with this program; if not, write to the Free Software Foundation, Inc.,
   51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.intervigil.wave;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class WaveReader {
    private static final int WAV_HEADER_CHUNK_ID = 0x52494646;  // "RIFF"
    private static final int WAV_FORMAT = 0x57415645;  // "WAVE"
    private static final int WAV_FORMAT_CHUNK_ID = 0x666d7420; // "fmt "
    private static final int WAV_DATA_CHUNK_ID = 0x64617461; // "data"
    private static final int STREAM_BUFFER_SIZE = 4096;

    private File input;
    private BufferedInputStream inputStream;

    private int mSampleRate;
    private int mChannels;
    private int mSampleBits;
    private int mFileSize;
    private int mDataSize;

    public WaveReader(String path, String name) {
        input = new File(path + File.separator + name);
    }

    public WaveReader(File file) {
        input = file;
    }

    public void openWave() throws FileNotFoundException, IOException {
        FileInputStream fileStream = new FileInputStream(input);
        inputStream = new BufferedInputStream(fileStream, STREAM_BUFFER_SIZE);

        int headerId = readUnsignedInt(inputStream);  // should be "RIFF"
        if (headerId != WAV_HEADER_CHUNK_ID) {
            throw new IOException("WaveReader: Invalid header chunk ID");
        }
        mFileSize = readUnsignedIntLE(inputStream);  // length of header
        int format = readUnsignedInt(inputStream);  // should be "WAVE"
        if (format != WAV_FORMAT) {
            throw new IOException("WaveReader: Invalid format");
        }
        
        int formatId = readUnsignedInt(inputStream);  // should be "fmt "
        if (formatId != WAV_FORMAT_CHUNK_ID) {
            throw new IOException("WaveReader: Invalid format chunk ID");
        }
        int formatSize = readUnsignedIntLE(inputStream);
        if (formatSize != 16) {
            
        }
        int audioFormat = readUnsignedShortLE(inputStream);
        if (audioFormat != 1) {
            throw new IOException("WaveReader: Unable to read non-PCM WAV files");
        }
        mChannels = readUnsignedShortLE(inputStream);
        mSampleRate = readUnsignedIntLE(inputStream);
        int byteRate = readUnsignedIntLE(inputStream);
        int blockAlign = readUnsignedShortLE(inputStream);
        mSampleBits = readUnsignedShortLE(inputStream);
        
        int dataId = readUnsignedInt(inputStream);
        if (dataId != WAV_DATA_CHUNK_ID) {
            throw new IOException("WaveReader: Invalid data chunk ID");
        }
        mDataSize = readUnsignedIntLE(inputStream);
    }

    public int getSampleRate() {
        // returns sample rate, typically 22050
        return mSampleRate;
    }

    public int getChannels() {
        // returns number of channels, mono or stereo
        return mChannels;
    }

    public int getPcmFormat() {
        // returns PCM format, typically 16 bit PCM
        return mSampleBits;
    }
    
    public int getFileSize() {
        // return file size + 8 bytes for (header chunk ID + file size) fields
        return mFileSize + 8;
    }

    public int getDataSize() {
        // returns number of bytes of sound data
        return mDataSize;
    }

    public int getLength() {
        // returns length in seconds
        if (mSampleRate == 0 || mChannels == 0 || (mSampleBits + 7) / 8 == 0) {
            return 0;
        } else {
            return mDataSize
                    / (mSampleRate * mChannels * ((mSampleBits + 7) / 8));
        }
    }

    public int readShort(short[] outBuf, int numSamples) throws IOException {
        byte[] buf = new byte[numSamples * 2];
        int bytesRead = inputStream.read(buf);

        int outIndex = 0;
        for (int i = 0; i < bytesRead; i += 2) {
            outBuf[outIndex] = (short) ((0xff & buf[i]) | ((0xff & buf[i + 1]) << 8));
            outIndex++;
        }

        return outIndex;
    }
    
    public int readShort(short[] left, short[] right, int numSamples) throws IOException {
        return 0;
    }

    public void closeWaveFile() throws IOException {
        inputStream.close();
    }
    
    private static int readUnsignedInt(BufferedInputStream in) throws IOException {
        byte[] buf = new byte[4];
        in.read(buf);
        return (buf[0] & 0xFF << 24
                | ((buf[1] & 0xFF) << 16)
                | ((buf[2] & 0xFF) << 8)
                | ((buf[3] & 0xFF)));
    }
    
    private static int readUnsignedIntLE(BufferedInputStream in) throws IOException {
        byte[] buf = new byte[4];
        in.read(buf);
        return (buf[0] & 0xFF
                | ((buf[1] & 0xFF) << 8)
                | ((buf[2] & 0xFF) << 16)
                | ((buf[3] & 0xFF) << 24));
    }
    
    private static short readUnsignedShortLE(BufferedInputStream in) throws IOException {
        byte[] buf = new byte[2];
        in.read(buf);
        return (short) (buf[0] & 0xFF | ((buf[1] & 0xFF) << 8));
    }
}
