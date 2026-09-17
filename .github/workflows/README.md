# GitHub Actions 維護規則

這個專案把正式發布限制在 `vX.Y.Z` tag。一般 PR 與 `main` 只執行 release unit tests 和 lint；只有 Release workflow 的 `sign` 工作可讀取 `release` environment 的正式簽章 secrets，`publish` 工作才有 `contents: write`。

儲存庫管理員仍需在 GitHub 設定以下保護。這些設定不能由 workflow 自己保護：

1. 將 `main` 設為 protected branch，禁止直接 push，要求 Pull Request 與 CI workflow 的 `verify` job required check（GitHub UI 通常顯示為 `CI / verify`）。
2. 將 `v*` 設為 protected tag pattern，只允許管理者建立；禁止更新或刪除版本 tag。
3. 使用 `.github/CODEOWNERS` 標示敏感檔案的責任人，並限制 workflow／Actions 設定的管理權限；小型單人專案可維持 0 個必要 owner approval。
4. 建立名稱為 `release` 的 Environment，加入四個 signing secrets，以及公開憑證變數 `ANDROID_SIGNING_CERT_SHA256`。
5. 啟用 GitHub 帳號的 passkey 或雙因素驗證；不要把 keystore、密碼、cache 或簽章輸出放進 repository。

正式發布入口是：更新版本與 `versionCode`、合併到 `main`、推送對應的 `vX.Y.Z` tag。workflow 會在 tag commit 不是 `origin/main` 最新提交、版本不一致、versionCode 沒遞增、簽章憑證不符或 Release 已存在時停止。
