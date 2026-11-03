package br.edu.gameia;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import java.util.ArrayDeque;
import java.util.Locale;

/** Interface libGDX e roteiro automático. As mesmas classes de IA atendem os dois. */
public class SteeringApp extends ApplicationAdapter {
    private static final Color FUNDO = Color.valueOf("101823"), PAINEL = Color.valueOf("172331");
    private static final Color TEXTO = Color.valueOf("EDF4FA"), CINZA = Color.valueOf("9FB0C2");
    private static final Color VERDE = Color.valueOf("65E0BA"), AZUL = Color.valueOf("70B8FF");
    private static final Color AMARELO = Color.valueOf("FFC86A"), ROSA = Color.valueOf("F58AAE");
    private final Mundo mundo = new Mundo();
    private final String pastaGravacao;
    private final ArrayDeque<Vector2> trilha = new ArrayDeque<>();
    private ShapeRenderer shapes;
    private SpriteBatch batch;
    private BitmapFont titulo, texto, pequeno;
    private FitViewport viewport;
    private boolean debug = true, pausado, demo;
    private float acumulador, tempoDemo;
    private int frame, etapa = -1;
    private String legenda = "Clique na arena para definir o alvo.";

    public SteeringApp(String pastaGravacao, boolean demo) {
        this.pastaGravacao = pastaGravacao;
        this.demo = demo || pastaGravacao != null;
    }

    @Override public void create() {
        shapes = new ShapeRenderer();
        batch = new SpriteBatch();
        titulo = new BitmapFont(Gdx.files.internal("fonts/title.fnt"));
        texto = new BitmapFont(Gdx.files.internal("fonts/body.fnt"));
        pequeno = new BitmapFont(Gdx.files.internal("fonts/small.fnt"));
        viewport = new FitViewport(1280, 800);
        viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);
        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override public boolean touchDown(int x, int y, int pointer, int button) {
                if (demo || button != Input.Buttons.LEFT) return false;
                Vector2 p = viewport.unproject(new Vector2(x, y));
                if (p.y >= 650 && p.y <= 688) {
                    if (p.x >= 40 && p.x < 420) trocar(Mundo.Modo.CHEGADA);
                    else if (p.x >= 435 && p.x < 815) trocar(Mundo.Modo.FSM);
                    else if (p.x >= 830 && p.x < 1240) trocar(Mundo.Modo.BANDO);
                } else if (p.x >= 40 && p.x <= 1240 && p.y >= 140 && p.y <= 620) {
                    if (mundo.modo == Mundo.Modo.CHEGADA) mundo.clicar(p.x, p.y);
                    if (mundo.modo == Mundo.Modo.FSM) { mundo.jogador.posicao.set(p); mundo.jogador.velocidade.setZero(); }
                }
                return true;
            }
        });
    }

    private void trocar(Mundo.Modo modo) {
        mundo.definirModo(modo);
        trilha.clear();
        pausado = false;
    }

    private void entrada() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) Gdx.app.exit();
        if (pastaGravacao != null) return;
        if (Gdx.input.isKeyJustPressed(Input.Keys.F5)) {
            demo = !demo; tempoDemo = 0; etapa = -1; pausado = false; trilha.clear();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.V)) debug = !debug;
        if (Gdx.input.isKeyJustPressed(Input.Keys.P)) pausado = !pausado;
        if (demo) return;
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) trocar(Mundo.Modo.CHEGADA);
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) trocar(Mundo.Modo.FSM);
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3)) trocar(Mundo.Modo.BANDO);
        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) { mundo.reiniciar(); trilha.clear(); }
        if (mundo.modo == Mundo.Modo.CHEGADA) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) mundo.temAlvo = false;
            if (Gdx.input.isKeyJustPressed(Input.Keys.S)) mundo.somenteSeek = !mundo.somenteSeek;
        }
        if (mundo.modo == Mundo.Modo.FSM) {
            float x = (pressionada(Input.Keys.D, Input.Keys.RIGHT) ? 1 : 0) - (pressionada(Input.Keys.A, Input.Keys.LEFT) ? 1 : 0);
            float y = (pressionada(Input.Keys.W, Input.Keys.UP) ? 1 : 0) - (pressionada(Input.Keys.S, Input.Keys.DOWN) ? 1 : 0);
            mundo.jogador.velocidade.set(x, y).nor().scl(mundo.jogador.maxVelocidade);
        }
        if (mundo.modo == Mundo.Modo.BANDO) {
            mundo.pesoSeparacao = ajustarPeso(mundo.pesoSeparacao, Input.Keys.Q, Input.Keys.A);
            mundo.pesoAlinhamento = ajustarPeso(mundo.pesoAlinhamento, Input.Keys.W, Input.Keys.S);
            mundo.pesoCoesao = ajustarPeso(mundo.pesoCoesao, Input.Keys.E, Input.Keys.D);
        }
    }

    private boolean pressionada(int a, int b) { return Gdx.input.isKeyPressed(a) || Gdx.input.isKeyPressed(b); }
    private float ajustarPeso(float peso, int mais, int menos) {
        if (Gdx.input.isKeyJustPressed(mais)) peso += .2f;
        if (Gdx.input.isKeyJustPressed(menos)) peso -= .2f;
        return Math.max(0, Math.min(5, peso));
    }

    /** Roteiro de 48 segundos com eventos determinísticos, sem substituir a física por animação. */
    private void roteiro() {
        int nova = tempoDemo < 14 ? 0 : tempoDemo < 32 ? 1 : 2;
        if (nova != etapa) {
            etapa = nova;
            debug = etapa != 2;
            trocar(Mundo.Modo.values()[etapa]);
            mundo.somenteSeek = false;
            mundo.pesoSeparacao = 1.7f; mundo.pesoAlinhamento = 1f; mundo.pesoCoesao = .8f;
            if (etapa == 0) mundo.clicar(920, 450);
        }
        if (etapa == 0) {
            if (tempoDemo >= 7) mundo.clicar(470, 280);
            legenda = tempoDemo < 7 ? "Alvo definido: seek aproxima e arrive reduz a velocidade." : "Novo clique: mudança suave de direção e parada no ponto.";
        } else if (etapa == 1) {
            if (tempoDemo < 18) {
                mundo.jogador.velocidade.setZero();
                legenda = "VAGANDO: sem jogador no raio de visão, o NPC usa wander.";
            } else if (tempoDemo < 26) {
                if (mundo.npc.estado == Npc.Estado.VAGANDO) {
                    mundo.jogador.posicao.set(mundo.npc.posicao).add(135, 30);
                }
                mundo.jogador.velocidade.set(95, 40 * (float)Math.sin(tempoDemo * 1.4f));
                legenda = "PERSEGUINDO: pursuit mira a posição futura do jogador.";
            } else {
                if (mundo.npc.estado == Npc.Estado.PERSEGUINDO) {
                    mundo.jogador.posicao.set(mundo.npc.posicao.x < 640 ? 1160 : 110, 550);
                    mundo.jogador.velocidade.setZero();
                }
                legenda = "Jogador fora do alcance: a FSM retorna ao estado VAGANDO.";
            }
        } else {
            if (tempoDemo < 40) {
                legenda = "30 agentes: separação, alinhamento e coesão atuam em conjunto.";
            } else {
                mundo.pesoSeparacao = 3.2f; mundo.pesoCoesao = .35f;
                legenda = "Pesos ajustados: mais separação e menos coesão espalham o bando.";
            }
        }
    }

    @Override public void render() {
        entrada();
        float dt = pastaGravacao != null ? 1f / 30f : Math.min(Gdx.graphics.getDeltaTime(), .1f);
        if (demo && !pausado) { roteiro(); tempoDemo += dt; }
        if (!pausado) {
            acumulador += dt;
            while (acumulador >= 1f / 120f) { mundo.atualizar(1f / 120f); acumulador -= 1f / 120f; }
            if (trilha.isEmpty() || trilha.peekLast().dst2(mundo.npc.posicao) > 9f) trilha.add(new Vector2(mundo.npc.posicao));
            while (trilha.size() > 160) trilha.removeFirst();
        }
        desenhar();
        if (pastaGravacao != null) {
            Pixmap pixmap = Pixmap.createFromFrameBuffer(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
            PixmapIO.writePNG(new FileHandle(pastaGravacao).child(String.format(Locale.ROOT, "frame-%05d.png", frame)), pixmap, 1, true);
            pixmap.dispose();
            if (++frame >= 1440) Gdx.app.exit();
        } else if (demo && tempoDemo >= 48f) { tempoDemo = 0; etapa = -1; debug = true; }
    }

    private void desenhar() {
        ScreenUtils.clear(FUNDO);
        viewport.apply();
        shapes.setProjectionMatrix(viewport.getCamera().combined);
        batch.setProjectionMatrix(viewport.getCamera().combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(PAINEL); shapes.rect(40, 140, 1200, 480);
        for (int i = 0; i < 3; i++) {
            shapes.setColor(mundo.modo.ordinal() == i ? VERDE : PAINEL);
            shapes.rect(40 + i * 395, 650, i == 2 ? 410 : 380, 38);
        }
        shapes.end();
        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(Color.valueOf("213141"));
        for (int x = 40; x <= 1240; x += 40) shapes.line(x, 140, x, 620);
        for (int y = 140; y <= 620; y += 40) shapes.line(40, y, 1240, y);
        shapes.end();

        float escala = viewport.getScreenWidth() / 1280f;
        Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST);
        Gdx.gl.glScissor(viewport.getScreenX() + Math.round(40 * escala), viewport.getScreenY() + Math.round(140 * escala), Math.round(1200 * escala), Math.round(480 * escala));
        shapes.begin(ShapeRenderer.ShapeType.Line);
        if (mundo.modo != Mundo.Modo.BANDO) {
            shapes.setColor(Color.valueOf("345B66"));
            Vector2 anterior = null;
            for (Vector2 p : trilha) { if (anterior != null) shapes.line(anterior, p); anterior = p; }
            if (mundo.modo == Mundo.Modo.FSM) {
                shapes.setColor(Color.valueOf("384D61"));
                shapes.circle(mundo.npc.posicao.x, mundo.npc.posicao.y, Npc.RAIO_VISAO, 90);
                if (mundo.npc.estado == Npc.Estado.PERSEGUINDO) {
                    Vector2 futuro = Steering.previsao(mundo.npc, mundo.jogador);
                    shapes.setColor(ROSA); shapes.circle(futuro.x, futuro.y, 9, 24);
                    shapes.line(mundo.jogador.posicao, futuro);
                }
            }
            if (mundo.modo == Mundo.Modo.CHEGADA && mundo.temAlvo) {
                shapes.setColor(Color.valueOf("66573D")); shapes.circle(mundo.alvo.x, mundo.alvo.y, 150, 80);
                shapes.setColor(AMARELO); shapes.circle(mundo.alvo.x, mundo.alvo.y, 11, 32);
                shapes.line(mundo.alvo.x - 18, mundo.alvo.y, mundo.alvo.x + 18, mundo.alvo.y);
                shapes.line(mundo.alvo.x, mundo.alvo.y - 18, mundo.alvo.x, mundo.alvo.y + 18);
            }
        }
        if (debug) {
            if (mundo.modo == Mundo.Modo.BANDO) for (Agente a : mundo.agentes) vetores(a);
            else vetores(mundo.npc);
        }
        shapes.end();
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        if (mundo.modo == Mundo.Modo.BANDO) for (Agente a : mundo.agentes) agente(a, VERDE, 9);
        else {
            agente(mundo.npc, mundo.modo == Mundo.Modo.FSM && mundo.npc.estado == Npc.Estado.PERSEGUINDO ? AMARELO : VERDE, 13);
            if (mundo.modo == Mundo.Modo.FSM) agente(mundo.jogador, AZUL, 12);
        }
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);

        batch.begin();
        escrever(pequeno, "GAME IA  /  ATIVIDADE PRÁTICA EM LIBGDX", 40, 778, VERDE);
        escrever(titulo, "Comportamentos de agentes autônomos", 40, 748, TEXTO);
        escrever(texto, demo ? "Demonstração automática  /  " + String.format(Locale.ROOT, "%02d / 48 s", (int)tempoDemo) : "Explore a direção, a locomoção e as decisões do NPC.", 40, 709, CINZA);
        String[] abas = { "1   SEEK + ARRIVE", "2   WANDER + FSM", "3   BANDO / 30 AGENTES" };
        for (int i = 0; i < 3; i++) escrever(texto, abas[i], 56 + i * 395, 677, mundo.modo.ordinal() == i ? FUNDO : CINZA);
        String status;
        if (mundo.modo == Mundo.Modo.CHEGADA) status = !mundo.temAlvo ? "WANDER / SEM ALVO" : mundo.somenteSeek ? "SEEK / SEM FRENAGEM" : mundo.npc.velocidade.len() < .1f ? "ARRIVE / PARADO NO ALVO" : mundo.npc.posicao.dst(mundo.alvo) < 150 ? "ARRIVE / DESACELERANDO" : "SEEK / APROXIMANDO";
        else if (mundo.modo == Mundo.Modo.FSM) status = "FSM / " + mundo.npc.estado;
        else status = String.format(Locale.ROOT, "SEPARAÇÃO %.1f    ALINHAMENTO %.1f    COESÃO %.1f", mundo.pesoSeparacao, mundo.pesoAlinhamento, mundo.pesoCoesao);
        escrever(pequeno, status, 60, 601, VERDE);
        if (mundo.modo != Mundo.Modo.BANDO) {
            escrever(pequeno, String.format(Locale.ROOT, "velocidade: %5.1f px/s     força: %5.1f px/s²", mundo.npc.velocidade.len(), mundo.npc.forca.len()), 60, 165, CINZA);
            escrever(pequeno, "limites: 220 px/s e 420 px/s²", 947, 165, CINZA);
        }
        if (mundo.modo == Mundo.Modo.FSM) escrever(pequeno, "NPC: verde/amarelo    Jogador: azul    Previsão: rosa", 700, 601, CINZA);
        if (pausado) escrever(titulo, "PAUSADO", 550, 390, TEXTO);
        if (!demo) legenda = mundo.modo == Mundo.Modo.CHEGADA ? "Clique: novo alvo    Espaço: remover alvo    S: comparar seek puro e arrive" : mundo.modo == Mundo.Modo.FSM ? "WASD / setas: mover jogador    Clique: reposicionar jogador    Visão: 230 px    Perda: 300 px" : "Pesos: Q/A separação    W/S alinhamento    E/D coesão    (aumentar / diminuir)";
        escrever(texto, legenda, 40, 107, TEXTO);
        escrever(pequeno, "1/2/3: modo    R: reiniciar    V: vetores    P: pausar    F5: demo    Esc: sair", 40, 68, CINZA);
        escrever(pequeno, "Vetor azul: velocidade    Vetor rosa: força", 40, 36, CINZA);
        escrever(pequeno, "Java 17  /  libGDX  /  passo fixo 120 Hz", 930, 36, CINZA);
        batch.end();
    }

    private void escrever(BitmapFont font, String s, float x, float y, Color cor) { font.setColor(cor); font.draw(batch, s, x, y); }
    private void agente(Agente a, Color cor, float raio) {
        float angulo = a.orientacao;
        Vector2 ponta = new Vector2(raio * 1.6f, 0).setAngleRad(angulo).add(a.posicao);
        Vector2 e = new Vector2(-raio, raio * .7f).rotateRad(angulo).add(a.posicao);
        Vector2 d = new Vector2(-raio, -raio * .7f).rotateRad(angulo).add(a.posicao);
        shapes.setColor(cor); shapes.triangle(ponta.x, ponta.y, e.x, e.y, d.x, d.y);
    }
    private void vetores(Agente a) { seta(a.posicao, new Vector2(a.velocidade).scl(.38f), AZUL); seta(a.posicao, new Vector2(a.forca).scl(.17f), ROSA); }
    private void seta(Vector2 inicio, Vector2 vetor, Color cor) {
        if (vetor.len2() < 2) return;
        Vector2 fim = new Vector2(inicio).add(vetor);
        Vector2 ponta = new Vector2(vetor).nor().scl(-8);
        shapes.setColor(cor); shapes.line(inicio, fim);
        shapes.line(fim, new Vector2(ponta).rotateDeg(30).add(fim));
        shapes.line(fim, new Vector2(ponta).rotateDeg(-30).add(fim));
    }

    @Override public void resize(int width, int height) { if (viewport != null) viewport.update(width, height, true); }
    @Override public void dispose() { shapes.dispose(); batch.dispose(); titulo.dispose(); texto.dispose(); pequeno.dispose(); }
}
