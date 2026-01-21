package com.ktb3.devths.vulnerable.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Document;

/**
 * INTENTIONALLY VULNERABLE FILE SERVICE FOR CODEQL TESTING
 * DO NOT USE IN PRODUCTION
 */
@Service
public class VulnerableFileService {

	private static final String UPLOAD_DIR = "/var/app/uploads/";

	// 취약점 #1: 파일 타입 검증 없음
	public String uploadFileNoValidation(MultipartFile file) throws IOException {
		String filename = file.getOriginalFilename();
		File dest = new File(UPLOAD_DIR + filename);
		file.transferTo(dest);
		return "File uploaded: " + filename;
	}

	// 취약점 #2: 확장자만 검증 (Content-Type은 검증 안 함)
	public String uploadFileWeakValidation(MultipartFile file) throws IOException {
		String filename = file.getOriginalFilename();

		// 취약점: 확장자만 체크 (파일명 변조 가능)
		if (filename != null && (filename.endsWith(".jpg") || filename.endsWith(".png"))) {
			File dest = new File(UPLOAD_DIR + filename);
			file.transferTo(dest);
			return "File uploaded: " + filename;
		}

		return "Invalid file type";
	}

	// 취약점 #3: 파일 크기 제한 없음 (DoS 가능)
	public String uploadFileNoSizeLimit(MultipartFile file) throws IOException {
		// 무제한 크기 파일 업로드 허용
		String filename = file.getOriginalFilename();
		File dest = new File(UPLOAD_DIR + filename);
		file.transferTo(dest);
		return "Large file uploaded: " + filename + " (" + file.getSize() + " bytes)";
	}

	// 취약점 #4: 원본 파일명 그대로 사용
	public String uploadFileWithOriginalName(MultipartFile file) throws IOException {
		// 취약점: ../../../etc/passwd 같은 경로 조작 가능
		String filename = file.getOriginalFilename();
		File dest = new File(UPLOAD_DIR + filename);

		// 디렉토리 생성도 안전하지 않음
		dest.getParentFile().mkdirs();
		file.transferTo(dest);

		return "File uploaded to: " + dest.getAbsolutePath();
	}

	// 취약점 #5: Path Traversal in file download
	public Resource downloadFileVulnerable(String filename) throws IOException {
		// 취약점: 경로 검증 없이 파일 읽기
		Path filePath = Paths.get(UPLOAD_DIR + filename);
		Resource resource = new UrlResource(filePath.toUri());

		if (resource.exists() || resource.isReadable()) {
			return resource;
		} else {
			throw new RuntimeException("Could not read file: " + filename);
		}
	}

	// 취약점 #6: 악성 스크립트 업로드 허용
	public String uploadScriptFile(MultipartFile file) throws IOException {
		String filename = file.getOriginalFilename();

		// 취약점: .jsp, .php, .sh 등 실행 가능한 파일 업로드 허용
		File dest = new File(UPLOAD_DIR + filename);
		file.transferTo(dest);

		// 더 위험: 업로드된 파일을 public 디렉토리에 저장
		return "Script uploaded and accessible at: /uploads/" + filename;
	}

	// 취약점 #7: Zip Slip
	public String extractZipVulnerable(MultipartFile file) throws IOException {
		ZipInputStream zis = new ZipInputStream(file.getInputStream());
		ZipEntry zipEntry;

		while ((zipEntry = zis.getNextEntry()) != null) {
			// 취약점: 경로 검증 없이 압축 해제
			File destFile = new File(UPLOAD_DIR + zipEntry.getName());

			// 디렉토리 생성
			if (zipEntry.isDirectory()) {
				destFile.mkdirs();
			} else {
				// 파일 쓰기
				destFile.getParentFile().mkdirs();
				FileOutputStream fos = new FileOutputStream(destFile);
				byte[] buffer = new byte[1024];
				int len;
				while ((len = zis.read(buffer)) > 0) {
					fos.write(buffer, 0, len);
				}
				fos.close();
			}
			zis.closeEntry();
		}
		zis.close();

		return "Zip file extracted";
	}

	// 취약점 #8: 이미지 메타데이터 검증 없음
	public String uploadImageNoMetadataCheck(MultipartFile file) throws IOException {
		// 취약점: EXIF 데이터에 악성 코드가 포함될 수 있음
		// 이미지 파일인지 검증하지 않음
		String filename = file.getOriginalFilename();
		File dest = new File(UPLOAD_DIR + filename);
		file.transferTo(dest);

		return "Image uploaded: " + filename;
	}

	// 취약점 #9: 사용자가 업로드 경로 지정
	public String uploadToCustomPath(MultipartFile file, String customPath) throws IOException {
		// 취약점: 사용자가 지정한 경로에 파일 저장
		File dest = new File(customPath + "/" + file.getOriginalFilename());
		dest.getParentFile().mkdirs();
		file.transferTo(dest);

		return "File uploaded to custom path: " + dest.getAbsolutePath();
	}

	// 취약점 #10: 권한 체크 없이 파일 삭제
	public String deleteFileNoAuthCheck(String filename) {
		// 취약점: 누구나 아무 파일이나 삭제 가능
		File file = new File(UPLOAD_DIR + filename);

		if (file.delete()) {
			return "File deleted: " + filename;
		} else {
			return "Failed to delete: " + filename;
		}
	}

	// 취약점 #11: 심볼릭 링크 공격
	public String readFileNoSymlinkCheck(String path) throws IOException {
		// 취약점: 심볼릭 링크 검증 없이 파일 읽기
		Path filePath = Paths.get(UPLOAD_DIR + path);
		return new String(Files.readAllBytes(filePath));
	}

	// 취약점 #12: XXE in XML file processing
	public String processXmlFileVulnerable(MultipartFile file) throws Exception {
		// 취약점: XXE 공격에 취약
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document doc = builder.parse(file.getInputStream());

		return "XML processed: " + doc.getDocumentElement().getTextContent();
	}

	// 취약점 #13: 파일명 sanitization 없음
	public String uploadFileNoSanitization(MultipartFile file) throws IOException {
		String filename = file.getOriginalFilename();

		// 취약점: null byte, .., /, \ 등 특수문자 필터링 없음
		// 예: "file.txt\0.jpg" -> null byte injection
		// 예: "../../etc/passwd" -> path traversal

		File dest = new File(UPLOAD_DIR + filename);
		file.transferTo(dest);

		return "File uploaded: " + filename;
	}

	// 취약점 #14: Race condition
	public String uploadFileWithRaceCondition(MultipartFile file) throws IOException {
		String filename = file.getOriginalFilename();
		File dest = new File(UPLOAD_DIR + filename);

		// 취약점: TOCTOU (Time-of-check to time-of-use) race condition
		if (!dest.exists()) {
			// 여기서 다른 스레드가 같은 파일을 생성할 수 있음
			Thread.yield(); // 의도적으로 race condition 유발
			file.transferTo(dest);
		}

		return "File uploaded: " + filename;
	}

	// 취약점 #15: 임시 파일 안전하게 삭제하지 않음
	public String processTempFileInsecurely(MultipartFile file) throws IOException {
		// 취약점: 임시 파일을 예측 가능한 이름으로 생성
		File tempFile = new File("/tmp/upload_" + System.currentTimeMillis());
		file.transferTo(tempFile);

		// 처리 로직...
		String content = new String(Files.readAllBytes(tempFile.toPath()));

		// 취약점: 임시 파일을 삭제하지 않음 (정보 유출 가능)
		// tempFile.delete(); // 주석 처리됨

		return "Processed file content length: " + content.length();
	}

	// 추가 취약점 #16: Double extension bypass
	public String uploadDoubleExtension(MultipartFile file) throws IOException {
		String filename = file.getOriginalFilename();

		// 취약점: .php.jpg 같은 이중 확장자 우회 가능
		if (filename != null && filename.contains(".jpg")) {
			File dest = new File(UPLOAD_DIR + filename);
			file.transferTo(dest);
			return "Image uploaded: " + filename;
		}

		return "Not an image";
	}

	// 추가 취약점 #17: MIME type validation bypass
	public String uploadMimeTypeBypass(MultipartFile file) throws IOException {
		// 취약점: Content-Type 헤더만 체크 (쉽게 위조 가능)
		String contentType = file.getContentType();

		if ("image/jpeg".equals(contentType) || "image/png".equals(contentType)) {
			String filename = file.getOriginalFilename();
			File dest = new File(UPLOAD_DIR + filename);
			file.transferTo(dest);
			return "Image uploaded: " + filename;
		}

		return "Invalid MIME type";
	}
}
