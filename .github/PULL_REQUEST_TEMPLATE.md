## Resumo & Issue Relacionada

<!-- Descreva de forma objetiva o problema resolvido ou a funcionalidade entregue por este Pull Request. -->

- **Issue Relacionada:** Closes #
- **Especificação / Task (se aplicável):** `SPEC-___` / `TASK-___`

---

## Mudanças por Camada

<!-- Resuma as alterações realizadas em cada camada da Clean Architecture (remova ou marque N/A nas camadas não tocadas). -->

- **`domain`** (`RuleEvaluator`, modelos de domínio, RBAC `AppPermission`):
  - 
- **`data`** (`SoundModeRepository`, `SoundPreferencesDataSource` / DataStore / Backup JSON, `SoundSystemController`):
  - 
- **`system/tile/widget`** (`InternalIntentSecurity`, `SystemAutomationReceiver`, `ExactRuleAlarmScheduler`, `BackgroundGeofenceManager`, `SmartNotificationManager`, `SystemZenRuleManager`, `SoundModeTileService`, `SoundModeGlanceWidget`, `RestoreSoundModeWorker`):
  - 
- **`ui`** (Jetpack Compose / Design System **ZENITH**, `MainScreen`, `MainScreenViewModel`, `AppLabels.kt`, `AppTestTags.kt`):
  - 
- **`tests`** (Unitários em `src/test/` e Instrumentados UI/E2E em `src/androidTest/`):
  - 

---

## Checklist de Qualidade & Segurança Obrigatório

- [ ] **Build, Testes Unitários & Lint:** `./gradlew testDebugUnitTest lintRelease assembleDebug` 100% verde sem regressões.
- [ ] **Testes Instrumentados (UI & E2E):** Suíte instrumentada (`MainScreenTest` & `ManageSoundModeE2ETest`) validada quando há mudança de UI, navegação ou fluxo de usuário.
- [ ] **Internacionalização Bilíngue (`i18n`):** Todas as strings visíveis sincronizadas em `AppLabels.kt` (`PT-BR` e `EN`) + `res/values/strings.xml` / `res/values-en/strings.xml` quando exigido por recursos Android.
- [ ] **Automação & Testabilidade:** Todos os novos elementos interativos ou verificáveis expõem seletores centralizados em `AppTestTags.kt`.
- [ ] **Diretriz Visual ZENITH:** Zero emojis na UI nativa, RemoteViews, Widgets Glance, Quick Settings Tiles ou Notificações (uso exclusivo de ícones vetoriais Material oficiais `Icons.*` / `R.drawable.ic_*`).
- [ ] **Segurança, RBAC & Retrocompatibilidade:**
  - Todas as `PendingIntent`s internas destinadas ao `SystemAutomationReceiver` ou à `MainActivity` são assinadas via `InternalIntentSecurity.signIntent(context, intent)`.
  - Respeito integral às permissões do controle de acesso interno (`AccessControlConfig` / `AppPermission`).
  - Migrações de `DataStore` e exportação/importação de Backup JSON (`SoundPreferencesDataSource.kt`) 100% retrocompatíveis com payloads anteriores e sem exposição de PIN/segredos.
  - Regras R8/ProGuard (`app/proguard-rules.pro`) preservadas/atualizadas para novas classes `@Serializable` ou carregadas via reflexão, e `scripts/git-pre-commit-guard.sh` executado sem alertas.
