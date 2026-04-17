package com.breaktobreak.dailynews.data.mock

import com.breaktobreak.dailynews.data.model.Article
import com.breaktobreak.dailynews.data.model.Company
import com.breaktobreak.dailynews.data.model.MarketDirection
import com.breaktobreak.dailynews.data.model.MarketIndicator
import com.breaktobreak.dailynews.data.model.USReport

object MockData {
    val morningArticles = listOf(
        Article(
            source = "한국경제",
            sourceColor = "kr",
            headline = "반도체 수출 3개월 연속 증가… AI 수요가 견인",
            summary = "4월 반도체 수출이 전년 대비 18.4% 증가하며 회복세가 뚜렷해졌다. 메모리 가격 반등과 AI 서버 수요 확대가 주요 동력으로 작용했다.",
            time = "오전 6:42",
            category = "경제",
            summaryPoints = listOf(
                "산업통상자원부 집계에서 4월 반도체 수출은 117억달러로 전년 동월 대비 18.4% 늘었고, 전체 수출 증가율(9.1%)의 두 배를 웃돌며 지수 반등을 주도했습니다.",
                "HBM과 DDR5 중심의 서버 메모리 ASP가 전분기 대비 7~9% 상승한 것이 배경으로, 미국 하이퍼스케일러의 AI 설비투자 확대가 주문 가시성을 높였습니다.",
                "수출 개선이 2분기에도 이어지면 무역수지 흑자 폭 확대와 원화 변동성 완화에 기여할 수 있어 코스피 반도체 밸류에이션 상단 재평가 가능성이 거론됩니다."
            ),
            tag = "ECON"
        ),
        Article(
            source = "매일경제",
            sourceColor = "kr",
            headline = "금리 인하 기대 속 코스피 2,850선 돌파",
            summary = "연준 6월 인하 가능성이 높아지며 외국인 매수세가 유입됐다. 전기차·2차전지 섹터가 지수 상승을 주도했다.",
            time = "오전 7:10",
            category = "매크로",
            summaryPoints = listOf(
                "코스피는 장중 2,850선을 돌파하며 6거래일 연속 상승했고, 외국인은 현·선물 합산 8,400억원 순매수로 지수 상승의 60% 이상을 설명했습니다.",
                "미국 4월 CPI가 전월 대비 0.2%로 둔화되며 연준 6월 인하 확률이 CME 기준 64%까지 오른 점이 위험자산 선호를 자극한 핵심 배경입니다.",
                "원/달러 환율이 1,360원대 중반으로 내려오면서 수입물가 부담이 완화될 수 있지만, 미 고용 지표 반등 시 금리 기대가 빠르게 되돌려질 리스크도 남아 있습니다."
            ),
            tag = "MARKET"
        ),
        Article(
            source = "조선비즈",
            sourceColor = "kr",
            headline = "서울 아파트 거래량 2년 만에 최고치 기록",
            summary = "4월 서울 아파트 매매 거래가 5,200건을 넘어서며 2024년 2월 이후 최고치를 기록했다. 강남3구와 마용성 중심으로 회복세가 뚜렷하다.",
            time = "오전 7:28",
            category = "부동산",
            summaryPoints = listOf(
                "서울부동산정보광장 기준 4월 거래 신고 건수는 5,200건을 넘어 최근 2년 최고치를 기록했고, 강남·서초·송파 비중이 전체의 27%로 확대됐습니다.",
                "전세가율 회복과 대출 금리 하락이 매수 심리를 되살렸으며, 신축 선호가 강화되면서 준공 10년 이하 단지의 거래 단가가 평균 4.3% 더 높게 형성됐습니다.",
                "거래량 회복이 실거래가 상승으로 연결되면 건설·리츠 종목에는 우호적이지만, 가계부채 재확대 시 규제 강화가 재개될 가능성도 동시에 커집니다."
            ),
            tag = "REAL ESTATE"
        ),
        Article(
            source = "연합뉴스",
            sourceColor = "kr",
            headline = "삼성전자, 2나노 파운드리 첫 양산 돌입",
            summary = "삼성전자가 화성 캠퍼스에서 2나노 공정 양산을 시작했다. 주요 고객사로 퀄컴과 테슬라가 거론되고 있다.",
            time = "오전 7:45",
            category = "IT",
            summaryPoints = listOf(
                "삼성전자는 화성 라인에서 2nm GAA 공정 초기 양산을 시작했고, 초기 월 생산능력은 1.5만장 규모로 2026년 상반기 2배 확대가 목표로 제시됐습니다.",
                "퀄컴·테슬라향 물량 수주 가능성이 거론되며 파운드리 가동률 개선 기대가 커졌고, 경쟁사 대비 전력 효율 15% 개선 수치가 마케팅 포인트로 부각됐습니다.",
                "초기 수율이 계획보다 5%p만 낮아도 감가 부담이 확대될 수 있어 단기 변동성은 남아 있으나, 성공 시 시스템반도체 생태계 확장 효과가 큽니다."
            ),
            tag = "TECH"
        )
    )

    val marketIndicators = listOf(
        MarketIndicator(name = "다우존스", value = "39,582.10", change = "+0.84%", direction = MarketDirection.UP),
        MarketIndicator(name = "나스닥", value = "17,940.22", change = "+1.21%", direction = MarketDirection.UP),
        MarketIndicator(name = "S&P 500", value = "5,303.27", change = "+0.92%", direction = MarketDirection.UP),
        MarketIndicator(name = "달러/원", value = "1,368.50", change = "−0.34%", direction = MarketDirection.DOWN),
        MarketIndicator(name = "금", value = "2,418.90", change = "+0.52%", direction = MarketDirection.UP),
        MarketIndicator(name = "WTI 유가", value = "82.14", change = "−1.08%", direction = MarketDirection.DOWN)
    )

    val usReport = USReport(
        date = "4/18 현지시간",
        body = "뉴욕증시 3대 지수는 강한 기업 실적과 인플레이션 둔화 신호에 힘입어 일제히 상승 마감했다. 엔비디아가 3.2% 오르며 AI 반도체 랠리를 다시 이끌었고, 금융주도 견조한 흐름을 보였다. 장 후반 발표된 주간 실업수당 청구건수가 예상치를 하회하며 연착륙 기대를 키웠다."
    )

    val usArticles = listOf(
        Article(
            source = "Bloomberg",
            headline = "엔비디아, 신제품 발표 앞두고 사상 최고가 경신",
            summary = "신형 AI 가속기 공개 기대와 데이터센터 수요가 겹치며 주가가 신고가를 재차 경신했다.",
            time = "06:40",
            category = "빅테크",
            summaryPoints = listOf(
                "엔비디아 주가는 장중 $1,045까지 오르며 사상 최고치를 다시 썼고, 20거래일 누적 상승률은 17%로 S&P500 대비 초과수익이 크게 확대됐습니다.",
                "클라우드 사업자 4곳의 2026년 AI 서버 CAPEX 가이던스가 전년 대비 평균 28% 상향된 점이 배경이며, 블랙웰 출하 일정이 투자심리를 추가로 자극했습니다.",
                "주가가 선행 EPS 41배 수준까지 올라 밸류 부담 논란은 있으나, 공급 병목 완화가 확인되면 실적 추정치 상향이 한 차례 더 나올 수 있다는 전망이 나옵니다."
            )
        ),
        Article(
            source = "Reuters",
            headline = "애플, 서비스 매출 분기 사상 최대… AI 전환 가속",
            summary = "서비스 부문 기록 경신과 온디바이스 AI 전략 강화가 동시에 확인됐다.",
            time = "06:12",
            category = "빅테크",
            summaryPoints = listOf(
                "애플 서비스 매출은 분기 기준 $25.3B로 전년 대비 13% 증가해 역대 최고치를 기록했고, 총이익률도 46%로 0.8%p 개선됐습니다.",
                "차기 OS 업데이트에서 온디바이스 AI 기능을 대거 탑재한다는 계획이 제시되며 아이폰 교체 주기 단축 기대가 재점화됐습니다.",
                "하드웨어 성장 둔화를 서비스가 방어하는 구조가 강화되면 실적 안정성이 높아지지만, 앱스토어 규제 리스크가 멀티플 확장 속도를 제약할 수 있습니다."
            )
        ),
        Article(
            source = "WSJ",
            headline = "JP모건 \"미국 경제 연착륙 확률 70%로 상향\"",
            summary = "고용·소비 지표가 예상보다 견조해 경기 침체 가능성이 낮아졌다는 분석이 나왔다.",
            time = "05:58",
            category = "매크로",
            summaryPoints = listOf(
                "JP모건은 12개월 내 경기침체 확률을 35%에서 25%로 낮추고, 연착륙 시나리오 확률을 70%로 상향 조정했습니다.",
                "핵심 근거로 실업률 4.0% 유지, 소매판매 전월 대비 0.6% 증가, 기업 신용스프레드 안정 구간 지속을 제시했습니다.",
                "연착륙 기대가 강화되면 금융·산업재에 우호적이지만, 물가 재가속이 발생하면 금리 인하 경로가 지연되어 밸류 성장주 변동성이 확대될 수 있습니다."
            )
        ),
        Article(
            source = "CNBC",
            headline = "테슬라, 2분기 인도량 시장 예상 상회",
            summary = "공격적 가격정책과 생산 효율 개선으로 인도량이 컨센서스를 웃돌았다.",
            time = "05:30",
            category = "모빌리티",
            summaryPoints = listOf(
                "테슬라 2분기 인도량은 46.8만대로 시장 예상치 44.9만대를 4.2% 상회했고, 상하이 공장 가동률 개선이 물량 회복을 이끌었습니다.",
                "평균판매단가(ASP)는 전년 대비 6% 하락했지만 원가 절감과 물류 효율화로 자동차 부문 총마진이 18% 수준을 방어했다는 평가가 나왔습니다.",
                "인도량 반등은 단기 실적에 긍정적이지만 추가 가격 인하 경쟁이 재개되면 하반기 EPS 추정치가 재차 하향될 가능성이 있습니다."
            )
        ),
        Article(
            source = "Bloomberg",
            headline = "연준 의사록, 6월 금리 인하 신호 강화",
            summary = "의사록에서 인플레이션 둔화 확인 시 정책 완화 여지가 언급됐다.",
            time = "05:02",
            category = "금리",
            summaryPoints = listOf(
                "FOMC 의사록에서 다수 위원이 물가 둔화의 추가 증거가 확인되면 정책금리 인하를 검토할 수 있다고 명시해 시장 기대를 키웠습니다.",
                "미국 2년물 금리는 발표 직후 11bp 하락한 4.31%를 기록했고, 나스닥 선물은 +0.9% 반등하며 금리 민감주가 강세를 보였습니다.",
                "다만 위원들은 임금과 서비스 물가의 하방 경직성을 리스크로 지적해, 향후 고용지표가 강하면 인하 시점이 재차 밀릴 수 있음을 시사했습니다."
            )
        ),
        Article(
            source = "FT",
            headline = "달러 인덱스 3주래 최저… 원화 강세 전환",
            summary = "달러 약세와 위험선호 회복으로 원화가 강세 구간에 진입했다.",
            time = "04:45",
            category = "환율",
            summaryPoints = listOf(
                "달러 인덱스(DXY)는 103.1까지 하락해 3주 최저를 기록했고, 원/달러 환율은 역외에서 1,359원까지 내려오며 심리적 저항선을 이탈했습니다.",
                "미국 금리 하락과 아시아 증시 자금 유입이 맞물리며 KRW·TWD 등 위험통화가 동반 강세를 보였고, 수입 기업의 환헤지 수요도 완화됐습니다.",
                "환율 하향 안정은 국내 물가 부담 완화에 긍정적이지만, 지정학 이벤트로 달러 수요가 재점화되면 변동성이 빠르게 확대될 수 있습니다."
            )
        )
    )

    val newsFeed = listOf(
        Article(
            source = "한국경제",
            headline = "한은, 기준금리 동결… 연내 인하 시점 저울질",
            summary = "물가 둔화 속도와 환율 변동성이 함께 고려되며, 완화 전환 시점은 하반기 이후가 유력하다는 분석이 나온다.",
            time = "12분 전",
            category = "금리",
            summaryPoints = listOf(
                "한국은행은 기준금리를 3.50%로 동결하며 성장 하방과 환율 상방 압력을 동시에 고려했다고 밝혔고, 물가 전망치는 2.6%로 유지했습니다.",
                "시장에서는 4분기 1회 인하 가능성을 58%로 반영하고 있으며, 미 연준 인하 시점과 원/달러 환율 안정 여부가 정책 전환의 핵심 전제 조건으로 꼽힙니다.",
                "금리 동결 장기화는 내수 회복 속도를 늦출 수 있지만 금융 안정성 측면에서는 가계부채 급증을 억제하는 효과가 있어 업종별 주가 차별화가 커질 수 있습니다."
            ),
            tag = "경제"
        ),
        Article(
            source = "전자신문",
            headline = "카카오, 자회사 구조조정 마무리 단계",
            summary = "핵심 사업 중심 재편으로 수익성 개선에 속도를 내며, 연내 비용 효율화 효과가 본격 반영될 전망이다.",
            time = "34분 전",
            category = "빅테크",
            summaryPoints = listOf(
                "카카오는 비핵심 자회사 통합·매각을 통해 고정비를 연간 약 2,100억원 절감하는 계획을 공개했고, 3분기부터 실적 반영이 예상됩니다.",
                "광고·커머스 핵심 플랫폼 집중 전략으로 영업레버리지 개선이 가능해졌지만, 단기적으로 일회성 구조조정 비용이 분기 변동성을 높일 수 있습니다.",
                "AI 서비스 상용화와 결합될 경우 ARPU 개선 여지가 있으나, 경쟁 플랫폼의 가격 프로모션 확대가 수익성 회복 속도를 늦출 수 있습니다."
            ),
            tag = "IT"
        ),
        Article(
            source = "연합뉴스",
            headline = "국회, 5월 임시회 소집… AI 기본법 처리 주목",
            summary = "산업 경쟁력 강화를 위한 제도적 기반 마련이 핵심 쟁점으로 부상하면서 관련 업계의 관심이 높아지고 있다.",
            time = "1시간 전",
            category = "정책",
            summaryPoints = listOf(
                "5월 임시회에서 AI 기본법이 우선 심사 안건으로 상정되며, 고위험 AI 분류·책임체계·데이터 활용 가이드라인이 주요 쟁점으로 부상했습니다.",
                "법안 통과 시 공공 조달과 산업 지원 예산이 확대될 가능성이 있어 AI 인프라·보안·클라우드 기업에 중기 수혜 기대가 형성되고 있습니다.",
                "반면 규제 강도가 높아질 경우 스타트업의 인증·컴플라이언스 비용이 증가해 산업 내 양극화가 심화될 수 있다는 우려도 병존합니다."
            ),
            tag = "정치"
        ),
        Article(
            source = "조선비즈",
            headline = "현대차, 전기차 신모델 북미 출시 일정 공개",
            summary = "북미 생산 거점과 배터리 공급망 전략을 결합해 전기차 시장 점유율 확대를 노리는 로드맵이 제시됐다.",
            time = "1시간 전",
            category = "모빌리티",
            summaryPoints = listOf(
                "현대차는 북미 전기차 신모델 출시를 2026년 1분기로 확정하고, 조지아 공장 증설로 연간 30만대 생산 체제를 구축하겠다고 밝혔습니다.",
                "현지 배터리 조달 비중을 70% 이상으로 높여 IRA 세액공제 수혜를 극대화하는 전략이 제시되며 가격 경쟁력 확보 기대가 커졌습니다.",
                "출시 초기 수요가 계획에 못 미치면 고정비 부담이 확대될 수 있지만, 성공 시 북미 점유율 상승과 밸류 체인 전반의 매출 레버리지가 기대됩니다."
            ),
            tag = "산업"
        ),
        Article(
            source = "매일경제",
            headline = "시중은행 예금금리 다시 하락세",
            summary = "시장금리 안정과 은행권 조달 비용 변화가 반영되며 고금리 특판이 빠르게 축소되는 모습이다.",
            time = "2시간 전",
            category = "금융",
            summaryPoints = listOf(
                "주요 시중은행 1년 만기 예금금리는 평균 3.25%로 한 달 전 대비 0.22%p 하락했고, 4%대 특판 상품은 대부분 종료됐습니다.",
                "국고채 3년물 하락과 조달비용 정상화가 배경이며, 은행의 NIM 방어가 가능해졌지만 수신 경쟁 약화로 고객 이탈 우려도 제기됩니다.",
                "예금금리 하락은 가계 자금의 주식·채권 이동을 자극할 수 있어 자본시장 유동성에는 우호적이나, 소비 위축 국면에서는 예대마진 논란이 재점화될 수 있습니다."
            ),
            tag = "금융"
        ),
        Article(
            source = "Reuters",
            headline = "유럽중앙은행, 6월 인하 사실상 확정",
            summary = "유로존 경기 둔화와 인플레이션 완화가 맞물리며 정책 전환 기대가 강화되고 위험자산 선호가 회복되고 있다.",
            time = "3시간 전",
            category = "매크로",
            summaryPoints = listOf(
                "ECB 위원 발언 이후 6월 인하 확률은 OIS 기준 89%까지 상승했고, 유로존 2년물 금리는 하루 만에 14bp 하락했습니다.",
                "제조업 PMI 부진과 핵심 물가 둔화가 정책 전환의 배경이며, 유럽 증시에서는 경기민감·금리민감 업종이 동반 반등했습니다.",
                "글로벌 동시 완화 기대는 신흥국 위험자산에 긍정적이지만, 유럽 성장 둔화가 장기화되면 한국 수출 사이클 회복 속도에 하방 압력으로 작용할 수 있습니다."
            ),
            tag = "글로벌"
        )
    )

    val companyList = listOf(
        Company(
            ticker = "005930",
            name = "삼성전자",
            sector = "반도체",
            description = "메모리 업황 회복과 고대역폭 메모리 수요 확대가 실적 개선 기대를 높이고 있다."
        ),
        Company(
            ticker = "000660",
            name = "SK하이닉스",
            sector = "반도체",
            description = "HBM 중심의 제품 믹스 고도화로 AI 인프라 투자 사이클의 수혜가 이어지고 있다."
        ),
        Company(
            ticker = "NVDA",
            name = "NVIDIA",
            sector = "AI/반도체",
            description = "데이터센터 GPU 수요 강세가 지속되며 AI 생태계 핵심 공급자로서 프리미엄이 유지되고 있다."
        ),
        Company(
            ticker = "035420",
            name = "NAVER",
            sector = "인터넷",
            description = "검색·커머스 기반 현금창출력에 생성형 AI 서비스 확장이 더해지며 체질 개선이 진행 중이다."
        ),
        Company(
            ticker = "AAPL",
            name = "Apple",
            sector = "플랫폼/디바이스",
            description = "서비스 매출 성장과 온디바이스 AI 전략이 하드웨어 교체 수요를 재자극할 가능성이 주목된다."
        ),
        Company(
            ticker = "051910",
            name = "LG화학",
            sector = "2차전지/화학",
            description = "배터리 소재 중심 포트폴리오 전환 속에서 수익성 방어와 신규 수주 확보가 핵심 과제로 평가된다."
        ),
        Company(
            ticker = "TSLA",
            name = "Tesla",
            sector = "전기차",
            description = "가격 정책과 자율주행 소프트웨어 전략이 마진과 밸류에이션 방향성을 좌우하는 구간이다."
        )
    )

    val bookmarkedArticles = listOf(
        Article(
            source = "Bloomberg",
            headline = "엔비디아, 신제품 발표 앞두고 사상 최고가 경신",
            summary = "AI 반도체 수요가 예상치를 상회하며 시가총액 상단을 다시 테스트하는 흐름이다.",
            time = "06:40",
            tag = "글로벌"
        ),
        Article(
            source = "한국경제",
            headline = "반도체 수출 3개월 연속 증가… AI 수요가 견인",
            summary = "메모리 가격 반등과 AI 서버 투자 확대가 맞물리며 한국 반도체 업황 회복 시그널이 강화됐다.",
            time = "오전 6:42",
            tag = "경제"
        ),
        Article(
            source = "연합뉴스",
            headline = "국회, 5월 임시회 소집… AI 기본법 처리 주목",
            summary = "AI 산업 규제와 진흥의 균형을 둘러싼 논의가 본격화되며 기업 대응 전략 수립이 요구된다.",
            time = "1시간 전",
            tag = "정치"
        )
    )
}
