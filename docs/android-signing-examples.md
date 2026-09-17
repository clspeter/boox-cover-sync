# 公開 Android 專案的 GitHub Actions 簽章案例

以下只引用公開 repository 的 workflow 與文件，目的是確認「把 release keystore／簽章資料交給 GitHub Actions」是實際採用的做法。公開內容不包含任何實際 secret 值。

## Mihon

Mihon 是大型開源 Android 漫畫閱讀器。它的 release workflow 在推送 `v*` tag 時執行，並把下列 secrets 以 Gradle 環境變數提供給 `assembleRelease` 與 `assembleFoss`：

- `SIGNING_KEY`：Base64 keystore
- `KEY_STORE_PASSWORD`：keystore 密碼
- `ALIAS`：key alias
- `KEY_PASSWORD`：key 密碼

同一個 workflow 會把 APK 上傳成 artifact，接著建立 GitHub Draft Release。Workflow 內沒有宣告 `environment:`，因此僅依公開原始碼無法確定這些 secrets 是 repository scope 或 organization scope；它沒有證據顯示使用 GitHub Environment scope。

來源（固定 commit）：

- [Mihon release workflow @ a7179805](https://github.com/mihonapp/mihon/blob/a7179805ad1e7b3c3a0a4098560d04fed24b2701/.github/workflows/release.yml)

這個案例適合參考我們的基本欄位命名與「tag → 編譯 → Draft Release」流程，但我們仍可另外加上 `release` environment、tag 保護與 APK 簽章驗證。

## Organic Maps

Organic Maps 的公開 credentials 文件明確使用 GitHub Environment scope：

- `RELEASE_KEYSTORE`：Android Java keystore 的 Base64 內容
- `SECURE_PROPERTIES`：包含 `release.keystore` 密碼的 Android Gradle 設定檔

文件示範將兩者分別寫入 `beta` 與 `production` environment。對應 workflow 的 job 也宣告 `environment: beta` 或 `environment: production`，再把 secrets 解碼成 runner 上的暫存檔；Android beta workflow 產生測試發布物，Android release workflow 使用 production secrets 產生正式發布物。這表示 beta 與 production 可以使用不同的簽章資料與權限規則。

來源（固定 commit）：

- [Organic Maps credentials 文件 @ e00f33a](https://github.com/organicmaps/organicmaps/blob/e00f33a0765ce601feabade0c09972753671a631/docs/CREDENTIALS.md)
- [Organic Maps Android beta workflow @ e00f33a](https://github.com/organicmaps/organicmaps/blob/e00f33a0765ce601feabade0c09972753671a/.github/workflows/android-beta.yaml)
- [Organic Maps Android release workflow @ e00f33a](https://github.com/organicmaps/organicmaps/blob/e00f33a0765ce601feabade0c09972753671a/.github/workflows/android-release.yaml)

這個案例最接近我們要採用的方式：建立專用 `release` environment，將 keystore 與密碼分開保存，只讓發布 job 使用；Base64 只適合作為檔案傳輸格式，不能當成加密。

## Home Assistant Android

公開的 `onPush.yml` 在 GitHub／Firebase 發布 job 使用 `ORIGINAL_KEYSTORE_FILE`、`ORIGINAL_KEYSTORE_FILE_PASSWORD`、`ORIGINAL_KEYSTORE_ALIAS` 與 `ORIGINAL_KEYSTORE_ALIAS_PASSWORD`。本機 composite action 將 Base64 keystore 還原到各 Android 模組，接著編譯 Release APK。Play 發布 job 則使用另一組 `UPLOAD_KEYSTORE_*`，不可把 Play upload key 與直接分發 APK 的簽章金鑰混為一談。

這兩個 job 沒有宣告 `environment:`，因此不是 Environment Secrets 的直接證據；repository／organization 層級無法從公開 workflow 區分。

- [Home Assistant 發布 workflow（固定 commit）](https://github.com/home-assistant/android/blob/cfcfc26fe176877eac49211b04c571b846b2f17e/.github/workflows/onPush.yml)
- [Home Assistant 還原 keystore action（同一 commit）](https://github.com/home-assistant/android/blob/cfcfc26fe176877eac49211b04c571b846b2f17e/.github/actions/inflate-secrets/action.yml)

## 對本專案的可採用結論

1. Base64 keystore、keystore 密碼、key alias 與 key 密碼分開保存，是公開專案實際使用的模式；alias 是名稱，不是第三個密碼。
2. 若要限制正式簽章，應仿 Organic Maps 宣告 job 的 `environment: release`；只把 secret 放入 GitHub 而沒有在 job 綁定 environment，不會產生同樣的環境保護效果。
3. 發布 workflow 應固定使用 tag、驗證 APK 簽章憑證指紋，並把 keystore 暫存檔放在 runner temporary directory，工作結束時清除。

GitHub 對 repository、environment 與 organization secrets 的官方說明：[Using secrets in GitHub Actions](https://docs.github.com/en/actions/how-tos/write-workflows/choose-what-workflows-do/use-secrets)。
