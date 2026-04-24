package com.sapiens.app.data.mock

import com.sapiens.app.data.model.Article
import com.sapiens.app.data.model.Company
import com.sapiens.app.data.model.FinancialMetric
import com.sapiens.app.data.model.FinancialSeries
import com.sapiens.app.data.model.MarketDirection
import com.sapiens.app.data.model.MarketIndex
import com.sapiens.app.data.model.MarketIndicator
import com.sapiens.app.data.model.MarketTheme
import com.sapiens.app.data.model.ThemeStock
import com.sapiens.app.data.model.YearFinancialValue
import com.sapiens.app.data.model.USReport

object MockData {
    val briefingHankyungArticles: List<Article> = (1..5).map { i ->
        Article(
            source = "한국경제",
            sourceColor = "kr",
            headline = "데모 한국경제 신문 기사 제목 $i",
            summary = "Firestore briefing/hankyung 이 비었을 때 표시됩니다.",
            time = String.format("2026-04-19 %02d:00", 8 + i),
            category = "경제",
            summaryPoints = listOf("파이프라인 실행 후 실제 한국경제 기사가 동기화됩니다."),
            tag = "HK",
            url = "https://demo.sapiens.invalid/briefing/hankyung/$i"
        )
    }

    val briefingMaeilArticles: List<Article> = (1..5).map { i ->
        Article(
            source = "매일경제",
            sourceColor = "kr",
            headline = "데모 매일경제 신문 기사 제목 $i",
            summary = "Firestore briefing/maeil 이 비었을 때 표시됩니다.",
            time = String.format("2026-04-19 %02d:30", 12 + i),
            category = "매크로",
            summaryPoints = listOf("파이프라인 실행 후 실제 매일경제 기사가 동기화됩니다."),
            tag = "MK",
            url = "https://demo.sapiens.invalid/briefing/maeil/$i"
        )
    }

    val marketIndicators = listOf(
        MarketIndicator(name = "다우존스", value = "39,582.10", change = "+0.84%", direction = MarketDirection.UP),
        MarketIndicator(name = "나스닥", value = "17,940.22", change = "+1.21%", direction = MarketDirection.UP),
        MarketIndicator(name = "S&P 500", value = "5,303.27", change = "+0.92%", direction = MarketDirection.UP),
        MarketIndicator(name = "달러/원", value = "1,368.50", change = "−0.34%", direction = MarketDirection.DOWN),
        MarketIndicator(name = "금", value = "2,418.90", change = "+0.52%", direction = MarketDirection.UP),
        MarketIndicator(name = "WTI 유가", value = "82.14", change = "−1.08%", direction = MarketDirection.DOWN)
    )

    val TICKER_PAGES = listOf(
        listOf(
            MarketIndicator(name = "코스피", value = "2,856.13", change = "+0.72%", direction = MarketDirection.UP),
            MarketIndicator(name = "코스닥", value = "871.42", change = "+0.38%", direction = MarketDirection.UP)
        ),
        listOf(
            MarketIndicator(name = "원화/달러", value = "1,356.80", change = "−0.41%", direction = MarketDirection.DOWN),
            MarketIndicator(name = "원화/엔화", value = "905.14", change = "−0.22%", direction = MarketDirection.DOWN)
        ),
        listOf(
            MarketIndicator(name = "금", value = "2,418.90", change = "+0.52%", direction = MarketDirection.UP),
            MarketIndicator(name = "은", value = "31.18", change = "+0.64%", direction = MarketDirection.UP)
        ),
        listOf(
            MarketIndicator(name = "S&P 500", value = "5,303.27", change = "+0.92%", direction = MarketDirection.UP),
            MarketIndicator(name = "나스닥", value = "17,940.22", change = "+1.21%", direction = MarketDirection.UP)
        ),
        listOf(
            MarketIndicator(name = "다우", value = "39,582.10", change = "+0.84%", direction = MarketDirection.UP),
            MarketIndicator(name = "WTI 유가", value = "82.14", change = "−1.08%", direction = MarketDirection.DOWN)
        )
    )

    val MARKET_INDEX_LIST = listOf(
        MarketIndex("국내", "코스피", "2,856.13", "+0.72%", "2,835.70", MarketDirection.UP),
        MarketIndex("국내", "코스닥", "871.42", "+0.38%", "868.12", MarketDirection.UP),
        MarketIndex("미국", "S&P 500", "5,303.27", "+0.92%", "5,254.35", MarketDirection.UP),
        MarketIndex("미국", "나스닥", "17,940.22", "+1.21%", "17,726.10", MarketDirection.UP),
        MarketIndex("환율", "달러/원", "1,368.50", "−0.34%", "1,373.20", MarketDirection.DOWN),
        MarketIndex("원자재", "금", "2,418.90", "+0.52%", "2,406.39", MarketDirection.UP),
        MarketIndex("원자재", "WTI 유가", "82.14", "−1.08%", "83.04", MarketDirection.DOWN),
        MarketIndex("미국", "다우존스", "39,582.10", "+0.84%", "39,251.44", MarketDirection.UP)
    )

    /** Firestore `market/themes` 비어 있을 때 국내 탭 데모용. */
    val marketThemes = listOf(
        MarketTheme(
            themeName = "스페이스X(SpaceX)",
            changeRate = "+8.38%",
            stocks = listOf(
                ThemeStock("와이제이링크", "5,920", "+29.82%", "209640"),
                ThemeStock("케이엔알시스템", "12,400", "+8.12%", "199430"),
                ThemeStock("한화에어로스페이스", "520,000", "+3.45%", "012450"),
                ThemeStock("비에이치아이", "42,100", "+1.20%", "083650"),
                ThemeStock("조정 종목", "10,000", "-2.10%", "005930")
            )
        ),
        MarketTheme(
            themeName = "2차전지·소재",
            changeRate = "-1.05%",
            stocks = listOf(
                ThemeStock("에코프로비엠", "98,700", "+4.52%", "247540"),
                ThemeStock("LG에너지솔루션", "410,000", "+1.88%", "373220"),
                ThemeStock("포스코퓨처엠", "312,000", "+0.65%", "003670"),
                ThemeStock("SK이노베이션", "115,500", "-0.43%", "096770")
            )
        ),
        MarketTheme(
            themeName = "반도체 장비",
            changeRate = "+2.31%",
            stocks = listOf(
                ThemeStock("원익IPS", "42,300", "+6.11%", "240810"),
                ThemeStock("주성엔지니어링", "28,900", "+2.40%", "036930"),
                ThemeStock("테스", "32,150", "+1.10%", "095610")
            )
        )
    )

    /** Firestore `market/industries` 비어 있을 때 업종 탭용(테마와 동일 [MarketTheme] 스키마). */
    val marketIndustries: List<MarketTheme> = emptyList()

    val usReport = USReport(
        date = "4/18 현지시간",
        body = "뉴욕증시 3대 지수는 강한 기업 실적과 인플레이션 둔화 신호에 힘입어 일제히 상승 마감했다. 엔비디아가 3.2% 오르며 AI 반도체 랠리를 다시 이끌었고, 금융주도 견조한 흐름을 보였다. 장 후반 발표된 주간 실업수당 청구건수가 예상치를 하회하며 연착륙 기대를 키웠다."
    )

    val usArticles = listOf(
        Article(
            source = "Bloomberg",
            headline = "엔비디아 신제품 공개 기대에 주가 사상 최고치 재경신",
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
            headline = "애플 서비스 매출 분기 최대, 온디바이스 AI 전환 가속",
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
            headline = "JP모건, 미국 경제 연착륙 확률 70%로 상향 제시",
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
            headline = "테슬라 2분기 인도량 예상 상회, 생산 효율 개선 확인",
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
            headline = "연준 의사록, 6월 금리 인하 가능성 신호 한층 강화",
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
            headline = "달러 인덱스 3주래 최저 하락, 원화 강세 흐름 전환",
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
            headline = "한은 기준금리 동결 유지, 연내 인하 시점 신중 검토",
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
            headline = "카카오 자회사 구조조정 마무리 단계, 핵심 사업 재편 가속",
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
            headline = "국회 5월 임시회 소집, AI 기본법 처리 논의 본격화",
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
            headline = "현대차 전기차 신모델 북미 출시 일정 공개, 점유율 확대 승부",
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
            headline = "시중은행 예금금리 다시 하락세, 고금리 특판 빠르게 축소",
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
            headline = "유럽중앙은행 6월 금리 인하 유력, 정책 전환 기대 확산",
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

    /** 프리뷰·디버그용(앱 뉴스는 Firestore `ai_issue`를 구독). */
    val NEWS_AI_ISSUE = newsFeed

    val NEWS_DOMESTIC_MARKET = listOf(
        Article(
            source = "연합뉴스",
            headline = "코스피 장중 2860선 돌파, 외국인 순매수 확대 지속",
            summary = "반도체와 2차전지 동반 강세로 지수가 급등했다.",
            time = "방금 전",
            category = "경제",
            tag = "경제",
            summaryPoints = listOf(
                "장중 코스피는 2,860선을 돌파하며 전일 대비 1.4% 상승했고, 외국인 순매수 규모가 약 4,200억원까지 확대되며 지수 상단을 견인했습니다.",
                "대형 반도체주와 2차전지 대표 종목이 동반 강세를 보이면서 업종별 체감 강도도 개선됐고, 프로그램 매수 유입으로 상승 탄력이 강화됐습니다.",
                "단기적으로는 환율 안정과 미국 금리 하향 기대가 위험자산 선호를 뒷받침하지만, 오후장에는 차익실현 물량 증가 여부가 변동성 핵심 변수입니다."
            )
        ),
        Article(
            source = "Reuters",
            headline = "미 10년물 금리 4.2% 하회, 기술주 선물 강세 확대",
            summary = "금리 하락과 함께 나스닥 선물이 반등세를 보인다.",
            time = "2분 전",
            category = "금리",
            tag = "금리",
            summaryPoints = listOf(
                "미국 10년물 국채금리가 4.2% 아래로 내려오면서 밸류에이션 부담이 컸던 대형 기술주 선물이 동반 반등했고, 나스닥 선물은 개장 전 0.9% 상승했습니다.",
                "최근 발표된 고용·소비 지표가 시장 예상보다 완만하게 둔화되며 연준의 추가 긴축 가능성이 낮아졌다는 해석이 채권시장 강세로 이어졌습니다.",
                "금리 민감 업종의 단기 반등 여력은 커졌지만, 물가 지표 재상승 시 수익률이 빠르게 되돌릴 수 있어 포지션 과열 구간에 대한 관리가 필요합니다."
            )
        ),
        Article(
            source = "전자신문",
            headline = "국내 AI 반도체 스타트업, 대형 수주 계약 체결로 기대 확대",
            summary = "데이터센터용 추론칩 납품 계약이 체결됐다.",
            time = "5분 전",
            category = "IT",
            tag = "IT",
            summaryPoints = listOf(
                "국내 AI 반도체 스타트업이 연간 약 1,500억원 규모의 데이터센터 추론칩 공급 계약을 체결하며, 상용 매출 가시성을 크게 높였습니다.",
                "주문처는 국내외 클라우드 사업자 복수로 알려졌고, 전력 효율 개선 수치가 기존 GPU 대비 최대 30% 수준이라는 점이 수주 배경으로 제시됐습니다.",
                "국내 팹리스 생태계에 대한 재평가가 확산될 수 있지만, 초기 양산 수율과 후속 고객 확장 속도가 밸류에이션 프리미엄 유지의 핵심 분기점입니다."
            )
        ),
        Article(
            source = "매일경제",
            headline = "원달러 환율 1355원대 진입, 수입주 원가 부담 완화",
            summary = "달러 약세가 이어지며 원화 강세 흐름이 나타났다.",
            time = "8분 전",
            category = "매크로",
            tag = "매크로",
            summaryPoints = listOf(
                "원/달러 환율이 장중 1,355원대로 하락하면서 전일 대비 9원가량 원화가 강세를 보였고, 수입 원가 부담이 큰 업종 중심으로 투자심리가 개선됐습니다.",
                "달러 인덱스가 103선 초반으로 내려오고 아시아 통화 전반이 동반 강세를 보인 점이 환율 하락을 지지했으며, 외국인 주식 순매수도 같은 방향으로 작용했습니다.",
                "환율 안정은 물가와 기업 마진 측면에서 긍정적이지만, 지정학 이벤트나 미국 지표 서프라이즈 발생 시 1,360원대 재돌파 가능성도 함께 열어둘 필요가 있습니다."
            )
        ),
        Article(
            source = "조선비즈",
            headline = "서울 아파트 매물 소진 빨라져, 핵심지 호가 상승세 확대",
            summary = "핵심 지역 중심으로 매물 감소가 나타나고 있다.",
            time = "11분 전",
            category = "부동산",
            tag = "부동산",
            summaryPoints = listOf(
                "서울 주요 권역에서 매물 회전일수가 단축되고 급매가 빠르게 소진되며, 일부 단지에서는 최근 2주 사이 호가가 1~3% 상향 조정됐습니다.",
                "대출 규제 완화 기대와 전세가격 반등이 매수 대기 수요를 자극했고, 신규 공급 불확실성이 겹치며 실수요 중심 거래가 점진적으로 회복되는 흐름입니다.",
                "거래량 절대 수준은 아직 과거 평균 대비 낮아 추세 전환 단정은 이르지만, 정책 방향과 금리 경로에 따라 하반기 가격 변동 폭이 확대될 가능성이 있습니다."
            )
        ),
        Article(
            source = "Bloomberg",
            headline = "엔비디아 공급망 병목 완화 신호, 부품주 동반 상승 확산",
            summary = "GPU 공급 안정 기대가 확대되며 관련주가 강세다.",
            time = "14분 전",
            category = "빅테크",
            tag = "빅테크",
            summaryPoints = listOf(
                "엔비디아 핵심 부품 리드타임이 직전 분기 대비 단축됐다는 공급망 점검 결과가 나오면서, 관련 메모리·패키징 업체 주가가 동반 상승했습니다.",
                "서버용 GPU 출하 병목이 완화되면 클라우드 사업자의 AI 인프라 집행 속도가 빨라질 수 있어, 후방 산업 전반의 실적 추정치 상향 여지가 커졌습니다.",
                "다만 특정 부품군의 공급 타이트는 여전히 남아 있어 완전한 정상화로 보기에는 이르며, 하반기 수요 탄력과 ASP 유지 여부를 함께 확인해야 합니다."
            )
        )
    )

    val NEWS_GLOBAL_MARKET = listOf(
        Article(
            source = "한국경제",
            headline = "한은 기준금리 동결 유지, 연내 인하 시점 신중 검토",
            summary = "물가 둔화와 환율 변동성 사이 균형을 유지했다.",
            time = "오늘",
            category = "금리",
            tag = "금리",
            summaryPoints = listOf(
                "한국은행은 기준금리를 현 수준으로 동결하며 물가 둔화 흐름을 확인했지만, 환율과 가계부채 변동성을 고려해 즉각적인 인하 신호는 유보했습니다.",
                "금통위는 연내 정책 전환 가능성을 열어두되 핵심 물가와 대외금리 경로를 추가 점검하겠다는 입장을 유지했고, 채권시장은 하반기 1회 인하 확률을 높였습니다.",
                "정책 불확실성 완화로 금융주에는 중립적이지만, 금리 민감 성장주의 재평가 폭은 향후 물가 지표와 원화 안정 여부에 따라 크게 달라질 전망입니다."
            )
        ),
        Article(
            source = "연합뉴스",
            headline = "국회 5월 임시회 소집, AI 기본법 처리 논의 본격화",
            summary = "AI 제도화 논의가 본격화됐다.",
            time = "오늘",
            category = "정치",
            tag = "정치",
            summaryPoints = listOf(
                "국회가 5월 임시회를 소집하면서 AI 기본법 처리 일정이 구체화됐고, 데이터 활용·책임 규정·안전성 기준을 둘러싼 조율이 본격화됐습니다.",
                "법안이 통과되면 공공·금융·의료 분야의 AI 도입 가이드라인이 선명해져 기업들의 서비스 출시 속도가 빨라질 수 있다는 기대가 커지고 있습니다.",
                "반면 규제 강도와 시행 시점이 과도하게 보수적으로 설계될 경우 초기 투자 비용이 상승할 수 있어, 세부 시행령의 균형이 산업 영향의 핵심 변수입니다."
            )
        ),
        Article(
            source = "조선비즈",
            headline = "현대차 전기차 신모델 북미 출시 일정 공개, 점유율 확대 승부",
            summary = "북미 시장 공략 로드맵이 공개됐다.",
            time = "오늘",
            category = "산업",
            tag = "산업",
            summaryPoints = listOf(
                "현대차는 차세대 전기차 신모델의 북미 출시 로드맵과 생산 계획을 공개하며, 현지 판매 확대를 위한 라인업 보강 일정을 구체화했습니다.",
                "현지 생산 비중 확대와 배터리 조달 체계 안정화 계획이 함께 제시되며 IRA 인센티브 수혜 가능성이 커졌고, 공급망 리스크 완화 기대도 반영됐습니다.",
                "다만 가격 경쟁 심화와 보조금 정책 변화에 따라 초기 마진 변동 가능성이 남아 있어, 출시 이후 예약률과 ASP 방어력이 실적의 핵심 지표가 됩니다."
            )
        ),
        Article(
            source = "Reuters",
            headline = "유럽중앙은행 6월 금리 인하 유력, 정책 전환 기대 확산",
            summary = "정책 전환 기대가 강화됐다.",
            time = "오늘",
            category = "매크로",
            tag = "매크로",
            summaryPoints = listOf(
                "유럽중앙은행 주요 인사 발언이 완화적으로 전환되며 6월 금리 인하 가능성이 사실상 기정사실화됐고, 유럽 국채금리는 전반적으로 하락했습니다.",
                "통화정책 전환 기대는 유로 약세와 함께 글로벌 위험자산 선호를 자극했으며, 특히 경기민감 업종과 성장주에 우호적인 투자 환경을 조성했습니다.",
                "다만 인하 이후 경기 회복 속도가 기대에 못 미치면 정책 효과가 제한될 수 있어, 하반기 기업 실적과 소비지표 개선 여부가 시장 방향을 결정할 전망입니다."
            )
        ),
        Article(
            source = "매일경제",
            headline = "시중은행 예금금리 다시 하락세, 고금리 특판 빠르게 축소",
            summary = "고금리 특판이 빠르게 축소되고 있다.",
            time = "오늘",
            category = "금융",
            tag = "금융",
            summaryPoints = listOf(
                "주요 시중은행의 정기예금 금리가 다시 하락세로 돌아서며 고금리 특판 상품이 빠르게 축소됐고, 단기 자금의 이동 경로에 변화가 나타나고 있습니다.",
                "기준금리 인하 기대가 반영되면서 조달 비용 부담을 낮추려는 은행권 전략이 강화됐고, 예대마진 방어와 수신 경쟁 완화가 동시에 진행되는 분위기입니다.",
                "예금금리 하락은 가계의 자산 배분을 예금에서 투자성 상품으로 이동시키는 촉매가 될 수 있어, 자본시장 유동성 측면에서 중기적으로 긍정적 신호입니다."
            )
        ),
        Article(
            source = "전자신문",
            headline = "카카오 자회사 구조조정 마무리 단계, 핵심 사업 재편 가속",
            summary = "핵심 사업 중심 재편이 마무리 단계다.",
            time = "오늘",
            category = "빅테크",
            tag = "빅테크",
            summaryPoints = listOf(
                "카카오의 비핵심 자회사 정리와 조직 재편이 마무리 단계에 진입하며, 비용 구조 단순화와 핵심 플랫폼 집중 전략이 본격화되고 있습니다.",
                "중복 기능 통합과 투자 우선순위 조정으로 고정비 부담이 완화될 경우, 광고·커머스·AI 서비스 중심의 수익성 개선 속도가 빨라질 가능성이 있습니다.",
                "다만 구조조정 이후 성장 동력 재확인이 지연되면 밸류에이션 리레이팅은 제한될 수 있어, 신규 서비스 지표와 분기별 이익 체력 확인이 필요합니다."
            )
        )
    )

    /** 해외 뉴스 탭용 목업 (영문 제목 · CNBC / Reuters / Bloomberg) */
    val OVERSEAS_NEWS_REALTIME = listOf(
        Article(
            source = "Bloomberg",
            headline = "Fed officials signal patience on cuts as inflation lingers above target",
            summary = "Several policymakers said they want more months of data before easing again.",
            time = "Just now",
            category = "Markets",
            tag = "MACRO",
            summaryPoints = listOf(
                "Minutes leaned toward a slower easing path than futures had priced.",
                "Core services inflation remains sticky in major metro areas.",
                "Traders trimmed June cut probabilities after the remarks."
            )
        ),
        Article(
            source = "Reuters",
            headline = "Oil slips as ceasefire talks progress; risk assets drift higher",
            summary = "Crude benchmarks fell about 1% while equities held modest gains.",
            time = "3m ago",
            category = "Energy",
            tag = "COMMODITIES",
            summaryPoints = listOf(
                "Brent moved back below \$83 on headline-driven flows.",
                "Shipping risk premia compressed slightly in Asian hours.",
                "Refiners are watching diesel cracks into summer driving season."
            )
        ),
        Article(
            source = "CNBC",
            headline = "Nvidia supplier checks point to improving lead times for AI accelerators",
            summary = "Channel checks suggest backlog digestion is ahead of plan.",
            time = "6m ago",
            category = "Tech",
            tag = "SEMIS",
            summaryPoints = listOf(
                "Packaging and memory vendors saw order visibility extend two quarters.",
                "Hyperscaler capex commentary remained constructive on AI clusters.",
                "Street models nudged up server shipment forecasts for Q3."
            )
        ),
        Article(
            source = "Bloomberg",
            headline = "Treasury yields dip after soft retail sales surprise",
            summary = "The 10-year yield fell 6bp as growth worries resurfaced.",
            time = "9m ago",
            category = "Rates",
            tag = "BONDS",
            summaryPoints = listOf(
                "Control group sales missed consensus for the first time in three prints.",
                "Curve bull-flattened with front-end outperforming.",
                "Fed watch tools showed a higher bar for a July move."
            )
        ),
        Article(
            source = "Reuters",
            headline = "EU trade chief warns of retaliation risk in transatlantic tariff row",
            summary = "Brussels reiterated readiness to respond if levies broaden.",
            time = "12m ago",
            category = "Geopolitics",
            tag = "TRADE",
            summaryPoints = listOf(
                "Automotive and pharma sectors were cited as sensitive areas.",
                "Euro dipped modestly on headline risk.",
                "Corporate treasuries flagged higher hedging costs into Q3."
            )
        )
    )

    val OVERSEAS_NEWS_POPULAR = listOf(
        Article(
            source = "CNBC",
            headline = "Magnificent Seven earnings: what Wall Street is watching this season",
            summary = "Analysts focus on AI monetization, cloud margins, and capex cadence.",
            time = "Top",
            category = "Equities",
            tag = "EARNINGS",
            summaryPoints = listOf(
                "Guidance bandwidth is wider than usual after tariff headlines.",
                "Buybacks could be a swing factor if cash yields stay elevated.",
                "Options markets priced a slightly softer reaction window."
            )
        ),
        Article(
            source = "Bloomberg",
            headline = "JPMorgan lifts S&P 500 year-end target on resilient earnings breadth",
            summary = "Strategists cited better-than-feared margins in industrials and financials.",
            time = "Top",
            category = "Strategy",
            tag = "INDEX",
            summaryPoints = listOf(
                "Revisions breadth turned positive for the first time in 2026.",
                "Small caps still lag but participation improved last week.",
                "Risk parity funds added modest equity beta."
            )
        ),
        Article(
            source = "Reuters",
            headline = "Apple explores on-device models for next-generation Siri features",
            summary = "Sources said privacy and latency drove the architecture choice.",
            time = "Top",
            category = "Tech",
            tag = "AAPL",
            summaryPoints = listOf(
                "Silicon teams are aligning with a common inference runtime.",
                "Partners expect developer APIs to tighten review requirements.",
                "Competitive response from Android OEMs is likely within two cycles."
            )
        ),
        Article(
            source = "CNBC",
            headline = "Goldman: hedge funds rotated from crowded growth into quality cyclicals",
            summary = "Prime book data showed two weeks of net buying in materials and energy.",
            time = "Top",
            category = "Flows",
            tag = "HF",
            summaryPoints = listOf(
                "Crowding scores in semis declined from late-2025 peaks.",
                "Macro pods added crude and copper deltas selectively.",
                "Risk appetite remains fragile around payroll weeks."
            )
        ),
        Article(
            source = "Bloomberg",
            headline = "BOE holds rates steady but opens door to summer easing",
            summary = "Governor remarks emphasized services inflation persistence.",
            time = "Top",
            category = "FX",
            tag = "GBP",
            summaryPoints = listOf(
                "Sterling slipped 0.3% against the dollar after the decision.",
                "OIS priced one cut by September with low conviction.",
                "Real yields in gilts remain attractive to overseas asset managers."
            )
        )
    )

    val OVERSEAS_NEWS_MAIN = listOf(
        Article(
            source = "Reuters",
            headline = "Global manufacturing PMI edges up; Asia factory orders improve",
            summary = "New export orders rose for the first time in six months in the regional gauge.",
            time = "Main",
            category = "Macro",
            tag = "PMI",
            summaryPoints = listOf(
                "Electronics supply chains showed the largest sequential gain.",
                "Auto production schedules stabilized in ASEAN hubs.",
                "Inventory restocking narratives gained traction in sell-side notes."
            )
        ),
        Article(
            source = "CNBC",
            headline = "Microsoft closes gap with OpenAI partnership renewal, analysts say",
            summary = "The multi-year pact reinforces Azure as default training substrate.",
            time = "Main",
            category = "Cloud",
            tag = "MSFT",
            summaryPoints = listOf(
                "Enterprise renewals cited as the key upside driver.",
                "Capex intensity will be dissected on the next call.",
                "Regulatory scrutiny on exclusivity clauses remains an overhang."
            )
        ),
        Article(
            source = "Bloomberg",
            headline = "Tesla robotaxi pilot expands to two new cities; insurance partners named",
            summary = "The program adds incremental miles but profitability timeline is debated.",
            time = "Main",
            category = "Autos",
            tag = "TSLA",
            summaryPoints = listOf(
                "Safety incident rate stayed below internal thresholds in early data.",
                "Underwriters pushed for dynamic pricing tied to weather and traffic.",
                "Short interest ticked lower after the announcement."
            )
        ),
        Article(
            source = "Reuters",
            headline = "OPEC+ maintains output stance; delegates cite balanced market",
            summary = "Ministers left quotas unchanged while monitoring voluntary cuts compliance.",
            time = "Main",
            category = "Energy",
            tag = "OIL",
            summaryPoints = listOf(
                "Saudi guidance reiterated readiness to act if inventories build.",
                "Non-OPEC supply growth from the Americas remains a headwind.",
                "Asian refiners trimmed sour crude differentials slightly."
            )
        ),
        Article(
            source = "CNBC",
            headline = "Amazon Web Services unveils cheaper inference tier for startups",
            summary = "The move targets price-sensitive workloads as competition intensifies.",
            time = "Main",
            category = "Cloud",
            tag = "AMZN",
            summaryPoints = listOf(
                "Credits bundle with marketplace listings for eligible founders.",
                "Analysts see limited near-term ARPU impact but higher attach potential.",
                "Competitors are expected to match within a quarter."
            )
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

    val companyFinancialSeries: Map<String, FinancialSeries> = mapOf(
        "005930" to FinancialSeries(
            metrics = listOf(
                metric("매출", 250f to "250조원", 275f to "275조원", 300f to "300조원", 320f to "320조원", 340f to "340조원"),
                metric("영업이익", 2.1f to "2.1조원", 2.4f to "2.4조원", 2.8f to "2.8조원", 3.0f to "3조원", 3.4f to "3.4조원"),
                metric("순이익", 1.5f to "1.5조원", 1.8f to "1.8조원", 2.1f to "2.1조원", 2.4f to "2.4조원", 2.7f to "2.7조원"),
                metric("EPS", 2500f to "2,500원", 2850f to "2,850원", 3200f to "3,200원", 3600f to "3,600원", 4100f to "4,100원"),
                metric("PER", 15.1f to "15.1배", 16.0f to "16.0배", 16.9f to "16.9배", 17.6f to "17.6배", 18.4f to "18.4배"),
                metric("ROE", 7.4f to "7.4%", 8.5f to "8.5%", 9.6f to "9.6%", 10.8f to "10.8%", 12.1f to "12.1%")
            )
        ),
        "000660" to FinancialSeries(
            metrics = listOf(
                metric("매출", 36f to "36조원", 41f to "41조원", 46f to "46조원", 52f to "52조원", 58f to "58조원"),
                metric("영업이익", 6.1f to "6.1조원", 7.2f to "7.2조원", 8.4f to "8.4조원", 10.1f to "10.1조원", 12.2f to "12.2조원"),
                metric("순이익", 4.8f to "4.8조원", 5.8f to "5.8조원", 6.8f to "6.8조원", 8.0f to "8조원", 9.6f to "9.6조원"),
                metric("EPS", 8600f to "8,600원", 10300f to "10,300원", 12000f to "12,000원", 14200f to "14,200원", 16800f to "16,800원"),
                metric("PER", 10.2f to "10.2배", 11.0f to "11.0배", 11.8f to "11.8배", 13.2f to "13.2배", 14.6f to "14.6배"),
                metric("ROE", 11.1f to "11.1%", 12.6f to "12.6%", 14.2f to "14.2%", 15.9f to "15.9%", 17.3f to "17.3%")
            )
        ),
        "NVDA" to FinancialSeries(
            metrics = listOf(
                metric("매출", 27f to "$27B", 43f to "$43B", 61f to "$61B", 130f to "$130B", 195f to "$195B"),
                metric("영업이익", 12f to "$12B", 21f to "$21B", 33f to "$33B", 82f to "$82B", 124f to "$124B"),
                metric("순이익", 10f to "$10B", 18f to "$18B", 30f to "$30B", 73f to "$73B", 112f to "$112B"),
                metric("EPS", 0.4f to "$0.4", 0.8f to "$0.8", 1.2f to "$1.2", 2.9f to "$2.9", 4.5f to "$4.5"),
                metric("PER", 36.1f to "36.1배", 40.3f to "40.3배", 44.5f to "44.5배", 48.2f to "48.2배", 52.1f to "52.1배"),
                metric("ROE", 40.2f to "40.2%", 45.6f to "45.6%", 51.0f to "51.0%", 57.4f to "57.4%", 61.8f to "61.8%")
            )
        ),
        "035420" to FinancialSeries(
            metrics = listOf(
                metric("매출", 9.4f to "9.4조원", 10.1f to "10.1조원", 10.8f to "10.8조원", 11.5f to "11.5조원", 12.6f to "12.6조원"),
                metric("영업이익", 1.2f to "1.2조원", 1.4f to "1.4조원", 1.6f to "1.6조원", 1.8f to "1.8조원", 2.1f to "2.1조원"),
                metric("순이익", 0.95f to "0.95조원", 1.08f to "1.08조원", 1.2f to "1.2조원", 1.35f to "1.35조원", 1.55f to "1.55조원"),
                metric("EPS", 4700f to "4,700원", 5150f to "5,150원", 5600f to "5,600원", 6300f to "6,300원", 7100f to "7,100원"),
                metric("PER", 18.7f to "18.7배", 19.6f to "19.6배", 20.6f to "20.6배", 21.4f to "21.4배", 22.7f to "22.7배"),
                metric("ROE", 6.4f to "6.4%", 7.1f to "7.1%", 7.9f to "7.9%", 8.6f to "8.6%", 9.4f to "9.4%")
            )
        ),
        "AAPL" to FinancialSeries(
            metrics = listOf(
                metric("매출", 356f to "$356B", 374f to "$374B", 391f to "$391B", 408f to "$408B", 427f to "$427B"),
                metric("영업이익", 108f to "$108B", 115f to "$115B", 121f to "$121B", 126f to "$126B", 132f to "$132B"),
                metric("순이익", 86f to "$86B", 91f to "$91B", 97f to "$97B", 101f to "$101B", 106f to "$106B"),
                metric("EPS", 5.6f to "$5.6", 6.0f to "$6.0", 6.4f to "$6.4", 6.8f to "$6.8", 7.2f to "$7.2"),
                metric("PER", 25.9f to "25.9배", 26.7f to "26.7배", 27.5f to "27.5배", 28.1f to "28.1배", 29.0f to "29.0배"),
                metric("ROE", 144f to "144%", 148f to "148%", 152f to "152%", 156f to "156%", 161f to "161%")
            )
        ),
        "051910" to FinancialSeries(
            metrics = listOf(
                metric("매출", 41f to "41조원", 45f to "45조원", 49f to "49조원", 53f to "53조원", 58f to "58조원"),
                metric("영업이익", 1.9f to "1.9조원", 2.4f to "2.4조원", 2.9f to "2.9조원", 3.4f to "3.4조원", 4.1f to "4.1조원"),
                metric("순이익", 1.3f to "1.3조원", 1.6f to "1.6조원", 2.0f to "2조원", 2.5f to "2.5조원", 3.1f to "3.1조원"),
                metric("EPS", 7600f to "7,600원", 8700f to "8,700원", 9800f to "9,800원", 11300f to "11,300원", 12900f to "12,900원"),
                metric("PER", 13.4f to "13.4배", 14.5f to "14.5배", 15.6f to "15.6배", 16.8f to "16.8배", 18.1f to "18.1배"),
                metric("ROE", 4.4f to "4.4%", 5.3f to "5.3%", 6.2f to "6.2%", 7.5f to "7.5%", 8.9f to "8.9%")
            )
        ),
        "TSLA" to FinancialSeries(
            metrics = listOf(
                metric("매출", 73f to "$73B", 85f to "$85B", 97f to "$97B", 112f to "$112B", 128f to "$128B"),
                metric("영업이익", 5.4f to "$5.4B", 6.7f to "$6.7B", 8.1f to "$8.1B", 10.4f to "$10.4B", 13.0f to "$13.0B"),
                metric("순이익", 4.3f to "$4.3B", 5.5f to "$5.5B", 6.7f to "$6.7B", 8.6f to "$8.6B", 10.9f to "$10.9B"),
                metric("EPS", 2.1f to "$2.1", 2.6f to "$2.6", 3.2f to "$3.2", 4.0f to "$4.0", 4.9f to "$4.9"),
                metric("PER", 33.2f to "33.2배", 37.8f to "37.8배", 42.7f to "42.7배", 49.3f to "49.3배", 56.1f to "56.1배"),
                metric("ROE", 10.1f to "10.1%", 12.2f to "12.2%", 14.3f to "14.3%", 16.7f to "16.7%", 19.8f to "19.8%")
            )
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

    private fun metric(
        title: String,
        @Suppress("UNUSED_PARAMETER") y2022: Pair<Float, String>,
        y2023: Pair<Float, String>,
        y2024: Pair<Float, String>,
        y2025: Pair<Float, String>,
        y2026: Pair<Float, String>
    ): FinancialMetric = FinancialMetric(
        title = title,
        values = listOf(
            YearFinancialValue("2023", y2023.first, y2023.second),
            YearFinancialValue("2024", y2024.first, y2024.second),
            YearFinancialValue("2025", y2025.first, y2025.second),
            YearFinancialValue("2026", y2026.first, y2026.second)
        )
    )
}
