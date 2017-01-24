/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.voice.pocketsphinx.internal;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.eclipse.smarthome.core.audio.AudioFormat;
import org.eclipse.smarthome.core.audio.AudioStream;
import org.eclipse.smarthome.core.voice.STTException;
import org.eclipse.smarthome.core.voice.STTListener;
import org.eclipse.smarthome.core.voice.STTService;
import org.eclipse.smarthome.core.voice.STTServiceHandle;
import org.eclipse.smarthome.core.voice.SpeechRecognitionErrorEvent;
import org.eclipse.smarthome.core.voice.SpeechRecognitionEvent;
import org.eclipse.smarthome.core.voice.SpeechStartEvent;
import org.eclipse.smarthome.core.voice.SpeechStopEvent;

import com.snirpoapps.speech.SpeechRecognizer;
import com.snirpoapps.speech.SpeechRecognizerSetup;

import edu.cmu.pocketsphinx.LogMath;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * This is a STT service implementation using Kaldi.
 *
 * @author Kelly Davis - Initial contribution and API
 *
 */
public class PocketsphinxSTTService implements STTService {

    /**
     * Set of supported locales
     */
    private static final Set<Locale> LOCALES;

    /**
     * Set of supported audio formats
     */
    private static final Set<AudioFormat> AUDIO_FORMATS;

    private static final File ASSETS_DIR = new File("D:\\speech");

    private final LogMath logMath;

    static {
        AUDIO_FORMATS = Collections.singleton(
                new AudioFormat(AudioFormat.CONTAINER_NONE, AudioFormat.CODEC_PCM_UNSIGNED, false, 16, null, 16000L));

        HashSet<Locale> locales = new HashSet<Locale>();
        // locales.add(new Locale("en", "US"));
        locales.add(new Locale("nl", "NL"));
        LOCALES = Collections.unmodifiableSet(locales);
    }

    private SpeechRecognizer speechRecognizer;

    public PocketsphinxSTTService() {
        try {
            speechRecognizer = SpeechRecognizerSetup.defaultSetup().setAcousticModel(new File(ASSETS_DIR, "nl"))
                    .setDictionary(new File(ASSETS_DIR, "nl.dict"))
                    // .setRawLogDir(assetsDir)
                    .setSampleRate(16000).setKeywordThreshold(1e-20f).getRecognizer();
            logMath = new LogMath();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<Locale> getSupportedLocales() {
        return LOCALES;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<AudioFormat> getSupportedFormats() {
        return AUDIO_FORMATS;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public STTServiceHandle recognize(STTListener sttListener, AudioStream audioStream, Locale locale,
            Set<String> grammars) throws STTException {
        if (!LOCALES.contains(locale)) {
            throw new IllegalArgumentException("The passed Locale is unsupported");
        }

        AudioFormat audioFormat = AUDIO_FORMATS.stream().filter(a -> a.isCompatible(audioStream.getFormat()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("The passed AudioSource's AudioFormat is unsupported"));

        speechRecognizer.addNgramSearch("stt", new File(ASSETS_DIR, "nl.lm.bin"));
        Disposable disposable = speechRecognizer.decodeStream("stt", audioStream).subscribeOn(Schedulers.io())
                .subscribe(
                        hyp -> sttListener.sttEventReceived(
                                new SpeechRecognitionEvent(hyp.getHypstr(), (float) logMath.exp(hyp.getProb()))),
                        e -> sttListener.sttEventReceived(new SpeechRecognitionErrorEvent(e.getMessage())),
                        () -> sttListener.sttEventReceived(new SpeechStopEvent()),
                        d -> sttListener.sttEventReceived(new SpeechStartEvent()));

        return new STTServiceHandle() {
            @Override
            public void abort() {
                disposable.dispose();
            }
        };
    }

    @Override
    public String getId() {
        return "pocketsphinxstt";
    }

    @Override
    public String getLabel(Locale locale) {
        return "Pocketsphinx STT";
    }
}
