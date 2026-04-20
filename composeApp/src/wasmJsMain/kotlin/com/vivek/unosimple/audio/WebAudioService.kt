package com.vivek.unosimple.audio

/**
 * Wasm [AudioService] — synthesizes SFX with the browser Web Audio API.
 * No asset files, no network fetches. Each effect is a short oscillator
 * envelope tuned to read as "card slap / UNO! / win / etc." without
 * sounding like a ringtone.
 *
 * The AudioContext is lazily created on first play — browsers require a
 * user gesture to start audio, and the first `play()` call typically lands
 * right after a tap. We also call `resume()` defensively on every play.
 */
class WebAudioService : AudioService {
    private var muted: Boolean = false

    override fun play(sound: SoundEffect) {
        if (muted) return
        // Map the shared SoundEffect enum onto a string the JS side
        // dispatches on. Strings keep the interop layer trivial.
        val kind = when (sound) {
            SoundEffect.CARD_DEAL -> "deal"
            SoundEffect.CARD_PLAY -> "play"
            SoundEffect.SHUFFLE -> "shuffle"
            SoundEffect.UNO_CALL -> "uno"
            SoundEffect.WIN -> "win"
            SoundEffect.ERROR -> "error"
        }
        runCatching { playNative(kind) }
    }

    override fun setMuted(muted: Boolean) {
        this.muted = muted
        runCatching { setNativeMuted(muted) }
    }
}

/**
 * Entry point into the JS side. Lazily provisions an AudioContext and
 * routes [kind] onto a tailored oscillator envelope. Designed to be
 * fire-and-forget — no callbacks, no errors thrown back to Kotlin.
 *
 * The `globalThis.__uno` object caches the AudioContext + muted flag
 * across calls so we don't spawn a new context per SFX.
 */
@JsFun(
    """
    (kind) => {
        const AC = globalThis.AudioContext || globalThis.webkitAudioContext;
        if (!AC) return;
        if (!globalThis.__uno) globalThis.__uno = { ctx: new AC(), muted: false };
        const s = globalThis.__uno;
        if (s.muted) return;
        const ctx = s.ctx;
        if (ctx.state === 'suspended') { try { ctx.resume(); } catch (e) {} }
        const now = ctx.currentTime;

        // One-shot oscillator + gain envelope with optional frequency ramp.
        const tone = (freqStart, freqEnd, dur, type, peak) => {
            const osc = ctx.createOscillator();
            const gain = ctx.createGain();
            osc.type = type || 'sine';
            osc.frequency.setValueAtTime(freqStart, now);
            if (freqEnd !== freqStart) osc.frequency.exponentialRampToValueAtTime(freqEnd, now + dur);
            gain.gain.setValueAtTime(0.0001, now);
            gain.gain.exponentialRampToValueAtTime(peak || 0.3, now + 0.01);
            gain.gain.exponentialRampToValueAtTime(0.0001, now + dur);
            osc.connect(gain).connect(ctx.destination);
            osc.start(now);
            osc.stop(now + dur + 0.02);
        };

        // Short noise burst, cut to [dur] seconds, for percussive "slap" feel.
        const noise = (dur, filterFreq, peak) => {
            const len = Math.floor(ctx.sampleRate * dur);
            const buf = ctx.createBuffer(1, len, ctx.sampleRate);
            const d = buf.getChannelData(0);
            for (let i = 0; i < len; i++) d[i] = (Math.random() * 2 - 1) * (1 - i / len);
            const src = ctx.createBufferSource();
            src.buffer = buf;
            const bp = ctx.createBiquadFilter();
            bp.type = 'bandpass';
            bp.frequency.value = filterFreq || 600;
            bp.Q.value = 0.8;
            const g = ctx.createGain();
            g.gain.setValueAtTime(0.0001, now);
            g.gain.exponentialRampToValueAtTime(peak || 0.25, now + 0.005);
            g.gain.exponentialRampToValueAtTime(0.0001, now + dur);
            src.connect(bp).connect(g).connect(ctx.destination);
            src.start(now);
            src.stop(now + dur + 0.02);
        };

        switch (kind) {
            case 'play':
                noise(0.09, 900, 0.3);
                tone(160, 90, 0.13, 'sine', 0.18);
                break;
            case 'deal':
                noise(0.05, 2200, 0.18);
                break;
            case 'shuffle':
                for (let i = 0; i < 4; i++) {
                    setTimeout(() => noise(0.04, 1800 + Math.random() * 600, 0.15), i * 50);
                }
                break;
            case 'uno':
                // Ascending triad — C E G — 1/8-note each.
                tone(523, 523, 0.11, 'triangle', 0.22);
                setTimeout(() => tone(659, 659, 0.11, 'triangle', 0.22), 110);
                setTimeout(() => tone(784, 784, 0.22, 'triangle', 0.26), 220);
                break;
            case 'win':
                // Rising arpeggio fanfare — C E G C'.
                const notes = [523, 659, 784, 1046];
                notes.forEach((f, i) => setTimeout(() => tone(f, f, 0.18, 'triangle', 0.28), i * 120));
                break;
            case 'error':
                tone(220, 140, 0.18, 'sawtooth', 0.18);
                break;
        }
    }
    """
)
private external fun playNative(kind: String)

@JsFun(
    """
    (m) => { if (globalThis.__uno) globalThis.__uno.muted = m; }
    """
)
private external fun setNativeMuted(muted: Boolean)
