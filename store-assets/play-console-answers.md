# Google Play Console — answers

Prepared answers for the declarations Play asks for before the first
release: checklist items 11, 12 and 13 in `docs/RELEASE.md`. Everything
here is derived from what the app actually does — if the app changes,
change this too.

Play's review reads English, so the pasteable text is in English.

---

## Read this first: the 12-tester rule

Personal developer accounts created from late 2023 onward must run a
**closed test with at least 12 testers, opted in continuously for 14 days**,
before production access is unlocked. It is not a formality you can skip,
and the 14 days only start counting once you have 12 testers actually
opted in — losing one resets nothing, but falling below 12 pauses the
clock.

If the developer account is an **organisation** account rather than a
personal one, this doesn't apply.

Practical order, if the rule applies: upload the bundle to **closed
testing** first, get the testers in, and fill in everything below while
the 14 days run. Nothing in the store listing has to wait.

---

## 11. Foreground service declaration

The app declares one foreground service type. Play asks for a written
justification and, in most cases, a **demo video** (an unlisted YouTube
link is fine — a phone screen recording showing the alarm start and stop
is enough).

**Type:** `mediaPlayback`
**Declared on:** `FindPhoneService`

**Justification (paste as-is):**

```
PhoneFinder helps a user locate their own misplaced phone from their
Pebble smartwatch. When the user presses the button on the watch, the
watch sends a command to the phone and the app plays a loud alarm tone
on the alarm audio stream until the user stops it — either by pressing
the watch button again or by tapping Stop in the notification.

The foreground service exists to play that audio tone. Playback is
always started by an explicit user action on the paired watch, or by the
"Test alarm" button inside the app; it is never started automatically or
in the background without user intent. The ongoing notification carries a
Stop action so the user can always end it from the phone. Playback runs
only for as long as the alarm sounds, and the service stops itself
immediately afterwards.
```

**What the video should show:** the app open, pressing "Test alarm", the
alarm sounding with the notification visible, then tapping **Stop** and
the notification disappearing. Showing the watch press as well is nice but
not required — reviewers care about the service and the user-visible stop
control.

---

## 12. Data safety form

The app collects nothing and transmits nothing. Answers:

| Question | Answer |
|---|---|
| Does your app collect or share any of the required user data types? | **No** |
| Is all of the user data collected by your app encrypted in transit? | n/a (nothing is collected) |
| Do you provide a way for users to request that their data be deleted? | n/a (nothing is collected) |

Backing facts, in case a reviewer asks:

- No network permission is declared at all. The app cannot talk to the
  internet.
- The only things written to disk are local settings: the chosen alarm
  sound (a system ringtone URI), the diagnostics toggle, and the pinned
  Pebble host app. None of it is personal data and none of it leaves the
  device.
- The diagnostics log is a troubleshooting record of watch traffic, shown
  behind a toggle on the About screen. It stays on the device, and both it
  and the pinned-host state are excluded from cloud backup
  (`backup_rules.xml` / `data_extraction_rules.xml`).
- No analytics, no crash reporting, no advertising SDK — no third-party
  SDKs at all except PebbleKitAndroid2, which talks to the Pebble
  companion app on the same phone over IPC, not over a network.

**Privacy policy URL:**
`https://mrmastodon.github.io/Pebble-Phone-Finder/privacy-policy.html`

---

## 13. Content rating questionnaire

Category: **Utility, Productivity, Communication, or Other**.

Every content question is **No** — there is no violence, sexuality,
profanity, drug reference, gambling, or user-generated content of any
kind. The app has one screen with one button and an About page.

Two that are easy to answer wrong:

- **Does the app share the user's location with other users?** No.
- **Does the app allow users to interact or exchange content?** No. The
  only communication is between the user's own watch and their own phone.

**Target audience:** the app isn't designed for or directed at children.
Selecting an adult age band (18+ or 13+) keeps it out of the Families
programme and its extra requirements, which is the right fit — there is
nothing child-directed about it.

---

## Other declarations you'll meet

| Question | Answer |
|---|---|
| Ads | **No**, the app contains no ads |
| In-app purchases | **No** |
| App access (login required?) | **No**, all functionality is available without an account |
| Government app | **No** |
| Financial features | **No** |
| Health apps | **No** |
| Data deletion request URL | n/a |

**Category:** Tools. **Contact email:** required and shown publicly on
the listing — use an address you're happy to publish.

---

## One thing worth knowing about the donate button

The About screen links out to PayPal ("Buy me a coffee"). Play's payments
policy requires Google Play Billing for purchases of in-app digital
goods, and links to outside payment for such goods are what gets apps
rejected.

What keeps this on the right side of the line is that the button unlocks
nothing: every feature of the app is available to everyone, the payment
is a voluntary tip, and no content or functionality is gated behind it.
That's the distinction the policy actually turns on.

It is still the single most likely thing in this app to draw a reviewer's
attention, so it's worth being aware of rather than surprised by. If it
ever is flagged, removing the button is a one-line change and costs the
app nothing functionally.
