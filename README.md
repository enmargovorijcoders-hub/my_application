# The P&S App

The P&S site in a small Android app (Android 4.0+, ARM and ARM64), with a zoom slider along the bottom. And its just a webwrapper using Android System Webview to show my site (https://enmargovorijcoders-hub.github.io/my-webpage/).

Build: upload everything in this folder to a GitHub repository (branch `main`), open the Actions tab,
and download the `ThePandSApp-apk` artifact when the run is green.

To point it at a different site, change `HOME` in `app/src/main/java/com/enmar/pandsapp/MainActivity.java`.

## About build.yml
The build recipe lives at `.github/workflows/build.yml`. That folder starts with a dot, so Mac and some
upload pages hide it. If `.github` is missing in your GitHub repository after uploading, create the file
by hand: Add file > Create new file, name it `.github/workflows/build.yml`, and paste in the contents of the
`build.yml` copy in this folder. (The copy in this folder does nothing by itself; GitHub only reads the one
inside `.github/workflows/`.)
