package AssessX_backend.repository;

import AssessX_backend.model.Test;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TestRepository extends JpaRepository<Test, Long> {

    List<Test> findAllByOrderByCreatedAtDesc();

    List<Test> findByCreatedById(Long userId);
}
