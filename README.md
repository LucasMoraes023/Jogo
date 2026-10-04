# MAGO SUPREMO — Android Java

Projeto inicial Android nativo em Java, com layouts XML e SQLite, pensado para funcionar offline.

## Estado real desta versão

**Base de desenvolvimento criada; compilação ainda não verificada neste ambiente.** Este repositório foi montado sem um Android SDK/Gradle previamente instalado. O APK não acompanha este ZIP porque não foi possível compilá-lo aqui. O workflow do GitHub Actions está configurado para compilar e publicar `app-debug.apk` quando executado num runner com acesso à internet.

### Implementado nesta base
- Aplicativo Android Java com tela principal em XML e identidade visual escura/dourada.
- SQLite local com contas, IDs reservados, coleção, cartas, moedas, cristais, missões, configurações e histórico.
- 100 cartas iniciais geradas por dados de carta com IDs permanentes e atributos próprios.
- Cadastro de nickname com validação e atribuição de ID entre 000000 e 999999; IDs usados são preservados após exclusão (não existe função de exclusão na interface inicial).
- Conta ativa local, carta inicial, abertura de pacote por 100 moedas e coleção.
- Batalha rápida local e classe de regras de combate separada da interface.
- Configuração de senha de administrador na primeira utilização e autenticação local; comandos básicos de consulta/concessão.
- Testes unitários de regras de combate e catálogo.
- Workflow GitHub Actions para testes, `assembleDebug` e upload do APK.

### Ainda incompleto — não considerar pronto
- O painel de administração cobre apenas parte dos comandos solicitados. Comandos de título, reset, capítulos, missões, backup/restauração validada e ajustes completos de atributos ainda precisam ser implementados.
- As missões são uma tela inicial informativa, ainda sem progressão completa persistida.
- Pacotes usam seleção aleatória uniforme; as probabilidades por raridade ainda precisam de implementação e exibição.
- Ainda não há editor de deck, combate com habilidades/efeitos completos, chefes, campanha, loja/evolução completas ou artes individuais finais das cartas.
- O armazenamento de credencial administrativa usa hash SHA-256 com salt nesta base; antes de uma versão pública, recomenda-se migrar para PBKDF2/Android Keystore e acrescentar limitação de tentativas.
- O fluxo de backup/restauração validado ainda não está implementado.
- Não há assinatura de release. A chave privada de lançamento nunca deve ser commitada.

## Requisitos
- JDK 17
- Android SDK Platform 35 e Build Tools 35.0.0
- Gradle 8.9 (o script `gradlew` deste projeto baixa a distribuição oficial na primeira execução; requer conexão com a internet)

## Compilar
```bash
./gradlew test
./gradlew assembleDebug
```
APK esperado: `app/build/outputs/apk/debug/app-debug.apk`.

## GitHub Actions
Faça push para `main`/`master` ou execute manualmente a ação **Android APK** na aba Actions. Após sucesso, baixe o artefato `mago-supremo-debug-apk`.

## Segurança e escopo
As contas e IDs são únicos somente no banco local deste aparelho, não globalmente entre dispositivos. A senha ADMIN MASTER é configurada na primeira execução e não existe credencial padrão. Esta é uma base inicial de desenvolvimento, não um jogo comercial completo.

## Créditos
Criado por Lucas Moraes
