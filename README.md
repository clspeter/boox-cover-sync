<div align="center">

# BOOX Cover Sync

**Your book cover, on your BOOX sleep and shutdown screens.**

No root required · Local processing · Optional background sync

**English** · [繁體中文](README.zh-TW.md)

[Download APK](https://github.com/clspeter/boox-cover-sync/releases/latest) · [Quick start](#quick-start) · [Known limitations](#known-limitations)

</div>

---

Use an EPUB cover as your BOOX screensaver: open the app to sync your latest NeoReader reading record, or choose a book yourself. Background sync is available if you prefer automatic updates.

> [!NOTE]
> Tested on **BOOX Page with BOOX 4.2**. Other models and firmware versions are unverified. The app interface is currently in **Traditional Chinese**; this guide includes the on-screen button labels.

## Choose how to sync

| Mode | What to do | Accessibility service |
| --- | --- | --- |
| **Sync when opening the app** | Read in NeoReader, then open BOOX Cover Sync. | Not needed |
| **Choose an EPUB** | Pick a book and apply its cover. | Not needed |
| **Background sync** | Enable automatic sync and allow background execution. | Required |

## Quick start

1. Open the [latest release](https://github.com/clspeter/boox-cover-sync/releases/latest), download the `.apk` asset, and install it on your BOOX. Allow installation from that source if prompted.
2. Open **BOOX Cover Sync**. If reading books or applying covers fails, tap **開啟公共儲存空間設定** and allow access to manage all files.
3. Open an EPUB in **NeoReader**, then open **BOOX Cover Sync** from the home screen. It will sync once automatically.
4. Wait for **同步完成** (sync complete), then put the device to sleep to check the cover.

To retry while the app is open, tap **同步最新閱讀封面** (sync latest reading cover). No computer or accessibility service is needed for this mode.

> [!TIP]
> Seeing an older book? BOOX reading records can lag behind. Wait about **20 seconds**, then retry. If the wrong cover persists, choose the EPUB directly below.

## Choose a cover yourself

1. Tap **選擇 EPUB** (choose EPUB).
2. Select your book and check the cover preview.
3. Enable **同步休眠畫面** (sleep screen), **同步關機畫面** (shutdown screen), or both.
4. Tap **立即重新同步** (sync again now).

Both screen switches default to on and may reset when reopening the app; check them again before syncing. Applying a shutdown cover **does not shut down the device**.

## Optional: background sync

1. On the BOOX Apps screen, long-press the app icon and unfreeze it if **解凍** is shown.
2. Open the app's **Optimize → Other → Allow background activity** settings and choose **Unlimited / 持續運行**.
3. Enable **BOOX Cover Sync** in the system's **Accessibility** settings.
4. Return to the app and turn on **啟用自動同步** (enable automatic sync).
5. Open another EPUB in NeoReader, **stay on it for about 10 seconds** to let sync finish, then put the device to sleep and check the cover.

Menu names vary by BOOX model, firmware, and system language. Background sync updates **both** sleep and shutdown covers, independently of the manual screen switches.

> [!IMPORTANT]
> **Unfreezing the app alone may not be enough.** The tested BOOX had a separate **5-minute background limit**, after which syncing could stop. Set it to Unlimited and recheck it after changing E InkWise profiles or reinstalling the app.
>
> Waking the screen does not reliably restart an app stopped by the system. Open BOOX Cover Sync manually to sync again.
>
> Sync usually finishes within about 5 seconds of opening a book. **Sleeping immediately after opening a book** cancels the unfinished sync, so that sleep still shows the previous book; the app catches up after wake and the next sleep shows the new cover.

For lower battery use, leave **啟用自動同步** off and open the app only when you want to change the cover. Unlimited background execution does not keep the screen on, but background processing may still use additional power. Battery impact has not been measured.

## Updating

Install the newer official release APK over the existing official version to retain settings. If installation fails, check the APK source and signing compatibility before uninstalling.

> [!WARNING]
> Earlier **debug builds use a different signing key** and cannot be updated directly to the official release. Switching requires uninstalling the debug app first, which removes its settings and permissions. Set up permissions and BOOX background options again afterward.

## Known limitations

| Area | What to expect |
| --- | --- |
| **Device compatibility** | Tested on BOOX Page / BOOX 4.2 only. Covers are sized for Page; appearance and behavior on other devices are unverified. |
| **Book formats** | EPUB only; no PDF cover extraction. The EPUB must declare a cover image. DRM-protected books or files without read access may not work. |
| **Latest-book detection** | Picks the book whose BOOX library record changed most recently (the newer of `updatedAt` and `lastAccess`); tested only with NeoReader on BOOX Page. If another app such as cloud sync updates a different book's record, the wrong book may be chosen. Selecting an EPUB avoids relying on those records. |
| **Background reliability** | BOOX may stop the app. Only a short background test has passed; long-term operation and battery impact remain unverified. The accessibility shortcut button is not implemented. |
| **Screen settings and confirmation** | Manual screen choices are not saved across app restarts; background sync applies both screens. “Sync complete” means the request was sent, so check the actual sleep screen. |
| **Storage** | Generated per-book cover images can accumulate; automatic cleanup is not implemented. |
| **Language** | The app UI is currently Traditional Chinese. An English README does not change the app language. |

## Troubleshooting

| Symptom | Try this |
| --- | --- |
| An older book's cover appears | Wait about 10 seconds after opening a book before sleeping; if still wrong, tap **同步最新閱讀封面** or use **選擇 EPUB**. |
| Sync stops after a few minutes | Unfreeze the app and set its background time limit to **Unlimited / 持續運行**. |
| Accessibility is enabled but nothing happens | Turn this app's accessibility service off and on, or open the app to sync manually. |
| A book cannot be found or read | Check that it is an EPUB, still exists on the device, and has the required storage access. |
| Sync completes but the sleep cover stays the same | Check the preview, enable **同步休眠畫面**, and retry. Confirm storage access if it still fails. |

Some permission indicators are retained for diagnostics and do not determine whether manual sync can work. Try syncing first; see the technical guide if more diagnosis is needed.

## Privacy

Book parsing and cover processing happen **on your device**. The app does not request internet access or upload your books. Cover previews and generated images can still reveal book titles—check them before sharing screenshots or files.

## Further reading

These documents are currently in Traditional Chinese:

- [Technical guide](docs/technical-guide.md) — building, permissions, and diagnosis.
- [Release process](docs/release-process.md) — automated builds and signing.
- [Project maintenance](docs/maintenance.md) — contribution workflow and repository protections.

---

[Download the latest release](https://github.com/clspeter/boox-cover-sync/releases/latest) · [繁體中文使用手冊](README.zh-TW.md)
