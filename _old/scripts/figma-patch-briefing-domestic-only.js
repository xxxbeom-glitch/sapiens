(async () => {
  const root = figma.currentPage.findOne((n) => n.name === '브리핑 / Mobile / 360×780');
  if (!root) return { err: '브리핑 프레임 없음' };

  function findCard(suffix) {
    let found = null;
    function walk(n) {
      if (n.name === 'Card / ' + suffix) found = n;
      if (!found && 'children' in n) for (const c of n.children) walk(c);
    }
    walk(root);
    return found;
  }

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

  async function setChars(node, text) {
    if (!node || node.type !== 'TEXT') return;
    try {
      const f = node.fontName;
      if (f !== figma.mixed) await figma.loadFontAsync(f);
    } catch (_) {}
    node.characters = text;
  }

  function syncListRowCount(card, targetRows) {
    while (true) {
      const lr = card.findAll((n) => n.name === 'ListRow');
      if (lr.length <= targetRows) break;
      const last = lr[lr.length - 1];
      const ix = card.children.indexOf(last);
      const prev = ix > 0 ? card.children[ix - 1] : null;
      if (prev && prev.name === 'DividerWrap') prev.remove();
      last.remove();
    }

    const ch = card.children;
    let mi = -1;
    for (let i = 0; i < ch.length; i++) {
      if (ch[i].name === 'MainHeadline') {
        mi = i;
        break;
      }
    }
    if (mi < 0) return { err: 'MainHeadline 없음' };
    const tDiv = ch[mi + 1];
    const tRow = ch[mi + 2];
    if (!tDiv || tDiv.name !== 'DividerWrap' || !tRow || tRow.name !== 'ListRow') {
      return {
        err: 'DividerWrap+ListRow 템플릿 없음',
        div: tDiv && tDiv.name,
        row: tRow && tRow.name,
      };
    }

    while (true) {
      const lr = card.findAll((n) => n.name === 'ListRow');
      if (lr.length >= targetRows) break;
      card.appendChild(tDiv.clone());
      card.appendChild(tRow.clone());
    }
    return { ok: true };
  }

  async function applyCard(card, chipText, hlText, rowTexts) {
    const sync = syncListRowCount(card, rowTexts.length);
    if (sync.err) return sync;
    const chip = card.findOne((n) => n.type === 'TEXT' && n.name === 'ChipLabel');
    const hl = card.findOne((n) => n.type === 'TEXT' && n.name === 'MainHeadline');
    await setChars(chip, chipText);
    await setChars(hl, hlText);
    const listRows = card.findAll((n) => n.name === 'ListRow');
    for (let i = 0; i < rowTexts.length; i++) {
      const h = listRows[i].findOne((n) => n.type === 'TEXT' && n.name === 'Headline');
      await setChars(h, rowTexts[i]);
    }
    return { ok: true, rows: rowTexts.length };
  }

  const dCard = findCard('Domestic');
  if (!dCard) return { err: '국내 카드 없음' };

  const r1 = await applyCard(dCard, domesticChip, domesticHeadline, domesticRows);
  if (r1.err) return r1;

  figma.viewport.scrollAndZoomIntoView([root]);
  return { ok: true, domestic: r1.rows };
})();
