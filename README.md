# ChatCtrl

**A Matrix chat app for matrix.org and your own server.**

[![Latest release](https://img.shields.io/github/v/release/VoidedKN0X/ChatCtrl?label=download)](https://github.com/VoidedKN0X/ChatCtrl/releases/latest) ![Android 15+](https://img.shields.io/badge/Android-15%2B-3DDC84?logo=android&logoColor=white) [![License: GPL-3.0](https://img.shields.io/badge/license-GPL--3.0-blue)](LICENSE) ![Telemetry-free](https://img.shields.io/badge/telemetry-none-brightgreen) [![Sponsor](https://img.shields.io/badge/sponsor-GitHub-ea4aaa?logo=githubsponsors&logoColor=white)](https://github.com/sponsors/VoidedKN0X)

ChatCtrl is a Matrix chat client in the spirit of Element X. It works with matrix.org, other public servers and your own self-hosted Synapse, and is built on the same Matrix Rust SDK as Element X, so encryption and verification work the same way.

Part of the [Ctrl series](#the-ctrl-series) by VKNXWorks.

<a href="https://apps.obtainium.imranr.dev/redirect?r=obtainium://add/https://github.com/VoidedKN0X/ChatCtrl"><img src=".github/assets/badge_obtainium.png" alt="Get it on Obtainium" height="80"></a> <a href="https://voidedkn0x.github.io/CtrlF-Droid/"><img src=".github/assets/badge_ctrlf-droid.png" alt="Get it from our F-Droid repo" height="80"></a>

## Features

- **Any server**: matrix.org, other public servers, or your own address
- **Sign in** with OAuth (including creating an account), username and password, single sign-on, or a **QR code** from a device that's already signed in
- **Link new device**: sign a new phone or computer in with a QR code, already verified (servers with Matrix Authentication Service and QR sign-in)
- **End-to-end encryption** by default, with device verification and key backup
- **Messages**: replies, threads, reactions, edits, polls, voice messages, images, videos, files and locations; stickers and GIFs other people send show in the chat
- **Quick gestures**: swipe a message to reply, double-tap to react, and see who has read up to where
- **Chat bubbles** in six styles, from Modern to no bubbles at all (like Element Web)
- **Media and files** for each chat: a photo and video grid and a list of files and voice messages
- **Search inside chats**, including encrypted ones
- **Push notifications through [ntfy](https://f-droid.org/packages/io.heckel.ntfy/)** only, grouped per chat with reply and Mark as read, with a background check every ~15 minutes as a fallback
- **Voice and video calls** in direct chats and rooms, with people on Element X and Element Web: the same Element Call as Element X, bundled in the app, with incoming calls ringing on a full screen, picture-in-picture and speaker, earpiece and Bluetooth audio ([what your server needs](#calls))
- **Link previews and drafts** for every chat
- **Send a location** (where you are or a dropped pin) and see received locations on an **OpenStreetMap** map, using Android's own location service
- **Create rooms**, start direct chats, explore public rooms, and manage members and moderation
- **Share to ChatCtrl** from other apps
- **Device list**: rename your signed-in sessions or sign them out
- **App lock** with its own PIN and optional fingerprint unlock; your saved sign-in stays encrypted while the app is locked (with notifications off)
- **Hide in screenshots** and **Hide in recent apps**, two separate switches, both off by default
- **The Ctrl series layout**: **Chats** and **Explore** tabs on a floating glass tab bar (a side rail on tablets), and your profile picture at the top right opens the **You** page with your account, **Settings** and **About**
- Two-pane layout on tablets and foldables

## Requirements

- Android 15 or newer on a 64-bit ARM phone
- A Matrix server with **Simplified Sliding Sync** (Synapse 1.114 or newer, Conduit, Conduwuit or Tuwunel)
- **Google-free and de-Googled:** no Google Play services, Firebase or other Google services, and the app never contacts Google. It works the same on de-Googled phones (GrapheneOS, CalyxOS, /e/OS, LineageOS without Google apps).

## Install

1. Open the [latest release](https://github.com/VoidedKN0X/ChatCtrl/releases/latest) on your phone, scroll down to **Assets** and tap the file that ends in `.apk` (for example `ChatCtrl-v2.0.1.apk`). The "Source code" zip and tar.gz files are not the app.
2. Open it on your phone. Android may ask you to allow installing apps from your browser or file manager.
3. Updates install over the old version. Your settings and login are kept.

**Automatic updates:** on your phone, tap one of the buttons below.

- **Obtainium:** install [Obtainium](https://github.com/ImranR98/Obtainium) first. The button adds ChatCtrl and installs every new release for you.
- **F-Droid:** adds the [CtrlF-Droid repo](https://voidedkn0x.github.io/CtrlF-Droid/) to F-Droid, Droid-ify or Neo Store, with every Ctrl app and automatic updates.

<a href="https://apps.obtainium.imranr.dev/redirect?r=obtainium://add/https://github.com/VoidedKN0X/ChatCtrl"><img src=".github/assets/badge_obtainium.png" alt="Get it on Obtainium" height="80"></a> <a href="https://voidedkn0x.github.io/CtrlF-Droid/"><img src=".github/assets/badge_ctrlf-droid.png" alt="Get it from our F-Droid repo" height="80"></a>

If the Obtainium button doesn't open the app, add `https://github.com/VoidedKN0X/ChatCtrl` in Obtainium by hand.

## Getting started

1. Pick a server from the list, or enter your own (`example.com` or `https://matrix.example.com`).
2. Sign in with whatever your server offers.
3. Confirm it's you: tap **Use another device** (approve in Element or ChatCtrl and compare emojis) or **Use your recovery key**, and your chats appear.

Your account, devices, security and recovery, **Settings**, **Sign out** and **About** are on the **You** page: tap your profile picture at the top right of **Chats** or **Explore**. Every path below that starts with **Settings** is there.

## Is it safe to install?

- **Open source:** all of ChatCtrl's code is in this repo, under the GPL-3.0 licence.
- **Built by GitHub, not on a personal computer:** every release on the [Releases page](https://github.com/VoidedKN0X/ChatCtrl/releases) is built from this code by [the build workflow](.github/workflows/build.yml) on GitHub Actions, including the bundled Element Call, which is built from source in the same run.
- **Always signed with the same key:** Android refuses to install an update signed with a different key, and the [CtrlF-Droid repo](https://voidedkn0x.github.io/CtrlF-Droid/) only accepts APKs signed with this one. The SHA-256 fingerprint of the signing certificate is:

  ```
  9E:E0:FC:5F:52:3F:E1:B8:D6:84:96:40:53:E0:AA:56:C6:5B:4F:72:6E:56:08:DE:9D:BE:8B:61:AC:92:9A:BA
  ```

  Advanced users can compare it with [AppVerifier](https://github.com/soupslurpr/AppVerifier).
- **Check a download:** each release's notes end with the APK's SHA-256, so you can check the file you downloaded (`sha256sum ChatCtrl-vX.Y.Z.apk`). With the [GitHub CLI](https://cli.github.com/), `gh attestation verify ChatCtrl-vX.Y.Z.apk -R VoidedKN0X/ChatCtrl` proves the APK was built by this repo's build workflow from the tagged commit. The signing-certificate fingerprint above stays the check Android and AppVerifier use.

### Permissions

| Permission | What ChatCtrl uses it for |
| --- | --- |
| Internet (`INTERNET`) | Talking to your homeserver, your ntfy server, your server's call service and the OpenStreetMap tile server |
| Network state (`ACCESS_NETWORK_STATE`) | Noticing when the connection comes back, so syncing can restart |
| Notifications (`POST_NOTIFICATIONS`) | Message, invite and call notifications |
| Microphone (`RECORD_AUDIO`) | Voice messages and calls; asked for the first time you use them |
| Camera (`CAMERA`) | Video calls and scanning QR codes to sign in or link a device; asked when needed |
| Biometrics (`USE_BIOMETRIC`) | Optional fingerprint unlock for the app lock |
| Hide floating windows (`HIDE_OVERLAY_WINDOWS`) | Hide other apps' floating windows over the PIN screen, and over the lock screen, recovery-key and sign-in screens and confirmations, only while they show. Granted at install, without a prompt |
| Keep awake (`WAKE_LOCK`) | Keeps the phone awake for the moment it takes to fetch a pushed message, and while a call rings |
| Battery optimisation (`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`) | Lets you exempt ChatCtrl from battery optimisation so pushed messages can be fetched while the phone sleeps |
| Audio settings (`MODIFY_AUDIO_SETTINGS`) | Switching calls between speaker, earpiece, wired and Bluetooth audio |
| Nearby devices (`BLUETOOTH_CONNECT`) | Using Bluetooth earphones for calls; never used to scan for devices |
| Foreground service (`FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_PHONE_CALL`, `FOREGROUND_SERVICE_MICROPHONE`, `FOREGROUND_SERVICE_CAMERA`) | The **Ongoing call** notification that keeps a call running while you use other apps or the screen is off |
| Own calls (`MANAGE_OWN_CALLS`) | Lets the ongoing call run as a "phone call" service; no access to your phone app or calls |
| Full-screen notifications (`USE_FULL_SCREEN_INTENT`) | The full-screen ringing screen for incoming calls |
| Location (`ACCESS_COARSE_LOCATION`, `ACCESS_FINE_LOCATION`) | **Send my current location** in a chat, read once when you ask; never in the background |
| Run at startup (`RECEIVE_BOOT_COMPLETED`) | Putting the 15-minute background check for new messages back on schedule after the phone restarts. Added by Android's background work library (WorkManager) |

`DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` also shows up in the app's details: it's an internal permission Android adds to every app built for Android 13 and newer, and only keeps ChatCtrl's own broadcasts private. ChatCtrl has no contacts, phone, SMS or storage permission: files and photos you send are picked with Android's own picker.

See [Privacy](#privacy) for everything ChatCtrl connects to.

## Tips and troubleshooting

- **Notifications:** instant push comes through [ntfy](https://f-droid.org/packages/io.heckel.ntfy/) only (no other push apps). Install ntfy, then turn it on in *Settings › Notifications › Delivery › Instant push with ntfy*. ChatCtrl also checks for new messages about every 15 minutes, with or without ntfy, so a push that doesn't arrive still turns into a notification.
- **Reliable notifications:** let both ChatCtrl and ntfy run unrestricted in the background. *Settings › Notifications* warns when either is battery-restricted and has buttons to fix it. A battery-optimised ChatCtrl gets no network while the phone sleeps, so pushed messages can't be fetched; a battery-optimised ntfy loses its connection to your ntfy server. On OPPO, OnePlus and realme phones, also turn on *Allow background activity* in *App info → Battery* for both apps.
- **Self-hosted ntfy:** your ntfy server's Matrix push gateway (`https://your-ntfy/_matrix/push/v1/notify`, built into ntfy) is used for every push, so your Matrix server must be able to reach it, and the ntfy server needs `base-url` set in its config. ntfy.sh works too. If the gateway can't be found, *Settings › Notifications* shows an error; no other gateway is ever used.
- **What a push contains:** only the room and message IDs and an unread count, never the message, sender or room name. ChatCtrl then fetches and decrypts the message itself from your homeserver. Your homeserver sends the push to the Matrix gateway of your own ntfy server, which passes it to ntfy on your phone.
- **Just your domain doesn't work?** Make sure `https://yourdomain/.well-known/matrix/client` points to your server, or enter the full server address.
- **Not included yet:** spaces, live location, QR code verification (not in the Matrix Rust SDK yet) and multiple accounts at once.

## Calls

Voice and video calls work in direct chats and rooms, and with everyone on your homeserver who uses Element X or Element Web (MatrixRTC, the calls Element X makes). ChatCtrl uses Element Call, the same call screen Element X has (the same release), built from source and bundled in the app: it runs in your phone's own WebView and nothing of it is loaded from the web. ChatCtrl's build leaves out Element Call's Google reCAPTCHA and sign-up pages, PostHog analytics, Sentry crash reporting, bug-report upload and MediaPipe background blur, none of which calls use (see [Build from source](#build-from-source)).

- **Start a call** with the phone (voice, direct chats) or camera (video) button at the top of a chat. While a call is going on in a chat, a **Call in progress** banner there has **Join**, and the chat list shows a camera next to the chat.
- **Incoming calls** in direct chats ring on a full screen, also over the phone's lock screen, with **Answer** and **Decline**, until the caller hangs up, you answer or decline on another device, or after 90 seconds (then you get a **Missed call** notification). Ringing comes through push, so [instant push with ntfy](#tips-and-troubleshooting) has to be on. Also check **Settings › Calls › Full-screen incoming calls** is on. A call started in a group shows as a normal notification.
- **During a call**: mute, camera on or off, switch camera, and the audio output (speaker, earpiece, wired headphones or Bluetooth) are on the call screen. Going back or home shrinks the call to a picture-in-picture window, and the **Ongoing call** notification has **Hang up**. The camera and microphone are only asked for the first time you start or answer a call.
- **With the app lock on**, answering a call or opening the call screen shows the lock screen first, a call that's already running keeps going while ChatCtrl locks, and the call never shows in Recents. With message content hidden in notifications (the app lock's default), the ringing screen and notifications say "Incoming call" without the caller's name.

### What your server needs

Calls need a MatrixRTC backend next to your homeserver, the same one Element X needs. If your server doesn't announce one, the call button says **Calls aren't set up on your server**, and **Settings › Calls** shows what your server offers.

1. **A LiveKit SFU** ([livekit-server](https://github.com/livekit/livekit)), reachable from the internet: its WebSocket port (7880, behind your reverse proxy), TCP 7881 and the UDP port range it's configured for (e.g. 50100-50200). Its built-in TURN server helps clients on restrictive networks.
2. **The MatrixRTC Authorization Service** ([lk-jwt-service](https://github.com/element-hq/lk-jwt-service)), with `LIVEKIT_URL` (the SFU's `wss://` address), `LIVEKIT_KEY` and `LIVEKIT_SECRET` (the same key as the SFU), and `LIVEKIT_FULL_ACCESS_HOMESERVERS` set to your server name. Synapse needs a `federation` or `openid` listener for it. A common setup is one hostname with `/livekit/jwt` and `/livekit/sfu` behind the reverse proxy (Nginx Proxy Manager works: one proxy host with two custom locations and WebSockets on).
3. **Synapse settings** in `homeserver.yaml`, from [Element Call's self-hosting guide](https://github.com/element-hq/element-call/blob/livekit/docs/self_hosting.md):
   ```yaml
   experimental_features:
     msc3266_enabled: true
     msc4143_enabled: true
     msc4222_enabled: true
   max_event_delay_duration: 24h
   rc_message:
     per_second: 0.5
     burst_count: 30
   rc_delayed_event_mgmt:
     per_second: 1
     burst_count: 20
   matrix_rtc:
     transports:
       - type: livekit
         livekit_service_url: https://matrix-rtc.example.com/livekit/jwt
   ```
4. **Or announce it in `.well-known`**, for homeservers without the transports endpoint: add this to `https://example.com/.well-known/matrix/client`:
   ```json
   "org.matrix.msc4143.rtc_foci": [
     { "type": "livekit", "livekit_service_url": "https://matrix-rtc.example.com/livekit/jwt" }
   ]
   ```

ChatCtrl asks your homeserver for its MatrixRTC service (the transports endpoint first, then `.well-known`) and only ever uses that one: there's no fallback to a public call service.

## App lock

Turn it on under **Settings › Privacy and security › App lock** (tap your profile picture, then **Settings**). It's off until you turn it on.

- **Its own app PIN** of at least 6 digits, separate from your phone's screen lock. Very obvious PINs such as 111111 or 123456 are refused.
- **Unlock with fingerprint** (optional) only accepts strong biometrics, such as the fingerprint sensor, checked by the phone's secure hardware. Face unlock that only uses the camera isn't accepted. The PIN is still needed after the phone restarts and at least every 72 hours, and adding a new fingerprint to the phone turns fingerprint unlock off until you turn it on again.
- **Your saved sign-in is encrypted with the PIN while ChatCtrl is locked**, with the key to its encrypted message store, and the running client is shut down. The unlocking key only exists in memory while the app is unlocked and is wiped the moment it locks. The PIN itself is never stored: it's stretched with 600,000 rounds of PBKDF2 and mixed with a key held in the phone's secure hardware, so a copy of the app's files isn't enough to guess it on a computer.
- **Wrong PINs:** after 5 tries you wait 30 seconds, doubling up to 15 minutes. Restarting the app or the phone, or changing the clock, doesn't skip the wait.
- **Lock again:** as soon as you leave the app (the default), after 1, 5 or 15 minutes or 1 hour away, or only when the app restarts. Picking photos or files, recording a voice message for the first time, sharing, opening a file or link, and the notification and battery settings don't count as leaving, unless you stay away for more than 5 minutes. Shrinking a call to picture-in-picture counts as leaving: the call carries on, and the small window only shows a lock until you unlock.
- **Change app PIN** asks for your current PIN first, and turning the app lock off asks for the PIN too.
- **Forgot your PIN?** It can't be recovered, because it encrypts your saved sign-in. The lock screen offers to sign out on this phone and delete its copy of your chats and keys. With recovery set up, your message history comes back after you sign in again.
- **With the app lock on**, the recent-apps screen always shows a blank card, the connection to your homeserver is closed while ChatCtrl is locked (with notifications off), and message text is hidden in notifications until you choose to show it (**Settings › Notifications**). Message notifications have no inline **Reply** while the app lock is on, so nobody can answer as you without the PIN. With notifications on, the background check and push still need your account while ChatCtrl is locked, so your saved sign-in then stays encrypted with the Android Keystore only, as without the app lock, and the connection stays available to them; Settings says so.

Two more switches in the same section work with or without the app lock, and are both off until you turn them on:

- **Hide in screenshots**: screenshots and screen recordings show a blank screen (and so does the recent-apps view).
- **Hide in recent apps**: the recent-apps view shows a blank card instead of ChatCtrl. Screenshots still work.

Password sign-in, the recovery key screens and the lock and PIN screens are always kept out of screenshots and recordings, whatever these switches say.

## Privacy

**Telemetry-free and Google-free.** ChatCtrl has no analytics, crash reporting, usage stats, ads or tracking of any kind, and no Google libraries or services. It only talks to your homeserver (and its sign-in page, if your server signs in through the browser), if you turn on push, your ntfy server, during calls, your server's call service, and while a location map is open, OpenStreetMap's map tile server (tile.openstreetmap.org, which only sees which map squares are loaded).

- **Push:** when you turn it on, ChatCtrl asks your ntfy server whether its Matrix gateway is there. Pushes then reach the phone through ntfy (see *What a push contains* above).
- **Link previews** are fetched by your homeserver, never by the phone.
- **Calls** connect only to your homeserver (and its domain), the MatrixRTC Authorization Service your homeserver announces, the LiveKit SFU it points to, and the TURN/STUN servers that SFU hands out. The bundled Element Call is built from source without its analytics (PostHog), crash reporting (Sentry), bug-report upload, Google reCAPTCHA and Google MediaPipe code, its fallback call servers are switched off, and the call WebView refuses every other web address. The WebView doesn't ask Google Safe Browsing about pages and sends no usage metrics.
- **The Matrix SDK's crash reporting is switched off**, and release builds don't write the SDK's logs anywhere.
- **Maps** only ever use OpenStreetMap's standard tiles. The map library has other map providers built in (such as Mapbox, Thunderforest and Bing), but ChatCtrl never selects them, so they're never contacted. The map only loads when you open **Send location** or tap a location in a chat.
- **Addresses that are never contacted:** the bundled Element Call contains links to element.io and other sites, but the call WebView refuses every address except the ones listed above. `matrix.gateway.unifiedpush.org` is only used to recognise and remove a push registration made by an older ChatCtrl version; the phone never connects to it.
- **Copying** a message, a location or a recovery key marks it as sensitive, so Android hides it from the clipboard preview and keyboards don't keep it in their clipboard history.
- **Links:** **Install ntfy** (f-droid.org), the GitHub and licence links in About, and links in messages open in your browser only when you tap them.

## How ChatCtrl is made

ChatCtrl is a one-person project by VoidedKN0X (VKNXWorks). Much of the code is written with the help of AI (Claude, by Anthropic). I decide what the app does, review the changes and use the app myself. Every change is listed in the [changelog](CHANGELOG.md), and you can read every line of code here.

## Build from source

You need [Android Studio](https://developer.android.com/studio), or JDK 17 and the Android SDK.

1. Clone the repo:
   ```bash
   git clone https://github.com/VoidedKN0X/ChatCtrl.git
   cd ChatCtrl
   ```
2. Build the call screen (Element Call) from source. You need git, [Node.js](https://nodejs.org/) 24 and Corepack 0.34.5 or newer (`npm install -g corepack@latest`, then `corepack enable`), and a Linux or macOS shell (on Windows, WSL):
   ```bash
   bash element-call/build.sh
   ```
   This fetches Element Call at the release pinned in `element-call/upstream.env`, applies ChatCtrl's changes (see [Element Call](#element-call) below), installs its locked dependencies and builds it into `element-call/dist/`. Run it again after updating ChatCtrl if `element-call/` changed. The app build stops with a message if this step is missing.
3. Build the app:
   ```bash
   ./gradlew assembleDebug
   ```
   On Windows, use `gradlew.bat assembleDebug`.
4. The APK is in `app/build/outputs/apk/debug/`. Copy it to your phone and install it.

Or open the folder in Android Studio and press **Run** with your phone connected.

The APK only includes 64-bit ARM code. To run it on an emulator, add `x86_64` to `abiFilters` in `app/build.gradle.kts`.

**Dependency verification:** every library and Gradle plugin the build downloads is checked against its SHA-256 in `gradle/verification-metadata.xml`, and the build stops if one doesn't match. When you add or update a dependency or plugin, run the **Dependency verification metadata** workflow from the Actions tab, check the diff of the file it prints, and commit the new `gradle/verification-metadata.xml` in the same push.

### Element Call

The call screen is [Element Call](https://github.com/element-hq/element-call) **0.28.0** (commit `9b3c3569ecefbb79cf527ea24e3756ba50553a8f`), the release Element X bundles. Instead of Element's prebuilt package, `element-call/build.sh` builds it from source, and CI does the same for every release (a failed Element Call build means no release). ChatCtrl's changes are all in `element-call/`:

- `patches/0001-build-no-sentry-or-codecov-plugins.patch`: the build no longer adds Sentry's release and debug IDs or uploads anything to Sentry or Codecov.
- `overrides/vite-chatctrl.config.ts`: the embedded build Element X ships, with Sentry's SDK, PostHog's SDK, LiveKit's track processors and Google's MediaPipe replaced by small local stand-ins in `overrides/src/chatctrl/`, so none of them are in the app. No source maps.
- `overrides/src/`: reCAPTCHA, the sign-up page (with Google's privacy and terms links) and account registration, PostHog analytics, bug-report upload and background blur are replaced by versions that do nothing. These are only used by Element Call's own website or on desktop, never by a call inside an app.
- `strip-locales.mjs`: removes the reCAPTCHA texts from the translations.
- `smoke-test.mjs`: after every build, starts Element Call in headless Chrome the way ChatCtrl does and checks that it loads as a widget and tries to reach nothing but the homeserver. The build also fails if any of the removed code shows up again.

Calls work exactly as with Element's package: the same widget URL, the same call service discovery and the same calls as Element X and Element Web. The APK's `assets/element-call/` folder includes Element Call's licence and a `SOURCE.txt` naming the commit and changes it was built from.

## Support the project

ChatCtrl is free and open source. If it's useful to you, you can support development on [GitHub Sponsors](https://github.com/sponsors/VoidedKN0X). In the app, the **About** section at the bottom of the **You** page (tap your profile picture) has **Support ChatCtrl** and **Report a bug**, which opens the [issue tracker](https://github.com/VoidedKN0X/ChatCtrl/issues). Starring the repo helps too.

Found a bug or have an idea? [Open an issue](https://github.com/VoidedKN0X/ChatCtrl/issues/new/choose) and pick **Bug report** or **Feature request**.

Found a security problem? Please don't open a public issue; see [SECURITY.md](SECURITY.md).

## The Ctrl series

Native Android apps for self-hosted services and everyday tools, all with the same "Ctrl / Signal" design, in dark and light.

| App | What it's for |
| --- | --- |
| [AdCtrl](https://github.com/VoidedKN0X/AdCtrl) | Control your AdGuard Home DNS server |
| [ArrCtrl](https://github.com/VoidedKN0X/ArrCtrl) | Radarr, Sonarr, Prowlarr, Bazarr, qBittorrent, autobrr and Cleanuparr in one app |
| [BeamCtrl](https://github.com/VoidedKN0X/BeamCtrl) | TV remote for phones with an IR blaster |
| [CallCtrl](https://github.com/VoidedKN0X/CallCtrl) | Phone app: dialer, call history and in-call screen |
| [CameraCtrl](https://github.com/VoidedKN0X/CameraCtrl) | Private camera with RAW (DNG) on every lens |
| **ChatCtrl** (this app) | Matrix chat client for matrix.org and self-hosted Synapse |
| [ClockCtrl](https://github.com/VoidedKN0X/ClockCtrl) | Alarms, timers, stopwatch and world clock |
| [ContactCtrl](https://github.com/VoidedKN0X/ContactCtrl) | Contacts: browse, edit, import and export |
| [CraftyCtrl](https://github.com/VoidedKN0X/CraftyCtrl) | Manage Minecraft servers on Crafty Controller |
| [DateCtrl](https://github.com/VoidedKN0X/DateCtrl) | Calendar for the calendars on your phone and your accounts |
| [FilesCtrl](https://github.com/VoidedKN0X/FilesCtrl) | File manager with NFS, SMB, SFTP, FTP and WebDAV |
| [FrigateCtrl](https://github.com/VoidedKN0X/FrigateCtrl) | Watch and review your Frigate NVR cameras |
| [GalleryCtrl](https://github.com/VoidedKN0X/GalleryCtrl) | Private, offline gallery with an encrypted vault |
| [ImmichCtrl](https://github.com/VoidedKN0X/ImmichCtrl) | Photos and backup for your Immich server |
| [JellyCtrl](https://github.com/VoidedKN0X/JellyCtrl) | Movies, shows, music and Live TV from Jellyfin |
| [LijnCtrl](https://github.com/VoidedKN0X/LijnCtrl) | Live De Lijn bus and tram departures |
| [LumenCtrl](https://github.com/VoidedKN0X/LumenCtrl) | Private, offline photo and video editor |
| [MailCtrl](https://github.com/VoidedKN0X/MailCtrl) | Email for Gmail and Proton Mail |
| [MapsCtrl](https://github.com/VoidedKN0X/MapsCtrl) | Private, offline maps and navigation |
| [MemoCtrl](https://github.com/VoidedKN0X/MemoCtrl) | Notes on your Memos server |
| [NaviCtrl](https://github.com/VoidedKN0X/NaviCtrl) | Music player for Navidrome servers |
| [PaperCtrl](https://github.com/VoidedKN0X/PaperCtrl) | Scan and browse documents on Paperless-ngx |
| [PrintCtrl](https://github.com/VoidedKN0X/PrintCtrl) | Bambu Lab printers through Bambuddy, and Spoolman spools |
| [ProxCtrl](https://github.com/VoidedKN0X/ProxCtrl) | Manage your Proxmox VE server |
| [RecorderCtrl](https://github.com/VoidedKN0X/RecorderCtrl) | Private, offline voice recorder |
| [RideCtrl](https://github.com/VoidedKN0X/RideCtrl) | Offline maintenance, fuel and mod log for your vehicles |
| [SeerrCtrl](https://github.com/VoidedKN0X/SeerrCtrl) | Request movies and shows on Seerr / Jellyseerr |
| [ShutterCtrl](https://github.com/VoidedKN0X/ShutterCtrl) | Remote control and photo transfer for Canon cameras |
| [SoundCtrl](https://github.com/VoidedKN0X/SoundCtrl) | JBL headphones and speakers, without the account |
| [SumCtrl](https://github.com/VoidedKN0X/SumCtrl) | Offline calculator, unit converter and work tools |
| [SynapseCtrl](https://github.com/VoidedKN0X/SynapseCtrl) | Administer your Matrix Synapse server |
| [TalkCtrl](https://github.com/VoidedKN0X/TalkCtrl) | Cardo intercom settings, radio and updates |
| [TapoCtrl](https://github.com/VoidedKN0X/TapoCtrl) | Tapo plugs and bulbs on your home network |
| [TextCtrl](https://github.com/VoidedKN0X/TextCtrl) | Private SMS and MMS messaging |
| [TrueCtrl](https://github.com/VoidedKN0X/TrueCtrl) | Monitor and manage your TrueNAS server |
| [WakeCtrl](https://github.com/VoidedKN0X/WakeCtrl) | Keep the screen awake, per app or while charging |
| [WrtCtrl](https://github.com/VoidedKN0X/WrtCtrl) | Manage your OpenWrt router |

## License

ChatCtrl is licensed under the [GNU General Public License v3.0](LICENSE). © VKNXWorks

ChatCtrl is an independent app and isn't made by or affiliated with Element (New Vector Ltd) or The Matrix.org Foundation. It uses the open-source Matrix Rust SDK.

The bundled fonts, [Space Grotesk](https://github.com/floriankarsten/space-grotesk) and [JetBrains Mono](https://github.com/JetBrains/JetBrainsMono), are licensed under the SIL Open Font License 1.1; see `FONT-LICENSE-SpaceGrotesk.txt` and `FONT-LICENSE-JetBrainsMono.txt` (also in the APK under `assets/licenses/`).

Calls use [Element Call](https://github.com/element-hq/element-call) by Element (AGPL-3.0-only), built from source at release 0.28.0 (commit `9b3c3569ecefbb79cf527ea24e3756ba50553a8f`) with the changes in `element-call/` (build script, patches and replaced files, under the same licence); see [Element Call](#element-call). Its licence and a note of the source are in the APK next to it (`assets/element-call/LICENSE-AGPL-3.0` and `SOURCE.txt`). Background blur isn't available in calls.
