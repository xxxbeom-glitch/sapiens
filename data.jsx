// Mock data for the 브리핑 app

const MORNING_ARTICLES = [
  {
    source: '한국경제',
    sourceColor: 'kr',
    headline: '반도체 수출 3개월 연속 증가… AI 수요가 견인',
    summary: '4월 반도체 수출이 전년 대비 18.4% 증가하며 회복세가 뚜렷해졌다. 메모리 가격 반등과 AI 서버 수요 확대가 주요 동력으로 작용했다.',
    time: '오전 6:42',
    tag: 'ECON',
  },
  {
    source: '매일경제',
    sourceColor: 'kr',
    headline: '금리 인하 기대 속 코스피 2,850선 돌파',
    summary: '연준 6월 인하 가능성이 높아지며 외국인 매수세가 유입됐다. 전기차·2차전지 섹터가 지수 상승을 주도했다.',
    time: '오전 7:10',
    tag: 'MARKET',
  },
  {
    source: '조선비즈',
    sourceColor: 'kr',
    headline: '서울 아파트 거래량 2년 만에 최고치 기록',
    summary: '4월 서울 아파트 매매 거래가 5,200건을 넘어서며 2024년 2월 이후 최고치를 기록했다. 강남3구와 마용성 중심으로 회복세가 뚜렷하다.',
    time: '오전 7:28',
    tag: 'REAL ESTATE',
  },
  {
    source: '연합뉴스',
    sourceColor: 'kr',
    headline: '삼성전자, 2나노 파운드리 첫 양산 돌입',
    summary: '삼성전자가 화성 캠퍼스에서 2나노 공정 양산을 시작했다. 주요 고객사로 퀄컴과 테슬라가 거론되고 있다.',
    time: '오전 7:45',
    tag: 'TECH',
  },
];

const MARKET_INDICATORS = [
  { name: '다우존스',  value: '39,582.10', change: '+0.84%', dir: 'up' },
  { name: '나스닥',    value: '17,940.22', change: '+1.21%', dir: 'up' },
  { name: 'S&P 500',  value: '5,303.27',  change: '+0.92%', dir: 'up' },
  { name: '달러/원',   value: '1,368.50',  change: '−0.34%', dir: 'down' },
  { name: '금',        value: '2,418.90',  change: '+0.52%', dir: 'up' },
  { name: 'WTI 유가',  value: '82.14',     change: '−1.08%', dir: 'down' },
];

const US_REPORT = {
  date: '4/18 현지시간',
  body: '뉴욕증시 3대 지수는 강한 기업 실적과 인플레이션 둔화 신호에 힘입어 일제히 상승 마감했다. 엔비디아가 3.2% 오르며 AI 반도체 랠리를 다시 이끌었고, 금융주도 견조한 흐름을 보였다. 장 후반 발표된 주간 실업수당 청구건수가 예상치를 하회하며 연착륙 기대를 키웠다.',
};

const US_ARTICLES = [
  { title: '엔비디아, 신제품 발표 앞두고 사상 최고가 경신',      time: '06:40', source: 'Bloomberg' },
  { title: '애플, 서비스 매출 분기 사상 최대… AI 전환 가속',    time: '06:12', source: 'Reuters' },
  { title: 'JP모건 "미국 경제 연착륙 확률 70%로 상향"',        time: '05:58', source: 'WSJ' },
  { title: '테슬라, 2분기 인도량 시장 예상 상회',               time: '05:30', source: 'CNBC' },
  { title: '연준 의사록, 6월 금리 인하 신호 강화',               time: '05:02', source: 'Bloomberg' },
  { title: '달러 인덱스 3주래 최저… 원화 강세 전환',             time: '04:45', source: 'FT' },
];

const WATCHLIST = [
  { ticker:'005930', name:'삼성전자',     price:'78,400', change:'+2.34%', dir:'up' },
  { ticker:'000660', name:'SK하이닉스',   price:'214,500', change:'+3.12%', dir:'up' },
  { ticker:'NVDA',   name:'Nvidia',      price:'$928.14', change:'+3.21%', dir:'up' },
  { ticker:'035420', name:'NAVER',       price:'185,200', change:'−0.54%', dir:'down' },
  { ticker:'AAPL',   name:'Apple',       price:'$214.67', change:'+1.08%', dir:'up' },
  { ticker:'051910', name:'LG화학',      price:'412,000', change:'−1.21%', dir:'down' },
  { ticker:'TSLA',   name:'Tesla',       price:'$178.22', change:'+0.88%', dir:'up' },
];

const NEWS_FEED = [
  { cat:'경제', title:'한은, 기준금리 동결… 연내 인하 시점 저울질', time:'12분 전', source:'한국경제' },
  { cat:'IT',  title:'카카오, 자회사 구조조정 마무리 단계', time:'34분 전', source:'전자신문' },
  { cat:'정치', title:'국회, 5월 임시회 소집… AI 기본법 처리 주목', time:'1시간 전', source:'연합뉴스' },
  { cat:'산업', title:'현대차, 전기차 신모델 북미 출시 일정 공개', time:'1시간 전', source:'조선비즈' },
  { cat:'금융', title:'시중은행 예금금리 다시 하락세', time:'2시간 전', source:'매일경제' },
  { cat:'글로벌', title:'유럽중앙은행, 6월 인하 사실상 확정', time:'3시간 전', source:'Reuters' },
];

Object.assign(window, {
  MORNING_ARTICLES, MARKET_INDICATORS, US_REPORT, US_ARTICLES, WATCHLIST, NEWS_FEED,
});
