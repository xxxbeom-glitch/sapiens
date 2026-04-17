package com.breaktobreak.dailynews.data.mock

import com.breaktobreak.dailynews.data.model.Article
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
            tag = "ECON"
        ),
        Article(
            source = "매일경제",
            sourceColor = "kr",
            headline = "금리 인하 기대 속 코스피 2,850선 돌파",
            summary = "연준 6월 인하 가능성이 높아지며 외국인 매수세가 유입됐다. 전기차·2차전지 섹터가 지수 상승을 주도했다.",
            time = "오전 7:10",
            tag = "MARKET"
        ),
        Article(
            source = "조선비즈",
            sourceColor = "kr",
            headline = "서울 아파트 거래량 2년 만에 최고치 기록",
            summary = "4월 서울 아파트 매매 거래가 5,200건을 넘어서며 2024년 2월 이후 최고치를 기록했다. 강남3구와 마용성 중심으로 회복세가 뚜렷하다.",
            time = "오전 7:28",
            tag = "REAL ESTATE"
        ),
        Article(
            source = "연합뉴스",
            sourceColor = "kr",
            headline = "삼성전자, 2나노 파운드리 첫 양산 돌입",
            summary = "삼성전자가 화성 캠퍼스에서 2나노 공정 양산을 시작했다. 주요 고객사로 퀄컴과 테슬라가 거론되고 있다.",
            time = "오전 7:45",
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
            summary = "",
            time = "06:40"
        ),
        Article(
            source = "Reuters",
            headline = "애플, 서비스 매출 분기 사상 최대… AI 전환 가속",
            summary = "",
            time = "06:12"
        ),
        Article(
            source = "WSJ",
            headline = "JP모건 \"미국 경제 연착륙 확률 70%로 상향\"",
            summary = "",
            time = "05:58"
        ),
        Article(
            source = "CNBC",
            headline = "테슬라, 2분기 인도량 시장 예상 상회",
            summary = "",
            time = "05:30"
        ),
        Article(
            source = "Bloomberg",
            headline = "연준 의사록, 6월 금리 인하 신호 강화",
            summary = "",
            time = "05:02"
        ),
        Article(
            source = "FT",
            headline = "달러 인덱스 3주래 최저… 원화 강세 전환",
            summary = "",
            time = "04:45"
        )
    )
}
