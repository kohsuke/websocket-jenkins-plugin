Attempted to add WebSocket support as a Jenkins plugin via [Tyrus](https://tyrus.java.net/).

Tyrus has [SPI](https://blogs.oracle.com/PavelBucek/entry/tyrus_container_spi) and an adapter that enables
it to run on top of Servlet 3.1, so this approach looked feasible.

However, in the end I hit an obstacle; the lack of `HttpServletRequest.upgrade()` API in Jetty
([ticket](https://bugs.eclipse.org/bugs/show_bug.cgi?id=478752)) where Jetty developers refused to support this part of the API.
Without this, it won't work on Winstone & test harness.