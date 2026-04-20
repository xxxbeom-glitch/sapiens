package com.sapiens.app.data.stock.api

import com.sapiens.app.data.stock.dto.KrxMdcstat03501ResponseDto
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Headers
import retrofit2.http.POST

/**
 * KRX 데이터시스템 공통 JSON 엔드포인트.
 * @see <a href="https://data.krx.co.kr/">data.krx.co.kr</a>
 */
interface KrxDataSystemApi {

    @Headers(
        "Referer: https://data.krx.co.kr/",
        "Accept: application/json, text/plain, */*",
    )
    @FormUrlEncoded
    @POST("comm/bldAttendant/getJsonData.cmd")
    suspend fun getMdcstat03501(
        @Field("bld") bld: String,
        @Field("trdDd") trdDd: String,
        @Field("tboxisuCd_finder_stkisu0_0") tboxisuCdFinderStkisu0_0: String,
        @Field("isuCd") isuCd: String,
        @Field("isuCd2") isuCd2: String,
        @Field("codeNmIsuCd_finder_stkisu0_0") codeNmIsuCdFinderStkisu0_0: String,
        @Field("param1IsuCd_finder_stkisu0_0") param1IsuCdFinderStkisu0_0: String,
        @Field("trdDdTp") trdDdTp: String,
        @Field("strtDd") strtDd: String,
        @Field("endDd") endDd: String,
        @Field("share") share: String,
        @Field("money") money: String,
        @Field("csvxls_isNo") csvxlsIsNo: String,
    ): KrxMdcstat03501ResponseDto
}
