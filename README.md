# Game Space Nova

Aplicativo Android novo e independente. Não deriva de nenhum projeto anterior.

## Compilar no GitHub
1. Crie um repositório vazio.
2. Envie **o conteúdo desta pasta** para a raiz do repositório (incluindo `.github/workflows/build-apk.yml`).
3. Abra **Actions → Build Game Space Nova APK → Run workflow**.
4. Baixe o artefato `GameSpaceNova-debug-apk`.

Requisitos do workflow: JDK 17 e Gradle Wrapper gerado pelo runner? O workflow usa a instalação Gradle disponibilizada por `setup-gradle`; `gradle assembleDebug` resolve as versões dos plugins.

## Incluído nesta primeira implementação
- Interface Compose com quatro áreas obrigatórias, navegação inferior e tema AMOLED.
- Biblioteca que lista aplicativos lançáveis, permite selecionar os que aparecem e abrir cada um.
- Editor de perfil por aplicativo (resolução, limpeza e taxa de atualização desejada).
- Modos de limpeza e lista de pacotes protegidos; o Android pode limitar o que um app comum consegue encerrar.
- Tradutor baseado em MediaProjection + ML Kit OCR + ML Kit Translate (inglês → português), botão sobreposto e gestos.
- Preferências visuais básicas e atalhos para permissões do Android.

## Limitações importantes da v0.1
- O modelo de tradução inglês→português é baixado sob demanda pelo ML Kit. Ele não está pré-embutido no APK: isso evita entregar um APK muito grande e permite que o SDK gerencie o modelo offline depois do download.
- A seleção de região é uma etapa ainda incompleta nesta primeira compilação: o gesto para a direita chama o fluxo regional, mas a interface de seleção por retângulo ainda precisa ser terminada.
- Os perfis guardam os valores, mas executar `wm size`, `wm density` e comandos privilegiados requer integração autorizada com Shizuku/ADB. A v0.1 não aplica esses comandos automaticamente.
- Android não permite a um aplicativo comum impor FPS internos ao jogo. O perfil guarda Hz desejados, que não equivalem a FPS garantidos.
- A lista de apps usa `QUERY_ALL_PACKAGES`; a política de publicação da Play Store pode limitar essa permissão. Para testes por APK/GitHub, ela é solicitada para suportar uma biblioteca personalizável.

O primeiro build deve ser considerado um protótipo funcional inicial e precisa de testes em aparelho real. A compilação não garante a operação correta de MediaProjection, gestos, permissões ou limpeza em todas as versões de Android.
