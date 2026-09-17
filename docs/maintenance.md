# 小型專案維護慣例

[使用手冊](../README.md) · [發布流程](release-process.md)

本專案採單人也能維持的流程：短期分支與 PR、自動檢查、版本 tag 發布；不要求每次發布人工批准或實機驗收。

## Commit 與分支

- 保留真實的開發順序；初次匯入較大、一次 push 多個 commits 都很正常，不改日期或捏造歷史。
- 每個 commit 代表一個可說明的改動，例如 `feat:`、`fix:`、`docs:`、`ci:` 或 `chore:`。不為湊數拆成無意義的小提交。
- 日常從 `main` 建立短期分支，開 PR，待 CI 通過後合併。草稿修正可以在 PR 合併時 squash；已公開的 main 不改寫歷史。
- 單人維護不要求第二人批准。CODEOWNERS 用於標示敏感檔案負責人；它本身不會阻止修改，須配合遠端規則。
- `main` 禁止 force push 與刪除。未來加入協作者時，再評估 workflow 與簽章相關檔案是否要求另一位維護者審查。

## 自動檢查與版本

PR 與 main 更新跑單元測試、lint 和發布檢查腳本的測試，不產生正式 APK。版本 tag 才執行完整 Release 打包與簽章。lint 以 0 errors 為門檻，既有 warnings 逐步修正。

`version.properties` 是版本來源。準備發布時更新 versionName 與遞增 versionCode，再建立對應 `vX.Y.Z` tag。公開版本不替換 APK、不移動 tag；修正另發新版。

測試與建置成功不代表所有 BOOX 韌體都相容。一般使用者可在 issue 回報機型、韌體、App 版本與操作步驟；勿附私人書籍 URI、閱讀紀錄、裝置序號或 keystore。

## 必要的安全保護

- Actions token 預設唯讀；僅發布 job 取得 Release 寫入權限。
- 正式簽章使用 `release` Environment Secrets；限制 Environment 僅供 `v*` tag 使用，不對一般 PR 開放。
- 建置與簽章分開；簽章 job 不執行專案 Gradle。第三方 actions 固定完整 commit SHA，更新需經 PR。
- 保護 main 與版本 tag，限制管理者以外的人建立版本 tag；公開 tag 禁止更新與刪除。GitHub 帳號啟用 passkey 或雙因素驗證。
- keystore 不進 Git、logs、cache 或 artifact；另存加密離線備份。簽章 job 仍能使用私鑰，因此擁有 repo 管理權限或能修改發布流程的人必須可信任。

## 定期維護

每月集中檢查一次 Actions 與 Gradle 相依套件更新，確認 CI 後再合併；不自動合併更新。發現安全修正時提早處理。公開文件以使用手冊與技術文件分開，README 保持操作導向。

正式 keystore 不是可任意輪替的一般 API token。遺失或外洩時先停止發布、檢查影響，再規劃 Android 簽章遷移；不要直接換 key 發布不能覆蓋安裝的 APK。

## 新 repo 的首次匯入

目標為 `clspeter/boox-cover-sync`。原始本機歷史保留作為備份；若歷史文件包含私人裝置或閱讀測試資訊，僅在隔離的發布副本去識別化，保留原有 commit 訊息、作者、時間與開發順序。清理會改變 commit SHA，因此應在新 repo 首次公開前完成，而非公開後重寫歷史。

遠端保護是否生效須由 GitHub API 或設定頁確認；加入 CODEOWNERS、workflow 或這份文件不代表遠端設定已完成。實際部署狀態記錄於[發布流程](release-process.md)。
