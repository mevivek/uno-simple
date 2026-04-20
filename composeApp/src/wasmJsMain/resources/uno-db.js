// UNO Simple — Firebase Realtime Database glue for Kotlin/Wasm.
//
// Kotlin/Wasm calls these functions via `external fun` declarations. This
// file wraps the Firebase JS SDK's realtime-database API in stable, small
// function signatures so changes in Firebase's module shape don't leak
// into the Kotlin side.
//
// Loaded from index.html after firebase-config.js / initializeApp. Talks to
// the same `window.firebaseApp` the rest of the page uses.

import {
    getDatabase,
    ref,
    set,
    onValue,
    off,
} from 'https://www.gstatic.com/firebasejs/10.13.2/firebase-database.js';

const db = getDatabase(window.firebaseApp);

/**
 * Map of subscription handles (returned from `unoDbSubscribe`) to their
 * off() cleanup closures. Used so `unoDbUnsubscribe(handle)` can tear down
 * the listener without the Kotlin side having to hold a reference to the
 * underlying Firebase ref or callback closure.
 */
const subscriptions = new Map();
let nextHandle = 1;

/** Write a string value at the given RTDB path. Resolves via Firebase's set(). */
export function unoDbSet(path, jsonString) {
    // jsonString is a kotlinx.serialization-encoded GameState (or PlayerSeat list).
    // Store as a string — Firebase will serialize unchanged.
    return set(ref(db, path), jsonString);
}

/**
 * Subscribe to value changes at `path`. Each emission invokes `callback`
 * with a single string argument: the latest stored value, or the empty
 * string when the node is absent.
 *
 * Returns an opaque integer handle. Pass it to `unoDbUnsubscribe` to detach.
 */
export function unoDbSubscribe(path, callback) {
    const r = ref(db, path);
    const cb = (snapshot) => {
        const v = snapshot.val();
        // Normalize null / missing values to empty string so Kotlin side
        // can pattern-match on `""` without NPE-ing through an `external`.
        callback(v == null ? "" : String(v));
    };
    onValue(r, cb);
    const handle = nextHandle++;
    subscriptions.set(handle, () => off(r, 'value', cb));
    return handle;
}

/** Detach a subscription created via `unoDbSubscribe`. */
export function unoDbUnsubscribe(handle) {
    const cleanup = subscriptions.get(handle);
    if (cleanup) {
        cleanup();
        subscriptions.delete(handle);
    }
}
