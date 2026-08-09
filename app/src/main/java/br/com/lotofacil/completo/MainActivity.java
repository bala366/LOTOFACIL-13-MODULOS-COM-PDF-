package br.com.lotofacil.completo;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.*;
import android.graphics.pdf.PdfDocument;
import android.provider.MediaStore;
import android.graphics.Typeface;
import android.net.Uri;
import android.view.*;
import android.widget.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

public class MainActivity extends Activity {
    static final int PURPLE=Color.rgb(106,27,154),PURPLE_DARK=Color.rgb(74,20,140),
            BG=Color.rgb(250,247,252),TEXT=Color.rgb(32,33,36);

    final ExecutorService executor=Executors.newSingleThreadExecutor();
    CoreEngine.Model model;
    int currentModule=1;
    ArrayList<int[]> history=new ArrayList<>();

    LinearLayout root,content,resultsBox;
    TextView status,perimeterInfo;
    CheckBox focusPerimeter;
    Spinner perimeterThreshold;
    Button btnLoad;

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setStatusBarColor(PURPLE_DARK);
        showDashboard();
    }

    @Override protected void onDestroy(){super.onDestroy();executor.shutdownNow();}

    void showDashboard(){
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(BG);
        root=vbox();root.setPadding(dp(16),dp(12),dp(16),dp(40));scroll.addView(root);

        LinearLayout header=vbox();header.setPadding(dp(18),dp(16),dp(18),dp(16));header.setBackgroundColor(PURPLE);
        header.addView(tv("☘  LOTOFÁCIL 13 MÓDULOS",28,true,Color.WHITE));
        header.addView(tv("6 módulos de estudo • perímetro histórico • padrões temperados",15,false,Color.WHITE));
        root.addView(header,lpMatch());

        section("BASE ÚNICA");
        btnLoad=button("SELECIONAR TXT DE RESULTADOS");root.addView(btnLoad,lpMatchH(dp(62)));
        status=tv("Carregue o TXT uma vez. Todos os módulos usam a mesma base.",15,false,TEXT);
        status.setPadding(0,dp(8),0,dp(8));root.addView(status,lpMatch());

        LinearLayout focusBox=vbox();focusBox.setPadding(dp(10),dp(10),dp(10),dp(10));focusBox.setBackgroundColor(Color.WHITE);
        focusPerimeter=new CheckBox(this);focusPerimeter.setText("FOCAR NO ESTUDO DO PERÍMETRO");focusPerimeter.setTextSize(16);focusPerimeter.setChecked(true);
        focusBox.addView(focusPerimeter,lpMatch());
        perimeterThreshold=new Spinner(this);
        String[] th={"Priorizar perímetro de 11 pontos","Priorizar perímetro de 12 pontos","Priorizar perímetro de 13 pontos"};
        perimeterThreshold.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,th));
        focusBox.addView(perimeterThreshold,lpMatch());
        perimeterInfo=tv("Perímetro: carregue a base para calcular média e faixa dominante.",14,false,TEXT);
        perimeterInfo.setPadding(0,dp(6),0,0);focusBox.addView(perimeterInfo,lpMatch());
        root.addView(focusBox,lpMatch());

        section("ESCOLHA O MÓDULO");
        addModuleButton("MÓDULO 1 — REPETIDAS + ESPELHO",1);
        addModuleButton("MÓDULO 2 — 4 FALHAS FIXAS → GRUPO DE 21",2);
        addModuleButton("MÓDULO 3 — NÚCLEO DAS 10 FALHAS",3);
        addModuleButton("MÓDULO 4 — ESTUDO DAS 300 DUPLAS",4);
        addModuleButton("MÓDULO 5 — TENDÊNCIA DEZENA POR DEZENA",5);
        addModuleButton("MÓDULO 6 — CAMPEÃO DOS MÉTODOS",6);
        addModuleButton("MÓDULO 7 — DUPLAS DE FALHA + TENDÊNCIA",7);
        addModuleButton("MÓDULO 8 — TEMPO, ESPAÇO E VIZINHANÇA",8);
        addModuleButton("MÓDULO 9 — VIZINHANÇA DOS 150 / CAÇA AOS 14",9);
        addModuleButton("MÓDULO 10 — FECHAMENTO INTELIGENTE",10);
        addModuleButton("MÓDULO 11 — CAMPEÃO DOS CAMPEÕES",11);
        addModuleButton("MÓDULO 12 — MEU JOGO / CLASSIFICADOR",12);
        addModuleButton("MÓDULO 13 — SIMILARIDADE + EVOLUÇÃO",13);

        content=vbox();root.addView(content,lpMatch());
        btnLoad.setOnClickListener(v->pickTxt());

        setContentView(scroll);
    }

    void addModuleButton(String s,int id){
        Button b=button(s);LinearLayout.LayoutParams lp=lpMatchH(dp(64));lp.setMargins(0,dp(5),0,dp(5));
        root.addView(b,lp);b.setOnClickListener(v->showModule(id));
    }

    void showModule(int id){
        currentModule=id;
        if(content==null)return;
        content.removeAllViews();
        resultsBox=vbox();
        if(id==1)module1();
        else if(id==2)module2();
        else if(id==3)module3();
        else if(id==4)module4();
        else if(id==5) module5();
        else if(id==6) module6();
        else if(id==7) module7();
        else if(id==8) module8();
        else if(id==9) module9();
        else if(id==10) module10();
        else if(id==11) module11();
        else if(id==12) module12();
        else module13();
    }

    int threshold(){
        int p=perimeterThreshold==null?0:perimeterThreshold.getSelectedItemPosition();
        return p==2?13:(p==1?12:11);
    }
    boolean fp(){return focusPerimeter!=null&&focusPerimeter.isChecked();}

    void module1(){
        sectionInto(content,"MÓDULO 1 — REPETIDAS + ESPELHO");
        content.addView(tv("Escolha as repetidas. O motor cruza repetidas + espelho e prioriza o perímetro 11→15/12→15/13→15 conforme sua escolha.",15,false,TEXT));
        Spinner reps=new Spinner(this);String[]opts=new String[11];for(int i=0;i<11;i++)opts[i]=String.valueOf(i+5);
        reps.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,opts));reps.setSelection(5);
        content.addView(reps,lpMatchH(dp(55)));
        Button run=button("GERAR PELO MÓDULO 1");content.addView(run,lpMatchH(dp(64)));content.addView(resultsBox,lpMatch());
        run.setOnClickListener(v->{
            if(!checkBase())return;run.setEnabled(false);resultsBox.removeAllViews();
            int r=Integer.parseInt(String.valueOf(reps.getSelectedItem()));
            executor.submit(()->{
                try{
                    List<CoreEngine.Candidate>top=CoreEngine.topRepeatsMirror(model,r,3,fp(),threshold(),this::post);
                    runOnUiThread(()->{showCandidates(top,false);run.setEnabled(true);status.setText("Módulo 1 concluído.");});
                }catch(Exception e){runOnUiThread(()->{status.setText("Erro: "+e.getMessage());run.setEnabled(true);});}
            });
        });
    }

    void module2(){
        sectionInto(content,"MÓDULO 2 — 4 FALHAS FIXAS");
        content.addView(tv("Digite 4 dezenas que devem ficar fora, separadas por espaço. As outras 21 formam o grupo. O motor avalia os 54.264 jogos de 15.",15,false,TEXT));
        EditText e=new EditText(this);e.setHint("Ex.: 02 07 18 24");content.addView(e,lpMatchH(dp(58)));
        Button run=button("ANALISAR GRUPO DE 21");content.addView(run,lpMatchH(dp(64)));content.addView(resultsBox,lpMatch());
        run.setOnClickListener(v->{
            if(!checkBase())return;
            int[]f=parse4(e.getText().toString());if(f==null){toast("Digite exatamente 4 dezenas diferentes de 01 a 25.");return;}
            run.setEnabled(false);resultsBox.removeAllViews();
            executor.submit(()->{
                try{
                    List<CoreEngine.Candidate>top=CoreEngine.topFromFourFixedFailures(model,f,3,fp(),threshold(),this::post);
                    runOnUiThread(()->{showCandidates(top,false);run.setEnabled(true);status.setText("Módulo 2 concluído.");});
                }catch(Exception ex){runOnUiThread(()->{status.setText("Erro: "+ex.getMessage());run.setEnabled(true);});}
            });
        });
    }

    void module3(){
        sectionInto(content,"MÓDULO 3 — NÚCLEO DAS 10 FALHAS");
        content.addView(tv("Varre todas as 3.268.760 combinações de 10 falhas. Estuda movimento das falhas, núcleos 5/6, frequência, pares, padrões e perímetro.",15,false,TEXT));
        Button run=button("ANALISAR TODAS AS FALHAS");content.addView(run,lpMatchH(dp(68)));content.addView(resultsBox,lpMatch());
        run.setOnClickListener(v->{
            if(!checkBase())return;run.setEnabled(false);resultsBox.removeAllViews();
            executor.submit(()->{
                try{
                    List<CoreEngine.Candidate>top=CoreEngine.topFailures(model,5,fp(),threshold(),this::post);
                    runOnUiThread(()->{showCandidates(top,true);run.setEnabled(true);status.setText("Módulo 3 concluído.");});
                }catch(Exception ex){runOnUiThread(()->{status.setText("Erro: "+ex.getMessage());run.setEnabled(true);});}
            });
        });
    }

    void module4(){
        sectionInto(content,"MÓDULO 4 — ESTUDO DAS 300 DUPLAS");
        content.addView(tv("Estuda as 300 duplas: falha contínua, saída, retorno e casamento. Para 9 repetidas: 3 duplas saem do último resultado e 3 entram do espelho. Para 10 repetidas: 2 duplas + 1 dezena individual saem e entram.",15,false,TEXT));
        Spinner reps=new Spinner(this);reps.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"9 repetidas","10 repetidas"}));
        content.addView(reps,lpMatchH(dp(55)));
        Button run=button("ANALISAR DUPLAS E GERAR");content.addView(run,lpMatchH(dp(68)));content.addView(resultsBox,lpMatch());
        run.setOnClickListener(v->{
            if(!checkBase())return;run.setEnabled(false);resultsBox.removeAllViews();
            int r=reps.getSelectedItemPosition()==0?9:10;
            executor.submit(()->{
                try{
                    List<CoreEngine.Candidate>top=CoreEngine.module4PairGames(model,r,3,fp(),threshold());
                    runOnUiThread(()->{showCandidates(top,false);run.setEnabled(true);status.setText("Módulo 4 concluído.");});
                }catch(Exception ex){runOnUiThread(()->{status.setText("Erro: "+ex.getMessage());run.setEnabled(true);});}
            });
        });
    }

    void module5(){
        sectionInto(content,"MÓDULO 5 — TENDÊNCIA DEZENA POR DEZENA");
        content.addView(tv("Ranqueia as 25 dezenas por tendência de entrada/falha usando frequência recente e sequência atual de falhas. Depois monta jogos temperados e cruza com perímetro.",15,false,TEXT));
        Button show=button("VER RANKING DAS 25 DEZENAS");content.addView(show,lpMatchH(dp(62)));
        Button run=button("GERAR JOGO PELO RANKING");content.addView(run,lpMatchH(dp(68)));content.addView(resultsBox,lpMatch());

        show.setOnClickListener(v->{
            if(!checkBase())return;resultsBox.removeAllViews();
            List<CoreEngine.DecadeTrend>r=CoreEngine.rankNumbers(model);
            StringBuilder s=new StringBuilder();
            int pos=1;
            for(CoreEngine.DecadeTrend d:r){
                s.append(pos++).append("º  ").append(String.format(Locale.US,"%02d",d.n))
                 .append("  tendência=").append(String.format(Locale.US,"%.2f",d.trend))
                 .append("  falha atual=").append(d.currentFailStreak).append("\n");
            }
            resultsBox.addView(tv(s.toString(),15,false,TEXT),lpMatch());
        });

        run.setOnClickListener(v->{
            if(!checkBase())return;run.setEnabled(false);resultsBox.removeAllViews();
            executor.submit(()->{
                try{
                    List<CoreEngine.Candidate>top=CoreEngine.module5NumberTrend(model,3,fp(),threshold());
                    runOnUiThread(()->{showCandidates(top,false);run.setEnabled(true);status.setText("Módulo 5 concluído.");});
                }catch(Exception ex){runOnUiThread(()->{status.setText("Erro: "+ex.getMessage());run.setEnabled(true);});}
            });
        });
    }


    void module6(){
        sectionInto(content,"MÓDULO 6 — CAMPEÃO DOS MÉTODOS");
        content.addView(tv(
                "Cruza os melhores candidatos dos módulos 1 a 5. Cada jogo recebe pontos por aparecer bem em mais de um método, " +
                "por ficar dentro do perímetro escolhido e por manter um padrão temperado. O objetivo é encontrar consenso entre " +
                "métodos diferentes, em vez de confiar em um único estudo.",15,false,TEXT));

        Spinner reps=new Spinner(this);
        reps.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,
                new String[]{"9 repetidas no Módulo 1/4","10 repetidas no Módulo 1/4"}));
        content.addView(reps,lpMatchH(dp(55)));

        EditText falhas4=new EditText(this);
        falhas4.setHint("4 falhas fixas do Módulo 2. Ex.: 02 07 18 24");
        content.addView(falhas4,lpMatchH(dp(58)));

        Button run=button("EXECUTAR CAMPEÃO DOS MÉTODOS");
        content.addView(run,lpMatchH(dp(72)));
        content.addView(resultsBox,lpMatch());

        run.setOnClickListener(v->{
            if(!checkBase())return;
            int[] f=parse4(falhas4.getText().toString());
            if(f==null){toast("Digite as 4 falhas fixas para o Módulo 2.");return;}
            int r=reps.getSelectedItemPosition()==0?9:10;
            run.setEnabled(false);resultsBox.removeAllViews();

            executor.submit(()->{
                try{
                    post("Módulo 6: coletando candidatos dos 5 métodos...");
                    ArrayList<CoreEngine.Candidate> pool=new ArrayList<>();

                    pool.addAll(CoreEngine.topRepeatsMirror(model,r,12,fp(),threshold(),null));
                    pool.addAll(CoreEngine.topFromFourFixedFailures(model,f,12,fp(),threshold(),null));
                    for(CoreEngine.Candidate fc:CoreEngine.topFailures(model,12,fp(),threshold(),null)){
                        pool.add(new CoreEngine.Candidate(CoreEngine.drawFromFailure(fc.mask),fc.score,fc.perimeterScore,fc.patternHits));
                    }
                    pool.addAll(CoreEngine.module4PairGames(model,r,12,fp(),threshold()));
                    pool.addAll(CoreEngine.module5NumberTrend(model,12,fp(),threshold()));

                    HashMap<Integer,Double> consensus=new HashMap<>();
                    HashMap<Integer,Integer> appearances=new HashMap<>();
                    HashMap<Integer,CoreEngine.Candidate> best=new HashMap<>();

                    for(CoreEngine.Candidate c:pool){
                        int mask=c.mask;
                        double add=c.score + c.perimeterScore*8.0 + Math.min(6,c.patternHits)*1.2;
                        consensus.put(mask,consensus.getOrDefault(mask,0.0)+add);
                        appearances.put(mask,appearances.getOrDefault(mask,0)+1);
                        CoreEngine.Candidate old=best.get(mask);
                        if(old==null||c.score>old.score)best.put(mask,c);
                    }

                    ArrayList<CoreEngine.Candidate> finals=new ArrayList<>();
                    for(Map.Entry<Integer,Double> e:consensus.entrySet()){
                        int mask=e.getKey();
                        int ap=appearances.get(mask);
                        CoreEngine.Candidate b=best.get(mask);
                        double finalScore=e.getValue()+ap*12.0;
                        finals.add(new CoreEngine.Candidate(mask,finalScore,b.perimeterScore,b.patternHits));
                    }
                    finals.sort((a,b)->Double.compare(b.score,a.score));
                    if(finals.size()>5) finals=new ArrayList<>(finals.subList(0,5));

                    final ArrayList<CoreEngine.Candidate> out=finals;
                    runOnUiThread(()->{
                        showCandidates(out,false);
                        TextView note=tv(
                                "No Módulo 6, a nota final aumenta quando o mesmo jogo ou um candidato equivalente aparece forte em " +
                                "mais de um método. O perímetro continua valendo para todos os candidatos.",13,false,TEXT);
                        note.setPadding(0,dp(8),0,dp(8));
                        resultsBox.addView(note,lpMatch());
                        run.setEnabled(true);
                        status.setText("Módulo 6 concluído.");
                    });
                }catch(Exception ex){
                    runOnUiThread(()->{status.setText("Erro: "+ex.getMessage());run.setEnabled(true);});
                }
            });
        });
    }

    void showCandidates(List<CoreEngine.Candidate>top,boolean failures){
        for(int i=0;i<top.size();i++){
            CoreEngine.Candidate c=top.get(i);
            int mask=failures?CoreEngine.drawFromFailure(c.mask):c.mask;
            LinearLayout card=vbox();card.setPadding(dp(12),dp(10),dp(12),dp(10));card.setBackgroundColor(Color.WHITE);
            card.addView(tv("JOGO DO MÓDULO "+currentModule+" — "+(failures?"FALHA/JOGO ":"PALPITE ")+(i+1),21,true,PURPLE_DARK));
            if(failures){
                card.addView(tv("10 FALHAS: "+fmt(CoreEngine.numsOf(c.mask)),18,true,TEXT));
                card.addView(tv("JOGO COMPLEMENTAR: "+fmt(CoreEngine.numsOf(mask)),20,true,PURPLE_DARK));
            }else{
                card.addView(tv(fmt(CoreEngine.numsOf(mask)),22,true,TEXT));
            }
            card.addView(tv("Padrões fortes: "+c.patternHits+" • Perímetro: "+String.format(Locale.US,"%.0f%%",c.perimeterScore*100)+" • Nota: "+String.format(Locale.US,"%.2f",c.score),14,false,TEXT));
            card.addView(tv("PADRÕES: "+CoreEngine.patternSummary(mask,model.lastDraw),13,false,TEXT));
            Button pdf=button("SALVAR ESTE JOGO EM PDF");
            final int pdfMask=mask;
            final String pdfTitle=(failures?"JOGO DO MÓDULO "+currentModule+" — FALHAS":"JOGO DO MÓDULO "+currentModule);
            pdf.setOnClickListener(v->saveGamePdf(pdfMask,pdfTitle,CoreEngine.patternSummary(pdfMask,model.lastDraw)));
            card.addView(pdf,lpMatchH(dp(56)));
            TextView warn=tv("Análise estatística não prevê nem garante prêmio.",12,false,Color.DKGRAY);warn.setPadding(0,dp(6),0,0);card.addView(warn);
            LinearLayout.LayoutParams lp=lpMatch();lp.setMargins(0,dp(8),0,dp(6));resultsBox.addView(card,lp);
        }
    }


    void module7(){
        sectionInto(content,"MÓDULO 7 — DUPLAS DE FALHA + TENDÊNCIA DE REPETIDAS");
        content.addView(tv(
            "Ranqueia as 300 duplas. Até 3 concursos antes do último: falha conjunta vale 100 e falha parcial vale 1. " +
            "Depois mede a tendência dos 3 concursos finais e decide entre 9 repetidas (troca 6 por 6) e 10 repetidas " +
            "(troca 5 por 5). O jogo final também é conferido por soma, linhas, colunas, primos, Fibonacci, miolo, cruz e borda.",
            15,false,TEXT));

        Button run=button("EXECUTAR MÓDULO 7 E GERAR JOGO");
        content.addView(run,lpMatchH(dp(72)));
        content.addView(resultsBox,lpMatch());

        run.setOnClickListener(v->{
            if(!checkBase())return;
            run.setEnabled(false);
            resultsBox.removeAllViews();

            executor.submit(()->{
                try{
                    CoreEngine.Module7Result r=CoreEngine.module7FailurePairTrend(model);
                    CoreEngine.Candidate scored=CoreEngine.scoreDraw(model,r.gameMask,fp(),threshold());

                    runOnUiThread(()->{
                        resultsBox.addView(tv(r.detail,15,false,TEXT));

                        StringBuilder tr=new StringBuilder("RANKING DE REPETIDAS\n");
                        int lim=Math.min(5,r.repeatRanking.size());
                        for(int i=0;i<lim;i++){
                            CoreEngine.RepeatTrend x=r.repeatRanking.get(i);
                            tr.append(i+1).append("º ").append(x.repeats)
                              .append(" repetidas • histórico=").append(x.historical)
                              .append(" • últimos3=").append(x.recent)
                              .append(" • score=").append(String.format(Locale.US,"%.2f",x.score))
                              .append("\n");
                        }
                        resultsBox.addView(tv(tr.toString(),14,false,TEXT));

                        StringBuilder pr=new StringBuilder("TOP 12 DUPLAS DE FALHA\n");
                        for(int i=0;i<Math.min(12,r.ranking.size());i++){
                            CoreEngine.PairFailureRank p=r.ranking.get(i);
                            pr.append(i+1).append("º ").append(p.pair())
                              .append(" • score=").append(p.score)
                              .append(" • juntas=").append(p.bothFail)
                              .append(" • parcial=").append(p.partialFail)
                              .append(" • atraso=").append(p.currentBothFailGap)
                              .append("\n");
                        }
                        resultsBox.addView(tv(pr.toString(),13,false,TEXT));

                        ArrayList<CoreEngine.Candidate> one=new ArrayList<>();
                        one.add(new CoreEngine.Candidate(r.gameMask,scored.score,scored.perimeterScore,scored.patternHits));
                        showCandidates(one,false);

                        status.setText("Módulo 7 concluído.");
                        run.setEnabled(true);
                    });
                }catch(Exception ex){
                    runOnUiThread(()->{
                        status.setText("Erro: "+ex.getMessage());
                        run.setEnabled(true);
                    });
                }
            });
        });
    }

    void saveGamePdf(int mask,String title,String study){
        try{
            PdfDocument doc=new PdfDocument();
            PdfDocument.Page page=doc.startPage(new PdfDocument.PageInfo.Builder(595,842,1).create());
            Canvas c=page.getCanvas();
            Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);

            p.setColor(PURPLE);
            c.drawRect(0,0,595,92,p);
            p.setColor(Color.WHITE);
            p.setTypeface(Typeface.DEFAULT_BOLD);
            p.setTextSize(25);
            c.drawText("LOTOFÁCIL — VOLANTE CAMPEÃO",28,40,p);
            p.setTextSize(12);
            c.drawText(title,28,67,p);

            boolean[] sel=new boolean[26];
            for(int n:CoreEngine.numsOf(mask))sel[n]=true;

            float sx=65,sy=125,w=92,h=54;
            for(int n=1;n<=25;n++){
                int row=(n-1)/5,col=(n-1)%5;
                float x=sx+col*w,y=sy+row*h;
                p.setColor(sel[n]?PURPLE:Color.rgb(238,238,238));
                c.drawRoundRect(x,y,x+72,y+40,8,8,p);
                p.setColor(sel[n]?Color.WHITE:TEXT);
                p.setTypeface(Typeface.DEFAULT_BOLD);
                p.setTextSize(15);
                c.drawText(String.format(Locale.US,"%02d",n),x+25,y+26,p);
            }

            p.setColor(PURPLE_DARK);
            p.setTypeface(Typeface.DEFAULT_BOLD);
            p.setTextSize(18);
            c.drawText("JOGO: "+fmt(CoreEngine.numsOf(mask)),30,440,p);
            p.setTextSize(13);
            c.drawText("MÓDULO: "+currentModule,30,463,p);

            p.setTextSize(13);
            c.drawText("ESTUDO DO JOGO",30,493,p);

            p.setColor(TEXT);
            p.setTypeface(Typeface.DEFAULT);
            p.setTextSize(10);
            drawPdfText(c,study,30,515,530,15,18,p);

            doc.finishPage(page);

            String fileName="LOTOFACIL_MODULO_"+currentModule+"_"+System.currentTimeMillis()+".pdf";
            OutputStream out;

            if(Build.VERSION.SDK_INT>=29){
                ContentValues cv=new ContentValues();
                cv.put(MediaStore.MediaColumns.DISPLAY_NAME,fileName);
                cv.put(MediaStore.MediaColumns.MIME_TYPE,"application/pdf");
                cv.put(MediaStore.MediaColumns.RELATIVE_PATH,"Download/LOTOFACIL_PDF");
                Uri uri=getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,cv);
                if(uri==null)throw new IOException("Não foi possível criar o PDF.");
                out=getContentResolver().openOutputStream(uri);
            }else{
                File dir=new File(getExternalFilesDir(null),"LOTOFACIL_PDF");
                if(!dir.exists())dir.mkdirs();
                out=new FileOutputStream(new File(dir,fileName));
            }

            if(out==null)throw new IOException("Saída do PDF indisponível.");
            doc.writeTo(out);
            out.close();
            doc.close();

            toast("PDF salvo em Downloads/LOTOFACIL_PDF");
            status.setText("PDF salvo: "+fileName);
        }catch(Exception e){
            status.setText("Erro PDF: "+e.getMessage());
        }
    }

    void drawPdfText(Canvas c,String text,float x,float y,float max,float lineH,int maxLines,Paint p){
        ArrayList<String> lines=new ArrayList<>();
        for(String par:text.replace("\r","").split("\n")){
            String line="";
            for(String word:par.split("\\s+")){
                String test=line.isEmpty()?word:line+" "+word;
                if(p.measureText(test)>max){
                    if(!line.isEmpty())lines.add(line);
                    line=word;
                }else line=test;
            }
            if(!line.isEmpty())lines.add(line);
        }
        for(int i=0;i<Math.min(maxLines,lines.size());i++)c.drawText(lines.get(i),x,y+i*lineH,p);
    }


    void module8(){
        sectionInto(content,"MÓDULO 8 — TEMPO, ESPAÇO E VIZINHANÇA");
        content.addView(tv("Mede a recorrência histórica e recente de 12, 13 e 14 pontos na vizinhança de uma troca.",15,false,TEXT));
        Button run=button("GERAR JOGO DO MÓDULO 8");content.addView(run,lpMatchH(dp(68)));
        Button bt=button("BACKTEST — ÚLTIMOS 100");content.addView(bt,lpMatchH(dp(56)));content.addView(resultsBox,lpMatch());
        run.setOnClickListener(v->{if(!checkBase())return;run.setEnabled(false);resultsBox.removeAllViews();executor.submit(()->{try{CoreEngine.Candidate c=CoreEngine.module8TimeSpace(model);runOnUiThread(()->{showCandidates(Arrays.asList(c),false);run.setEnabled(true);});}catch(Exception e){runOnUiThread(()->{status.setText("Erro: "+e.getMessage());run.setEnabled(true);});}});});
        bt.setOnClickListener(v->runBacktest("Módulo 8",m->CoreEngine.module8TimeSpace(m).mask));
    }
    void module9(){
        sectionInto(content,"MÓDULO 9 — VIZINHANÇA DOS 150 / CAÇA AOS 14");
        content.addView(tv("Gera os 150 vizinhos de uma troca do jogo-base e ranqueia a região mais forte.",15,false,TEXT));
        Button run=button("ANALISAR OS 150 E GERAR");content.addView(run,lpMatchH(dp(68)));
        Button bt=button("BACKTEST — ÚLTIMOS 100");content.addView(bt,lpMatchH(dp(56)));content.addView(resultsBox,lpMatch());
        run.setOnClickListener(v->{if(!checkBase())return;run.setEnabled(false);resultsBox.removeAllViews();executor.submit(()->{try{CoreEngine.Candidate c=CoreEngine.module9Neighbors150(model);runOnUiThread(()->{showCandidates(Arrays.asList(c),false);run.setEnabled(true);});}catch(Exception e){runOnUiThread(()->{status.setText("Erro: "+e.getMessage());run.setEnabled(true);});}});});
        bt.setOnClickListener(v->runBacktest("Módulo 9",m->CoreEngine.module9Neighbors150(m).mask));
    }
    void module10(){
        sectionInto(content,"MÓDULO 10 — FECHAMENTO INTELIGENTE");
        content.addView(tv("Seleciona 18 dezenas fortes e gera 10 cartões de 15 com diversidade.",15,false,TEXT));
        Button run=button("GERAR FECHAMENTO");content.addView(run,lpMatchH(dp(68)));
        Button bt=button("BACKTEST DO CARTÃO CAMPEÃO");content.addView(bt,lpMatchH(dp(56)));content.addView(resultsBox,lpMatch());
        run.setOnClickListener(v->{if(!checkBase())return;run.setEnabled(false);resultsBox.removeAllViews();executor.submit(()->{try{CoreEngine.ClosureResult r=CoreEngine.module10SmartClosure(model,18,10);runOnUiThread(()->{resultsBox.addView(tv(r.detail,14,false,TEXT));showCandidates(r.tickets,false);run.setEnabled(true);});}catch(Exception e){runOnUiThread(()->{status.setText("Erro: "+e.getMessage());run.setEnabled(true);});}});});
        bt.setOnClickListener(v->runBacktest("Módulo 10",m->CoreEngine.module10SmartClosure(m,18,10).tickets.get(0).mask));
    }
    void module11(){
        sectionInto(content,"MÓDULO 11 — CAMPEÃO DOS CAMPEÕES");
        content.addView(tv("Cruza o consenso dos estudos novos e escolhe o jogo final pelos padrões.",15,false,TEXT));
        Button run=button("GERAR CAMPEÃO DOS CAMPEÕES");content.addView(run,lpMatchH(dp(68)));
        Button bt=button("BACKTEST — ÚLTIMOS 100");content.addView(bt,lpMatchH(dp(56)));content.addView(resultsBox,lpMatch());
        run.setOnClickListener(v->{if(!checkBase())return;run.setEnabled(false);resultsBox.removeAllViews();executor.submit(()->{try{CoreEngine.Candidate c=CoreEngine.module11ChampionOfChampions(model);runOnUiThread(()->{showCandidates(Arrays.asList(c),false);run.setEnabled(true);});}catch(Exception e){runOnUiThread(()->{status.setText("Erro: "+e.getMessage());run.setEnabled(true);});}});});
        bt.setOnClickListener(v->runBacktest("Módulo 11",m->CoreEngine.module11ChampionOfChampions(m).mask));
    }
    void runBacktest(String name,CoreEngine.HistoricalGenerator gen){
        if(!checkBase())return;status.setText("Backtest: "+name+"...");
        executor.submit(()->{try{CoreEngine.BacktestResult r=CoreEngine.backtest(model,100,gen);runOnUiThread(()->{resultsBox.addView(tv(r.summary(),14,true,TEXT));status.setText("Backtest concluído.");});}catch(Exception e){runOnUiThread(()->status.setText("Erro no backtest: "+e.getMessage()));}});
    }


    int[] parse15(String s){
        if(s==null)return null;
        String[] parts=s.trim().split("[^0-9]+");
        LinkedHashSet<Integer> set=new LinkedHashSet<>();
        for(String p:parts){
            if(p.isEmpty())continue;
            try{
                int n=Integer.parseInt(p);
                if(n>=1&&n<=25)set.add(n);
            }catch(Exception ignored){}
        }
        if(set.size()!=15)return null;
        int[] a=new int[15];int i=0;
        for(int n:set)a[i++]=n;
        Arrays.sort(a);
        return a;
    }

    void module12(){
        sectionInto(content,"MÓDULO 12 — MEU JOGO / CLASSIFICADOR");
        content.addView(tv(
            "Digite exatamente 15 dezenas. O aplicativo mantém o seu jogo e apenas classifica pelos padrões, calcula score, perímetro e permite salvar o volante em PDF.",
            15,false,TEXT));

        EditText e=new EditText(this);
        e.setHint("Ex.: 01 02 03 05 06 08 09 10 11 13 15 17 20 22 25");
        content.addView(e,lpMatchH(dp(64)));

        Button run=button("CLASSIFICAR MEU JOGO");
        content.addView(run,lpMatchH(dp(68)));
        content.addView(resultsBox,lpMatch());

        run.setOnClickListener(v->{
            if(!checkBase())return;
            int[] nums=parse15(e.getText().toString());
            if(nums==null){
                toast("Digite exatamente 15 dezenas diferentes de 01 a 25.");
                return;
            }

            resultsBox.removeAllViews();
            int mask=CoreEngine.maskOf(nums);
            CoreEngine.ManualGameAnalysis a=CoreEngine.analyzeManualGame(model,mask);

            resultsBox.addView(tv(a.detail,15,true,TEXT));

            ArrayList<CoreEngine.Candidate> one=new ArrayList<>();
            one.add(a.candidate);
            showCandidates(one,false);
            status.setText("Seu jogo foi classificado.");
        });
    }

    void module13(){
        sectionInto(content,"MÓDULO 13 — SIMILARIDADE + EVOLUÇÃO");
        content.addView(tv(
            "Pega o último resultado e procura em todo o histórico resultados parecidos, mudando no máximo 1, 2 ou 3 dezenas. Depois observa como esses casos evoluíram nos concursos seguintes e gera os melhores jogos evoluídos.",
            15,false,TEXT));

        Button run=button("PROCURAR SIMILARES E GERAR MELHOR EVOLUÍDO");
        content.addView(run,lpMatchH(dp(72)));

        Button bt=button("BACKTEST MÓDULO 13 — ÚLTIMOS 100");
        content.addView(bt,lpMatchH(dp(58)));

        content.addView(resultsBox,lpMatch());

        run.setOnClickListener(v->{
            if(!checkBase())return;
            run.setEnabled(false);
            resultsBox.removeAllViews();

            executor.submit(()->{
                try{
                    List<CoreEngine.SimilarResult> list=CoreEngine.module13SimilarityEvolution(model,10);

                    runOnUiThread(()->{
                        if(list.isEmpty()){
                            resultsBox.addView(tv("Nenhum resultado histórico encontrado com variação de 1 a 3 dezenas.",14,false,TEXT));
                        }else{
                            StringBuilder s=new StringBuilder("TOP SIMILARES HISTÓRICOS\n");
                            int pos=1;
                            for(CoreEngine.SimilarResult r:list){
                                s.append(pos++).append("º • concurso ")
                                 .append(r.sourceContestIndex+1)
                                 .append(" • variação=").append(r.distance)
                                 .append(" • origem=").append(fmt(CoreEngine.numsOf(r.sourceMask)))
                                 .append(" • evolução=").append(String.format(Locale.US,"%.2f",r.evolutionScore))
                                 .append("\n");
                            }

                            resultsBox.addView(tv(s.toString(),13,false,TEXT));

                            ArrayList<CoreEngine.Candidate> top=new ArrayList<>();
                            for(int i=0;i<Math.min(3,list.size());i++)top.add(list.get(i).evolved);
                            showCandidates(top,false);
                        }

                        status.setText("Módulo 13 concluído.");
                        run.setEnabled(true);
                    });

                }catch(Exception ex){
                    runOnUiThread(()->{
                        status.setText("Erro: "+ex.getMessage());
                        run.setEnabled(true);
                    });
                }
            });
        });

        bt.setOnClickListener(v->runBacktest(
            "Módulo 13",
            m->{
                List<CoreEngine.SimilarResult> r=CoreEngine.module13SimilarityEvolution(m,1);
                return r.isEmpty()?m.lastDraw:r.get(0).evolved.mask;
            }
        ));
    }

    void pickTxt(){
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("text/*");
        startActivityForResult(i,1001);
    }
    @Override protected void onActivityResult(int req,int res,Intent data){
        super.onActivityResult(req,res,data);
        if(req==1001&&res==RESULT_OK&&data!=null)loadTxt(data.getData());
    }
    void loadTxt(Uri uri){
        status.setText("Carregando base e calculando perímetro...");
        executor.submit(()->{
            ArrayList<int[]>h=new ArrayList<>();
            try(BufferedReader br=new BufferedReader(new InputStreamReader(getContentResolver().openInputStream(uri),StandardCharsets.UTF_8))){
                String line;while((line=br.readLine())!=null){int[]d=CoreEngine.parseDrawLine(line);if(d!=null)h.add(d);}
                if(h.size()<2)throw new Exception("Base insuficiente");
                CoreEngine.Model mm=new CoreEngine.Model(h);history=h;model=mm;
                runOnUiThread(()->{
                    status.setText("Base carregada: "+h.size()+" concursos. Último resultado: "+fmt(CoreEngine.numsOf(mm.lastDraw)));
                    perimeterInfo.setText(
                        "PERÍMETRO HISTÓRICO\n"+
                        "11→15: moda "+mm.perimeterMode11+" concursos • média "+String.format(Locale.US,"%.2f",mm.perimeterMean11)+"\n"+
                        "12→15: moda "+mm.perimeterMode12+" concursos • média "+String.format(Locale.US,"%.2f",mm.perimeterMean12)+"\n"+
                        "13→15: moda "+mm.perimeterMode13+" concursos • média "+String.format(Locale.US,"%.2f",mm.perimeterMean13)
                    );
                });
            }catch(Exception e){runOnUiThread(()->status.setText("Erro ao carregar: "+e.getMessage()));}
        });
    }

    boolean checkBase(){if(model==null){toast("Carregue primeiro o TXT de resultados.");return false;}return true;}
    int[] parse4(String s){
        String[]p=s.trim().split("[^0-9]+");ArrayList<Integer>a=new ArrayList<>();boolean[]seen=new boolean[26];
        for(String x:p)if(!x.isEmpty())try{int n=Integer.parseInt(x);if(n>=1&&n<=25&&!seen[n]){seen[n]=true;a.add(n);}}catch(Exception ignored){}
        if(a.size()!=4)return null;int[]r=new int[4];for(int i=0;i<4;i++)r[i]=a.get(i);return r;
    }

    void post(String s){runOnUiThread(()->status.setText(s));}
    String fmt(int[]a){StringBuilder s=new StringBuilder();for(int n:a){if(s.length()>0)s.append(' ');s.append(String.format(Locale.US,"%02d",n));}return s.toString();}
    void section(String s){TextView t=tv(s,21,true,TEXT);t.setPadding(0,dp(16),0,dp(7));root.addView(t,lpMatch());}
    void sectionInto(LinearLayout p,String s){TextView t=tv(s,21,true,TEXT);t.setPadding(0,dp(16),0,dp(7));p.addView(t,lpMatch());}
    LinearLayout vbox(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    TextView tv(String s,int sp,boolean bold,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(sp);t.setTextColor(color);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    Button button(String s){Button b=new Button(this);b.setText(s);b.setTextSize(15);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setTextColor(Color.WHITE);b.setBackgroundColor(PURPLE);b.setAllCaps(false);return b;}
    LinearLayout.LayoutParams lpMatch(){return new LinearLayout.LayoutParams(-1,-2);}
    LinearLayout.LayoutParams lpMatchH(int h){return new LinearLayout.LayoutParams(-1,h);}
    int dp(int n){return(int)(n*getResources().getDisplayMetrics().density+.5f);}
    void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
}
