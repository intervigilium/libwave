/* WaveWriter.java

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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

public class WaveWriter {
    private static final int OUTPUT_STREAM_BUFFER = 16384;

    private File output;
    private BufferedOutputStream outputStream;
    private int bytesWritten;

    private int sampleRate;
    private int channels;
    private int sampleBits;


    /**
     * Constructor; initializes WaveWriter with file name and path
     *
     * @param path  output file path
     * @param name  output file name
     * @param sampleRate  output sample rate
     * @param channels  number of channels
     * @param sampleBits  number of bits per sample (S8LE, S16LE)
     */
    public WaveWriter(String path, String name, int sampleRate, int channels,
            int sampleBits) {
        this.output = new File(path + File.separator + name);

        this.sampleRate = sampleRate;
        this.channels = channels;
        this.sampleBits = sampleBits;

        this.bytesWritten = 0;
    }

    /**
     * Constructor; initializes WaveWriter with file name and path
     *
     * @param file  output file handle
     * @param sampleRate  output sample rate
     * @param channels  number of channels
     * @param sampleBits  number of bits per sample (S8LE, S16LE)
     */
    public WaveWriter(File file, int sampleRate, int channels, int sampleBits) {
        this.output = file;

        this.sampleRate = sampleRate;
        this.channels = channels;
        this.sampleBits = sampleBits;

        this.bytesWritten = 0;
    }

    /**
     * Create output WAV file
     *
     * @return whether file creation succeeded
     *
     * @throws IOException if file I/O error occurs allocating header
     */
    public boolean createWaveFile() throws IOException {
        if (this.output.exists()) {
            this.output.delete();
        }

        if (this.output.createNewFile()) {
            FileOutputStream fileStream = new FileOutputStream(output);
            this.outputStream = new BufferedOutputStream(fileStream,
                    OUTPUT_STREAM_BUFFER);
            // write 44 bytes of space for the header
            this.outputStream.write(new byte[44]);
            return true;
        }
        return false;
    }

    /**
     * Read audio data from input file (mono)
     *
     * @param src  mono audio data input buffer
     * @param bufferSize  buffer size in number of samples
     *
     * @throws IOException if file I/O error occurs
     */
    public void write(short[] src, int bufferSize) throws IOException {
        for (int i = 0; i < bufferSize; i++) {
            writeUnsignedShortLE(this.outputStream, src[i]);
            bytesWritten += 2;
        }
    }

    /**
     * Close output WAV file and write WAV header. WaveWriter
     * cannot be used again following this call.
     *
     * @throws IOException if file I/O error occurs writing WAV header
     */
    public void closeWaveFile() throws IOException {
        this.outputStream.flush();
        this.outputStream.close();
        writeWaveHeader();
    }

    private void writeWaveHeader() throws IOException {
        // rewind to beginning of the file
        RandomAccessFile file = new RandomAccessFile(this.output, "rw");
        file.seek(0);

        int bytesPerSec = (sampleBits + 7) / 8;

        file.writeBytes("RIFF"); // wave label
        file.writeInt(Integer.reverseBytes(bytesWritten + 36)); // length in
                                                                // bytes without
                                                                // header
        file.writeBytes("WAVEfmt ");
        file.writeInt(Integer.reverseBytes(16)); // length of pcm format
                                                 // declaration area
        file.writeShort(Short.reverseBytes((short) 1)); // is PCM
        file.writeShort(Short.reverseBytes((short) channels)); // number of
                                                               // channels, this
                                                               // is mono
        file.writeInt(Integer.reverseBytes(sampleRate)); // sample rate, this is
                                                         // probably 22050 Hz
        file
                .writeInt(Integer.reverseBytes(sampleRate * channels
                        * bytesPerSec)); // bytes per second
        file.writeShort(Short.reverseBytes((short) (channels * bytesPerSec))); // bytes
                                                                               // per
                                                                               // sample
                                                                               // time
        file.writeShort(Short.reverseBytes((short) sampleBits)); // bits per
                                                                 // sample, this
                                                                 // is 16 bit
                                                                 // pcm
        file.writeBytes("data"); // data section label
        file.writeInt(Integer.reverseBytes(bytesWritten)); // length of raw pcm
                                                           // data in bytes
        file.close();
        file = null;
    }

    private static void writeUnsignedShortLE(BufferedOutputStream stream, short sample)
            throws IOException {
        // write already writes the lower order byte of this short
        stream.write(sample);
        stream.write((sample >> 8));
    }
}
