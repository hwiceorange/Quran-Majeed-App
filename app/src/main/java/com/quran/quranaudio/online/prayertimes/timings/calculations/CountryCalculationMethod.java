package com.quran.quranaudio.online.prayertimes.timings.calculations;

import android.location.Address;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Author: Rai Adnan
 * Whatsapp: +923002375907
 * Email: officialshaheendevelopers@gmail.com
 * Portfolio: https://codecanyon.net/user/shaheendevelopers/portfolio
 */
public class CountryCalculationMethod {

    private static final Map<String, CalculationMethodEnum> CALCULATION_METHOD_BY_COUNTRY = createMap();

    private CountryCalculationMethod() {
    }

    private static Map<String, CalculationMethodEnum> createMap() {
        Map<String, CalculationMethodEnum> result = new HashMap<>();

        result.put("AF", CalculationMethodEnum.UNIVERSITY_OF_ISLAMIC_SCIENCES_KARACHI);
        result.put("AX", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("AL", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("DZ", CalculationMethodEnum.ALGERIAN_MINISTRY_OF_RELIGIOUS_AFFAIRS_AND_WAKFS);
        result.put("AS", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("AD", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("AO", CalculationMethodEnum.EGYPTIAN_GENERAL_AUTHORITY_OF_SURVEY);
        result.put("AI", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("AQ", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("AG", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("AR", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("AM", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("AW", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("AU", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("AT", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("AZ", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("BH", CalculationMethodEnum.UMM_AL_QURA_UNIVERSITY_MAKKAH);
        result.put("BD", CalculationMethodEnum.UNIVERSITY_OF_ISLAMIC_SCIENCES_KARACHI);
        result.put("BB", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("BY", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("BE", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("BZ", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("BJ", CalculationMethodEnum.EGYPTIAN_GENERAL_AUTHORITY_OF_SURVEY);
        result.put("BM", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("BT", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("BO", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("BQ", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("BA", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("BW", CalculationMethodEnum.EGYPTIAN_GENERAL_AUTHORITY_OF_SURVEY);
        result.put("BV", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("BR", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("IO", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("VG", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("BN", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("BG", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("BF", CalculationMethodEnum.EGYPTIAN_GENERAL_AUTHORITY_OF_SURVEY);
        result.put("BI", CalculationMethodEnum.EGYPTIAN_GENERAL_AUTHORITY_OF_SURVEY);
        result.put("KH", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("CM", CalculationMethodEnum.EGYPTIAN_GENERAL_AUTHORITY_OF_SURVEY);
        result.put("CA", CalculationMethodEnum.ISLAMIC_SOCIETY_OF_NORTH_AMERICA);
        result.put("CV", CalculationMethodEnum.EGYPTIAN_GENERAL_AUTHORITY_OF_SURVEY);
        result.put("KY", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("CF", CalculationMethodEnum.EGYPTIAN_GENERAL_AUTHORITY_OF_SURVEY);
        result.put("TD", CalculationMethodEnum.EGYPTIAN_GENERAL_AUTHORITY_OF_SURVEY);
        result.put("CL", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("CN", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("CX", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("CC", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("CO", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("KM", CalculationMethodEnum.EGYPTIAN_GENERAL_AUTHORITY_OF_SURVEY);
        result.put("CG", CalculationMethodEnum.EGYPTIAN_GENERAL_AUTHORITY_OF_SURVEY);
        result.put("CK", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("CR", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("CI", CalculationMethodEnum.EGYPTIAN_GENERAL_AUTHORITY_OF_SURVEY);
        result.put("HR", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("CU", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("CW", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("CY", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("CZ", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("CD", CalculationMethodEnum.EGYPTIAN_GENERAL_AUTHORITY_OF_SURVEY);
        result.put("DK", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("DJ", CalculationMethodEnum.EGYPTIAN_GENERAL_AUTHORITY_OF_SURVEY);
        result.put("DM", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("DO", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("EC", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("EG", CalculationMethodEnum.EGYPTIAN_GENERAL_AUTHORITY_OF_SURVEY);
        result.put("SV", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("GQ", CalculationMethodEnum.EGYPTIAN_GENERAL_AUTHORITY_OF_SURVEY);
        result.put("ER", CalculationMethodEnum.EGYPTIAN_GENERAL_AUTHORITY_OF_SURVEY);
        result.put("EE", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("ET", CalculationMethodEnum.EGYPTIAN_GENERAL_AUTHORITY_OF_SURVEY);
        result.put("FK", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("FO", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("FJ", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("FI", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("FR", CalculationMethodEnum.MOSQUEE_DE_PARIS_FRANCE);
        result.put("GF", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("PF", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("TF", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("GA", CalculationMethodEnum.EGYPTIAN_GENERAL_AUTHORITY_OF_SURVEY);
        result.put("GE", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("DE", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("GH", CalculationMethodEnum.EGYPTIAN_GENERAL_AUTHORITY_OF_SURVEY);
        result.put("GI", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("GR", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("GL", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("GD", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("GP", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("GU", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("GT", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("GG", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("GN", CalculationMethodEnum.EGYPTIAN_GENERAL_AUTHORITY_OF_SURVEY);
        result.put("GW", CalculationMethodEnum.EGYPTIAN_GENERAL_AUTHORITY_OF_SURVEY);
        result.put("GY", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("HT", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("HM", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("HN", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("HK", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("HU", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("IS", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("IN", CalculationMethodEnum.UNIVERSITY_OF_ISLAMIC_SCIENCES_KARACHI);
        result.put("IR", CalculationMethodEnum.INSTITUTE_OF_GEOPHYSICS_UNIVERSITY_OF_TEHRAN);
        result.put("IQ", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("IE", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("IL", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("IT", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("JM", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("JP", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("JE", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("JO", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("KZ", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("KE", CalculationMethodEnum.EGYPTIAN_GENERAL_AUTHORITY_OF_SURVEY);
        result.put("KI", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("XK", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("KW", CalculationMethodEnum.KUWAIT);
        result.put("KG", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("LA", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("LV", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("LB", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("LS", CalculationMethodEnum.EGYPTIAN_GENERAL_AUTHORITY_OF_SURVEY);
        result.put("LR", CalculationMethodEnum.EGYPTIAN_GENERAL_AUTHORITY_OF_SURVEY);
        result.put("LY", CalculationMethodEnum.EGYPTIAN_GENERAL_AUTHORITY_OF_SURVEY);
        result.put("LI", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("LT", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("LU", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("MO", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("MK", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("MG", CalculationMethodEnum.EGYPTIAN_GENERAL_AUTHORITY_OF_SURVEY);
        result.put("MW", CalculationMethodEnum.EGYPTIAN_GENERAL_AUTHORITY_OF_SURVEY);
        result.put("MY", CalculationMethodEnum.DEPARTMENT_OF_ISLAMIC_ADVANCEMENT_MALAYSIA);
        result.put("MV", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("ML", CalculationMethodEnum.EGYPTIAN_GENERAL_AUTHORITY_OF_SURVEY);
        result.put("MT", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("MH", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("MQ", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("MR", CalculationMethodEnum.EGYPTIAN_GENERAL_AUTHORITY_OF_SURVEY);
        result.put("MU", CalculationMethodEnum.EGYPTIAN_GENERAL_AUTHORITY_OF_SURVEY);
        result.put("YT", CalculationMethodEnum.EGYPTIAN_GENERAL_AUTHORITY_OF_SURVEY);
        result.put("MX", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("FM", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("MD", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("MC", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("MN", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("ME", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("MS", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("MA", CalculationMethodEnum.MOROCCAN_MINISTRY_OF_ISLAMIC_AFFAIRS);
        result.put("MZ", CalculationMethodEnum.EGYPTIAN_GENERAL_AUTHORITY_OF_SURVEY);
        result.put("NA", CalculationMethodEnum.EGYPTIAN_GENERAL_AUTHORITY_OF_SURVEY);
        result.put("NR", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("NP", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("NL", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("NC", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("NZ", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("NI", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("NE", CalculationMethodEnum.EGYPTIAN_GENERAL_AUTHORITY_OF_SURVEY);
        result.put("NG", CalculationMethodEnum.EGYPTIAN_GENERAL_AUTHORITY_OF_SURVEY);
        result.put("NU", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("NF", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("KP", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("MP", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("NO", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("OM", CalculationMethodEnum.UMM_AL_QURA_UNIVERSITY_MAKKAH);
        result.put("PK", CalculationMethodEnum.UNIVERSITY_OF_ISLAMIC_SCIENCES_KARACHI);
        result.put("PW", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("PS", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("PA", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("PG", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("PY", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("PE", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("PH", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("PN", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("PL", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("PT", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("PR", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("QA", CalculationMethodEnum.QATAR);
        result.put("ID", CalculationMethodEnum.KEMENTERIAN_AGAMA_RI_INDONESIA);
        result.put("MM", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("RE", CalculationMethodEnum.EGYPTIAN_GENERAL_AUTHORITY_OF_SURVEY);
        result.put("RO", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("RU", CalculationMethodEnum.SPIRITUAL_ADMINISTRATION_OF_MUSLIMS_OF_RUSSIA);
        result.put("RW", CalculationMethodEnum.EGYPTIAN_GENERAL_AUTHORITY_OF_SURVEY);
        result.put("BL", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("SH", CalculationMethodEnum.EGYPTIAN_GENERAL_AUTHORITY_OF_SURVEY);
        result.put("KN", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("LC", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("MF", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("PM", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("VC", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("WS", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("SM", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("ST", CalculationMethodEnum.EGYPTIAN_GENERAL_AUTHORITY_OF_SURVEY);
        result.put("SA", CalculationMethodEnum.UMM_AL_QURA_UNIVERSITY_MAKKAH);
        result.put("SN", CalculationMethodEnum.EGYPTIAN_GENERAL_AUTHORITY_OF_SURVEY);
        result.put("RS", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("SC", CalculationMethodEnum.EGYPTIAN_GENERAL_AUTHORITY_OF_SURVEY);
        result.put("SL", CalculationMethodEnum.EGYPTIAN_GENERAL_AUTHORITY_OF_SURVEY);
        result.put("SG", CalculationMethodEnum.MAJLIS_UGAMA_ISLAM_SINGAPURA);
        result.put("SX", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("SK", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("SI", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("SB", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("SO", CalculationMethodEnum.EGYPTIAN_GENERAL_AUTHORITY_OF_SURVEY);
        result.put("ZA", CalculationMethodEnum.EGYPTIAN_GENERAL_AUTHORITY_OF_SURVEY);
        result.put("GS", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("KR", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("SS", CalculationMethodEnum.EGYPTIAN_GENERAL_AUTHORITY_OF_SURVEY);
        result.put("ES", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("LK", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("SD", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("SR", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("SJ", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("SZ", CalculationMethodEnum.EGYPTIAN_GENERAL_AUTHORITY_OF_SURVEY);
        result.put("SE", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("CH", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("SY", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("TW", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("TJ", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("TZ", CalculationMethodEnum.EGYPTIAN_GENERAL_AUTHORITY_OF_SURVEY);
        result.put("TH", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("BS", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("GM", CalculationMethodEnum.EGYPTIAN_GENERAL_AUTHORITY_OF_SURVEY);
        result.put("TL", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("TG", CalculationMethodEnum.EGYPTIAN_GENERAL_AUTHORITY_OF_SURVEY);
        result.put("TK", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("TO", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("TT", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("TN", CalculationMethodEnum.TUNISIAN_MINISTRY_OF_RELIGIOUS_AFFAIRS);
        result.put("TR", CalculationMethodEnum.DIYANET_ISLERI_BASKANLIGI_TURKEY);
        result.put("TM", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("TC", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("TV", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("VI", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("UG", CalculationMethodEnum.EGYPTIAN_GENERAL_AUTHORITY_OF_SURVEY);
        result.put("UA", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("AE", CalculationMethodEnum.UMM_AL_QURA_UNIVERSITY_MAKKAH);
        result.put("GB", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("US", CalculationMethodEnum.ISLAMIC_SOCIETY_OF_NORTH_AMERICA);
        result.put("UM", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("UY", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("UZ", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("VU", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("VA", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("VE", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("VN", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("WF", CalculationMethodEnum.MUSLIM_WORLD_LEAGUE);
        result.put("YE", CalculationMethodEnum.UMM_AL_QURA_UNIVERSITY_MAKKAH);
        result.put("ZM", CalculationMethodEnum.EGYPTIAN_GENERAL_AUTHORITY_OF_SURVEY);
        result.put("ZW", CalculationMethodEnum.EGYPTIAN_GENERAL_AUTHORITY_OF_SURVEY);
        return Collections.unmodifiableMap(result);
    }

    public static CalculationMethodEnum getCalculationMethodByAddress(Address address) {
        String countryCode = address.getCountryCode();

        if (countryCode != null && "GB".equals(countryCode.toUpperCase())) {
            if ("London".equalsIgnoreCase(address.getLocality())) {
                return CalculationMethodEnum.LONDON_UNIFIED_PRAYER_TIMES;
            } else {
                return CalculationMethodEnum.getDefault();
            }
        } else if (countryCode != null && CALCULATION_METHOD_BY_COUNTRY.get(countryCode.toUpperCase()) != null) {
            return CALCULATION_METHOD_BY_COUNTRY.get(countryCode.toUpperCase());
        } else {
            return CalculationMethodEnum.getDefault();
        }
    }
}
