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

  const oCard = findCard('Overseas');
  if (!oCard) return { err: '해외 카드 없음' };

  const r2 = await applyCard(oCard, overseasChip, overseasHeadline, overseasRows);
  if (r2.err) return r2;

  figma.viewport.scrollAndZoomIntoView([root]);
  return { ok: true, overseas: r2.rows };
})();
