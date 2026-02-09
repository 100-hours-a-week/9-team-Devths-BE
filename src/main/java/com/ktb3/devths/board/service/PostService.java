package com.ktb3.devths.board.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ktb3.devths.board.domain.entity.Post;
import com.ktb3.devths.board.domain.entity.PostTag;
import com.ktb3.devths.board.dto.response.PostListResponse;
import com.ktb3.devths.board.repository.PostRepository;
import com.ktb3.devths.board.repository.PostTagRepository;
import com.ktb3.devths.global.storage.domain.constant.RefType;
import com.ktb3.devths.global.storage.domain.entity.S3Attachment;
import com.ktb3.devths.global.storage.repository.S3AttachmentRepository;
import com.ktb3.devths.global.storage.service.S3StorageService;
import com.ktb3.devths.global.util.LogSanitizer;
import com.ktb3.devths.user.domain.entity.UserInterest;
import com.ktb3.devths.user.repository.UserInterestRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostService {

	private static final int DEFAULT_PAGE_SIZE = 20;
	private static final int MAX_PAGE_SIZE = 100;

	private final PostRepository postRepository;
	private final PostTagRepository postTagRepository;
	private final UserInterestRepository userInterestRepository;
	private final S3AttachmentRepository s3AttachmentRepository;
	private final S3StorageService s3StorageService;

	@Transactional(readOnly = true)
	public PostListResponse getPostList(Integer size, Long lastId, String tag) {
		int pageSize = (size == null || size <= 0) ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
		Pageable pageable = PageRequest.of(0, pageSize + 1);

		String keyword = (tag != null) ? tag.strip() : null;
		boolean hasKeyword = keyword != null && !keyword.isEmpty();

		if (hasKeyword) {
			log.info("게시글 검색 키워드: {}", LogSanitizer.sanitize(keyword));
		}

		List<Post> posts = fetchPosts(keyword, hasKeyword, lastId, pageable);

		if (posts.isEmpty()) {
			return PostListResponse.of(posts, pageSize, Map.of(), Map.of(), Map.of(), s3StorageService);
		}

		boolean hasNext = posts.size() > pageSize;
		List<Post> actualPosts = hasNext ? posts.subList(0, pageSize) : posts;

		List<Long> postIds = actualPosts.stream()
			.map(Post::getId)
			.toList();

		List<Long> userIds = actualPosts.stream()
			.map(post -> post.getUser().getId())
			.distinct()
			.toList();

		Map<Long, List<PostTag>> tagMap = postTagRepository.findByPostIdIn(postIds).stream()
			.collect(Collectors.groupingBy(pt -> pt.getPost().getId()));

		Map<Long, S3Attachment> profileImageMap = buildProfileImageMap(userIds);

		Map<Long, List<UserInterest>> interestMap = userInterestRepository.findByUserIdIn(userIds).stream()
			.collect(Collectors.groupingBy(ui -> ui.getUser().getId()));

		return PostListResponse.of(posts, pageSize, tagMap, profileImageMap, interestMap, s3StorageService);
	}

	private List<Post> fetchPosts(String keyword, boolean hasKeyword, Long lastId, Pageable pageable) {
		if (hasKeyword && lastId != null) {
			return postRepository.findPostsByKeywordNotDeletedAfterCursor(keyword, lastId, pageable);
		}
		if (hasKeyword) {
			return postRepository.findPostsByKeywordNotDeleted(keyword, pageable);
		}
		if (lastId != null) {
			return postRepository.findPostsNotDeletedAfterCursor(lastId, pageable);
		}
		return postRepository.findPostsNotDeleted(pageable);
	}

	private Map<Long, S3Attachment> buildProfileImageMap(List<Long> userIds) {
		if (userIds.isEmpty()) {
			return Collections.emptyMap();
		}

		return s3AttachmentRepository
			.findByRefTypeAndRefIdInAndIsDeletedFalse(RefType.USER, userIds)
			.stream()
			.collect(Collectors.toMap(
				S3Attachment::getRefId,
				attachment -> attachment,
				(first, second) -> first
			));
	}
}
