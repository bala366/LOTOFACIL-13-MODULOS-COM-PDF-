package br.com.lotofacil.completo;

import java.util.*;

public final class CoreEngine {
    private CoreEngine(){}

    public interface Progress { void onProgress(String message); }
    public static final int FULL_MASK=(1<<25)-1;
    public static final int BORDER_MASK=maskOf(new int[]{1,2,3,4,5,6,10,11,15,16,20,21,22,23,24,25});
    public static final int CENTER_MASK=maskOf(new int[]{7,8,9,12,13,14,17,18,19});
    public static final int CROSS_MASK=maskOf(new int[]{3,8,11,12,13,14,15,18,23});
    public static final int PRIME_MASK=maskOf(new int[]{2,3,5,7,11,13,17,19,23});
    public static final int FIB_MASK=maskOf(new int[]{1,2,3,5,8,13,21});

    public static int maskOf(int[] nums){
        int m=0;
        for(int n:nums)if(n>=1&&n<=25)m|=1<<(n-1);
        return m;
    }
    public static int[] numsOf(int mask){
        int[] a=new int[Integer.bitCount(mask)];
        int k=0;
        for(int n=1;n<=25;n++)if((mask&(1<<(n-1)))!=0)a[k++]=n;
        return a;
    }
    public static int drawFromFailure(int f){return FULL_MASK^f;}
    public static int failureFromDraw(int d){return FULL_MASK^d;}

    public static final class Model {
        public final int[] draws, failures;
        public final int lastDraw,lastFailure;
        public final int[] hitFreqAll=new int[26],hitFreqRecent=new int[26],failFreqAll=new int[26],failFreqRecent=new int[26];
        public final int[][] drawPair=new int[26][26],failPair=new int[26][26];

        public final int modeDrawBorder,modeDrawCenter,modeDrawCross,modeDrawPrime,modeDrawFib,modeDrawEven;
        public final int[] modeDrawRows=new int[5],modeDrawCols=new int[5];

        public final int modeFailBorder,modeFailCenter,modeFailCross,modeFailPrime,modeFailFib,modeFailEven;
        public final int[] modeFailRows=new int[5],modeFailCols=new int[5];

        public final int[] failTransitionHist=new int[11];
        public final int modeFailureOverlap;

        // Perimeter: distance from previous score >= threshold to later 15 occurrence for historical games.
        public final Map<Integer,Integer> perimeter11=new TreeMap<>();
        public final Map<Integer,Integer> perimeter12=new TreeMap<>();
        public final Map<Integer,Integer> perimeter13=new TreeMap<>();
        public final int perimeterMode11, perimeterMode12, perimeterMode13;
        public final double perimeterMean11, perimeterMean12, perimeterMean13;

        public Model(List<int[]> history){
            if(history==null||history.size()<2)throw new IllegalArgumentException("Base insuficiente");
            draws=new int[history.size()];
            failures=new int[history.size()];

            Map<Integer,Integer> db=new HashMap<>(),dc=new HashMap<>(),dx=new HashMap<>(),dp=new HashMap<>(),df=new HashMap<>(),de=new HashMap<>();
            Map<Integer,Integer> fb=new HashMap<>(),fc=new HashMap<>(),fx=new HashMap<>(),fp=new HashMap<>(),ff=new HashMap<>(),fe=new HashMap<>();
            @SuppressWarnings("unchecked") Map<Integer,Integer>[] dr=new HashMap[5], dco=new HashMap[5], fr=new HashMap[5], fco=new HashMap[5];
            for(int i=0;i<5;i++){dr[i]=new HashMap<>();dco[i]=new HashMap<>();fr[i]=new HashMap<>();fco[i]=new HashMap<>();}

            for(int i=0;i<history.size();i++){
                int dm=maskOf(history.get(i)), fm=failureFromDraw(dm);
                draws[i]=dm;failures[i]=fm;
                int[] dn=numsOf(dm), fn=numsOf(fm);

                for(int n:dn)hitFreqAll[n]++;
                for(int n:fn)failFreqAll[n]++;
                for(int a=0;a<dn.length;a++)for(int b=a+1;b<dn.length;b++){
                    int x=Math.min(dn[a],dn[b]),y=Math.max(dn[a],dn[b]);drawPair[x][y]++;
                }
                for(int a=0;a<fn.length;a++)for(int b=a+1;b<fn.length;b++){
                    int x=Math.min(fn[a],fn[b]),y=Math.max(fn[a],fn[b]);failPair[x][y]++;
                }

                collect(dm,db,dc,dx,dp,df,de,dr,dco);
                collect(fm,fb,fc,fx,fp,ff,fe,fr,fco);

                if(i>0)failTransitionHist[Integer.bitCount(fm&failures[i-1])]++;
            }

            int from=Math.max(0,history.size()-60);
            for(int i=from;i<history.size();i++){
                for(int n:numsOf(draws[i]))hitFreqRecent[n]++;
                for(int n:numsOf(failures[i]))failFreqRecent[n]++;
            }

            lastDraw=draws[draws.length-1];lastFailure=failures[failures.length-1];

            modeDrawBorder=mode(db);modeDrawCenter=mode(dc);modeDrawCross=mode(dx);modeDrawPrime=mode(dp);modeDrawFib=mode(df);modeDrawEven=mode(de);
            modeFailBorder=mode(fb);modeFailCenter=mode(fc);modeFailCross=mode(fx);modeFailPrime=mode(fp);modeFailFib=mode(ff);modeFailEven=mode(fe);
            for(int i=0;i<5;i++){
                modeDrawRows[i]=mode(dr[i]);modeDrawCols[i]=mode(dco[i]);
                modeFailRows[i]=mode(fr[i]);modeFailCols[i]=mode(fco[i]);
            }
            int mo=0,bc=-1;
            for(int i=0;i<failTransitionHist.length;i++)if(failTransitionHist[i]>bc){bc=failTransitionHist[i];mo=i;}
            modeFailureOverlap=mo;

            buildPerimeter(perimeter11,11);
            buildPerimeter(perimeter12,12);
            buildPerimeter(perimeter13,13);
            perimeterMode11=mode(perimeter11); perimeterMode12=mode(perimeter12); perimeterMode13=mode(perimeter13);
            perimeterMean11=meanDist(perimeter11); perimeterMean12=meanDist(perimeter12); perimeterMean13=meanDist(perimeter13);
        }

        private void buildPerimeter(Map<Integer,Integer> out,int threshold){
            // For each historical winning draw j, search backwards for nearest earlier draw i
            // that would have scored at least threshold against draw j.
            for(int j=1;j<draws.length;j++){
                int target=draws[j];
                for(int i=j-1;i>=0;i--){
                    int score=Integer.bitCount(target&draws[i]);
                    if(score>=threshold){
                        int dist=j-i;
                        out.put(dist,out.getOrDefault(dist,0)+1);
                        break;
                    }
                }
            }
        }
    }

    private static double meanDist(Map<Integer,Integer> m){
        long n=0,s=0;
        for(Map.Entry<Integer,Integer>e:m.entrySet()){n+=e.getValue();s+=(long)e.getKey()*e.getValue();}
        return n==0?0:s/(double)n;
    }
    private static void collect(int m,Map<Integer,Integer>b,Map<Integer,Integer>c,Map<Integer,Integer>x,Map<Integer,Integer>p,
                                Map<Integer,Integer>f,Map<Integer,Integer>e,Map<Integer,Integer>[]r,Map<Integer,Integer>[]co){
        bump(b,Integer.bitCount(m&BORDER_MASK));bump(c,Integer.bitCount(m&CENTER_MASK));bump(x,Integer.bitCount(m&CROSS_MASK));
        bump(p,Integer.bitCount(m&PRIME_MASK));bump(f,Integer.bitCount(m&FIB_MASK));bump(e,countEven(m));
        int[] rr=rows(m),cc=cols(m);for(int i=0;i<5;i++){bump(r[i],rr[i]);bump(co[i],cc[i]);}
    }
    private static void bump(Map<Integer,Integer>m,int v){m.put(v,m.getOrDefault(v,0)+1);}
    private static int mode(Map<Integer,Integer>m){
        int best=0,bc=-1;
        for(Map.Entry<Integer,Integer>e:m.entrySet())if(e.getValue()>bc){bc=e.getValue();best=e.getKey();}
        return best;
    }

    public static final class Candidate {
        public final int mask;
        public final double score,perimeterScore;
        public final int patternHits;
        public Candidate(int mask,double score,double perimeterScore,int patternHits){
            this.mask=mask;this.score=score;this.perimeterScore=perimeterScore;this.patternHits=patternHits;
        }
    }

    public static final class DecadeTrend {
        public final int n, hitRecent, failRecent, currentFailStreak;
        public final double trend;
        public DecadeTrend(int n,int hitRecent,int failRecent,int currentFailStreak,double trend){
            this.n=n;this.hitRecent=hitRecent;this.failRecent=failRecent;this.currentFailStreak=currentFailStreak;this.trend=trend;
        }
    }

    public static List<DecadeTrend> rankNumbers(Model m){
        ArrayList<DecadeTrend> out=new ArrayList<>();
        for(int n=1;n<=25;n++){
            int bit=1<<(n-1), streak=0;
            for(int i=m.draws.length-1;i>=0;i--){
                if((m.draws[i]&bit)==0)streak++; else break;
            }
            double hitRate=m.hitFreqRecent[n]/60.0;
            double failRate=m.failFreqRecent[n]/60.0;
            double trend=hitRate-failRate + Math.min(0.35,streak*0.06);
            out.add(new DecadeTrend(n,m.hitFreqRecent[n],m.failFreqRecent[n],streak,trend));
        }
        out.sort((a,b)->Double.compare(b.trend,a.trend));
        return out;
    }

    public static double perimeterScore(Model m,int candidate,int focusThreshold){
        Map<Integer,Integer> distMap=focusThreshold==13?m.perimeter13:(focusThreshold==12?m.perimeter12:m.perimeter11);
        int mode=focusThreshold==13?m.perimeterMode13:(focusThreshold==12?m.perimeterMode12:m.perimeterMode11);

        // Candidate's current "age": nearest previous historical draw with score >= threshold.
        int age=999;
        for(int i=m.draws.length-1;i>=0;i--){
            int sc=Integer.bitCount(candidate&m.draws[i]);
            if(sc>=focusThreshold){age=m.draws.length-i;break;}
        }
        if(age==999)return 0.0;

        int total=0,bestCount=0;
        for(int c:distMap.values())total+=c;
        for(int d=Math.max(1,age-1);d<=age+1;d++)bestCount+=distMap.getOrDefault(d,0);
        double local=total==0?0:bestCount/(double)total;
        double modeFit=1.0/(1.0+Math.abs(age-mode));
        return Math.min(1.0,0.55*local*10.0+0.45*modeFit);
    }

    public static Candidate scoreDraw(Model m,int mask,boolean focusPerimeter,int threshold){
        int hits=0;double s=0;
        int border=Integer.bitCount(mask&BORDER_MASK),center=Integer.bitCount(mask&CENTER_MASK),cross=Integer.bitCount(mask&CROSS_MASK),
            prime=Integer.bitCount(mask&PRIME_MASK),fib=Integer.bitCount(mask&FIB_MASK),even=countEven(mask);
        int[] rr=rows(mask),cc=cols(mask);
        if(Math.abs(border-m.modeDrawBorder)<=1){s+=2.2;hits++;}
        // Critérios adicionais pedidos para a Lotofácil:
        // faixa de soma 190–220 e borda campeã em torno de 10 dezenas.
        int sum=0; for(int n:numsOf(mask)) sum+=n;
        if(sum>=190 && sum<=220){s+=2.4;hits++;}
        if(border==10){s+=2.6;hits++;}
        else if(border==9 || border==11){s+=1.0;}
        if(Math.abs(center-m.modeDrawCenter)<=1){s+=2.0;hits++;}
        if(Math.abs(cross-m.modeDrawCross)<=1){s+=1.8;hits++;}
        if(Math.abs(prime-m.modeDrawPrime)<=1){s+=1.6;hits++;}
        if(Math.abs(fib-m.modeDrawFib)<=1){s+=1.4;hits++;}
        if(Math.abs(even-m.modeDrawEven)<=1){s+=1.8;hits++;}
        double rp=0,cp=0;for(int i=0;i<5;i++){rp+=Math.abs(rr[i]-m.modeDrawRows[i]);cp+=Math.abs(cc[i]-m.modeDrawCols[i]);}
        if(rp<=3){s+=2;hits++;} if(cp<=3){s+=2;hits++;}

        int[] nums=numsOf(mask);
        double fr=0;for(int n:nums)fr+=m.hitFreqRecent[n];
        s+=fr/50.0;

        double ps=perimeterScore(m,mask,threshold);
        s+=(focusPerimeter?10.0:3.0)*ps;
        if(ps>=0.65)hits++;

        // Tempered pattern: 3-5 strong matches are enough.
        if(hits<3)s+=hits*0.5;
        else if(hits<=5)s+=5+(hits-3)*0.8;
        else s+=6.6-(hits-5)*0.3;

        return new Candidate(mask,s,ps,hits);
    }

    public static Candidate scoreFailure(Model m,int failureMask,boolean focusPerimeter,int threshold){
        int draw=drawFromFailure(failureMask);
        Candidate dc=scoreDraw(m,draw,focusPerimeter,threshold);
        int overlap=Integer.bitCount(failureMask&m.lastFailure);
        double s=dc.score;
        if(Math.abs(overlap-m.modeFailureOverlap)<=1)s+=4;
        if(overlap==5)s+=5;if(overlap==6)s+=6;
        int[] nums=numsOf(failureMask);
        double ff=0;for(int n:nums)ff+=m.failFreqRecent[n];
        s+=ff/45.0;
        return new Candidate(failureMask,s,dc.perimeterScore,dc.patternHits);
    }

    public static List<Candidate> topFromFourFixedFailures(Model m,int[] failures,int topK,boolean focusPerimeter,int threshold,Progress progress){
        int fm=maskOf(failures);
        int group21=FULL_MASK^fm;
        int[] nums=numsOf(group21);
        PriorityQueue<Candidate>pq=new PriorityQueue<>(Comparator.comparingDouble(c->c.score));
        long[] done={0};
        combineScoreDraw(nums,15,0,0,0,m,pq,topK,focusPerimeter,threshold,progress,done,54264);
        ArrayList<Candidate>out=new ArrayList<>(pq);out.sort((a,b)->Double.compare(b.score,a.score));return out;
    }

    public static List<Candidate> topFailures(Model m,int topK,boolean focusPerimeter,int threshold,Progress progress){
        int[] nums=new int[25];for(int i=0;i<25;i++)nums[i]=i+1;
        PriorityQueue<Candidate>pq=new PriorityQueue<>(Comparator.comparingDouble(c->c.score));
        long[] done={0};
        combineScoreFailure(nums,10,0,0,0,m,pq,topK,focusPerimeter,threshold,progress,done,3268760);
        ArrayList<Candidate>out=new ArrayList<>(pq);out.sort((a,b)->Double.compare(b.score,a.score));return out;
    }

    private static void combineScoreDraw(int[]v,int k,int idx,int chosen,int mask,Model m,PriorityQueue<Candidate>pq,int topK,
                                         boolean fp,int th,Progress progress,long[]done,long total){
        if(chosen==k){
            Candidate c=scoreDraw(m,mask,fp,th);offer(pq,c,topK);done[0]++;
            if(progress!=null&&done[0]%10000==0)progress.onProgress(done[0]+" / "+total+" jogos avaliados");
            return;
        }
        int need=k-chosen;
        for(int i=idx;i<=v.length-need;i++)combineScoreDraw(v,k,i+1,chosen+1,mask|(1<<(v[i]-1)),m,pq,topK,fp,th,progress,done,total);
    }
    private static void combineScoreFailure(int[]v,int k,int idx,int chosen,int mask,Model m,PriorityQueue<Candidate>pq,int topK,
                                            boolean fp,int th,Progress progress,long[]done,long total){
        if(chosen==k){
            Candidate c=scoreFailure(m,mask,fp,th);offer(pq,c,topK);done[0]++;
            if(progress!=null&&done[0]%50000==0)progress.onProgress(done[0]+" / "+total+" falhas avaliadas");
            return;
        }
        int need=k-chosen;
        for(int i=idx;i<=v.length-need;i++)combineScoreFailure(v,k,i+1,chosen+1,mask|(1<<(v[i]-1)),m,pq,topK,fp,th,progress,done,total);
    }

    public static List<Candidate> topRepeatsMirror(Model m,int repeats,int topK,boolean focusPerimeter,int threshold,Progress progress){
        int mirrorNeed=15-repeats;
        int[] last=numsOf(m.lastDraw), mirror=numsOf(FULL_MASK^m.lastDraw);
        List<Integer> a=combinationMasks(last,repeats),b=combinationMasks(mirror,mirrorNeed);
        PriorityQueue<Candidate>pq=new PriorityQueue<>(Comparator.comparingDouble(c->c.score));
        long total=(long)a.size()*b.size(),done=0;
        for(int x:a)for(int y:b){
            Candidate c=scoreDraw(m,x|y,focusPerimeter,threshold);offer(pq,c,topK);done++;
            if(progress!=null&&done%50000==0)progress.onProgress(done+" / "+total+" combinações avaliadas");
        }
        ArrayList<Candidate>out=new ArrayList<>(pq);out.sort((x,y)->Double.compare(y.score,x.score));return out;
    }

    public static final class PairStat {
        public final int a,b,hitTogether,failTogether,currentState,currentRun;
        public final double score;
        public PairStat(int a,int b,int hitTogether,int failTogether,int currentState,int currentRun,double score){
            this.a=a;this.b=b;this.hitTogether=hitTogether;this.failTogether=failTogether;this.currentState=currentState;this.currentRun=currentRun;this.score=score;
        }
    }

    public static List<PairStat> rankPairs(Model m,boolean fromLastDraw){
        ArrayList<PairStat>out=new ArrayList<>();
        int base=fromLastDraw?m.lastDraw:(FULL_MASK^m.lastDraw);
        int[] nums=numsOf(base);
        for(int i=0;i<nums.length;i++)for(int j=i+1;j<nums.length;j++){
            int a=nums[i],b=nums[j],bit=(1<<(a-1))|(1<<(b-1));
            int hit=0,fail=0;
            for(int d:m.draws){
                int c=Integer.bitCount(d&bit);
                if(c==2)hit++;
                if(c==0)fail++;
            }
            int state=((m.lastDraw&bit)==bit)?1:0, run=0;
            for(int k=m.draws.length-1;k>=0;k--){
                boolean together=(m.draws[k]&bit)==bit;
                if((state==1&&together)||(state==0&&!together))run++;else break;
            }
            double sc = fromLastDraw ? (fail*0.7 + run*3.0) : (hit*0.9 + run*2.5);
            out.add(new PairStat(a,b,hit,fail,state,run,sc));
        }
        out.sort((x,y)->Double.compare(y.score,x.score));
        return out;
    }

    public static List<Candidate> module4PairGames(Model m,int repeats,int topK,boolean focusPerimeter,int threshold){
        // Build compact candidate pool from top pairs rather than all pair cartesian products.
        // For 9 repeats: remove 3 pairs from last + add 3 pairs from mirror.
        // For 10 repeats: remove 2 pairs + 1 individual and add 2 pairs + 1 individual.
        List<PairStat> outPairs=rankPairs(m,true);
        List<PairStat> inPairs=rankPairs(m,false);
        int limit=Math.min(18,Math.min(outPairs.size(),inPairs.size()));
        PriorityQueue<Candidate>pq=new PriorityQueue<>(Comparator.comparingDouble(c->c.score));

        if(repeats==9){
            for(int a=0;a<limit;a++)for(int b=a+1;b<limit;b++)for(int c=b+1;c<limit;c++){
                int rem=pairMask(outPairs.get(a))|pairMask(outPairs.get(b))|pairMask(outPairs.get(c));
                if(Integer.bitCount(rem)!=6)continue;
                for(int x=0;x<limit;x++)for(int y=x+1;y<limit;y++)for(int z=y+1;z<limit;z++){
                    int add=pairMask(inPairs.get(x))|pairMask(inPairs.get(y))|pairMask(inPairs.get(z));
                    if(Integer.bitCount(add)!=6)continue;
                    int game=(m.lastDraw&~rem)|add;
                    if(Integer.bitCount(game)!=15)continue;
                    offer(pq,scoreDraw(m,game,focusPerimeter,threshold),topK);
                }
            }
        }else{
            int[] last=numsOf(m.lastDraw),mir=numsOf(FULL_MASK^m.lastDraw);
            int plim=Math.min(16,limit);
            for(int a=0;a<plim;a++)for(int b=a+1;b<plim;b++){
                int remPairs=pairMask(outPairs.get(a))|pairMask(outPairs.get(b));
                if(Integer.bitCount(remPairs)!=4)continue;
                for(int single:last){
                    int sb=1<<(single-1);if((remPairs&sb)!=0)continue;
                    int rem=remPairs|sb;
                    for(int x=0;x<plim;x++)for(int y=x+1;y<plim;y++){
                        int addPairs=pairMask(inPairs.get(x))|pairMask(inPairs.get(y));
                        if(Integer.bitCount(addPairs)!=4)continue;
                        for(int sin:mir){
                            int ib=1<<(sin-1);if((addPairs&ib)!=0)continue;
                            int add=addPairs|ib;
                            int game=(m.lastDraw&~rem)|add;
                            if(Integer.bitCount(game)!=15)continue;
                            offer(pq,scoreDraw(m,game,focusPerimeter,threshold),topK);
                        }
                    }
                }
            }
        }
        ArrayList<Candidate>out=new ArrayList<>(pq);out.sort((x,y)->Double.compare(y.score,x.score));return out;
    }
    private static int pairMask(PairStat p){return (1<<(p.a-1))|(1<<(p.b-1));}

    public static List<Candidate> module5NumberTrend(Model m,int topK,boolean focusPerimeter,int threshold){
        List<DecadeTrend> r=rankNumbers(m);
        int[] pool=new int[Math.min(20,r.size())];
        for(int i=0;i<pool.length;i++)pool[i]=r.get(i).n;
        PriorityQueue<Candidate>pq=new PriorityQueue<>(Comparator.comparingDouble(c->c.score));
        long[]done={0};
        combineScoreDraw(pool,15,0,0,0,m,pq,topK,focusPerimeter,threshold,null,done,1);
        ArrayList<Candidate>out=new ArrayList<>(pq);out.sort((a,b)->Double.compare(b.score,a.score));return out;
    }

    private static void offer(PriorityQueue<Candidate>pq,Candidate c,int k){
        if(pq.size()<k)pq.offer(c);else if(c.score>pq.peek().score){pq.poll();pq.offer(c);}
    }

    public static List<Integer> combinationMasks(int[]values,int k){
        ArrayList<Integer>out=new ArrayList<>();
        if(k<0||k>values.length)return out;
        if(k==0){out.add(0);return out;}
        combine(values,k,0,0,0,out);return out;
    }
    private static void combine(int[]v,int k,int idx,int chosen,int mask,List<Integer>out){
        if(chosen==k){out.add(mask);return;}
        int need=k-chosen;
        for(int i=idx;i<=v.length-need;i++)combine(v,k,i+1,chosen+1,mask|(1<<(v[i]-1)),out);
    }

    public static int countEven(int mask){int c=0;for(int n=2;n<=25;n+=2)if((mask&(1<<(n-1)))!=0)c++;return c;}
    public static int[] rows(int mask){int[]r=new int[5];for(int n=1;n<=25;n++)if((mask&(1<<(n-1)))!=0)r[(n-1)/5]++;return r;}
    public static int[] cols(int mask){int[]c=new int[5];for(int n=1;n<=25;n++)if((mask&(1<<(n-1)))!=0)c[(n-1)%5]++;return c;}

    public static int[] parseDrawLine(String line){
        if(line==null)return null;
        String[] parts=line.trim().split("[^0-9]+");
        ArrayList<Integer>vals=new ArrayList<>();
        for(String p:parts)if(!p.isEmpty())try{vals.add(Integer.parseInt(p));}catch(Exception ignored){}
        for(int start=vals.size()-15;start>=0;start--){
            int[]a=new int[15];boolean[]seen=new boolean[26];boolean ok=true;
            for(int j=0;j<15;j++){
                int n=vals.get(start+j);
                if(n<1||n>25||seen[n]){ok=false;break;}
                seen[n]=true;a[j]=n;
            }
            if(ok){Arrays.sort(a);return a;}
        }
        return null;
    }

    public static final class PairFailureRank {
        public final int a,b;
        public final long score;
        public final int bothFail,partialFail,bothHit;
        public final int currentBothFailGap;
        public PairFailureRank(int a,int b,long score,int bothFail,int partialFail,int bothHit,int currentBothFailGap){
            this.a=a;this.b=b;this.score=score;this.bothFail=bothFail;this.partialFail=partialFail;
            this.bothHit=bothHit;this.currentBothFailGap=currentBothFailGap;
        }
        public String pair(){
            return String.format(Locale.US,"%02d-%02d",a,b);
        }
    }

    public static final class RepeatTrend {
        public final int repeats;
        public final int historical;
        public final int recent;
        public final double score;
        public RepeatTrend(int repeats,int historical,int recent,double score){
            this.repeats=repeats;this.historical=historical;this.recent=recent;this.score=score;
        }
    }

    public static final class Module7Result {
        public final int chosenRepeats;
        public final int gameMask;
        public final int[] removedFromLast;
        public final int[] keptFailingMirror;
        public final List<PairFailureRank> ranking;
        public final List<RepeatTrend> repeatRanking;
        public final String detail;
        Module7Result(int chosenRepeats,int gameMask,int[] removedFromLast,int[] keptFailingMirror,
                      List<PairFailureRank> ranking,List<RepeatTrend> repeatRanking,String detail){
            this.chosenRepeats=chosenRepeats;this.gameMask=gameMask;this.removedFromLast=removedFromLast;
            this.keptFailingMirror=keptFailingMirror;this.ranking=ranking;this.repeatRanking=repeatRanking;this.detail=detail;
        }
    }

    // Ranking-base das 300 duplas: termina três concursos antes do último carregado.
    // Falha das duas juntas = +100; falha de apenas uma = +1.
    public static List<PairFailureRank> rankFailurePairsThreeBefore(Model m){
        int endExclusive=Math.max(1,m.draws.length-3);
        ArrayList<PairFailureRank> out=new ArrayList<>();

        for(int a=1;a<=25;a++){
            for(int b=a+1;b<=25;b++){
                int bothFail=0,partial=0,bothHit=0,lastBoth=-1;
                long score=0;

                for(int i=0;i<endExclusive;i++){
                    boolean ha=(m.draws[i]&(1<<(a-1)))!=0;
                    boolean hb=(m.draws[i]&(1<<(b-1)))!=0;

                    if(!ha&&!hb){score+=100;bothFail++;lastBoth=i;}
                    else if(ha!=hb){score+=1;partial++;}
                    else bothHit++;
                }

                int gap=lastBoth<0?endExclusive:endExclusive-1-lastBoth;
                out.add(new PairFailureRank(a,b,score,bothFail,partial,bothHit,gap));
            }
        }

        out.sort((x,y)->{
            int c=Long.compare(y.score,x.score);
            if(c!=0)return c;
            c=Integer.compare(y.currentBothFailGap,x.currentBothFailGap);
            if(c!=0)return c;
            return x.pair().compareTo(y.pair());
        });

        return out;
    }

    // Tendência de repetição: histórico até três antes + validação/força dos três concursos finais.
    public static List<RepeatTrend> rankRepeatTrend(Model m){
        int[] hist=new int[16],recent=new int[16];
        int split=Math.max(1,m.draws.length-3);

        for(int i=1;i<m.draws.length;i++){
            int r=Integer.bitCount(m.draws[i]&m.draws[i-1]);
            if(i<split)hist[r]++; else recent[r]++;
        }

        ArrayList<RepeatTrend> out=new ArrayList<>();
        for(int r=5;r<=15;r++){
            double sc=hist[r]*0.12+recent[r]*8.0;
            // pequena preferência por faixas centrais sem forçar resultado
            if(r>=8&&r<=11)sc+=0.5;
            out.add(new RepeatTrend(r,hist[r],recent[r],sc));
        }

        out.sort((a,b)->Double.compare(b.score,a.score));
        return out;
    }

    private static double pairValue(List<PairFailureRank> rank,int a,int b){
        int x=Math.min(a,b),y=Math.max(a,b);
        for(PairFailureRank p:rank){
            if(p.a==x&&p.b==y){
                // score histórico + bônus moderado de atraso desde a última falha conjunta
                return p.score + Math.min(25,p.currentBothFailGap)*2.0;
            }
        }
        return 0;
    }

    private static int[] bestFailureSetFromPool(int poolMask,int need,List<PairFailureRank> rank){
        int[] pool=numsOf(poolMask);
        if(need<=0)return new int[0];

        // Universo pequeno: escolhe a combinação de "need" que maximiza a soma das duplas internas.
        int total=1<<pool.length;
        double best=-1;
        int bestMask=0;

        for(int sub=0;sub<total;sub++){
            if(Integer.bitCount(sub)!=need)continue;
            double s=0;
            for(int i=0;i<pool.length;i++)if((sub&(1<<i))!=0){
                for(int j=i+1;j<pool.length;j++)if((sub&(1<<j))!=0){
                    s+=pairValue(rank,pool[i],pool[j]);
                }
            }
            if(s>best){best=s;bestMask=sub;}
        }

        int[] out=new int[need];int k=0;
        for(int i=0;i<pool.length;i++)if((bestMask&(1<<i))!=0)out[k++]=pool[i];
        Arrays.sort(out);
        return out;
    }

    public static Module7Result module7FailurePairTrend(Model m){
        List<PairFailureRank> pairRank=rankFailurePairsThreeBefore(m);
        List<RepeatTrend> repeatRank=rankRepeatTrend(m);

        int repeats=repeatRank.isEmpty()?10:repeatRank.get(0).repeats;
        // Para o módulo de troca por duplas, trabalha de forma prática entre 9 e 10 repetidas,
        // escolhendo qual das duas tendências está mais forte no ranking atual.
        double s9=-1,s10=-1;
        for(RepeatTrend r:repeatRank){
            if(r.repeats==9)s9=r.score;
            if(r.repeats==10)s10=r.score;
        }
        repeats=s9>s10?9:10;

        int removeCount=15-repeats; // 9 repetidas -> tira 6; 10 -> tira 5
        int enterCount=removeCount;
        int mirrorMask=FULL_MASK^m.lastDraw;

        int[] remove=bestFailureSetFromPool(m.lastDraw,removeCount,pairRank);

        // No espelho, o que NÃO entra continua falhando:
        // 9 repetidas -> 4 falhas no espelho; 10 -> 5 falhas no espelho.
        int keepFailMirror=10-enterCount;
        int[] mirrorFail=bestFailureSetFromPool(mirrorMask,keepFailMirror,pairRank);

        int removeMask=maskOf(remove);
        int mirrorFailMask=maskOf(mirrorFail);
        int enterMask=mirrorMask^mirrorFailMask;
        int game=(m.lastDraw^removeMask)|enterMask;

        // Segurança: jogo final sempre com 15.
        if(Integer.bitCount(game)!=15){
            throw new IllegalStateException("Montagem do Módulo 7 não fechou 15 dezenas.");
        }

        StringBuilder d=new StringBuilder();
        d.append("TENDÊNCIA ESCOLHIDA: ").append(repeats).append(" repetidas\n");
        d.append("Sai do último resultado: ").append(Arrays.toString(remove)).append("\n");
        d.append("Continua falhando no espelho: ").append(Arrays.toString(mirrorFail)).append("\n");
        d.append("Troca: ").append(removeCount).append(" sai / ").append(enterCount).append(" entra\n");
        d.append("Score das duplas: falha conjunta +100; falha parcial +1.\n");
        d.append("Ranking-base fechado 3 concursos antes; os 3 finais pesam na tendência de repetição.\n");

        return new Module7Result(repeats,game,remove,mirrorFail,pairRank,repeatRank,d.toString());
    }

    public static int sumMask(int mask){
        int s=0;for(int n:numsOf(mask))s+=n;return s;
    }

    public static String patternSummary(int mask,int previousDraw){
        int border=Integer.bitCount(mask&BORDER_MASK);
        int center=Integer.bitCount(mask&CENTER_MASK);
        int cross=Integer.bitCount(mask&CROSS_MASK);
        int prime=Integer.bitCount(mask&PRIME_MASK);
        int fib=Integer.bitCount(mask&FIB_MASK);
        int even=countEven(mask);
        int repeat=Integer.bitCount(mask&previousDraw);
        int sum=sumMask(mask);
        int[] rr=rows(mask),cc=cols(mask);

        return "Soma="+sum+
               " • Repetidas="+repeat+
               " • Borda="+border+
               " • Miolo="+center+
               " • Cruz="+cross+
               " • Primos="+prime+
               " • Fibonacci="+fib+
               " • Pares="+even+
               " • Linhas="+Arrays.toString(rr)+
               " • Colunas="+Arrays.toString(cc);
    }


    public static final class BacktestResult {
        public final int tests,best; public final int[] hits; public final double average;
        BacktestResult(int tests,int best,int[] hits,double average){this.tests=tests;this.best=best;this.hits=hits;this.average=average;}
        public String summary(){
            StringBuilder s=new StringBuilder("BACKTEST SEM OLHAR O FUTURO\n");
            s.append("Testes: ").append(tests).append(" • média: ").append(String.format(Locale.US,"%.2f",average)).append(" • melhor: ").append(best).append("\n");
            for(int h=11;h<=15;h++)s.append(h).append(" pontos: ").append(hits[h]).append("\n");
            return s.toString();
        }
    }
    public interface HistoricalGenerator { int generate(Model m); }

    private static Model partialModel(Model m,int endExclusive){
        ArrayList<int[]> h=new ArrayList<>();
        for(int i=0;i<endExclusive;i++)h.add(numsOf(m.draws[i]));
        return new Model(h);
    }
    public static BacktestResult backtest(Model m,int maxTests,HistoricalGenerator g){
        int start=Math.max(25,m.draws.length-maxTests),tests=0,best=0; long sum=0; int[] hc=new int[16];
        for(int target=start;target<m.draws.length;target++){
            try{
                Model pm=partialModel(m,target);
                int game=g.generate(pm);
                int hit=Integer.bitCount(game&m.draws[target]);
                hc[hit]++;sum+=hit;best=Math.max(best,hit);tests++;
            }catch(Exception ignored){}
        }
        return new BacktestResult(tests,best,hc,tests==0?0:(double)sum/tests);
    }

    public static int[] neighbors14(int base){
        int[] in=numsOf(base),out=numsOf(FULL_MASK^base),a=new int[150];int k=0;
        for(int x:in)for(int y:out)a[k++]=(base^(1<<(x-1)))|(1<<(y-1));
        return a;
    }
    private static double hi(Model m,int mask,int from){
        double s=0;
        for(int i=Math.max(0,from);i<m.draws.length;i++){
            int h=Integer.bitCount(mask&m.draws[i]);
            if(h==15)s+=40; else if(h==14)s+=18; else if(h==13)s+=7; else if(h==12)s+=2.5; else if(h==11)s+=.7;
        }
        return s;
    }
    private static int trendBase(Model m){
        List<Candidate> c=module5NumberTrend(m,1,true,11);
        return c.isEmpty()?m.lastDraw:c.get(0).mask;
    }
    public static Candidate module8TimeSpace(Model m){
        int base=trendBase(m); Candidate best=null; int recent=Math.max(0,m.draws.length-80);
        for(int mask:neighbors14(base)){
            Candidate c=scoreDraw(m,mask,true,11);
            Candidate x=new Candidate(mask,c.score+hi(m,mask,0)*.15+hi(m,mask,recent)*.70,c.perimeterScore,c.patternHits);
            if(best==null||x.score>best.score)best=x;
        } return best;
    }
    public static Candidate module9Neighbors150(Model m){
        int base=trendBase(m); Candidate best=null; int recent=Math.max(0,m.draws.length-50);
        for(int mask:neighbors14(base)){
            Candidate c=scoreDraw(m,mask,true,11);
            Candidate x=new Candidate(mask,c.score+hi(m,mask,0)*.08+hi(m,mask,recent),c.perimeterScore,c.patternHits);
            if(best==null||x.score>best.score)best=x;
        } return best;
    }

    public static final class ClosureResult {
        public final int[] universe; public final List<Candidate> tickets; public final String detail;
        ClosureResult(int[]u,List<Candidate>t,String d){universe=u;tickets=t;detail=d;}
    }
    public static ClosureResult module10SmartClosure(Model m,int universeSize,int ticketCount){
        universeSize=Math.max(16,Math.min(20,universeSize));ticketCount=Math.max(3,Math.min(30,ticketCount));
        List<DecadeTrend> rank=rankNumbers(m); int[] universe=new int[universeSize];
        for(int i=0;i<universeSize;i++)universe[i]=rank.get(i).n; Arrays.sort(universe);
        ArrayList<Candidate> pool=new ArrayList<>();
        for(int sub=0;sub<(1<<universeSize);sub++){
            if(Integer.bitCount(sub)!=15)continue;int mask=0;
            for(int i=0;i<universeSize;i++)if((sub&(1<<i))!=0)mask|=1<<(universe[i]-1);
            pool.add(scoreDraw(m,mask,true,11));
        }
        pool.sort((a,b)->Double.compare(b.score,a.score));
        ArrayList<Candidate> chosen=new ArrayList<>();
        for(Candidate c:pool){
            boolean near=false;for(Candidate x:chosen)if(Integer.bitCount(c.mask&x.mask)>=14){near=true;break;}
            if(!near)chosen.add(c);if(chosen.size()>=ticketCount)break;
        }
        return new ClosureResult(universe,chosen,"Universo forte: "+Arrays.toString(universe)+"\nCartões diversificados: "+chosen.size());
    }
    public static Candidate module11ChampionOfChampions(Model m){
        ArrayList<Integer> masks=new ArrayList<>();
        masks.add(trendBase(m));
        masks.add(module7FailurePairTrend(m).gameMask);
        masks.add(module8TimeSpace(m).mask);
        masks.add(module9Neighbors150(m).mask);
        ClosureResult cl=module10SmartClosure(m,18,5); if(!cl.tickets.isEmpty())masks.add(cl.tickets.get(0).mask);

        double[] vote=new double[26];
        for(int mask:masks)for(int n:numsOf(mask))vote[n]++;
        Integer[] ns=new Integer[25];for(int i=0;i<25;i++)ns[i]=i+1;
        Arrays.sort(ns,(a,b)->Double.compare(vote[b],vote[a]));
        int base=0;for(int i=0;i<15;i++)base|=1<<(ns[i]-1);
        Candidate best=scoreDraw(m,base,true,11);
        for(int mask:neighbors14(base)){
            Candidate c=scoreDraw(m,mask,true,11);double v=0;for(int n:numsOf(mask))v+=vote[n];
            Candidate x=new Candidate(mask,c.score+v*2,c.perimeterScore,c.patternHits);
            if(x.score>best.score)best=x;
        }return best;
    }


    public static final class ManualGameAnalysis {
        public final int mask;
        public final Candidate candidate;
        public final String classification;
        public final String detail;
        ManualGameAnalysis(int mask,Candidate candidate,String classification,String detail){
            this.mask=mask;this.candidate=candidate;this.classification=classification;this.detail=detail;
        }
    }

    public static ManualGameAnalysis analyzeManualGame(Model m,int mask){
        if(Integer.bitCount(mask)!=15)throw new IllegalArgumentException("O jogo precisa ter exatamente 15 dezenas.");
        Candidate c=scoreDraw(m,mask,true,11);

        int sum=sumMask(mask);
        int repeats=Integer.bitCount(mask&m.lastDraw);
        int border=Integer.bitCount(mask&BORDER_MASK);
        int center=Integer.bitCount(mask&CENTER_MASK);
        int cross=Integer.bitCount(mask&CROSS_MASK);
        int prime=Integer.bitCount(mask&PRIME_MASK);
        int fib=Integer.bitCount(mask&FIB_MASK);
        int even=countEven(mask);

        int grade=0;
        if(sum>=190&&sum<=220)grade+=2;
        if(repeats>=8&&repeats<=11)grade+=2;
        if(border>=9&&border<=11)grade+=2;
        if(center>=4&&center<=6)grade++;
        if(cross>=4&&cross<=7)grade++;
        if(prime>=4&&prime<=7)grade++;
        if(fib>=3&&fib<=6)grade++;
        if(even>=6&&even<=9)grade++;
        grade+=Math.min(3,c.patternHits/2);

        String cls;
        if(grade>=12)cls="PADRÃO MUITO FORTE";
        else if(grade>=9)cls="PADRÃO FORTE";
        else if(grade>=6)cls="PADRÃO MÉDIO";
        else cls="FORA DO PADRÃO PRINCIPAL";

        String detail=
            "CLASSIFICAÇÃO: "+cls+"\n"+
            patternSummary(mask,m.lastDraw)+"\n"+
            "Perímetro: "+String.format(Locale.US,"%.0f%%",c.perimeterScore*100)+"\n"+
            "Score geral: "+String.format(Locale.US,"%.2f",c.score)+"\n"+
            "Critérios fortes: "+c.patternHits;

        return new ManualGameAnalysis(mask,c,cls,detail);
    }

    public static final class SimilarResult {
        public final int sourceContestIndex;
        public final int sourceMask;
        public final int distance;
        public final Candidate evolved;
        public final double evolutionScore;
        SimilarResult(int sourceContestIndex,int sourceMask,int distance,Candidate evolved,double evolutionScore){
            this.sourceContestIndex=sourceContestIndex;this.sourceMask=sourceMask;this.distance=distance;
            this.evolved=evolved;this.evolutionScore=evolutionScore;
        }
    }

    public static List<SimilarResult> module13SimilarityEvolution(Model m,int topK){
        ArrayList<SimilarResult> out=new ArrayList<>();
        int target=m.lastDraw;

        for(int i=0;i<m.draws.length-1;i++){
            int common=Integer.bitCount(target&m.draws[i]);
            int distance=15-common;
            if(distance<1||distance>3)continue;

            int lookEnd=Math.min(m.draws.length,i+6);
            double evo=0;
            int bestFuture=m.draws[i+1];
            double bestFutureScore=-1;

            for(int j=i+1;j<lookEnd;j++){
                int hit=Integer.bitCount(m.draws[j]&target);
                double local=0;
                if(hit>=14)local+=25;
                else if(hit==13)local+=12;
                else if(hit==12)local+=5;
                else if(hit==11)local+=2;
                else local+=hit*.15;

                Candidate cf=scoreDraw(m,m.draws[j],true,11);
                local+=cf.score*.10;
                evo+=local;

                if(local>bestFutureScore){
                    bestFutureScore=local;
                    bestFuture=m.draws[j];
                }
            }

            int consensus=(m.draws[i]&bestFuture)|(target&bestFuture)|(target&m.draws[i]);
            int[] ranked=numsOf(consensus);
            int base=target;

            if(ranked.length>=15){
                double[] w=new double[26];
                for(int n:ranked){
                    if((target&(1<<(n-1)))!=0)w[n]+=3;
                    if((m.draws[i]&(1<<(n-1)))!=0)w[n]+=2;
                    if((bestFuture&(1<<(n-1)))!=0)w[n]+=4;
                    w[n]+=m.hitFreqRecent[n]*0.05;
                }
                Integer[] ns=new Integer[ranked.length];
                for(int k=0;k<ranked.length;k++)ns[k]=ranked[k];
                Arrays.sort(ns,(a,b)->Double.compare(w[b],w[a]));
                base=0;
                for(int k=0;k<15;k++)base|=1<<(ns[k]-1);
            }

            Candidate best=scoreDraw(m,base,true,11);
            for(int mask:neighbors14(base)){
                Candidate c=scoreDraw(m,mask,true,11);
                if(c.score>best.score)best=c;
            }

            out.add(new SimilarResult(i,m.draws[i],distance,best,evo+best.score));
        }

        out.sort((a,b)->Double.compare(b.evolutionScore,a.evolutionScore));
        if(out.size()>topK)return new ArrayList<>(out.subList(0,topK));
        return out;
    }

}
