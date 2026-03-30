package AssessX_backend.repository;

import AssessX_backend.model.Result;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResultRepository extends JpaRepository<Result, Long> {

    List<Result> findByUserId(Long userId);

    List<Result> findByTestId(Long testId);

    List<Result> findByPracticeId(Long practiceId);

    @Query("""
        SELECT r FROM Result r
        JOIN r.user u
        JOIN u.groups g
        WHERE g.id = :groupId
        ORDER BY r.submittedAt DESC
    """)
    List<Result> findByGroupId(@Param("groupId") Long groupId);
}
