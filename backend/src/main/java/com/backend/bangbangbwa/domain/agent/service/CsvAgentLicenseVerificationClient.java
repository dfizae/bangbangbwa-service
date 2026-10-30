package com.backend.bangbangbwa.domain.agent.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 서울 열린데이터광장 landBizInfo(부동산 중개업 정보) CSV를 클래스패스(리소스)에서 읽어
 * 메모리에 적재해두고, 중개업등록번호(REST_BRKR_INFO)로 조회하는 구현체입니다.
 *
 * - CSV는 src/main/resources 하위에 두며(agent.license.csv-location), 조회는 메모리 Map에서
 *   이루어지므로 빠릅니다.
 * - 데이터 갱신은 파일 교체 후 재빌드/재배포로 반영됩니다.
 * - 실시간 공공 API나 S3 적재로 전환하더라도 이 클래스만 교체하면 되고 서비스는 그대로입니다.
 */
@Component
public class CsvAgentLicenseVerificationClient
        implements AgentLicenseVerificationClient {

    private static final Logger log =
            LoggerFactory.getLogger(CsvAgentLicenseVerificationClient.class);

    // CSV 헤더명 (서울시 부동산 중개업소 정보 CSV의 한글 컬럼명)
    private static final String COL_LICENSE_NUMBER = "중개업등록번호"; // 조회 키
    private static final String COL_BROKER_NAME = "중개업자명";
    private static final String COL_OFFICE_NAME = "사업자상호";
    private static final String COL_OFFICE_ADDRESS = "주소";
    private static final String COL_OFFICE_PHONE = "전화번호";
    private static final String COL_BUSINESS_STATUS = "상태구분";

    private static final String STATUS_MATCHED = "MATCHED";
    private static final String STATUS_NOT_FOUND = "NOT_FOUND";

    private final Resource csvResource;
    private final Charset csvCharset;

    /*
     * 조회는 이 맵에서만 이루어집니다.
     * 수동 재적재(reload) 시 새 맵으로 통째로 교체하기 위해 volatile로 둡니다.
     */
    private volatile Map<String, BrokerRecord> brokerIndex = Map.of();

    public CsvAgentLicenseVerificationClient(
            @Value("${agent.license.csv-location:classpath:agent-license/agent.csv}")
            Resource csvResource,
            @Value("${agent.license.csv-charset:UTF-8}") String csvCharset
    ) {
        this.csvResource = csvResource;
        this.csvCharset = Charset.forName(csvCharset);
    }

    /*
     * 부팅 시 1회 적재합니다.
     * 적재 실패(파일 없음/형식 오류)가 앱 기동을 막지 않도록 예외를 삼키고 빈 맵으로 시작합니다.
     */
    @PostConstruct
    public void loadOnStartup() {
        try {
            reload();
        } catch (RuntimeException exception) {
            log.error(
                    "중개업 등록정보 CSV 초기 적재에 실패했습니다. 빈 상태로 기동합니다. location={}",
                    csvResource,
                    exception
            );
        }
    }

    /**
     * CSV를 다시 읽어 메모리 맵으로 교체합니다.
     * 관리자용 수동 갱신 훅이나 테스트에서 사용할 수 있습니다.
     */
    public void reload() {
        Map<String, BrokerRecord> loaded = new HashMap<>();

        try (InputStream inputStream = csvResource.getInputStream();
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(inputStream, csvCharset))) {

            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new IllegalStateException(
                        "CSV가 비어 있습니다. location=" + csvResource);
            }

            Map<String, Integer> columnIndex =
                    indexColumns(parseLine(stripBom(headerLine)));

            int licenseCol = requireColumn(columnIndex, COL_LICENSE_NUMBER);
            int brokerCol = requireColumn(columnIndex, COL_BROKER_NAME);
            int officeNameCol = requireColumn(columnIndex, COL_OFFICE_NAME);
            int addressCol = requireColumn(columnIndex, COL_OFFICE_ADDRESS);
            int phoneCol = requireColumn(columnIndex, COL_OFFICE_PHONE);
            int statusCol = requireColumn(columnIndex, COL_BUSINESS_STATUS);

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }

                List<String> fields = parseLine(line);

                String rawLicense = valueAt(fields, licenseCol);
                String normalized = normalize(rawLicense);
                if (normalized.isEmpty()) {
                    continue;
                }

                loaded.putIfAbsent(
                        normalized,
                        new BrokerRecord(
                                valueAt(fields, brokerCol),
                                valueAt(fields, officeNameCol),
                                valueAt(fields, addressCol),
                                valueAt(fields, phoneCol),
                                valueAt(fields, statusCol)
                        )
                );
            }
        } catch (RuntimeException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "중개업 등록정보 CSV 적재 중 오류가 발생했습니다. location=" + csvResource,
                    exception
            );
        }

        this.brokerIndex = loaded;
        log.info("중개업 등록정보 CSV 적재 완료. {}건. location={}", loaded.size(), csvResource);
    }

    @Override
    public BrokerLicenseLookupResult lookup(String licenseNumber) {
        BrokerRecord record = brokerIndex.get(normalize(licenseNumber));
        LocalDateTime checkedAt = LocalDateTime.now();

        if (record == null) {
            return new BrokerLicenseLookupResult(
                    false,
                    STATUS_NOT_FOUND,
                    null,
                    null,
                    null,
                    null,
                    null,
                    checkedAt
            );
        }

        return new BrokerLicenseLookupResult(
                true,
                STATUS_MATCHED,
                record.brokerName(),
                record.officeName(),
                record.officeAddress(),
                record.officePhone(),
                record.businessStatus(),
                checkedAt
        );
    }

    private Map<String, Integer> indexColumns(List<String> header) {
        Map<String, Integer> columnIndex = new HashMap<>();
        for (int i = 0; i < header.size(); i++) {
            columnIndex.put(header.get(i).trim(), i);
        }
        return columnIndex;
    }

    private int requireColumn(Map<String, Integer> columnIndex, String name) {
        Integer index = columnIndex.get(name);
        if (index == null) {
            throw new IllegalStateException(
                    "CSV에 필수 컬럼이 없습니다: " + name
            );
        }
        return index;
    }

    private String valueAt(List<String> fields, int index) {
        if (index < 0 || index >= fields.size()) {
            return null;
        }
        String value = fields.get(index).trim();
        return value.isEmpty() ? null : value;
    }

    /*
     * 중개업등록번호 매칭용 정규화.
     * 사용자 입력과 CSV 값 모두 공백/하이픈을 제거해 동일 기준으로 비교합니다.
     */
    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("[\\s-]", "");
    }

    private String stripBom(String line) {
        if (!line.isEmpty() && line.charAt(0) == 0xFEFF) {
            return line.substring(1);
        }
        return line;
    }

    /*
     * RFC 4180 기반 최소 CSV 파서.
     * 따옴표로 감싼 필드 내부의 콤마와 두 번 연속 따옴표("")를 처리합니다.
     * (필드 내부 줄바꿈은 처리하지 않습니다 - 해당 데이터가 있으면 CSV 라이브러리 도입 필요)
     */
    private List<String> parseLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        current.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    current.append(c);
                }
            } else {
                if (c == '"') {
                    inQuotes = true;
                } else if (c == ',') {
                    fields.add(current.toString());
                    current.setLength(0);
                } else {
                    current.append(c);
                }
            }
        }

        fields.add(current.toString());
        return fields;
    }

    private record BrokerRecord(
            String brokerName,
            String officeName,
            String officeAddress,
            String officePhone,
            String businessStatus
    ) {
    }
}
