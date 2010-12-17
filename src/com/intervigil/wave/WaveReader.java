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


    /**
     * Constructor; initializes WaveReader to read from given file
     *
     * @param path  path to input file
     * @param name  name of input file
     */
    public WaveReader(String path, String name) {
        input = new File(path + File.separator + name);
    }

    /**
     * Constructor; initializes WaveReader to read from given file
     *
     * @param file  handle to input file
     */
    public WaveReader(File file) {
        input = file;
    }

    /**
     * Open WAV file for reading
     *
     * @throws FileNotFoundException if input file does not exist
     * @throws IOException if WAV header information is invalid
     */
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

    /**
     * Get sample rate
     *
     * @return input file's sample rate
     */
    public int getSampleRate() {
        return mSampleRate;
    }

    /**
     * Get number of channels
     *
     * @return number of channels in input file
     */
    public int getChannels() {
        return mChannels;
    }

    /**
     * Get PCM format, S16LE or S8LE
     *
     * @return number of bits per sample
     */
    public int getPcmFormat() {
        return mSampleBits;
    }
    
    /**
     * Get file size
     *
     * @return total input file size in bytes
     */
    public int getFileSize() {
        return mFileSize + 8;
    }

    /**
     * Get input file's audio data size
     * Basically file size without headers included
     *
     * @return audio data size in bytes
     */
    public int getDataSize() {
        return mDataSize;
    }

    /**
     * Get input file length
     *
     * @return length of file in seconds
     */
    public int getLength() {
        if (mSampleRate == 0 || mChannels == 0 || (mSampleBits + 7) / 8 == 0) {
            return 0;
        } else {
            return mDataSize / (mSampleRate * mChannels * ((mSampleBits + 7) / 8));
        }
    }

    /**
     * Read audio data from input file (mono)
     *
     * @param dst  mono audio data output buffer
     * @param numSamples  number of samples to read
     *
     * @return number of samples read
     *
     * @throws IOException if file I/O error occurs
     */
    public int read(short[] dst, int numSamples) throws IOException {
        if (mChannels != 1) {
            return -1;
        }
        int index;
        for (index = 0; index < numSamples; index++) {
            short val = readUnsignedShortLE(inputStream);
            if (val == -1) {
                break;
            }
            dst[index] = val;
        }
        return index;
    }

    /**
     * Read audio data from input file (stereo)
     *
     * @param left  left channel audio output buffer
     * @param right  right channel audio output buffer
     * @param numSamples  number of samples to read
     *
     * @return number of samples read
     *
     * @throws IOException if file I/O error occurs
     */
    public int read(short[] left, short[] right, int numSamples) throws IOException {
        if (mChannels != 2) {
            return -1;
        }
        int index;
        for (index = 0; index < numSamples * 2; index++) {
            short val = readUnsignedShortLE(inputStream);
            if (val == -1) {
                break;
            }
            if (index % 2 == 0) {
                left[index] = val;
            } else {
                right[index] = val;
            }
        }
        return index;
    }

    /**
     * Close WAV file. WaveReader object cannot be used again following this call.
     *
     * @throws IOException if I/O error occurred closing filestream
     */
    public void closeWaveFile() throws IOException {
        inputStream.close();
    }
    
    private static int readUnsignedInt(BufferedInputStream in) throws IOException {
        int ret;
        byte[] buf = new byte[4];
        ret = in.read(buf);
        if (ret == -1) {
            return -1;
        } else {
            return (buf[0] & 0xFF << 24
                    | ((buf[1] & 0xFF) << 16)
                    | ((buf[2] & 0xFF) << 8)
                    | ((buf[3] & 0xFF)));
        }
    }
    
    private static int readUnsignedIntLE(BufferedInputStream in) throws IOException {
        int ret;
        byte[] buf = new byte[4];
        ret = in.read(buf);
        if (ret == -1) {
            return -1;
        } else {
            return (buf[0] & 0xFF
                    | ((buf[1] & 0xFF) << 8)
                    | ((buf[2] & 0xFF) << 16)
                    | ((buf[3] & 0xFF) << 24));
        }
    }
    
    private static short readUnsignedShortLE(BufferedInputStream in) throws IOException {
        int ret;
        byte[] buf = new byte[2];
        ret = in.read(buf);
        if (ret == -1) {
            return -1;
        } else {
            return (short) (buf[0] & 0xFF | ((buf[1] & 0xFF) << 8));
        }
    }
}
