package AssessX_backend.repository;

import AssessX_backend.model.CodeSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CodeSubmissionRepository extends JpaRepository<CodeSubmission, Long> {

    List<CodeSubmission> findByResultId(Long resultId);

    List<CodeSubmission> findByResultIdOrderBySubmittedAtDesc(Long resultId);
}
