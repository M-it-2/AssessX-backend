package AssessX_backend.repository;

import AssessX_backend.model.PracticeHint;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PracticeHintRepository extends JpaRepository<PracticeHint, Long> {

    boolean existsByUserIdAndAssignmentId(Long userId, Long assignmentId);

    void deleteByAssignmentId(Long assignmentId);
}
