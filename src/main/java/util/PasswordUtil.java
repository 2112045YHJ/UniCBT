package main.java.util; // util 패키지에 위치

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
// 실제 해싱을 위해서는 BCrypt 같은 라이브러리 임포트 필요
// import org.mindrot.jbcrypt.BCrypt;

/**
 * 비밀번호 관련 유틸리티 메서드를 제공하는 클래스입니다.
 */
public class PasswordUtil {

    private static final DateTimeFormatter BIRTHDATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * 생년월일을 기반으로 초기 비밀번호 문자열 ('a' + 생년월일 8자리)을 생성합니다.
     * @param birthDate 사용자의 생년월일
     * @return 생성된 초기 비밀번호 문자열, 생년월일이 없으면 기본값 반환
     */
    public static String generateInitialPassword(LocalDate birthDate) {
        if (birthDate == null) {
            // 생년월일 정보가 없는 경우에 대한 기본 비밀번호 정책
            // (예: "tempPassword123!" 또는 예외 발생)
            // 여기서는 임시로 학번 등을 활용하지 않고 고정된 값을 반환하나, 실제로는 더 나은 정책 필요
            System.err.println("경고: 생년월일 정보가 없어 기본 임시 비밀번호를 사용합니다. (보안 취약)");
            return "a00000000"; // 또는 다른 안전한 임시 비밀번호 생성 로직
        }
        return "a" + birthDate.format(BIRTHDATE_FORMATTER);
    }

    /**
     * 평문 비밀번호를 해싱합니다. (실제 운영 환경에서는 강력한 해싱 알고리즘 사용 필수)
     * @param plainPassword 해싱할 평문 비밀번호
     * @return 해싱된 비밀번호 문자열
     */
    public static String hashPassword(String plainPassword) {
        // TODO: jBCrypt, SCrypt, Argon2 등 안전한 해싱 라이브러리 연동
        // 예시 (jBCrypt): return BCrypt.hashpw(plainPassword, BCrypt.gensalt(12));
        System.out.println("주의: 개발용 임시 비밀번호 처리 - 실제 해싱이 적용되지 않았습니다.");
        return plainPassword; // !!중요!! 개발 초기 단계에서만 임시 사용, 실제로는 반드시 해싱해야 합니다.
    }

    /**
     * 입력된 평문 비밀번호와 저장된 해시된 비밀번호를 비교합니다.
     * @param plainPassword 사용자가 입력한 평문 비밀번호
     * @param hashedPassword 데이터베이스에 저장된 해시된 비밀번호
     * @return 일치하면 true, 아니면 false
     */
    public static boolean checkPassword(String plainPassword, String hashedPassword) {
        // TODO: 실제 운영 환경에서는 해싱 라이브러리의 검증 메서드 사용
        // 예시 (jBCrypt): return BCrypt.checkpw(plainPassword, hashedPassword);
        System.out.println("주의: 개발용 임시 비밀번호 처리 - 실제 해싱 검증이 적용되지 않았습니다.");
        return plainPassword.equals(hashedPassword); // !!중요!! 개발 초기 단계에서만 임시 사용
    }
}