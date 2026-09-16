# BOOX Page Root／還原社群資料

研究日期：2026-09-16。本文只整理公開來源；沒有在裝置上執行 root、刷寫、解鎖或還原。

## 先看結論

- BOOX Page 確實有社群成功 root 案例；可明確核對成功的是 3.3.2，另有 3.5.1 的 EDL／備份實測與 root 指引引用。當前裝置的 `2026-06-05_17-34_4.2-rel_0605_cf5a910290` 沒有找到相同 build 的 root 或 unroot 驗證。
- Page 的常見路徑是 Qualcomm EDL 讀出**同一台裝置自己的** `boot_a`／`boot_b`，在該裝置上用 Magisk patch，再以 EDL 寫回正確 slot。Page 的新版 bootloader 可能拒絕 `fastboot flash`，社群改用 EDL。
- 最可恢復的設計是先保留完整 EDL raw dump，再只修改 boot。要取消 root，優先使用 Magisk App 的卸載；App 無法使用時，才考慮用同一台裝置、同一韌體所讀出的原始 boot image 寫回。完整 raw dump 還原需要確認 EDL 工具的 LUN／partition 格式，不能套用別台機器的指令或映像。
- 不建議目前直接刷 `vbmeta`、刷別人的 patched image、恢復原廠 boot 到未知 slot，或重新鎖定 bootloader。這些都沒有針對目前 4.2 build 的可靠驗證。

## Magisk 官方規則

官方安裝文件：<https://topjohnwu.github.io/Magisk/install.html>

可核對的流程與限制：

1. 先確認 bootloader 已解鎖，取得裝置自己的 `boot.img`（若架構不同則是 `init_boot.img` 或 `recovery.img`）。官方說映像可從官方 firmware package 取得。
2. 將映像放到裝置，Magisk 選 `Install` → `Select and Patch a File`，完成後以 `adb pull /sdcard/Download/magisk_patched_[random_strings].img` 取回 patched image，再依裝置方式刷入。
3. 官方明確警告：`NEVER flash patched image shared by others ... ALWAYS patch boot image on the same device`。因此不能拿網路上 Page 3.5.x 的 patched boot 直接套到本機 4.2。
4. 官方的卸載入口是 Magisk App 直接卸載；若只能用 custom recovery，可將 Magisk APK 改名為 `uninstall.zip` 再刷入。這是通用 Magisk 卸載說明，不代表 BOOX Page 4.2 的 recovery 一定能刷該 zip。

官方 OTA 文件另說明：<https://topjohnwu.github.io/Magisk/ota.html> 的 `Magisk → Uninstall → Restore Images` 會使用安裝時建立的備份，把被 Magisk 修改的 partition 還原成 stock，以通過 OTA 的 block verification；執行後不要重開機，否則會變成已卸載 Magisk。這個功能依賴 Magisk 當時成功建立的備份，不能假定每種 EDL／手動流程都有可用備份。

官方安裝文件也指出，若另有 `vbmeta`，patch／刷入可能清除資料；因此 `vbmeta` 不是目前 Page 4.2 可安全照抄的步驟。

## BOOX Page 社群 root 路徑

### Page 3.5.1 的 EDL walkthrough

來源：<https://www.mobileread.com/forums/showthread.php?p=4424204>

該篇作者明確標示裝置是 Qualcomm Snapdragon 662、韌體 3.5.1，並回報以下 EDL／備份項目已測試可用：

- `adb reboot edl` 進 Qualcomm EDL 9008；也記錄 EDL cable／測試點作為無法進 Android 時的替代入口。
- Page 是 A/B device，且有 `super` partition。
- 以 EDL 工具和對應 Firehose loader 讀出 partition；社群建議先做完整 NAND／eMMC backup。
- 文章把 root 寫成「follow Renate's thread instructions ... use the boot.img backup」，也就是建議使用該台 Page 從 EDL 讀出的 boot image，再依 Poke5 指引 patch；這篇本身不能單獨當作 3.5.1 root 已成功的證據。

### Page 3.3.2 的明確成功回報

來源：<https://www.mobileread.com/forums/showthread.php?p=4375510>

Page 使用者回報在 3.3.2 上先用 EDL 讀出 `boot_a`、在同一台裝置上以 Magisk patch，之後先 `fastboot boot` 測試，再 `fastboot flash`，表示「Seems I am rooted now on 3.3.2 through multiple reboots」。同一串也提醒 A/B slot 要先確認；這是明確的舊版成功案例，不能推論 4.2 相容。

### Poke5 指引被 Page 引用

來源：<https://www.mobileread.com/forums/showthread.php?t=357269>

這篇不是 Page 專文，但 Page walkthrough 明確引用它。其核心流程是：

1. 取得與裝置相符的官方 update，解密為 `update.zip`，從 `payload.bin` 取出 `boot.img`／`vbmeta.img`；或在 Page 上直接從 EDL 讀出 boot。
2. 將 boot image 放到同一台裝置，使用 Magisk patch，取回 patched image。
3. 查 A/B 目前 slot，再只對應該 slot 操作。
4. 原始 Poke5 指引以 fastboot 寫入 `vbmeta` 與 patched boot；但 Page 後來的實測顯示 fastboot flash 可能不可用，因此不能照抄這個最後步驟。

### Page 新版 fastboot 失效後改用 EDL

來源：<https://www.mobileread.com/forums/showthread.php?p=4518202>

2025 年 Page 使用者回報 `fastboot flash` 會失敗：`FAILED (remote: 'Unrecognized command flash')`。回覆建議改用 EDL；同一串也說明：

- Page 與 Go6 使用相同 SM6115，Renate 提供的 loader 名稱是 `poke6.bin`，可改名為 `page.bin`，但這只表示社群硬體／loader 關聯，沒有證明適用目前 4.2 build。
- 進 EDL 並載入 loader 後，可以做約 32 GB 的完整 raw backup；原文命令為 `edl /r big-honking-backup.img`。
- 對 Page，社群回答的寫入概念是 `edl /w /pboot_a patched_boot.img`（或依 active slot 改成 `boot_b`），再重開機。
- 回覆指出 patched boot 通常已處理相關驗證，`vbmeta` 可先不碰；這是社群個人經驗，不是對 4.2 的保證。

## EDL 的官方／工具說明與風險

Renate EDL Utility：<https://www.temblast.com/edl.htm>

該工具文件說明：

- EDL 通常以 USB VID/PID `05c6/9008` 出現；Windows 使用 WinUSB，文件特別提醒不要使用 Qualcomm serial driver 來取代 WinUSB。
- Firehose loader 依 processor、製造商、flash memory type，以及 HWID／hash 等條件相符；不能只因檔名像 `page.bin` 就認定相容。
- `/r` 是 read、`/w` 是 write；未指定 partition 的 write 可能作用於整個 device，工具文件明確要求小心。
- 文件提供 `edl /r /pboot_a boota.img /t` 讀 boot、`edl /w /precovery rec.img` 寫 recovery、`edl /z` 重開等一般範例。這些是工具語法，不是本機 4.2 的操作指示。

Page 的 2025 變磚案例：<https://www.mobileread.com/forums/showthread.php?t=366814>

這串是重要的還原證據，但同時顯示風險：

- Page 可在 EDL 讀 GPT，列出 `boot_a`、`boot_b`、`super`、`recovery_a`、`recovery_b`、`vbmeta_a`、`vbmeta_b` 等 partition。
- Page 無法正常開機時，社群先用 EDL 寫入特殊 `misc`，再進 recovery，以 sideload firmware，最後恢復進 system；這證明有救援路徑，但不是「一定成功」的標準 unroot 流程。
- 同一案例出現 USB 連接、電池與開機異常，表示錯誤映像／錯誤 partition 可能造成額外故障。

另一個 Page 案例：<https://www.mobileread.com/forums/showthread.php?p=4499437> 回報刷錯 boot 後卡在開機畫面，後來透過 recovery sideload firmware 恢復可開機；但 Wi‑Fi 仍有故障。這表示「能進 recovery 並 sideload」不等於完整回復到原廠狀態，還原後仍須逐項驗證硬體相關功能。

### `boox-ams-fix` 不是目前的還原方案

來源：<https://github.com/dynamicfire/boox-ams-fix>

這個 Magisk module 是針對 BOOX 修改過的 `services.jar`／Magisk App 啟動 NPE 問題，README 說明其內置 JAR 來自 P6 Pro、目標是 firmware 4.1；其他 build 可能因 ART 不匹配而無法開機。它是 root 後 Magisk 管理程式異常的修補，不是移除 root 或解決本案 BOOX force-stop 的證據；目前沒有 Page 4.2／本機 build 的適用驗證。

## 還原／unroot 的可證實階梯

1. **Magisk App 可開啟時**：先用官方卸載功能。若目的是 OTA 前暫時還原，使用 `Uninstall → Restore Images`，並遵守官方「還原後不要先重開機」說明。
2. **Magisk App 不能開，但仍能進 Android／ADB 時**：不要先刷網路映像。應先讀取、保存目前兩個 boot slot，確認 active slot 和目前 build，再決定是否以本機原始 boot 還原。
3. **只有 EDL 可用時**：載入已確認與本機 HWID／storage 相容的 loader，先做並驗證完整 raw backup；只把同一台、同一 build、root 前保存的 `boot_a`／`boot_b` 寫回相應 partition。Page 社群有這條路徑，但沒有本機 4.2 的成功報告。
4. **沒有 root 前 boot 備份時**：可研究完全相同型號／build 的官方 `update.upx`，由 `decryptBooxUpdateUpx` 取得 update package，再抽出 stock boot；但目前沒有證據已找到本機 4.2 package，也不能以其他 Page build 的 boot 代替。
5. **完整 raw dump 還原**：工具與社群都證實可以讀取完整 dump；寫回時必須依實際 EDL client 的 partition／LUN 格式與同一台裝置的 GPT 操作，不能在未確認格式前提供一條「整碟寫回」命令。社群特別警告不要在保留資料的備份後再 factory reset／wipe userdata，因為可能刪除隱藏的 userdata 解密金鑰，使備份資料無法使用。

## 目前裝置的適用性判定

已知本機：BOOX Page、Android 11、`2026-06-05_17-34_4.2-rel_0605_cf5a910290`、ADB 回報 bootloader unlocked／Verified Boot orange、A/B；目前未找到 `su` 或一般 Magisk package。

截至本研究：

- 找到 Page 3.5.1 的 EDL／備份實測，以及 2025 年 Page 因 fastboot flash 失效而改用 EDL 的討論。
- 找到 Page 3.3.2 的明確 root 成功回報；3.5.1 walkthrough 僅能證實 EDL／備份實測與引用 root 指引，不能單獨當作該版本 root 成功證據。
- 沒找到 Page 4.2、上述 2026-06-05 build 的社群 root 或 unroot 成功驗證。
- 沒找到 BOOX 官方 root／unroot 指引或對該 build 的公開完整 stock image 還原說明。
- `boox-ams-fix` 的公開 README 警告其 4.1／特定裝置 JAR 不能直接套用其他 build；也沒有證據顯示它能修正目前的 force-stop。
- 因此可對使用者說「社群有可行的 EDL + 本機 boot patch 路徑，也有用本機 dump 還原的方向」，不能說「舊版方法已確認適用 4.2」，也不能承諾不清除資料即可救回。

## 來源索引

### 使用者補充的較新 Page 案例與 OTA

Gist：<https://gist.github.com/carlosonunez/a0ec3f02576867329bc313bae889563d?permalink_comment_id=6009281>

- Gist 本文宣稱流程仍適用 Page 7 的 `2026-02-10`、commit `5c56f32dee`。這是比前述 MobileRead 更近期的適用回報；仍不是本機 `2026-06-05...cf5a910290` build，不能視為本機已驗證。
- 指定留言經 GitHub API 核對：<https://api.github.com/gists/a0ec3f02576867329bc313bae889563d/comments/6009281>，2026-03-02 的 BeeFox-sys 只問應寫回原 slot 還是另一 slot；不是 OTA／unroot 成功回報。
- 初次 Root 是把目前 slot 的 boot 讀出、在同一裝置 patch，再寫回相同 slot。Gist 寫入範例的註解 `or boot_a` 有重複筆誤，必須以實際 slot 為準。本機上次讀到 `_b`，操作前仍需重查。
- A/B OTA 是另一流程：Magisk 官方建議還原原始映像、安裝 OTA，完成後先不重開機，再將 Magisk 安裝至已更新的 inactive slot，最後由系統更新介面重開機。這是通用流程，沒有本機 Page 4.2 的實測保證。較保守做法是先取消 Root／還原正確原始映像，正常更新驗證後，再讀出新版 boot 重新 patch；不可把舊版 patched boot 刷到新版系統。
- Gist 的防火牆與 debloat 是額外選項，與本任務封面同步無關，不能一併套用。本文新來源修正前述「只有舊版 Page 案例」的範圍，但不改變「本機完整版本尚未驗證」的結論。

- Magisk Installation／Uninstallation：<https://topjohnwu.github.io/Magisk/install.html>
- Magisk OTA／Restore Images：<https://topjohnwu.github.io/Magisk/ota.html>
- BOOX Page EDL walkthrough（3.5.1）：<https://www.mobileread.com/forums/showthread.php?p=4424204>
- Rooting the Poke5（Page 所引用的通用 Magisk 流程）：<https://www.mobileread.com/forums/showthread.php?t=357269>
- Anyone able to root the Boox Page?：<https://www.mobileread.com/forums/showthread.php?t=357331>
- Page fastboot `Unrecognized command flash`／EDL workaround：<https://www.mobileread.com/forums/showthread.php?p=4518202>
- Page EDL 變磚／recovery 救援案例：<https://www.mobileread.com/forums/showthread.php?t=366814>
- Page 錯誤 boot 後 recovery sideload 案例：<https://www.mobileread.com/forums/showthread.php?p=4499437>
- EDL Utility 文件：<https://www.temblast.com/edl.htm>
- BOOX update.upx 解密工具：<https://github.com/Hagb/decryptBooxUpdateUpx>
