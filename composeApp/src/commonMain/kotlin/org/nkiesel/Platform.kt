package org.nkiesel

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform