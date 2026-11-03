package br.edu.gameia;

import com.badlogic.gdx.math.Vector2;

/** Camada de locomoção: integra força, velocidade e posição. Massa unitária. */
public class Agente {
    public final Vector2 posicao = new Vector2();
    public final Vector2 velocidade = new Vector2();
    public final Vector2 forca = new Vector2();
    public float maxVelocidade = 220f; // pixels/s: limite do módulo da velocidade
    public float maxForca = 420f;      // pixels/s²: limite do módulo da força (massa = 1)
    public float orientacao;
    public float anguloWander;

    public Agente(float x, float y) { posicao.set(x, y); }

    public void mover(Vector2 steering, float delta) {
        if (delta <= 0) return;
        forca.set(steering).limit(maxForca); // não modifica o vetor do chamador
        velocidade.mulAdd(forca, delta).limit(maxVelocidade);
        posicao.mulAdd(velocidade, delta);
        if (velocidade.len2() > 1f) orientacao = velocidade.angleRad();
    }

    /** Tolerância final aplicada somente quando a desaceleração já terminou. */
    public boolean concluirChegada(Vector2 alvo) {
        if (posicao.dst2(alvo) < 4f && velocidade.len2() < 9f) {
            posicao.set(alvo);
            velocidade.setZero();
            forca.setZero();
            return true;
        }
        return false;
    }
}
