# Atividade prática: agentes autônomos em libGDX

Implementação em Java 17 e libGDX 1.14.0 baseada na atividade e nos conceitos do material `game_IA_atualizado.pptx`. Inclui todos os itens obrigatórios e o bônus de bando.

## Executar

Para experimentar sem configurar Java, use a entrega separada `Aplicativo-Windows.zip`: extraia a pasta inteira e abra `Iniciar.bat`. Esse pacote inclui um runtime Java para Windows x64.

Para desenvolver este projeto, instale um **JDK 17 ou superior**, configure `JAVA_HOME` e execute na raiz:

```powershell
.\gradlew.bat lwjgl3:run
```

No Linux/macOS:

```sh
sh gradlew lwjgl3:run
```

O Gradle Wrapper baixa a versão correta do Gradle e as dependências na primeira execução. É necessário acesso à internet nessa etapa. Também é possível abrir a pasta no IntelliJ IDEA como projeto Gradle.

## Controles

| Controle | Ação |
| --- | --- |
| 1 / 2 / 3 ou clique na aba | Alternar entre chegada, FSM e bando |
| Clique na arena, modo 1 | Definir um alvo para o NPC |
| Espaço, modo 1 | Remover o alvo e voltar ao wander |
| S, modo 1 | Comparar seek puro com seek + arrive |
| WASD ou setas, modo 2 | Mover o jogador azul |
| Clique na arena, modo 2 | Reposicionar o jogador |
| Q / A, modo 3 | Aumentar / diminuir o peso da separação |
| W / S, modo 3 | Aumentar / diminuir o peso do alinhamento |
| E / D, modo 3 | Aumentar / diminuir o peso da coesão |
| V | Exibir / ocultar os vetores de depuração |
| P | Pausar / retomar |
| R | Reiniciar as posições do modo atual |
| F5 | Ativar / desativar a demonstração automática de 48 segundos |
| Esc | Sair |

## Onde cada requisito foi implementado

| Requisito | Implementação |
| --- | --- |
| Modelagem do agente | `Agente.java`: posição, velocidade e força em `Vector2`; limites escalares `maxVelocidade` e `maxForca` |
| Seek e arrive ao clique | `Steering.seek`, `Steering.arrive`, `Mundo.clicar` e modo `CHEGADA` |
| Wander sem alvo | `Steering.wander`; círculo projetado à frente e mudança gradual do ângulo |
| FSM e pursuit | `Npc.decidir`, estados `VAGANDO` / `PERSEGUINDO`, previsão de posição do jogador |
| Bônus | `Steering.bando` e modo `BANDO`: 30 agentes, regras locais e pesos ajustáveis |
| Depuração | `SteeringApp`: `ShapeRenderer`, velocidade azul, força rosa e métricas numéricas |

Não há dependência de `gdx-ai`: os comportamentos e a FSM foram implementados diretamente para tornar as fórmulas da aula visíveis no código. `maxVelocidade` e `maxForca` são números porque limitam o módulo dos vetores.

## Estrutura

```text
core/src/main/java/br/edu/gameia/
  Agente.java        integração e limites de movimento
  Steering.java      cálculo das forças
  Npc.java           máquina de estados
  Mundo.java         simulação e composição dos comportamentos
  SteeringApp.java   desenho, entrada e demonstração
core/src/test/java/br/edu/gameia/
  ComportamentosTest.java
lwjgl3/src/main/java/br/edu/gameia/desktop/
  DesktopLauncher.java
assets/fonts/        fontes bitmap e licença OFL
EXPLICACAO.md        fórmulas e decisões de implementação
gravar-video.ps1     reprodução opcional da captura
```

## Testar e gerar o JAR

```powershell
.\gradlew.bat test lwjgl3:dist
java -jar .\lwjgl3\build\libs\atividade-steering.jar
```

No macOS, ao executar o JAR diretamente, acrescente `-XstartOnFirstThread` antes de `-jar`.

Os cinco testes verificam a chegada com parada, as transições da FSM, a antecipação do pursuit, as direções das três forças do bando e a estabilidade de 30 agentes durante 60 segundos simulados. A simulação usa passo fixo de 1/120 s.

## Vídeo entregue

`Demonstracao-48s.mp4`: captura da própria aplicação, em 1280 × 800, 30 FPS, com legendas na tela e sem áudio.

| Trecho | Comportamento |
| --- | --- |
| 00:00–00:14 | Aproximação ao alvo, desaceleração, parada e mudança de alvo |
| 00:14–00:18 | Wander sem jogador no alcance |
| 00:18–00:26 | Detecção e pursuit do jogador com posição futura indicada |
| 00:26–00:32 | Perda do jogador e retorno ao wander |
| 00:32–00:40 | Bando de 30 agentes com as três forças |
| 00:40–00:48 | Alteração dos pesos para espalhar o bando |

Os eventos do vídeo são programados pelo método `roteiro`, mas o movimento usa as mesmas classes, forças e integração do modo interativo.

## Publicar no seu repositório

Esta pasta contém um repositório Git local com o código inicial versionado. Nenhum repositório remoto foi criado, pois não foi fornecido um destino de publicação. Depois de criar um repositório vazio na sua conta, na raiz desta pasta execute:

```sh
git remote add origin URL_DO_SEU_REPOSITORIO
git push -u origin main
```

Anexe também o vídeo à entrega ou disponibilize seu link conforme a orientação da disciplina. Revise o código e a explicação para conseguir apresentar como os comportamentos funcionam.

## Referências e recursos

- Material teórico fornecido: `game_IA_atualizado.pptx`, aula de Game IA, Prof. Dr. Leandro Pupo Natale.
- Documentação oficial para importar e executar projetos libGDX: https://libgdx.com/wiki/start/import-and-running
- Código oficial do backend desktop: https://github.com/libgdx/libgdx/tree/1.14.0/backends/gdx-backend-lwjgl3
- Tipografia IBM Plex Sans, sob SIL Open Font License 1.1, incluída em `assets/fonts/OFL.txt`: https://github.com/google/fonts/tree/main/ofl/ibmplexsans
