package com.ktb3.devths.board.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ktb3.devths.board.domain.entity.Post;
import com.ktb3.devths.board.domain.entity.PostTag;
import com.ktb3.devths.board.dto.response.PostDetailResponse;
import com.ktb3.devths.board.dto.response.PostSummaryResponse;
import com.ktb3.devths.board.repository.LikeRepository;
import com.ktb3.devths.board.repository.PostRepository;
import com.ktb3.devths.board.repository.PostTagRepository;
import com.ktb3.devths.global.exception.CustomException;
import com.ktb3.devths.global.response.ErrorCode;
import com.ktb3.devths.global.storage.domain.constant.RefType;
import com.ktb3.devths.global.storage.domain.entity.S3Attachment;
import com.ktb3.devths.global.storage.repository.S3AttachmentRepository;
import com.ktb3.devths.global.storage.service.S3StorageService;
import com.ktb3.devths.user.domain.constant.Interests;
import com.ktb3.devths.user.domain.entity.User;
import com.ktb3.devths.user.repository.UserInterestRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostDetailQueryService {

	private final PostRepository postRepository;
	private final PostTagRepository postTagRepository;
	private final LikeRepository likeRepository;
	private final UserInterestRepository userInterestRepository;
	private final S3AttachmentRepository s3AttachmentRepository;
	private final S3StorageService s3StorageService;
	private final PostDetailCacheService postDetailCacheService;

	@Transactional(readOnly = true)
	public PostDetailResponse loadAndCache(Long userId, Long postId) {
		Post post = postRepository.findByIdAndIsDeletedFalseWithUser(postId)
			.orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

		User author = post.getUser();
		Long authorId = author.getId();

		List<S3Attachment> attachments = s3AttachmentRepository
			.findByRefTypeAndRefIdAndIsDeletedFalseOrderBySortOrderAsc(RefType.POST, postId);

		List<PostTag> postTags = postTagRepository.findByPostIdIn(List.of(postId));

		S3Attachment profileImage = s3AttachmentRepository
			.findTopByRefTypeAndRefIdAndIsDeletedFalseOrderByCreatedAtDesc(RefType.USER, authorId)
			.orElse(null);

		String profileImageUrl = (profileImage != null)
			? s3StorageService.getPublicUrl(profileImage.getS3Key())
			: null;

		List<String> interests = userInterestRepository.findInterestsByUserId(authorId).stream()
			.map(Interests::getDisplayName)
			.toList();

		PostSummaryResponse.PostAuthorInfo authorInfo = new PostSummaryResponse.PostAuthorInfo(
			authorId, author.getNickname(), profileImageUrl, interests
		);

		boolean isLiked = likeRepository.existsByPostIdAndUserId(postId, userId);

		PostDetailResponse response = PostDetailResponse.of(post, attachments, postTags, authorInfo, isLiked,
			s3StorageService);
		postDetailCacheService.put(postId, userId, response);

		return response;
	}
}
