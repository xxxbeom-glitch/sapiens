package com.sapiens.app.data.stock.dto

import com.google.gson.annotations.SerializedName

/** KRX 데이터시스템 `MDCSTAT03501` (투자심리·배당 등) JSON 응답. */
data class KrxMdcstat03501ResponseDto(
    @SerializedName("output")
    val output: List<KrxMdcstat03501RowDto>? = null,
)

data class KrxMdcstat03501RowDto(
    @SerializedName("PER")
    val per: String? = null,
    @SerializedName("PBR")
    val pbr: String? = null,
    @SerializedName("EPS")
    val eps: String? = null,
    /** 배당수익률 */
    @SerializedName("DVR")
    val dvr: String? = null,
    @SerializedName("DPS")
    val dps: String? = null,
)
