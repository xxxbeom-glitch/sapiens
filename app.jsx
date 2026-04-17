// App — 브리핑 (Dark — Liquid Lava)
const { useState: useStateA, useEffect: useEffectA } = React;

function NewsScreen({ onOpen }) {
  const tw = useTweaks();
  const shell = cardShell(tw.cardStyle);
  return (
    <div style={{ padding:'8px 16px 24px' }}>
      <div style={{ ...shell, borderRadius:18, overflow:'hidden' }}>
        {NEWS_FEED.map((n, i) => (
          <button key={i} className="press"
            onClick={() => onOpen({ title:n.title, source:n.source, time:n.time })}
            style={{
              width:'100%', textAlign:'left', cursor:'pointer',
              background:'transparent', border:'none',
              padding:'14px 18px',
              position:'relative',
              display:'flex', flexDirection:'column', gap:6, fontFamily:'inherit',
              color: C.ink,
            }}>
            {i < NEWS_FEED.length - 1 && (
              <div style={{
                position:'absolute', left:18, right:0, bottom:0,
                height:'0.5px', background: C.hair,
              }} />
            )}
            <div style={{ display:'flex', alignItems:'center', gap:8 }}>
              <span style={{
                fontSize:11, padding:'2px 8px', borderRadius:6,
                background: C.elevated, color: C.ink,
                fontFamily:"'SUIT',system-ui,sans-serif", fontWeight:600, letterSpacing:0,
              }}>{n.cat}</span>
              <span className="mono" style={{ fontSize:11, color: C.mute }}>
                {n.time}
              </span>
            </div>
            <div style={{
              fontSize:15.5, color: C.ink,
              fontFamily:"'SUIT',system-ui,sans-serif",
              fontWeight:600, letterSpacing:'-0.01em', lineHeight:1.35,
            }}>{n.title}</div>
            <div style={{ fontSize:12, color: C.mute,
              fontFamily:"'SUIT',system-ui,sans-serif", fontWeight:500 }}>{n.source}</div>
          </button>
        ))}
      </div>
    </div>
  );
}

function WatchScreen() {
  const tw = useTweaks();
  const shell = cardShell(tw.cardStyle);
  return (
    <div style={{ padding:'8px 16px 24px' }}>
      <div style={{ ...shell, borderRadius:18, padding:'2px 18px' }}>
        {WATCHLIST.map((s, i) => (
          <div key={s.ticker} className="press" style={{
            display:'grid', gridTemplateColumns:'auto 1fr auto auto', gap:12,
            alignItems:'center', padding:'14px 0',
            borderBottom: i < WATCHLIST.length - 1
              ? `0.5px solid ${C.hair}` : 'none',
          }}>
            <div className="mono" style={{
              fontSize:10.5, padding:'3px 7px',
              background: C.elevated,
              borderRadius:5, color: C.mute, letterSpacing:'0.02em',
              fontWeight:500,
            }}>{s.ticker}</div>
            <div style={{
              fontSize:15.5, fontWeight:600,
              color: C.ink,
              fontFamily:"'SUIT',system-ui,sans-serif",
              letterSpacing:'-0.01em',
            }}>{s.name}</div>
            <div className="mono" style={{ fontSize:13.5,
              color: C.ink, fontWeight:500, opacity:0.9 }}>
              {s.price}
            </div>
            <div className="mono" style={{
              fontSize:13, fontWeight:600, minWidth:62, textAlign:'right',
              color: s.dir === 'up' ? C.up : (s.dir === 'down' ? C.down : C.flat),
              letterSpacing:'-0.01em',
            }}>{s.change}</div>
          </div>
        ))}
      </div>
    </div>
  );
}

function BriefingScreen({ onOpen }) {
  return (
    <>
      <SectionLabel label="아침 브리핑" subtitle="오늘의 뉴스 · 한국 신문" />
      <MorningCard articles={MORNING_ARTICLES} onOpen={onOpen} />

      <SectionLabel label="미국 장 시황" />
      <USMarketCard
        indicators={MARKET_INDICATORS}
        report={US_REPORT}
        articles={US_ARTICLES}
        onArticle={onOpen}
      />

      <div style={{ height:24 }} />
      <div style={{ textAlign:'center', padding:'0 0 24px' }}>
        <span className="mono" style={{
          fontSize:11, letterSpacing:'0.04em',
          color: C.mute, fontWeight:500,
        }}>AI 요약 · 06:00 업데이트</span>
      </div>
    </>
  );
}

function App() {
  const [tab, setTab] = useStateA(() => localStorage.getItem('briefing:tab') || 'briefing');
  const [sheet, setSheet] = useStateA(null);
  useEffectA(() => { localStorage.setItem('briefing:tab', tab); }, [tab]);

  const titleForTab = { briefing:'브리핑', news:'뉴스', watch:'관심종목' };
  const today = '4월 18일 (토)';

  return (
    <div style={{
      width:412, height:892, borderRadius:48, overflow:'hidden', position:'relative',
      background: C.page,
      border:'10px solid #0A090D',
      boxShadow:'0 50px 100px rgba(0,0,0,0.55), 0 0 0 1px rgba(255,255,255,0.05)',
      boxSizing:'border-box',
    }}>
      <AndroidStatusBar dark={true} />
      <div style={{
        position:'absolute', top:10, left:'50%', transform:'translateX(-50%)',
        width:22, height:22, borderRadius:'50%', background:'#000', zIndex:50,
      }} />

      <TopAppBar title={titleForTab[tab]} date={today} />

      <div style={{
        position:'absolute', top: 40 + 56, bottom: 76, left:0, right:0,
        background: C.page,
      }}>
        <div className="noscroll" style={{ height:'100%', overflowY:'auto', paddingBottom:16 }}>
          {tab === 'briefing' && <BriefingScreen onOpen={setSheet} />}
          {tab === 'news' && <NewsScreen onOpen={setSheet} />}
          {tab === 'watch' && <WatchScreen />}
        </div>
      </div>

      <div style={{ position:'absolute', bottom:0, left:0, right:0, zIndex:40 }}>
        <NavBar active={tab} onChange={setTab} />
      </div>

      <BottomSheet open={!!sheet} onClose={() => setSheet(null)} article={sheet} />
    </div>
  );
}

ReactDOM.createRoot(document.getElementById('root')).render(<App />);
