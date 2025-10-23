// com.dodam.board.repository.BoardRepository.java
package com.dodam.community.repository;

import com.dodam.board.entity.BoardEntity;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface CommunityRepository extends JpaRepository<BoardEntity, Long> {
	@Query("""
		    select b from BoardEntity b
		      left join b.boardCategory c
		      left join b.boardState s
		    where (:bcanum is null or c.bcanum = :bcanum)
		      and (:bsnum  is null or s.bsnum  = :bsnum)
		      and (
		            :q is null
		         or lower(b.bsub) like lower(concat('%', :q, '%'))
		         or lower(b.bcontent) like lower(concat('%', :q, '%'))
		      )
		    order by b.bnum desc
		  """)
		  Page<BoardEntity> search(@Param("bcanum") Long bcanum,
		                           @Param("bsnum")  Long bsnum,
		                           @Param("q")      String q,
		                           Pageable pageable); // Pageable에는 @Param 불필요
}
