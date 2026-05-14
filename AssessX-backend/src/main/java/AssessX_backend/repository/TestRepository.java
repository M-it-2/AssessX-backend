package AssessX_backend.repository;

import AssessX_backend.model.Test;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TestRepository extends JpaRepository<Test, Long> {

    List<Test> findAllByOrderByCreatedAtDesc();

    List<Test> findByCreatedById(Long userId);

    Optional<Test> findByTitle(String title);
}
