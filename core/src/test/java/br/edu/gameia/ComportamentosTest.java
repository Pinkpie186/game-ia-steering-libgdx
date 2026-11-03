package br.edu.gameia;

import com.badlogic.gdx.math.Vector2;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;

class ComportamentosTest {
    @Test void chegaSemOscilarEPreservaAlvo() {
        Agente a = new Agente(100, 300);
        Vector2 alvo = new Vector2(800, 400), copia = alvo.cpy();
        for (int i = 0; i < 1440; i++) {
            a.mover(Steering.arrive(a, alvo, 150), 1f / 120);
            a.concluirChegada(alvo);
            assertTrue(a.velocidade.len() <= a.maxVelocidade + .001f);
            assertTrue(a.forca.len() <= a.maxForca + .001f);
        }
        assertEquals(copia, alvo);
        assertEquals(alvo, a.posicao);
        assertEquals(0, a.velocidade.len());
    }

    @Test void fsmDetectaPersegueEVoltaAVagarComHisterese() {
        Npc npc = new Npc(300, 360);
        Agente jogador = new Agente(900, 360);
        Random random = new Random(42);
        npc.decidir(jogador, random, 1f / 120);
        assertEquals(Npc.Estado.VAGANDO, npc.estado);
        jogador.posicao.set(500, 360);
        npc.decidir(jogador, random, 1f / 120);
        assertEquals(Npc.Estado.PERSEGUINDO, npc.estado);
        jogador.posicao.set(570, 360);
        npc.decidir(jogador, random, 1f / 120);
        assertEquals(Npc.Estado.PERSEGUINDO, npc.estado);
        jogador.posicao.set(700, 360);
        npc.decidir(jogador, random, 1f / 120);
        assertEquals(Npc.Estado.VAGANDO, npc.estado);
    }

    @Test void pursuitAntecipaMovimentoSemModificarJogador() {
        Agente npc = new Agente(300, 350), jogador = new Agente(450, 350);
        jogador.velocidade.set(0, 100);
        assertTrue(Steering.previsao(npc, jogador).y > jogador.posicao.y);
        assertTrue(Steering.pursuit(npc, jogador).y > 0);
        assertEquals(new Vector2(450, 350), jogador.posicao);
        assertEquals(new Vector2(0, 100), jogador.velocidade);
    }

    @Test void regrasDeBandoTemDirecoesEsperadas() {
        Agente a = new Agente(300, 350), b = new Agente(320, 350);
        b.velocidade.set(0, 100);
        List<Agente> grupo = List.of(a, b);
        assertTrue(Steering.bando(a, grupo, 1, 0, 0).x < 0);
        assertTrue(Steering.bando(a, grupo, 0, 1, 0).y > 0);
        assertTrue(Steering.bando(a, grupo, 0, 0, 1).x > 0);
        b.posicao.set(900, 350);
        assertTrue(Steering.bando(a, grupo, 1, 1, 1).isZero());
    }

    @Test void trintaAgentesPermanecemEstaveisPorUmMinuto() {
        Mundo m = new Mundo(); m.definirModo(Mundo.Modo.BANDO);
        for (int i = 0; i < 7200; i++) m.atualizar(1f / 120);
        assertEquals(30, m.agentes.size());
        for (Agente a : m.agentes) {
            assertTrue(Float.isFinite(a.posicao.x) && Float.isFinite(a.posicao.y));
            assertTrue(a.posicao.x >= 55 && a.posicao.x <= 1225);
            assertTrue(a.posicao.y >= 155 && a.posicao.y <= 605);
            assertTrue(a.velocidade.len() <= a.maxVelocidade + .001f);
            assertTrue(a.forca.len() <= a.maxForca + .001f);
        }
    }
}
