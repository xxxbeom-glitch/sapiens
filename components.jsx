// Components for 브리핑 app — Dark mode (Liquid Lava accent)
const { useState, useEffect, useRef } = React;

// ─── Tokens ─────────────────────────────────────────────
const M3 = {
  surface: 'var(--md-surface)',
  surfaceContainer: 'var(--md-surface-container)',
  surfaceContainerHigh: 'var(--md-surface-container-high)',
  surfaceContainerLow: 'var(--md-surface-container-low)',
  surfaceContainerLowest: 'var(--md-surface-container-lowest)',
  onSurface: 'var(--md-on-surface)',
  onSurfaceVar: 'var(--md-on-surface-var)',
  outline: 'var(--md-outline)',
  outlineVar: 'var(--md-outline-variant)',
  primary: 'var(--md-primary)',
  onPrimary: 'var(--md-on-primary)',
  primaryContainer: 'var(--md-primary-container)',
  onPrimaryContainer: 'var(--md-on-primary-container)',
  secondaryContainer: 'var(--md-secondary-container)',
  onSecondaryContainer: 'var(--md-on-secondary-container)',
};

// Concrete colors (for places that can't use CSS vars nicely)
const C = {
  page:       '#151419',  // Dark Void
  card:       '#1B1B1E',  // Gluon Grey
  elevated:   '#262626',  // Slate Grey
  ink:        '#FBFBFB',  // Snow
  mute:       '#878787',  // Dusty Grey
  accent:     '#F56E0F',  // Liquid Lava
  accentSoft: 'rgba(245,110,15,0.14)',
  hair:       'rgba(255,255,255,0.08)',
  hair2:      'rgba(255,255,255,0.14)',
  up:         '#FF3B30',
  down:       '#0064FF',
  flat:       '#878787',
};

const M3_TYPE = {
  displaySmall:  { fontSize:36, fontWeight:700, lineHeight:'44px', letterSpacing:0 },
  headlineLarge: { fontSize:32, fontWeight:700, lineHeight:'40px', letterSpacing:0 },
  headlineMedium:{ fontSize:28, fontWeight:700, lineHeight:'36px', letterSpacing:0 },
  headlineSmall: { fontSize:24, fontWeight:700, lineHeight:'32px', letterSpacing:0 },
  titleLarge:    { fontSize:22, fontWeight:600, lineHeight:'28px', letterSpacing:0 },
  titleMedium:   { fontSize:16, fontWeight:600, lineHeight:'24px', letterSpacing:'0.15px' },
  titleSmall:    { fontSize:14, fontWeight:600, lineHeight:'20px', letterSpacing:'0.1px' },
  bodyLarge:     { fontSize:16, fontWeight:500, lineHeight:'24px', letterSpacing:'0.15px' },
  bodyMedium:    { fontSize:14, fontWeight:500, lineHeight:'20px', letterSpacing:'0.25px' },
  bodySmall:     { fontSize:12, fontWeight:500, lineHeight:'16px', letterSpacing:'0.4px' },
  labelLarge:    { fontSize:14, fontWeight:600, lineHeight:'20px', letterSpacing:'0.1px' },
  labelMedium:   { fontSize:12, fontWeight:500, lineHeight:'16px', letterSpacing:'0.5px' },
  labelSmall:    { fontSize:11, fontWeight:500, lineHeight:'16px', letterSpacing:'0.5px' },
};

// ─── Tweaks hook ────────────────────────────────────────
function useTweaks() {
  const [t, setT] = useState(() => window.__tweaks || {});
  useEffect(() => {
    const h = (e) => setT({ ...e.detail });
    window.addEventListener('tweakschange', h);
    return () => window.removeEventListener('tweakschange', h);
  }, []);
  return t;
}

function densityPad(d) {
  if (d === 'compact') return 12;
  if (d === 'spacious') return 20;
  return 16;
}

// Card variants — dark surface
function cardShell(style) {
  if (style === 'outlined') return {
    background: C.card,
    border: `1px solid ${C.hair2}`,
    boxShadow: 'none',
  };
  if (style === 'flat') return {
    background: C.card,
    border: 'none',
    boxShadow: 'none',
  };
  // elevated — subtle lift on dark
  return {
    background: C.card,
    border: 'none',
    boxShadow: '0 1px 0 rgba(255,255,255,0.03) inset, 0 2px 6px rgba(0,0,0,0.35)',
  };
}

// ─── Icons (inline SVG) ─────────────────────────────────
function Icon({ name, size=24, color='currentColor', strokeWidth=1.8 }) {
  const props = { width:size, height:size, viewBox:'0 0 24 24', fill:'none',
    stroke:color, strokeWidth, strokeLinecap:'round', strokeLinejoin:'round' };
  switch (name) {
    case 'chevron-right':
      return <svg {...props}><path d="M9 6l6 6-6 6"/></svg>;
    case 'chevron-left':
      return <svg {...props}><path d="M15 6l-6 6 6 6"/></svg>;
    case 'arrow-forward':
      return <svg {...props}><path d="M5 12h14M13 5l7 7-7 7"/></svg>;
    case 'more-vert':
      return <svg {...props} fill={color} stroke="none"><circle cx="12" cy="5" r="1.6"/><circle cx="12" cy="12" r="1.6"/><circle cx="12" cy="19" r="1.6"/></svg>;
    case 'search':
      return <svg {...props}><circle cx="11" cy="11" r="7"/><path d="M20 20l-3.5-3.5"/></svg>;
    case 'notifications':
      return <svg {...props}><path d="M6 16V11a6 6 0 1 1 12 0v5l1.5 2.5h-15L6 16z"/><path d="M10 20a2 2 0 0 0 4 0"/></svg>;
    case 'feed':
      return <svg {...props}><rect x="4" y="4" width="16" height="16" rx="2"/><path d="M8 9h8M8 13h8M8 17h5"/></svg>;
    case 'article':
      return <svg {...props}><rect x="4" y="3" width="16" height="18" rx="2"/><path d="M8 7h8M8 11h8M8 15h5"/></svg>;
    case 'trending':
      return <svg {...props}><path d="M3 17l6-6 4 4 8-8"/><path d="M14 7h7v7"/></svg>;
    case 'bookmark':
      return <svg {...props}><path d="M6 4h12v17l-6-4-6 4V4z"/></svg>;
    case 'open-in-new':
      return <svg {...props}><path d="M14 4h6v6M10 14L20 4M18 14v6H4V6h6"/></svg>;
    default: return null;
  }
}

// ─── Top App Bar ────────────────────────────────────────
function TopAppBar({ title, date }) {
  return (
    <div style={{
      height: 56, display:'flex', alignItems:'center',
      padding:'0 8px 0 20px',
      background: 'rgba(21,20,25,0.78)',
      backdropFilter:'blur(20px) saturate(180%)',
      WebkitBackdropFilter:'blur(20px) saturate(180%)',
      position:'relative', zIndex:2,
      borderBottom:`0.5px solid ${C.hair}`,
    }}>
      <div style={{
        flex:1, color: C.ink,
        fontSize:22, fontWeight:700, letterSpacing:'-0.02em',
        fontFamily:"'SUIT',system-ui,sans-serif",
      }}>{title}</div>
      <div className="mono" style={{
        fontSize:12, fontWeight:500, color:C.mute, paddingRight:8,
      }}>{date}</div>
      <button style={{
        width:40, height:40, borderRadius:'50%', background:'transparent',
        border:'none', cursor:'pointer',
        display:'flex', alignItems:'center', justifyContent:'center',
        color: C.accent,
      }}>
        <Icon name="more-vert" size={20} />
      </button>
    </div>
  );
}

// ─── Section header ─────────────────────────────────────
function SectionLabel({ label, subtitle }) {
  return (
    <div style={{ padding:'24px 20px 10px' }}>
      <div style={{
        fontSize:12, fontWeight:600, color: C.mute,
        textTransform:'uppercase', letterSpacing:'0.06em',
        fontFamily:"'SUIT',system-ui,sans-serif",
      }}>{label}</div>
      {subtitle && (
        <div style={{
          fontSize:14, color: C.ink, marginTop:6,
          fontFamily:"'SUIT',system-ui,sans-serif", fontWeight:500,
          opacity:0.85,
        }}>{subtitle}</div>
      )}
    </div>
  );
}

// ─── Source chip — Liquid Lava tint ─────────────────────
function SourceChip({ label }) {
  return (
    <span style={{
      display:'inline-flex', alignItems:'center', gap:5,
      height:22, padding:'0 9px', borderRadius:6,
      background: C.accentSoft,
      color: C.accent,
      fontSize:11, fontWeight:600, letterSpacing:'-0.01em',
      fontFamily:"'SUIT',system-ui,sans-serif",
    }}>
      <span style={{ width:5, height:5, borderRadius:'50%', background: C.accent }} />
      {label}
    </span>
  );
}

// ─── Morning briefing — swipeable cards ─────────────────
function MorningCard({ articles, onOpen }) {
  const tw = useTweaks();
  const [idx, setIdx] = useState(0);
  const pad = densityPad(tw.density);
  const shell = cardShell(tw.cardStyle);

  const onScroll = (e) => {
    const w = e.currentTarget.clientWidth;
    const i = Math.round(e.currentTarget.scrollLeft / w);
    if (i !== idx) setIdx(i);
  };

  return (
    <div>
      <div onScroll={onScroll} className="noscroll" style={{
        display:'flex', overflowX:'auto', scrollSnapType:'x mandatory',
        padding:'0 16px',
      }}>
        {articles.map((a, i) => (
          <div key={i} style={{
            flex:'0 0 100%', scrollSnapAlign:'start', boxSizing:'border-box',
            paddingRight: i === articles.length - 1 ? 0 : 6,
            paddingLeft: i === 0 ? 0 : 6,
          }}>
            <button
              className="press"
              onClick={() => onOpen && onOpen(a)}
              style={{
                width:'100%', textAlign:'left', cursor:'pointer',
                ...shell, borderRadius:18, padding:pad+2,
                display:'flex', flexDirection:'column', gap:12,
                fontFamily:'inherit',
              }}>
              <div style={{ display:'flex', alignItems:'center', justifyContent:'space-between' }}>
                <SourceChip label={a.source} />
                <span className="mono" style={{
                  ...M3_TYPE.labelSmall, color: C.mute, letterSpacing:'0.08em',
                }}>{a.tag}</span>
              </div>
              <div style={{
                ...M3_TYPE.titleMedium, color: C.ink,
                fontFamily:"'SUIT',system-ui,sans-serif",
                fontWeight:700, letterSpacing:'-0.01em', lineHeight:1.35,
                display:'-webkit-box', WebkitLineClamp:2, WebkitBoxOrient:'vertical', overflow:'hidden',
              }}>
                {a.headline}
              </div>
              <div style={{
                ...M3_TYPE.bodyMedium, color: C.mute,
                fontFamily:"'SUIT',system-ui,sans-serif",
                lineHeight:1.55,
                display:'-webkit-box', WebkitLineClamp:2, WebkitBoxOrient:'vertical', overflow:'hidden',
              }}>
                {a.summary}
              </div>
              <div style={{ height:'0.5px', background: C.hair }} />
              <div style={{ display:'flex', alignItems:'center', justifyContent:'space-between' }}>
                <div style={{ ...M3_TYPE.labelMedium, color: C.mute,
                  fontFamily:"'SUIT',system-ui,sans-serif" }}>
                  {a.source} · <span className="mono">{a.time}</span>
                </div>
                <Icon name="chevron-right" size={20} color={C.mute} />
              </div>
            </button>
          </div>
        ))}
      </div>
      {tw.showIndicators && (
        <div style={{ display:'flex', justifyContent:'center', gap:6, paddingTop:14 }}>
          {articles.map((_, i) => (
            <div key={i} style={{
              height:6, width: i === idx ? 20 : 6, borderRadius:999,
              background: i === idx ? C.accent : C.hair2,
              transition:'all 0.22s ease',
            }} />
          ))}
        </div>
      )}
    </div>
  );
}

// ─── US market card ─────────────────────────────────────
function USMarketCard({ indicators, report, articles, onArticle }) {
  const tw = useTweaks();
  const shell = cardShell(tw.cardStyle);
  return (
    <div style={{ margin:'0 16px', ...shell, borderRadius:18, overflow:'hidden' }}>

      {/* Indicators */}
      <div style={{ padding:'16px 16px 4px' }}>
        <div style={{ display:'flex', alignItems:'baseline', justifyContent:'space-between', marginBottom:6 }}>
          <div style={{ ...M3_TYPE.titleSmall, color: C.ink,
            fontFamily:"'SUIT',system-ui,sans-serif",
            fontWeight:700, letterSpacing:'-0.01em' }}>시장 지표</div>
          <div className="mono" style={{ ...M3_TYPE.labelSmall, color: C.mute }}>
            4/18 장 마감 기준
          </div>
        </div>
        <div>
          {indicators.map((r, i) => (
            <div key={r.name} style={{
              display:'grid', gridTemplateColumns:'1.1fr 1.2fr 0.8fr',
              alignItems:'center', padding:'12px 0',
              borderBottom: i < indicators.length - 1 ? `0.5px solid ${C.hair}` : 'none',
            }}>
              <div style={{ ...M3_TYPE.bodyLarge, color: C.ink, fontWeight:500,
                fontFamily:"'SUIT',system-ui,sans-serif" }}>{r.name}</div>
              <div className="mono" style={{
                ...M3_TYPE.bodyMedium, color: C.ink, fontWeight:500, opacity:0.9,
                textAlign:'right', paddingRight:10,
              }}>{r.value}</div>
              <div className="mono" style={{
                ...M3_TYPE.labelLarge, textAlign:'right', fontWeight:600,
                color: r.dir === 'up' ? C.up : (r.dir === 'down' ? C.down : C.flat),
              }}>{r.change}</div>
            </div>
          ))}
        </div>
      </div>

      <div style={{ height:'0.5px', background: C.hair, margin:'4px 16px' }} />

      {/* Report */}
      <div style={{ padding:'16px' }}>
        <div style={{ display:'flex', alignItems:'baseline', justifyContent:'space-between', marginBottom:10 }}>
          <div style={{ ...M3_TYPE.titleSmall, color: C.ink,
            fontFamily:"'SUIT',system-ui,sans-serif",
            fontWeight:700, letterSpacing:'-0.01em' }}>미국 시황 리포트</div>
          <div className="mono" style={{ ...M3_TYPE.labelSmall, color: C.mute }}>
            {report.date}
          </div>
        </div>
        <div style={{
          ...M3_TYPE.bodyMedium, color: C.mute,
          fontFamily:"'SUIT',system-ui,sans-serif",
          lineHeight:1.65,
        }}>{report.body}</div>
      </div>

      <div style={{ height:'0.5px', background: C.hair, margin:'0 16px' }} />

      {/* Article list */}
      <div style={{ padding:'14px 16px 4px' }}>
        <div style={{ ...M3_TYPE.titleSmall, color: C.ink,
          fontFamily:"'SUIT',system-ui,sans-serif",
          fontWeight:700, letterSpacing:'-0.01em', marginBottom:2 }}>
          주요 기사
        </div>
      </div>
      <div style={{ padding:'0 4px 8px' }}>
        {articles.map((a, i) => (
          <button key={i}
            className="press"
            onClick={() => onArticle && onArticle(a)}
            style={{
              width:'100%', textAlign:'left', cursor:'pointer',
              background:'transparent', border:'none',
              padding:'14px 12px',
              display:'flex', alignItems:'center', justifyContent:'space-between', gap:12,
              borderBottom: i < articles.length - 1 ? `0.5px solid ${C.hair}` : 'none',
              fontFamily:'inherit', color: C.ink,
            }}>
            <div style={{
              ...M3_TYPE.bodyLarge, color: C.ink,
              fontFamily:"'SUIT',system-ui,sans-serif",
              lineHeight:1.4, letterSpacing:'-0.01em', fontWeight:500,
              display:'-webkit-box', WebkitLineClamp:1, WebkitBoxOrient:'vertical', overflow:'hidden',
              flex:1,
            }}>{a.title}</div>
            <Icon name="chevron-right" size={20} color={C.mute} />
          </button>
        ))}
      </div>
    </div>
  );
}

// ─── Bottom sheet ───────────────────────────────────────
function BottomSheet({ open, onClose, article }) {
  if (!open || !article) return null;
  return (
    <div style={{
      position:'absolute', inset:0, zIndex:100,
      display:'flex', flexDirection:'column', justifyContent:'flex-end',
      animation:'fadeIn 0.18s ease',
    }}>
      <div onClick={onClose} style={{ position:'absolute', inset:0, background:'rgba(0,0,0,0.55)' }} />
      <div style={{
        position:'relative', background: C.card,
        borderRadius:'20px 20px 0 0',
        padding:'10px 22px 36px',
        maxHeight:'78%', overflow:'auto',
        animation:'sheetIn 0.32s cubic-bezier(0.2, 0.8, 0.2, 1)',
        boxShadow:'0 -1px 0 rgba(255,255,255,0.04), 0 -12px 40px rgba(0,0,0,0.6)',
      }}>
        <div style={{
          width:36, height:5, background: C.hair2,
          borderRadius:999, margin:'0 auto 18px',
        }} />
        <div style={{ display:'flex', alignItems:'center', gap:8, marginBottom:14 }}>
          <SourceChip label={article.source || 'NEWS'} />
          <span className="mono" style={{ fontSize:11, color: C.mute }}>
            {article.time}
          </span>
        </div>
        <div style={{
          fontSize:22, color: C.ink,
          fontFamily:"'SUIT',system-ui,sans-serif",
          fontWeight:700, letterSpacing:'-0.02em', marginBottom:12, lineHeight:1.3,
        }}>{article.title || article.headline}</div>
        <div style={{
          fontSize:15, color: C.mute,
          fontFamily:"'SUIT',system-ui,sans-serif", fontWeight:500,
          lineHeight:1.55, marginBottom:22, letterSpacing:'-0.01em',
        }}>
          {article.summary || '현지시간 기준 이 기사는 브리핑 AI가 자동 요약한 내용입니다. 원문을 보려면 아래 링크를 통해 언론사 페이지로 이동하세요.'}
        </div>
        <div style={{ display:'flex', gap:10 }}>
          <button style={{
            flex:1, height:46, borderRadius:14, border:'none',
            background: C.elevated, color: C.ink,
            fontSize:15, fontFamily:"'SUIT',system-ui,sans-serif",
            fontWeight:600, cursor:'pointer',
          }}>저장</button>
          <button style={{
            flex:2, height:46, borderRadius:14, border:'none',
            background: C.accent, color:'#FFFFFF',
            fontSize:15, fontFamily:"'SUIT',system-ui,sans-serif",
            fontWeight:600, cursor:'pointer',
            display:'inline-flex', alignItems:'center', justifyContent:'center', gap:6,
          }}>원문 보기<Icon name="arrow-forward" size={16} color="currentColor" strokeWidth={2.2}/></button>
        </div>
      </div>
    </div>
  );
}

// ─── Bottom Navigation Bar ──────────────────────────────
function NavBar({ active, onChange }) {
  const tabs = [
    { id:'briefing', label:'브리핑',     iconOutline:'feed' },
    { id:'news',     label:'뉴스',        iconOutline:'article' },
    { id:'watch',    label:'관심종목',    iconOutline:'trending' },
  ];
  return (
    <div style={{
      background:'rgba(21,20,25,0.82)',
      backdropFilter:'blur(22px) saturate(180%)',
      WebkitBackdropFilter:'blur(22px) saturate(180%)',
      display:'flex', height:76, padding:'8px 0 22px',
      borderTop:`0.5px solid ${C.hair}`,
    }}>
      {tabs.map(t => {
        const on = active === t.id;
        return (
          <button key={t.id} onClick={() => onChange(t.id)}
            style={{
              flex:1, background:'transparent', border:'none', cursor:'pointer',
              display:'flex', flexDirection:'column', alignItems:'center',
              gap:3, padding:0, fontFamily:'inherit',
            }}>
            <div style={{
              height:28, display:'flex', alignItems:'center', justifyContent:'center',
            }}>
              <Icon name={t.iconOutline} size={26}
                color={on ? C.accent : C.mute}
                strokeWidth={on ? 2 : 1.6} />
            </div>
            <div style={{
              fontSize:10.5, letterSpacing:'0.01em',
              fontFamily:"'SUIT',system-ui,sans-serif",
              color: on ? C.accent : C.mute,
              fontWeight: on ? 600 : 500,
            }}>{t.label}</div>
          </button>
        );
      })}
    </div>
  );
}

Object.assign(window, {
  M3, C, M3_TYPE, useTweaks, densityPad, cardShell, Icon,
  TopAppBar, SectionLabel, SourceChip, MorningCard, USMarketCard, BottomSheet, NavBar,
});
