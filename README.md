<div align="center">

# 🛡️ RtpTun (Open Source Edition)

**Открытое высокопроизводительное FOSS-решение для защиты сетевого стека, аудита и безопасной передачи данных на Android**

[![Лицензия](https://img.shields.io/badge/Лицензия-GNU%20GPL%20v3.0-blue?style=for-the-badge&logo=gnu)](LICENSE)
[![Исходный_код](https://img.shields.io/badge/Код-100%25%20Open%20Source-00FF88?style=for-the-badge&logo=github)](https://github.com/)
[![База WDTT](https://img.shields.io/badge/Base-WDTT%20by%20amurcanov-orange?style=for-the-badge&logo=github)](https://github.com/amurcanov/proxy-turn-vk-android)
[![Форк qWDTT](https://img.shields.io/badge/Fork-qWDTT%20by%20SpaceNeuroX-purple?style=for-the-badge&logo=github)](https://github.com/SpaceNeuroX/proxy-turn-vk-android)
[![Платформа](https://img.shields.io/badge/Android-8.0%20(API%2026)%20--%2015-white?style=for-the-badge&logo=android)](../../releases)
[![Рендеринг](https://img.shields.io/badge/UI-Jetpack%20Compose%20(120%20FPS)-purple?style=for-the-badge&logo=jetpackcompose)](../../#)

</div>

---

> [!WARNING]
> ### ⚠️ ВАЖНОЕ ПРЕДУПРЕЖДЕНИЕ И ПРАВИЛА (DISCLAIMER)
> 1. **Назначение:** Проект **RtpTun** является открытым программным обеспечением (Open Source) под лицензией **GNU GPL v3** и предназначен исключительно для аудита безопасности, защиты приватности персональных данных и передачи трафика в открытых сетях.
> 2. **Правомерность:** Продукт **НЕ СОЗДАВАЛСЯ** и **НЕ ПРЕДНАЗНАЧЕН** для совершения противоправных действий, нарушения авторских прав или несанкционированного доступа.
> 3. **Ответственность:** Авторы оригинальных баз, форков и текущие разработчики **НЕ НЕСУТ** никакой ответственности за сценарии использования ПО конечными пользователями или администраторами сторонних узлов.
> 4. **Условия:** Вся ответственность за соблюдение законодательства своей юрисдикции и правил провайдеров лежит исключительно на пользователе. Программа поставляется **«КАК ЕСТЬ» (AS IS)** без каких-либо явных или подразумеваемых гарантий.

---

## 💎 О проекте и источниках (Credits & Upstream)

**RtpTun** — это полностью открытый клиент сетевой инкапсуляции и транспортный фреймворк для Android, разработанный на базе открытых наработок сетевого сообщества.

### 👥 Авторы и база проекта:
* 📄 **Лицензия:** GNU General Public License v3.0 (GPL-3.0)
* 👨‍💻 **Автор оригинальной базы WDTT:** [amurcanov](https://github.com/amurcanov) (`proxy-turn-vk-android`) — [github.com/amurcanov/proxy-turn-vk-android](https://github.com/amurcanov/proxy-turn-vk-android)
* 🚀 **Разработка и форк qWDTT:** [SpaceNeuroX](https://github.com/SpaceNeuroX) (`proxy-turn-vk-android`) — [github.com/SpaceNeuroX/proxy-turn-vk-android](https://github.com/SpaceNeuroX/proxy-turn-vk-android)

> 💡 *В приложении доступно диалоговое окно сведений: при нажатии на заголовок **«RTpTUN»** вверху главного экрана открывается всплывающее меню «О авторах и проекте» с полными ссылками на репозитории создателей.*

---

## ⚡ Что нового в релизе v1.3.0

* ☁️ **Open Subscription Engine:**
  * Поддержка защищённых динамических ссылок подписки (`/sub/<token>`).
  * Разделение узлов: облачные серверы получают статус `☁️ ОБЛАКО`, пользовательские — `📋 СВОЙ`.
  * Бережное обновление: автоочистка только устаревших серверов из подписки без затрагивания ручных профилей.
  * Настраиваемый планировщик фоновой синхронизации (6 / 12 / 24 ч) через Android `WorkManager`.
* 🔄 **Watchdog & Guaranteed Interface Auto-Reconnect:**
  * Устойчивый цикл поднятия виртуального интерфейса Android (`tun0`) с экспоненциальным backoff до подтверждения статуса `Tunnel.State.UP`.
  * Фоновый `Watchdog`, мгновенно восстанавливающий туннель при смене сети (Wi-Fi ➔ LTE) или системном сбросе сокета.
* 🎬 **GPU Splash Screen & Vector Speedometer:**
  * Плавная видео-заставка на `TextureView` с сохранением оригинальных пропорций (`Matrix Fit-Center`).
  * 7-сегментный векторный спидометр задержки с динамической шкалой пинга.
* 🧭 **Dynamic Layout & Barrier Protection:**
  * Автоматический расчёт отступов контента (`78.dp`) при боковой вертикальной навигации.
  * Встроенный барьер сетевого состояния (предотвращает запуск сервиса при отключённых сетевых модулях).

---

## 🔬 Архитектура и сетевой стек

┌─────────────────────────────────────────────────────────────────────────────┐
│ ANDROID OPEN-SOURCE UI LAYER │ │ [ Kotlin Jetpack Compose (120 FPS) ] ◄──► [
Jetpack DataStore Settings ] │
└──────────────────────────────────────┬──────────────────────────────────────┘
│ JNI / VpnService API ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ SYSTEM VPN & WIREGUARD BACKEND │ │ [ Android VpnService (tun0) ] ◄───► [
WireGuard GoBackend ] │
└──────────────────────────────────────┬──────────────────────────────────────┘
│ Local Loopback (127.0.0.1:9000) ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ RTPTUN GO CORE (WDTT / qWDTT BASE) │ │
┌───────────────────────────────────────────────────────────────┐ │ │ │
Dual-Layer AEAD Encryption Engine │ │ │ │ (HKDF Session Derivation + ChaCha20) │
│ │ └───────────────────────────────┬───────────────────────────────┘ │ │ │ │ │
┌───────────────────────────────┴───────────────────────────────┐ │ │ │ WebRTC /
RTP Stream Mimicry │ │ │ │ (Инкапсуляция под голосовой трафик) │ │ │
└───────────────────────────────┬───────────────────────────────┘ │
└──────────────────────────────────────┼──────────────────────────────────────┘
│ UDP / DTLS Transport ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ UPSTREAM ENDPOINTS │ │ [ Direct Peers ] ◄──────► [ Relay Cluster ] │
└─────────────────────────────────────────────────────────────────────────────┘


---

## 🛠️ Сборка из исходного кода

### Требования
* **JDK:** OpenJDK 17+
* **Android SDK:** API Level 35 (Build-tools 35.0.0)
* **Android NDK:** 26.1+
* **Go Compiler:** 1.22+

### Пошаговая сборка

```bash
# 1. Клонирование репозитория
git clone --recursive https://github.com/your-username/rtptun-android.git
cd rtptun-android

# 2. Сборка нативного Go-модуля
cd core
make build-android
cd ..

# 3. Сборка APK
./gradlew assembleRelease

Готовый установочный пакет: app/build/outputs/apk/release/

📋 Формат конфигурации и схемы импорта

Структура конфигурационной строки

host|port|password|hashes|name|country|workers|expire_unix

| Поле          | Тип    | Описание                 | Пример             |
| ------------- | ------ | ------------------------ | ------------------ |
| `host`        | String | IP-адрес или домен узла  | `154.86.119.116`   |
| `port`        | String | Порт сервиса             | `56000`            |
| `password`    | String | Ключ аутентификации      | `open_secret_2026` |
| `hashes`      | String | Сессионные хэши/токены   | `nki-wqEB9x...`    |
| `name`        | String | Отображаемое имя         | `Германия [DE]`    |
| `country`     | String | ISO-код страны           | `DE`               |
| `workers`     | Int    | Количество потоков       | `18`               |
| `expire_unix` | Long   | Timestamp срока действия | `1787491200`       |

Поддерживаемые форматы ссылок (URI-схемы):

  - wdtt://<HOST>:<PORT>:0:0:<PASS>:<HASHES>#<NAME>
  - ptvb://config?name=<NAME>&peer=<HOST>:<PORT>&hashes=<HASHES>&workers=<N>&port=9000&pass=<PASS>
  - rtptun://import?data=<BASE64_ENCRYPTED_BLOB>

🔒 Лицензия и условия использования

Проект RtpTun лицензирован в соответствии с GNU General Public License v3.0
(GPLv3):

  - Вы можете свободно изучать, изменять и распространять исходный код.
  - Любые производные работы обязаны распространяться под аналогичной лицензией
    GPLv3 с сохранением авторских прав оригинальных создателей (amurcanov,
    SpaceNeuroX).
  - Проект придерживается строгой политики отсутствия логов (No-Logs Policy):
    сетевая активность не сохраняется и не передаётся на внешние серверы.

RtpTun Open Source Community · 2026
Основано на открытых разработках WDTT & qWDTT
