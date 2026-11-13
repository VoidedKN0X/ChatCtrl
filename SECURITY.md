# Security policy

Thank you for helping keep ChatCtrl and its users safe.

## Reporting a problem

**Please don't report security problems in a public issue.** Report them privately instead:

1. Go to the [Security tab](https://github.com/VoidedKN0X/ChatCtrl/security) of this repo.
2. Click **Report a vulnerability** and describe what you found, how to reproduce it, and which version of ChatCtrl you used.

Only the maintainer can see the report. I'll reply as soon as I can, work on a fix, and credit you in the changelog if you'd like.

## Supported versions

Only the latest release gets security fixes. Please update before reporting, and check the [changelog](CHANGELOG.md) to see whether it's already fixed.

## What counts

- Your access token, password, recovery key, encryption keys or messages leaking to anyone other than your own Matrix homeserver (and, for push, your own ntfy server)
- Ways around the app lock, or protected screens showing up in screenshots, Recents or on the lock screen when they shouldn't
- Messages, links or files from other people making ChatCtrl do something you didn't ask for, such as opening a dangerous link or writing files outside its own folders
- The call screen loading or talking to anything other than the bundled Element Call, your homeserver and the call service it announces
- Another app on the phone being able to make ChatCtrl do something you didn't ask for
- Anything that lets someone run code or read files they shouldn't

Problems in your Matrix homeserver (Synapse or another), the Matrix Rust SDK, Element Call or ntfy themselves should go to those projects.

## How releases are protected

Every release is built from this repo by GitHub Actions and signed with the same release key. Its SHA-256 certificate fingerprint is:

```
9E:E0:FC:5F:52:3F:E1:B8:D6:84:96:40:53:E0:AA:56:C6:5B:4F:72:6E:56:08:DE:9D:BE:8B:61:AC:92:9A:BA
```

The [CtrlF-Droid repo](https://voidedkn0x.github.io/CtrlF-Droid/) only accepts APKs signed with this key.
