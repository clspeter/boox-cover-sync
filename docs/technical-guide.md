# BOOX Cover Sync 技術文件

[返回使用手冊](../README.md)

本文件供開發、建置與診斷使用；一般操作與背景設定請看使用手冊。實作與驗收資料整理至 2026-09-17。

## 安裝、更新與權限

目前為可側載的 Android 專案，APK 可自行建置。Android 最低版本為 8.0（API 26），但不代表所有 Android 裝置都具備 BOOX 所需介面。

### 使用電腦側載

電腦需安裝 Android Platform Tools，並讓 BOOX 開啟 USB 偵錯、接受電腦授權。在專案根目錄執行 PowerShell：

```powershell
adb devices
$serial = '填入 adb devices 顯示的裝置序號'
adb -s $serial install -r .\app\build\outputs\apk\debug\app-debug.apk
adb -s $serial shell am start -n tw.mustp.booxcoversync/.MainActivity
```

`install -r` 可在簽章相同時覆蓋更新並保留 App 資料。若出現 `INSTALL_FAILED_UPDATE_INCOMPATIBLE`，先確認是否更換建置電腦或 debug keystore；不要直接卸載。卸載會失去 App 設定、授權與本機識別金鑰，重新安裝也可能重新啟用 BOOX 凍結設定。

安裝後可拔除 USB；日常同步不依賴電腦。

### 儲存空間與權限

| 項目 | 用途與設定 |
| --- | --- |
| 管理所有檔案（Android 11+） | 從 App 的「開啟公共儲存空間設定」授權。最近閱讀模式可能需要直接讀取公共書庫，也要讓 BOOX 系統讀取輸出的封面。 |
| 檔案選擇器授權 | 手動選 EPUB 時取得讀取權；不等同公共儲存空間授權。 |
| 無障礙服務 | 僅背景自動同步需要。手動開 App／選檔模式不依賴它。 |
| DUMP、Usage Access | 保留給舊版 dumpsys 定位實驗與診斷。**目前正式定位使用 Metadata provider，不以這兩項作為前置檢查。** |

公共輸出路徑無法寫入時，App 會依序嘗試 app-specific 外部 Pictures 與內部 Pictures；但 BOOX 系統服務未必能讀取這些位置，因此「App 成功寫檔」不代表休眠圖一定可用。

UI 仍保留 DUMP／Usage Access 的診斷狀態，且「條件完整／尚缺必要權限」文字仍沿用舊版三項權限判斷。即使顯示缺少權限，目前 Metadata 流程仍可能正常同步；該狀態不能單獨判定定位是否可用，也不能證明無障礙服務真的保持連接。

## 背景限制與耗電

### BOOX 4.2 的 5 分鐘限制

本機曾在解除凍結、加入 Android Doze 白名單後，仍被系統強制停止。後續發現個別 App 的上述背景時間設定仍為 **5 分鐘**。解除凍結、Doze 白名單與這個時間上限是不同設定，不能互相取代。

改成「持續運行」後，一次 9 分鐘、51 次採樣皆保持服務連接，沒有新的強制停止；覆蓋安裝後也確認設定保留。這是本機短時間驗證，尚非長期存活保證，也沒有直接取得最初發起 force-stop 的呼叫堆疊。

社群指出背景設定會隨 E InkWise 的 App 設定檔切換。更換「個性化／推薦」等設定檔後，請再次確認時間上限。選單名稱與位置可能因機型、語言與韌體而不同。

來源：[Palma 2 Pro 背景執行設定](https://github.com/Kisuke-CZE/Palma_2_Pro-tips#enabling-app-to-run-in-background)、[BOOX App Management Settings](https://help.boox.com/hc/en-us/articles/10701262170644-App-Management-Settings)。社群機型與 Page 不同，但本機已實際確認相同時間選項。

### 耗電取捨

「持續運行」允許 App 留在背景，不代表持續運算或保持螢幕亮著。背景模式由事件觸發，沒有固定每幾秒永遠查詢的輪詢，也沒有為同步持續保持喚醒的 WakeLock。

查詢、EPUB 解析、圖片處理及服務常駐仍可能增加耗電；**目前沒有耗電百分比或續航實測**。想優先省電，可關閉「啟用自動同步」，需要換封面時才手動開啟 App。

## 服務存活與診斷

喚醒接收器只在無障礙服務執行時註冊；套件 `stopped=true` 或服務停用時，無法靠喚醒事件自救。手動開啟 Activity 可走獨立同步流程。右下角無障礙快捷按鈕尚未實作，也不能取代服務存活管理。

以下指令皆從專案根目錄執行，先用 `adb devices` 取得序號，再設定 `$serial`。

### 只讀觀察服務狀態

連接 ADB 後，在專案根目錄執行：

```powershell
.\scripts\boox-watch-service.ps1 -Serial $serial -DurationSeconds 90
```

腳本預設每 2 秒取樣，只在狀態變動時輸出 JSON；時間範圍為 1–600 秒，可用 `-IntervalSeconds` 設定 1–30 秒取樣間隔。它不重啟 App、不修改設定、不讀取畫面內容，也不輸出書名或閱讀 URI。

| 欄位 | 意義 |
| --- | --- |
| `packageStopped` | 套件是否處於 Android 停止狀態 |
| `accessibilityEnabled` | 設定清單是否含本 App；不等於服務已連接 |
| `serviceBound` | 本 App 無障礙服務是否已連接 |
| `serviceListedCrashed` | 系統是否把服務列於異常清單 |
| `lastExit` | 最近退出時間、原因代碼、呼叫 PID 與是否為安裝相關退出 |

退出原因 `10 / USER REQUESTED` **不能單獨解讀成使用者按了停止**。若呼叫方是 system_server，只能證明系統執行停止動作，無法辨識最初發起的元件。覆蓋安裝本身也可能留下停止紀錄。

### 舊版定位與權限診斷

```powershell
.\scripts\boox-dump-diagnostics.ps1 -Serial $serial
```

預設只讀取套件、無障礙、權限及 dumpsys recents 的診斷結果；它不是目前 Metadata provider 的完整健康檢查，不應以 recents 診斷失敗判定最新閱讀模式必然失效。

| 選項 | 行為 |
| --- | --- |
| `-GrantDump` | **會修改授權**：嘗試授予受保護的 DUMP 權限，裝置可能拒絕。 |
| `-GrantUsageStats` | **會修改授權**：嘗試授予 PACKAGE_USAGE_STATS 並設定 Usage Stats app-op。 |
| `-RawRecents` | 相容舊介面的選項；刻意不輸出原始 recents，以免揭露閱讀路徑。 |

不需要為了一般同步先執行授權選項。分享診斷時，也請遮蔽裝置序號等個人識別資訊。

## 手動啟動行為

`MainActivity` 搭配 `ManualSyncLaunchGate`，在冷啟動或從背景返回時同步一次；從自身開啟的檔案選擇器或設定頁返回、configuration recreation 時避免重複。單次同步不依賴背景自動同步開關，也不以相同 fingerprint 跳過發佈。

Activity 的休眠／關機開關預設開啟，未保存至 SharedPreferences；只作用於手動同步。背景模式目前同時發送兩種畫面。成功狀態代表發送廣播成功，沒有 BOOX 接收端套用成功回執。

## 同步流程與資料處理

### 最近閱讀與背景模式

```text
開啟 App／按鈕，或背景事件
  → BOOX Metadata provider
  → 選擇 lastAccess 最新的有效 EPUB
  → 讀取並解析封面
  → 縮放、寫入 JPEG
  → BOOX screensaver 廣播
```

`BooxMetadataProviderLocator` 查詢 `com.onyx.content.database.ContentProvider/Metadata`。`DumpsysNeoReaderLocator` 保留作為實驗／診斷程式，不在目前正式同步路徑。

NeoReader 的私有 FileProvider URI 可能拒絕直接讀取。`BooxEpubInputSource` 只對已知的 `com.onyx.kreader.onyx.fileprovider` 與 `external` root 嘗試公共儲存空間 fallback，驗證 canonical path、儲存根目錄與 EPUB 副檔名；不是任意 URI 的權限繞過機制。

背景流程將視窗事件合併至第一個事件的 2 秒查詢期限。未找到候選、候選未變或處理完成後，仍可用完該輪最多兩次後續查詢，間隔為 5 秒與 10 秒；圖片處理時間另計。新事件可能啟動新一輪，這不是保證在固定秒數內追上新書。

背景模式用每次安裝產生的 HMAC-SHA256 金鑰識別 URI；同一本書不重複解析或送圖。手動模式允許重送。關閉螢幕、關閉背景開關或服務結束會取消背景待處理工作；手動一次同步不由該背景狀態機控制。

### 封面檔案與 BOOX 廣播

輸出為 **1264 × 1680、JPEG 品質 90**，預設保留比例、白底留白，針對 BOOX Page 尺寸設計。

| 路徑形式 | 使用方式 |
| --- | --- |
| `Pictures/BookCover/cover-<HMAC>.jpg` | 最近閱讀／背景模式，每本書使用穩定檔名，避免不同書共用路徑的快取問題。 |
| `Pictures/BookCover/current-a.jpg`、`current-b.jpg` | 手動選檔的重新同步，交替使用兩個檔案。 |

公共目錄優先，其次為 App 專屬外部目錄與內部目錄。圖片透過同目錄暫存檔與替換寫入；目前每本書的 `cover-*.jpg` **沒有總數上限或自動淘汰機制**，因此不能把整個目錄描述為「最多只有兩張封面」。

App 發送 implicit `onyx.action.SCREENSAVER` 廣播：`type=16` 設定休眠封面、`type=17` 設定關機封面。先前實測加上 explicit `com.onyx` package 會失敗，因此 adapter 保持 implicit 發送。

### 隱私與解析限制

- 所有 EPUB 解析、閱讀紀錄查詢與圖片處理都在本機完成；Manifest 未宣告 INTERNET 權限。
- UI 不顯示書名或完整來源路徑，但會顯示封面預覽；輸出的封面圖片本身仍可能含書名，分享畫面或檔案前請留意。
- EPUB 解析支援 EPUB 2／3 的封面宣告，檢查路徑穿越、重複項目、壓縮比例、大小與圖片記憶體限制，拒絕 XML `DOCTYPE`／`ENTITY`。
- 不承諾可讀取 DRM 或無法取得讀取授權的書籍，不支援 PDF 等非 EPUB 的自動封面擷取。
- App 不會 Root、修改系統分割區，或自行授予受保護權限。

## 開發與建置

使用 Kotlin、Android SDK 與 Gradle wrapper。需要 JDK 17、Android SDK Platform 36、Build Tools 35.0.0；SDK 路徑可透過 `ANDROID_HOME` 或本機 `local.properties` 的 `sdk.dir` 設定。首次建置需能下載 Gradle／依賴。

在專案根目錄執行 PowerShell：

```powershell
$env:GRADLE_USER_HOME = Join-Path (Get-Location) '.gradle-user-home'
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug '-Pkotlin.compiler.execution.strategy=in-process' --no-daemon
```

macOS／Linux 可使用 `./gradlew` 執行相同 tasks。APK 產物：

```text
app/build/outputs/apk/debug/app-debug.apk
```

`.gradle-user-home/`、`work/`、建置輸出、APK 與 `local.properties` 均已忽略。保持既有 debug keystore 才能對已安裝版本覆蓋更新；不要將金鑰、書籍、私人 log 或裝置備份提交到 Git。

### 程式位置

| 位置 | 職責 |
| --- | --- |
| `MainActivity.kt`、`ManualSyncLaunchGate.kt` | 手動啟動／按鈕、檔案選擇、畫面與啟動去重 |
| `reader/` | Metadata 候選選擇、observer、舊版 dumpsys locator |
| `autosync/` | 無障礙事件、排程、HMAC、EPUB 輸入與同步 pipeline |
| `epub/`、`image/`、`boox/` | 安全解析、圖片輸出與 BOOX 廣播 |
| `app/src/test/`、`scripts/` | JVM／Robolectric 測試與 ADB 診斷 |

上表 Kotlin 位置皆相對於 `app/src/main/java/tw/mustp/booxcoversync/`。

## 已驗證與尚未驗證

截至 **2026-09-17**，實測 BOOX Page／Android 11／韌體 `2026-06-05_17-34_4.2-rel_0605_cf5a910290`：

| 項目 | 結果與界線 |
| --- | --- |
| 建置 | 80 個測試通過、lint 通過、debug APK 建置及覆蓋安裝成功。 |
| 手動啟動同步 | 無障礙停用、套件強制停止後冷啟動，以及從桌面熱返回，均重新送出最新閱讀紀錄的封面。 |
| 實際休眠封面 | 最新手動同步的輸出與休眠截圖縮成相同灰階尺寸比較，平均差為 0；不是所有書籍的全面測試。 |
| 背景與關機封面 | 合成 A/B EPUB 曾完成自動同步與休眠換圖；改為持續運行後通過 9 分鐘觀察。關機封面曾於 2026-09-14 單獨驗證，最新版本沒有再做完全關機測試。 |
| 未驗證 | 長時間存活、實際耗電、其他機型／韌體，以及每次 Metadata 都準確對應目前書籍。 |

驗收時請依序確認「來源書籍是否正確、同步工作是否完成、實際休眠畫面是否換圖」，不要只看 App 開關或廣播送出。完全關機測試需先取得裝置使用者確認。

## 診斷紀錄與參考

- [同步診斷紀錄與背景限制發現](sync-diagnosis-2026-09-16.md)
- [Root／還原與 OTA 社群研究](boox-root-restore-sources-2026-09-16.md)：研究備忘，正常使用不需 Root，也不是目前韌體的刷機操作指引。
- [BOOX 官方：設定書封為螢幕保護畫面](https://booxsupport.zendesk.com/hc/en-us/articles/4577439831828-How-to-set-a-book-cover-as-a-screensaver)
- [BOOX 廣播介面參考實作](https://github.com/lukebatchelor/koreader-boox-shutdown-image)
