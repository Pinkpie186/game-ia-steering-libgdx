package br.edu.gameia;

import com.badlogic.gdx.math.Vector2;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Simulação independente do desenho. Atualização sempre em passos de 1/120 s. */
public class Mundo {
    public enum Modo { CHEGADA, FSM, BANDO }
    public Modo modo = Modo.CHEGADA;
    public Npc npc;
    public Agente jogador;
    public final Vector2 alvo = new Vector2(870, 390);
    public boolean temAlvo;
    public boolean somenteSeek;
    public final List<Agente> agentes = new ArrayList<>();
    public float pesoSeparacao = 1.7f, pesoAlinhamento = 1.0f, pesoCoesao = 0.8f;
    private Random random;

    public Mundo() { reiniciar(); }

    public void reiniciar() {
        random = new Random(42);
        npc = new Npc(300, 360);
        npc.velocidade.set(55, 0);
        jogador = new Agente(1100, 510);
        jogador.maxVelocidade = 165f;
        temAlvo = false;
        agentes.clear();
        for (int i = 0; i < 30; i++) {
            Agente a = new Agente(400 + random.nextFloat() * 440, 260 + random.nextFloat() * 220);
            a.maxVelocidade = 120;
            a.maxForca = 230;
            a.velocidade.set(90, 0).setAngleRad(random.nextFloat() * 6.283185f);
            agentes.add(a);
        }
    }

    public void definirModo(Modo modo) { this.modo = modo; reiniciar(); }

    public void clicar(float x, float y) {
        alvo.set(Math.max(55, Math.min(1225, x)), Math.max(155, Math.min(605, y)));
        temAlvo = true;
    }

    public void atualizar(float dt) {
        switch (modo) {
            case CHEGADA -> {
                Vector2 f = temAlvo ? (somenteSeek ? Steering.seek(npc, alvo)
                        : Steering.arrive(npc, alvo, 150)) : Steering.wander(npc, random, dt).add(Steering.bordas(npc).scl(2));
                npc.mover(f, dt);
                if (temAlvo && !somenteSeek) npc.concluirChegada(alvo);
                conter(npc);
            }
            case FSM -> {
                jogador.posicao.mulAdd(jogador.velocidade, dt);
                conter(jogador);
                npc.mover(npc.decidir(jogador, random, dt), dt);
                conter(npc);
            }
            case BANDO -> {
                // Duas fases: todas as forças leem o MESMO estado dos vizinhos.
                List<Vector2> forcas = new ArrayList<>();
                for (Agente a : agentes) forcas.add(Steering.bando(a, agentes,
                        pesoSeparacao, pesoAlinhamento, pesoCoesao).add(Steering.bordas(a).scl(3f)));
                for (int i = 0; i < agentes.size(); i++) {
                    Agente a = agentes.get(i);
                    a.mover(forcas.get(i), dt);
                    conter(a);
                }
            }
        }
    }

    private void conter(Agente a) {
        if (a.posicao.x < 55) { a.posicao.x = 55; a.velocidade.x = Math.abs(a.velocidade.x); }
        if (a.posicao.x > 1225) { a.posicao.x = 1225; a.velocidade.x = -Math.abs(a.velocidade.x); }
        if (a.posicao.y < 155) { a.posicao.y = 155; a.velocidade.y = Math.abs(a.velocidade.y); }
        if (a.posicao.y > 605) { a.posicao.y = 605; a.velocidade.y = -Math.abs(a.velocidade.y); }
    }
}
