# GameInfo

An Android app for browsing game info/recommended specs, with a PC compatibility checker.
Game artwork is fetched live from official sources (Steam, the App Store, and optionally IGDB) —
nothing is bundled or invented, see `CATALOG_NOTES.md`.

## Build an APK with GitHub Actions (no Android Studio needed)

1. Create a new **empty** repository on GitHub (don't add a README/license there).
2. Upload this whole `GameInfo` folder to it — either:
   - on github.com: "Add file" → "Upload files", drag the folder's contents in, commit; or
   - from a computer: `git init && git add . && git commit -m "Initial commit" && git remote add origin <your-repo-url> && git push -u origin main`
3. On GitHub, open the **Actions** tab. A workflow called **Build APK** runs automatically on
   every push (or click "Run workflow" to trigger it by hand).
4. When the run finishes (green check), open it and scroll to **Artifacts** →
   download **GameInfo-debug-apk**. Unzip it to get `app-debug.apk`.
5. Copy that APK to your phone and open it to install (you'll need to allow "install from
   unknown sources" for whichever app you use to open it — Files, a browser, etc.).

This produces an unsigned **debug** build, which is fine for installing on your own device.

## More icon coverage (optional)

Steam and the App Store cover most PC and mobile titles. Console-exclusive games (no Steam or
App Store listing) will only get an icon if you add free IGDB/Twitch API credentials:

1. Register an app at https://dev.twitch.tv/console → "Register Your Application".
2. Put the Client ID and Client Secret into
   `app/src/main/java/com/arstudio/gameinfo/ApiKeys.kt`.
3. Commit and push — the next Actions build will include IGDB as an additional icon source.
