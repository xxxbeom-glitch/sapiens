(async () => {
  const root = figma.currentPage.findOne((n) => n.name === '브리핑 / Mobile / 360×780');
  if (!root) return { err: '브리핑 프레임 없음. 먼저 프레임을 만들거나 이름을 확인하세요.' };

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

  const dCard = findCard('Domestic');
  const oCard = findCard('Overseas');
  if (!dCard || !oCard) return { err: 'Card / Domestic 또는 Overseas 없음', hasD: !!dCard, hasO: !!oCard };

  const chipD = dCard.findOne((n) => n.type === 'TEXT' && n.name === 'ChipLabel');
  const hlD = dCard.findOne((n) => n.type === 'TEXT' && n.name === 'MainHeadline');
  await setChars(chipD, domesticChip);
  await setChars(hlD, domesticHeadline);

  const rowsD = dCard.findAll((n) => n.name === 'ListRow');
  for (let i = 0; i < domesticRows.length; i++) {
    const row = rowsD[i];
    if (!row) break;
    const t = row.findOne((n) => n.type === 'TEXT');
    await setChars(t, domesticRows[i]);
  }

  const chipO = oCard.findOne((n) => n.type === 'TEXT' && n.name === 'ChipLabel');
  const hlO = oCard.findOne((n) => n.type === 'TEXT' && n.name === 'MainHeadline');
  await setChars(chipO, overseasChip);
  await setChars(hlO, overseasHeadline);

  const rowsO = oCard.findAll((n) => n.name === 'ListRow');
  for (let i = 0; i < overseasRows.length; i++) {
    const row = rowsO[i];
    if (!row) break;
    const t = row.findOne((n) => n.type === 'TEXT');
    await setChars(t, overseasRows[i]);
  }

  figma.viewport.scrollAndZoomIntoView([root]);
  return { ok: true, domesticListRows: rowsD.length, overseasListRows: rowsO.length };
})();
