package com.ktb3.devths.vulnerable.controller;

import java.io.IOException;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ktb3.devths.vulnerable.service.VulnerableFileService;

import lombok.RequiredArgsConstructor;

/**
 * INTENTIONALLY VULNERABLE FILE UPLOAD/DOWNLOAD CODE FOR CODEQL TESTING
 * DO NOT USE IN PRODUCTION
 */
@RestController
@RequestMapping("/api/vulnerable/files")
@RequiredArgsConstructor
public class VulnerableFileController {

	private final VulnerableFileService fileService;

	// 취약점 #1: 파일 타입 검증 없음
	@PostMapping("/upload-no-validation")
	public String uploadFileNoValidation(@RequestParam("file") MultipartFile file) throws IOException {
		return fileService.uploadFileNoValidation(file);
	}

	// 취약점 #2: 확장자만 검증 (쉽게 우회 가능)
	@PostMapping("/upload-weak-validation")
	public String uploadFileWeakValidation(@RequestParam("file") MultipartFile file) throws IOException {
		return fileService.uploadFileWeakValidation(file);
	}

	// 취약점 #3: 파일 크기 제한 없음
	@PostMapping("/upload-no-size-limit")
	public String uploadFileNoSizeLimit(@RequestParam("file") MultipartFile file) throws IOException {
		return fileService.uploadFileNoSizeLimit(file);
	}

	// 취약점 #4: 원본 파일명 그대로 사용 (Path Traversal)
	@PostMapping("/upload-original-filename")
	public String uploadFileOriginalName(@RequestParam("file") MultipartFile file) throws IOException {
		return fileService.uploadFileWithOriginalName(file);
	}

	// 취약점 #5: 파일 다운로드 시 Path Traversal
	@GetMapping("/download")
	public ResponseEntity<Resource> downloadFile(@RequestParam String filename) throws IOException {
		Resource resource = fileService.downloadFileVulnerable(filename);

		return ResponseEntity.ok()
			// 취약점: Content-Disposition 헤더에 사용자 입력 직접 사용
			.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
			.contentType(MediaType.APPLICATION_OCTET_STREAM)
			.body(resource);
	}

	// 취약점 #6: 파일 내용 검증 없음 (악성 스크립트 업로드 가능)
	@PostMapping("/upload-script")
	public String uploadScript(@RequestParam("file") MultipartFile file) throws IOException {
		return fileService.uploadScriptFile(file);
	}

	// 취약점 #7: Zip Slip (압축 해제 시 경로 검증 없음)
	@PostMapping("/upload-and-extract")
	public String uploadAndExtractZip(@RequestParam("file") MultipartFile file) throws IOException {
		return fileService.extractZipVulnerable(file);
	}

	// 취약점 #8: 이미지 메타데이터 검증 없음
	@PostMapping("/upload-image")
	public String uploadImage(@RequestParam("file") MultipartFile file) throws IOException {
		return fileService.uploadImageNoMetadataCheck(file);
	}

	// 취약점 #9: 파일 업로드 경로를 사용자가 지정
	@PostMapping("/upload-custom-path")
	public String uploadToCustomPath(
		@RequestParam("file") MultipartFile file,
		@RequestParam("path") String path) throws IOException {
		return fileService.uploadToCustomPath(file, path);
	}

	// 취약점 #10: 파일 삭제 시 권한 체크 없음
	@DeleteMapping("/delete")
	public String deleteFile(@RequestParam String filename) {
		return fileService.deleteFileNoAuthCheck(filename);
	}

	// 취약점 #11: 심볼릭 링크 공격
	@GetMapping("/read")
	public String readFile(@RequestParam String path) throws IOException {
		return fileService.readFileNoSymlinkCheck(path);
	}

	// 취약점 #12: XXE를 통한 파일 업로드
	@PostMapping("/upload-xml")
	public String uploadXml(@RequestParam("file") MultipartFile file) throws Exception {
		return fileService.processXmlFileVulnerable(file);
	}

	// 취약점 #13: 파일 이름에 특수문자 허용
	@PostMapping("/upload-special-chars")
	public String uploadWithSpecialChars(@RequestParam("file") MultipartFile file) throws IOException {
		String filename = file.getOriginalFilename();
		// 취약점: 특수문자 필터링 없음 (null byte, .., /, \ 등)
		return fileService.uploadFileNoSanitization(file);
	}

	// 취약점 #14: Race condition in file operations
	@PostMapping("/upload-race-condition")
	public String uploadWithRaceCondition(@RequestParam("file") MultipartFile file) throws IOException {
		return fileService.uploadFileWithRaceCondition(file);
	}

	// 취약점 #15: 임시 파일 안전하게 삭제하지 않음
	@PostMapping("/process-temp-file")
	public String processTempFile(@RequestParam("file") MultipartFile file) throws IOException {
		return fileService.processTempFileInsecurely(file);
	}
}
