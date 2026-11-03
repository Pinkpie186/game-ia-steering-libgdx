package br.edu.gameia;

import com.badlogic.gdx.math.Vector2;
import java.util.Random;

/** Camada de seleção de ação: FSM com histerese para não alternar na borda da visão. */
public class Npc extends Agente {
    public enum Estado { VAGANDO, PERSEGUINDO }
    public Estado estado = Estado.VAGANDO;
    public static final float RAIO_VISAO = 230f;
    public static final float RAIO_PERDA = 300f;

    public Npc(float x, float y) { super(x, y); }

    public Vector2 decidir(Agente jogador, Random random, float delta) {
        float distancia = posicao.dst(jogador.posicao);
        if (estado == Estado.VAGANDO && distancia <= RAIO_VISAO) estado = Estado.PERSEGUINDO;
        else if (estado == Estado.PERSEGUINDO && distancia > RAIO_PERDA) estado = Estado.VAGANDO;
        Vector2 forca = estado == Estado.PERSEGUINDO
                ? Steering.pursuit(this, jogador) : Steering.wander(this, random, delta);
        return forca.add(Steering.bordas(this).scl(2f));
    }
}
