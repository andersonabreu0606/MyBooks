# MyBooks

Aplicação Android para gerir uma biblioteca pessoal e acompanhar livros emprestados.

## Funcionalidades

- Catálogo de livros com título, autor, ISBN e notas
- Pesquisa e filtros por disponibilidade
- Registo de empréstimos com nome, telefone e data prevista
- Identificação visual de empréstimos atrasados
- Devolução e histórico de empréstimos
- Dados guardados apenas no dispositivo, sem conta ou servidor
- Interface em português, adaptada a tema claro e escuro

## Tecnologia

- Kotlin
- Jetpack Compose + Material 3
- Android Gradle Plugin 9.3.1 e Kotlin integrado
- Persistência local com `SharedPreferences` e JSON
- `minSdk 26`, `targetSdk 37` e `compileSdk 37`

## Executar

1. Instale a versão mais recente do Android Studio e o Android SDK 37.
2. Abra esta pasta no Android Studio.
3. Aguarde a sincronização do Gradle.
4. Execute a configuração `app` num emulador ou dispositivo com Android 8.0 ou superior.

No Windows, também pode executar `gradlew.bat assembleDebug` depois de configurar o JDK 17 e o Android SDK.

## Estrutura

- `app/src/main/java/pt/mybooks/app/data`: modelos e persistência local
- `app/src/main/java/pt/mybooks/app/ui`: estado, ecrãs e componentes Compose
- `app/src/main/java/pt/mybooks/app/ui/theme`: identidade visual

Os dados não saem do dispositivo. Desinstalar a aplicação elimina o catálogo e o histórico local.
