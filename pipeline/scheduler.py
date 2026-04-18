"""
KST 기준 06:30, 12:00, 15:00, 18:00에 main.run() 실행.
`schedule` 라이브러리로 분 단위 체크 후, KST 시각이 슬롯과 일치할 때만 run() 호출.
"""
from __future__ import annotations

import logging
import time
from datetime import datetime

import pytz
import schedule

from main import run

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
)
logger = logging.getLogger("scheduler")

RUN_SLOTS_KST = ("06:30", "12:00", "15:00", "18:00")

_last_fired: tuple | None = None


def _kst_slot() -> tuple[datetime, str]:
    kst = pytz.timezone("Asia/Seoul")
    now = datetime.now(kst)
    return now, f"{now.hour:02d}:{now.minute:02d}"


def tick() -> None:
    global _last_fired  # noqa: PLW0603
    now, hm = _kst_slot()
    if hm not in RUN_SLOTS_KST:
        return
    key = (now.date(), hm)
    if key == _last_fired:
        return
    _last_fired = key
    logger.info("KST %s — run() 시작", hm)
    try:
        run()
    except Exception:
        logger.exception("run() 실패")
    else:
        logger.info("run() 정상 종료")


def main_loop() -> None:
    logger.info("스케줄러 시작 (KST %s, 매 분 검사)", ", ".join(RUN_SLOTS_KST))
    schedule.every(1).minutes.do(tick)
    while True:
        schedule.run_pending()
        time.sleep(1)


if __name__ == "__main__":
    main_loop()
