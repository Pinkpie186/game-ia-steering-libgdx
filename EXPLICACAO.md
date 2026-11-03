# Explicação da implementação

## 1. Agente e camadas

`Agente` guarda posição, velocidade e força como vetores bidimensionais. `maxVelocidade` limita a rapidez e `maxForca` limita a aceleração, considerando massa igual a 1. A cada passo `dt`:

```text
força = limitar(steering, maxForca)
velocidade = limitar(velocidade + força × dt, maxVelocidade)
posição = posição + velocidade × dt
```

Há três responsabilidades: `Npc` escolhe a ação com a FSM, `Steering` calcula a direção e `Agente.mover` integra o movimento. A interface desenha o resultado sem decidir a IA.

Os métodos de `Vector2` alteram o próprio objeto. Por isso, os comportamentos criam cópias antes de subtrair posições ou modificar a velocidade desejada. O passo fixo de 1/120 s permite atualizar a simulação com estabilidade mesmo quando a renderização varia.

## 2. Seek e arrive

Seek calcula uma velocidade desejada apontando para o alvo:

```text
direção = normalizar(alvo − posição)
velocidadeDesejada = direção × maxVelocidade
steering = (velocidadeDesejada − velocidade) / tempoResposta
```

O tempo de resposta é 0,25 s. Ele converte a diferença de velocidade em aceleração; a força final passa pelo limite `maxForca`.

Arrive utiliza a mesma direção, mas reduz a rapidez dentro de um raio de 150 pixels:

```text
rapidezDesejada = maxVelocidade × mínimo(1, distância / 150)
```

Fora do raio, a aproximação funciona como seek. Dentro dele, a velocidade desejada diminui com a distância. Quando o NPC está a menos de 2 pixels e sua velocidade é menor que 3 px/s, `concluirChegada` fixa o ponto final e zera a velocidade. Isso elimina o pequeno resíduo numérico depois da frenagem. A tecla S permite observar a oscilação do seek puro e comparar com arrive.

## 3. Wander e máquina de estados

Wander projeta um círculo 95 pixels à frente e escolhe um ponto em sua borda, de raio 48 pixels. Um ruído angular altera o ponto aos poucos. Assim, o NPC descreve curvas, em vez de trocar totalmente de direção a cada quadro.

A FSM usa os estados:

```text
VAGANDO -- distância ao jogador <= 230 px --> PERSEGUINDO
PERSEGUINDO -- distância ao jogador > 300 px --> VAGANDO
```

O alcance maior para perder o alvo cria histerese: o estado não fica alternando quando o jogador está perto do limite de detecção. A percepção considera uma área circular de 360 graus em uma arena sem obstáculos.

No estado `VAGANDO`, a força vem de wander. No estado `PERSEGUINDO`, vem de pursuit. O círculo na tela representa o raio inicial de visão de 230 pixels, enquanto a perda usa 300 pixels.

## 4. Pursuit

Pursuit antecipa o movimento do jogador:

```text
T = mínimo(0,8 s, distância / maxVelocidade)
posiçãoFutura = posiçãoJogador + velocidadeJogador × T
```

O NPC se dirige à posição futura. Esta implementação usa arrive no ponto previsto, com raio de 85 pixels, para evitar oscilação quando já interceptou o jogador. O círculo rosa indica a previsão. No modo interativo, WASD ou as setas alteram a velocidade real do jogador.

## 5. Bando de 30 agentes

Cada agente considera apenas os vizinhos em um raio de 120 pixels. Não há líder.

- **Separação:** para vizinhos a menos de 42 pixels, acumula vetores apontando para longe. A contribuição tem intensidade inversamente proporcional à distância.
- **Alinhamento:** orienta a velocidade para a direção média das velocidades dos vizinhos.
- **Coesão:** aplica seek ao centro médio das posições dos vizinhos.

A soma das forças usa pesos ajustáveis:

```text
força = separação × 1,7 + alinhamento × 1,0 + coesão × 0,8
```

Os pesos podem variar de 0 a 5. Alterá-los muda a contribuição relativa de cada regra. A soma final continua limitada por `maxForca`, o que impede acelerações arbitrariamente grandes.

O cálculo ocorre em duas fases: primeiro calcula todas as forças, depois move todos os agentes. Assim, um agente não lê a posição já atualizada de outro no mesmo passo.

## 6. Limites da arena e depuração

Uma força adicional desvia os agentes das bordas antes de tocá-las. Existe também uma proteção final que contém as posições e reflete a componente da velocidade em caso de ultrapassagem. Essa força é independente das três regras de bando.

O desenho usa `ShapeRenderer`. A seta azul representa a velocidade, a rosa representa a força aplicada, e a trilha indica o caminho recente do NPC. Os vetores usam escalas visuais diferentes para caberem na tela; os valores numéricos usam as unidades reais da simulação.

## 7. Validação

Os testes automatizados executam a simulação sem abrir uma janela. Eles verificam comportamento observável, incluindo a parada no ponto e a sequência de estados, além dos limites de força e velocidade. O vídeo registra a renderização OpenGL real do programa durante 1.440 quadros a 30 FPS, totalizando 48 segundos.
