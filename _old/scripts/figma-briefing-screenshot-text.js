(async () => {
  const W = 360;
  const PAD_H_SECTION = 20;
  const PAD_V_SECTION = 12;
  const CARD_OUT_H = 16;
  const CARD_OUT_BOTTOM = 8;
  const CARD_IN_H = 24;
  const CARD_IN_V = 26;
  const CARD_RADIUS = 18;
  const APPBAR_H = 64;
  const NAV_H = 80;
  const CHIP_PAD_H = 6;
  const CHIP_PAD_V = 2;
  const CHIP_RADIUS = 4;

  const domesticChip = '매일경제';
  const domesticHeadline =
    "호르무즈 해협 개방 하루 만에 다시 봉쇄, 트럼프 '협상 거부 시 이란 ...";
  const domesticRows = [
    '풀무원, 연해주에 여의도 크기 스마트 콩 농장 건설...',
    "K증시 대도약 시대, 국내 최대 규모 투자 콘퍼런스 '...",
    '국내 첫 대학병원 종합 평가, 삼성서울병원이 총점 ...',
    "이란, 호르무즈해협 다시 폐쇄...미국과 '기싸움' 격화",
    '문과 출신 직장인들, 생존을 위해 자격증에 몰려...4...',
    '오세훈 국민의힘 후보 확정, 정원오 민주당 후보와 ...',
    '용산국제업무지구 1만가구 공급 계획, 절반을 주거...',
    '코딩 없이도 AI 에이전트 개발한다...2030년 22억 ...',
    '6월 지방선거 앞두고 건설사들 분양 앞당겨...전국 5...',
  ];

  const overseasChip = 'Finance';
  const overseasHeadline =
    'AI 스타트업 커서, 500억 달러 이상 기업가치로 20억 달러 규모 자금...';
  const overseasRows = [
    '약세 소프트웨어·기술주들이 시장 반등에 동참, 주...',
    '월스트리트, 1990년 이후 최고의 회복력 보여줘...S...',
    '호르무즈 해협 긴장에 유럽 주식 하락·석유·가스 가...',
    "휘발유 가격 급등에 우버·리프트 기사들 '생존 위기'....",
    '트럼프 정부 에너지 장관, 휘발유 가격이 2027년까...',
    '저출산, 높은 부채, AI 기술... 미국이 인구 위기로 향...',
    '대형 서점 체인이 소매시장을 지배하는 와중에도 독...',
  ];

  for (const n of [...figma.currentPage.children]) {
    if (n.name === '브리핑 / Mobile / 360×780' || n.name.startsWith('브리핑 / Mobile')) {
      n.remove();
    }
  }

  function norm(s) {
    return (s || '').toLowerCase().replace(/\s+/g, ' ').trim();
  }

  function findColorVar(...candidates) {
    const vars = figma.variables.getLocalVariables('COLOR');
    for (const cand of candidates) {
      const parts = cand.split('/').map((p) => p.trim().toLowerCase());
      const hit = vars.find((v) => {
        const vn = norm(v.name);
        return parts.every((p) => vn.includes(p));
      });
      if (hit) return hit;
    }
    for (const cand of candidates) {
      const exact = vars.find((v) => norm(v.name) === norm(cand));
      if (exact) return exact;
    }
    return null;
  }

  function findCollectionForVar(variable) {
    if (!variable) return null;
    return figma.variables.getVariableCollectionById(variable.variableCollectionId);
  }

  function fillVar(v) {
    if (!v) return [{ type: 'SOLID', color: { r: 0.08, g: 0.08, b: 0.09, a: 1 } }];
    return [
      {
        type: 'SOLID',
        color: { r: 0, g: 0, b: 0, a: 1 },
        boundVariables: { color: { type: 'VARIABLE_ALIAS', id: v.id } },
      },
    ];
  }

  function solidPaint(r, g, b, a = 1) {
    return [{ type: 'SOLID', color: { r, g, b, a } }];
  }

  function chipBgFill() {
    return solidPaint(0.961, 0.431, 0.059, 0.14);
  }

  function findTextStyle(...names) {
    const styles = figma.getLocalTextStyles();
    for (const name of names) {
      const s = styles.find((st) => st.name === name);
      if (s) return s;
    }
    for (const name of names) {
      const ln = name.toLowerCase();
      const s = styles.find((st) => st.name.toLowerCase().includes(ln.replace(/\s/g, '')));
      if (s) return s;
    }
    return null;
  }

  async function setStyledText(node, style, text, fallbackSize = 14) {
    if (style) {
      try {
        await figma.loadFontAsync(style.fontName);
        node.textStyleId = style.id;
        node.characters = text;
        return style.name;
      } catch {
        try {
          const map = { SemiBold: 'Semi Bold', Bold: 'Bold', Medium: 'Medium', Regular: 'Regular' };
          const st = (style.fontName && style.fontName.style) || 'Medium';
          const interStyle = map[st] || 'Medium';
          await figma.loadFontAsync({ family: 'Inter', style: interStyle });
          node.fontName = { family: 'Inter', style: interStyle };
          if (typeof style.fontSize === 'number') node.fontSize = style.fontSize;
          node.textStyleId = style.id;
          node.characters = text;
        } catch {
          node.characters = text;
        }
        return style.name + ' (fallback)';
      }
    }
    await figma.loadFontAsync({ family: 'Inter', style: 'Medium' });
    node.fontSize = fallbackSize;
    node.characters = text;
    return 'fallback';
  }

  const vBg = findColorVar('Background / Default', 'Background/Default', 'Background', 'Default');
  const vCard = findColorVar('Background / Card', 'Background/Card', 'Card');
  const vTextPri = findColorVar('Text / Primary', 'Text/Primary', 'TextPrimary');
  const vTextSec = findColorVar('Text / Secondary', 'Text/Secondary', 'TextSecondary');
  const vAccent = findColorVar('Brand / Accent', 'Brand/Accent', 'Accent');

  const col = vBg ? findCollectionForVar(vBg) : figma.variables.getLocalVariableCollections()[0];
  const darkMode = col && col.modes.find((m) => norm(m.name) === 'dark') ? col.modes.find((m) => norm(m.name) === 'dark') : col && col.modes[0];

  const stTitleLarge = findTextStyle('Title / Large', 'Title/Large');
  const stTitleSmall = findTextStyle('Title / Small', 'Title/Small');
  const stBriefHl = findTextStyle('Sapiens / Briefing Headline', 'Briefing/Headline', 'Briefing Headline');
  const stBriefPub = findTextStyle('Sapiens / Briefing Publisher', 'Briefing/Publisher', 'Briefing Publisher');
  const stBodyLarge = findTextStyle('Body / Large Medium', 'Body/Large Medium');
  const stLabelSmall = findTextStyle('Label / Small', 'Label/Small');

  const contentMinH = 780 - APPBAR_H - NAV_H;
  const root = figma.createFrame();
  root.name = '브리핑 / Mobile / 360×780';
  root.resize(W, 780);
  root.layoutMode = 'VERTICAL';
  root.primaryAxisSizingMode = 'FIXED';
  root.counterAxisSizingMode = 'FIXED';
  root.itemSpacing = 0;
  root.fills = fillVar(vBg);
  root.clipsContent = true;
  if (col && darkMode && typeof root.setExplicitVariableModeForCollection === 'function') {
    root.setExplicitVariableModeForCollection(col.id, darkMode.modeId);
  }

  const appBar = figma.createFrame();
  appBar.name = 'AppBar';
  appBar.layoutMode = 'HORIZONTAL';
  appBar.primaryAxisSizingMode = 'FIXED';
  appBar.counterAxisSizingMode = 'FIXED';
  appBar.resize(W, APPBAR_H);
  appBar.layoutAlign = 'STRETCH';
  appBar.primaryAxisAlignItems = 'MIN';
  appBar.counterAxisAlignItems = 'CENTER';
  appBar.paddingLeft = appBar.paddingRight = 16;
  appBar.fills = fillVar(vBg);
  const appTitle = figma.createText();
  appTitle.name = 'Title';
  await setStyledText(appTitle, stTitleLarge, '브리핑', 28);
  try {
    appTitle.fontSize = 28;
  } catch (_) {}
  try {
    const fn = appTitle.fontName;
    await figma.loadFontAsync({ family: fn.family, style: 'Bold' });
    appTitle.fontName = { family: fn.family, style: 'Bold' };
  } catch (_) {}
  if (vTextPri) appTitle.fills = fillVar(vTextPri);
  appBar.appendChild(appTitle);

  const body = figma.createFrame();
  body.name = 'Body / Scroll';
  body.layoutMode = 'VERTICAL';
  body.primaryAxisSizingMode = 'AUTO';
  body.counterAxisSizingMode = 'FIXED';
  body.resize(W, contentMinH);
  body.layoutAlign = 'STRETCH';
  body.layoutGrow = 1;
  body.clipsContent = true;
  body.itemSpacing = 0;
  body.paddingTop = 8;
  body.paddingBottom = 0;
  body.fills = [];

  async function sectionLabel(text) {
    const f = figma.createFrame();
    f.name = 'SectionLabel / ' + text;
    f.layoutMode = 'VERTICAL';
    f.primaryAxisSizingMode = 'AUTO';
    f.counterAxisSizingMode = 'FIXED';
    f.resize(W, 1);
    f.layoutAlign = 'STRETCH';
    f.paddingLeft = f.paddingRight = PAD_H_SECTION;
    f.paddingTop = f.paddingBottom = PAD_V_SECTION;
    f.fills = [];
    const t = figma.createText();
    t.name = 'SectionTitle';
    await setStyledText(t, stTitleSmall, text, 14);
    if (vTextPri) t.fills = fillVar(vTextPri);
    f.appendChild(t);
    return f;
  }

  function divider(cardInnerWidth) {
    const r = figma.createRectangle();
    r.name = 'Divider';
    r.resize(cardInnerWidth, 0.5);
    r.layoutAlign = 'STRETCH';
    if (vTextSec) {
      r.fills = fillVar(vTextSec);
      r.opacity = 0.2;
    } else {
      r.fills = solidPaint(0.529, 0.529, 0.529, 0.2);
    }
    const wrap = figma.createFrame();
    wrap.name = 'DividerWrap';
    wrap.layoutMode = 'VERTICAL';
    wrap.primaryAxisSizingMode = 'AUTO';
    wrap.counterAxisSizingMode = 'FIXED';
    wrap.resize(W - CARD_OUT_H * 2, 1);
    wrap.layoutAlign = 'STRETCH';
    wrap.paddingTop = 8;
    wrap.paddingBottom = 6;
    wrap.paddingLeft = wrap.paddingRight = CARD_IN_H;
    wrap.fills = [];
    wrap.appendChild(r);
    return wrap;
  }

  async function listRow(text, cardInnerWidth) {
    const row = figma.createFrame();
    row.name = 'ListRow';
    row.layoutMode = 'HORIZONTAL';
    row.primaryAxisSizingMode = 'FIXED';
    row.counterAxisSizingMode = 'AUTO';
    row.resize(cardInnerWidth, 1);
    row.layoutAlign = 'STRETCH';
    row.paddingTop = row.paddingBottom = 8;
    row.fills = [];
    const tx = figma.createText();
    tx.name = 'Headline';
    await setStyledText(tx, stBodyLarge, text, 16);
    if (vTextPri) tx.fills = fillVar(vTextPri);
    tx.textAutoResize = 'HEIGHT';
    tx.layoutAlign = 'STRETCH';
    tx.layoutGrow = 1;
    row.appendChild(tx);
    return row;
  }

  async function briefingCard(opts) {
    const innerW = W - CARD_OUT_H * 2;
    const outer = figma.createFrame();
    outer.name = 'CardWrap / ' + opts.suffix;
    outer.layoutMode = 'VERTICAL';
    outer.primaryAxisSizingMode = 'AUTO';
    outer.counterAxisSizingMode = 'FIXED';
    outer.resize(W, 1);
    outer.layoutAlign = 'STRETCH';
    outer.paddingLeft = outer.paddingRight = CARD_OUT_H;
    outer.paddingTop = 0;
    outer.paddingBottom = opts.bottomPad != null ? opts.bottomPad : CARD_OUT_BOTTOM;
    outer.fills = [];

    const card = figma.createFrame();
    card.name = 'Card / ' + opts.suffix;
    card.layoutMode = 'VERTICAL';
    card.primaryAxisSizingMode = 'AUTO';
    card.counterAxisSizingMode = 'FIXED';
    card.resize(innerW, 1);
    card.layoutAlign = 'STRETCH';
    card.itemSpacing = 0;
    card.paddingLeft = card.paddingRight = CARD_IN_H;
    card.paddingTop = card.paddingBottom = CARD_IN_V;
    card.cornerRadius = CARD_RADIUS;
    card.fills = fillVar(vCard);

    const chip = figma.createFrame();
    chip.name = 'PublisherChip';
    chip.layoutMode = 'HORIZONTAL';
    chip.primaryAxisSizingMode = 'AUTO';
    chip.counterAxisSizingMode = 'AUTO';
    chip.paddingLeft = chip.paddingRight = CHIP_PAD_H;
    chip.paddingTop = chip.paddingBottom = CHIP_PAD_V;
    chip.cornerRadius = CHIP_RADIUS;
    chip.fills = chipBgFill();
    const chipT = figma.createText();
    chipT.name = 'ChipLabel';
    await setStyledText(chipT, stBriefPub, opts.chipLabel, 11);
    if (vAccent) chipT.fills = fillVar(vAccent);
    chip.appendChild(chipT);
    card.appendChild(chip);

    const sp8 = figma.createFrame();
    sp8.name = 'Spacer8';
    sp8.resize(1, 8);
    sp8.fills = [];
    card.appendChild(sp8);

    const hl = figma.createText();
    hl.name = 'MainHeadline';
    await setStyledText(hl, stBriefHl, opts.headline, 24);
    if (vTextPri) hl.fills = fillVar(vTextPri);
    hl.textAutoResize = 'HEIGHT';
    hl.layoutAlign = 'STRETCH';
    card.appendChild(hl);

    const innerContentW = innerW - CARD_IN_H * 2;
    for (const line of opts.rows) {
      card.appendChild(divider(innerContentW));
      card.appendChild(await listRow(line, innerContentW));
    }

    outer.appendChild(card);
    return outer;
  }

  body.appendChild(await sectionLabel('국내 주요뉴스'));
  body.appendChild(
    await briefingCard({
      suffix: 'Domestic',
      chipLabel: domesticChip,
      headline: domesticHeadline,
      rows: domesticRows,
    }),
  );

  const gap18 = figma.createFrame();
  gap18.name = 'Spacer / 18';
  gap18.resize(W, 18);
  gap18.fills = [];
  body.appendChild(gap18);

  body.appendChild(await sectionLabel('해외 주요뉴스'));
  body.appendChild(
    await briefingCard({
      suffix: 'Overseas',
      chipLabel: overseasChip,
      headline: overseasHeadline,
      rows: overseasRows,
      bottomPad: 0,
    }),
  );

  const nav = figma.createFrame();
  nav.name = 'BottomNavigation';
  nav.layoutMode = 'HORIZONTAL';
  nav.primaryAxisSizingMode = 'FIXED';
  nav.counterAxisSizingMode = 'FIXED';
  nav.resize(W, NAV_H);
  nav.layoutAlign = 'STRETCH';
  nav.primaryAxisAlignItems = 'CENTER';
  nav.counterAxisAlignItems = 'CENTER';
  nav.fills = fillVar(vBg);

  const tabs = [
    { label: '브리핑', selected: true },
    { label: '뉴스', selected: false },
    { label: '마켓', selected: false },
    { label: '마이', selected: false },
  ];

  for (const tab of tabs) {
    const cell = figma.createFrame();
    cell.name = 'NavItem / ' + tab.label;
    cell.layoutMode = 'VERTICAL';
    cell.primaryAxisSizingMode = 'AUTO';
    cell.counterAxisSizingMode = 'FIXED';
    cell.resize(W / 4, 1);
    cell.layoutGrow = 1;
    cell.layoutAlign = 'STRETCH';
    cell.primaryAxisAlignItems = 'CENTER';
    cell.counterAxisAlignItems = 'CENTER';
    cell.itemSpacing = 4;
    cell.paddingTop = cell.paddingBottom = 8;
    cell.fills = [];

    const ico = figma.createFrame();
    ico.name = 'Icon24';
    ico.resize(24, 24);
    ico.cornerRadius = 4;
    ico.fills = tab.selected && vAccent ? fillVar(vAccent) : vTextSec ? fillVar(vTextSec) : solidPaint(0.5, 0.5, 0.5, 1);

    const lt = figma.createText();
    lt.name = 'Label';
    const lblStyle = stLabelSmall || stTitleSmall;
    await setStyledText(lt, lblStyle, tab.label, 11);
    if (tab.selected && vAccent) lt.fills = fillVar(vAccent);
    else if (vTextSec) lt.fills = fillVar(vTextSec);

    cell.appendChild(ico);
    cell.appendChild(lt);
    nav.appendChild(cell);
  }

  root.appendChild(appBar);
  root.appendChild(body);
  root.appendChild(nav);

  figma.currentPage.appendChild(root);
  figma.viewport.scrollAndZoomIntoView([root]);
  figma.currentPage.selection = [root];

  return {
    id: root.id,
    domesticRows: domesticRows.length,
    overseasRows: overseasRows.length,
  };
})();
