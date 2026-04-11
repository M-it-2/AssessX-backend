package AssessX_backend.repository;

import AssessX_backend.model.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, Long> {
    List<Assignment> findByGroupId(Long groupId);
    List<Assignment> findByCreatedById(Long userId);
    List<Assignment> findByGroupIdIn(Collection<Long> groupIds);
    List<Assignment> findByTestId(Long testId);
    List<Assignment> findByPracticeId(Long practiceId);
}
