# kpresence

KPresence is a lightweight Kotlin/JVM library for interacting with Discord Rich Presence.
This is a rewrite of [the original by vyfor](https://github.com/vyfor/kpresence) focusing on features
for the [IntelliJ Discord Rich Presence](https://github.com/pandier/intellij-discord-rp) plugin.
Because of that, the scope has been lowered from Kotlin Multiplatform to Kotlin JVM.

Requires Java 17 or later.

## Adding dependency

```gradle
dependencies {
    implementation("io.github.pandier:kpresence:0.7.0")
}
```

## Example

```kt
fun main(): Unit = runBlocking {
    val client = KPresenceClient(applicationId) {
        parentScope = this@runBlocking
        logger = KPresenceLogger.Println()
    }

    // Connects to Discord in the background
    client.connect()

    client.update {
        details = "This is the 1st line"
        state = "This is the 2nd line"

        timestamps {
            start = now() - 60_000
        }

        assets {
            largeImage = "cool_logo"
            largeText = "This is a cool logo"

            smallImage = "https://example.com/cooler_logo.png"
        }

        button("Checkout KPresence", "https://github.com/pandier/kpresence")
    }
    
    delay(10_000)
    
    // Disconnects gracefuly and closes the client (cancels its job)
    client.disconnect().await()
    client.close()
}
```
