# MultiplayerGame

Разработать многопользовательскую игру с чатом

1. Игрок подключается к серверу по известному адресу (ws://..., ip:port, http://... )
2. При подключении игрок выбирает себе имя.
3. После входа в игру персонаж игрока отображается на двумерной карты в виде примитива.  4. Цвет персонажа и его начальную позицию сервер выбирает таким образом, чтобы различать игроков.
5. Игрок может перемещать своего персонажа по карте с помощью стрелок.
6. Игрок может написать сообщение в общий чат.
7. Игроки, подключенные к серверу, видят сообщение рядом с персонажем отправителя, а также в общем списке сообщений.

### Client
- Unity 6.3 LTS
- C#
- TextMeshPro
- NativeWebSocket
- Unity Input System

### Server
- Kotlin 2.4.21
- Java 21 (build environment)
- Spring Boot 4.1.2
- Spring WebSocket
- Maven
- Jackson

### Testing
- JUnit
- Spring Boot Test
- GitHub Actions

GitHub: https://github.com/evgeniibutorin/MultiplayerGame