package main.java.util;

import main.java.dto.UserDto; // UserDto 클래스가 main.java.dto 패키지에 있다고 가정
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * 엑셀 파일 파싱 관련 유틸리티 클래스입니다.
 */
public class ExcelParserUtil {

    private static final DateTimeFormatter DATE_FORMATTER_YYYY_MM_DD = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 엑셀 파일에서 학생 정보를 읽어 UserDto 리스트로 반환합니다.
     * 첫 번째 행은 헤더로 간주하고 건너뜁니다.
     * <p>
     * 예상 엑셀 컬럼 순서:
     * A열 (0): 학번 (필수)
     * B열 (1): 이름 (필수)
     * C열 (2): 생년월일 (날짜 형식 또는 "yyyy-MM-dd", 필수)
     * D열 (3): 학과명 (문자열)
     * E열 (4): 학년 (숫자, 예: 1, 2, 3)
     * F열 (5): 상태 (문자열, 예: "재학", "휴학", 기본값: "재학")
     * </p>
     * @param excelFile 파싱할 엑셀 파일
     * @return UserDto 리스트
     * @throws IOException 파일 읽기 오류 발생 시
     * @throws IllegalArgumentException 지원하지 않는 파일 형식이거나 필수 데이터 누락 등 내용 오류 시
     */
    public static List<UserDto> parseStudentExcel(File excelFile) throws IOException, IllegalArgumentException {
        List<UserDto> studentList = new ArrayList<>();
        FileInputStream fis = null;
        Workbook workbook = null;

        try {
            fis = new FileInputStream(excelFile);
            String fileName = excelFile.getName().toLowerCase();

            if (fileName.endsWith(".xlsx")) {
                workbook = new XSSFWorkbook(fis);
            } else if (fileName.endsWith(".xls")) {
                workbook = new HSSFWorkbook(fis);
            } else {
                throw new IllegalArgumentException("지원하지 않는 엑셀 파일 형식입니다. (.xlsx 또는 .xls 확장자 사용)");
            }

            Sheet sheet = workbook.getSheetAt(0); // 첫 번째 시트를 사용
            Iterator<Row> rowIterator = sheet.iterator();

            // 헤더 행 건너뛰기
            if (rowIterator.hasNext()) {
                rowIterator.next();
            }

            int rowNumForLog = 1; // 로그용 행 번호 (데이터 행 기준)
            while (rowIterator.hasNext()) {
                rowNumForLog++;
                Row row = rowIterator.next();
                UserDto userDto = new UserDto();

                try {
                    // 학번 (A열, 인덱스 0) - 필수
                    Cell studentNumberCell = row.getCell(0, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    String studentNumber = getCellValueAsString(studentNumberCell);
                    if (studentNumber == null || studentNumber.trim().isEmpty()) {
                        System.err.println("경고: 엑셀 " + rowNumForLog + "행 - 학번 누락. 해당 행을 건너뜁니다.");
                        continue;
                    }
                    userDto.setStudentNumber(studentNumber.trim());

                    // 이름 (B열, 인덱스 1) - 필수
                    Cell nameCell = row.getCell(1, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    String name = getCellValueAsString(nameCell);
                    if (name == null || name.trim().isEmpty()) {
                        System.err.println("경고: 엑셀 " + rowNumForLog + "행 (학번: " + studentNumber + ") - 이름 누락. 해당 행을 건너뜁니다.");
                        continue;
                    }
                    userDto.setName(name.trim());

                    // 생년월일 (C열, 인덱스 2) - 필수 (비밀번호 생성용)
                    Cell birthDateCell = row.getCell(2, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    LocalDate birthDate = getCellDateValue(birthDateCell);
                    if (birthDate == null) {
                        System.err.println("경고: 엑셀 " + rowNumForLog + "행 (학번: " + studentNumber + ") - 생년월일 누락 또는 형식 오류. 해당 행을 건너뜁니다.");
                        continue;
                    }
                    userDto.setBirthDate(birthDate);

                    // 학과명 (D열, 인덱스 3)
                    Cell departmentNameCell = row.getCell(3, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    userDto.setDepartmentName(getCellValueAsString(departmentNameCell).trim());

                    // 학년 (E열, 인덱스 4)
                    Cell gradeCell = row.getCell(4, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    String gradeStr = getCellValueAsString(gradeCell).replaceAll("[^0-9]", "");
                    if (!gradeStr.isEmpty()) {
                        try {
                            userDto.setGrade(Integer.parseInt(gradeStr));
                        } catch (NumberFormatException e) {
                            System.err.println("경고: 엑셀 " + rowNumForLog + "행 (학번: " + studentNumber + ") - 학년 형식 오류: " + getCellValueAsString(gradeCell) + ". 기본값(0)으로 설정됩니다.");
                            userDto.setGrade(0); // 또는 다른 오류 처리
                        }
                    } else {
                        userDto.setGrade(0); // 빈 값이면 기본값(0)
                    }

                    // 상태 (F열, 인덱스 5)
                    Cell statusCell = row.getCell(5, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    String status = getCellValueAsString(statusCell).trim();
                    if (!status.isEmpty()) {
                        // TODO: 입력된 status 값이 유효한 값인지 검증하는 로직 추가 (예: "재학", "휴학" 등 정의된 값만 허용)
                        userDto.setStatus(status);
                    } else {
                        userDto.setStatus("재학"); // 엑셀에 상태값이 없으면 기본 "재학"으로
                    }

                    // UserDto에 level 기본값 설정 (학생이므로 1)
                    userDto.setLevel(1);

                    studentList.add(userDto);

                } catch (Exception cellProcessingException) {
                    System.err.println("엑셀 파일 " + rowNumForLog + "행 (학번: " + userDto.getStudentNumber() + ") 처리 중 예기치 않은 오류 발생: " + cellProcessingException.getMessage());
                    // 정책에 따라 해당 행을 건너뛰거나, 전체 파싱 작업을 중단할 수 있음
                }
            } // while
        } finally {
            if (workbook != null) workbook.close(); // try-with-resources로 변경하면 자동 close
            if (fis != null) fis.close();          // try-with-resources로 변경하면 자동 close
        }
        return studentList;
    }

    /**
     * 셀 타입에 관계없이 셀 값을 문자열로 가져옵니다. 빈 셀은 빈 문자열을 반환합니다.
     */
    private static String getCellValueAsString(Cell cell) {
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return "";
        }
        // DataFormatter는 셀의 서식을 최대한 존중하여 문자열로 변환합니다.
        // 예를 들어, 숫자가 특정 형식으로 표시되어 있다면 그 형식대로 가져옵니다.
        // 순수 데이터 값이 필요하다면 cell.getNumericCellValue() 등을 직접 사용하고 형식을 제어해야 합니다.
        DataFormatter formatter = new DataFormatter();
        return formatter.formatCellValue(cell).trim();
    }

    /**
     * 셀에서 날짜 값을 LocalDate로 가져옵니다.
     * 숫자형(엑셀 날짜 표준) 또는 "yyyy-MM-dd" 형식의 문자열을 파싱하려고 시도합니다.
     * @param cell 읽어올 셀
     * @return LocalDate 객체, 파싱 실패 시 null 반환
     */
    private static LocalDate getCellDateValue(Cell cell) {
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return null;
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            if (DateUtil.isCellDateFormatted(cell)) {
                Date dateVal = cell.getDateCellValue();
                return dateVal.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            } else {
                // 일반 숫자인 경우, 날짜로 간주하지 않음.
                // (만약 특정 숫자 형식이 날짜를 의미한다면 추가 파싱 로직 필요)
                return null;
            }
        } else if (cell.getCellType() == CellType.STRING) {
            String dateStr = cell.getStringCellValue().trim();
            try {
                // "yyyy-MM-dd" 형식 시도
                return LocalDate.parse(dateStr, DATE_FORMATTER_YYYY_MM_DD);
            } catch (DateTimeParseException e1) {
                try {
                    // "yyyy.MM.dd" 형식 시도
                    return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy.MM.dd"));
                } catch (DateTimeParseException e2) {
                    try {
                        // "yyyy/MM/dd" 형식 시도
                        return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy/MM/dd"));
                    } catch (DateTimeParseException e3) {
                        System.err.println("지원하지 않는 날짜 문자열 형식입니다: " + dateStr);
                        return null;
                    }
                }
            }
        }
        return null; // 그 외 타입은 날짜로 처리하지 않음
    }
}