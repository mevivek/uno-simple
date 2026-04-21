package com.vivek.unosimple.platform

/** Android / iOS / Desktop have no URL-routing surface. */
actual fun currentUrlPath(): String = ""
