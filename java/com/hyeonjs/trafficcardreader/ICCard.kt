package com.hyeonjs.trafficcardreader

import android.nfc.tech.IsoDep
import kotlin.experimental.and

class ICCard {
    var type: String? = null
    var number: String? = null
    var aid: String? = null
    var aidSize: String? = null
    var balanceCmd: String = "905C000004"
    var balance: Int = 0

    @Throws(Exception::class)
    fun ICCard(id: IsoDep) {
        id.connect()
        // CLA | INS | P1 | P2 | Lc | Data           | Le
        // 00  | A4  | 04 | 00 | 07 | A0000004520001 | 00
        // Lc는 Data로 넘길 값의 길이
        // DF Name을 Data로 넘김
        // Le는 결과물로 예상되는 최대 바이트 수 (00은 256를 의미)
        val cmd = str2bytes("00"+"A4"+"04"+"00" + "07" + "A0000004520001" + "00")
        val transceive = id.transceive(cmd)

        val rawData = bytes2hex(transceive)
        val data = rawData.toCharArray()

        // TAG, 크기, 값 순서대로 나옴. 크기랑 값 중 하나는 안나오는 듯?
        // TAG 순서 : FCI Template | DF Name | FCI Proprietary Template | 카드 규격 및 선/후불 구분 | 지원 항목 | ID CENTER | 잔액조회명령 | 교통 호환 ADF AID | 부가 데이터 파일 정보 | 카드 타입 (성인/어린이/청소년 등) | 유효기간 | 카드일련번호 | 카드관리번호 | 카드 사업자 임의 정보 | 처리 결과
        // FCI Template : 6F, 필수
        // DF Name : 84, A0000004520001로 값 고정, 필수
        // FCI Proprietary Template  : A5, 필수
        // 카드 규격 및 선/후불 구분 : 50, 필수
        // 지원 항목 : 47, 지원하는 NFC 표준 방식이나 하이패스 같은거 정보, 필수
        // ID CENTER : 43, 카드 사업자 구분하는 정보 (티머니, 캐시비 등), 필수
        // 잔액조회명령 : 11, 905C000004가 표준인데, 종종 다름, 선택
        // 교통 호환 ADF AID : 4F, D4106509900020가 표준이라는 것 같은데, D4106509900020가 나오는걸 본 적이 없음, 필수
        // 부가 데이터 파일 정보 : 9F10, 필수
        // 카드 타입 (성인/어린이/청소년 등) : 45, 필수
        // 유효기간 : 5F25, 필수, 근데 국내 교통카드는 유효기간 없지 않나요?
        // 카드일련번호 : 12, 선택
        // 카드관리번호 : 13, 선택
        // 카드 사업자 임의 정보 : BF0C, 선택
        // 처리 결과 : 90 00이면 정상 처리

        //정상적으로 처리되지 않은 경우 걸러내기
        if (!rawData.endsWith("9000")) return

        //2칸씩( = 1바이트) 읽음, 중간에 n값 수정해서 이미 읽거나 안읽어도 되는 부분 넘김
        var n = 0
        while (n < data.size) {

            // 현재 바이트
            val s = data[n].toString() + "" + data[n + 1]

            // FCI Template
            if (s == "6F") {
                n += 2 //용도 모름
            }

            // DF Name (전용파일 이름)
            else if (s == "84") {
                val size = ("" + data[n + 2] + data[n + 3]).toInt(16)
                n += 2 * size + 2 //어차피 A0000004520001로 동일하니, 별다른 처리 안함. 애초에 다른 값이였다면 정상적으로 안읽힘
            }

            //FCI Proprietary Template
            else if (s == "A5") {
                n += 2 //용도 모름
            }

            //선불/후불 구분
            else if (s == "50") {
                val size = ("" + data[n + 2] + data[n + 3]).toInt()
                n += 2 * size + 2 //선불은 01 00, 후불은 11 00이지만 이 앱애서는 구분 안할 예정
            }

            //지원 항목
            else if (s == "47") {
                val size = ("" + data[n + 2] + data[n + 3]).toInt(16)
                n += 2 * size + 2 //이 앱에서는 사용하지 않을 예정
            }

            //ID CENTER (카드 사업자 구분)
            else if (s == "43") {
                val size = ("" + data[n + 2] + data[n + 3]).toInt(16)
                //티머니, 캐시비 등 구분
                type = when (rawData.substring(n + 4, n + 4 + 2 * size).toInt(16)) {
                    0x08 -> "티머니"
                    0x0B -> "캐시비"
                    else -> "(알 수 없음)"
                }
                n += 2 * size + 2
            }

            //잔액조회명령
            else if (s == "11") {
                val size = ("" + data[n + 2] + data[n + 3]).toInt(16)
                balanceCmd = rawData.substring(n + 4, n + 4 + 2 * size) //이걸로 transceive 또 보내서 잔액 확인 가능
                n += 2 * size + 2
            }

            //교통 호환 ADF AID
            else if (s == "4F") {
                val size = ("" + data[n + 2] + data[n + 3]).toInt(16) //표준은 7인데 종종 8인 카드도 있는 듯
                aid = rawData.substring(n + 4, n + 4 + 2 * size)
                aidSize = rawData.substring(n + 2, n + 2 + 2) //요청 넣을 때 따로 계산하기 귀찮아요
                n += 2 * size + 2
            }

            //부가 데이터 파일 정보
            else if (s == "9F" && data[n + 2] == '1' && data[n + 3] == '0') {
                val size = ("" + data[n + 4] + data[n + 5]).toInt(16)
                n += 2 * size + 4 //용도 모름, EA 00 34로 고정인 듯
            }

            //카드 타입 정보
            else if (s == "45") {
                val size = ("" + data[n + 2] + data[n + 3]).toInt()
                n += 2 * size + 2 //일반, 청소년용 등 구분하는 값인데, 이 앱에서는 사용하지 않을 예정
            }

            //카드 유효기간 - YYMM 형식, 근데 이제 유효기간 없어지지 않았나?
            else if (s == "5F" && data[n + 2] == '2' && data[n + 3] == '4') {
                val size = ("" + data[n + 4] + data[n + 5]).toInt(16)
                n += 2 * size + 4 //교통카드는 유효기간 없는걸로 아는데, 아무튼 이 앱에서는 사용하지 않을 예정
            }

            //카드 일련번호
            else if (s == "12") {
                val size = ("" + data[n + 2] + data[n + 3]).toInt(16)
                number = rawData.substring(n + 4, n + 4 + 2 * size)
                n += 2 * size + 2
            }

            //카드 관리번호
            else if (s == "13") {
                val size = ("" + data[n + 2] + data[n + 3]).toInt(16)
                n += 2 * size + 2 //이 앱애서는 사용하지 않을 예정
            }

            //기타 카드 사업자 임의 정보
            else if (s == "BF" && data[n + 2] == '0' && data[n + 3] == 'C') {
                val size = ("" + data[n + 4] + data[n + 5]).toInt(16)
                n += 2 * size + 4 //뭔지 모루겟소요
            }

            n += 2
        }

        //잔액조회 1 - 잔액조회 하기 전에 AID 넘겨줘야 됨
        // CLA | INS | P1 | P2 | Lc        | Data  | Le
        // 00  | A4  | 04 | 00 | {aidSize} | {aid} | 00
        id.transceive(str2bytes("00A40400" + aidSize.toString() + aid.toString() + "00"))

        //잔액조회 2 - 잔액 조회 명령 전달 부분
        // CLA | INS | P1 | P2 | Lc | Data         | Le
        // 00  | A4  | 04 | 00 | 07 | {balanceCmd} | 00
        val balance = bytes2hex(id.transceive(str2bytes(balanceCmd)))
        this.balance = balance.substring(0, 8).toInt(16)

        id.close()
    }


    private val HEX_ARRAY = "0123456789ABCDEF".toCharArray()
    fun bytes2hex(bytes: ByteArray): String {
        val hex = CharArray(2 * bytes.size)
        bytes.forEachIndexed { i, byte ->
            val unsigned = 0xff and byte.toInt()
            hex[2 * i] = HEX_ARRAY[unsigned / 16]
            hex[2 * i + 1] = HEX_ARRAY[unsigned % 16]
        }

        return hex.joinToString("")
    }

    private fun str2bytes(str: String): ByteArray {
        val length = str.length
        val arr = ByteArray(length / 2)
        var n = 0
        while (n < length) {
            arr[n / 2] =
                ((Character.digit(str[n], 16) shl 4) + Character.digit(str[n + 1], 16)).toByte()
            n += 2
        }
        return arr
    }

}
