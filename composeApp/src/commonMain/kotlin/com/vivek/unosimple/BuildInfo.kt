package com.vivek.unosimple

/**
 * Tiny build stamp rendered in the app footer so you can tell at a glance
 * whether a newly-deployed build has actually loaded (vs. the browser
 * serving a stale cached bundle).
 *
 * Bump [BUILD_STAMP] every time a new version is shipped to Firebase
 * Hosting. Format: `YYYY-MM-DD.N` where N increments for multiple
 * same-day deploys.
 */
object BuildInfo {
    const val BUILD_STAMP: String = "2026-04-20.22"
}
