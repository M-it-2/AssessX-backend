package AssessX_backend.repository;

import AssessX_backend.model.CodePractice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CodePracticeRepository extends JpaRepository<CodePractice, Long> {

    List<CodePractice> findAllByOrderByCreatedAtDesc();

    List<CodePractice> findByCreatedById(Long userId);
}
