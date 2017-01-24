/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.io.udpaudiosource;

import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;

import javax.sound.sampled.AudioSystem;

import org.eclipse.smarthome.core.audio.AudioException;
import org.eclipse.smarthome.core.audio.AudioFormat;
import org.eclipse.smarthome.core.audio.AudioSource;
import org.eclipse.smarthome.core.audio.AudioStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link UDPAudioSource} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Chiel Prins - Initial contribution
 */
public class UDPAudioSource implements AudioSource {

    private Logger LOGGER = LoggerFactory.getLogger(UDPAudioSource.class);

    private static final int UDP_PORT = 8888;

    /**
     * Java Sound audio format
     */
    private static final javax.sound.sampled.AudioFormat JAVA_AUDIO_FORMAT = new javax.sound.sampled.AudioFormat(
            16000.0f, 16, 1, true, false);

    /**
     * AudioFormat of the JavaSoundAudioSource
     */
    private static final AudioFormat AUDIO_FORMAT = convertAudioFormat(JAVA_AUDIO_FORMAT);

    private DatagramInputStream input;

    @Override
    public String getId() {
        return "udpaudiosource";
    }

    @Override
    public String getLabel(Locale locale) {
        return "UDP Network Audio Source";
    }

    @Override
    public Set<AudioFormat> getSupportedFormats() {
        return Collections.singleton(AUDIO_FORMAT);
    }

    @Override
    public AudioStream getInputStream(AudioFormat format) throws AudioException {
        if (!AUDIO_FORMAT.isCompatible(format)) {
            throw new AudioException("Cannot produce streams in format " + format);
        }
        return new AudioInputStream(new javax.sound.sampled.AudioInputStream(getDatagramInputStream(),
                JAVA_AUDIO_FORMAT, AudioSystem.NOT_SPECIFIED), AUDIO_FORMAT);
    }

    private synchronized DatagramInputStream getDatagramInputStream() {
        if (input == null) {
            try {
                input = new DatagramInputStream(new DatagramSocket(UDP_PORT));
            } catch (SocketException e) {
                throw new RuntimeException("Could not create socket!", e);
            }
        }
        return input;
    }

    @Override
    public String toString() {
        return getId();
    }

    /**
     * Converts a javax.sound.sampled.AudioFormat to a org.eclipse.smarthome.core.audio.AudioFormat
     *
     * @param audioFormat the AudioFormat to convert
     * @return The converted AudioFormat
     */
    private static AudioFormat convertAudioFormat(javax.sound.sampled.AudioFormat audioFormat) {
        int frameSize = audioFormat.getFrameSize(); // In bytes
        int bitsPerFrame = frameSize * 8;
        Integer bitDepth = AudioSystem.NOT_SPECIFIED == frameSize ? null : bitsPerFrame;

        float frameRate = audioFormat.getFrameRate();
        Integer bitRate = AudioSystem.NOT_SPECIFIED == frameRate ? null : (int) (frameRate * bitsPerFrame);

        float sampleRate = audioFormat.getSampleRate();
        Long frequency = AudioSystem.NOT_SPECIFIED == sampleRate ? null : (long) sampleRate;

        return new AudioFormat(AudioFormat.CONTAINER_WAVE, audioFormat.getEncoding().toString(),
                audioFormat.isBigEndian(), bitDepth, bitRate, frequency);
    }
}
