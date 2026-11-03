package br.edu.gameia;

import com.badlogic.gdx.math.Vector2;
import java.util.List;
import java.util.Random;

/** Camada de direção. Todos os métodos devolvem forças e preservam seus argumentos. */
public final class Steering {
    private Steering() {}
    private static final float TEMPO_RESPOSTA = 0.25f;

    private static Vector2 ajustar(Agente a, Vector2 desejada) {
        return desejada.sub(a.velocidade).scl(1f / TEMPO_RESPOSTA).limit(a.maxForca);
    }

    public static Vector2 seek(Agente a, Vector2 alvo) {
        return ajustar(a, new Vector2(alvo).sub(a.posicao).nor().scl(a.maxVelocidade));
    }

    public static Vector2 arrive(Agente a, Vector2 alvo, float raio) {
        Vector2 desejada = new Vector2(alvo).sub(a.posicao);
        float distancia = desejada.len();
        float rapidez = a.maxVelocidade * Math.min(1f, distancia / raio);
        return ajustar(a, desejada.nor().scl(rapidez));
    }

    public static Vector2 previsao(Agente a, Agente alvo) {
        float tempo = Math.min(0.8f, a.posicao.dst(alvo.posicao) / a.maxVelocidade);
        return new Vector2(alvo.posicao).mulAdd(alvo.velocidade, tempo);
    }

    public static Vector2 pursuit(Agente a, Agente alvo) {
        // Seek na posição FUTURA, com desaceleração na interceptação para evitar oscilação.
        return arrive(a, previsao(a, alvo), 85f);
    }

    public static Vector2 wander(Agente a, Random random, float delta) {
        // Ruído angular a passos fixos: não se sorteia uma direção inteira por frame.
        a.anguloWander += (random.nextFloat() * 2f - 1f) * 2.4f * (float)Math.sqrt(delta);
        Vector2 frente = new Vector2(a.velocidade);
        if (frente.isZero()) frente.set(1, 0).setAngleRad(a.orientacao);
        frente.nor().scl(95f);
        Vector2 borda = new Vector2(48f, 0).setAngleRad(a.anguloWander + a.orientacao);
        return seek(a, new Vector2(a.posicao).add(frente).add(borda));
    }

    /** Regras locais de Reynolds: repulsão inversa à distância, direção média e centro médio. */
    public static Vector2 bando(Agente a, List<Agente> agentes, float separacao,
                               float alinhamento, float coesao) {
        Vector2 separar = new Vector2(), alinhar = new Vector2(), centro = new Vector2();
        int vizinhos = 0, proximos = 0;
        for (Agente outro : agentes) {
            if (outro == a) continue;
            float d2 = a.posicao.dst2(outro.posicao);
            if (d2 < 120f * 120f) {
                alinhar.add(outro.velocidade);
                centro.add(outro.posicao);
                vizinhos++;
                if (d2 < 42f * 42f) {
                    if (d2 > 0.0001f) separar.add(new Vector2(a.posicao).sub(outro.posicao).scl(1f / d2));
                    else separar.add(System.identityHashCode(a) > System.identityHashCode(outro) ? 1 : -1, 0);
                    proximos++;
                }
            }
        }
        Vector2 total = new Vector2();
        if (proximos > 0) total.mulAdd(ajustar(a, separar.nor().scl(a.maxVelocidade)), separacao);
        if (vizinhos > 0) {
            total.mulAdd(ajustar(a, alinhar.scl(1f / vizinhos).nor().scl(a.maxVelocidade)), alinhamento);
            total.mulAdd(seek(a, centro.scl(1f / vizinhos)), coesao);
        }
        return total;
    }

    /** Contenção suave, antes da borda. A integração também possui uma proteção de limite. */
    public static Vector2 bordas(Agente a) {
        Vector2 desejada = new Vector2(a.velocidade);
        boolean perto = false;
        if (a.posicao.x < 100) { desejada.x = a.maxVelocidade; perto = true; }
        if (a.posicao.x > 1180) { desejada.x = -a.maxVelocidade; perto = true; }
        if (a.posicao.y < 200) { desejada.y = a.maxVelocidade; perto = true; }
        if (a.posicao.y > 560) { desejada.y = -a.maxVelocidade; perto = true; }
        return perto ? ajustar(a, desejada.nor().scl(a.maxVelocidade)) : new Vector2();
    }
}
