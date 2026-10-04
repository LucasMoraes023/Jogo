# MAGO SUPREMO

Jogo offline de cartas de fantasia sombria para Android, implementado em Java
nativo com Android Views/XML, Gradle e SQLite. O aplicativo não solicita
permissões de rede e guarda contas, coleção, moedas, cristais e progresso no
dispositivo.

## Compilar

Requisitos: JDK 17 e Android SDK com API 35 e Build Tools 35.0.0.

```bash
./gradlew testDebugUnitTest assembleDebug
```

O APK de depuração é gerado em
`app/build/outputs/apk/debug/app-debug.apk`. O workflow Android APK executa os
testes e publica esse arquivo como artefato `app-debug.apk`.

## Começar a jogar

Crie uma conta pela tela inicial. O jogo gera um ID exclusivo de seis dígitos
entre `000001` e `999999`; `000000` é reservado para a conta ADMIN MASTER.
Contas novas começam com moedas, cristais e cartas iniciais.

A conta administrativa inicial é:

- ID: `000000`
- Senha: `Arcano#000000`

Troque a senha administrativa no código antes de distribuir uma compilação
personalizada. O painel ADMIN permite consultar contas recentes e conceder
moedas. Esta é uma conta mestre local para uso offline, não um serviço de
autenticação remoto.

## Sistemas implementados

- Coleção local de 144 cartas em nove raridades.
- Pacotes com moedas ou cristais, câmbio na loja e cartas duplicadas.
- Evolução de cartas consumindo duplicatas.
- Combate por turnos em campanha e arena, com recompensas persistidas.
- Missões resgatáveis, conquistas, experiência e progresso de campanha.
- Painel administrativo e aba Sobre.

Os testes unitários básicos cobrem as raridades e os limites dos IDs de conta.
