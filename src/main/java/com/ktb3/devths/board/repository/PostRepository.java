package com.ktb3.devths.board.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ktb3.devths.board.domain.entity.Post;

public interface PostRepository extends JpaRepository<Post, Long> {

	@Query("SELECT p FROM Post p "
		+ "JOIN FETCH p.user "
		+ "WHERE p.isDeleted = false "
		+ "AND p.user.isWithdraw = false "
		+ "ORDER BY p.id DESC")
	List<Post> findPostsNotDeleted(Pageable pageable);

	@Query("SELECT p FROM Post p "
		+ "JOIN FETCH p.user "
		+ "WHERE p.isDeleted = false "
		+ "AND p.user.isWithdraw = false "
		+ "AND p.id < :lastId "
		+ "ORDER BY p.id DESC")
	List<Post> findPostsNotDeletedAfterCursor(
		@Param("lastId") Long lastId,
		Pageable pageable
	);

	@Query("SELECT p FROM Post p "
		+ "JOIN FETCH p.user "
		+ "WHERE p.isDeleted = false "
		+ "AND p.user.isWithdraw = false "
		+ "AND (p.title LIKE CONCAT('%', :keyword, '%') OR p.content LIKE CONCAT('%', :keyword, '%')) "
		+ "ORDER BY p.id DESC")
	List<Post> findPostsByKeywordNotDeleted(
		@Param("keyword") String keyword,
		Pageable pageable
	);

	@Query("SELECT p FROM Post p "
		+ "JOIN FETCH p.user "
		+ "WHERE p.isDeleted = false "
		+ "AND p.user.isWithdraw = false "
		+ "AND (p.title LIKE CONCAT('%', :keyword, '%') OR p.content LIKE CONCAT('%', :keyword, '%')) "
		+ "AND p.id < :lastId "
		+ "ORDER BY p.id DESC")
	List<Post> findPostsByKeywordNotDeletedAfterCursor(
		@Param("keyword") String keyword,
		@Param("lastId") Long lastId,
		Pageable pageable
	);
}
